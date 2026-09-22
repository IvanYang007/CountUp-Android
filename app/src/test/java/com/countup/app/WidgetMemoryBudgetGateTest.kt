package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMemoryBudgetGateTest {

    @Test
    fun zenHorizonPayloadStrictlyUnder40KB() {
        // 4x1 ribbon at default dimensions (280x28)
        val estimate4x1 = WidgetMemoryBudgetGate.estimateZenHorizonPayload(is2x1 = false)
        assertTrue(estimate4x1.isWithinBudget)
        assertTrue("Expected < 40KB but was ${estimate4x1.estimatedBytes}", estimate4x1.estimatedBytes < WidgetMemoryBudgetGate.ZEN_HORIZON_MAX_BYTES)
        assertEquals(WidgetMemoryBudgetGate.ZEN_HORIZON_MAX_BYTES, estimate4x1.maxAllowedBytes)

        // 2x1 ribbon at default dimensions
        val estimate2x1 = WidgetMemoryBudgetGate.estimateZenHorizonPayload(is2x1 = true)
        assertTrue(estimate2x1.isWithinBudget)
        assertTrue("Expected < 40KB but was ${estimate2x1.estimatedBytes}", estimate2x1.estimatedBytes < WidgetMemoryBudgetGate.ZEN_HORIZON_MAX_BYTES)

        // Stress test: huge display density (e.g. 1200x120px) must be clamped by MAX_BITMAP_BYTES
        val clampedExtreme = WidgetMemoryBudgetGate.estimateZenHorizonPayload(
            is2x1 = false,
            widthPx = 1200,
            heightPx = 120,
        )
        assertTrue("Clamped extreme should stay < 40KB", clampedExtreme.isWithinBudget)
        assertTrue(clampedExtreme.estimatedBytes < WidgetMemoryBudgetGate.ZEN_HORIZON_MAX_BYTES)
    }


    @Test
    fun solarRhythmPayloadStrictlyUnder40KB() {
        // 4x2 rich canvas
        val estimate4x2 = WidgetMemoryBudgetGate.estimateSolarRhythmPayload(is4x2 = true)
        assertTrue(estimate4x2.isWithinBudget)
        assertTrue("Expected < 40KB but was ${estimate4x2.estimatedBytes}", estimate4x2.estimatedBytes < WidgetMemoryBudgetGate.SOLAR_RHYTHM_MAX_BYTES)
        assertEquals(WidgetMemoryBudgetGate.SOLAR_RHYTHM_MAX_BYTES, estimate4x2.maxAllowedBytes)

        // 2x2 compact fallback
        val estimate2x2 = WidgetMemoryBudgetGate.estimateSolarRhythmPayload(is4x2 = false)
        assertTrue(estimate2x2.isWithinBudget)
        assertTrue("Expected < 40KB but was ${estimate2x2.estimatedBytes}", estimate2x2.estimatedBytes < WidgetMemoryBudgetGate.SOLAR_RHYTHM_MAX_BYTES)
    }

    @Test
    fun zenPebblePayloadStrictlyUnder15KB() {
        val estimate = WidgetMemoryBudgetGate.estimateZenPebblePayload()
        assertTrue(estimate.isWithinBudget)
        assertTrue("Expected < 15KB but was ${estimate.estimatedBytes}", estimate.estimatedBytes < WidgetMemoryBudgetGate.ZEN_PEBBLE_MAX_BYTES)
        assertEquals(WidgetMemoryBudgetGate.ZEN_PEBBLE_MAX_BYTES, estimate.maxAllowedBytes)
        // Assert ultra-compact footprint (< 2 KB actual footprint)
        assertTrue(estimate.estimatedBytes < 2048)
    }

    @Test
    fun zenOrbitPayloadStrictlyUnder40KB() {
        val estimate = WidgetMemoryBudgetGate.estimateZenOrbitPayload()
        assertTrue(estimate.isWithinBudget)
        assertTrue("Expected < 40KB but was ${estimate.estimatedBytes}", estimate.estimatedBytes < WidgetMemoryBudgetGate.ZEN_ORBIT_MAX_BYTES)
        assertEquals(WidgetMemoryBudgetGate.ZEN_ORBIT_MAX_BYTES, estimate.maxAllowedBytes)
    }

    @Test
    fun assertAllWithinBudgetCumulativeGatePasses() {
        val estimates = WidgetMemoryBudgetGate.assertAllWithinBudget()
        assertEquals(6, estimates.size)
        for (estimate in estimates) {
            assertTrue("${estimate.widgetType} must be within budget", estimate.isWithinBudget)
        }
    }
}

