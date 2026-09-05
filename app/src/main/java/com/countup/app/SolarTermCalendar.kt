package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Represents one of the 24 Solar Terms (二十四节气) in the astronomical solar cycle.
 */
@Immutable
data class SolarTerm(
    val id: Int,
    @get:StringRes val seasonRes: Int,
    @get:StringRes val nameRes: Int,
    val month: Int,
    val day: Int,
)

object SolarTermCalendar {
    val TERMS: List<SolarTerm> = listOf(
        SolarTerm(1, R.string.season_spring, R.string.solar_term_beginning_of_spring, 2, 4),
        SolarTerm(2, R.string.season_spring, R.string.solar_term_rain_water, 2, 19),
        SolarTerm(3, R.string.season_spring, R.string.solar_term_awakening_of_insects, 3, 5),
        SolarTerm(4, R.string.season_spring, R.string.solar_term_spring_equinox, 3, 20),
        SolarTerm(5, R.string.season_spring, R.string.solar_term_pure_brightness, 4, 4),
        SolarTerm(6, R.string.season_spring, R.string.solar_term_grain_rain, 4, 20),
        SolarTerm(7, R.string.season_summer, R.string.solar_term_beginning_of_summer, 5, 5),
        SolarTerm(8, R.string.season_summer, R.string.solar_term_grain_buds, 5, 21),
        SolarTerm(9, R.string.season_summer, R.string.solar_term_grain_in_ear, 6, 5),
        SolarTerm(10, R.string.season_summer, R.string.solar_term_summer_solstice, 6, 21),
        SolarTerm(11, R.string.season_summer, R.string.solar_term_minor_heat, 7, 7),
        SolarTerm(12, R.string.season_summer, R.string.solar_term_major_heat, 7, 22),
        SolarTerm(13, R.string.season_autumn, R.string.solar_term_beginning_of_autumn, 8, 7),
        SolarTerm(14, R.string.season_autumn, R.string.solar_term_end_of_heat, 8, 23),
        SolarTerm(15, R.string.season_autumn, R.string.solar_term_white_dew, 9, 7),
        SolarTerm(16, R.string.season_autumn, R.string.solar_term_autumnal_equinox, 9, 23),
        SolarTerm(17, R.string.season_autumn, R.string.solar_term_cold_dew, 10, 8),
        SolarTerm(18, R.string.season_autumn, R.string.solar_term_frosts_descent, 10, 23),
        SolarTerm(19, R.string.season_winter, R.string.solar_term_beginning_of_winter, 11, 7),
        SolarTerm(20, R.string.season_winter, R.string.solar_term_minor_snow, 11, 22),
        SolarTerm(21, R.string.season_winter, R.string.solar_term_major_snow, 12, 7),
        SolarTerm(22, R.string.season_winter, R.string.solar_term_winter_solstice, 12, 21),
        SolarTerm(23, R.string.season_winter, R.string.solar_term_minor_cold, 1, 5),
        SolarTerm(24, R.string.season_winter, R.string.solar_term_major_cold, 1, 20),
    )

    // Ordered chronologically by calendar day of year (Jan 1 to Dec 31)
    private val CHRONOLOGICAL_TERMS: List<SolarTerm> = listOf(
        TERMS[22], // 小寒 Jan 5
        TERMS[23], // 大寒 Jan 20
        TERMS[0],  // 立春 Feb 4
        TERMS[1],  // 雨水 Feb 19
        TERMS[2],  // 惊蛰 Mar 5
        TERMS[3],  // 春分 Mar 20
        TERMS[4],  // 清明 Apr 4
        TERMS[5],  // 谷雨 Apr 20
        TERMS[6],  // 立夏 May 5
        TERMS[7],  // 小满 May 21
        TERMS[8],  // 芒种 Jun 5
        TERMS[9],  // 夏至 Jun 21
        TERMS[10], // 小暑 Jul 7
        TERMS[11], // 大暑 Jul 22
        TERMS[12], // 立秋 Aug 7
        TERMS[13], // 处暑 Aug 23
        TERMS[14], // 白露 Sep 7
        TERMS[15], // 秋分 Sep 23
        TERMS[16], // 寒露 Oct 8
        TERMS[17], // 霜降 Oct 23
        TERMS[18], // 立冬 Nov 7
        TERMS[19], // 小雪 Nov 22
        TERMS[20], // 大雪 Dec 7
        TERMS[21], // 冬至 Dec 21
    )

    /**
     * Determines the active solar term for the specified calendar [date].
     */
    fun getActiveSolarTerm(date: LocalDate): SolarTerm {
        val monthDay = date.monthValue * 100 + date.dayOfMonth
        return CHRONOLOGICAL_TERMS.lastOrNull { monthDay >= it.month * 100 + it.day } ?: TERMS[21]
    }
}

