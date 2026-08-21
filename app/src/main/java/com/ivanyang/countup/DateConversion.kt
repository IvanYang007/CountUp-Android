package com.ivanyang.countup

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
