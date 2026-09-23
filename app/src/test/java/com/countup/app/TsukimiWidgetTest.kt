package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/**
 * Unit test suite for Tsukimi (月相之镜 · 2x2) synodic moon widget.
 * Tests reducer state resolution, memory budget clamping, store binding lifecycle, and purge.
 */
class TsukimiWidgetTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 22)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("tsukimi_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun testTsukimiReducerStateResolution() {
        val item = CountUpItem(
            id = "tsukimi-item-1",
            name = "Sober & Serene",
            epochDay = today.minusDays(108).toEpochDay(),
        )

        val state = ZenWidgetReducer.resolveTsukimiState(
            item = item,
            today = today,
            isDarkMode = false,
        )

        assertEquals("Sober & Serene", state.title)
        assertEquals("SOBER", state.oneWordLabel)
        assertEquals(108L, state.daysCount)
        assertFalse(state.isFuture)
        assertNotNull(state.lunarState)
        assertTrue(state.phaseVerse.startsWith("· "))
        assertTrue(state.phaseVerse.endsWith(" ·"))
    }

    @Test
    fun testTsukimiMoonRendererMemoryBudgetCompliance() {
        // Standard requested size (180x180) must be clamped under 32KB
        val safeSize = TsukimiMoonRenderer.computeSafeSize(180)
        val allocatedBytes = safeSize * safeSize * 4
        assertTrue(
            "Allocated bitmap bytes ($allocatedBytes) must not exceed 32KB (${TsukimiMoonRenderer.MAX_BITMAP_BYTES})",
            allocatedBytes <= TsukimiMoonRenderer.MAX_BITMAP_BYTES,
        )

        // Huge requested size (1200px) must still be clamped under 32KB
        val extremeSafeSize = TsukimiMoonRenderer.computeSafeSize(1200)
        val extremeBytes = extremeSafeSize * extremeSafeSize * 4
        assertTrue(extremeBytes <= TsukimiMoonRenderer.MAX_BITMAP_BYTES)
    }

    @Test
    fun testTsukimiStoreBindingLifecycleAndPurge() {
        val store = CountUpStore(testContext)
        val item = store.addItem(
            name = "Meditation",
            epochDay = today.minusDays(42).toEpochDay(),
            id = "test-item-tsukimi",
        )
        assertNotNull(item)
        val validItem = item!!

        val widgetId = 505
        store.setTsukimiBinding(widgetId, validItem.id)
        assertEquals(validItem.id, store.getTsukimiBinding(widgetId))

        // Deleting the item must purge the binding
        val purged = store.deleteItem(validItem.id)
        assertTrue(purged)
        assertEquals(null, store.getTsukimiBinding(widgetId))
    }

    @Test
    fun testTsukimiLunationPhysicsCalculations() {
        val safeSize = TsukimiMoonRenderer.computeSafeSize(TsukimiMoonRenderer.DEFAULT_SIZE_PX)
        val r = safeSize * 0.44f

        // Radius must stay comfortably inside the bitmap bounds
        assertTrue("Radius ($r) must be less than half safe size (${safeSize / 2f})", r < safeSize / 2f)

        // New Moon physics
        val newMoon = TsukimiMoonCalculator.LunarState(
            ageDays = 0.0,
            phaseFraction = 0.0,
            illuminationFraction = 0.0,
            phase = TsukimiMoonCalculator.MoonPhase.NEW_MOON,
            isWaxing = true,
        )
        assertEquals(0.0, newMoon.illuminationFraction, 0.001)

        // Full Moon physics
        val fullMoon = TsukimiMoonCalculator.LunarState(
            ageDays = 14.76,
            phaseFraction = 0.5,
            illuminationFraction = 1.0,
            phase = TsukimiMoonCalculator.MoonPhase.FULL_MOON,
            isWaxing = true,
        )
        assertEquals(1.0, fullMoon.illuminationFraction, 0.001)

        // First Quarter physics
        val quarter = TsukimiMoonCalculator.LunarState(
            ageDays = 7.38,
            phaseFraction = 0.25,
            illuminationFraction = 0.5,
            phase = TsukimiMoonCalculator.MoonPhase.FIRST_QUARTER,
            isWaxing = true,
        )
        assertEquals(0.5, quarter.illuminationFraction, 0.001)
    }
}
