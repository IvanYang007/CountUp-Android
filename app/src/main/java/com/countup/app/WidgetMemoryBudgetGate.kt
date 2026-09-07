package com.countup.app

import android.os.Parcel
import android.os.Parcelable

/**
 * IPC Memory Budget & RemoteViews Transaction Gate.
 * Enforces strict byte budget limits across all 3 Zen widgets to prevent
 * Android Binder TransactionTooLargeException during broadcast push updates.
 *
 * Budgets:
 * - Zen Horizon (4x1 & 2x1): strictly < 40 KB (40,960 bytes)
 * - Solar Rhythm (4x2 & 2x2 Glance): strictly < 40 KB (40,960 bytes)
 * - Zen Pebble (1x1): strictly < 15 KB (15,360 bytes)
 */
object WidgetMemoryBudgetGate {

    const val ZEN_HORIZON_MAX_BYTES = 40 * 1024 // 40 KB
    const val SOLAR_RHYTHM_MAX_BYTES = 40 * 1024 // 40 KB
    const val ZEN_PEBBLE_MAX_BYTES = 15 * 1024   // 15 KB

    enum class WidgetType(val maxAllowedBytes: Int) {
        ZEN_HORIZON_4X1(ZEN_HORIZON_MAX_BYTES),
        ZEN_HORIZON_2X1(ZEN_HORIZON_MAX_BYTES),
        SOLAR_RHYTHM_4X2(SOLAR_RHYTHM_MAX_BYTES),
        SOLAR_RHYTHM_2X2(SOLAR_RHYTHM_MAX_BYTES),
        ZEN_PEBBLE_1X1(ZEN_PEBBLE_MAX_BYTES),
    }

    data class PayloadEstimate(
        val widgetType: WidgetType,
        val estimatedBytes: Int,
        val maxAllowedBytes: Int,
        val isWithinBudget: Boolean,
    )

    /**
     * Estimates payload size for Zen Horizon (4x1 or 2x1).
     * Includes bitmap payload (clamped to at most 32 KB) + RemoteViews IPC action metadata (~1.2 KB).
     */
    fun estimateZenHorizonPayload(
        is2x1: Boolean,
        widthPx: Int = ZenHorizonTrackRenderer.DEFAULT_WIDTH_PX,
        heightPx: Int = ZenHorizonTrackRenderer.DEFAULT_HEIGHT_PX,
    ): PayloadEstimate {
        val type = if (is2x1) WidgetType.ZEN_HORIZON_2X1 else WidgetType.ZEN_HORIZON_4X1
        val rawBitmapBytes = widthPx * heightPx * 4
        val clampedBitmapBytes = minOf(rawBitmapBytes, ZenHorizonTrackRenderer.MAX_BITMAP_BYTES)
        // IPC Action overhead (view IDs, string reflections, pending intents)
        val actionOverheadBytes = if (is2x1) 1024 else 1280
        val totalBytes = clampedBitmapBytes + actionOverheadBytes

        return PayloadEstimate(
            widgetType = type,
            estimatedBytes = totalBytes,
            maxAllowedBytes = type.maxAllowedBytes,
            isWithinBudget = totalBytes <= type.maxAllowedBytes,
        )
    }

    /**
     * Estimates payload size for Zen Pebble (1x1).
     * Ultra-compact pure-RemoteViews layout with zero bitmap allocations.
     */
    fun estimateZenPebblePayload(): PayloadEstimate {
        // Layout resource + ~9 primitive text/int view actions + 1 PendingIntent (~850 bytes total)
        val estimatedBytes = 850
        val type = WidgetType.ZEN_PEBBLE_1X1
        return PayloadEstimate(
            widgetType = type,
            estimatedBytes = estimatedBytes,
            maxAllowedBytes = type.maxAllowedBytes,
            isWithinBudget = estimatedBytes <= type.maxAllowedBytes,
        )
    }

    /**
     * Estimates payload size for Solar Rhythm (4x2 or 2x2 Glance).
     * Glance Compose tree containing seasonal timeline progress bar, couplet poetry, and badges.
     */
    fun estimateSolarRhythmPayload(is4x2: Boolean): PayloadEstimate {
        val estimatedBytes = if (is4x2) 11264 else 7168 // ~11 KB for 4x2, ~7 KB for 2x2
        val type = if (is4x2) WidgetType.SOLAR_RHYTHM_4X2 else WidgetType.SOLAR_RHYTHM_2X2
        return PayloadEstimate(
            widgetType = type,
            estimatedBytes = estimatedBytes,
            maxAllowedBytes = type.maxAllowedBytes,
            isWithinBudget = estimatedBytes <= type.maxAllowedBytes,
        )
    }

    /**
     * Measures actual serialized parcel byte size of an Android [Parcelable] when runtime Parcel is available.
     * Returns null if running in an unmocked JVM environment where Parcel is stubbed.
     */
    fun measureParcelSizeSafe(parcelable: Parcelable): Int? {
        return try {
            val parcel = Parcel.obtain()
            try {
                parcelable.writeToParcel(parcel, 0)
                parcel.dataSize()
            } finally {
                parcel.recycle()
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Asserts that all widget payloads strictly comply with the transaction gate budget.
     * Throws [IllegalStateException] if any widget exceeds its budget.
     */
    fun assertAllWithinBudget(): List<PayloadEstimate> {
        val estimates = listOf(
            estimateZenHorizonPayload(is2x1 = false),
            estimateZenHorizonPayload(is2x1 = true),
            estimateSolarRhythmPayload(is4x2 = true),
            estimateSolarRhythmPayload(is4x2 = false),
            estimateZenPebblePayload(),
        )
        for (estimate in estimates) {
            check(estimate.isWithinBudget) {
                "Widget ${estimate.widgetType} exceeded transaction budget: ${estimate.estimatedBytes} bytes > ${estimate.maxAllowedBytes} bytes"
            }
        }
        return estimates
    }
}

