package com.countup.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap

/**
 * Procedural vector renderer for the Shuin (金石朱印 · 1x1) cinnabar stone seal.
 * Renders an unboxed, organic vermilion seal plate with wabi-sabi hand-carved edge asymmetry,
 * earthy matte cinnabar radial gradient, subtle stone beveling, and dashed hairline inner rim,
 * strictly complying with the Android Binder RemoteViews IPC memory budget (<=24 KB).
 */
object ShuinSealRenderer {

    const val DEFAULT_SIZE_PX = 140
    const val MAX_BITMAP_BYTES = 24 * 1024 // 24 KB IPC allocation gate
    const val ASPECT_RATIO = 1.18f // Authentic tall scholar's seal (随形长方闲章)

    data class SealDimensions(val width: Int, val height: Int)

    /** Clamps requested width and height so IPC allocation strictly respects [maxBytes]. */
    fun computeSafeDimensions(
        requestedWidth: Int,
        aspectRatio: Float = ASPECT_RATIO,
        maxBytes: Int = MAX_BITMAP_BYTES
    ): SealDimensions {
        val rawW = requestedWidth.toDouble()
        val rawH = rawW * aspectRatio
        val rawBytes = rawW * rawH * 4.0
        val scale = if (rawBytes > maxBytes) {
            kotlin.math.sqrt(maxBytes.toDouble() / rawBytes)
        } else {
            1.0
        }
        var safeW = (rawW * scale).toInt().coerceAtLeast(36)
        var safeH = (safeW * aspectRatio).toInt().coerceAtLeast(36)
        while (safeW * safeH * 4 > maxBytes && safeW > 36) {
            safeW--
            safeH = (safeW * aspectRatio).toInt()
        }
        return SealDimensions(safeW, safeH)
    }

