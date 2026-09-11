package com.countup.app

import androidx.annotation.ColorInt
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Curated palette tokens for CountUp home-screen widgets, harmonized with
 * prototype_zen_card_styles.html, prototype_darkmode_strategy.html, and PatinaPigments.
 */
@Immutable
data class WidgetColorPalette(
    @field:ColorInt val canvasBg: Int,
    @field:ColorInt val surfaceBg: Int,
    @field:ColorInt val primaryInk: Int,
    @field:ColorInt val secondaryInk: Int,
    @field:ColorInt val hairline: Int,
    @field:ColorInt val accentPrimary: Int,
    @field:ColorInt val accentGold: Int,
    @field:ColorInt val accentKintsugi: Int,
    @field:ColorInt val microDivider: Int = secondaryInk,
)

object WidgetThemeTokens {

    // --- Light Theme: Paper White Palette matching App Card ---
    const val LIGHT_CANVAS_BG = 0xFFFFFFFF.toInt()   // Clean Paper White
    const val LIGHT_SURFACE_BG = 0xFFFAF5EE.toInt()  // Subtle washi tint
    const val LIGHT_PRIMARY_INK = 0xFF2C2416.toInt() // Deep ink black
    const val LIGHT_SECONDARY_INK = 0xFF6B5D4F.toInt() // Softened tea ink
    const val LIGHT_HAIRLINE = 0xFFE3D3B8.toInt()     // Subtle paper crease hairline
    const val LIGHT_ACCENT_PRIMARY = 0xFF46664B.toInt() // Bamboo forest jade
    const val LIGHT_ACCENT_GOLD = 0xFFDEB285.toInt()    // Filament Gold (PatinaPigments.GoldLight)
    const val LIGHT_ACCENT_KINTSUGI = 0xFFD6A848.toInt() // Kintsugi Gold (PatinaPigments.Gold)
    const val LIGHT_MILESTONE_ACCENT = 0xFFC2410C.toInt() // Radiant persimmon milestone dot

    // --- Dark Theme: Twilight Sumi Stone Palette matching App Dark Card ---
    const val DARK_CANVAS_BG = 0xFF26231E.toInt()    // Aligns with ZenDarkCard / Washi Dark Card (#26231E)
    const val DARK_SURFACE_BG = 0xFF232620.toInt()   // Aligns with ZenDarkSurface (#232620)
    const val DARK_PRIMARY_INK = 0xFFF4EFE6.toInt()  // Aligns with ZenDarkTextPrimary (#F4EFE6)
    const val DARK_SECONDARY_INK = 0xFFABA494.toInt() // Aligns with ZenDarkTextSecondary (#ABA494)
    const val DARK_HAIRLINE = 0xFF3A3D35.toInt()     // Dark slate hairline (#3A3D35)
    const val DARK_ACCENT_PRIMARY = 0xFF8FAF84.toInt() // Tea moss green
    const val DARK_ACCENT_GOLD = 0xFFDEB285.toInt()    // Filament Gold
    const val DARK_ACCENT_KINTSUGI = 0xFFD6A848.toInt() // Kintsugi Gold
    const val DARK_MILESTONE_ACCENT = 0xFFC88D58.toInt() // Patina bronze milestone dot

    // --- Micro-Divider Hairline Tokens ---
    const val MICRO_DIVIDER_LIGHT = LIGHT_SECONDARY_INK
    const val MICRO_DIVIDER_DARK = 0xFF8E8A7E.toInt()

    val Light = WidgetColorPalette(
        canvasBg = LIGHT_CANVAS_BG,
        surfaceBg = LIGHT_SURFACE_BG,
        primaryInk = LIGHT_PRIMARY_INK,
        secondaryInk = LIGHT_SECONDARY_INK,
        hairline = LIGHT_HAIRLINE,
        accentPrimary = LIGHT_ACCENT_PRIMARY,
        accentGold = LIGHT_ACCENT_GOLD,
        accentKintsugi = LIGHT_ACCENT_KINTSUGI,
        microDivider = MICRO_DIVIDER_LIGHT,
    )

    val Dark = WidgetColorPalette(
        canvasBg = DARK_CANVAS_BG,
        surfaceBg = DARK_SURFACE_BG,
        primaryInk = DARK_PRIMARY_INK,
        secondaryInk = DARK_SECONDARY_INK,
        hairline = DARK_HAIRLINE,
        accentPrimary = DARK_ACCENT_PRIMARY,
        accentGold = DARK_ACCENT_GOLD,
        accentKintsugi = DARK_ACCENT_KINTSUGI,
        microDivider = MICRO_DIVIDER_DARK,
    )

    /** Resolves the active widget palette based on system or user dark mode flag. */
    fun resolve(isDarkMode: Boolean): WidgetColorPalette = if (isDarkMode) Dark else Light

    /**
     * Resolves the widget palette harmonized with an optional [CountUpItem]'s custom card color.
     */
    fun resolveWithItem(item: CountUpItem?, isDarkMode: Boolean): WidgetColorPalette {
        val base = resolve(isDarkMode)
        if (item == null) return base
        val cardStyle = resolveCardStyle(item.cardColor, isDark = isDarkMode)
        return base.copy(
            canvasBg = cardStyle.cardBg.toArgb(),
            primaryInk = cardStyle.primaryInk.toArgb(),
            secondaryInk = cardStyle.mutedInk.toArgb(),
        )
    }

    /**
     * Calculates the WCAG 2.1 relative luminance for an ARGB color integer.
     * https://www.w3.org/WAI/GL/wiki/Relative_luminance
     */
    fun relativeLuminance(@ColorInt color: Int): Double {
        val r = sRgbToLinear(((color shr 16) and 0xFF) / 255.0)
        val g = sRgbToLinear(((color shr 8) and 0xFF) / 255.0)
        val b = sRgbToLinear((color and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun sRgbToLinear(channel: Double): Double {
        return if (channel <= 0.03928) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    /**
     * Calculates the WCAG contrast ratio between two colors (range: 1.0 to 21.0).
     */
    fun contrastRatio(@ColorInt foreground: Int, @ColorInt background: Int): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val brighter = max(l1, l2)
        val darker = min(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }
}
