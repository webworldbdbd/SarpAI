package com.sarpai.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarpai.app.SarpApp
import com.sarpai.app.ai.*
import com.sarpai.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo: Repo = (app as SarpApp).repo
    private val db = repo.db
    private val s = repo.settings

    val conversations = db.conversations().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memory = db.memory().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = db.notes().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentId = MutableStateFlow<Long?>(null)
    val messages = currentId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else db.messages().observe(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val draft = MutableStateFlow("")               // survives tab switches
    val sending = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val theme = MutableStateFlow(s.theme)
    val provider = MutableStateFlow(s.provider)
    val keyVersion = MutableStateFlow(0)          // bump to re-read key presence
    val testStatus = MutableStateFlow<String?>(null)
    private var job: Job? = null

    fun newChat() { cancel(); currentId.value = null; error.value = null }
    fun open(id: Long) { cancel(); currentId.value = id; error.value = null }

    fun send(text: String) {
        if (sending.value || text.isBlank()) return        // blocks duplicate taps
        if (!repo.keys.has(provider.value)) { error.value = "No API key for ${ProviderRegistry.get(provider.value).label}. Add one in Settings."; return }
        sending.value = true; error.value = null
        job = viewModelScope.launch {
            try {
                var cid = currentId.value
                if (cid == null) {
                    cid = db.conversations().insert(ConversationEntity(title = text.trim().take(40), updatedAt = now()))
                    currentId.value = cid
                }
                db.messages().insert(MessageEntity(conversationId = cid, role = "user", content = text.trim(), createdAt = now()))
                generate(cid, text)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { error.value = "Something went wrong: ${e.message ?: e.javaClass.simpleName}"
            } finally { sending.value = false }
        }
    }

    /** Retry after a failure (last message is the user's) or regenerate (drops last assistant reply). */
    fun retryOrRegenerate() {
        val cid = currentId.value ?: return
        if (sending.value) return
        sending.value = true; error.value = null
        job = viewModelScope.launch {
            try {
                val list = db.messages().list(cid)
                val last = list.lastOrNull() ?: return@launch
                if (last.role == "assistant") db.messages().delete(last.id)
                val lastUser = db.messages().list(cid).lastOrNull { it.role == "user" } ?: return@launch
                generate(cid, lastUser.content)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { error.value = "Something went wrong: ${e.message ?: e.javaClass.simpleName}"
            } finally { sending.value = false }
        }
    }

    private suspend fun generate(cid: Long, query: String) {
        val p = ProviderRegistry.get(provider.value)
        val key = repo.keys.get(p.id) ?: return
        val history = db.messages().list(cid).map { ChatMsg(it.role, it.content) }
        val facts = if (s.memoryEnabled) MemoryRetriever.relevant(db.memory().enabled(), query) else emptyList()
        val system = buildString {
            if (s.customInstructions.isNotBlank()) append(s.customInstructions.trim()).append("\n\n")
            if (facts.isNotEmpty()) {
                append("Saved facts the user chose to share (use only if relevant):\n")
                facts.forEach { append("- [").append(it.category).append("] ").append(it.text).append('\n') }
            }
        }
        val res = p.chat(AiRequest(key, s.model(p.id, p.defaultModel), s.endpoint.takeIf { p.id == "openai" },
            system, history, s.maxTokens, s.temperature.toDouble()))
        when (res) {
            is AiResult.Ok -> db.messages().insert(MessageEntity(conversationId = cid, role = "assistant", content = res.text, createdAt = now()))
            is AiResult.Err -> error.value = res.message
        }
        db.conversations().touch(cid, now())
    }

    fun cancel() { job?.cancel(); sending.value = false }

    // --- settings / keys ---
    fun setProvider(id: String) { s.provider = id; provider.value = id; testStatus.value = null }
    fun saveKey(id: String, key: String) {
        if (key.isBlank()) return
        if (!repo.keys.available) { error.value = "Secure key storage is not available on this device, so the key could not be saved."; return }
        repo.keys.set(id, key); keyVersion.value++
    }
    fun deleteKey(id: String) { repo.keys.delete(id); keyVersion.value++; testStatus.value = null }
    fun setTheme(t: String) { s.theme = t; theme.value = t }

    /** Real call with a tiny prompt. Result is whatever the provider actually returned. */
    fun testConnection() {
        val p = ProviderRegistry.get(provider.value)
        val key = repo.keys.get(p.id)
        if (key.isNullOrBlank()) { testStatus.value = "No key saved for ${p.label}."; return }
        testStatus.value = "Testing…"
        viewModelScope.launch {
            val r = p.chat(AiRequest(key, s.model(p.id, p.defaultModel), s.endpoint.takeIf { p.id == "openai" },
                "", listOf(ChatMsg("user", "Reply with the single word OK.")), 256, 0.0))
            testStatus.value = when (r) { is AiResult.Ok -> "Connected: ${p.label} responded."; is AiResult.Err -> "Failed: ${r.message}" }
        }
    }

    // --- conversations ---
    fun rename(id: Long, title: String) = viewModelScope.launch { if (title.isNotBlank()) db.conversations().rename(id, title.trim()) }
    fun deleteConversation(id: Long) = viewModelScope.launch {
        if (currentId.value == id) cancel()      // stop a running reply so it is not written to a deleted chat
        db.messages().deleteForConversation(id); db.conversations().delete(id)
        if (currentId.value == id) currentId.value = null
    }
    fun deleteAllConversations() = viewModelScope.launch {
        cancel(); db.messages().deleteAll(); db.conversations().deleteAll(); currentId.value = null
    }

    // --- memory / notes ---
    fun addMemory(cat: String, text: String) = viewModelScope.launch { if (text.isNotBlank()) db.memory().insert(MemoryEntity(category = cat.ifBlank { "General" }, text = text.trim(), createdAt = now())) }
    fun setMemoryEnabled(id: Long, on: Boolean) = viewModelScope.launch { db.memory().setEnabled(id, on) }
    fun deleteMemory(id: Long) = viewModelScope.launch { db.memory().delete(id) }
    fun clearMemory() = viewModelScope.launch { db.memory().deleteAll() }
    fun addNote(title: String, body: String, isTask: Boolean) = viewModelScope.launch {
        if (title.isNotBlank() || body.isNotBlank()) db.notes().insert(NoteEntity(title = title.trim(), body = body.trim(), isTask = isTask, updatedAt = now()))
    }
    fun setDone(id: Long, done: Boolean) = viewModelScope.launch { db.notes().setDone(id, done, now()) }
    fun deleteNote(id: Long) = viewModelScope.launch { db.notes().delete(id) }

    /** Wipes DB content, keys and settings. */
    fun eraseEverything() = viewModelScope.launch {
        cancel()
        db.messages().deleteAll(); db.conversations().deleteAll(); db.memory().deleteAll(); db.notes().deleteAll()
        repo.keys.deleteAll(); s.clear()
        currentId.value = null; theme.value = s.theme; provider.value = s.provider; keyVersion.value++
    }

    fun dbSizeBytes(): Long = getApplication<Application>().getDatabasePath("sarp.db").let { f ->
        listOf(f, java.io.File(f.path + "-wal"), java.io.File(f.path + "-shm")).sumOf { if (it.exists()) it.length() else 0L }
    }
    fun cacheSizeBytes(): Long = getApplication<Application>().cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun clearCache() { getApplication<Application>().cacheDir.listFiles()?.forEach { it.deleteRecursively() } }

    private fun now() = System.currentTimeMillis()
}
