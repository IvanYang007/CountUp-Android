package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [computeWidgetCanvasDimensions].
 * Verifies that the widget background bitmap size remains strictly bounded across all
 * launcher dimensions, row counts, and edge cases to guarantee immunity from
 * Android's 1MB Binder transaction buffer overflow (TransactionTooLargeException).
 */
class WidgetDimensionTest {

    @Test
    fun `default row count dimensions adhere to bounds when options height is absent or small`() {
        // When optionsHeightDp <= 100 (e.g. 0 on older launchers or initial placement)
        val (w1, h1) = computeWidgetCanvasDimensions(optionsHeightDp = 0, rowCount = 1)
        assertEquals(360, w1)
        assertEquals(160, h1)

        val (w2, h2) = computeWidgetCanvasDimensions(optionsHeightDp = 50, rowCount = 2)
        assertEquals(360, w2)
        assertEquals(200, h2)

        val (w3, h3) = computeWidgetCanvasDimensions(optionsHeightDp = 100, rowCount = 3)
        assertEquals(360, w3)
        assertEquals(240, h3)

        val (w4, h4) = computeWidgetCanvasDimensions(optionsHeightDp = 0, rowCount = 4)
        assertEquals(360, w4)
        assertEquals(240, h4)

        val (w5, h5) = computeWidgetCanvasDimensions(optionsHeightDp = 0, rowCount = 5)
        assertEquals(360, w5)
        assertEquals(240, h5)
    }

    @Test
    fun `launcher options height scales dynamically and strictly clamps between 140 and 260`() {
        // Below min clamp: 120 * 0.6 = 72 -> clamped to 140
        val (wLow, hLow) = computeWidgetCanvasDimensions(optionsHeightDp = 120, rowCount = 2)
        assertEquals(360, wLow)
        assertEquals(140, hLow)

        // Mid-range scaling: 250 * 0.6 = 150
        val (wMid1, hMid1) = computeWidgetCanvasDimensions(optionsHeightDp = 250, rowCount = 2)
        assertEquals(360, wMid1)
        assertEquals(150, hMid1)

        // Mid-range scaling: 350 * 0.6 = 210
        val (wMid2, hMid2) = computeWidgetCanvasDimensions(optionsHeightDp = 350, rowCount = 3)
        assertEquals(360, wMid2)
        assertEquals(210, hMid2)

        // Mid-range scaling: 400 * 0.6 = 240
        val (wMid3, hMid3) = computeWidgetCanvasDimensions(optionsHeightDp = 400, rowCount = 3)
        assertEquals(360, wMid3)
        assertEquals(240, hMid3)

        // Above max clamp: 500 * 0.6 = 300 -> clamped to 260
        val (wHigh, hHigh) = computeWidgetCanvasDimensions(optionsHeightDp = 500, rowCount = 4)
        assertEquals(360, wHigh)
        assertEquals(260, hHigh)

        // Extreme tablet / foldable dimension: 1200 * 0.6 = 720 -> clamped to 260
        val (wExtreme, hExtreme) = computeWidgetCanvasDimensions(optionsHeightDp = 1200, rowCount = 5)
        assertEquals(360, wExtreme)
        assertEquals(260, hExtreme)
    }

    @Test
    fun `memory footprint never exceeds 375KB across all possible input domains`() {
        val binderBufferLimit = 1024 * 1024 // 1 MB Android IPC limit
        val bytesPerPixelArgb8888 = 4

        for (optionsHeight in -50..1200 step 25) {
            for (rows in 0..10) {
                val (width, height) = computeWidgetCanvasDimensions(optionsHeight, rows)

                assertEquals("Target width must always be 360", 360, width)
                assertTrue("Height must be at least 140", height >= 140)
                assertTrue("Height must never exceed 260", height <= 260)

                val bitmapSizeBytes = width * height * bytesPerPixelArgb8888
                assertTrue("Bitmap byte size ($bitmapSizeBytes bytes) must be <= 374,400 bytes", bitmapSizeBytes <= 374_400)

                val safetyMargin = binderBufferLimit - bitmapSizeBytes
                assertTrue("Safety margin to 1MB Binder limit ($safetyMargin bytes) must be >= 600KB", safetyMargin >= 600_000)
            }
        }
    }
}
