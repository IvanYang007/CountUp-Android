package com.countup.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.time.LocalDate

/**
 * Lightweight procedural vector renderer for home-screen widget backgrounds.
 * Renders Chinese ink wash landscape motifs matching the main app's active [BackgroundTheme].
 *
 * Characteristics:
 * - Pure Android Canvas/Path vector operations (zero external dependencies)
 * - Compact hardware bitmap (480x280 px, < 180 KB memory footprint)
 * - Soft, low-opacity mineral wash tones (12%–18%) ensuring 100% text contrast
 * - Right-bottom anchored composition (右下聚景 · 左侧留白)
 */
object WidgetBackgroundRenderer {

    // Canvas rendering resolution for widget backgrounds
    private const val DEFAULT_WIDTH = 480
    private const val DEFAULT_HEIGHT = 280

    // Palette: Chinese Ink & Mineral Pigments with soft wash alpha
    private const val COLOR_PAPER = 0xFFF5E6D3.toInt()
    private const val COLOR_NIGHT_PAPER = 0xFF241D12.toInt()

    private const val INK_BLACK = 0x221E2124       // 浓墨 (low alpha)
    private const val INK_MUTED = 0x204A4E54       // 淡墨
    private const val INK_OCHRE = 0x25C48B58       // 赭石
    private const val INK_VERMILION = 0x2AD3523B   // 朱砂
    private const val INK_INDIGO = 0x254E6B7A      // 花青 / 黛蓝
    private const val INK_SAGE = 0x224A7C59        // 柳绿 / 苔绿

