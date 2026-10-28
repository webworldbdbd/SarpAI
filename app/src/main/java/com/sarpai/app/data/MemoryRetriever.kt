package com.sarpai.app.data

/** Sends only the few saved facts that overlap with the user's message — never the whole memory. */
object MemoryRetriever {
    private val splitter = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private fun tokens(s: String) = s.lowercase().split(splitter).filter { it.length > 1 }.toSet()

    fun relevant(entries: List<MemoryEntity>, query: String, limit: Int = 5): List<MemoryEntity> {
        val q = tokens(query)
        if (q.isEmpty()) return emptyList()
        return entries.filter { it.enabled }
            .map { it to tokens(it.category + " " + it.text).count { t -> t in q } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit).map { it.first }
    }
}

/** Removes anything that looks like a provider key from text before it is shown or logged. */
object Redactor {
    private val pattern = Regex("(sk-[A-Za-z0-9_\\-]{8,}|AIza[0-9A-Za-z_\\-]{20,}|sk-ant-[A-Za-z0-9_\\-]{8,})")
    fun redact(s: String) = pattern.replace(s, "[REDACTED]")
}
