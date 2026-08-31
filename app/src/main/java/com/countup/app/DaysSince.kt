package com.countup.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
