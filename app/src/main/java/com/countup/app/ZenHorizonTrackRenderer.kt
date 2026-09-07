package com.countup.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

/**
 * Procedural vector renderer for the Zen Horizon 1dp hairline milestone track.
 * Renders the hairline background, gold milestone fill, and Patina Gold circular pebble indicator.
 */
object ZenHorizonTrackRenderer {

    const val DEFAULT_WIDTH_PX = 280
    const val DEFAULT_HEIGHT_PX = 28
    const val MAX_BITMAP_BYTES = 32 * 1024 // 32 KB strict IPC allocation gate

    /** Clamps requested bitmap dimensions so IPC allocation strictly respects [maxBytes]. */
    fun computeSafeDimensions(widthPx: Int, heightPx: Int, maxBytes: Int = MAX_BITMAP_BYTES): Pair<Int, Int> {
        val rawBytes = widthPx * heightPx * 4
        return if (rawBytes > maxBytes) {
            val scale = kotlin.math.sqrt(maxBytes.toDouble() / rawBytes)
            val w = (widthPx * scale).toInt().coerceAtLeast(10)
            val h = (heightPx * scale).toInt().coerceAtLeast(4)
            Pair(w, h)
        } else {
            Pair(widthPx, heightPx)
        }
    }

    /** Draws an anti-aliased, rounded-cap stroke line onto [canvas]. */
    fun drawTrackLine(
        canvas: Canvas,
        startX: Float,
        endX: Float,
        centerY: Float,
        thickness: Float,
        color: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = thickness
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        canvas.drawLine(startX, centerY, endX, centerY, paint)
    }

    /**
     * Renders a [Bitmap] containing the 1dp hairline milestone track,
     * filled progress bar, and Patina Gold circular pebble indicator.
     */
    fun renderTrack(
        progress: Float, // 0.0f..1.0f
        isDark: Boolean,
        widthPx: Int = DEFAULT_WIDTH_PX,
        heightPx: Int = DEFAULT_HEIGHT_PX,
    ): Bitmap? {
        return try {
            val (safeWidth, safeHeight) = computeSafeDimensions(widthPx, heightPx, MAX_BITMAP_BYTES)

            val bitmap = createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val palette = WidgetThemeTokens.resolve(isDark)
            val centerY = heightPx / 2f

            // Hairline track height: ~2px (corresponds to 1dp on ~2x-3x displays)
            val trackThickness = maxOf(2f, heightPx * 0.09f)
            val pebbleRadius = heightPx * 0.26f
            val haloRadius = pebbleRadius + maxOf(2f, heightPx * 0.08f)

            // Padding on left and right so pebble doesn't clip at 0% or 100%
            val startX = haloRadius
            val endX = widthPx - haloRadius
            val trackLength = maxOf(1f, endX - startX)

            // 1. Draw 1dp background hairline track (#E3D3B8 in light, #3A3D35 in dark)
            drawTrackLine(canvas, startX, endX, centerY, trackThickness, palette.hairline)

            // 2. Draw filled milestone progress bar
            val clampedProgress = progress.coerceIn(0f, 1f)
            val currentX = startX + (trackLength * clampedProgress)

            if (clampedProgress > 0f) {
                drawTrackLine(canvas, startX, currentX, centerY, trackThickness, palette.accentGold)
            }

            // 3. Draw Patina Gold pebble halo (matching widget background for clean cutout)
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.canvasBg
                style = Paint.Style.FILL
            }
            canvas.drawCircle(currentX, centerY, haloRadius, haloPaint)

            // 4. Draw Patina Gold pebble indicator (#DEB285)
            val pebblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentGold
                style = Paint.Style.FILL
            }
            canvas.drawCircle(currentX, centerY, pebbleRadius, pebblePaint)

            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}