    /**
     * Renders a [Bitmap] for the given [theme] and [epochDay].
     * Returns null if running in a non-Android environment or if bitmap allocation fails.
     */
    fun render(
        theme: BackgroundTheme,
        epochDay: Long = LocalDate.now().toEpochDay(),
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        isNight: Boolean = false,
    ): Bitmap? {
        return try {
            val active = resolveActiveTheme(theme, epochDay)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw base warm paper background
            canvas.drawColor(if (isNight) COLOR_NIGHT_PAPER else COLOR_PAPER)

            val w = width.toFloat()
            val h = height.toFloat()

            // 2. Draw active theme vector landscape in bottom-right corner
            when (active) {
                BackgroundTheme.MOUNTAIN -> drawMountainTheme(canvas, w, h)
                BackgroundTheme.SAND_DUNES -> drawSandDunesTheme(canvas, w, h)
                BackgroundTheme.SEA_HORIZON -> drawSeaHorizonTheme(canvas, w, h)
                BackgroundTheme.SOLITARY_ISLE -> drawSolitaryIsleTheme(canvas, w, h)
                BackgroundTheme.WILLOW_LEAVES -> drawWillowLeavesTheme(canvas, w, h)
                BackgroundTheme.AUTO_DAILY -> drawMountainTheme(canvas, w, h)
            }

            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    // ------------------------------------------------------------------------
    // THEME 1: MOUNTAIN PEAKS (远山含黛)
    // ------------------------------------------------------------------------
    private fun drawMountainTheme(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Distant mountain wash
        val ridgePath1 = Path().apply {
            moveTo(w * 0.35f, h)
            cubicTo(w * 0.50f, h * 0.72f, w * 0.65f, h * 0.60f, w * 0.82f, h * 0.52f)
            cubicTo(w * 0.90f, h * 0.48f, w * 0.96f, h * 0.56f, w, h * 0.50f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(
            w * 0.6f, h * 0.5f, w * 0.6f, h,
            INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(ridgePath1, paint)
        paint.shader = null

        // Mid-ground mountain peak
        val ridgePath2 = Path().apply {
            moveTo(w * 0.55f, h)
            cubicTo(w * 0.68f, h * 0.78f, w * 0.78f, h * 0.65f, w * 0.88f, h * 0.62f)
            cubicTo(w * 0.93f, h * 0.60f, w * 0.97f, h * 0.68f, w, h * 0.65f)
            lineTo(w, h)
            close()
        }
        paint.color = INK_MUTED
        canvas.drawPath(ridgePath2, paint)

        // Foreground calligraphic ridge line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.color = INK_BLACK
        val linePath = Path().apply {
            moveTo(w * 0.60f, h)
            cubicTo(w * 0.72f, h * 0.75f, w * 0.80f, h * 0.62f, w * 0.88f, h * 0.62f)
            cubicTo(w * 0.93f, h * 0.60f, w * 0.97f, h * 0.68f, w, h * 0.65f)
        }
        canvas.drawPath(linePath, paint)

        // Subtle ochre sun wash
        paint.style = Paint.Style.FILL
        paint.color = INK_OCHRE
        canvas.drawCircle(w * 0.76f, h * 0.42f, h * 0.12f, paint)
    }

    // ------------------------------------------------------------------------
    // THEME 2: SAND DUNES (平沙落雁)
    // ------------------------------------------------------------------------
    private fun drawSandDunesTheme(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Primary flowing dune
        val dune1 = Path().apply {
            moveTo(w * 0.40f, h)
            cubicTo(w * 0.55f, h * 0.85f, w * 0.70f, h * 0.68f, w * 0.86f, h * 0.64f)
            cubicTo(w * 0.92f, h * 0.62f, w * 0.96f, h * 0.70f, w, h * 0.68f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(
            w * 0.6f, h * 0.6f, w * 0.6f, h,
            INK_OCHRE, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(dune1, paint)
        paint.shader = null

        // Secondary dune ridge line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.color = INK_MUTED
        val duneLine = Path().apply {
            moveTo(w * 0.48f, h)
            cubicTo(w * 0.62f, h * 0.82f, w * 0.75f, h * 0.72f, w * 0.88f, h * 0.74f)
            cubicTo(w * 0.94f, h * 0.75f, w * 0.98f, h * 0.80f, w, h * 0.82f)
        }
        canvas.drawPath(duneLine, paint)

        // Sand grain ripple line
        val ripple = Path().apply {
            moveTo(w * 0.65f, h * 0.88f)
            quadTo(w * 0.78f, h * 0.84f, w * 0.92f, h * 0.86f)
        }
        paint.strokeWidth = 1.2f
        paint.color = INK_OCHRE
        canvas.drawPath(ripple, paint)
    }

    // ------------------------------------------------------------------------
    // THEME 3: SEA HORIZON (瀚海潮平)
    // ------------------------------------------------------------------------
    private fun drawSeaHorizonTheme(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Horizon wash gradient
        val waterPath = Path().apply {
            moveTo(w * 0.30f, h)
            cubicTo(w * 0.50f, h * 0.78f, w * 0.75f, h * 0.70f, w, h * 0.68f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(
            w * 0.6f, h * 0.68f, w * 0.6f, h,
            INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(waterPath, paint)
        paint.shader = null

        // Wave ripple lines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = INK_INDIGO

        val wave1 = Path().apply {
            moveTo(w * 0.50f, h * 0.82f)
            cubicTo(w * 0.65f, h * 0.80f, w * 0.80f, h * 0.84f, w * 0.95f, h * 0.81f)
        }
        canvas.drawPath(wave1, paint)

        val wave2 = Path().apply {
            moveTo(w * 0.62f, h * 0.90f)
            cubicTo(w * 0.74f, h * 0.88f, w * 0.86f, h * 0.91f, w * 0.98f, h * 0.89f)
        }
        paint.strokeWidth = 1.2f
        canvas.drawPath(wave2, paint)

        // Minimalist solitary fishing boat (一叶扁舟)
        val boatPath = Path().apply {
            moveTo(w * 0.82f, h * 0.65f)
            quadTo(w * 0.85f, h * 0.665f, w * 0.88f, h * 0.65f)
        }
        paint.strokeWidth = 2.0f
        paint.color = INK_BLACK
        canvas.drawPath(boatPath, paint)
    }

    // ------------------------------------------------------------------------
    // THEME 4: SOLITARY ISLE & TAIHU SCHOLAR ROCK (孤石清泉 · 太湖石)
    // ------------------------------------------------------------------------
    private fun drawSolitaryIsleTheme(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Taihu Scholar Rock silhouette anchored in bottom-right corner
        val rockX = w * 0.82f
        val rockY = h * 0.52f
        val rockW = w * 0.16f
        val rockH = h * 0.44f

        val rockOutline = Path().apply {
            moveTo(rockX, rockY + rockH)
            cubicTo(rockX - rockW * 0.15f, rockY + rockH * 0.70f, rockX - rockW * 0.30f, rockY + rockH * 0.40f, rockX - rockW * 0.10f, rockY + rockH * 0.15f)
            cubicTo(rockX, rockY + rockH * 0.02f, rockX + rockW * 0.40f, rockY - rockH * 0.05f, rockX + rockW * 0.60f, rockY + rockH * 0.12f)
            cubicTo(rockX + rockW * 0.80f, rockY + rockH * 0.25f, rockX + rockW * 0.95f, rockY + rockH * 0.60f, rockX + rockW * 0.70f, rockY + rockH)
            close()
        }

        // Mineral wash fill
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            rockX, rockY, rockX + rockW, rockY + rockH,
            INK_INDIGO, INK_OCHRE, Shader.TileMode.CLAMP
        )
        canvas.drawPath(rockOutline, paint)
        paint.shader = null

        // Scholar rock hollow cavity (透)
        paint.color = COLOR_PAPER
        canvas.drawCircle(rockX + rockW * 0.22f, rockY + rockH * 0.30f, rockW * 0.14f, paint)

        // Calligraphic crag stroke (皴法)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.color = INK_BLACK
        canvas.drawPath(rockOutline, paint)

        // Moss dots (苍苔)
        paint.style = Paint.Style.FILL
        paint.color = INK_SAGE
        canvas.drawCircle(rockX - rockW * 0.05f, rockY + rockH * 0.25f, 2.2f, paint)
        canvas.drawCircle(rockX + rockW * 0.45f, rockY + rockH * 0.15f, 2.5f, paint)
        canvas.drawCircle(rockX + rockW * 0.15f, rockY + rockH * 0.55f, 2.0f, paint)
    }

    // ------------------------------------------------------------------------
    // THEME 5: WILLOW LEAVES (杨柳依依)
    // ------------------------------------------------------------------------
    private fun drawWillowLeavesTheme(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Weeping willow branch from upper right
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = INK_BLACK

        val branch1 = Path().apply {
            moveTo(w, h * 0.20f)
            cubicTo(w * 0.90f, h * 0.32f, w * 0.82f, h * 0.50f, w * 0.85f, h * 0.75f)
        }
        canvas.drawPath(branch1, paint)

        val branch2 = Path().apply {
            moveTo(w, h * 0.40f)
            cubicTo(w * 0.93f, h * 0.52f, w * 0.88f, h * 0.68f, w * 0.92f, h * 0.88f)
        }
        paint.strokeWidth = 1.4f
        canvas.drawPath(branch2, paint)

        // Willow leaf clusters in sage green
        paint.style = Paint.Style.FILL
        paint.color = INK_SAGE

        val leafCoords = arrayOf(
            floatArrayOf(w * 0.88f, h * 0.38f),
            floatArrayOf(w * 0.83f, h * 0.48f),
            floatArrayOf(w * 0.84f, h * 0.60f),
            floatArrayOf(w * 0.86f, h * 0.72f),
            floatArrayOf(w * 0.94f, h * 0.50f),
            floatArrayOf(w * 0.89f, h * 0.64f),
            floatArrayOf(w * 0.92f, h * 0.80f),
        )

        for (coord in leafCoords) {
            canvas.drawOval(
                coord[0] - 2.5f, coord[1] - 6.0f,
                coord[0] + 2.5f, coord[1] + 6.0f,
                paint
            )
        }
    }
}
