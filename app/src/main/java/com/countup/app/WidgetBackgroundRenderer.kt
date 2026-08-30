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
 * Renders Chinese ink wash landscape motifs for all 30 themes matching the main app's active [BackgroundTheme].
 *
 * Characteristics:
 * - Pure Android Canvas/Path vector operations (zero external dependencies)
 * - Isotropic Geometric Coordinate System: celestial bodies (sun, moon), ripples, and scholar rocks
 *   use uniform aspect units `unit = minOf(w * 0.7f, 280f * (w / 480f))` and anchor to `(w, h)`
 *   (bottom-right corner), guaranteeing ZERO stretching or distortion when users expand the widget vertically.
 * - Soft, low-opacity mineral wash tones (12%–22%) ensuring 100% text contrast
 * - Seamless natural negative space (留白) expanding upward as widget height increases
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

            // 2. Draw active theme vector landscape in bottom-right corner with isotropic coordinates
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
                BackgroundTheme.LOTUS_DRAGONFLY -> drawLotusDragonflyWidget(canvas, w, h)
                BackgroundTheme.CRISP_SPRING_RAIN -> drawCrispSpringRainWidget(canvas, w, h)
                BackgroundTheme.SOLITARY_SAIL_RIVER -> drawSolitarySailRiverWidget(canvas, w, h)
                BackgroundTheme.OCEAN_MOON_TIDE -> drawOceanMoonTideWidget(canvas, w, h)
                BackgroundTheme.WILD_SKY_RIVER_MOON -> drawWildSkyRiverMoonWidget(canvas, w, h)
                BackgroundTheme.GREEN_HILLS_SAIL -> drawGreenHillsSailWidget(canvas, w, h)
                BackgroundTheme.STARS_FALL_RIVER_FLOW -> drawStarsFallRiverFlowWidget(canvas, w, h)
                BackgroundTheme.CLOUDS_COTTAGE -> drawCloudsCottageWidget(canvas, w, h)
                BackgroundTheme.WINE_SPRING_MOON -> drawWineSpringMoonWidget(canvas, w, h)
                BackgroundTheme.APRICOT_RAIN -> drawApricotRainWidget(canvas, w, h)
                BackgroundTheme.DEEP_FOREST_DEER -> drawDeepForestDeerWidget(canvas, w, h)
                BackgroundTheme.PEAR_BLOSSOM_WILLOW -> drawPearBlossomWillowWidget(canvas, w, h)
                BackgroundTheme.SPRING_WATER_SLEEP -> drawSpringWaterSleepWidget(canvas, w, h)
                BackgroundTheme.READING_LAMP_MOON -> drawReadingLampMoonWidget(canvas, w, h)
                BackgroundTheme.MOON_IN_HAND_WIND -> drawMoonInHandWindWidget(canvas, w, h)
                BackgroundTheme.MOSS_COURTYARD_PLANTAIN -> drawMossCourtyardPlantainWidget(canvas, w, h)
                BackgroundTheme.FISH_JUMPING_DUCKWEED -> drawFishJumpingDuckweedWidget(canvas, w, h)
                BackgroundTheme.AUTO_DAILY -> drawMountainWidget(canvas, w, h)
            }

            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    // Helper: Computes isotropic unit dimension clamped so tall widgets maintain 1:1 geometry
    private fun getIsotropicUnit(w: Float, h: Float): Float {
        val baseH = minOf(h, 280f * (w / 480f))
        return minOf(w * 0.75f, baseH)
    }

    // ------------------------------------------------------------------------
    // 1. 远山含黛 · Mountain Peaks
    // ------------------------------------------------------------------------
    private fun drawMountainWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ridge1 = Path().apply {
            moveTo(w - u * 0.95f, h)
            cubicTo(w - u * 0.70f, h - u * 0.40f, w - u * 0.45f, h - u * 0.60f, w - u * 0.22f, h - u * 0.70f)
            cubicTo(w - u * 0.12f, h - u * 0.75f, w - u * 0.05f, h - u * 0.62f, w, h - u * 0.68f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.75f, w - u * 0.5f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(ridge1, paint); paint.shader = null
        paint.color = INK_MUTED
        val ridge2 = Path().apply {
            moveTo(w - u * 0.65f, h)
            cubicTo(w - u * 0.48f, h - u * 0.32f, w - u * 0.30f, h - u * 0.50f, w - u * 0.15f, h - u * 0.54f)
            cubicTo(w - u * 0.08f, h - u * 0.56f, w - u * 0.03f, h - u * 0.46f, w, h - u * 0.48f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(ridge2, paint)
    }

    // ------------------------------------------------------------------------
    // 2. 平沙落雁 · Sand Dunes
    // ------------------------------------------------------------------------
    private fun drawSandDunesWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dune1 = Path().apply {
            moveTo(w - u * 0.90f, h)
            cubicTo(w - u * 0.65f, h - u * 0.22f, w - u * 0.42f, h - u * 0.48f, w - u * 0.20f, h - u * 0.52f)
            cubicTo(w - u * 0.10f, h - u * 0.54f, w - u * 0.05f, h - u * 0.42f, w, h - u * 0.46f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.6f, w - u * 0.5f, h, INK_OCHRE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(dune1, paint); paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 3. 烟波浩渺 · Calm Sea Horizon
    // ------------------------------------------------------------------------
    private fun drawSeaHorizonWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        // Isotropic perfect circle for Sun anchored to bottom-right
        canvas.drawCircle(w - u * 0.25f, h - u * 0.75f, u * 0.20f, paint)
        val water = Path().apply {
            moveTo(w - u * 0.95f, h)
            cubicTo(w - u * 0.65f, h - u * 0.32f, w - u * 0.32f, h - u * 0.42f, w, h - u * 0.45f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.45f, w - u * 0.5f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(water, paint); paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 4. 太湖石秀 · Solitary Taihu Scholar Stone
    // ------------------------------------------------------------------------
    private fun drawSolitaryIsleWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rockX = w - u * 0.32f
        val rockY = h - u * 0.70f
        val rockW = u * 0.24f
        val rockH = u * 0.65f
        val rockOutline = Path().apply {
            moveTo(rockX, rockY + rockH)
            cubicTo(rockX - rockW * 0.15f, rockY + rockH * 0.70f, rockX - rockW * 0.30f, rockY + rockH * 0.40f, rockX - rockW * 0.10f, rockY + rockH * 0.15f)
            cubicTo(rockX, rockY + rockH * 0.02f, rockX + rockW * 0.40f, rockY - rockH * 0.05f, rockX + rockW * 0.60f, rockY + rockH * 0.12f)
            cubicTo(rockX + rockW * 0.80f, rockY + rockH * 0.25f, rockX + rockW * 0.95f, rockY + rockH * 0.60f, rockX + rockW * 0.70f, rockY + rockH)
            close()
        }
        paint.shader = LinearGradient(rockX, rockY, rockX + rockW, rockY + rockH, INK_INDIGO, INK_OCHRE, Shader.TileMode.CLAMP)
        canvas.drawPath(rockOutline, paint); paint.shader = null
        paint.color = COLOR_PAPER
        canvas.drawCircle(rockX + rockW * 0.22f, rockY + rockH * 0.30f, rockW * 0.14f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f; paint.color = INK_BLACK
        canvas.drawPath(rockOutline, paint)
    }

    // ------------------------------------------------------------------------
    // 5. 柳浪闻莺 · Willow Leaves & Ripples
    // ------------------------------------------------------------------------
    private fun drawWillowLeavesWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f; paint.color = INK_BLACK
        val branch1 = Path().apply {
            moveTo(w, h - u * 1.10f)
            cubicTo(w - u * 0.15f, h - u * 0.95f, w - u * 0.26f, h - u * 0.70f, w - u * 0.22f, h - u * 0.35f)
        }
        canvas.drawPath(branch1, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_SAGE
        listOf(
            floatArrayOf(w - u * 0.18f, h - u * 0.85f),
            floatArrayOf(w - u * 0.25f, h - u * 0.72f),
            floatArrayOf(w - u * 0.23f, h - u * 0.54f),
        ).forEach { c ->
            canvas.drawOval(c[0] - 2.5f, c[1] - 5.5f, c[0] + 2.5f, c[1] + 5.5f, paint)
        }
    }

    // ------------------------------------------------------------------------
    // 6. 幽竹虚心 · Zen Bamboo Grove
    // ------------------------------------------------------------------------
    private fun drawZenBambooWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.5f; paint.strokeCap = Paint.Cap.ROUND
        val stalk = Path().apply {
            moveTo(w - u * 0.15f, h)
            lineTo(w - u * 0.14f, h - u * 1.0f)
        }
        canvas.drawPath(stalk, paint)
        paint.style = Paint.Style.FILL
        val leaf = Path().apply {
            moveTo(w - u * 0.15f, h - u * 0.75f)
            quadTo(w - u * 0.30f, h - u * 0.72f, w - u * 0.38f, h - u * 0.60f)
            quadTo(w - u * 0.26f, h - u * 0.64f, w - u * 0.15f, h - u * 0.75f)
        }
        canvas.drawPath(leaf, paint)
    }

    // ------------------------------------------------------------------------
    // 7. 满船清梦压星河 · Dream Boat
    // ------------------------------------------------------------------------
    private fun drawDreamBoatWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val water = Path().apply {
            moveTo(w - u * 0.85f, h)
            cubicTo(w - u * 0.55f, h - u * 0.25f, w - u * 0.32f, h - u * 0.45f, w, h - u * 0.45f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.4f, h - u * 0.5f, w - u * 0.4f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(water, paint); paint.shader = null
        paint.color = INK_BLACK
        val bx = w - u * 0.22f
        val by = h - u * 0.35f
        val boat = Path().apply {
            moveTo(bx - 22f, by)
            quadTo(bx, by + 7f, bx + 22f, by - 3f)
            quadTo(bx + 8f, by + 3f, bx - 22f, by)
        }
        canvas.drawPath(boat, paint)
    }

    // ------------------------------------------------------------------------
    // 8. 清泉石上流 · Clear Spring
    // ------------------------------------------------------------------------
    private fun drawClearSpringWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 14f; strokeCap = Paint.Cap.ROUND; color = INK_INDIGO }
        val stream = Path().apply {
            moveTo(w - u * 0.90f, h)
            cubicTo(w - u * 0.60f, h - u * 0.16f, w - u * 0.32f, h - u * 0.34f, w, h - u * 0.32f)
        }
        canvas.drawPath(stream, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_SAGE
        val stone1 = Path().apply { addOval(w - u * 0.30f, h - u * 0.28f, w - u * 0.12f, h - u * 0.12f, Path.Direction.CW) }
        canvas.drawPath(stone1, paint)
    }

    // ------------------------------------------------------------------------
    // 9. 长河落日圆 · Desert Sunset
    // ------------------------------------------------------------------------
    private fun drawDesertSunsetWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        // Isotropic perfect circle for desert sunset
        canvas.drawCircle(w - u * 0.22f, h - u * 0.68f, u * 0.24f, paint)
        val dune = Path().apply {
            moveTo(w - u * 0.90f, h)
            cubicTo(w - u * 0.60f, h - u * 0.16f, w - u * 0.32f, h - u * 0.38f, w, h - u * 0.42f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.42f, w - u * 0.5f, h, INK_OCHRE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(dune, paint); paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 10. 一行白鹭上青天 · Egrets Ascending
    // ------------------------------------------------------------------------
    private fun drawEgretsAscendingWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; strokeCap = Paint.Cap.ROUND; color = INK_BLACK }
        listOf(
            Pair(w - u * 0.40f, h - u * 0.95f),
            Pair(w - u * 0.28f, h - u * 0.85f),
            Pair(w - u * 0.15f, h - u * 0.72f),
        ).forEach { (ex, ey) ->
            val egret = Path().apply {
                moveTo(ex - 10f, ey + 3f)
                quadTo(ex - 3f, ey - 5f, ex, ey)
                quadTo(ex + 3f, ey - 5f, ex + 10f, ey + 3f)
            }
            canvas.drawPath(egret, paint)
        }
    }

    // ------------------------------------------------------------------------
    // 11. 疏影横斜水清浅 · Plum Shadow
    // ------------------------------------------------------------------------
    private fun drawPlumShadowWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f; color = INK_BLACK }
        val twig = Path().apply {
            moveTo(w, h - u * 0.90f)
            cubicTo(w - u * 0.15f, h - u * 0.82f, w - u * 0.28f, h - u * 0.95f, w - u * 0.38f, h - u * 0.86f)
        }
        canvas.drawPath(twig, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_VERMILION
        canvas.drawCircle(w - u * 0.38f, h - u * 0.86f, 3.5f, paint)
        canvas.drawCircle(w - u * 0.25f, h - u * 0.95f, 3.0f, paint)
    }

    // ------------------------------------------------------------------------
    // 12. 古道西风瘦马 · Ancient Road
    // ------------------------------------------------------------------------
    private fun drawAncientRoadWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_OCHRE
        val ridge = Path().apply {
            moveTo(w - u * 0.75f, h)
            cubicTo(w - u * 0.48f, h - u * 0.22f, w - u * 0.28f, h - u * 0.38f, w, h - u * 0.40f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(ridge, paint)
    }

    // ------------------------------------------------------------------------
    // 13. 斜风细雨不须归 · Spring Rain
    // ------------------------------------------------------------------------
    private fun drawSpringRainWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val hill = Path().apply {
            moveTo(w - u * 0.70f, h)
            cubicTo(w - u * 0.42f, h - u * 0.22f, w - u * 0.22f, h - u * 0.32f, w, h - u * 0.35f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(hill, paint)
    }

    // ------------------------------------------------------------------------
    // 14. 小荷才露尖尖角，早有蜻蜓立上头 · Lotus & Dragonfly
    // ------------------------------------------------------------------------
    private fun drawLotusDragonflyWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.0f; paint.color = INK_SAGE
        val stem = Path().apply {
            moveTo(w - u * 0.22f, h)
            quadTo(w - u * 0.25f, h - u * 0.42f, w - u * 0.28f, h - u * 0.74f)
        }
        canvas.drawPath(stem, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_VERMILION
        canvas.drawCircle(w - u * 0.28f, h - u * 0.76f, 3.5f, paint)
        paint.color = INK_BLACK
        canvas.drawCircle(w - u * 0.30f, h - u * 0.83f, 1.8f, paint)
    }

    // ------------------------------------------------------------------------
    // 15. 天街小雨润如酥 · Crisp Spring Rain
    // ------------------------------------------------------------------------
    private fun drawCrispSpringRainWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val mound = Path().apply {
            moveTo(w - u * 0.60f, h)
            cubicTo(w - u * 0.38f, h - u * 0.20f, w - u * 0.18f, h - u * 0.26f, w, h - u * 0.24f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(mound, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.0f; paint.color = INK_INDIGO
        for (i in 0 until 8) {
            val rx = w - u * (0.60f - (i % 4) * 0.15f)
            val ry = h - u * (1.05f - (i / 4) * 0.42f)
            canvas.drawLine(rx, ry, rx - 6f, ry + 16f, paint)
        }
    }

    // ------------------------------------------------------------------------
    // 16. 孤帆远影碧空尽，唯见长江天际流 · Solitary Sail
    // ------------------------------------------------------------------------
    private fun drawSolitarySailRiverWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val river = Path().apply {
            moveTo(w - u * 0.95f, h)
            cubicTo(w - u * 0.60f, h - u * 0.32f, w - u * 0.30f, h - u * 0.60f, w, h - u * 0.68f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.65f, w - u * 0.5f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(river, paint); paint.shader = null
        paint.color = INK_BLACK
        val sx = w - u * 0.18f
        val sy = h - u * 0.66f
        val sail = Path().apply {
            moveTo(sx, sy)
            lineTo(sx + 5f, sy + 8f)
            lineTo(sx - 3f, sy + 8f)
            close()
        }
        canvas.drawPath(sail, paint)
    }

    // ------------------------------------------------------------------------
    // 17. 海上明月共潮生 · Ocean Moon & Rising Tide
    // ------------------------------------------------------------------------
    private fun drawOceanMoonTideWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        // Isotropic perfect moon
        canvas.drawCircle(w - u * 0.22f, h - u * 0.85f, u * 0.22f, paint)
        val tide = Path().apply {
            moveTo(w - u * 0.85f, h)
            cubicTo(w - u * 0.55f, h - u * 0.28f, w - u * 0.28f, h - u * 0.45f, w, h - u * 0.48f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.48f, w - u * 0.5f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(tide, paint); paint.shader = null
    }

    // ------------------------------------------------------------------------
    // 18. 野旷天低树，江清月近人 · Wild Horizon & River Moon
    // ------------------------------------------------------------------------
    private fun drawWildSkyRiverMoonWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_OCHRE
        // Isotropic perfect moon
        canvas.drawCircle(w - u * 0.35f, h - u * 0.22f, u * 0.14f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f; paint.color = INK_BLACK
        val tree = Path().apply {
            moveTo(w - u * 0.15f, h - u * 0.34f)
            lineTo(w - u * 0.15f, h - u * 0.58f)
            quadTo(w - u * 0.24f, h - u * 0.66f, w - u * 0.15f, h - u * 0.58f)
        }
        canvas.drawPath(tree, paint)
    }

    // ------------------------------------------------------------------------
    // 19. 两岸青山相对出，孤帆一片日边来 · Green Hills & Sail
    // ------------------------------------------------------------------------
    private fun drawGreenHillsSailWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_VERMILION
        canvas.drawCircle(w - u * 0.25f, h - u * 0.88f, u * 0.16f, paint)
        paint.color = INK_SAGE
        val cliff = Path().apply {
            moveTo(w - u * 0.60f, h)
            cubicTo(w - u * 0.45f, h - u * 0.45f, w - u * 0.32f, h - u * 0.65f, w - u * 0.26f, h - u * 0.50f)
            lineTo(w - u * 0.26f, h)
            close()
        }
        canvas.drawPath(cliff, paint)
        paint.color = INK_INDIGO
        val cliff2 = Path().apply {
            moveTo(w - u * 0.22f, h)
            cubicTo(w - u * 0.18f, h - u * 0.60f, w - u * 0.08f, h - u * 0.74f, w, h - u * 0.64f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(cliff2, paint)
    }

    // ------------------------------------------------------------------------
    // 20. 星垂平野阔，月涌大江流 · Falling Stars & River Flow
    // ------------------------------------------------------------------------
    private fun drawStarsFallRiverFlowWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val surge = Path().apply {
            moveTo(w - u * 0.90f, h)
            cubicTo(w - u * 0.60f, h - u * 0.32f, w - u * 0.30f, h - u * 0.20f, w, h - u * 0.50f)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(w - u * 0.5f, h - u * 0.50f, w - u * 0.5f, h, INK_INDIGO, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(surge, paint); paint.shader = null
        paint.color = INK_INDIGO
        canvas.drawCircle(w - u * 0.40f, h - u * 0.95f, 1.8f, paint)
        canvas.drawCircle(w - u * 0.22f, h - u * 1.05f, 1.8f, paint)
    }

    // ------------------------------------------------------------------------
    // 21. 白云生处有人家 · Clouds & Cottage
    // ------------------------------------------------------------------------
    private fun drawCloudsCottageWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_INDIGO
        val ridge = Path().apply {
            moveTo(w - u * 0.75f, h)
            cubicTo(w - u * 0.48f, h - u * 0.34f, w - u * 0.26f, h - u * 0.58f, w, h - u * 0.54f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(ridge, paint)
        paint.color = INK_OCHRE
        val rx = w - u * 0.30f
        val ry = h - u * 0.48f
        val roof = Path().apply {
            moveTo(rx, ry)
            lineTo(rx + 10f, ry - 6f)
            lineTo(rx + 20f, ry)
            close()
        }
        canvas.drawPath(roof, paint)
    }

    // ------------------------------------------------------------------------
    // 22. 酒浓春入梦，窗破月寻人 · Window Moon
    // ------------------------------------------------------------------------
    private fun drawWineSpringMoonWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f; paint.color = INK_BLACK
        val wx = w - u * 0.32f
        val wy = h - u * 0.88f
        canvas.drawRect(wx, wy, wx + 40f, wy + 40f, paint)
        canvas.drawLine(wx + 20f, wy, wx + 20f, wy + 40f, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_VERMILION
        canvas.drawCircle(wx + 20f, wy + 20f, 16f, paint)
    }

    // ------------------------------------------------------------------------
    // 23. 红杏开时，一霎清明雨 · Apricot Rain
    // ------------------------------------------------------------------------
    private fun drawApricotRainWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f; paint.color = INK_BLACK
        val branch = Path().apply {
            moveTo(w, h - u * 0.92f)
            quadTo(w - u * 0.18f, h - u * 0.84f, w - u * 0.30f, h - u * 0.76f)
        }
        canvas.drawPath(branch, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_VERMILION
        canvas.drawCircle(w - u * 0.30f, h - u * 0.76f, 4f, paint)
        canvas.drawCircle(w - u * 0.18f, h - u * 0.86f, 3.5f, paint)
    }

    // ------------------------------------------------------------------------
    // 24. 树深时见鹿 · Deep Forest Deer
    // ------------------------------------------------------------------------
    private fun drawDeepForestDeerWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_BLACK; paint.strokeWidth = 3f; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w - u * 0.28f, h, w - u * 0.30f, h - u * 0.70f, paint)
        canvas.drawLine(w - u * 0.12f, h, w - u * 0.14f, h - u * 0.82f, paint)
        paint.color = INK_OCHRE
        canvas.drawCircle(w - u * 0.24f, h - u * 0.36f, 6f, paint)
    }

    // ------------------------------------------------------------------------
    // 25. 梨花淡白柳深青，柳絮飞时花满城 · Pear Blossom & Willow
    // ------------------------------------------------------------------------
    private fun drawPearBlossomWillowWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f; paint.color = INK_SAGE
        val willow = Path().apply {
            moveTo(w, h - u * 1.05f)
            quadTo(w - u * 0.18f, h - u * 0.84f, w - u * 0.24f, h - u * 0.42f)
        }
        canvas.drawPath(willow, paint)
        paint.style = Paint.Style.FILL; paint.color = COLOR_PAPER
        canvas.drawCircle(w - u * 0.26f, h - u * 0.70f, 3.5f, paint)
        canvas.drawCircle(w - u * 0.18f, h - u * 0.82f, 3.0f, paint)
    }

    // ------------------------------------------------------------------------
    // 26. 半篙春水一蓑烟，抱月怀中枕斗眠 · Spring Water Sleep
    // ------------------------------------------------------------------------
    private fun drawSpringWaterSleepWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_BLACK; paint.strokeWidth = 1.6f
        canvas.drawLine(w - u * 0.42f, h - u * 0.62f, w - u * 0.18f, h, paint)
        val bx = w - u * 0.26f
        val by = h - u * 0.28f
        val boat = Path().apply {
            moveTo(bx - 16f, by)
            quadTo(bx, by + 5f, bx + 16f, by - 2f)
            close()
        }
        canvas.drawPath(boat, paint)
    }

    // ------------------------------------------------------------------------
    // 27. 吹灭读书灯，一身都是月 · Extinguished Lamp & Moon
    // ------------------------------------------------------------------------
    private fun drawReadingLampMoonWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_INDIGO
        canvas.drawCircle(w - u * 0.26f, h - u * 0.64f, u * 0.28f, paint)
        paint.color = INK_BLACK
        val lx = w - u * 0.20f
        val ly = h - u * 0.25f
        canvas.drawRect(lx - 5f, ly - 8f, lx + 5f, ly, paint)
    }

    // ------------------------------------------------------------------------
    // 28. 揖让月在手，动摇风满怀 · Moon in Hand & Wind
    // ------------------------------------------------------------------------
    private fun drawMoonInHandWindWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f; paint.color = INK_INDIGO
        val rx = w - u * 0.26f
        val ry = h - u * 0.35f
        canvas.drawOval(rx - 22f, ry - 10f, rx + 22f, ry + 10f, paint)
        paint.style = Paint.Style.FILL; paint.color = INK_OCHRE
        canvas.drawCircle(rx, ry, 7f, paint)
    }

    // ------------------------------------------------------------------------
    // 29. 绿芜墙绕青苔院，中庭日淡芭蕉卷 · Plantain Leaf
    // ------------------------------------------------------------------------
    private fun drawMossCourtyardPlantainWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        val leaf = Path().apply {
            moveTo(w - u * 0.32f, h)
            cubicTo(w - u * 0.36f, h - u * 0.50f, w - u * 0.22f, h - u * 0.68f, w - u * 0.12f, h - u * 0.74f)
            cubicTo(w - u * 0.06f, h - u * 0.56f, w - u * 0.18f, h - u * 0.34f, w - u * 0.20f, h)
            close()
        }
        canvas.drawPath(leaf, paint)
    }

    // ------------------------------------------------------------------------
    // 30. 小鱼跳出绿萍中 · Leaping Fish in Duckweed
    // ------------------------------------------------------------------------
    private fun drawFishJumpingDuckweedWidget(canvas: Canvas, w: Float, h: Float) {
        val u = getIsotropicUnit(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = INK_SAGE
        canvas.drawOval(w - u * 0.42f, h - u * 0.25f, w - u * 0.26f, h - u * 0.16f, paint)
        canvas.drawOval(w - u * 0.24f, h - u * 0.28f, w - u * 0.08f, h - u * 0.20f, paint)
        paint.color = INK_VERMILION
        val fx = w - u * 0.30f
        val fy = h - u * 0.42f
        val fish = Path().apply {
            moveTo(fx - 10f, fy + 8f)
            quadTo(fx, fy - 10f, fx + 10f, fy + 4f)
            close()
        }
        canvas.drawPath(fish, paint)
    }
}
