package com.sarpai.app.ai

import com.sarpai.app.data.Redactor
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class ChatMsg(val role: String, val content: String)   // role: "user" | "assistant"

sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Err(val message: String) : AiResult()
}

data class AiRequest(
    val apiKey: String, val model: String, val endpoint: String?, val system: String,
    val messages: List<ChatMsg>, val maxTokens: Int, val temperature: Double
)

interface AiProvider {
    val id: String
    val label: String
    val defaultModel: String
    suspend fun chat(r: AiRequest): AiResult
}

private val client = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
private val JSON = "application/json".toMediaType()

private suspend fun post(url: String, headers: Map<String, String>, body: JSONObject): Pair<Int, String>? =
    suspendCancellableCoroutine { cont ->
        // Bad URL or illegal characters in a header value (e.g. a pasted key with a newline) throw
        // IllegalArgumentException; treat that as a failed request instead of crashing the app.
        val call = try {
            val rb = Request.Builder().url(url).post(body.toString().toRequestBody(JSON))
            headers.forEach { (k, v) -> rb.header(k, v) }
            client.newCall(rb.build())
        } catch (_: IllegalArgumentException) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resume(null) }
            override fun onResponse(call: Call, response: Response) {
                // Reading the body can time out; an exception here would otherwise leave the coroutine waiting forever.
                val result = try {
                    response.use { it.code to (it.body?.string() ?: "") }
                } catch (_: IOException) { null }
                if (cont.isActive) cont.resume(result)
            }
        })
    }

private fun apiMessage(body: String): String = try {
    val j = JSONObject(body); val e = j.opt("error")
    when (e) { is JSONObject -> e.optString("message"); is String -> e; else -> "" }
} catch (_: Exception) { "" }

private fun httpError(code: Int, body: String): AiResult.Err {
    val detail = Redactor.redact(apiMessage(body)).take(200)
    val msg = when (code) {
        400 -> "Request rejected (400) — check the model name and settings."
        401, 403 -> "The API key was rejected (HTTP $code). It may be invalid, expired, or lack access."
        404 -> "Model or endpoint not found (404). Check the model name / endpoint."
        429 -> "Rate limit or quota reached (429). Wait a bit or check your provider billing/quota."
        in 500..599 -> "The provider is having problems (HTTP $code). Try again later."
        else -> "Unexpected response (HTTP $code)."
    }
    return AiResult.Err(if (detail.isNotBlank()) "$msg\nProvider said: $detail" else msg)
}

private const val NET_ERR = "Could not reach the provider. Check your internet connection and endpoint."
private const val EMPTY = "The provider returned an empty response."

object GeminiProvider : AiProvider {
    override val id = "gemini"; override val label = "Google Gemini"; override val defaultModel = "gemini-2.5-flash"
    override suspend fun chat(r: AiRequest): AiResult {
        val contents = JSONArray()
        r.messages.forEach {
            contents.put(JSONObject().put("role", if (it.role == "assistant") "model" else "user")
                .put("parts", JSONArray().put(JSONObject().put("text", it.content))))
        }
        val body = JSONObject().put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", r.temperature).put("maxOutputTokens", r.maxTokens))
        if (r.system.isNotBlank()) body.put("systemInstruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", r.system))))
        val (code, text) = post(
            "https://generativelanguage.googleapis.com/v1beta/models/${r.model}:generateContent",
            mapOf("x-goog-api-key" to r.apiKey), body) ?: return AiResult.Err(NET_ERR)
        if (code !in 200..299) return httpError(code, text)
        return try {
            val cand = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
            val parts = cand.optJSONObject("content")?.optJSONArray("parts")
            val out = if (parts == null) "" else (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }
            when {
                out.isNotBlank() -> AiResult.Ok(out)
                cand.optString("finishReason") == "MAX_TOKENS" ->
                    AiResult.Err("The model used the whole token limit before answering. Increase \"Max response tokens\" in Settings.")
                else -> AiResult.Err(EMPTY)
            }
        } catch (_: Exception) { AiResult.Err(EMPTY) }
    }
}

object ClaudeProvider : AiProvider {
    override val id = "claude"; override val label = "Anthropic Claude"; override val defaultModel = "claude-sonnet-4-5"
    override suspend fun chat(r: AiRequest): AiResult {
        val msgs = JSONArray()
        r.messages.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject().put("model", r.model).put("max_tokens", r.maxTokens)
            .put("temperature", r.temperature).put("messages", msgs)
        if (r.system.isNotBlank()) body.put("system", r.system)
        val (code, text) = post("https://api.anthropic.com/v1/messages",
            mapOf("x-api-key" to r.apiKey, "anthropic-version" to "2023-06-01"), body) ?: return AiResult.Err(NET_ERR)
        if (code !in 200..299) return httpError(code, text)
        return try {
            val arr = JSONObject(text).getJSONArray("content")
            val out = (0 until arr.length()).map { arr.getJSONObject(it) }
                .filter { it.optString("type") == "text" }.joinToString("") { it.optString("text") }
            if (out.isBlank()) AiResult.Err(EMPTY) else AiResult.Ok(out)
        } catch (_: Exception) { AiResult.Err(EMPTY) }
    }
}

/** OpenAI, and any OpenAI-compatible server via a custom base URL (e.g. https://host/v1). */
object OpenAiProvider : AiProvider {
    override val id = "openai"; override val label = "OpenAI / compatible"; override val defaultModel = "gpt-4o-mini"
    override suspend fun chat(r: AiRequest): AiResult {
        val msgs = JSONArray()
        if (r.system.isNotBlank()) msgs.put(JSONObject().put("role", "system").put("content", r.system))
        r.messages.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val official = r.endpoint.isNullOrBlank()
        val base = (r.endpoint?.takeIf { it.isNotBlank() } ?: "https://api.openai.com/v1").trimEnd('/')
        val body = JSONObject().put("model", r.model).put("messages", msgs)
        // Official API: newer models need max_completion_tokens; reasoning models (gpt-5, o-series) accept only default temperature.
        // Custom OpenAI-compatible servers keep the classic max_tokens + temperature.
        if (official) {
            body.put("max_completion_tokens", r.maxTokens)
            val m = r.model.lowercase()
            val fixedTemp = m.startsWith("gpt-5") || Regex("^o\\d").containsMatchIn(m)
            if (!fixedTemp) body.put("temperature", r.temperature)
        } else {
            body.put("max_tokens", r.maxTokens).put("temperature", r.temperature)
        }
        if (!base.startsWith("https://")) return AiResult.Err("Endpoint must start with https://")
        val (code, text) = post("$base/chat/completions",
            mapOf("Authorization" to "Bearer ${r.apiKey}"), body) ?: return AiResult.Err(NET_ERR)
        if (code !in 200..299) return httpError(code, text)
        return try {
            val out = JSONObject(text).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content")
            if (out.isBlank()) AiResult.Err(EMPTY) else AiResult.Ok(out)
        } catch (_: Exception) { AiResult.Err(EMPTY) }
    }
}

object ProviderRegistry {
    val all: List<AiProvider> = listOf(GeminiProvider, ClaudeProvider, OpenAiProvider)
    fun get(id: String): AiProvider = all.firstOrNull { it.id == id } ?: GeminiProvider
}
