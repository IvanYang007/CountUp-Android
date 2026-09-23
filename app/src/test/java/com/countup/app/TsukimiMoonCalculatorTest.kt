package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TsukimiMoonCalculatorTest {

    @Test
    fun testJulianDayCalculation() {
        // 2000-01-01 12:00 TT is JD 2451545.0
        val jd = TsukimiMoonCalculator.toJulianDay(LocalDate.of(2000, 1, 1))
        assertEquals(2451545.0, jd, 0.001)
    }

    @Test
    fun testKnownNewMoon() {
        // 2024-01-11 was a known New Moon (11:57 UTC)
        val state = TsukimiMoonCalculator.calculate(LocalDate.of(2024, 1, 11))
        assertEquals(TsukimiMoonCalculator.MoonPhase.NEW_MOON, state.phase)
        assertTrue("Age should be close to 0 or 29.5, got: ${state.ageDays}", state.ageDays < 1.5 || state.ageDays > 28.0)
        assertTrue("Illumination fraction should be near 0, got: ${state.illuminationFraction}", state.illuminationFraction < 0.05)
    }

    @Test
    fun testKnownFullMoon() {
        // 2024-09-17 was the Mid-Autumn Festival (Full Moon)
        val state = TsukimiMoonCalculator.calculate(LocalDate.of(2024, 9, 17))
        assertEquals(TsukimiMoonCalculator.MoonPhase.FULL_MOON, state.phase)
        assertTrue("Age should be near 14.8, got: ${state.ageDays}", state.ageDays in 13.5..16.5)
        assertTrue("Illumination fraction should be near 1.0, got: ${state.illuminationFraction}", state.illuminationFraction > 0.90)
    }

    @Test
    fun testBilingualVerseFormatting() {
        val fullMoonState = TsukimiMoonCalculator.LunarState(
            ageDays = 14.8,
            phaseFraction = 0.5,
            illuminationFraction = 1.0,
            phase = TsukimiMoonCalculator.MoonPhase.FULL_MOON,
            isWaxing = true
        )
        val verse = TsukimiMoonCalculator.formatBilingualVerse(fullMoonState)
        assertEquals("· 望月 FULL MOON ·", verse)
    }
}
