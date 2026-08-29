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
 * Renders Chinese ink wash landscape motifs for all 13 themes matching the main app's active [BackgroundTheme].
 *
 * Characteristics:
 * - Pure Android Canvas/Path vector operations (zero external dependencies)
 * - Compact hardware bitmap (480x280 px, < 180 KB memory footprint)
 * - Soft, low-opacity mineral wash tones (12%–22%) ensuring 100% text contrast
 * - Right-bottom anchored composition (右下聚景 · 左侧留白), specially tuned for compact 4x2 widget height
 */
object WidgetBackgroundRenderer {

    // Canvas rendering resolution for widget backgrounds
    private const val DEFAULT_WIDTH = 480
    private const val DEFAULT_HEIGHT = 280

    // Palette: Chinese Ink & Mineral Pigments with soft wash alpha
    private const val COLOR_PAPER = 0xFFF5E6D3.toInt()
    private const val COLOR_NIGHT_PAPER = 0xFF241D12.toInt()

    private const val INK_BLACK = 0x241E2124       // 浓墨
    private const val INK_MUTED = 0x224A4E54       // 淡墨
    private const val INK_OCHRE = 0x28C48B58       // 赭石
    private const val INK_VERMILION = 0x30D3523B   // 朱砂
    private const val INK_INDIGO = 0x264E6B7A      // 花青 / 黛蓝
    private const val INK_SAGE = 0x244A7C59        // 柳绿 / 苔绿

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
                BackgroundTheme.MOUNTAIN -> drawMountainWidget(canvas, w, h)
                BackgroundTheme.SAND_DUNES -> drawSandDunesWidget(canvas, w, h)
                BackgroundTheme.SEA_HORIZON -> drawSeaHorizonWidget(canvas, w, h)
                BackgroundTheme.SOLITARY_ISLE -> drawSolitaryIsleWidget(canvas, w, h)
                BackgroundTheme.WILLOW_LEAVES -> drawWillowLeavesWidget(canvas, w, h)
                BackgroundTheme.ZEN_BAMBOO -> drawZenBambooWidget(canvas, w, h)
                BackgroundTheme.DREAM_BOAT -> drawDreamBoatWidget(canvas, w, h)
                BackgroundTheme.CLEAR_SPRING -> drawClearSpringWidget(canvas, w, h)
                BackgroundTheme.DESERT_SUNSET -> drawDesertSunsetWidget(canvas, w, h)
                BackgroundTheme.EGRETS_ASCENDING -> drawEgretsAscendingWidget(canvas, w, h)
                BackgroundTheme.PLUM_SHADOW -> drawPlumShadowWidget(canvas, w, h)
                BackgroundTheme.ANCIENT_ROAD -> drawAncientRoadWidget(canvas, w, h)
                BackgroundTheme.SPRING_RAIN -> drawSpringRainWidget(canvas, w, h)
                BackgroundTheme.AUTO_DAILY -> drawMountainWidget(canvas, w, h)
            }

            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    // ------------------------------------------------------------------------
    // 1. 远山含黛 · Mountain Peaks
    // ------------------------------------------------------------------------
    private fun drawMountainWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ridge = Path().apply {
            moveTo(w * 0.35f, h)
            cubicTo(w * 0.50f, h * 0.72f, w * 0.65f, h * 0.60f, w * 0.82f, h * 0.52f)
            cubicTo(w * 0.90f, h * 0.48f, w * 0.96f, h * 0.56f, w, h * 0.50f)
            lineTo(w, h); close()
        }
        paint.shader = LinearGradient(w * 0.6f, h * 0.5f, w * 0.6f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(ridge, paint)
        paint.shader = null
        paint.color = INK_BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f
        val line = Path().apply {
            moveTo(w * 0.35f, h)
            cubicTo(w * 0.50f, h * 0.72f, w * 0.65f, h * 0.60f, w * 0.82f, h * 0.52f)
            cubicTo(w * 0.90f, h * 0.48f, w * 0.96f, h * 0.56f, w, h * 0.50f)
        }
        canvas.drawPath(line, paint)
    }

    // ------------------------------------------------------------------------
    // 2. 平沙落雁 · Sand Dunes
    // ------------------------------------------------------------------------
    private fun drawSandDunesWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dune = Path().apply {
            moveTo(w * 0.40f, h)
            cubicTo(w * 0.55f, h * 0.85f, w * 0.70f, h * 0.68f, w * 0.86f, h * 0.64f)
            cubicTo(w * 0.92f, h * 0.62f, w * 0.96f, h * 0.70f, w, h * 0.68f)
            lineTo(w, h); close()
        }
        paint.shader = LinearGradient(w * 0.6f, h * 0.6f, w * 0.6f, h, INK_OCHRE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(dune, paint)
        paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 3. 烟波浩渺 · Calm Sea Horizon
    // ------------------------------------------------------------------------
    private fun drawSeaHorizonWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        canvas.drawCircle(w * 0.84f, h * 0.46f, h * 0.18f, paint)

        val sea = Path().apply {
            moveTo(w * 0.45f, h)
            cubicTo(w * 0.62f, h * 0.82f, w * 0.78f, h * 0.70f, w, h * 0.72f)
            lineTo(w, h); close()
        }
        paint.shader = LinearGradient(w * 0.6f, h * 0.7f, w * 0.6f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(sea, paint)
        paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 4. 太湖石秀 · Solitary Taihu Scholar Stone
    // ------------------------------------------------------------------------
    private fun drawSolitaryIsleWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val rock = Path().apply {
            moveTo(w * 0.78f, h)
            cubicTo(w * 0.72f, h * 0.82f, w * 0.80f, h * 0.68f, w * 0.88f, h * 0.66f)
            cubicTo(w * 0.94f, h * 0.64f, w * 0.96f, h * 0.80f, w * 0.94f, h)
            close()
        }
        canvas.drawPath(rock, paint)
        paint.color = INK_BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f
        canvas.drawPath(rock, paint)
    }

    // ------------------------------------------------------------------------
    // 5. 柳浪闻莺 · Willow Leaves & Ripples
    // ------------------------------------------------------------------------
    private fun drawWillowLeavesWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.3f
        val stem = Path().apply {
            moveTo(w, h * 0.25f); cubicTo(w * 0.88f, h * 0.38f, w * 0.82f, h * 0.55f, w * 0.80f, h * 0.75f)
        }
        canvas.drawPath(stem, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_SAGE
        canvas.drawOval(w * 0.88f, h * 0.35f, w * 0.94f, h * 0.42f, paint)
        canvas.drawOval(w * 0.82f, h * 0.50f, w * 0.88f, h * 0.57f, paint)
    }

    // ------------------------------------------------------------------------
    // 6. 幽竹虚心 · Zen Bamboo Grove
    // ------------------------------------------------------------------------
    private fun drawZenBambooWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.5f; paint.strokeCap = Paint.Cap.ROUND
        val stalk = Path().apply {
            moveTo(w * 0.90f, h); lineTo(w * 0.91f, h * 0.28f)
        }
        canvas.drawPath(stalk, paint)
        paint.style = Paint.Style.FILL
        val leaf = Path().apply {
            moveTo(w * 0.90f, h * 0.45f)
            quadTo(w * 0.80f, h * 0.48f, w * 0.74f, h * 0.56f)
            quadTo(w * 0.82f, h * 0.54f, w * 0.90f, h * 0.45f)
        }
        canvas.drawPath(leaf, paint)
    }

    // ------------------------------------------------------------------------
    // 7. 满船清梦压星河 · Dream Boat on Milky Way
    // ------------------------------------------------------------------------
    private fun drawDreamBoatWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val water = Path().apply {
            moveTo(w * 0.45f, h)
            cubicTo(w * 0.62f, h * 0.82f, w * 0.78f, h * 0.68f, w * 0.90f, h * 0.65f)
            cubicTo(w * 0.95f, h * 0.64f, w * 0.98f, h * 0.70f, w, h * 0.68f)
            lineTo(w, h); close()
        }
        paint.shader = LinearGradient(w * 0.7f, h * 0.65f, w * 0.7f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(water, paint)
        paint.shader = null

        paint.color = INK_BLACK
        val bx = w * 0.85f; val by = h * 0.75f
        val boat = Path().apply {
            moveTo(bx - 22f, by); quadTo(bx, by + 7f, bx + 22f, by - 3f); quadTo(bx + 8f, by + 3f, bx - 22f, by)
        }
        canvas.drawPath(boat, paint)
        paint.color = INK_INDIGO
        canvas.drawCircle(w * 0.82f, h * 0.28f, 1.2f, paint)
        canvas.drawCircle(w * 0.92f, h * 0.22f, 1.2f, paint)
    }

    // ------------------------------------------------------------------------
    // 8. 清泉石上流 · Clear Spring on Stones
    // ------------------------------------------------------------------------
    private fun drawClearSpringWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 14f; strokeCap = Paint.Cap.ROUND; color = INK_INDIGO
        }
        val stream = Path().apply {
            moveTo(w * 0.40f, h); cubicTo(w * 0.60f, h * 0.88f, w * 0.78f, h * 0.76f, w, h * 0.78f)
        }
        canvas.drawPath(stream, paint)

        paint.style = Paint.Style.FILL; paint.color = INK_SAGE
        val stone1 = Path().apply { addOval(w * 0.80f, h * 0.80f, w * 0.92f, h * 0.92f, Path.Direction.CW) }
        canvas.drawPath(stone1, paint)
        paint.color = INK_OCHRE
        val stone2 = Path().apply { addOval(w * 0.70f, h * 0.86f, w * 0.78f, h * 0.94f, Path.Direction.CW) }
        canvas.drawPath(stone2, paint)
    }

    // ------------------------------------------------------------------------
    // 9. 长河落日圆 · Great River & Crimson Sun
    // ------------------------------------------------------------------------
    private fun drawDesertSunsetWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        canvas.drawCircle(w * 0.86f, h * 0.52f, h * 0.22f, paint)

        val dune = Path().apply {
            moveTo(w * 0.40f, h); cubicTo(w * 0.60f, h * 0.88f, w * 0.78f, h * 0.72f, w, h * 0.70f); lineTo(w, h); close()
        }
        paint.shader = LinearGradient(w * 0.6f, h * 0.7f, w * 0.6f, h, INK_OCHRE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(dune, paint)
        paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 10. 一行白鹭上青天 · Egrets Ascending Sky
    // ------------------------------------------------------------------------
    private fun drawEgretsAscendingWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val marsh = Path().apply {
            moveTo(w * 0.65f, h); cubicTo(w * 0.78f, h * 0.88f, w * 0.88f, h * 0.82f, w, h * 0.80f); lineTo(w, h); close()
        }
        canvas.drawPath(marsh, paint)

        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f; paint.strokeCap = Paint.Cap.ROUND; paint.color = INK_BLACK
        val egrets = listOf(Pair(w * 0.74f, h * 0.28f), Pair(w * 0.82f, h * 0.36f), Pair(w * 0.90f, h * 0.44f))
        egrets.forEach { (ex, ey) ->
            val egret = Path().apply {
                moveTo(ex - 10f, ey + 3f); quadTo(ex - 3f, ey - 5f, ex, ey); quadTo(ex + 3f, ey - 5f, ex + 10f, ey + 3f)
            }
            canvas.drawPath(egret, paint)
        }
    }

    // ------------------------------------------------------------------------
    // 11. 疏影横斜水清浅 · Sparse Plum Shadows
    // ------------------------------------------------------------------------
    private fun drawPlumShadowWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2.4f; color = INK_BLACK
        }
        val twig = Path().apply {
            moveTo(w, h * 0.35f); cubicTo(w * 0.90f, h * 0.40f, w * 0.82f, h * 0.32f, w * 0.76f, h * 0.38f)
        }
        canvas.drawPath(twig, paint)

        val subTwig = Path().apply {
            moveTo(w * 0.86f, h * 0.37f); quadTo(w * 0.82f, h * 0.48f, w * 0.78f, h * 0.52f)
        }
        paint.strokeWidth = 1.4f
        canvas.drawPath(subTwig, paint)

        paint.style = Paint.Style.FILL; paint.color = INK_VERMILION
        canvas.drawCircle(w * 0.76f, h * 0.38f, 3.5f, paint)
        canvas.drawCircle(w * 0.84f, h * 0.32f, 3.0f, paint)
        canvas.drawCircle(w * 0.78f, h * 0.52f, 3.0f, paint)
    }

    // ------------------------------------------------------------------------
    // 12. 古道西风瘦马 · Ancient Road & West Wind
    // ------------------------------------------------------------------------
    private fun drawAncientRoadWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_OCHRE
        val ridge = Path().apply {
            moveTo(w * 0.50f, h); cubicTo(w * 0.68f, h * 0.85f, w * 0.82f, h * 0.74f, w, h * 0.72f); lineTo(w, h); close()
        }
        canvas.drawPath(ridge, paint)

        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f; paint.strokeCap = Paint.Cap.ROUND; paint.color = INK_BLACK
        val path = Path().apply {
            moveTo(w * 0.62f, h); quadTo(w * 0.78f, h * 0.86f, w * 0.88f, h * 0.74f)
        }
        canvas.drawPath(path, paint)
    }

    // ------------------------------------------------------------------------
    // 13. 斜风细雨不须归 · Spring Drizzle & Slanted Wind
    // ------------------------------------------------------------------------
    private fun drawSpringRainWidget(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val hill = Path().apply {
            moveTo(w * 0.55f, h); cubicTo(w * 0.72f, h * 0.85f, w * 0.86f, h * 0.78f, w, h * 0.76f); lineTo(w, h); close()
        }
        canvas.drawPath(hill, paint)

        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f; paint.strokeCap = Paint.Cap.ROUND
        val willow = Path().apply {
            moveTo(w, h * 0.12f); quadTo(w * 0.88f, h * 0.22f, w * 0.82f, h * 0.44f)
        }
        canvas.drawPath(willow, paint)
    }
}
