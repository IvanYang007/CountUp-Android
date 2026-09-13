package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
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

/**
 * Card Style Category for grouping presets into tactile Zen themes (Washi, Earth, Sumi).
 */
@Immutable
data class CardColorCategory(
    val id: String,
    @get:StringRes val labelRes: Int,
    val presetIds: List<String>,
)

/** The default card preset ID (Classic Paper White with Ochre Gold / 沉金 badge). */
const val DEFAULT_CARD_COLOR: String = ""

/**
 * 3 Curated Zen Categories:
 * - Washi (宣纸): 4 Light Paper variations (Gold, Sage, Terracotta, Lapis)
 * - Earth (泥陶): 4 Organic Nature variations (Celadon Bamboo, Linen Sandalwood, Forest Sage, Ochre Sage)
 * - Sumi (夜墨): 4 Deep Contemplative variations (Gold, Jade, Crimson, Night Mist)
 */
val CARD_COLOR_CATEGORIES: List<CardColorCategory> = listOf(
    CardColorCategory(
        id = "washi",
        labelRes = R.string.color_category_washi,
        presetIds = listOf(DEFAULT_CARD_COLOR, "paper_sage", "paper_terracotta", "paper_indigo"),
    ),
    CardColorCategory(
        id = "earth",
        labelRes = R.string.color_category_earth,
        presetIds = listOf("celadon_bamboo", "linen_sandalwood", "sage_forest", "sage_ochre"),
    ),
    CardColorCategory(
        id = "sumi",
        labelRes = R.string.color_category_sumi,
        presetIds = listOf("ink_gold", "ink_jade", "ink_crimson", "night_mist"),
    ),
)

/**
 * The 3 curated Paper White card presets (Gold, Terracotta, Sage badges)
 * used for initial random selection when creating a new item.
 */
val DEFAULT_WHITE_CARD_COLOR_IDS: List<String> = listOf(
    DEFAULT_CARD_COLOR,
    "paper_terracotta",
    "paper_sage",
)

/**
 * Returns a randomly selected Paper White card preset ID from the 3 curated options.
 */
fun randomWhiteCardColor(): String = DEFAULT_WHITE_CARD_COLOR_IDS.random()

/**
 * 12 Curated Zen combinations:
 * - 4 Washi variations (Gold, Sage, Terracotta, Mineral Lapis)
 * - 4 Earth variations (Celadon Bamboo, Linen Sandalwood, Forest, Ochre)
 * - 4 Sumi variations (Gold, Jade, Crimson, Night Mist)
 */
