package com.countup.app

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Units of time that can be cycled in-place on the Zen Horizon widget.
 */
enum class ZenWidgetDisplayUnit(val code: String) {
    DAYS("DAYS"),
    MONTHS("MONTHS"),
    WEEKS("WEEKS"),
    HOURS("HOURS"),
    YEARS("YEARS");

    fun next(): ZenWidgetDisplayUnit {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromCode(code: String?): ZenWidgetDisplayUnit =
            entries.firstOrNull { it.code == code } ?: DAYS
    }
}

/**
 * Sealed resolution result for widget counter target resolution.
 */
sealed interface WidgetTargetResolution {
    data class Resolved(val item: CountUpItem) : WidgetTargetResolution
    data object Unconfigured : WidgetTargetResolution
    data class Deleted(val boundItemId: String) : WidgetTargetResolution
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
    val pebbleNumberText: String,
    val startDateFormatted: String,
    val milestoneGoal: Long,
    val milestoneRemainingDays: Long,
    val milestoneProgress: Float, // 0.0f..1.0f
    val palette: WidgetColorPalette,
    val isFuture: Boolean = false,
)

object ZenWidgetReducer {

    private val STANDARD_MILESTONES = listOf(
        7L, 30L, 50L, 100L, 200L, 365L, 500L, 1000L
    )

    /**
     * Resolves the target item for a widget based on explicit binding, pin status, and widget visibility.
     * When a specific [boundItemId] was assigned but no longer exists in [items], it resolves to [WidgetTargetResolution.Deleted]
     * to avoid silently displaying an arbitrary or private item on the home screen.
     */
    fun resolveTarget(items: List<CountUpItem>, boundItemId: String?): WidgetTargetResolution {
        if (boundItemId != null) {
            val found = items.firstOrNull { it.id == boundItemId }
            return if (found != null) {
                WidgetTargetResolution.Resolved(found)
            } else {
                WidgetTargetResolution.Deleted(boundItemId)
            }
        }
        val fallback = items.firstOrNull { it.isPinned && it.showInWidget }
            ?: items.firstOrNull { it.showInWidget }
            ?: items.firstOrNull()
        return if (fallback != null) {
            WidgetTargetResolution.Resolved(fallback)
        } else {
            WidgetTargetResolution.Unconfigured
        }
    }

    /**
     * Resolves the target item for a widget. Returns null if unconfigured or bound target was deleted.
     */
    fun resolveTargetItem(items: List<CountUpItem>, boundItemId: String?): CountUpItem? =
        when (val res = resolveTarget(items, boundItemId)) {
            is WidgetTargetResolution.Resolved -> res.item
            else -> null
        }

    /**
     * Resolves the nearest upcoming milestone goal strictly greater than [count].
     */
    fun resolveNextMilestone(count: Long): Long =
        if (count < 0) 0L else STANDARD_MILESTONES.firstOrNull { count < it } ?: (((count / 1000L) + 1L) * 1000L)

