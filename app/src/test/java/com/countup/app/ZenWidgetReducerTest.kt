package com.countup.app

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZenWidgetReducerTest {

    private val fixedToday = LocalDate.of(2026, 9, 6)

    @Test
    fun `unit decomposition produces accurate values across all 5 units`() {
        // 412 days
        val days = 412L

        val (daysVal, daysUnit) = ZenWidgetReducer.decomposeUnit(days, ZenWidgetDisplayUnit.DAYS)
        assertEquals("412", daysVal)
        assertEquals("DAYS", daysUnit)

        val (monthsVal, monthsUnit) = ZenWidgetReducer.decomposeUnit(days, ZenWidgetDisplayUnit.MONTHS)
        assertEquals("13.5", monthsVal)
        assertEquals("MONTHS", monthsUnit)

        val (weeksVal, weeksUnit) = ZenWidgetReducer.decomposeUnit(days, ZenWidgetDisplayUnit.WEEKS)
        assertEquals("58.9", weeksVal)
        assertEquals("WEEKS", weeksUnit)

        val (hoursVal, hoursUnit) = ZenWidgetReducer.decomposeUnit(days, ZenWidgetDisplayUnit.HOURS)
        assertEquals("9,888", hoursVal)
        assertEquals("HOURS", hoursUnit)

        val (yearsVal, yearsUnit) = ZenWidgetReducer.decomposeUnit(days, ZenWidgetDisplayUnit.YEARS)
        assertEquals("1.1", yearsVal)
        assertEquals("YEARS", yearsUnit)
    }

    @Test
    fun `formatCompactNumber compresses large numerals without overflow`() {
        assertEquals("0", ZenWidgetReducer.formatCompactNumber(0))
        assertEquals("88", ZenWidgetReducer.formatCompactNumber(88))
        assertEquals("412", ZenWidgetReducer.formatCompactNumber(412))
        assertEquals("999", ZenWidgetReducer.formatCompactNumber(999))
        assertEquals("1.0k", ZenWidgetReducer.formatCompactNumber(1000))
        assertEquals("1.2k", ZenWidgetReducer.formatCompactNumber(1200))
        assertEquals("9.9k", ZenWidgetReducer.formatCompactNumber(9940))
        assertEquals("10k", ZenWidgetReducer.formatCompactNumber(10000))
        assertEquals("25k", ZenWidgetReducer.formatCompactNumber(25400))
    }

    @Test
    fun `milestone calculation accurately resolves next goal and progress ratio`() {
        assertEquals(7L, ZenWidgetReducer.resolveNextMilestone(3L))
        assertEquals(30L, ZenWidgetReducer.resolveNextMilestone(7L))
        assertEquals(50L, ZenWidgetReducer.resolveNextMilestone(42L))
        assertEquals(100L, ZenWidgetReducer.resolveNextMilestone(50L))
        assertEquals(365L, ZenWidgetReducer.resolveNextMilestone(200L))
        assertEquals(500L, ZenWidgetReducer.resolveNextMilestone(412L))
        assertEquals(1000L, ZenWidgetReducer.resolveNextMilestone(500L))
        assertEquals(2000L, ZenWidgetReducer.resolveNextMilestone(1000L))

        val item412 = CountUpItem(
            id = "sober",
            name = "Days Sober",
            epochDay = fixedToday.minusDays(412).toEpochDay()
        )
        val state = ZenWidgetReducer.resolveZenWidgetState(item412, fixedToday)
        assertEquals(412L, state.daysCount)
        assertEquals(500L, state.milestoneGoal)
        assertEquals(88L, state.milestoneRemainingDays)
        // 412 is between 365 and 500: (412 - 365) / (500 - 365) = 47 / 135 ≈ 0.348
        assertTrue(state.milestoneProgress > 0.34f && state.milestoneProgress < 0.36f)
    }

    @Test
    fun `ZenWidgetDisplayUnit cycles predictably through all 5 units`() {
        var unit = ZenWidgetDisplayUnit.DAYS
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.MONTHS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.WEEKS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.HOURS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.YEARS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.DAYS, unit)
    }

    @Test
    fun `resolveZenWidgetState provides correct theme tokens for dark and light mode`() {
        val item = CountUpItem(id = "meditation", name = "Meditation", epochDay = fixedToday.toEpochDay())
        
        val lightState = ZenWidgetReducer.resolveZenWidgetState(item, fixedToday, isDarkMode = false)
        assertEquals(WidgetThemeTokens.LIGHT_CANVAS_BG, lightState.palette.canvasBg)

        val darkState = ZenWidgetReducer.resolveZenWidgetState(item, fixedToday, isDarkMode = true)
        assertEquals(WidgetThemeTokens.DARK_CANVAS_BG, darkState.palette.canvasBg)
    }

    @Test
    fun `resolveZenWidgetState harmonizes palette canvasBg with item custom cardColor`() {
        val celadonItem = CountUpItem(
            id = "celadon",
            name = "Bamboo Grove",
            epochDay = fixedToday.toEpochDay(),
            cardColor = "celadon_bamboo",
        )
        val state = ZenWidgetReducer.resolveZenWidgetState(celadonItem, fixedToday, isDarkMode = false)
        val expectedCardStyle = resolveCardStyle("celadon_bamboo", isDark = false)
        assertEquals(expectedCardStyle.cardBg.toArgb(), state.palette.canvasBg)
        assertEquals(expectedCardStyle.primaryInk.toArgb(), state.palette.primaryInk)
        assertEquals(expectedCardStyle.mutedInk.toArgb(), state.palette.secondaryInk)

        val darkCeladonState = ZenWidgetReducer.resolveZenWidgetState(celadonItem, fixedToday, isDarkMode = true)
        val expectedDarkStyle = resolveCardStyle("celadon_bamboo", isDark = true)
        assertEquals(expectedDarkStyle.cardBg.toArgb(), darkCeladonState.palette.canvasBg)
    }
}

