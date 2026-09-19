package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Display modes for the tactile odometer on the item card.
 */
enum class TimeDisplayMode(val code: String) {
    DAYS("DAYS"),
    ELAPSED_BREAKDOWN("ELAPSED_BREAKDOWN"),
    TOTAL_WEEKS("TOTAL_WEEKS");

    fun next(): TimeDisplayMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    fun next(count: Long): TimeDisplayMode {
        val absDays = abs(count)
        return when (this) {
            DAYS -> ELAPSED_BREAKDOWN
            ELAPSED_BREAKDOWN -> if (absDays >= 7L) TOTAL_WEEKS else DAYS
            TOTAL_WEEKS -> DAYS
        }
    }

    companion object {
        fun fromCode(code: String?): TimeDisplayMode =
            entries.firstOrNull { it.code == code } ?: DAYS
    }
}

/**
 * Encapsulates the formatted value and unit label resource for a decomposed time interval.
 */
@Immutable
data class DecomposedTime(
    val valueText: String,
    @get:StringRes val unitLabelRes: Int,
    val mode: TimeDisplayMode = TimeDisplayMode.DAYS,
)

/**
 * Pure calendar-day arithmetic shared by the app and the widget.
 *
 * Uses calendar dates, never elapsed milliseconds, so DST transitions and leap
 * days are handled by [LocalDate] and never affect the day count.
 * Reading the count never mutates the stored date.
 *
 * @return whole days from [anchorDate] to [today]; negative when [anchorDate]
 *         is in the future, so a future anchor counts down (e.g. -5).
 */
fun daysSince(anchorDate: LocalDate, today: LocalDate): Long {
    return ChronoUnit.DAYS.between(anchorDate, today)
}

/**
 * Decomposes an anchor-to-today interval into natural, human-friendly units based on [mode].
 */
fun decomposeTime(
    anchorDate: LocalDate,
    today: LocalDate,
    mode: TimeDisplayMode = TimeDisplayMode.DAYS,
    locale: java.util.Locale = java.util.Locale.getDefault(),
): DecomposedTime {
    val totalDays = daysSince(anchorDate, today)
    val isChinese = locale.language.equals("zh", ignoreCase = true)
    val isTraditional = isChinese && (
        locale.country.equals("TW", ignoreCase = true) ||
        locale.country.equals("HK", ignoreCase = true) ||
        locale.country.equals("MO", ignoreCase = true) ||
        locale.script.equals("Hant", ignoreCase = true)
    )

    return when (mode) {
        TimeDisplayMode.DAYS -> {
            val absDays = abs(totalDays)
            DecomposedTime(
                valueText = absDays.toString(),
                unitLabelRes = if (absDays == 1L) R.string.unit_day_singular else R.string.unit_days,
                mode = TimeDisplayMode.DAYS,
            )
        }
        TimeDisplayMode.ELAPSED_BREAKDOWN -> {
            if (totalDays == 0L) {
                DecomposedTime(
                    valueText = if (isChinese) "0 天" else "0 DAYS",
                    unitLabelRes = R.string.unit_today,
                    mode = TimeDisplayMode.ELAPSED_BREAKDOWN,
                )
            } else {
                val isFuture = totalDays < 0
                val (startDate, endDate) = if (isFuture) today to anchorDate else anchorDate to today
                val period = Period.between(startDate, endDate)
                val years = period.years
                val months = period.months
                val days = period.days

                val parts = mutableListOf<String>()
                if (isChinese) {
                    val monthUnit = if (isTraditional) "個月" else "个月"
                    if (years > 0) parts.add("$years 年")
                    if (months > 0) parts.add("$months $monthUnit")
                    if (days > 0 || parts.isEmpty()) parts.add("$days 天")
                } else {
                    if (years > 0) parts.add(if (years == 1) "1 YEAR" else "$years YEARS")
                    if (months > 0) parts.add(if (months == 1) "1 MONTH" else "$months MONTHS")
                    if (days > 0 || parts.isEmpty()) parts.add(if (days == 1) "1 DAY" else "$days DAYS")
                }

                val text = parts.joinToString(" ")
                DecomposedTime(
                    valueText = text,
                    unitLabelRes = R.string.unit_none,
                    mode = TimeDisplayMode.ELAPSED_BREAKDOWN,
                )
            }
        }
        TimeDisplayMode.TOTAL_WEEKS -> {
            if (totalDays == 0L) {
                val zeroText = if (isChinese) {
                    if (isTraditional) "0 週" else "0 周"
                } else {
                    "0 WEEKS"
                }
                DecomposedTime(
                    valueText = zeroText,
                    unitLabelRes = R.string.unit_none,
                    mode = TimeDisplayMode.TOTAL_WEEKS,
                )
            } else {
                val absDays = abs(totalDays)
                val weeks = absDays / 7
                val remDays = absDays % 7

                val text = if (isChinese) {
                    val weekUnit = if (isTraditional) "週" else "周"
                    if (remDays == 0L) {
                        "$weeks $weekUnit"
                    } else {
                        "$weeks $weekUnit $remDays 天"
                    }
                } else {
                    if (remDays == 0L) {
                        "$weeks WEEKS"
                    } else {
                        "$weeks WEEKS $remDays DAYS"
                    }
                }

                DecomposedTime(
                    valueText = text,
                    unitLabelRes = R.string.unit_none,
                    mode = TimeDisplayMode.TOTAL_WEEKS,
                )
            }
        }
    }
}

/**
 * Evaluates whether a given day count represents a landmark zen milestone.
 * Milestones: 7, 30, 50, 100, 200, 365, 500, 1000, and multiples of 1000.
 */
fun isMilestoneDay(count: Long): Boolean {
    if (count <= 0) return false
    return when (count) {
        7L, 30L, 50L, 100L, 200L, 365L, 500L, 1000L -> true
        else -> count % 1000L == 0L
    }
}

