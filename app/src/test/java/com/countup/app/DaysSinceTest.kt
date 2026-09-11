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

    @Test
    fun `TimeDisplayMode next cycles predictably`() {
        assertEquals(TimeDisplayMode.ELAPSED_BREAKDOWN, TimeDisplayMode.DAYS.next())
        assertEquals(TimeDisplayMode.TOTAL_WEEKS, TimeDisplayMode.ELAPSED_BREAKDOWN.next())
        assertEquals(TimeDisplayMode.DAYS, TimeDisplayMode.TOTAL_WEEKS.next())
    }

    @Test
    fun `decomposeTime formats DAYS mode correctly`() {
        val anchor = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2026, 7, 1)
        val result = decomposeTime(anchor, today, TimeDisplayMode.DAYS)
        assertEquals("546", result.valueText)
        assertEquals(R.string.unit_days, result.unitLabelRes)
    }

    @Test
    fun `decomposeTime formats DAYS mode with singular DAY for exactly 1 day interval`() {
        val today = LocalDate.of(2026, 7, 21)

        // 1 day in past
        val pastYesterday = today.minusDays(1)
        val pastResult = decomposeTime(pastYesterday, today, TimeDisplayMode.DAYS)
        assertEquals("1", pastResult.valueText)
        assertEquals(R.string.unit_day_singular, pastResult.unitLabelRes)

        // 1 day in future
        val futureTomorrow = today.plusDays(1)
        val futureResult = decomposeTime(futureTomorrow, today, TimeDisplayMode.DAYS)
        assertEquals("1", futureResult.valueText)
        assertEquals(R.string.unit_day_singular, futureResult.unitLabelRes)

        // 0 days
        val zeroResult = decomposeTime(today, today, TimeDisplayMode.DAYS)
        assertEquals("0", zeroResult.valueText)
        assertEquals(R.string.unit_days, zeroResult.unitLabelRes)

        // 2 days
        val twoDaysResult = decomposeTime(today.minusDays(2), today, TimeDisplayMode.DAYS)
        assertEquals("2", twoDaysResult.valueText)
        assertEquals(R.string.unit_days, twoDaysResult.unitLabelRes)
    }

    @Test
    fun `decomposeTime formats ELAPSED_BREAKDOWN mode for multi-year and partial intervals`() {
        val today = LocalDate.of(2026, 7, 21)

        // 1. Exact Today
        val zeroRes = decomposeTime(today, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("0 DAYS", zeroRes.valueText)
        assertEquals(R.string.unit_today, zeroRes.unitLabelRes)

        // 2. 1 year, 3 months, 10 days
        val past1 = LocalDate.of(2025, 4, 11)
        val res1 = decomposeTime(past1, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("1 YEAR 3 MONTHS 10 DAYS", res1.valueText)
        assertEquals(R.string.unit_none, res1.unitLabelRes)

        // 3. Less than a year (e.g. 5 months 9 days)
        val past2 = LocalDate.of(2026, 2, 12)
        val res2 = decomposeTime(past2, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("5 MONTHS 9 DAYS", res2.valueText)
        assertEquals(R.string.unit_none, res2.unitLabelRes)

        // 4. Future target
        val future = LocalDate.of(2026, 9, 25)
        val futureRes = decomposeTime(future, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("2 MONTHS 4 DAYS", futureRes.valueText)
        assertEquals(R.string.unit_none, futureRes.unitLabelRes)
    }

    @Test
    fun `decomposeTime formats TOTAL_WEEKS mode correctly`() {
        val today = LocalDate.of(2026, 7, 21)

        // Exact 0 days
        val zero = decomposeTime(today, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("0 WEEKS", zero.valueText)
        assertEquals(R.string.unit_none, zero.unitLabelRes)

        // Exact weeks (28 days = 4w)
        val fourWeeks = today.minusDays(28)
        val res1 = decomposeTime(fourWeeks, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("4 WEEKS", res1.valueText)
        assertEquals(R.string.unit_none, res1.unitLabelRes)

        // Partial weeks (31 days = 4w 3d)
        val fourWeeksThreeDays = today.minusDays(31)
        val res2 = decomposeTime(fourWeeksThreeDays, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("4 WEEKS 3 DAYS", res2.valueText)
        assertEquals(R.string.unit_none, res2.unitLabelRes)

        // Future weeks (16 days until = 2 WEEKS 2 DAYS)
        val future = today.plusDays(16)
        val res3 = decomposeTime(future, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("2 WEEKS 2 DAYS", res3.valueText)
        assertEquals(R.string.unit_none, res3.unitLabelRes)
    }

    @Test
    fun `decomposeTime satisfies user format requirements for 1 WEEKS 2 DAYS and 2 DAYS`() {
        val today = LocalDate.of(2026, 7, 21)
        val nineDaysAgo = today.minusDays(9)
        val weeksRes = decomposeTime(nineDaysAgo, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("1 WEEKS 2 DAYS", weeksRes.valueText)
        assertEquals(R.string.unit_none, weeksRes.unitLabelRes)

        val twoDaysAgo = today.minusDays(2)
        val breakdownRes = decomposeTime(twoDaysAgo, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("2 DAYS", breakdownRes.valueText)
        assertEquals(R.string.unit_none, breakdownRes.unitLabelRes)
    }

    @Test
    fun `decomposeTime formats future target in DAYS mode with unit_days`() {
        val today = LocalDate.of(2026, 7, 21)
        val future = today.plusDays(10)
        val result = decomposeTime(future, today, TimeDisplayMode.DAYS)
        assertEquals("10", result.valueText)
        assertEquals(R.string.unit_days, result.unitLabelRes)
    }

    @Test
    fun `decomposeTime formats Simplified Chinese correctly`() {
        val today = LocalDate.of(2026, 7, 21)
        val past = LocalDate.of(2025, 4, 11)
        val localeZh = java.util.Locale.SIMPLIFIED_CHINESE

        val breakdown = decomposeTime(past, today, TimeDisplayMode.ELAPSED_BREAKDOWN, localeZh)
        assertEquals("1 年 3 个月 10 天", breakdown.valueText)

        val weeks = decomposeTime(today.minusDays(31), today, TimeDisplayMode.TOTAL_WEEKS, localeZh)
        assertEquals("4 周 3 天", weeks.valueText)

        val zero = decomposeTime(today, today, TimeDisplayMode.ELAPSED_BREAKDOWN, localeZh)
        assertEquals("0 天", zero.valueText)
    }

    @Test
    fun `decomposeTime formats Traditional Chinese correctly`() {
        val today = LocalDate.of(2026, 7, 21)
        val past = LocalDate.of(2025, 4, 11)
        val localeTw = java.util.Locale.TRADITIONAL_CHINESE

        val breakdown = decomposeTime(past, today, TimeDisplayMode.ELAPSED_BREAKDOWN, localeTw)
        assertEquals("1 年 3 個月 10 天", breakdown.valueText)

        val weeks = decomposeTime(today.minusDays(31), today, TimeDisplayMode.TOTAL_WEEKS, localeTw)
        assertEquals("4 週 3 天", weeks.valueText)

        val zero = decomposeTime(today, today, TimeDisplayMode.ELAPSED_BREAKDOWN, localeTw)
        assertEquals("0 天", zero.valueText)
    }
}

