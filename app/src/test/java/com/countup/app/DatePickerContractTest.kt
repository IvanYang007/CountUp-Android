package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests verifying date picker conversion contracts, Saturday boundary invariance,
 * and anchor date formatting.
 */
class DatePickerContractTest {

    @Test
    fun `datePickerMillisToLocalDate accurately round-trips Saturday anchor dates without drift`() {
        // Sep 12, 2026 is Saturday
        val saturdayDate = LocalDate.of(2026, 9, 12)
        assertEquals(DayOfWeek.SATURDAY, saturdayDate.dayOfWeek)

        val millis = saturdayDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val converted = datePickerMillisToLocalDate(millis)

        assertEquals(saturdayDate, converted)
        assertEquals(DayOfWeek.SATURDAY, converted.dayOfWeek)
    }

    @Test
    fun `all Saturdays across multiple years preserve day-of-week through picker conversion`() {
        var cursor = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2027, 1, 1)

        var saturdayCount = 0
        while (cursor.isBefore(end)) {
            if (cursor.dayOfWeek == DayOfWeek.SATURDAY) {
                val millis = cursor.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val converted = datePickerMillisToLocalDate(millis)
                assertEquals(cursor, converted)
                assertEquals(DayOfWeek.SATURDAY, converted.dayOfWeek)
                saturdayCount++
            }
            cursor = cursor.plusDays(1)
        }

        // Approx 52 Saturdays * 3 years = ~156 Saturdays tested
        assertTrue("Must have tested multiple Saturdays", saturdayCount >= 150)
    }

    @Test
    fun `anchor date formatting displays Saturday dates correctly for SINCE and UNTIL`() {
        val satDate = LocalDate.of(2026, 9, 12)
        val localized = formatLocalized(satDate)
        assertNotNull(localized)
        assertTrue(localized.contains("2026"))

        val sinceLabel = formatAnchorDateSubLabel(
            count = 0L,
            date = satDate,
            sinceTemplate = "since %s",
            untilTemplate = "until %s",
        )
        assertTrue(sinceLabel.startsWith("SINCE "))
        assertTrue(sinceLabel.contains("2026"))

        val untilLabel = formatAnchorDateSubLabel(
            count = -7L,
            date = satDate,
            sinceTemplate = "since %s",
            untilTemplate = "until %s",
        )
        assertTrue(untilLabel.startsWith("UNTIL "))
        assertTrue(untilLabel.contains("2026"))
    }
}
