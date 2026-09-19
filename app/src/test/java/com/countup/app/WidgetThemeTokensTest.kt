package com.countup.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetThemeTokensTest {

    private fun contrastRatio(fg: Int, bg: Int): Float {
        val lum1 = Color(fg).luminance()
        val lum2 = Color(bg).luminance()
        val maxLum = maxOf(lum1, lum2)
        val minLum = minOf(lum1, lum2)
        return (maxLum + 0.05f) / (minLum + 0.05f)
    }

    @Test
    fun `light palette matches xuan paper and patina design tokens`() {
        val light = WidgetThemeTokens.resolve(isDarkMode = false)

        assertEquals(0xFFFFFFFF.toInt(), light.canvasBg)
        assertEquals(0xFFFAF5EE.toInt(), light.surfaceBg)
        assertEquals(0xFF2C2416.toInt(), light.primaryInk)
        assertEquals(0xFF6B5D4F.toInt(), light.secondaryInk)
        assertEquals(0xFFE3D3B8.toInt(), light.hairline)
        assertEquals(0xFF46664B.toInt(), light.accentPrimary)
        assertEquals(0xFFDEB285.toInt(), light.accentGold)
        assertEquals(0xFFD6A848.toInt(), light.accentKintsugi)
    }

    @Test
    fun `dark palette matches forest charcoal and ink stone design tokens`() {
        val dark = WidgetThemeTokens.resolve(isDarkMode = true)

        assertEquals(WidgetThemeTokens.DARK_CANVAS_BG, dark.canvasBg)
        assertEquals(WidgetThemeTokens.DARK_SURFACE_BG, dark.surfaceBg)
        assertEquals(WidgetThemeTokens.DARK_PRIMARY_INK, dark.primaryInk)
        assertEquals(WidgetThemeTokens.DARK_SECONDARY_INK, dark.secondaryInk)
        assertEquals(WidgetThemeTokens.DARK_HAIRLINE, dark.hairline)
        assertEquals(0xFF8FAF84.toInt(), dark.accentPrimary)
        assertEquals(0xFFDEB285.toInt(), dark.accentGold)
        assertEquals(0xFFD6A848.toInt(), dark.accentKintsugi)
    }

    @Test
    fun `text tokens satisfy WCAG AA contrast ratio against canvas backgrounds`() {
        val light = WidgetThemeTokens.Light
        val lightContrast = contrastRatio(light.primaryInk, light.canvasBg)
        // WCAG AA requires at least 4.5:1 for normal text
        assertTrue("Light primary ink contrast should exceed 4.5:1, was $lightContrast", lightContrast >= 4.5)

        val dark = WidgetThemeTokens.Dark
        val darkContrast = contrastRatio(dark.primaryInk, dark.canvasBg)
        assertTrue("Dark primary ink contrast should exceed 4.5:1, was $darkContrast", darkContrast >= 4.5)
    }

    @Test
    fun `accent gold tokens match PatinaPigments exact values`() {
        assertEquals(0xFFDEB285.toInt(), WidgetThemeTokens.LIGHT_ACCENT_GOLD)
        assertEquals(0xFFD6A848.toInt(), WidgetThemeTokens.LIGHT_ACCENT_KINTSUGI)
    }
}

