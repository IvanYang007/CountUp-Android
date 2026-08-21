package com.ivanyang.countup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DaysSinceTest {

    @Test
    fun `today produces zero`() {
        val today = LocalDate.of(2026, 7, 21)
        assertEquals(0, daysSince(today, today))
    }

    @Test
    fun `known past date produces expected positive count`() {
        val lastHaircut = LocalDate.of(2026, 7, 4)
        val today = LocalDate.of(2026, 7, 21)
        assertEquals(17, daysSince(lastHaircut, today))
    }

    @Test
    fun `future date produces zero`() {
        val future = LocalDate.of(2030, 1, 1)
        val today = LocalDate.of(2026, 7, 21)
        assertEquals(0, daysSince(future, today))
    }

    @Test
    fun `yesterday produces one`() {
        val today = LocalDate.of(2026, 7, 21)
        assertEquals(1, daysSince(today.minusDays(1), today))
    }

    @Test
    fun `date picker utc milliseconds convert to intended local date`() {
        val date = LocalDate.of(2026, 1, 15)
        val utcMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(date, datePickerMillisToLocalDate(utcMillis))
    }

    @Test
    fun `leap day boundary produces expected calendar-day count`() {
        // 2024 is a leap year: Feb 28 -> Mar 1 is two calendar days.
        val fromLeap = LocalDate.of(2024, 2, 28)
        val afterLeap = LocalDate.of(2024, 3, 1)
        assertEquals(2, daysSince(fromLeap, afterLeap))

        // 2023 is not a leap year: Feb 28 -> Mar 1 is one calendar day.
        val fromNonLeap = LocalDate.of(2023, 2, 28)
        val afterNonLeap = LocalDate.of(2023, 3, 1)
        assertEquals(1, daysSince(fromNonLeap, afterNonLeap))
    }
}