val CARD_COLOR_PRESETS: List<CardColorPreset> = listOf(
    // --- SUITE I: WASHI (宣纸) ---
    // 1. Paper White + Ochre Gold badge (Default - 宣白 · 沉金)
    CardColorPreset(
        id = "",
        nameRes = R.string.color_paper_gold,
        cardBg = Color(0xFFFFFFFF),
        badgeBg = Color(0xFFDEB285),
        badgeTint = ZenInkBlack,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 2. Paper White + Willow Sage badge (宣白 · 柳叶)
    CardColorPreset(
        id = "paper_sage",
        nameRes = R.string.color_paper_sage,
        cardBg = Color(0xFFFFFFFF),
        badgeBg = Color(0xFF5E8C6D),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 3. Paper White + Terracotta badge (素白 · 陶土)
    CardColorPreset(
        id = "paper_terracotta",
        nameRes = R.string.color_paper_terracotta,
        cardBg = Color(0xFFFBF9F5),
        badgeBg = Color(0xFFD87A4F),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 4. Paper White + Mineral Lapis badge (宣白 · 黛蓝)
    CardColorPreset(
        id = "paper_indigo",
        nameRes = R.string.color_paper_indigo,
        cardBg = Color(0xFFFFFFFF),
        badgeBg = Color(0xFF425B6C),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),

    // --- SUITE II: EARTH (泥陶) ---
    // 5. Celadon Mist + Deep Bamboo badge (天青 · 墨竹)
    CardColorPreset(
        id = "celadon_bamboo",
        nameRes = R.string.color_celadon_bamboo,
        cardBg = Color(0xFFE1E9E4),
        badgeBg = Color(0xFF3B5B46),
        badgeTint = Color.White,
        primaryInk = Color(0xFF203126),
        mutedInk = Color(0xFF55695C),
        isDark = false,
    ),
    // 6. Raw Linen + Sandalwood badge (素麻 · 白檀)
    CardColorPreset(
        id = "linen_sandalwood",
        nameRes = R.string.color_linen_sandalwood,
        cardBg = Color(0xFFECE4D5),
        badgeBg = Color(0xFF8A6B4E),
        badgeTint = Color.White,
        primaryInk = ZenInkBlack,
        mutedInk = ZenInkMuted,
        isDark = false,
    ),
    // 7. Willow Sage + Deep Forest badge (柳绿 · 幽森)
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
    // 8. Willow Sage + Ochre Gold badge (柳绿 · 赭石)
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

    // --- SUITE III: SUMI (夜墨) ---
    // 9. Sumi Ink + Pure Gold badge (墨黑 · 沉金)
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
    // 10. Sumi Ink + River Jade badge (墨黑 · 碧翠)
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
    // 11. Sumi Ink + Cinnabar Seal badge (墨黑 · 辰砂)
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
    // 12. Midnight Indigo + Silver Mist badge (暮夜 · 银霜)
    CardColorPreset(
        id = "night_mist",
        nameRes = R.string.color_night_mist,
        cardBg = Color(0xFF1F262E),
        badgeBg = Color(0xFF7A91A1),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFCCD7E0),
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
 * Dark Mode Default Card Style (Warm Amber Charcoal Washi #26231E + Ochre Gold badge #DEB285).
 */
private val DEFAULT_DARK_CARD_PRESET = CardColorPreset(
    id = "",
    nameRes = R.string.color_paper_gold,
    cardBg = Color(0xFF26231E),
    badgeBg = Color(0xFFDEB285),
    badgeTint = ZenInkBlack,
    primaryInk = ZenDarkTextPrimary,
    mutedInk = ZenDarkTextSecondary,
    isDark = true,
)

/**
 * 12 Curated Nocturnal Zen Presets ("Moonlight on Ancient Bricks" / 月映青砖 · 五墨六彩):
 * - Washi (宣纸四品): Charcoal washi slates reflecting subtle botanical & mineral paper fiber.
 * - Earth (泥陶四品): Rich ceramic glazes & aged timber (深瓷天青, 老檀沉木, 苍松夜黛, 岩壁幽苔).
 * - Sumi (夜墨四品): Deep obsidian lacquerware infused with mineral pigments (松烟, 凝翠, 辰砂, 宿墨).
 * All badge colors remain 100% identical between light and dark modes.
 */
val DARK_CARD_COLOR_PRESETS: List<CardColorPreset> = listOf(
    // --- SUITE I: WASHI (宣纸四品：月下纸韵，素笺生辉) ---
    // 1. 宣白 · 沉金 (Default)
    DEFAULT_DARK_CARD_PRESET,
    // 2. 宣白 · 柳叶
    CardColorPreset(
        id = "paper_sage",
        nameRes = R.string.color_paper_sage,
        cardBg = Color(0xFF232724),
        badgeBg = Color(0xFF68B285),
        badgeTint = Color.White,
        primaryInk = ZenDarkTextPrimary,
        mutedInk = ZenDarkTextSecondary,
        isDark = true,
    ),
    // 3. 素白 · 陶土
    CardColorPreset(
        id = "paper_terracotta",
        nameRes = R.string.color_paper_terracotta,
        cardBg = Color(0xFF2A2421),
        badgeBg = Color(0xFFE58356),
        badgeTint = Color.White,
        primaryInk = ZenDarkTextPrimary,
        mutedInk = ZenDarkTextSecondary,
        isDark = true,
    ),
    // 4. 宣白 · 黛蓝
    CardColorPreset(
        id = "paper_indigo",
        nameRes = R.string.color_paper_indigo,
        cardBg = Color(0xFF21252C),
        badgeBg = Color(0xFF6BA2C7),
        badgeTint = Color.White,
        primaryInk = ZenDarkTextPrimary,
        mutedInk = ZenDarkTextSecondary,
        isDark = true,
    ),

    // --- SUITE II: EARTH (泥陶四品：大地自然，温润如玉) ---
    // 5. 天青 · 墨竹 (Ru Ware Celadon Mist -> Moonlit Celadon Slate)
    CardColorPreset(
        id = "celadon_bamboo",
        nameRes = R.string.color_celadon_bamboo,
        cardBg = Color(0xFF26423A),
        badgeBg = Color(0xFF60A880),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6E4DB),
        isDark = true,
    ),
    // 6. 素麻 · 白檀 (Raw Linen & Sandalwood -> Smoked Sandalwood Earth)
    CardColorPreset(
        id = "linen_sandalwood",
        nameRes = R.string.color_linen_sandalwood,
        cardBg = Color(0xFF463625),
        badgeBg = Color(0xFFD1A16E),
        badgeTint = ZenInkBlack,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 7. 柳绿 · 幽森 (Willow Sage & Forest -> Deep Willow Sage Green)
    CardColorPreset(
        id = "sage_forest",
        nameRes = R.string.color_sage_forest,
        cardBg = Color(0xFF2E4D3A),
        badgeBg = Color(0xFF6DB88A),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6E4DB),
        isDark = true,
    ),
    // 8. 柳绿 · 赭石 (Willow Sage & Ochre -> Olive Tea Green)
    CardColorPreset(
        id = "sage_ochre",
        nameRes = R.string.color_sage_ochre,
        cardBg = Color(0xFF3E482A),
        badgeBg = Color(0xFFDEB285),
        badgeTint = ZenInkBlack,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6E4DB),
        isDark = true,
    ),

    // --- SUITE III: SUMI (夜墨四品：松烟焦墨，沉着深远) ---
    // 9. 墨黑 · 沉金 (Lampblack & Gold)
    CardColorPreset(
        id = "ink_gold",
        nameRes = R.string.color_ink_gold,
        cardBg = Color(0xFF191613),
        badgeBg = Color(0xFFDEB285),
        badgeTint = ZenInkBlack,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 10. 墨黑 · 碧翠 (Pine Obsidian)
    CardColorPreset(
        id = "ink_jade",
        nameRes = R.string.color_ink_jade,
        cardBg = Color(0xFF141A16),
        badgeBg = Color(0xFF54A874),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 11. 墨黑 · 辰砂 (Cinnabar Lacquer)
    CardColorPreset(
        id = "ink_crimson",
        nameRes = R.string.color_ink_crimson,
        cardBg = Color(0xFF1F1515),
        badgeBg = Color(0xFFDE584C),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFD6C8B7),
        isDark = true,
    ),
    // 12. 暮夜 · 银霜 (Midnight Indigo Obsidian)
    CardColorPreset(
        id = "night_mist",
        nameRes = R.string.color_night_mist,
        cardBg = Color(0xFF15202D),
        badgeBg = Color(0xFF8AB0C7),
        badgeTint = Color.White,
        primaryInk = Color(0xFFFAF7F2),
        mutedInk = Color(0xFFCCD7E0),
        isDark = true,
    ),
)

/**
 * Resolves an unobtrusive directional specular parting-line border for dark mode surfaces.
 * Natural ambient light falls from above:
 * - Top edge catches soft specular reflection (28% opacity of the badge accent or gold).
 * - Mid-body transitions smoothly to 14%.
 * - Bottom dissolves into the canvas (5% opacity).
 */
fun cardDarkBorderBrush(style: CardColorPreset): Brush {
    val accent = style.badgeBg
    return Brush.verticalGradient(
        0.0f to accent.copy(alpha = 0.28f),
        0.4f to accent.copy(alpha = 0.14f),
        1.0f to accent.copy(alpha = 0.05f),
    )
}

private val DARK_PRESET_MAP: Map<String, CardColorPreset> = DARK_CARD_COLOR_PRESETS.associateBy { it.id }

/**
 * Resolves a [colorId] (current or legacy) to a full [CardColorPreset].
 * In dark mode, card surfaces settle into night-calibrated tones,
 * while light mode colors remain 100% untouched.
 */
fun resolveCardStyle(colorId: String?, isDark: Boolean = false): CardColorPreset {
    val map = if (isDark) DARK_PRESET_MAP else PRESET_MAP
    val fallback = if (isDark) DEFAULT_DARK_CARD_PRESET else CARD_COLOR_PRESETS[0]
    if (colorId.isNullOrBlank()) return fallback
    val exact = map[colorId]
    if (exact != null) return exact
    val legacyTarget = LEGACY_ID_MAP[colorId]
    if (legacyTarget != null) return map[legacyTarget] ?: fallback
    return fallback
}

/**
 * Resolves a stored [colorId] into a solid card background [Color].
 */
fun cardBackgroundColor(colorId: String?, isDark: Boolean = false): Color {
    if (colorId.isNullOrBlank()) return if (isDark) ZenDarkCard else CARD_COLOR_PRESETS[0].cardBg
    val clean = colorId.trim().removePrefix("#")
    if (clean.length == 6 || clean.length == 8) {
        val longVal = clean.toLongOrNull(16)
        if (longVal != null) {
            return if (clean.length == 6) Color(longVal or 0xFF000000) else Color(longVal)
        }
    }
    return resolveCardStyle(colorId, isDark).cardBg
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
fun cardBorderColor(backgroundColor: Color, isDark: Boolean = false): Color =
    if (isDark) ZenDarkHairline else if (isDarkCardBackground(backgroundColor)) Color(0x33FFFFFF) else Color(0x242C2416)

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
