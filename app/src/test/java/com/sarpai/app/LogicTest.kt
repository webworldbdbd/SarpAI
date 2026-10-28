package com.sarpai.app

import com.sarpai.app.data.MemoryEntity
import com.sarpai.app.data.MemoryRetriever
import com.sarpai.app.data.Redactor
import org.junit.Assert.*
import org.junit.Test

class LogicTest {
    private fun m(id: Long, cat: String, text: String, on: Boolean = true) = MemoryEntity(id, cat, text, on, 0)

    @Test fun retrievesOnlyRelevantEnabledMemory() {
        val all = listOf(m(1, "Study", "I study electronics at polytechnic"), m(2, "Food", "I like rice"), m(3, "Study", "electronics secret", false))
        val r = MemoryRetriever.relevant(all, "help with my electronics homework")
        assertEquals(listOf(1L), r.map { it.id })
    }
    @Test fun emptyQueryReturnsNothing() = assertTrue(MemoryRetriever.relevant(listOf(m(1, "a", "b c")), "  ").isEmpty())
    @Test fun limitIsRespected() {
        val all = (1..10).map { m(it.toLong(), "x", "alpha beta") }
        assertEquals(3, MemoryRetriever.relevant(all, "alpha", 3).size)
    }
    @Test fun banglaTextMatches() {
        val r = MemoryRetriever.relevant(listOf(m(1, "নাম", "আমার নাম শাফায়েত")), "আমার নাম কি")
        assertEquals(1, r.size)
    }
    @Test fun banglaWordsAreNotSplitIntoFragments() {
        // "আম" (mango) must not match "আমার" (my) — whole words only.
        val r = MemoryRetriever.relevant(listOf(m(1, "ফল", "আম আমার প্রিয়")), "আমার নাম কি")
        assertEquals(listOf(1L), r.map { it.id })   // matches via the whole word আমার
        val none = MemoryRetriever.relevant(listOf(m(2, "ফল", "আম খেতে ভালো")), "আমার নাম কি")
        assertTrue(none.isEmpty())
    }
    @Test fun redactorHidesKeys() {
        val s = Redactor.redact("bad key sk-abcdefghijklmnop and AIzaSyA1234567890abcdefghijk")
        assertFalse(s.contains("sk-abc")); assertFalse(s.contains("AIzaSy")); assertTrue(s.contains("[REDACTED]"))
    }
}
