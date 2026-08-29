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
 * 13 Classical Chinese Poetic & Zen Landscape (国画诗词水墨意境) Abstract Background Themes.
 *
 * Core Design Principles:
 * - Landscape contours anchor seamlessly to screen borders (right & bottom).
 * - Visual weight concentrated in the bottom-right corner (右下聚景).
 * - Generous, airy negative space on the left (左侧大面积留白 · 计白当黑) to keep habit cards legible.
 * - Sinuous Bézier curves and mineral gradient washes (12%–22% opacity) ensuring 100% text contrast.
 */
enum class BackgroundTheme(val id: String, @get:StringRes val labelRes: Int) {
    AUTO_DAILY("auto_daily", R.string.bg_auto_daily),
    MOUNTAIN("mountain", R.string.bg_mountain),
    SAND_DUNES("sand_dunes", R.string.bg_sand_dunes),
    SEA_HORIZON("sea_horizon", R.string.bg_sea_horizon),
    SOLITARY_ISLE("solitary_isle", R.string.bg_solitary_isle),
    WILLOW_LEAVES("willow_leaves", R.string.bg_willow_leaves),
    ZEN_BAMBOO("zen_bamboo", R.string.bg_zen_bamboo),
    DREAM_BOAT("dream_boat", R.string.bg_dream_boat),
    CLEAR_SPRING("clear_spring", R.string.bg_clear_spring),
    DESERT_SUNSET("desert_sunset", R.string.bg_desert_sunset),
    EGRETS_ASCENDING("egrets_ascending", R.string.bg_egrets_ascending),
    PLUM_SHADOW("plum_shadow", R.string.bg_plum_shadow),
    ANCIENT_ROAD("ancient_road", R.string.bg_ancient_road),
    SPRING_RAIN("spring_rain", R.string.bg_spring_rain);

    fun next(): BackgroundTheme {
        val all = entries
        val nextIdx = (all.indexOf(this) + 1) % all.size
        return all[nextIdx]
    }

    companion object {
        fun fromId(id: String?): BackgroundTheme {
            return when (id) {
                "cold_river_snow" -> WILLOW_LEAVES
                "misty_grove" -> ZEN_BAMBOO
                else -> entries.firstOrNull { it.id == id } ?: AUTO_DAILY
            }
        }
    }
}

/**
 * Resolves [theme] to one of the 13 concrete poetic themes.
 * When set to [BackgroundTheme.AUTO_DAILY], cycles predictably based on [epochDay] across all 13 daily landscapes.
 */
fun resolveActiveTheme(theme: BackgroundTheme, epochDay: Long = LocalDate.now().toEpochDay()): BackgroundTheme {
    if (theme != BackgroundTheme.AUTO_DAILY) return theme
    val cycle = listOf(
        BackgroundTheme.MOUNTAIN,
        BackgroundTheme.SAND_DUNES,
        BackgroundTheme.SEA_HORIZON,
        BackgroundTheme.SOLITARY_ISLE,
        BackgroundTheme.WILLOW_LEAVES,
        BackgroundTheme.ZEN_BAMBOO,
        BackgroundTheme.DREAM_BOAT,
        BackgroundTheme.CLEAR_SPRING,
        BackgroundTheme.DESERT_SUNSET,
        BackgroundTheme.EGRETS_ASCENDING,
        BackgroundTheme.PLUM_SHADOW,
        BackgroundTheme.ANCIENT_ROAD,
        BackgroundTheme.SPRING_RAIN,
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
        BackgroundTheme.ZEN_BAMBOO -> drawInkZenBambooTheme()
        BackgroundTheme.DREAM_BOAT -> drawDreamBoatTheme()
        BackgroundTheme.CLEAR_SPRING -> drawClearSpringTheme()
        BackgroundTheme.DESERT_SUNSET -> drawDesertSunsetTheme()
        BackgroundTheme.EGRETS_ASCENDING -> drawEgretsAscendingTheme()
        BackgroundTheme.PLUM_SHADOW -> drawPlumShadowTheme()
        BackgroundTheme.ANCIENT_ROAD -> drawAncientRoadTheme()
        BackgroundTheme.SPRING_RAIN -> drawSpringRainTheme()
        BackgroundTheme.AUTO_DAILY -> drawInkMountainTheme()
    }
}

