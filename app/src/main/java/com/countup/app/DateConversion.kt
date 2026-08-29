package com.countup.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Converts the Material DatePicker's selection (a UTC-milliseconds epoch value)
 * into a calendar [LocalDate]. The picker reports the value in UTC, so the
 * conversion must pin the zone to UTC before deriving the date to avoid
 * timezone drift; the device's local timezone is never applied here.
 */
fun datePickerMillisToLocalDate(value: Long): LocalDate {
    return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()
}

/**
 * Formats a date using the device's localized medium format (e.g. "Aug 2, 2026").
 * Kept here as the single formatting helper for both the app and widget.
 */
fun formatLocalized(date: LocalDate): String {
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
}

/**
 * Derives the uppercase date sub-label for a count-up card.
 * - When [count] < 0 (future anchor date): returns "UNTIL <date>"
 * - When [count] >= 0 (today or past date): returns "SINCE <date>"
 */
fun formatAnchorDateSubLabel(
    count: Long,
    date: LocalDate,
    sinceTemplate: String,
    untilTemplate: String,
): String {
    val formattedDate = formatLocalized(date)
    val template = if (count < 0) untilTemplate else sinceTemplate
    return String.format(template, formattedDate).uppercase()
}
