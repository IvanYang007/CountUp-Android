package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SolarTermCalendarTest {

    @Test
    fun getActiveSolarTerm_resolvesSeptember4_toEndOfHeat() {
        val date = LocalDate.of(2026, 9, 4)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(14, term.id)
        assertEquals(R.string.season_autumn, term.seasonRes)
        assertEquals(R.string.solar_term_end_of_heat, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesSeptember7_toWhiteDew() {
        val date = LocalDate.of(2026, 9, 7)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(15, term.id)
        assertEquals(R.string.season_autumn, term.seasonRes)
        assertEquals(R.string.solar_term_white_dew, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesSeptember23_toAutumnalEquinox() {
        val date = LocalDate.of(2026, 9, 23)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(16, term.id)
        assertEquals(R.string.season_autumn, term.seasonRes)
        assertEquals(R.string.solar_term_autumnal_equinox, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesFebruary4_toBeginningOfSpring() {
        val date = LocalDate.of(2026, 2, 4)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(1, term.id)
        assertEquals(R.string.season_spring, term.seasonRes)
        assertEquals(R.string.solar_term_beginning_of_spring, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesJanuary2_toWinterSolstice() {
        val date = LocalDate.of(2026, 1, 2)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(22, term.id)
        assertEquals(R.string.season_winter, term.seasonRes)
        assertEquals(R.string.solar_term_winter_solstice, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesJanuary5_toMinorCold() {
        val date = LocalDate.of(2026, 1, 5)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(23, term.id)
        assertEquals(R.string.season_winter, term.seasonRes)
        assertEquals(R.string.solar_term_minor_cold, term.nameRes)
    }

    @Test
    fun getActiveSolarTerm_resolvesJanuary20_toMajorCold() {
        val date = LocalDate.of(2026, 1, 20)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(24, term.id)
        assertEquals(R.string.season_winter, term.seasonRes)
        assertEquals(R.string.solar_term_major_cold, term.nameRes)
    }

    @Test
    fun solarTerms_containsExactly24Terms_with6PerSeason() {
        assertEquals(24, SolarTermCalendar.TERMS.size)

        val springCount = SolarTermCalendar.TERMS.count { it.seasonRes == R.string.season_spring }
        val summerCount = SolarTermCalendar.TERMS.count { it.seasonRes == R.string.season_summer }
        val autumnCount = SolarTermCalendar.TERMS.count { it.seasonRes == R.string.season_autumn }
        val winterCount = SolarTermCalendar.TERMS.count { it.seasonRes == R.string.season_winter }

        assertEquals(6, springCount)
        assertEquals(6, summerCount)
        assertEquals(6, autumnCount)
        assertEquals(6, winterCount)

        SolarTermCalendar.TERMS.forEach { term ->
            assertTrue(term.id in 1..24)
            assertTrue(term.month in 1..12)
            assertTrue(term.day in 1..31)
            assertTrue(term.seasonRes != 0)
            assertTrue(term.nameRes != 0)
            assertTrue(term.degree in 0..345)
            assertEquals(0, term.degree % 15)
            assertTrue(term.line1Res != 0)
            assertTrue(term.line2Res != 0)
        }
    }

    @Test
    fun getActiveSolarTerm_endOfHeat_hasCorrectDegreeAndLines() {
        val date = LocalDate.of(2026, 9, 5)
        val term = SolarTermCalendar.getActiveSolarTerm(date)

        assertEquals(14, term.id)
        assertEquals(150, term.degree)
        assertEquals(R.string.solar_term_whisper_14_line1, term.line1Res)
        assertEquals(R.string.solar_term_whisper_14_line2, term.line2Res)
    }

    @Test
    fun getNextCardinalAnchor_onSeptember5_resolves18DaysToAutumnalEquinox() {
        val date = LocalDate.of(2026, 9, 5)
        val countdown = SolarTermCalendar.getNextCardinalAnchor(date)

        assertEquals(18L, countdown.days)
        assertEquals(R.string.solar_term_autumnal_equinox, countdown.targetNameRes)
    }

    @Test
    fun getNextCardinalAnchor_onAutumnalEquinox_resolvesToWinterSolstice() {
        val date = LocalDate.of(2026, 9, 23)
        val countdown = SolarTermCalendar.getNextCardinalAnchor(date)

        assertEquals(89L, countdown.days)
        assertEquals(R.string.solar_term_winter_solstice, countdown.targetNameRes)
    }

    @Test
    fun getNextCardinalAnchor_onDecember25_resolvesToSpringEquinoxNextYear() {
        val date = LocalDate.of(2026, 12, 25)
        val countdown = SolarTermCalendar.getNextCardinalAnchor(date)

        assertEquals(85L, countdown.days)
        assertEquals(R.string.solar_term_spring_equinox, countdown.targetNameRes)
    }
}

