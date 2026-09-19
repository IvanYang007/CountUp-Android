package com.countup.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Card Color Warmth Shift (Patina) mathematical and domain model.
 * Adheres to android-kotlin-testing standards for boundary, token, and contrast verification.
 */
class PatinaTest {

    @Test
    fun `calculateWarmth produces zero for non-positive days`() {
        assertEquals(0f, calculateWarmth(-100L), 0.0001f)
        assertEquals(0f, calculateWarmth(-10L), 0.0001f)
        assertEquals(0f, calculateWarmth(-1L), 0.0001f)
        assertEquals(0f, calculateWarmth(0L), 0.0001f)
    }

    @Test
    fun `calculateWarmth strictly increases monotonically up to day 180 and clamps at 1`() {
        var prevWarmth = 0f
        for (day in 1L..180L) {
            val warmth = calculateWarmth(day)
            assertTrue("Warmth on day $day ($warmth) should be >= day ${day - 1} ($prevWarmth)", warmth >= prevWarmth)
            assertTrue("Warmth on day $day should be <= 1.0", warmth <= 1.0001f)
            prevWarmth = warmth
        }

        assertEquals(1.0f, calculateWarmth(180L), 0.0001f)
        assertEquals(1.0f, calculateWarmth(365L), 0.0001f)
        assertEquals(1.0f, calculateWarmth(1000L), 0.0001f)
        assertEquals(1.0f, calculateWarmth(10000L), 0.0001f)
    }

    @Test
    fun `calculateWarmth follows concave power curve giving early perceptible reward`() {
        // At day 30 (1/6th of 180), warmth should be significantly higher than linear 1/6 (0.166)
        // (30/180)^0.72 = (0.1666)^0.72 ≈ 0.275
        val warmth30 = calculateWarmth(30L)
        assertTrue("Day 30 warmth should be greater than linear 0.166", warmth30 > 0.25f)
        assertTrue("Day 30 warmth should be less than 0.35", warmth30 < 0.35f)
    }

    @Test
    fun `resolvePatinaPhase correctly categorizes lifecycle phases across boundaries`() {
        assertEquals(PatinaPhase.FRESH_GROWTH, resolvePatinaPhase(-5L))
        assertEquals(PatinaPhase.FRESH_GROWTH, resolvePatinaPhase(0L))
        assertEquals(PatinaPhase.FRESH_GROWTH, resolvePatinaPhase(1L))
        assertEquals(PatinaPhase.FRESH_GROWTH, resolvePatinaPhase(14L))

        assertEquals(PatinaPhase.WARMING, resolvePatinaPhase(15L))
        assertEquals(PatinaPhase.WARMING, resolvePatinaPhase(29L))

        assertEquals(PatinaPhase.GROUNDED, resolvePatinaPhase(30L))
        assertEquals(PatinaPhase.GROUNDED, resolvePatinaPhase(89L))

        assertEquals(PatinaPhase.SEASONED, resolvePatinaPhase(90L))
        assertEquals(PatinaPhase.SEASONED, resolvePatinaPhase(179L))

        assertEquals(PatinaPhase.KINTSUGI, resolvePatinaPhase(180L))
        assertEquals(PatinaPhase.KINTSUGI, resolvePatinaPhase(365L))
        assertEquals(PatinaPhase.KINTSUGI, resolvePatinaPhase(5000L))
    }


    @Test
    fun `getPatinaColor hits exact milestone pigment coordinates`() {
        val goldLight = getPatinaColor(0f)
        assertEquals(PatinaPigments.GoldLight.red, goldLight.red, 0.001f)
        assertEquals(PatinaPigments.GoldLight.green, goldLight.green, 0.001f)
        assertEquals(PatinaPigments.GoldLight.blue, goldLight.blue, 0.001f)

        val sand = getPatinaColor(0.25f)
        assertEquals(PatinaPigments.Sand.red, sand.red, 0.001f)
        assertEquals(PatinaPigments.Sand.green, sand.green, 0.001f)
        assertEquals(PatinaPigments.Sand.blue, sand.blue, 0.001f)

        val ochre = getPatinaColor(0.75f)
        assertEquals(PatinaPigments.Ochre.red, ochre.red, 0.001f)
        assertEquals(PatinaPigments.Ochre.green, ochre.green, 0.001f)
        assertEquals(PatinaPigments.Ochre.blue, ochre.blue, 0.001f)

        val gold = getPatinaColor(1.0f)
        assertEquals(PatinaPigments.Gold.red, gold.red, 0.001f)
        assertEquals(PatinaPigments.Gold.green, gold.green, 0.001f)
        assertEquals(PatinaPigments.Gold.blue, gold.blue, 0.001f)
    }

    @Test
    fun `resolvePatina builds complete tokens with valid alpha and border brush`() {
        val testDays = listOf(-5L, 0L, 1L, 15L, 30L, 90L, 180L, 365L)
        for (day in testDays) {
            val patina = resolvePatina(day)
            assertEquals(day, patina.days)
            assertNotNull(patina.phase)
            assertNotNull(patina.borderBrush)
            assertTrue("Alpha for day $day (${patina.alpha}) must be in [0.28, 0.72]", patina.alpha in 0.279f..0.721f)
            assertTrue("Warmth for day $day (${patina.warmth}) must be in [0.0, 1.0]", patina.warmth in 0f..1f)
            assertTrue("Leading color alpha must be > 0", patina.leadingColor.alpha > 0f)
            assertTrue("Trailing color alpha must be > 0", patina.trailingColor.alpha > 0f)
        }
    }

    @Test
    fun `resolvePatina adapts badge tokens for theme contrast compliance`() {
        // Light theme check: badge text must use high-contrast ZenInkBlack (WCAG AAA > 7:1)
        val lightPatina = resolvePatina(30L, isDark = false)
        assertEquals(ZenInkBlack, lightPatina.badgeTextColor)
        assertEquals(0.14f, lightPatina.badgeBg.alpha, 0.01f)

        // Dark theme check: badge text uses luminous ZenWhite (WCAG AAA > 7:1)
        val darkPatina = resolvePatina(30L, isDark = true)
        assertEquals(ZenWhite, darkPatina.badgeTextColor)
        assertEquals(0.22f, darkPatina.badgeBg.alpha, 0.01f)
    }

    @Test
    fun `getPatinaPrimaryColor matches primary color in resolved tokens`() {
        val testDays = listOf(-5L, 0L, 10L, 30L, 100L, 180L, 365L)
        for (day in testDays) {
            val color = getPatinaPrimaryColor(day)
            val fullData = resolvePatina(day)
            assertEquals(fullData.primaryColor, color)
        }
    }

    @Test
    fun `patina phase string resource mappings exist for all phases`() {
        for (phase in PatinaPhase.entries) {
            assertTrue("Resource ID should be valid for $phase", phase.labelRes != 0)
        }
    }
}
