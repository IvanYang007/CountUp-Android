package com.countup.app

/**
 * Pure functional sanitizer for raw speech recognition candidates.
 * Extracts the first non-blank transcription, trims outer whitespace,
 * and strips accidental surrounding quotation marks added by speech engines.
 */
object VoiceTextSanitizer {

    /**
     * Sanitizes raw candidates returned from [android.speech.RecognizerIntent.EXTRA_RESULTS].
     * @return The cleaned item title, or null if candidates are null, empty, or all blank.
     */
    fun sanitize(candidates: List<String>?): String? =
        candidates?.asSequence()
            ?.map { it.trim().trim('"', '“', '”', '\'', '‘', '’') }
            ?.firstOrNull { it.isNotBlank() }
}
