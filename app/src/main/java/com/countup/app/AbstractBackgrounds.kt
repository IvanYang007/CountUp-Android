package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * 5 distinct Chinese Ink Painting (国画水墨意境) abstract background themes.
 * Core Design Principles:
 * - Landscape contours anchor seamlessly to screen borders (right & bottom)
 * - Visual weight concentrated in the bottom-right corner (右下聚景)
 * - Generous, airy negative space on the left (左侧大面积留白 · 计白当黑)
 * - No abrupt or rigid horizontal lines; organic curves and soft gradient washes only (水墨晕染渐变)
 */
enum class BackgroundTheme(val id: String, @get:StringRes val labelRes: Int) {
    AUTO_DAILY("auto_daily", R.string.bg_auto_daily),
    MOUNTAIN("mountain", R.string.bg_mountain),
    SAND_DUNES("sand_dunes", R.string.bg_sand_dunes),
    SEA_HORIZON("sea_horizon", R.string.bg_sea_horizon),
    SOLITARY_ISLE("solitary_isle", R.string.bg_solitary_isle),
    WILLOW_LEAVES("willow_leaves", R.string.bg_willow_leaves);

    fun next(): BackgroundTheme {
        val all = entries
        val nextIdx = (all.indexOf(this) + 1) % all.size
        return all[nextIdx]
    }

    companion object {
        fun fromId(id: String?): BackgroundTheme {
            return when (id) {
                "willow_leaves", "cold_river_snow", "misty_grove" -> WILLOW_LEAVES
                else -> entries.firstOrNull { it.id == id } ?: AUTO_DAILY
            }
        }
    }
}

/**
 * Resolves [theme] to one of the 5 concrete visual themes.
 * When set to [BackgroundTheme.AUTO_DAILY], cycles predictably based on [epochDay].
 */
fun resolveActiveTheme(theme: BackgroundTheme, epochDay: Long = LocalDate.now().toEpochDay()): BackgroundTheme {
    if (theme != BackgroundTheme.AUTO_DAILY) return theme
    val cycle = listOf(
        BackgroundTheme.MOUNTAIN,
        BackgroundTheme.SAND_DUNES,
        BackgroundTheme.SEA_HORIZON,
        BackgroundTheme.SOLITARY_ISLE,
        BackgroundTheme.WILLOW_LEAVES,
    )
    val index = Math.floorMod(epochDay, cycle.size.toLong()).toInt()
    return cycle[index]
}

// Chinese Ink & Mineral Pigment Palette (国画水墨与矿物色)
private val InkBlack = Color(0xFF1E2124)       // 浓墨 / 焦墨
private val InkMuted = Color(0xFF4A4E54)       // 淡墨 / 宿墨
private val InkOchre = Color(0xFFC48B58)       // 赭石 / 浅绛
private val InkVermilion = Color(0xFFD3523B)   // 朱砂 / 丹霞
private val InkIndigo = Color(0xFF4E6B7A)      // 花青 / 黛蓝
private val InkSage = Color(0xFF4A7C59)        // 柳绿 / 苔绿

/**
 * Renders the chosen abstract Zen/Ink Wash background behind content.
 */
fun Modifier.drawAbstractBackground(
    theme: BackgroundTheme,
    epochDay: Long = LocalDate.now().toEpochDay(),
): Modifier = this.drawBehind {
    val active = resolveActiveTheme(theme, epochDay)
    when (active) {
        BackgroundTheme.MOUNTAIN -> drawInkMountainTheme()
        BackgroundTheme.SAND_DUNES -> drawInkSandDunesTheme()
        BackgroundTheme.SEA_HORIZON -> drawInkSeaHorizonTheme()
        BackgroundTheme.SOLITARY_ISLE -> drawInkSolitaryIsleTheme()
        BackgroundTheme.WILLOW_LEAVES -> drawInkWillowLeavesTheme()
        BackgroundTheme.AUTO_DAILY -> drawInkMountainTheme()
    }
}

