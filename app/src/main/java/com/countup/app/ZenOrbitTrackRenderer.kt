package com.countup.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural vector renderer for the Zen Orbit (律动之环 · 温润玉环) dial.
 * Renders a single 8dp warm jade orbit track with a nestled historical cadence pebble,
 * living count sweep arc, and Kintsugi Gold resonance shift upon reaching cadence.
 */
object ZenOrbitTrackRenderer {

    const val DEFAULT_SIZE_PX = 180
    const val MAX_BITMAP_BYTES = 32 * 1024 // 32 KB IPC allocation gate

    /** Clamps requested size so IPC allocation strictly respects [maxBytes]. */
    fun computeSafeSize(requestedSize: Int, maxBytes: Int = MAX_BITMAP_BYTES): Int {
        val rawBytes = requestedSize.toLong() * requestedSize.toLong() * 4L
        return if (rawBytes > maxBytes) {
            val scale = kotlin.math.sqrt(maxBytes.toDouble() / rawBytes.toDouble())
            (requestedSize * scale).toInt().coerceAtLeast(40)
        } else {
            requestedSize
        }
    }

    /**
     * Renders a [Bitmap] containing the single 8dp Warm Jade Orbit:
     * - Background 360° quiet groove track
     * - Living streak arc representing current elapsed days (Bamboo Jade -> Kintsugi Gold)
     * - Historical average cadence pebble nestled on the same orbit track
     */
    fun renderOrbit(
        daysCount: Long,
        averageDays: Int,
        resetCount: Int,
        isDark: Boolean,
        sizePx: Int = DEFAULT_SIZE_PX,
    ): Bitmap? {
        return try {
            val safeSize = computeSafeSize(sizePx, MAX_BITMAP_BYTES)
            val bitmap = createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val palette = WidgetThemeTokens.resolve(isDark)
            val cx = safeSize / 2f
            val cy = safeSize / 2f

            // Single unified orbital circle with 8dp-proportional gauge
            val rOrbit = safeSize * 0.38f
            val strokeOrbit = maxOf(3f, safeSize * 0.056f)
            val strokeGroove = maxOf(2f, strokeOrbit * 0.75f)

            val hasHistory = resetCount > 0 && averageDays > 0
            val baselineTarget = if (hasHistory) averageDays.toLong() else 30L
            val scaleMax = maxOf(baselineTarget * 5L / 4L, (daysCount * 11L) / 10L, 20L).toFloat()

            val currentFraction = (daysCount / scaleMax).coerceIn(0f, 1f)
            val cadenceFraction = if (hasHistory) (averageDays / scaleMax).coerceIn(0f, 1f) else 0f
            val isHarmonicOrBeyond = hasHistory && daysCount >= averageDays

            // 1. Quiet 360° Background Groove
            val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.hairline
                strokeWidth = strokeGroove
                style = Paint.Style.STROKE
                alpha = if (isDark) 75 else 95
            }
            canvas.drawCircle(cx, cy, rOrbit, groovePaint)

            val startAngle = -90f // 12 o'clock

            // 2. Living Current Streak Arc (Bamboo Jade -> Kintsugi Gold)
            if (currentFraction > 0f) {
                val currentColor = if (isHarmonicOrBeyond) {
                    palette.accentKintsugi
                } else {
                    palette.accentPrimary
                }
                val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = currentColor
                    strokeWidth = strokeOrbit
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                val orbitRect = RectF(cx - rOrbit, cy - rOrbit, cx + rOrbit, cy + rOrbit)
                canvas.drawArc(orbitRect, startAngle, currentFraction * 360f, false, currentPaint)
            }

            // 3. Cadence Anchor Pebble nestled on the same 8dp orbit
            if (hasHistory && cadenceFraction > 0f) {
                val cadenceAngleRad = Math.toRadians((startAngle + cadenceFraction * 360f).toDouble())
                val pebbleX = cx + (rOrbit * cos(cadenceAngleRad)).toFloat()
                val pebbleY = cy + (rOrbit * sin(cadenceAngleRad)).toFloat()
                val pebbleRadius = strokeOrbit * 0.50f

                val cadenceColor = if (isHarmonicOrBeyond) {
                    palette.accentKintsugi
                } else {
                    if (isDark) 0xFFD4B87C.toInt() else 0xFF9C8360.toInt()
                }

                val pebblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cadenceColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(pebbleX, pebbleY, pebbleRadius, pebblePaint)

                // Resonant halo when reaching harmonic cadence or beyond
                if (isHarmonicOrBeyond) {
                    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.accentKintsugi
                        style = Paint.Style.STROKE
                        strokeWidth = maxOf(1f, strokeOrbit * 0.22f)
                        alpha = if (isDark) 160 else 180
                    }
                    canvas.drawCircle(pebbleX, pebbleY, pebbleRadius + maxOf(1.5f, strokeOrbit * 0.28f), haloPaint)
                }
            }

            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}
