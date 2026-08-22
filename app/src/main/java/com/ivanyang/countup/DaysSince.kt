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
 * @return whole days from [anchorDate] to [today]; negative when [anchorDate]
 *         is in the future, so a future anchor counts down (e.g. -5).
 */
fun daysSince(anchorDate: LocalDate, today: LocalDate): Long {
    return ChronoUnit.DAYS.between(anchorDate, today)
}