// ============================================================================
// 1. 远山含黛 · Karst Mountain Peaks
// ============================================================================
private fun DrawScope.drawInkMountainTheme() {
    val w = size.width
    val h = size.height

    // Dawn vermilion glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.11f), Color.Transparent),
            center = Offset(w * 0.84f, h * 0.20f),
            radius = 56.dp.toPx(),
        ),
        radius = 56.dp.toPx(),
        center = Offset(w * 0.84f, h * 0.20f),
    )

    // Main mountain peak
    val mainPeak = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.48f, h * 0.68f, w * 0.62f, h * 0.44f, w * 0.76f, h * 0.42f)
        cubicTo(w * 0.86f, h * 0.41f, w * 0.94f, h * 0.54f, w, h * 0.62f)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = mainPeak,
        brush = Brush.linearGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.16f), Color.Transparent),
            start = Offset(w * 0.6f, h * 0.42f),
            end = Offset(w * 0.6f, h),
        ),
    )
    val ridgeLine = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.48f, h * 0.68f, w * 0.62f, h * 0.44f, w * 0.76f, h * 0.42f)
        cubicTo(w * 0.86f, h * 0.41f, w * 0.94f, h * 0.54f, w, h * 0.62f)
    }
    drawPath(ridgeLine, color = InkBlack.copy(alpha = 0.24f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// 2. 平沙落雁 · Sinuous Sand Dunes
// ============================================================================
private fun DrawScope.drawInkSandDunesTheme() {
    val w = size.width
    val h = size.height

    val dune = Path().apply {
        moveTo(w, h * 0.46f)
        cubicTo(w * 0.76f, h * 0.55f, w * 0.54f, h * 0.66f, w * 0.44f, h * 0.82f)
        cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.96f, w * 0.48f, h)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = dune,
        brush = Brush.linearGradient(
            colors = listOf(InkOchre.copy(alpha = 0.20f), Color.Transparent),
            start = Offset(w, h * 0.46f),
            end = Offset(w * 0.42f, h * 0.85f),
        ),
    )
    val duneLine = Path().apply {
        moveTo(w, h * 0.46f)
        cubicTo(w * 0.76f, h * 0.55f, w * 0.54f, h * 0.66f, w * 0.44f, h * 0.82f)
        cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.96f, w * 0.48f, h)
    }
    drawPath(duneLine, color = InkOchre.copy(alpha = 0.32f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// 3. 烟波浩渺 · Calm Sea Horizon
// ============================================================================
private fun DrawScope.drawInkSeaHorizonTheme() {
    val w = size.width
    val h = size.height

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(w * 0.75f, h * 0.45f),
            radius = 50.dp.toPx(),
        ),
        radius = 50.dp.toPx(),
        center = Offset(w * 0.75f, h * 0.45f),
    )

    val sea = Path().apply {
        moveTo(w * 0.25f, h)
        cubicTo(w * 0.50f, h * 0.85f, w * 0.70f, h * 0.65f, w, h * 0.68f)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = sea,
        brush = Brush.linearGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.18f), Color.Transparent),
            start = Offset(w * 0.6f, h * 0.65f),
            end = Offset(w * 0.6f, h),
        ),
    )
}

