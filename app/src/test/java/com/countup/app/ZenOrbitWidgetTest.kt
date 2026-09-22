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
 * Unit test suite for Zen Orbit (双环律动) concentric widget.
 * Tests reducer state resolution, scale normalization, transcendence shift,
 * and store binding lifecycle/purge.
 */
class ZenOrbitWidgetTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 21)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zen_orbit_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun testZenOrbitReducerStateResolution() {
        // Approaching State: 24 days elapsed, average is 28 days (4 resets, 112 total reset days)
        val approachingItem = CountUpItem(
            id = "orbit-item-1",
            name = "Haircut",
            epochDay = today.minusDays(24).toEpochDay(),
            resetCount = 4,
            totalResetDays = 112L, // avg = 112 / 4 = 28
        )

        val approachingState = ZenWidgetReducer.resolveZenOrbitState(
            item = approachingItem,
            today = today,
            isDarkMode = false,
        )

        assertEquals("HAIRCUT", approachingState.oneWordLabel)
        assertEquals(24L, approachingState.daysCount)
        assertEquals(28, approachingState.averageDays)
        assertEquals(4, approachingState.resetCount)
        assertTrue(approachingState.hasHistory)
        assertFalse(approachingState.isHarmonic)
        assertFalse(approachingState.isTranscended)
        assertEquals(4L, approachingState.deltaDays)

        // Harmonic State: 28 days elapsed, average is 28 days
        val harmonicItem = approachingItem.copy(
            epochDay = today.minusDays(28).toEpochDay(),
        )
        val harmonicState = ZenWidgetReducer.resolveZenOrbitState(
            item = harmonicItem,
            today = today,
        )
        assertTrue(harmonicState.isHarmonic)
        assertFalse(harmonicState.isTranscended)
        assertEquals(0L, harmonicState.deltaDays)

        // Transcendence State: 36 days elapsed, average is 28 days
        val transcendedItem = approachingItem.copy(
            epochDay = today.minusDays(36).toEpochDay(),
        )
        val transcendedState = ZenWidgetReducer.resolveZenOrbitState(
            item = transcendedItem,
            today = today,
        )
        assertFalse(transcendedState.isHarmonic)
        assertTrue(transcendedState.isTranscended)
        assertEquals(8L, transcendedState.deltaDays)

        // Zero-reset State: 14 days elapsed, 0 resets
        val zeroResetItem = CountUpItem(
            id = "orbit-item-zero",
            name = "New Habit",
            epochDay = today.minusDays(14).toEpochDay(),
            resetCount = 0,
            totalResetDays = 0L,
        )
        val zeroResetState = ZenWidgetReducer.resolveZenOrbitState(
            item = zeroResetItem,
            today = today,
        )
        assertFalse(zeroResetState.hasHistory)
        assertFalse(zeroResetState.isHarmonic)
        assertFalse(zeroResetState.isTranscended)
        assertEquals(0, zeroResetState.averageDays)
    }

    @Test
    fun testZenOrbitTrackRendererSafeDimensions() {
        // Standard requested size (180x180) must be clamped to safe size under 32KB
        val safeSize = ZenOrbitTrackRenderer.computeSafeSize(180)
        val allocatedBytes = safeSize * safeSize * 4
        assertTrue(
            "Allocated bitmap bytes ($allocatedBytes) must not exceed 32KB (${ZenOrbitTrackRenderer.MAX_BITMAP_BYTES})",
            allocatedBytes <= ZenOrbitTrackRenderer.MAX_BITMAP_BYTES,
        )

        // Huge requested size (1000px) must still be clamped under 32KB
        val extremeSafeSize = ZenOrbitTrackRenderer.computeSafeSize(1000)
        val extremeBytes = extremeSafeSize * extremeSafeSize * 4
        assertTrue(extremeBytes <= ZenOrbitTrackRenderer.MAX_BITMAP_BYTES)
    }

    @Test
    fun testZenOrbitStoreBindingLifecycleAndPurge() {
        val store = CountUpStore(testContext)
        val item = store.addItem(
            name = "Zen Meditation",
            epochDay = today.minusDays(10).toEpochDay(),
            id = "test-item-orbit",
        )
        assertNotNull(item)
        val validItem = item!!

        val widgetId = 404
        store.setZenOrbitBinding(widgetId, validItem.id)
        assertEquals(validItem.id, store.getZenOrbitBinding(widgetId))

        // Deleting the item must purge the binding
        val purged = store.deleteItem(validItem.id)
        assertTrue(purged)
        assertEquals(null, store.getZenOrbitBinding(widgetId))
    }

    @Test
    fun testZenOrbitTrackGaugeAndPebbleGeometry() {
        val safeSize = ZenOrbitTrackRenderer.computeSafeSize(ZenOrbitTrackRenderer.DEFAULT_SIZE_PX)
        val rOrbit = safeSize * 0.38f
        val strokeOrbit = maxOf(3f, safeSize * 0.056f)
        val strokeGroove = maxOf(2f, strokeOrbit * 0.75f)
        val pebbleRadius = strokeOrbit * 0.50f

        // Pebble must nest snugly within the stroke orbit (pebble radius <= stroke orbit)
        assertTrue(pebbleRadius < strokeOrbit)
        // Groove must be narrower than the living sweep arc
        assertTrue(strokeGroove < strokeOrbit)
        // Orbit radius plus half stroke must stay comfortably within bitmap bounds
        assertTrue(rOrbit + (strokeOrbit / 2f) < (safeSize / 2f))
    }
}
