package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HeroWidgetTest {

    @Test
    fun `milestone days are identified properly for hero widget counts`() {
        assertTrue(isMilestoneDay(7))
        assertTrue(isMilestoneDay(30))
        assertTrue(isMilestoneDay(50))
        assertTrue(isMilestoneDay(100))
        assertTrue(isMilestoneDay(365))
        assertTrue(isMilestoneDay(1000))
        assertTrue(isMilestoneDay(2000))
    }

    @Test
    fun `hero widget circle styling resolves distinct high contrast colors`() {
        val rowSage = WidgetRowData(
            id = "1",
            name = "Meditation",
            count = 30,
            futureFlag = false,
            cardColor = "sage_forest",
        )
        val styleSage = resolveWidgetCircleStyle(rowSage, 0)
        // Forest green badge
        assertEquals(0xFF33523D.toInt(), styleSage.circleColor)
        // Light ink on dark green
        assertEquals(0xFFFFFFFF.toInt(), styleSage.textInk)

        val rowGold = WidgetRowData(
            id = "2",
            name = "Anniversary",
            count = 365,
            futureFlag = false,
            cardColor = "ink_gold",
        )
        val styleGold = resolveWidgetCircleStyle(rowGold, 0)
        // Ochre gold badge
        assertEquals(0xFFDEB285.toInt(), styleGold.circleColor)
        // Dark ink on ochre gold
        assertEquals(0xFF2C2416.toInt(), styleGold.textInk)
    }

    @Test
    fun `daysSince calculation correctly produces day counts for past and future hero items`() {
        val today = LocalDate.of(2026, 8, 31)
        val pastDate = LocalDate.of(2026, 8, 1) // 30 days ago
        val futureDate = LocalDate.of(2026, 9, 7) // 7 days in future

        assertEquals(30L, daysSince(pastDate, today))
        assertEquals(-7L, daysSince(futureDate, today))
    }

    @Test
    fun `hero widget display mode decomposes time into human readable intervals`() {
        val today = LocalDate.of(2026, 8, 31)
        val pastDate = LocalDate.of(2025, 5, 11) // 1 year, 3 months, 20 days

        val daysDecomposed = decomposeTime(pastDate, today, TimeDisplayMode.DAYS)
        assertEquals("477", daysDecomposed.valueText)
        assertEquals(R.string.unit_days, daysDecomposed.unitLabelRes)

        val breakdownDecomposed = decomposeTime(pastDate, today, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertEquals("1y 3m 20d", breakdownDecomposed.valueText)
        assertEquals(R.string.unit_elapsed, breakdownDecomposed.unitLabelRes)

        val weeksDecomposed = decomposeTime(pastDate, today, TimeDisplayMode.TOTAL_WEEKS)
        assertEquals("68w 1d", weeksDecomposed.valueText)
        assertEquals(R.string.unit_weeks, weeksDecomposed.unitLabelRes)
    }

    @Test
    fun `hero widget display mode cycles properly across all 3 modes`() {
        var mode = TimeDisplayMode.DAYS
        mode = mode.next()
        assertEquals(TimeDisplayMode.ELAPSED_BREAKDOWN, mode)
        mode = mode.next()
        assertEquals(TimeDisplayMode.TOTAL_WEEKS, mode)
        mode = mode.next()
        assertEquals(TimeDisplayMode.DAYS, mode)
    }
}
