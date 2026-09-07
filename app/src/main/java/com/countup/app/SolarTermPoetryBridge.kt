package com.countup.app

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Palette tokens representing the natural color temperature of the four seasons.
 */
@Immutable
data class SeasonalPalette(
    @field:ColorInt val badgeBg: Int,
    @field:ColorInt val badgeTextColor: Int,
    @field:ColorInt val primaryTint: Int,
    @field:ColorInt val progressTint: Int,
)

/**
 * Formatted snapshot of solar term metadata and seasonal styling for widgets.
 */
@Immutable
data class SolarTermWidgetDisplay(
    val termId: Int,
    @get:StringRes val nameRes: Int,
    @get:StringRes val seasonRes: Int,
    @get:StringRes val line1Res: Int,
    @get:StringRes val line2Res: Int,
    val palette: SeasonalPalette,
)

object SolarTermPoetryBridge {

    // --- Light Palette: Xuan Paper tints ---
    private val SPRING_LIGHT = SeasonalPalette(
        badgeBg = 0xFFEBF2ED.toInt(),
        badgeTextColor = 0xFF2E5137.toInt(),
        primaryTint = 0xFF4A6B53.toInt(), // Bamboo jade
        progressTint = 0xFF4A7C59.toInt(),
    )
    private val SUMMER_LIGHT = SeasonalPalette(
        badgeBg = 0xFFFBF4EB.toInt(),
        badgeTextColor = 0xFF7A4E1D.toInt(),
        primaryTint = 0xFF9E652B.toInt(), // Summer amber
        progressTint = 0xFFD4A574.toInt(),
    )
    private val AUTUMN_LIGHT = SeasonalPalette(
        badgeBg = 0xFFF4ECE4.toInt(),
        badgeTextColor = 0xFF6A3E1E.toInt(),
        primaryTint = 0xFF8A5A36.toInt(), // Autumn tea ochre
        progressTint = 0xFFD97642.toInt(),
    )
    private val WINTER_LIGHT = SeasonalPalette(
        badgeBg = 0xFFECF1F7.toInt(),
        badgeTextColor = 0xFF2B3D54.toInt(),
        primaryTint = 0xFF465A73.toInt(), // Indigo slate
        progressTint = 0xFF7D9BA8.toInt(),
    )

    // --- Dark Palette: Ink Stone tints ---
    private val SPRING_DARK = SeasonalPalette(
        badgeBg = 0xFF1C2820.toInt(),
        badgeTextColor = 0xFFB4DEC0.toInt(),
        primaryTint = 0xFF97C4A3.toInt(),
        progressTint = 0xFF8FAF84.toInt(),
    )
    private val SUMMER_DARK = SeasonalPalette(
        badgeBg = 0xFF2B2217.toInt(),
        badgeTextColor = 0xFFF2D1A8.toInt(),
        primaryTint = 0xFFE5B585.toInt(),
        progressTint = 0xFFDEB285.toInt(),
    )
    private val AUTUMN_DARK = SeasonalPalette(
        badgeBg = 0xFF2D221B.toInt(),
        badgeTextColor = 0xFFE8B99E.toInt(),
        primaryTint = 0xFFD69F7E.toInt(),
        progressTint = 0xFFE5A36F.toInt(),
    )
    private val WINTER_DARK = SeasonalPalette(
        badgeBg = 0xFF1B232C.toInt(),
        badgeTextColor = 0xFFC4D6ED.toInt(),
        primaryTint = 0xFF99B2D1.toInt(),
        progressTint = 0xFF8B9484.toInt(),
    )

    /**
     * Resolves the seasonal palette based on [seasonRes] and dark mode state.
     */
    fun resolveSeasonalPalette(@StringRes seasonRes: Int, isDarkMode: Boolean): SeasonalPalette {
        return if (isDarkMode) {
            when (seasonRes) {
                R.string.season_spring -> SPRING_DARK
                R.string.season_summer -> SUMMER_DARK
                R.string.season_autumn -> AUTUMN_DARK
                R.string.season_winter -> WINTER_DARK
                else -> AUTUMN_DARK
            }
        } else {
            when (seasonRes) {
                R.string.season_spring -> SPRING_LIGHT
                R.string.season_summer -> SUMMER_LIGHT
                R.string.season_autumn -> AUTUMN_LIGHT
                R.string.season_winter -> WINTER_LIGHT
                else -> AUTUMN_LIGHT
            }
        }
    }

    /**
     * Builds the complete display snapshot for a [SolarTerm].
     */
    fun resolveWidgetDisplay(term: SolarTerm, isDarkMode: Boolean): SolarTermWidgetDisplay {
        val palette = resolveSeasonalPalette(term.seasonRes, isDarkMode)
        return SolarTermWidgetDisplay(
            termId = term.id,
            nameRes = term.nameRes,
            seasonRes = term.seasonRes,
            line1Res = term.line1Res,
            line2Res = term.line2Res,
            palette = palette,
        )
    }
}