/**
 * 1. 水墨远山 · Misty Karst Peaks (右下聚景，线条接边缘，左侧留白)
 */
private fun DrawScope.drawInkMountainTheme() {
    val w = size.width
    val h = size.height

    // Dawn vermilion glow in upper right sky
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.11f), Color.Transparent),
            center = Offset(w * 0.84f, h * 0.20f),
            radius = 56.dp.toPx(),
        ),
        radius = 56.dp.toPx(),
        center = Offset(w * 0.84f, h * 0.20f),
    )

    // Distant birds soaring into left void
    val birds = Path().apply {
        moveTo(w * 0.65f, h * 0.24f)
        quadraticTo(w * 0.665f, h * 0.233f, w * 0.68f, h * 0.242f)
        moveTo(w * 0.72f, h * 0.27f)
        quadraticTo(w * 0.732f, h * 0.263f, w * 0.745f, h * 0.272f)
    }
    drawPath(birds, color = InkBlack.copy(alpha = 0.22f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))

    // Main soaring mountain peak (from bottom border to right border)
    val mainPeak = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.48f, h * 0.68f, w * 0.62f, h * 0.44f, w * 0.76f, h * 0.42f)
        cubicTo(w * 0.86f, h * 0.41f, w * 0.94f, h * 0.54f, w, h * 0.62f)
        lineTo(w, h)
        lineTo(w * 0.35f, h)
        close()
    }
    drawPath(
        path = mainPeak,
        brush = Brush.verticalGradient(
            colors = listOf(InkBlack.copy(alpha = 0.16f), InkBlack.copy(alpha = 0.01f)),
            startY = h * 0.42f,
            endY = h,
        ),
    )
    val mainRidgeLine = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.48f, h * 0.68f, w * 0.62f, h * 0.44f, w * 0.76f, h * 0.42f)
        cubicTo(w * 0.86f, h * 0.41f, w * 0.94f, h * 0.54f, w, h * 0.62f)
    }
    drawPath(
        path = mainRidgeLine,
        brush = Brush.linearGradient(
            colors = listOf(InkBlack.copy(alpha = 0.05f), InkBlack.copy(alpha = 0.28f)),
            start = Offset(w * 0.35f, h),
            end = Offset(w * 0.76f, h * 0.42f),
        ),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
    )

    // Layered foreground foothill (touches bottom border and right border)
    val frontFoothill = Path().apply {
        moveTo(w * 0.52f, h)
        cubicTo(w * 0.68f, h * 0.78f, w * 0.84f, h * 0.72f, w, h * 0.76f)
        lineTo(w, h)
        lineTo(w * 0.52f, h)
        close()
    }
    drawPath(
        path = frontFoothill,
        brush = Brush.linearGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.14f), InkIndigo.copy(alpha = 0.02f)),
            start = Offset(w, h * 0.76f),
            end = Offset(w * 0.52f, h),
        ),
    )
    val frontFoothillLine = Path().apply {
        moveTo(w * 0.52f, h)
        cubicTo(w * 0.68f, h * 0.78f, w * 0.84f, h * 0.72f, w, h * 0.76f)
    }
    drawPath(
        path = frontFoothillLine,
        color = InkIndigo.copy(alpha = 0.22f),
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
    )
}

/**
 * 2. 寒江平沙 · Sweeping Riverbanks & Wind Sands (纯斜向流畅沙脊，无突兀横线)
 * Sinuous ochre and ink riverbanks sweeping diagonally from the right border into the bottom border.
 */
