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
enum class TimeDisplayMode {
    DAYS,
    ELAPSED_BREAKDOWN,
    TOTAL_WEEKS;

    fun next(): TimeDisplayMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
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
): DecomposedTime {
    val totalDays = daysSince(anchorDate, today)
    return when (mode) {
        TimeDisplayMode.DAYS -> {
            val isFuture = totalDays < 0
            DecomposedTime(
                valueText = if (isFuture) kotlin.math.abs(totalDays).toString() else totalDays.toString(),
                unitLabelRes = if (isFuture) R.string.unit_until_short else R.string.unit_days,
                mode = TimeDisplayMode.DAYS,
            )
        }
        TimeDisplayMode.ELAPSED_BREAKDOWN -> {
            if (totalDays == 0L) {
                DecomposedTime(
                    valueText = "0",
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
                if (years > 0) parts.add("${years}y")
                if (months > 0) parts.add("${months}m")
                if (days > 0 || parts.isEmpty()) parts.add("${days}d")

                val text = parts.joinToString(" ")
                DecomposedTime(
                    valueText = text,
                    unitLabelRes = if (isFuture) R.string.unit_until_short else R.string.unit_elapsed,
                    mode = TimeDisplayMode.ELAPSED_BREAKDOWN,
                )
            }
        }
        TimeDisplayMode.TOTAL_WEEKS -> {
            if (totalDays == 0L) {
                DecomposedTime(
                    valueText = "0",
                    unitLabelRes = R.string.unit_weeks,
                    mode = TimeDisplayMode.TOTAL_WEEKS,
                )
            } else {
                val isFuture = totalDays < 0
                val absDays = abs(totalDays)
                val weeks = absDays / 7
                val remDays = absDays % 7

                val text = if (remDays == 0L) {
                    "${weeks}w"
                } else {
                    "${weeks}w ${remDays}d"
                }

                DecomposedTime(
                    valueText = text,
                    unitLabelRes = if (isFuture) R.string.unit_until_short else R.string.unit_weeks,
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

