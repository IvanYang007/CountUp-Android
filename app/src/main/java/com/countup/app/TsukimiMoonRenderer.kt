package com.countup.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlin.math.abs
import kotlin.math.cos

/**
 * Procedural vector renderer for the Tsukimi (月相之镜) lunar disc.
 * Renders an astronomical moon disc with illuminated Washi crescent and filament gold accent
 * while strictly adhering to Android Binder RemoteViews IPC memory budget (<32 KB).
 */
object TsukimiMoonRenderer {

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
     * Renders a [Bitmap] containing the Tsukimi Moon Disc:
     * - Twilight Sumi night sky base circle
     * - Illuminated Washi crescent/gibbous arc calculated via [lunarState]
     * - Warm filament gold outer rim
     */
    fun renderMoonDisc(
        lunarState: TsukimiMoonCalculator.LunarState,
        isDark: Boolean,
        sizePx: Int = DEFAULT_SIZE_PX
    ): Bitmap? {
        return try {
            val safeSize = computeSafeSize(sizePx, MAX_BITMAP_BYTES)
            val bitmap = createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val palette = WidgetThemeTokens.resolve(isDark)
            val cx = safeSize / 2f
            val cy = safeSize / 2f
            val r = safeSize * 0.44f

            // 0. Ambient Soft Elevation Shadow (Prototype CSS: 0 4px 16px rgba(0,0,0,0.25))
            val shadowColor = if (isDark) Color.argb(45, 0, 0, 0) else Color.argb(35, 44, 36, 22)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shadowColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy + safeSize * 0.02f, r + safeSize * 0.015f, shadowPaint)

            // 1. Twilight Sumi Night Base Disc
            val baseNightColor = if (isDark) {
                Color.rgb(0x18, 0x16, 0x14) // Deep Sumi
            } else {
                Color.rgb(0x2A, 0x26, 0x22) // Twilight Slate
            }
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = baseNightColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, r, basePaint)

            // 2. Illuminated Crescent/Gibbous Arc
            val fraction = lunarState.illuminationFraction
            if (fraction > 0.005) {
                val moonPath = buildMoonPath(cx, cy, r, lunarState)
                if (moonPath != null) {
                    val washiMoonColor = if (isDark) {
                        Color.rgb(0xFA, 0xF5, 0xEE) // Luminous Washi
                    } else {
                        Color.rgb(0xFF, 0xFD, 0xF8) // Bright Washi Silk
                    }
                    val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = washiMoonColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(moonPath, moonPaint)
                }
            }

            // 3. Filament Gold Accent Outer Rim
            val rimColor = if (fraction > 0.35) {
                palette.accentGold
            } else {
                palette.hairline
            }
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = rimColor
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1.5f, safeSize * 0.016f)
                alpha = if (fraction > 0.35) (if (isDark) 190 else 160) else 80
            }
            canvas.drawCircle(cx, cy, r, rimPaint)

            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Builds the geometric terminator path for the illuminated moon portion.
     */
    fun buildMoonPath(
        cx: Float,
        cy: Float,
        r: Float,
        lunarState: TsukimiMoonCalculator.LunarState
    ): Path? {
        val p = lunarState.phaseFraction
        if (p < 0.005 || p > 0.995) return null // New Moon: fully dark

        val path = Path()
        val theta = p * 2.0 * Math.PI
        val rx = (r * abs(cos(theta))).toFloat().coerceAtLeast(0.1f)

        val topY = cy - r
        val botY = cy + r

        val outerOval = RectF(cx - r, topY, cx + r, botY)
        val termOval = RectF(cx - rx, topY, cx + rx, botY)

        if (lunarState.isWaxing) {
            // Waxing: Right half is lit.
            // Arc 1: Outer semicircle on the right from 12 o'clock (-90°) to 6 o'clock (+90°)
            path.arcTo(outerOval, -90f, 180f, true)

            // Arc 2: Terminator from 6 o'clock back to 12 o'clock
            if (p < 0.25) {
                // Crescent: terminator bows rightward (sweep -180)
                path.arcTo(termOval, 90f, -180f, false)
            } else {
                // Gibbous: terminator bows leftward into dark side (sweep +180)
                path.arcTo(termOval, 90f, 180f, false)
            }
        } else {
            // Waning: Left half is lit.
            // Arc 1: Outer semicircle on the left from 12 o'clock (-90°) to 6 o'clock (sweep -180°)
            path.arcTo(outerOval, -90f, -180f, true)

            // Arc 2: Terminator from 6 o'clock back to 12 o'clock
            if (p < 0.75) {
                // Gibbous: terminator bows rightward into dark side (sweep -180)
                path.arcTo(termOval, 90f, -180f, false)
            } else {
                // Crescent: terminator bows leftward into lit side (sweep +180)
                path.arcTo(termOval, 90f, 180f, false)
            }
        }

        path.close()
        return path
    }
}