private fun DrawScope.drawInkSandDunesTheme() {
    val w = size.width
    val h = size.height

    // Main sweeping sandbank entering diagonally from right border (w, h*0.46) to bottom border (w*0.48, h)
    val bankPath = Path().apply {
        moveTo(w, h * 0.46f)
        cubicTo(w * 0.76f, h * 0.55f, w * 0.54f, h * 0.66f, w * 0.44f, h * 0.82f)
        cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.96f, w * 0.48f, h)
        lineTo(w, h)
        lineTo(w, h * 0.46f)
        close()
    }
    drawPath(
        path = bankPath,
        brush = Brush.linearGradient(
            colors = listOf(InkOchre.copy(alpha = 0.20f), InkOchre.copy(alpha = 0.02f)),
            start = Offset(w, h * 0.46f),
            end = Offset(w * 0.42f, h * 0.85f),
        ),
    )
    // Sinuous diagonal contour stroke (pure diagonal sweep, no horizontal lines)
    val bankContour = Path().apply {
        moveTo(w, h * 0.46f)
        cubicTo(w * 0.76f, h * 0.55f, w * 0.54f, h * 0.66f, w * 0.44f, h * 0.82f)
        cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.96f, w * 0.48f, h)
    }
    drawPath(
        path = bankContour,
        brush = Brush.linearGradient(
            colors = listOf(InkOchre.copy(alpha = 0.32f), InkOchre.copy(alpha = 0.06f)),
            start = Offset(w, h * 0.46f),
            end = Offset(w * 0.44f, h * 0.82f),
        ),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
    )

    // Inner diagonal sand ridge sweep from right border (w, h*0.72) to bottom border (w*0.66, h)
    val innerDune = Path().apply {
        moveTo(w, h * 0.72f)
        cubicTo(w * 0.88f, h * 0.76f, w * 0.76f, h * 0.86f, w * 0.66f, h)
        lineTo(w, h)
        lineTo(w, h * 0.72f)
        close()
    }
    drawPath(
        path = innerDune,
        brush = Brush.verticalGradient(
            colors = listOf(InkMuted.copy(alpha = 0.16f), InkMuted.copy(alpha = 0.03f)),
            startY = h * 0.72f,
            endY = h,
        ),
    )
    val innerDuneLine = Path().apply {
        moveTo(w, h * 0.72f)
        cubicTo(w * 0.88f, h * 0.76f, w * 0.76f, h * 0.86f, w * 0.66f, h)
    }
    drawPath(innerDuneLine, color = InkMuted.copy(alpha = 0.24f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))

    // Faint diagonal sand shadow wash
    val shadowWash = Path().apply {
        moveTo(w, h * 0.86f)
        cubicTo(w * 0.92f, h * 0.90f, w * 0.84f, h * 0.96f, w * 0.80f, h)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = shadowWash,
        brush = Brush.linearGradient(
            colors = listOf(InkOchre.copy(alpha = 0.12f), Color.Transparent),
            start = Offset(w, h * 0.86f),
            end = Offset(w * 0.80f, h),
        ),
    )
}

/**
 * 3. 烟波浩渺 · Misty Tidal Swell & Distant Sail (右下聚景，线条接边缘，左侧留白)
 */
