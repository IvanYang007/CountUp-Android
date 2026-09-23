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
 * Unit test suite for Shuin (金石朱印 · 1x1) cinnabar seal widget.
 * Tests reducer state resolution, memory budget clamping, store binding lifecycle, and purge.
 */
class ShuinWidgetTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 22)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("shuin_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun testShuinReducerStateResolution() {
        val item = CountUpItem(
            id = "shuin-item-1",
            name = "Sobriety Journey",
            epochDay = today.minusDays(42).toEpochDay(),
        )

        val state = ZenWidgetReducer.resolveShuinState(
            item = item,
            today = today,
            isDarkMode = false,
        )

        assertEquals("Sobriety Journey", state.title)
        assertEquals("SOBRIETY", state.oneWordLabel)
        assertEquals(42L, state.daysCount)
        assertEquals("42", state.compactNumberText)
        assertFalse(state.isFuture)
    }

    @Test
    fun testShuinReducerFutureCountdown() {
        val futureItem = CountUpItem(
            id = "shuin-future-1",
            name = "Marathon",
            epochDay = today.plusDays(15).toEpochDay(),
        )

        val state = ZenWidgetReducer.resolveShuinState(
            item = futureItem,
            today = today,
            isDarkMode = false,
        )

        assertEquals("MARATHON", state.oneWordLabel)
        assertEquals(15L, state.daysCount)
        assertEquals("15", state.compactNumberText)
        assertTrue(state.isFuture)
    }

    @Test
    fun testShuinReducerLargeCountFormatting() {
        val longItem = CountUpItem(
            id = "shuin-long-1",
            name = "Life Mastery",
            epochDay = today.minusDays(1500).toEpochDay(),
        )

        val state = ZenWidgetReducer.resolveShuinState(
            item = longItem,
            today = today,
            isDarkMode = false,
        )

        assertEquals(1500L, state.daysCount)
        assertEquals("1.5k", state.compactNumberText)
    }

    @Test
    fun testShuinSealRendererMemoryBudgetCompliance() {
        // Standard requested size (140 width) must be clamped under 24KB
        val safeDims = ShuinSealRenderer.computeSafeDimensions(140)
        val allocatedBytes = safeDims.width * safeDims.height * 4
        assertTrue(
            "Allocated bitmap bytes ($allocatedBytes) must not exceed 24KB (${ShuinSealRenderer.MAX_BITMAP_BYTES})",
            allocatedBytes <= ShuinSealRenderer.MAX_BITMAP_BYTES,
        )

        // Huge requested size (1200px) must still be clamped under 24KB
        val extremeSafeDims = ShuinSealRenderer.computeSafeDimensions(1200)
        val extremeBytes = extremeSafeDims.width * extremeSafeDims.height * 4
        assertTrue(extremeBytes <= ShuinSealRenderer.MAX_BITMAP_BYTES)

        // Tall Scholar's Seal (1:1.18 aspect ratio) dimensions must also strictly respect 24KB
        val dims = ShuinSealRenderer.computeSafeDimensions(140)
        val dimsBytes = dims.width * dims.height * 4
        assertTrue(
            "Tall seal bitmap bytes ($dimsBytes) for dims (${dims.width}x${dims.height}) must not exceed 24KB",
            dimsBytes <= ShuinSealRenderer.MAX_BITMAP_BYTES,
        )
        assertTrue("Height must be >= width for tall scholar seal", dims.height >= dims.width)

        // Extreme requested size in tall format
        val extremeDims = ShuinSealRenderer.computeSafeDimensions(1200)
        val extremeDimsBytes = extremeDims.width * extremeDims.height * 4
        assertTrue(
            "Extreme tall seal bitmap bytes ($extremeDimsBytes) must not exceed 24KB",
            extremeDimsBytes <= ShuinSealRenderer.MAX_BITMAP_BYTES,
        )
    }

    @Test
    fun testShuinStoreBindingLifecycleAndPurge() {
        val store = CountUpStore(testContext)
        val item = store.addItem(
            name = "Meditation",
            epochDay = today.minusDays(108).toEpochDay(),
            id = "test-item-shuin",
        )
        assertNotNull(item)
        val validItem = item!!

        val widgetId = 506
        store.setShuinBinding(widgetId, validItem.id)
        assertEquals(validItem.id, store.getShuinBinding(widgetId))

        // Deleting the item must purge the binding
        val purged = store.deleteItem(validItem.id)
        assertTrue(purged)
        assertEquals(null, store.getShuinBinding(widgetId))
    }
}