    /**
     * Renders a [Bitmap] containing the organic cinnabar stone seal plate:
     * - Soft ambient ground shadow for floating wallpaper depth
     * - Wabi-sabi hand-carved asymmetrical squircle seal body (1:1.18 tall seal aspect ratio)
     * - Matte cinnabar radial gradient (#B1492D -> #9E3C24 -> #93351F)
     * - Subtle mineral edge bevel & bottom inner shadow (zero glossy specular reflection)
     * - Inner eroded dashed stone hairline rim
     * - Tight zero-moat padding (1.8%) maximizing fill in Android launcher cells
     */
    fun renderSeal(
        isDark: Boolean = false,
        sizePx: Int = DEFAULT_SIZE_PX,
    ): Bitmap? {
        return try {
            val dims = computeSafeDimensions(sizePx, ASPECT_RATIO, MAX_BITMAP_BYTES)
            val bitmap = createBitmap(dims.width, dims.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val w = dims.width.toFloat()
            val h = dims.height.toFloat()
            val paddingX = w * 0.018f
            val paddingY = h * 0.018f
            val shadowOffset = h * 0.022f

            // Asymmetrical wabi-sabi corner radii (TL, TR, BR, BL) matching prototype: 20px 18px 22px 19px on 74px body
            val rTL = w * 0.28f
            val rTR = w * 0.25f
            val rBR = w * 0.30f
            val rBL = w * 0.27f
            val radii = floatArrayOf(
                rTL, rTL,
                rTR, rTR,
                rBR, rBR,
                rBL, rBL,
            )

            // 1. Soft Ambient Ground Shadow
            val shadowRect = RectF(paddingX, paddingY + shadowOffset, w - paddingX, h - paddingY + shadowOffset)
            val shadowPath = Path().apply {
                addRoundRect(shadowRect, radii, Path.Direction.CW)
            }
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(if (isDark) 0x65 else 0x48, 0x1E, 0x08, 0x06)
                style = Paint.Style.FILL
            }
            canvas.drawPath(shadowPath, shadowPaint)

            // 2. Cinnabar Stone Seal Body with Matte Vermilion Radial Gradient
            val sealRect = RectF(paddingX, paddingY, w - paddingX, h - paddingY)
            val sealPath = Path().apply {
                addRoundRect(sealRect, radii, Path.Direction.CW)
            }

            // Radial gradient centered at 35% 30% from prototype CSS, using sampled prototype terracotta cinnabar
            val gradCx = paddingX + (w - 2 * paddingX) * 0.35f
            val gradCy = paddingY + (h - 2 * paddingY) * 0.30f
            val gradRadius = (w - 2 * paddingX) * 0.95f
            val vermilionColors = intArrayOf(
                0xFFB1492D.toInt(), // Warm terracotta cinnabar center (sampled prototype #B1492D)
                0xFF9E3C24.toInt(), // Rich earthy body (sampled prototype #9E3C24)
                0xFF93351F.toInt(), // Deep aged stone edge (sampled prototype #93351F)
            )
            val vermilionStops = floatArrayOf(0.0f, 0.65f, 1.0f)
            val sealShader = RadialGradient(
                gradCx, gradCy, gradRadius,
                vermilionColors, vermilionStops,
                Shader.TileMode.CLAMP,
            )
            val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = sealShader
                style = Paint.Style.FILL
            }
            canvas.drawPath(sealPath, sealPaint)

            // 3. Subtle Mineral Edge Bevel & Border (prototype: 1.5px solid rgba(44, 36, 22, 0.25))
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(0x38, 0x2C, 0x1E, 0x14)
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1.0f, w * 0.018f)
            }
            canvas.drawPath(sealPath, borderPaint)

            // Top-left subtle edge highlight (prototype: inset 0 0 0 1.5px rgba(255, 255, 255, 0.18))
            val topBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, paddingY,
                    0f, paddingY + h * 0.35f,
                    Color.argb(0x2E, 0xFF, 0xEA, 0xE0),
                    Color.argb(0x00, 0xFF, 0xEA, 0xE0),
                    Shader.TileMode.CLAMP,
                )
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1.0f, w * 0.016f)
            }
            canvas.drawPath(sealPath, topBevelPaint)

            // Bottom edge inner shadow (prototype: inset 0 -2px 4px rgba(0, 0, 0, 0.35))
            val bottomShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, h - paddingY - h * 0.25f,
                    0f, h - paddingY,
                    Color.argb(0x00, 0x1E, 0x05, 0x03),
                    Color.argb(0x45, 0x1E, 0x05, 0x03),
                    Shader.TileMode.CLAMP,
                )
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1.2f, w * 0.020f)
            }
            canvas.drawPath(sealPath, bottomShadowPaint)

            // 4. Inner Eroded Dashed Stone Hairline Rim (prototype: inset 4px, 1px dashed rgba(255,255,255,0.35))
            val rimInsetX = w * 0.08f
            val rimInsetY = h * 0.08f
            val rimRect = RectF(paddingX + rimInsetX, paddingY + rimInsetY, w - paddingX - rimInsetX, h - paddingY - rimInsetY)
            val rimRadii = floatArrayOf(
                (rTL - rimInsetX).coerceAtLeast(5f), (rTL - rimInsetX).coerceAtLeast(5f),
                (rTR - rimInsetX).coerceAtLeast(5f), (rTR - rimInsetX).coerceAtLeast(5f),
                (rBR - rimInsetX).coerceAtLeast(5f), (rBR - rimInsetX).coerceAtLeast(5f),
                (rBL - rimInsetX).coerceAtLeast(5f), (rBL - rimInsetX).coerceAtLeast(5f),
            )
            val rimPath = Path().apply {
                addRoundRect(rimRect, rimRadii, Path.Direction.CW)
            }
            // Delicate dashed pattern with ~8-9 crisp dashes across top edge
            val dashLen = maxOf(4.0f, w * 0.07f)
            val dashGap = maxOf(3.0f, w * 0.05f)
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(if (isDark) 0x75 else 0x65, 0xFA, 0xF4, 0xEE)
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1.2f, w * 0.018f)
                pathEffect = DashPathEffect(floatArrayOf(dashLen, dashGap), 0f)
            }
            canvas.drawPath(rimPath, rimPaint)

            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}

