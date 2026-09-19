package com.countup.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Converts the Material DatePicker's selection (a UTC-milliseconds epoch value)
 * into a calendar [LocalDate]. The picker reports the value in UTC, so the
 * conversion must pin the zone to UTC before deriving the date to avoid
 * timezone drift; the device's local timezone is never applied here.
 */
fun datePickerMillisToLocalDate(value: Long): LocalDate {
    return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()
}

private val mediumDateFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()

/**
 * Retrieves a thread-safe cached [DateTimeFormatter] instance for the given [locale].
 */
fun getLocalizedDateFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter {
    return mediumDateFormatters.computeIfAbsent(locale) { loc ->
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(loc)
    }
}

/**
 * Formats a date using the device's localized medium format (e.g. "Aug 2, 2026").
 * Reuses thread-safe cached [DateTimeFormatter] instances keyed by locale to eliminate
 * redundant allocations on scroll hot paths.
 */
fun formatLocalized(date: LocalDate, locale: Locale = Locale.getDefault()): String {
    return getLocalizedDateFormatter(locale).format(date)
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
    locale: Locale = Locale.getDefault(),
): String {
    val formattedDate = formatLocalized(date, locale)
    val template = if (count < 0) untilTemplate else sinceTemplate
    return String.format(locale, template, formattedDate).uppercase(locale)
}