// ============================================================================
// 4. 太湖石秀 · Solitary Taihu Scholar Stone
// ============================================================================
private fun DrawScope.drawInkSolitaryIsleTheme() {
    val w = size.width
    val h = size.height

    val rock = Path().apply {
        moveTo(w * 0.70f, h)
        cubicTo(w * 0.65f, h * 0.82f, w * 0.72f, h * 0.68f, w * 0.80f, h * 0.65f)
        cubicTo(w * 0.88f, h * 0.62f, w * 0.94f, h * 0.78f, w * 0.92f, h)
        close()
    }
    drawPath(rock, color = InkSage.copy(alpha = 0.22f))
    drawPath(rock, color = InkBlack.copy(alpha = 0.35f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// 5. 柳浪闻莺 · Willow Leaves & Spring Ripples
// ============================================================================
private fun DrawScope.drawInkWillowLeavesTheme() {
    val w = size.width
    val h = size.height

    val stem = Path().apply {
        moveTo(w, h * 0.35f)
        cubicTo(w * 0.86f, h * 0.45f, w * 0.78f, h * 0.60f, w * 0.74f, h * 0.75f)
    }
    drawPath(stem, color = InkBlack.copy(alpha = 0.30f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))

    val leaves = listOf(
        Offset(w * 0.92f, h * 0.40f),
        Offset(w * 0.86f, h * 0.48f),
        Offset(w * 0.80f, h * 0.58f),
        Offset(w * 0.75f, h * 0.70f),
    )
    leaves.forEach { pt ->
        drawOval(color = InkSage.copy(alpha = 0.32f), topLeft = pt, size = Size(14.dp.toPx(), 6.dp.toPx()))
    }
}

// ============================================================================
// 6. 幽竹虚心 · Zen Bamboo Grove
// ============================================================================
private fun DrawScope.drawInkZenBambooTheme() {
    val w = size.width
    val h = size.height

    // Bamboo stalk 1
    val stalk1 = Path().apply {
        moveTo(w * 0.86f, h); lineTo(w * 0.88f, h * 0.35f)
    }
    drawPath(stalk1, color = InkSage.copy(alpha = 0.35f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

    // Bamboo stalk 2
    val stalk2 = Path().apply {
        moveTo(w * 0.94f, h); lineTo(w * 0.95f, h * 0.28f)
    }
    drawPath(stalk2, color = InkBlack.copy(alpha = 0.30f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

    // Bamboo leaves
    val bLeaves = listOf(
        Offset(w * 0.84f, h * 0.45f),
        Offset(w * 0.82f, h * 0.52f),
        Offset(w * 0.90f, h * 0.38f),
        Offset(w * 0.88f, h * 0.60f),
    )
    bLeaves.forEach { pt ->
        val lf = Path().apply {
            moveTo(pt.x, pt.y)
            quadraticTo(pt.x - 18.dp.toPx(), pt.y + 4.dp.toPx(), pt.x - 28.dp.toPx(), pt.y + 14.dp.toPx())
            quadraticTo(pt.x - 14.dp.toPx(), pt.y + 10.dp.toPx(), pt.x, pt.y)
        }
        drawPath(lf, color = InkSage.copy(alpha = 0.38f))
    }
}

// ============================================================================
// 7. 满船清梦压星河 · Dream Boat on Milky Way
// ============================================================================
private fun DrawScope.drawDreamBoatTheme() {
    val w = size.width
    val h = size.height

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.82f, h * 0.20f),
            radius = 64.dp.toPx(),
        ),
        radius = 64.dp.toPx(),
        center = Offset(w * 0.82f, h * 0.20f),
    )

    listOf(
        Offset(w * 0.72f, h * 0.14f), Offset(w * 0.86f, h * 0.16f),
        Offset(w * 0.68f, h * 0.25f), Offset(w * 0.82f, h * 0.28f), Offset(w * 0.92f, h * 0.22f),
    ).forEach { pos ->
        drawCircle(color = InkIndigo.copy(alpha = 0.25f), radius = 1.5.dp.toPx(), center = pos)
    }

    val water = Path().apply {
        moveTo(w * 0.30f, h)
        cubicTo(w * 0.50f, h * 0.82f, w * 0.70f, h * 0.68f, w * 0.88f, h * 0.60f)
        cubicTo(w * 0.94f, h * 0.58f, w * 0.98f, h * 0.65f, w, h * 0.62f)
        lineTo(w, h); close()
    }
    drawPath(water, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.16f), Color.Transparent), start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h)))

    val bx = w * 0.76f; val by = h * 0.68f
    val boat = Path().apply {
        moveTo(bx - 32.dp.toPx(), by)
        quadraticTo(bx, by + 10.dp.toPx(), bx + 32.dp.toPx(), by - 4.dp.toPx())
        quadraticTo(bx + 12.dp.toPx(), by + 4.dp.toPx(), bx - 32.dp.toPx(), by)
    }
    drawPath(boat, color = InkBlack.copy(alpha = 0.40f))

    drawLine(color = InkBlack.copy(alpha = 0.35f), start = Offset(bx - 4.dp.toPx(), by - 12.dp.toPx()), end = Offset(bx + 16.dp.toPx(), by + 12.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
}

// ============================================================================
// 8. 清泉石上流 · Clear Spring on Stones
// ============================================================================
private fun DrawScope.drawClearSpringTheme() {
    val w = size.width
    val h = size.height

    drawCircle(
        brush = Brush.radialGradient(listOf(InkSage.copy(alpha = 0.12f), Color.Transparent), center = Offset(w * 0.80f, h * 0.20f), radius = 56.dp.toPx()),
        radius = 56.dp.toPx(), center = Offset(w * 0.80f, h * 0.20f),
    )

    val stream = Path().apply {
        moveTo(w * 0.25f, h)
        cubicTo(w * 0.45f, h * 0.90f, w * 0.70f, h * 0.65f, w, h * 0.70f)
    }
    drawPath(stream, color = InkIndigo.copy(alpha = 0.16f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))

    drawOval(color = InkSage.copy(alpha = 0.20f), topLeft = Offset(w * 0.74f, h * 0.80f), size = Size(56.dp.toPx(), 32.dp.toPx()))
    drawOval(color = InkOchre.copy(alpha = 0.22f), topLeft = Offset(w * 0.62f, h * 0.88f), size = Size(42.dp.toPx(), 24.dp.toPx()))
}

// ============================================================================
// 9. 长河落日圆 · Great River & Crimson Sun
// ============================================================================
private fun DrawScope.drawDesertSunsetTheme() {
    val w = size.width
    val h = size.height

    val sunRadius = 54.dp.toPx()
    val sunCenter = Offset(w * 0.78f, h * 0.42f)
    drawCircle(
        brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.24f), Color.Transparent), center = sunCenter, radius = sunRadius * 1.4f),
        radius = sunRadius * 1.4f, center = sunCenter,
    )
    drawCircle(color = InkVermilion.copy(alpha = 0.22f), radius = sunRadius, center = sunCenter)

    val dunes = Path().apply {
        moveTo(w * 0.20f, h)
        cubicTo(w * 0.45f, h * 0.85f, w * 0.65f, h * 0.65f, w * 0.85f, h * 0.60f)
        cubicTo(w * 0.92f, h * 0.58f, w * 0.96f, h * 0.66f, w, h * 0.64f)
        lineTo(w, h); close()
    }
    drawPath(dunes, brush = Brush.linearGradient(listOf(InkOchre.copy(alpha = 0.22f), Color.Transparent), start = Offset(w * 0.5f, h * 0.6f), end = Offset(w * 0.5f, h)))
}