private fun DrawScope.drawInkSeaHorizonTheme() {
    val w = size.width
    val h = size.height

    // Water vapor wash anchored to bottom-right corner
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(w * 0.80f, h * 0.76f),
            radius = w * 0.45f,
        ),
        topLeft = Offset(w * 0.40f, h * 0.60f),
        size = Size(w * 0.80f, h * 0.32f),
    )

    // Distant tiny solitary sail flick (右侧孤帆远影)
    val sailPath = Path().apply {
        moveTo(w * 0.78f, h * 0.62f)
        lineTo(w * 0.78f, h * 0.655f)
        moveTo(w * 0.78f, h * 0.625f)
        quadraticTo(w * 0.793f, h * 0.638f, w * 0.78f, h * 0.65f)
        moveTo(w * 0.768f, h * 0.658f)
        lineTo(w * 0.792f, h * 0.658f)
    }
    drawPath(sailPath, color = InkBlack.copy(alpha = 0.30f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))

    // Main tidal wave sweep touching bottom border (w*0.40, h) and right border (w, h*0.70)
    val wavePath = Path().apply {
        moveTo(w * 0.40f, h)
        cubicTo(w * 0.58f, h * 0.74f, w * 0.82f, h * 0.78f, w, h * 0.70f)
        lineTo(w, h)
        lineTo(w * 0.40f, h)
        close()
    }
    drawPath(
        path = wavePath,
        brush = Brush.linearGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.13f), InkIndigo.copy(alpha = 0.02f)),
            start = Offset(w, h * 0.70f),
            end = Offset(w * 0.40f, h),
        ),
    )
    val waveLine = Path().apply {
        moveTo(w * 0.40f, h)
        cubicTo(w * 0.58f, h * 0.74f, w * 0.82f, h * 0.78f, w, h * 0.70f)
    }
    drawPath(
        path = waveLine,
        brush = Brush.horizontalGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.05f), InkIndigo.copy(alpha = 0.24f)),
            startX = w * 0.40f,
            endX = w,
        ),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
    )

    // Foreground shoreline tide touching bottom border (w*0.58, h) and right border (w, h*0.84)
    val foreTide = Path().apply {
        moveTo(w * 0.58f, h)
        cubicTo(w * 0.72f, h * 0.86f, w * 0.88f, h * 0.82f, w, h * 0.84f)
    }
    drawPath(foreTide, color = InkIndigo.copy(alpha = 0.20f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
}

/**
 * 4. 孤石清泉 · Zen Scholar Stone & Fading Ripples (右下孤石，圆融涟漪，无直横线)
 */
private fun DrawScope.drawInkSolitaryIsleTheme() {
    val w = size.width
    val h = size.height

    val rockBaseY = h * 0.84f
    val rockCenterX = w * 0.78f

    // Concentric ripple washes expanding from right stone to the left void
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(InkMuted.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(rockCenterX, rockBaseY),
            radius = w * 0.35f,
        ),
        topLeft = Offset(rockCenterX - w * 0.35f, rockBaseY - 10.dp.toPx()),
        size = Size(w * 0.70f, 20.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx()),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(rockCenterX, rockBaseY),
            radius = w * 0.50f,
        ),
        topLeft = Offset(rockCenterX - w * 0.50f, rockBaseY - 16.dp.toPx()),
        size = Size(w * 1.00f, 32.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )

    // Taihu Scholar Stone sitting quietly on the right
    val stonePath = Path().apply {
        moveTo(rockCenterX - 45.dp.toPx(), rockBaseY)
        cubicTo(
            rockCenterX - 38.dp.toPx(), rockBaseY - 32.dp.toPx(),
            rockCenterX - 18.dp.toPx(), rockBaseY - 52.dp.toPx(),
            rockCenterX + 8.dp.toPx(), rockBaseY - 48.dp.toPx(),
        )
        cubicTo(
            rockCenterX + 32.dp.toPx(), rockBaseY - 44.dp.toPx(),
            rockCenterX + 48.dp.toPx(), rockBaseY - 22.dp.toPx(),
            rockCenterX + 42.dp.toPx(), rockBaseY,
        )
        close()
    }
    drawPath(
        path = stonePath,
        brush = Brush.radialGradient(
            colors = listOf(InkBlack.copy(alpha = 0.22f), InkSage.copy(alpha = 0.12f), InkMuted.copy(alpha = 0.04f)),
            center = Offset(rockCenterX - 8.dp.toPx(), rockBaseY - 15.dp.toPx()),
            radius = 55.dp.toPx(),
        ),
    )
    drawPath(
        path = stonePath,
        brush = Brush.linearGradient(
            colors = listOf(InkBlack.copy(alpha = 0.32f), InkBlack.copy(alpha = 0.10f)),
            start = Offset(rockCenterX - 35.dp.toPx(), rockBaseY),
            end = Offset(rockCenterX + 35.dp.toPx(), rockBaseY - 48.dp.toPx()),
        ),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
    )

    // Moss dots on stone
    drawCircle(color = InkBlack.copy(alpha = 0.28f), radius = 2.dp.toPx(), center = Offset(rockCenterX - 12.dp.toPx(), rockBaseY - 36.dp.toPx()))
    drawCircle(color = InkBlack.copy(alpha = 0.22f), radius = 1.8.dp.toPx(), center = Offset(rockCenterX - 4.dp.toPx(), rockBaseY - 42.dp.toPx()))
    drawCircle(color = InkBlack.copy(alpha = 0.25f), radius = 2.2.dp.toPx(), center = Offset(rockCenterX + 15.dp.toPx(), rockBaseY - 30.dp.toPx()))

    // Shoreline curve anchoring bottom border (w*0.62, h) to right border (w, h*0.92)
    val shoreLine = Path().apply {
        moveTo(w * 0.62f, h)
        cubicTo(w * 0.74f, h * 0.94f, w * 0.88f, h * 0.90f, w, h * 0.92f)
    }
    drawPath(shoreLine, color = InkMuted.copy(alpha = 0.16f), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
}

/**
 * 5. 柳影拂风 · Willow Leaves (写意水墨垂柳，柳丝轻扬，秀叶婆娑，右下聚景，左侧留白)
 */
private fun DrawScope.drawInkWillowLeavesTheme() {
    val w = size.width
    val h = size.height

    // Spring water wash in bottom right corner (右下角春水微澜，接屏幕边缘)
    val springWater = Path().apply {
        moveTo(w * 0.46f, h)
        cubicTo(w * 0.62f, h * 0.82f, w * 0.84f, h * 0.84f, w, h * 0.76f)
        lineTo(w, h)
        lineTo(w * 0.46f, h)
        close()
    }
    drawPath(
        path = springWater,
        brush = Brush.linearGradient(
            colors = listOf(InkSage.copy(alpha = 0.12f), InkSage.copy(alpha = 0.01f)),
            start = Offset(w, h * 0.76f),
            end = Offset(w * 0.46f, h),
        ),
    )
    val waterRipples = Path().apply {
        moveTo(w * 0.46f, h)
        cubicTo(w * 0.62f, h * 0.82f, w * 0.84f, h * 0.84f, w, h * 0.76f)
    }
    drawPath(waterRipples, color = InkSage.copy(alpha = 0.20f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))

    // Secondary ripple line
    val waterRipples2 = Path().apply {
        moveTo(w * 0.60f, h)
        cubicTo(w * 0.74f, h * 0.90f, w * 0.88f, h * 0.88f, w, h * 0.86f)
    }
    drawPath(waterRipples2, color = InkSage.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

    // === Weeping Willow Branches (垂柳枝条 · 从右侧边缘优雅垂下) ===
    // Main Branch 1 (Upper right)
    val stem1 = Path().apply {
        moveTo(w, h * 0.42f)
        cubicTo(w * 0.86f, h * 0.48f, w * 0.78f, h * 0.60f, w * 0.74f, h * 0.78f)
    }
    drawPath(
        path = stem1,
        brush = Brush.linearGradient(
            colors = listOf(InkBlack.copy(alpha = 0.35f), InkMuted.copy(alpha = 0.15f)),
            start = Offset(w, h * 0.42f),
            end = Offset(w * 0.74f, h * 0.78f),
        ),
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
    )

    // Branch 2 (Mid-right drooping branch)
    val stem2 = Path().apply {
        moveTo(w, h * 0.55f)
        cubicTo(w * 0.90f, h * 0.62f, w * 0.82f, h * 0.72f, w * 0.80f, h * 0.86f)
    }
    drawPath(
        path = stem2,
        brush = Brush.linearGradient(
            colors = listOf(InkBlack.copy(alpha = 0.30f), InkMuted.copy(alpha = 0.12f)),
            start = Offset(w, h * 0.55f),
            end = Offset(w * 0.80f, h * 0.86f),
        ),
        style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
    )

    // Branch 3 (Lower right graceful tender branch)
    val stem3 = Path().apply {
        moveTo(w, h * 0.68f)
        cubicTo(w * 0.94f, h * 0.74f, w * 0.88f, h * 0.82f, w * 0.86f, h * 0.92f)
    }
    drawPath(
        path = stem3,
        brush = Brush.linearGradient(
            colors = listOf(InkBlack.copy(alpha = 0.25f), InkMuted.copy(alpha = 0.10f)),
            start = Offset(w, h * 0.68f),
            end = Offset(w * 0.86f, h * 0.92f),
        ),
        style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round),
    )

    // === Calligraphic Willow Leaves (写意细长柳叶) ===
    // Helper function to draw an organic willow leaf with tapering ends
    fun drawWillowLeaf(startX: Float, startY: Float, tipX: Float, tipY: Float, widthDp: Float, alpha: Float, isGreen: Boolean = true) {
        val leaf = Path().apply {
            moveTo(startX, startY)
            val midX = (startX + tipX) / 2f
            val midY = (startY + tipY) / 2f
            val perpX = -(tipY - startY) * 0.22f
            val perpY = (tipX - startX) * 0.22f
            quadraticTo(midX + perpX, midY + perpY, tipX, tipY)
            quadraticTo(midX - perpX * 0.4f, midY - perpY * 0.4f, startX, startY)
            close()
        }
        val col = if (isGreen) InkSage else InkBlack
        drawPath(leaf, color = col.copy(alpha = alpha))
    }

    // Leaf cluster on Stem 1
    drawWillowLeaf(w * 0.92f, h * 0.46f, w * 0.89f, h * 0.50f, 3f, 0.32f)
    drawWillowLeaf(w * 0.86f, h * 0.51f, w * 0.82f, h * 0.56f, 3.5f, 0.34f)
    drawWillowLeaf(w * 0.82f, h * 0.57f, w * 0.77f, h * 0.62f, 3.2f, 0.30f)
    drawWillowLeaf(w * 0.78f, h * 0.64f, w * 0.73f, h * 0.70f, 3f, 0.28f)
    drawWillowLeaf(w * 0.75f, h * 0.72f, w * 0.71f, h * 0.77f, 2.5f, 0.24f)
    drawWillowLeaf(w * 0.74f, h * 0.78f, w * 0.72f, h * 0.83f, 2.2f, 0.20f)

    // Leaf cluster on Stem 2
    drawWillowLeaf(w * 0.94f, h * 0.58f, w * 0.90f, h * 0.63f, 3.2f, 0.30f)
    drawWillowLeaf(w * 0.88f, h * 0.65f, w * 0.84f, h * 0.71f, 3.5f, 0.32f)
    drawWillowLeaf(w * 0.83f, h * 0.74f, w * 0.79f, h * 0.80f, 3.0f, 0.26f)
    drawWillowLeaf(w * 0.81f, h * 0.81f, w * 0.78f, h * 0.87f, 2.4f, 0.22f)

    // Leaf cluster on Stem 3
    drawWillowLeaf(w * 0.95f, h * 0.71f, w * 0.91f, h * 0.76f, 2.8f, 0.26f)
    drawWillowLeaf(w * 0.90f, h * 0.78f, w * 0.87f, h * 0.84f, 3.0f, 0.28f)
    drawWillowLeaf(w * 0.87f, h * 0.86f, w * 0.84f, h * 0.92f, 2.4f, 0.22f)

    // A few delicate floating willow leaves drifting into the left spring breeze (飘零柳叶随风)
    drawWillowLeaf(w * 0.68f, h * 0.76f, w * 0.64f, h * 0.78f, 2.2f, 0.22f, isGreen = true)
    drawWillowLeaf(w * 0.60f, h * 0.82f, w * 0.56f, h * 0.835f, 1.8f, 0.18f, isGreen = true)
}
