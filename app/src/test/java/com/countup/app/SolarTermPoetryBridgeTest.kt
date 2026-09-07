package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SolarTermPoetryBridgeTest {

    @Test
    fun `all 24 solar terms resolve non-zero string resources and valid palettes`() {
        assertEquals(24, SolarTermCalendar.TERMS.size)

        for (term in SolarTermCalendar.TERMS) {
            val lightDisplay = SolarTermPoetryBridge.resolveWidgetDisplay(term, isDarkMode = false)
            val darkDisplay = SolarTermPoetryBridge.resolveWidgetDisplay(term, isDarkMode = true)

            assertEquals(term.id, lightDisplay.termId)
            assertNotEquals(0, lightDisplay.nameRes)
            assertNotEquals(0, lightDisplay.seasonRes)
            assertNotEquals(0, lightDisplay.line1Res)
            assertNotEquals(0, lightDisplay.line2Res)

            assertNotNull(lightDisplay.palette)
            assertNotNull(darkDisplay.palette)
            assertNotEquals(lightDisplay.palette.primaryTint, darkDisplay.palette.primaryTint)
        }
    }

    @Test
    fun `seasonal palette maps unique color tints across all four seasons`() {
        val spring = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_spring, isDarkMode = false)
        val summer = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_summer, isDarkMode = false)
        val autumn = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_autumn, isDarkMode = false)
        val winter = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_winter, isDarkMode = false)

        val tints = setOf(spring.primaryTint, summer.primaryTint, autumn.primaryTint, winter.primaryTint)
        assertEquals(4, tints.size)
    }

    @Test
    fun `dark mode seasonal palettes provide higher luminance tints for readability`() {
        val springDark = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_spring, isDarkMode = true)
        val springLight = SolarTermPoetryBridge.resolveSeasonalPalette(R.string.season_spring, isDarkMode = false)

        // Dark mode primary tint (e.g. #97C4A3) has higher relative luminance than light mode tint (#4A6B53)
        val darkLuminance = WidgetThemeTokens.relativeLuminance(springDark.primaryTint)
        val lightLuminance = WidgetThemeTokens.relativeLuminance(springLight.primaryTint)

        org.junit.Assert.assertTrue(
            "Dark mode tint should be brighter on dark background",
            darkLuminance > lightLuminance
        )
    }
}

