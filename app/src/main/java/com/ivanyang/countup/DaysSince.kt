package com.ivanyang.countup

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure calendar-day arithmetic shared by the app and the widget.
 *
 * Uses calendar dates, never elapsed milliseconds, so DST transitions and leap
 * days are handled by [LocalDate] and never affect the day count.
 * Reading the count never mutates the stored date.
 *
 * @return whole days from [anchorDate] to [today], clamped to >= 0 so a
 *         future anchor displays 0.
 */
fun daysSince(anchorDate: LocalDate, today: LocalDate): Long {
    return ChronoUnit.DAYS.between(anchorDate, today).coerceAtLeast(0)
}
