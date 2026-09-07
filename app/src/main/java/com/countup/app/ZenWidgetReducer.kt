package com.countup.app

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Units of time that can be cycled in-place on the Zen Horizon widget.
 */
enum class ZenWidgetDisplayUnit {
    DAYS,
    MONTHS,
    WEEKS,
    HOURS,
    YEARS;

    fun next(): ZenWidgetDisplayUnit {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

/**
 * Immutable, decoupled view state for Zen widgets, computed cleanly by [ZenWidgetReducer].
 */
@Immutable
data class ZenWidgetViewState(
    val itemId: String,
    val title: String,
    val daysCount: Long,
    val primaryValueText: String,
    val unitLabelText: String,
    val compactValueText: String,
    val startDateFormatted: String,
    val milestoneGoal: Long,
    val milestoneRemainingDays: Long,
    val milestoneProgress: Float, // 0.0f..1.0f
    val palette: WidgetColorPalette,
)

object ZenWidgetReducer {

    private val STANDARD_MILESTONES = listOf(
        7L, 30L, 50L, 100L, 200L, 365L, 500L, 1000L
    )

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    /**
     * Resolves the target item for a widget based on explicit binding, pin status, and widget visibility.
     */
    fun resolveTargetItem(items: List<CountUpItem>, boundItemId: String?): CountUpItem? =
        items.firstOrNull { it.id == boundItemId }
            ?: items.firstOrNull { it.isPinned && it.showInWidget }
            ?: items.firstOrNull { it.showInWidget }
            ?: items.firstOrNull()

    /**
     * Resolves the nearest upcoming milestone goal strictly greater than [count].
     */
    fun resolveNextMilestone(count: Long): Long {
        if (count < 0) return 0L
        for (m in STANDARD_MILESTONES) {
            if (count < m) return m
        }
        val thousands = (count / 1000L) + 1L
        return thousands * 1000L
    }

    /**
     * Resolves the previous landmark milestone base at or below [count].
     */
    fun resolvePreviousMilestone(count: Long): Long {
        if (count <= 7L) return 0L
        for (i in STANDARD_MILESTONES.indices.reversed()) {
            if (count >= STANDARD_MILESTONES[i]) {
                return STANDARD_MILESTONES[i]
            }
        }
        return (count / 1000L) * 1000L
    }

    /**
     * Formats large day counts into compact 3-4 glyph strings (e.g. 412 -> "412", 1200 -> "1.2k", 12500 -> "12k").
     */
    fun formatCompactNumber(count: Long): String {
        val absCount = abs(count)
        val sign = if (count < 0) "-" else ""
        return when {
            absCount < 1000L -> "$sign$absCount"
            absCount < 10000L -> {
                val value = absCount / 1000.0
                String.format(Locale.US, "$sign%.1fk", value)
            }
            else -> "$sign${absCount / 1000L}k"
        }
    }

    /**
     * Resolves a concise 1-word label for 1x1 pebble widgets.
     * Prioritizes explicit [customTag], then single-word item comment, then the first word of item name.
     */
    fun resolveOneWordLabel(item: CountUpItem, customTag: String? = null): String =
        item.resolveOneWordLabel(customTag)

    /**
     * Decomposes [days] into value text and uppercase unit string for [unit].
     */
    fun decomposeUnit(days: Long, unit: ZenWidgetDisplayUnit): Pair<String, String> {
        return when (unit) {
            ZenWidgetDisplayUnit.DAYS -> days.toString() to "DAYS"
            ZenWidgetDisplayUnit.MONTHS -> {
                val months = days / 30.4375
                String.format(Locale.US, "%.1f", months) to "MONTHS"
            }
            ZenWidgetDisplayUnit.WEEKS -> {
                val weeks = days / 7.0
                String.format(Locale.US, "%.1f", weeks) to "WEEKS"
            }
            ZenWidgetDisplayUnit.HOURS -> {
                val hours = days * 24L
                String.format(Locale.US, "%,d", hours) to "HOURS"
            }
            ZenWidgetDisplayUnit.YEARS -> {
                val years = days / 365.25
                String.format(Locale.US, "%.1f", years) to "YEARS"
            }
        }
    }

    /**
     * Transforms a [CountUpItem] into an immutable [ZenWidgetViewState].
     */
    fun resolveZenWidgetState(
        item: CountUpItem,
        today: LocalDate,
        unit: ZenWidgetDisplayUnit = ZenWidgetDisplayUnit.DAYS,
        isDarkMode: Boolean = false,
    ): ZenWidgetViewState {
        val anchorDate = LocalDate.ofEpochDay(item.epochDay)
        val days = java.time.temporal.ChronoUnit.DAYS.between(anchorDate, today)

        val (valueText, unitLabel) = decomposeUnit(days, unit)
        val compactText = formatCompactNumber(days)

        val nextMilestone = resolveNextMilestone(days)
        val prevMilestone = resolvePreviousMilestone(days)

        val range = max(1L, nextMilestone - prevMilestone)
        val progress = ((days - prevMilestone).toFloat() / range).coerceIn(0f, 1f)
        val remaining = max(0L, nextMilestone - days)

        val formattedStart = "Since " + anchorDate.format(DATE_FORMATTER)
        val cardStyle = resolveCardStyle(item.cardColor, isDark = isDarkMode)
        val basePalette = WidgetThemeTokens.resolve(isDarkMode)
        val palette = basePalette.copy(
            canvasBg = cardStyle.cardBg.toArgb(),
            primaryInk = cardStyle.primaryInk.toArgb(),
            secondaryInk = cardStyle.mutedInk.toArgb(),
        )

        return ZenWidgetViewState(
            itemId = item.id,
            title = item.name,
            daysCount = days,
            primaryValueText = valueText,
            unitLabelText = unitLabel,
            compactValueText = compactText,
            startDateFormatted = formattedStart,
            milestoneGoal = nextMilestone,
            milestoneRemainingDays = remaining,
            milestoneProgress = progress,
            palette = palette,
        )
    }
}
