package com.sarpai.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** API keys live ONLY here: AES-256 encrypted prefs, master key in Android Keystore. Never in Room, logs or backups. */
class SecureKeyStore(private val ctx: Context) {
    private val fileName = "sarp_secure_keys"

    private fun open(): SharedPreferences = EncryptedSharedPreferences.create(
        ctx, fileName,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Keystore problems on some devices throw here. Retry once with a fresh file; if it still fails, run without key storage instead of crashing. */
    private val prefs: SharedPreferences? = try { open() } catch (_: Exception) {
        try { ctx.deleteSharedPreferences(fileName); open() } catch (_: Exception) { null }
    }

    /** False when the secure storage could not be opened on this device. */
    val available: Boolean get() = prefs != null
    fun get(provider: String): String? = prefs?.getString("key_$provider", null)
    fun has(provider: String) = !get(provider).isNullOrBlank()
    fun set(provider: String, key: String) { prefs?.edit()?.putString("key_$provider", key.trim())?.apply() }
    fun delete(provider: String) { prefs?.edit()?.remove("key_$provider")?.apply() }
    fun deleteAll() { prefs?.edit()?.clear()?.apply() }
}

class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var provider: String get() = p.getString("provider", "gemini")!!; set(v) = p.edit().putString("provider", v).apply()
    fun model(id: String, def: String) = p.getString("model_$id", null)?.takeIf { it.isNotBlank() } ?: def
    fun setModel(id: String, v: String) = p.edit().putString("model_$id", v.trim()).apply()
    var endpoint: String get() = p.getString("endpoint_openai", "")!!; set(v) = p.edit().putString("endpoint_openai", v.trim()).apply()
    var theme: String get() = p.getString("theme", "system")!!; set(v) = p.edit().putString("theme", v).apply()
    var memoryEnabled: Boolean get() = p.getBoolean("memory_enabled", true); set(v) = p.edit().putBoolean("memory_enabled", v).apply()
    var customInstructions: String get() = p.getString("instructions", "")!!; set(v) = p.edit().putString("instructions", v).apply()
    var maxTokens: Int get() = p.getInt("max_tokens", 2048); set(v) = p.edit().putInt("max_tokens", v).apply()
    var temperature: Float get() = p.getFloat("temperature", 0.7f); set(v) = p.edit().putFloat("temperature", v).apply()
    fun clear() = p.edit().clear().apply()
}

class Repo(val ctx: Context) {
    val db: AppDb = Room.databaseBuilder(ctx, AppDb::class.java, "sarp.db").build()
    val keys = SecureKeyStore(ctx)
    val settings = Settings(ctx)
}