// ============================================================================
// 10. 一行白鹭上青天 · Egrets Ascending Sky
// ============================================================================
private fun DrawScope.drawEgretsAscendingTheme() {
    val w = size.width
    val h = size.height

    val marsh = Path().apply {
        moveTo(w * 0.40f, h); cubicTo(w * 0.60f, h * 0.85f, w * 0.75f, h * 0.72f, w, h * 0.75f); lineTo(w, h); close()
    }
    drawPath(marsh, color = InkSage.copy(alpha = 0.15f))

    val egrets = listOf(
        Offset(w * 0.42f, h * 0.22f), Offset(w * 0.52f, h * 0.30f),
        Offset(w * 0.62f, h * 0.39f), Offset(w * 0.70f, h * 0.48f),
    )
    egrets.forEachIndexed { i, pt ->
        val s = (1.1f - i * 0.1f) * 1.dp.toPx()
        val egretPath = Path().apply {
            moveTo(pt.x - 12 * s, pt.y + 4 * s)
            quadraticTo(pt.x - 4 * s, pt.y - 6 * s, pt.x, pt.y)
            quadraticTo(pt.x + 4 * s, pt.y - 6 * s, pt.x + 12 * s, pt.y + 4 * s)
        }
        drawPath(egretPath, color = InkBlack.copy(alpha = 0.65f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ============================================================================
// 11. 疏影横斜水清浅 · Sparse Plum Shadows
// ============================================================================
private fun DrawScope.drawPlumShadowTheme() {
    val w = size.width
    val h = size.height

    val branch = Path().apply {
        moveTo(w, h * 0.45f)
        cubicTo(w * 0.88f, h * 0.48f, w * 0.78f, h * 0.38f, w * 0.65f, h * 0.42f)
        cubicTo(w * 0.55f, h * 0.45f, w * 0.48f, h * 0.38f, w * 0.40f, h * 0.40f)
    }
    drawPath(branch, color = InkBlack.copy(alpha = 0.55f), style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))

    val petals = listOf(
        Offset(w * 0.68f, h * 0.26f), Offset(w * 0.65f, h * 0.42f),
        Offset(w * 0.58f, h * 0.43f), Offset(w * 0.48f, h * 0.38f), Offset(w * 0.40f, h * 0.40f),
    )
    petals.forEach { pt ->
        drawCircle(color = InkVermilion.copy(alpha = 0.65f), radius = 4.dp.toPx(), center = pt)
    }
}

// ============================================================================
// 12. 古道西风瘦马 · Ancient Road & West Wind
// ============================================================================
private fun DrawScope.drawAncientRoadTheme() {
    val w = size.width
    val h = size.height

    val ridge = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.55f, h * 0.80f, w * 0.70f, h * 0.62f, w * 0.85f, h * 0.58f)
        cubicTo(w * 0.92f, h * 0.56f, w * 0.97f, h * 0.64f, w, h * 0.60f)
        lineTo(w, h); close()
    }
    drawPath(ridge, color = InkOchre.copy(alpha = 0.16f))

    val path = Path().apply {
        moveTo(w * 0.48f, h); quadraticTo(w * 0.68f, h * 0.85f, w * 0.76f, h * 0.72f); quadraticTo(w * 0.82f, h * 0.64f, w * 0.88f, h * 0.60f)
    }
    drawPath(path, color = InkBlack.copy(alpha = 0.25f), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// 13. 斜风细雨不须归 · Spring Drizzle & Slanted Wind
// ============================================================================
private fun DrawScope.drawSpringRainTheme() {
    val w = size.width
    val h = size.height

    val hill = Path().apply {
        moveTo(w * 0.28f, h)
        cubicTo(w * 0.50f, h * 0.86f, w * 0.72f, h * 0.70f, w * 0.88f, h * 0.65f)
        cubicTo(w * 0.94f, h * 0.63f, w * 0.98f, h * 0.70f, w, h * 0.68f)
        lineTo(w, h); close()
    }
    drawPath(hill, brush = Brush.linearGradient(listOf(InkSage.copy(alpha = 0.16f), Color.Transparent), start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h)))

    for (i in 0 until 16) {
        val rx = w * (0.42f + (i % 5) * 0.11f)
        val ry = h * (0.22f + (i / 5) * 0.22f)
        drawLine(color = InkIndigo.copy(alpha = 0.18f), start = Offset(rx, ry), end = Offset(rx - 14.dp.toPx(), ry + 32.dp.toPx()), strokeWidth = 1.0.dp.toPx(), cap = StrokeCap.Round)
    }

    val willow = Path().apply {
        moveTo(w, h * 0.15f); quadraticTo(w * 0.88f, h * 0.25f, w * 0.82f, h * 0.42f)
    }
    drawPath(willow, color = InkSage.copy(alpha = 0.35f), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
}
