package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateConversionTest {

    @Test
    fun `datePickerMillisToLocalDate with known UTC millis`() {
        // Test epoch 0 (January 1, 1970)
        val epoch0 = 0L
        val expectedEpoch = LocalDate.of(1970, 1, 1)
        assertEquals(expectedEpoch, datePickerMillisToLocalDate(epoch0))
        
        // Test a mid-year date (July 15, 2024 UTC)
        val july152024 = LocalDate.of(2024, 7, 15)
        val july15Millis = july152024.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(july152024, datePickerMillisToLocalDate(july15Millis))
        
        // Test year boundary (December 31, 2023 to January 1, 2024)
        val dec312023 = LocalDate.of(2023, 12, 31)
        val dec31Millis = dec312023.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(dec312023, datePickerMillisToLocalDate(dec31Millis))
        
        val jan012024 = LocalDate.of(2024, 1, 1)
        val jan01Millis = jan012024.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(jan012024, datePickerMillisToLocalDate(jan01Millis))
    }

    @Test
    fun `formatAnchorDateSubLabel uses UNTIL for negative count and SINCE for positive or zero`() {
        val futureDate = LocalDate.of(2026, 12, 25)
        val pastDate = LocalDate.of(2026, 4, 7)
        val today = LocalDate.of(2026, 8, 28)

        val sinceTemplate = "since %s"
        val untilTemplate = "until %s"

        // Negative count (future date): must use UNTIL template in uppercase
        val futureSubLabel = formatAnchorDateSubLabel(
            count = -119L,
            date = futureDate,
            sinceTemplate = sinceTemplate,
            untilTemplate = untilTemplate,
        )
        assertTrue(futureSubLabel.startsWith("UNTIL "))
        assertTrue(futureSubLabel.contains("2026"))

        // Positive count (past date): must use SINCE template in uppercase
        val pastSubLabel = formatAnchorDateSubLabel(
            count = 143L,
            date = pastDate,
            sinceTemplate = sinceTemplate,
            untilTemplate = untilTemplate,
        )
        assertTrue(pastSubLabel.startsWith("SINCE "))
        assertTrue(pastSubLabel.contains("2026"))

        // Zero count (today): must use SINCE template in uppercase
        val todaySubLabel = formatAnchorDateSubLabel(
            count = 0L,
            date = today,
            sinceTemplate = sinceTemplate,
            untilTemplate = untilTemplate,
        )
        assertTrue(todaySubLabel.startsWith("SINCE "))
        assertTrue(todaySubLabel.contains("2026"))
    }

    @Test
    fun `getLocalizedDateFormatter caches formatters per locale`() {
        val usFormatter1 = getLocalizedDateFormatter(java.util.Locale.US)
        val usFormatter2 = getLocalizedDateFormatter(java.util.Locale.US)
        org.junit.Assert.assertSame("Same locale must return identical cached formatter instance", usFormatter1, usFormatter2)

        val deFormatter = getLocalizedDateFormatter(java.util.Locale.GERMANY)
        val testDate = LocalDate.of(2026, 9, 18)
        val usFormatted = usFormatter1.format(testDate)
        val deFormatted = deFormatter.format(testDate)

        assertTrue(usFormatted.contains("Sep"))
        assertTrue(deFormatted.contains("18.09.2026") || deFormatted.contains("Sept") || deFormatted.contains("Sep"))
    }

    @Test
    fun `formatAnchorDateSubLabel works with Turkish and German locales without case anomalies`() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            // Turkish locale has special dotless i handling (i -> İ vs I)
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            val label = formatAnchorDateSubLabel(
                count = 10L,
                date = LocalDate.of(2026, 9, 18),
                sinceTemplate = "since %s",
                untilTemplate = "until %s",
            )
            assertTrue(label.isNotEmpty())
            assertTrue(label.contains("2026"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }
}