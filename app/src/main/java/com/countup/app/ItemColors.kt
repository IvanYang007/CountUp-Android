package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Mid-Century Modern & Zen Card & Icon Badge Combination Presets.
 *
 * Each preset couples a grounding surface (Paper White, Willow Sage, or Sumi Ink)
 * with a harmonious, high-contrast icon circle badge, text ink, and borders.
 */
@Immutable
data class CardColorPreset(
    val id: String,
    @get:StringRes val nameRes: Int,
    val cardBg: Color,
    val badgeBg: Color,
    val badgeTint: Color,
    val primaryInk: Color,
    val mutedInk: Color,
    val isDark: Boolean = false,
)

/** The default card preset ID (Classic Paper White with Willow Sage icon badge). */
const val DEFAULT_CARD_COLOR: String = ""

/**
 * The 3 curated Paper White card presets (Sage, Terracotta, Indigo badges)
 * used for initial random selection when creating a new item.
 */
val DEFAULT_WHITE_CARD_COLOR_IDS: List<String> = listOf(
    DEFAULT_CARD_COLOR,
    "paper_terracotta",
    "paper_indigo",
)

/**
 * Returns a randomly selected Paper White card preset ID from the 3 curated options.
 */
fun randomWhiteCardColor(): String = DEFAULT_WHITE_CARD_COLOR_IDS.random()

/**
 * 8 Curated Zen combinations:
 * - 3 Paper White variations (Sage, Terracotta, Indigo badges)
 * - 2 Willow Sage variations (Forest, Ochre badges)
 * - 3 Sumi Ink variations (Gold, Jade, Crimson badges)
 */
val CARD_COLOR_PRESETS: List<CardColorPreset> = listOf(
    // 1. Paper White + Sage badge (Default)
    CardColorPreset(
        id = "",
        nameRes = R.string.color_paper_sage,
        cardBg = Color(0xFFFFFFFF),
        badgeBg = Color(0xFF5E8C6D),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 2. Paper White + Terracotta badge
    CardColorPreset(
        id = "paper_terracotta",
        nameRes = R.string.color_paper_terracotta,
        cardBg = Color(0xFFFAFAF7),
        badgeBg = Color(0xFFD87A4F),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 3. Paper White + Slate Indigo badge
    CardColorPreset(
        id = "paper_indigo",
        nameRes = R.string.color_paper_indigo,
        cardBg = Color(0xFFFFFFFF),
        badgeBg = Color(0xFF5A7B8C),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 4. Willow Sage + Deep Forest badge
    CardColorPreset(
        id = "sage_forest",
        nameRes = R.string.color_sage_forest,
        cardBg = Color(0xFF5E8C6D),
        badgeBg = Color(0xFF33523D),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6E4DB),
        isDark = true,
    ),
    // 5. Willow Sage + Ochre Gold badge
    CardColorPreset(
        id = "sage_ochre",
        nameRes = R.string.color_sage_ochre,
        cardBg = Color(0xFF5E8C6D),
        badgeBg = Color(0xFFDEB285),
        badgeTint = ZenInkBlack,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6E4DB),
        isDark = true,
    ),
    // 6. Sumi Ink + Ochre Gold badge
    CardColorPreset(
        id = "ink_gold",
        nameRes = R.string.color_ink_gold,
        cardBg = Color(0xFF24201A),
        badgeBg = Color(0xFFDEB285),
        badgeTint = ZenInkBlack,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 7. Sumi Ink + Jade Green badge
    CardColorPreset(
        id = "ink_jade",
        nameRes = R.string.color_ink_jade,
        cardBg = Color(0xFF24201A),
        badgeBg = Color(0xFF4D7A58),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 8. Sumi Ink + Japanese Vermilion badge
    CardColorPreset(
        id = "ink_crimson",
        nameRes = R.string.color_ink_crimson,
        cardBg = Color(0xFF24201A),
        badgeBg = Color(0xFFC45249),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
)

private val PRESET_MAP: Map<String, CardColorPreset> = CARD_COLOR_PRESETS.associateBy { it.id }

/**
 * Legacy preset ID compatibility resolver.
 */
private val LEGACY_ID_MAP: Map<String, String> = mapOf(
    "willow_sage" to "sage_forest",
    "deep_ink" to "ink_gold",
    "terracotta" to "paper_terracotta",
    "dusty_indigo" to "paper_indigo",
    "ochre_gold" to "ink_gold",
    "rose_clay" to "ink_crimson",
    "warm_sand" to "paper_terracotta",
    "paper_white" to "",
)

/**
 * Resolves a [colorId] (current or legacy) to a full [CardColorPreset].
 */
fun resolveCardStyle(colorId: String?): CardColorPreset {
    if (colorId.isNullOrBlank()) return CARD_COLOR_PRESETS[0]
    val exact = PRESET_MAP[colorId]
    if (exact != null) return exact
    val legacyTarget = LEGACY_ID_MAP[colorId]
    if (legacyTarget != null) return PRESET_MAP[legacyTarget] ?: CARD_COLOR_PRESETS[0]
    return CARD_COLOR_PRESETS[0]
}

/**
 * Resolves a stored [colorId] into a solid card background [Color].
 */
fun cardBackgroundColor(colorId: String?): Color {
    if (colorId.isNullOrBlank()) return CARD_COLOR_PRESETS[0].cardBg
    val exact = PRESET_MAP[colorId]
    if (exact != null) return exact.cardBg
    val legacyTarget = LEGACY_ID_MAP[colorId]
    if (legacyTarget != null) return PRESET_MAP[legacyTarget]?.cardBg ?: CARD_COLOR_PRESETS[0].cardBg
    return try {
        val clean = colorId.trim().removePrefix("#")
        if (clean.length == 6 || clean.length == 8) {
            val longVal = clean.toLong(16)
            if (clean.length == 6) Color(longVal or 0xFF000000) else Color(longVal)
        } else {
            CARD_COLOR_PRESETS[0].cardBg
        }
    } catch (_: Exception) {
        CARD_COLOR_PRESETS[0].cardBg
    }
}

/**
 * Evaluates whether [backgroundColor] is dark, using WCAG perceived luminance.
 */
fun isDarkCardBackground(backgroundColor: Color): Boolean = backgroundColor.luminance() < 0.38f

/**
 * Returns high-contrast primary text/number ink for the given [backgroundColor].
 */
fun cardPrimaryInk(backgroundColor: Color): Color =
    if (isDarkCardBackground(backgroundColor)) Color(0xFFFAF7F2) else ZenInkBlack

/**
 * Returns high-contrast secondary/muted text ink for the given [backgroundColor].
 */
fun cardMutedInk(backgroundColor: Color): Color =
    if (isDarkCardBackground(backgroundColor)) Color(0xFFD6C8B7) else ZenInkMuted

/**
 * Returns a high-contrast card border brush/tint for the given [backgroundColor].
 */
fun cardBorderColor(backgroundColor: Color): Color =
    if (isDarkCardBackground(backgroundColor)) Color(0x33FFFFFF) else Color(0x242C2416)

/**
 * Computes an icon badge container color that complements the [backgroundColor] / [colorId].
 */
fun cardBadgeColor(colorId: String?, itemId: String = "", backgroundColor: Color = Color.White): Color =
    resolveCardStyle(colorId).badgeBg

/**
 * Computes the icon badge tint (foreground glyph color) based on badge container color.
 */
fun cardBadgeTint(badgeColor: Color, cardBackgroundColor: Color = Color.White): Color =
    if (badgeColor.luminance() > 0.40f) ZenInkBlack else Color.White
