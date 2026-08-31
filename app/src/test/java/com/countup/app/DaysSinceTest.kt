package com.countup.app

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
    fun `future date produces negative countdown`() {
        val today = LocalDate.of(2026, 7, 21)
        assertEquals(-5, daysSince(today.plusDays(5), today))
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

    @Test
    fun `isMilestoneDay correctly identifies landmark zen milestone day counts`() {
        // Milestone days
        val milestones = listOf(7L, 30L, 50L, 100L, 200L, 365L, 500L, 1000L, 2000L, 5000L, 10000L)
        milestones.forEach { day ->
            org.junit.Assert.assertTrue("Day $day should be a milestone", isMilestoneDay(day))
        }

        // Non-milestone days
        val nonMilestones = listOf(-10L, -1L, 0L, 1L, 2L, 6L, 8L, 29L, 31L, 49L, 51L, 99L, 101L, 364L, 366L, 999L, 1001L)
        nonMilestones.forEach { day ->
            org.junit.Assert.assertFalse("Day $day should NOT be a milestone", isMilestoneDay(day))
        }
    }
}
