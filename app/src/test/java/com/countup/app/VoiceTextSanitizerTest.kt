package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceTextSanitizerTest {

    @Test
    fun nullOrEmptyCandidatesReturnNull() {
        assertNull(VoiceTextSanitizer.sanitize(null))
        assertNull(VoiceTextSanitizer.sanitize(emptyList()))
        assertNull(VoiceTextSanitizer.sanitize(listOf("")))
        assertNull(VoiceTextSanitizer.sanitize(listOf("   ", "\t", "\n")))
    }

    @Test
    fun validSpeechCandidateIsTrimmed() {
        assertEquals("Buy milk", VoiceTextSanitizer.sanitize(listOf("Buy milk")))
        assertEquals("Read 20 pages", VoiceTextSanitizer.sanitize(listOf("  Read 20 pages  \n")))
    }

    @Test
    fun stripsAccidentalQuotationMarks() {
        assertEquals("Workout", VoiceTextSanitizer.sanitize(listOf("\"Workout\"")))
        assertEquals("Cold shower", VoiceTextSanitizer.sanitize(listOf("“Cold shower”")))
        assertEquals("Meditation", VoiceTextSanitizer.sanitize(listOf("'Meditation'")))
        assertEquals("Deep breathing", VoiceTextSanitizer.sanitize(listOf("‘Deep breathing’")))
        assertEquals("跑步", VoiceTextSanitizer.sanitize(listOf("“跑步”")))
        assertEquals("早起背单词", VoiceTextSanitizer.sanitize(listOf("‘早起背单词’")))
    }

    @Test
    fun fallsBackToSecondCandidateWhenFirstIsBlank() {
        val candidates = listOf("", "   ", "Walk the dog", "Walk dog")
        assertEquals("Walk the dog", VoiceTextSanitizer.sanitize(candidates))
    }

    @Test
    fun preservesInternalPunctuationAndSpacing() {
        assertEquals("Call Mom & Dad", VoiceTextSanitizer.sanitize(listOf("  Call Mom & Dad  ")))
        assertEquals("Run 5k: personal best", VoiceTextSanitizer.sanitize(listOf("\"Run 5k: personal best\"")))
    }
}
