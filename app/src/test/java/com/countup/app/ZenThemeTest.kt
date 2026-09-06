package com.countup.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Validates ZenTheme, ZenColorScheme semantic tokens, WCAG 2.1 contrast ratios,
 * and immutable light mode preservation.
 */
class ZenThemeTest {

    private fun contrastRatio(fg: Color, bg: Color): Float {
        val l1 = fg.luminance()
        val l2 = bg.luminance()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun `lightZenColors maintains 100 percent exact original colors without regression`() {
        val light = lightZenColors()

        assertFalse(light.isDark)
        assertEquals(ZenPaperBackground, light.paperBackground)
        assertEquals(Color(0xFFF5E6D3), light.paperBackground)

        assertEquals(ZenPaperSurface, light.paperSurface)
        assertEquals(Color(0xFFEBDCC3), light.paperSurface)

        assertEquals(ZenPaperCard, light.paperCard)
        assertEquals(Color(0xFFFFFFFF), light.paperCard)

        assertEquals(ZenInkBlack, light.inkBlack)
        assertEquals(Color(0xFF2C2416), light.inkBlack)

        assertEquals(ZenInkMuted, light.inkMuted)
        assertEquals(Color(0xFF6B5D4F), light.inkMuted)

        assertEquals(ZenHairlineRule, light.hairlineRule)
        assertEquals(Color(0xFFE3D3B8), light.hairlineRule)

        assertEquals(ZenHairlineRuleVariant, light.hairlineRuleVariant)
        assertEquals(Color(0xFFD9C6A6), light.hairlineRuleVariant)

        assertEquals(ZenVermilion, light.cinnabarVermilion)
        assertEquals(Color(0xFFD97642), light.cinnabarVermilion)

        assertEquals(ZenSage, light.willowSage)
        assertEquals(Color(0xFF4A7C59), light.willowSage)

        assertEquals(ZenOchre, light.ochreGold)
        assertEquals(Color(0xFFD4A574), light.ochreGold)

        assertEquals(ZenIndigo, light.dustyIndigo)
        assertEquals(Color(0xFF7D9BA8), light.dustyIndigo)

        assertEquals(ZenError, light.errorCrimson)
        assertEquals(Color(0xFFA64942), light.errorCrimson)

        assertEquals(ZenSealInk, light.sealInk)
        assertEquals(Color(0xFF5A4D41), light.sealInk)
    }

    @Test
    fun `darkZenColors contains specified warm twilight charcoal palette`() {
        val dark = darkZenColors()

        assertTrue(dark.isDark)
        assertEquals(Color(0xFF191B17), dark.paperBackground)
        assertEquals(Color(0xFF252922), dark.paperSurface)
        assertEquals(Color(0xFF24201A), dark.paperCard)
        assertEquals(Color(0xFFF1EBDD), dark.inkBlack)
        assertEquals(Color(0xFFC4BEAE), dark.inkMuted)
        assertEquals(Color(0xFF8E8A7E), dark.inkSubtle)
        assertEquals(Color(0xFF3A3D35), dark.hairlineRule)
        assertEquals(Color(0xFF4A4D44), dark.hairlineRuleVariant)
        assertEquals(Color(0xFFE5A36F), dark.cinnabarVermilion)
        assertEquals(Color(0xFF8FAF84), dark.willowSage)
        assertEquals(Color(0xFFD4A574), dark.ochreGold)
        assertEquals(Color(0xFFA8C4D0), dark.dustyIndigo)
        assertEquals(Color(0xFFCF6E67), dark.errorCrimson)
        assertEquals(Color(0xFFC4BEAE), dark.sealInk)
    }

    @Test
    fun `primary text achieves WCAG AAA contrast in both light and dark modes`() {
        val light = lightZenColors()
        val dark = darkZenColors()

        // Light mode: soot ink on washi paper >= 7.0:1
        val lightRatio = contrastRatio(light.inkBlack, light.paperBackground)
        assertTrue("Light primary text ratio ($lightRatio) must be >= 7.0 (AAA)", lightRatio >= 7.0f)

        // Dark mode: aged parchment on charcoal slate >= 7.0:1
        val darkRatio = contrastRatio(dark.inkBlack, dark.paperBackground)
        assertTrue("Dark primary text ratio ($darkRatio) must be >= 7.0 (AAA)", darkRatio >= 7.0f)
    }

    @Test
    fun `secondary text achieves WCAG AA contrast in both light and dark modes`() {
        val light = lightZenColors()
        val dark = darkZenColors()

        // Light mode: muted ink on paper surface >= 4.5:1
        val lightSurfaceRatio = contrastRatio(light.inkMuted, light.paperSurface)
        assertTrue("Light secondary text ratio ($lightSurfaceRatio) must be >= 4.5 (AA)", lightSurfaceRatio >= 4.5f)

        // Dark mode: driftwood sand on deep moss surface >= 4.5:1
        val darkSurfaceRatio = contrastRatio(dark.inkMuted, dark.paperSurface)
        assertTrue("Dark secondary text ratio ($darkSurfaceRatio) must be >= 4.5 (AA)", darkSurfaceRatio >= 4.5f)
    }

    @Test
    fun `primary action maintains high contrast against onPrimary in dark mode`() {
        val dark = darkZenColors()
        val actionRatio = contrastRatio(ZenDarkOnPrimary, dark.willowSage)
        assertTrue("Dark onPrimary ratio ($actionRatio) on action button must be >= 4.5", actionRatio >= 4.5f)
    }

    @Test
    fun `seasonal pigments resolve correctly in both light and dark modes`() {
        val light = lightZenColors()
        val dark = darkZenColors()

        // Spring
        assertEquals(light.willowSage, getSeasonColor(R.string.season_spring, light))
        assertEquals(dark.willowSage, getSeasonColor(R.string.season_spring, dark))
        assertEquals(Color(0xFF8FAF84), getSeasonColor(R.string.season_spring, dark))

        // Summer
        assertEquals(light.ochreGold, getSeasonColor(R.string.season_summer, light))
        assertEquals(dark.ochreGold, getSeasonColor(R.string.season_summer, dark))
        assertEquals(Color(0xFFD4A574), getSeasonColor(R.string.season_summer, dark))

        // Autumn
        assertEquals(light.cinnabarVermilion, getSeasonColor(R.string.season_autumn, light))
        assertEquals(dark.cinnabarVermilion, getSeasonColor(R.string.season_autumn, dark))
        assertEquals(Color(0xFFE5A36F), getSeasonColor(R.string.season_autumn, dark))

        // Winter
        assertEquals(light.dustyIndigo, getSeasonColor(R.string.season_winter, light))
        assertEquals(dark.dustyIndigo, getSeasonColor(R.string.season_winter, dark))
        assertEquals(Color(0xFFA8C4D0), getSeasonColor(R.string.season_winter, dark))
    }
}