    /**
     * Resolves the previous landmark milestone base at or below [count].
     */
    fun resolvePreviousMilestone(count: Long): Long =
        if (count <= 7L) 0L else STANDARD_MILESTONES.lastOrNull { count >= it } ?: ((count / 1000L) * 1000L)

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
     * Decomposes [days] into value text and uppercase unit string for [unit].
     */
    fun decomposeUnit(days: Long, unit: ZenWidgetDisplayUnit, isFuture: Boolean = false): Pair<String, String> {
        val absDays = abs(days)
        val isCountdown = isFuture || days < 0
        return when (unit) {
            ZenWidgetDisplayUnit.DAYS -> {
                val unitStr = if (isCountdown) {
                    "UNTIL"
                } else if (absDays == 1L) {
                    "DAY"
                } else {
                    "DAYS"
                }
                absDays.toString() to unitStr
            }
            ZenWidgetDisplayUnit.MONTHS -> {
                val months = absDays / 30.4375
                String.format(Locale.US, "%.1f", months) to "MONTHS"
            }
            ZenWidgetDisplayUnit.WEEKS -> {
                val weeks = absDays / 7.0
                String.format(Locale.US, "%.1f", weeks) to "WEEKS"
            }
            ZenWidgetDisplayUnit.HOURS -> {
                val hours = absDays * 24L
                String.format(Locale.US, "%,d", hours) to "HOURS"
            }
            ZenWidgetDisplayUnit.YEARS -> {
                val years = absDays / 365.25
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
        sinceTemplate: String? = null,
        untilTemplate: String? = null,
    ): ZenWidgetViewState {
        val rawDays = today.toEpochDay() - item.epochDay
        val isFuture = rawDays < 0
        val days = abs(rawDays)
        val anchorDate = LocalDate.ofEpochDay(item.epochDay)

        val (valueText, unitLabel) = decomposeUnit(days, unit, isFuture = isFuture)
        val compactText = formatCompactNumber(days)
        val pebbleNumber = if (isFuture) "${compactText}D" else compactText

        val nextMilestone = resolveNextMilestone(days)
        val prevMilestone = resolvePreviousMilestone(days)

        val range = max(1L, nextMilestone - prevMilestone)
        val progress = ((days - prevMilestone).toFloat() / range).coerceIn(0f, 1f)
        val remaining = max(0L, nextMilestone - days)

        val localizedDate = formatLocalized(anchorDate)
        val formattedStart = if (sinceTemplate != null && untilTemplate != null) {
            val template = if (isFuture) untilTemplate else sinceTemplate
            String.format(template, localizedDate)
        } else {
            (if (isFuture) "Until " else "Since ") + localizedDate
        }
        val palette = WidgetThemeTokens.resolveWithItem(item, isDarkMode = isDarkMode)

        return ZenWidgetViewState(
            itemId = item.id,
            title = item.name,
            daysCount = days,
            primaryValueText = valueText,
            unitLabelText = unitLabel,
            compactValueText = compactText,
            pebbleNumberText = pebbleNumber,
            startDateFormatted = formattedStart,
            milestoneGoal = nextMilestone,
            milestoneRemainingDays = remaining,
            milestoneProgress = progress,
            palette = palette,
            isFuture = isFuture,
        )
    }

    /**
     * Transforms a [CountUpItem] into an immutable [ZenOrbitViewState] for the single 8dp warm jade orbit dial.
     */
    fun resolveZenOrbitState(
        item: CountUpItem,
        today: LocalDate,
        isDarkMode: Boolean = false,
        customTag: String? = null,
    ): ZenOrbitViewState {
        val rawDays = today.toEpochDay() - item.epochDay
        val isFuture = rawDays < 0
        val days = abs(rawDays)
        val avgDays = item.averageResetDays
        val resets = item.resetCount
        val hasHistory = resets > 0 && avgDays > 0
        val isHarmonic = hasHistory && days == avgDays.toLong()
        val isTranscended = hasHistory && days > avgDays.toLong()
        val deltaDays = if (hasHistory) abs(days - avgDays.toLong()) else 0L
        val palette = WidgetThemeTokens.resolveWithItem(item, isDarkMode = isDarkMode)
        val tag = item.resolveOneWordLabel(customTag)

        return ZenOrbitViewState(
            itemId = item.id,
            title = item.name,
            oneWordLabel = tag,
            daysCount = days,
            averageDays = avgDays,
            resetCount = resets,
            hasHistory = hasHistory,
            isHarmonic = isHarmonic,
            isTranscended = isTranscended,
            deltaDays = deltaDays,
            palette = palette,
            isFuture = isFuture,
        )
    }

    /**
     * Resolves the immutable view state for the Tsukimi (月相之镜 · 2x2) widget.
     */
    fun resolveTsukimiState(
        item: CountUpItem,
        today: LocalDate,
        isDarkMode: Boolean = false,
        customTag: String? = null,
    ): TsukimiViewState {
        val rawDays = today.toEpochDay() - item.epochDay
        val isFuture = rawDays < 0
        val days = abs(rawDays)
        val palette = WidgetThemeTokens.resolveWithItem(item, isDarkMode = isDarkMode)
        val tag = item.resolveOneWordLabel(customTag)
        val lunarState = TsukimiMoonCalculator.calculate(today)
        val phaseVerse = TsukimiMoonCalculator.formatBilingualVerse(lunarState)

        return TsukimiViewState(
            itemId = item.id,
            title = item.name,
            oneWordLabel = tag,
            daysCount = days,
            lunarState = lunarState,
            phaseVerse = phaseVerse,
            palette = palette,
            isFuture = isFuture,
        )
    }

    /**
     * Resolves the immutable view state for the Shuin (金石朱印 · 1x1) cinnabar seal widget.
     */
    fun resolveShuinState(
        item: CountUpItem,
        today: LocalDate,
        isDarkMode: Boolean = false,
        customTag: String? = null,
    ): ShuinViewState {
        val rawDays = today.toEpochDay() - item.epochDay
        val isFuture = rawDays < 0
        val days = abs(rawDays)
        val palette = WidgetThemeTokens.resolveWithItem(item, isDarkMode = isDarkMode)
        val tag = item.resolveOneWordLabel(customTag)
        val compactText = formatCompactNumber(days)

        return ShuinViewState(
            itemId = item.id,
            title = item.name,
            oneWordLabel = tag,
            daysCount = days,
            compactNumberText = compactText,
            palette = palette,
            isFuture = isFuture,
        )
    }
}

/**
 * Immutable view state for the Zen Orbit (律动之环 · 温润玉环) widget dial.
 */
@Immutable
data class ZenOrbitViewState(
    val itemId: String,
    val title: String,
    val oneWordLabel: String,
    val daysCount: Long,
    val averageDays: Int,
    val resetCount: Int,
    val hasHistory: Boolean,
    val isHarmonic: Boolean,
    val isTranscended: Boolean,
    val deltaDays: Long,
    val palette: WidgetColorPalette,
    val isFuture: Boolean = false,
)

/**
 * Immutable view state for the Tsukimi (月相之镜 · 2x2) synodic moon widget.
 */
@Immutable
data class TsukimiViewState(
    val itemId: String,
    val title: String,
    val oneWordLabel: String,
    val daysCount: Long,
    val lunarState: TsukimiMoonCalculator.LunarState,
    val phaseVerse: String,
    val palette: WidgetColorPalette,
    val isFuture: Boolean = false,
)

/**
 * Immutable view state for the Shuin (金石朱印 · 1x1) cinnabar seal widget.
 */
@Immutable
data class ShuinViewState(
    val itemId: String,
    val title: String,
    val oneWordLabel: String,
    val daysCount: Long,
    val compactNumberText: String,
    val palette: WidgetColorPalette,
    val isFuture: Boolean = false,
)


