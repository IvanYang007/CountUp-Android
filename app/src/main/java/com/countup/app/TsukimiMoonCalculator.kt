package com.countup.app

import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.floor

/**
 * Astronomical Moon Phase Calculator based on Jean Meeus' synodic lunation algorithms.
 * Completely offline, zero-permission, and battery-neutral.
 */
object TsukimiMoonCalculator {

    /** Mean synodic month period in solar days. */
    const val SYNODIC_MONTH = 29.530588853

    /** Reference New Moon Epoch: 2000-01-06 18:14 UTC (Julian Date 2451550.26). */
    private const val REFERENCE_NEW_MOON_JD = 2451550.26

    enum class MoonPhase(val defaultName: String, val poeticKanji: String) {
        NEW_MOON("NEW MOON", "朔月"),
        WAXING_CRESCENT("WAXING CRESCENT", "娥眉月"),
        FIRST_QUARTER("FIRST QUARTER", "上弦月"),
        WAXING_GIBBOUS("WAXING GIBBOUS", "盈凸月"),
        FULL_MOON("FULL MOON", "望月"),
        WANING_GIBBOUS("WANING GIBBOUS", "亏凸月"),
        LAST_QUARTER("LAST QUARTER", "下弦月"),
        WANING_CRESCENT("WANING CRESCENT", "残月")
    }

    data class LunarState(
        val ageDays: Double,
        val phaseFraction: Double,
        val illuminationFraction: Double,
        val phase: MoonPhase,
        val isWaxing: Boolean
    )

    /**
     * Converts a civil Gregorian [date] to Julian Day Number at 12:00 UTC (noon).
     */
    fun toJulianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        val day = date.dayOfMonth

        if (month <= 2) {
            year -= 1
            month += 12
        }

        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)

        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) +
            day + b - 1524.0
    }

    /**
     * Calculates the exact [LunarState] for a given [date].
     */
    fun calculate(date: LocalDate): LunarState {
        val jd = toJulianDay(date)
        val daysSinceEpoch = jd - REFERENCE_NEW_MOON_JD
        val newMoons = daysSinceEpoch / SYNODIC_MONTH
        val rawFraction = newMoons - floor(newMoons)
        val phaseFraction = if (rawFraction < 0.0) rawFraction + 1.0 else rawFraction

        val ageDays = phaseFraction * SYNODIC_MONTH
        val angleRad = phaseFraction * 2.0 * Math.PI
        val illuminationFraction = (1.0 - cos(angleRad)) / 2.0
        val isWaxing = phaseFraction <= 0.5

        val phase = when {
            ageDays < 1.84 || ageDays >= 27.69 -> MoonPhase.NEW_MOON
            ageDays < 5.53 -> MoonPhase.WAXING_CRESCENT
            ageDays < 9.22 -> MoonPhase.FIRST_QUARTER
            ageDays < 12.92 -> MoonPhase.WAXING_GIBBOUS
            ageDays < 16.61 -> MoonPhase.FULL_MOON
            ageDays < 20.30 -> MoonPhase.WANING_GIBBOUS
            ageDays < 23.99 -> MoonPhase.LAST_QUARTER
            else -> MoonPhase.WANING_CRESCENT
        }

        return LunarState(
            ageDays = ageDays,
            phaseFraction = phaseFraction,
            illuminationFraction = illuminationFraction,
            phase = phase,
            isWaxing = isWaxing
        )
    }

    /**
     * Formats a calm, bilingual badge for the Tsukimi widget (e.g. `· 望月 FULL MOON ·`).
     */
    fun formatBilingualVerse(state: LunarState): String {
        return "· ${state.phase.poeticKanji} ${state.phase.defaultName} ·"
    }
}
