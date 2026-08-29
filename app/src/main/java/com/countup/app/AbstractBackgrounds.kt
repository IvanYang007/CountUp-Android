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
 * 7 Classical Chinese Poetic Landscape (国画诗词水墨意境) Abstract Background Themes.
 *
 * Core Design Principles:
 * - Landscape contours anchor seamlessly to screen borders (right & bottom).
 * - Visual weight concentrated in the bottom-right corner (右下聚景).
 * - Generous, airy negative space on the left (左侧大面积留白 · 计白当黑) to keep habit cards legible.
 * - Sinuous Bézier curves and mineral gradient washes (12%–22% opacity) ensuring 100% text contrast.
 */
enum class BackgroundTheme(val id: String, @get:StringRes val labelRes: Int) {
    AUTO_DAILY("auto_daily", R.string.bg_auto_daily),
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
                "dream_boat", "sea_horizon" -> DREAM_BOAT
                "clear_spring", "solitary_isle" -> CLEAR_SPRING
                "desert_sunset", "sand_dunes" -> DESERT_SUNSET
                "egrets_ascending" -> EGRETS_ASCENDING
                "plum_shadow" -> PLUM_SHADOW
                "ancient_road", "mountain" -> ANCIENT_ROAD
                "spring_rain", "willow_leaves", "cold_river_snow", "misty_grove" -> SPRING_RAIN
                else -> entries.firstOrNull { it.id == id } ?: AUTO_DAILY
            }
        }
    }
}

/**
 * Resolves [theme] to one of the 7 concrete poetic themes.
 * When set to [BackgroundTheme.AUTO_DAILY], cycles predictably based on [epochDay].
 */
fun resolveActiveTheme(theme: BackgroundTheme, epochDay: Long = LocalDate.now().toEpochDay()): BackgroundTheme {
    if (theme != BackgroundTheme.AUTO_DAILY) return theme
    val cycle = listOf(
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
        BackgroundTheme.DREAM_BOAT -> drawDreamBoatTheme()
        BackgroundTheme.CLEAR_SPRING -> drawClearSpringTheme()
        BackgroundTheme.DESERT_SUNSET -> drawDesertSunsetTheme()
        BackgroundTheme.EGRETS_ASCENDING -> drawEgretsAscendingTheme()
        BackgroundTheme.PLUM_SHADOW -> drawPlumShadowTheme()
        BackgroundTheme.ANCIENT_ROAD -> drawAncientRoadTheme()
        BackgroundTheme.SPRING_RAIN -> drawSpringRainTheme()
        BackgroundTheme.AUTO_DAILY -> drawDreamBoatTheme()
    }
}

// ============================================================================
// THEME 1: 满船清梦压星河 · Dream Boat on Milky Way
// “醉后不知天在水，满船清梦压星河。” —— 唐珙
// ============================================================================
private fun DrawScope.drawDreamBoatTheme() {
    val w = size.width
    val h = size.height

    // Starlight glow in high sky
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.82f, h * 0.20f),
            radius = 64.dp.toPx(),
        ),
        radius = 64.dp.toPx(),
        center = Offset(w * 0.82f, h * 0.20f),
    )

    // Starlight dots in upper right void
    val stars = listOf(
        Offset(w * 0.72f, h * 0.14f),
        Offset(w * 0.86f, h * 0.16f),
        Offset(w * 0.68f, h * 0.25f),
        Offset(w * 0.82f, h * 0.28f),
        Offset(w * 0.92f, h * 0.22f),
    )
    stars.forEach { pos ->
        drawCircle(color = InkIndigo.copy(alpha = 0.25f), radius = 1.5.dp.toPx(), center = pos)
    }

    // Wide water wave wash in bottom right
    val water = Path().apply {
        moveTo(w * 0.30f, h)
        cubicTo(w * 0.50f, h * 0.82f, w * 0.70f, h * 0.68f, w * 0.88f, h * 0.60f)
        cubicTo(w * 0.94f, h * 0.58f, w * 0.98f, h * 0.65f, w, h * 0.62f)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = water,
        brush = Brush.linearGradient(
            colors = listOf(InkIndigo.copy(alpha = 0.16f), Color.Transparent),
            start = Offset(w * 0.6f, h * 0.6f),
            end = Offset(w * 0.6f, h),
        ),
    )

    // Second gentle ripple line
    val ripple = Path().apply {
        moveTo(w * 0.50f, h)
        cubicTo(w * 0.68f, h * 0.88f, w * 0.82f, h * 0.76f, w, h * 0.78f)
    }
    drawPath(ripple, color = InkMuted.copy(alpha = 0.18f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))

    // Solitary Fishing Skiff (扁舟剪影与短桨)
    val bx = w * 0.76f
    val by = h * 0.68f
    val boat = Path().apply {
        moveTo(bx - 32.dp.toPx(), by)
        quadraticTo(bx, by + 10.dp.toPx(), bx + 32.dp.toPx(), by - 4.dp.toPx())
        quadraticTo(bx + 12.dp.toPx(), by + 4.dp.toPx(), bx - 32.dp.toPx(), by)
    }
    drawPath(boat, color = InkBlack.copy(alpha = 0.40f))

    // Oar
    drawLine(
        color = InkBlack.copy(alpha = 0.35f),
        start = Offset(bx - 4.dp.toPx(), by - 12.dp.toPx()),
        end = Offset(bx + 16.dp.toPx(), by + 12.dp.toPx()),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

// ============================================================================
// THEME 2: 清泉石上流 · Clear Spring on Stones
// “明月松间照，清泉石上流。” —— 王维
// ============================================================================
private fun DrawScope.drawClearSpringTheme() {
    val w = size.width
    val h = size.height

    // Moon halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkSage.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.80f, h * 0.20f),
            radius = 56.dp.toPx(),
        ),
        radius = 56.dp.toPx(),
        center = Offset(w * 0.80f, h * 0.20f),
    )

    // Flowing water ribbons sweeping downward
    val stream1 = Path().apply {
        moveTo(w * 0.25f, h)
        cubicTo(w * 0.45f, h * 0.90f, w * 0.70f, h * 0.65f, w, h * 0.70f)
    }
    drawPath(stream1, color = InkIndigo.copy(alpha = 0.16f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))

    val stream2 = Path().apply {
        moveTo(w * 0.40f, h)
        cubicTo(w * 0.60f, h * 0.82f, w * 0.80f, h * 0.55f, w, h * 0.58f)
    }
    drawPath(stream2, color = InkIndigo.copy(alpha = 0.12f), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))

    // Smooth Moss Stones (3枚圆润青石)
    drawOval(
        color = InkSage.copy(alpha = 0.20f),
        topLeft = Offset(w * 0.74f, h * 0.80f),
        size = Size(56.dp.toPx(), 32.dp.toPx()),
    )
    drawOval(
        color = InkOchre.copy(alpha = 0.22f),
        topLeft = Offset(w * 0.62f, h * 0.88f),
        size = Size(42.dp.toPx(), 24.dp.toPx()),
    )
    drawOval(
        color = InkSage.copy(alpha = 0.16f),
        topLeft = Offset(w * 0.86f, h * 0.70f),
        size = Size(36.dp.toPx(), 20.dp.toPx()),
    )
}

// ============================================================================
// THEME 3: 长河落日圆 · Great River & Crimson Sun
// “大漠孤烟直，长河落日圆。” —— 王维
// ============================================================================
private fun DrawScope.drawDesertSunsetTheme() {
    val w = size.width
    val h = size.height

    val sunRadius = 54.dp.toPx()
    val sunCenter = Offset(w * 0.78f, h * 0.42f)

    // Crimson Sun Wash Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.24f), Color.Transparent),
            center = sunCenter,
            radius = sunRadius * 1.4f,
        ),
        radius = sunRadius * 1.4f,
        center = sunCenter,
    )
    drawCircle(color = InkVermilion.copy(alpha = 0.22f), radius = sunRadius, center = sunCenter)

    // Golden Ochre Desert Dunes & River Banks
    val dunes = Path().apply {
        moveTo(w * 0.20f, h)
        cubicTo(w * 0.45f, h * 0.85f, w * 0.65f, h * 0.65f, w * 0.85f, h * 0.60f)
        cubicTo(w * 0.92f, h * 0.58f, w * 0.96f, h * 0.66f, w, h * 0.64f)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = dunes,
        brush = Brush.linearGradient(
            colors = listOf(InkOchre.copy(alpha = 0.22f), Color.Transparent),
            start = Offset(w * 0.5f, h * 0.6f),
            end = Offset(w * 0.5f, h),
        ),
    )

    // Sinuous sand contour line
    val duneLine = Path().apply {
        moveTo(w * 0.45f, h)
        cubicTo(w * 0.62f, h * 0.82f, w * 0.78f, h * 0.72f, w, h * 0.70f)
    }
    drawPath(duneLine, color = InkBlack.copy(alpha = 0.22f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// THEME 4: 一行白鹭上青天 · Egrets Ascending Sky
// “两个黄鹂鸣翠柳，一行白鹭上青天。” —— 杜甫
// ============================================================================
private fun DrawScope.drawEgretsAscendingTheme() {
    val w = size.width
    val h = size.height

    // Marsh Reeds Mound at Bottom Right
    val marsh = Path().apply {
        moveTo(w * 0.40f, h)
        cubicTo(w * 0.60f, h * 0.85f, w * 0.75f, h * 0.72f, w, h * 0.75f)
        lineTo(w, h)
        close()
    }
    drawPath(marsh, color = InkSage.copy(alpha = 0.15f))

    // Reed grass strokes
    val reeds = listOf(0.65f, 0.72f, 0.78f, 0.84f, 0.90f)
    reeds.forEach { rx ->
        val rPath = Path().apply {
            moveTo(w * rx, h * 0.85f)
            quadraticTo(w * (rx + 0.02f), h * 0.74f, w * (rx - 0.01f), h * 0.66f)
        }
        drawPath(rPath, color = InkBlack.copy(alpha = 0.25f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }

    // Ascending Egrets Queue (4只白鹭对角线翱翔直上青天)
    val egrets = listOf(
        Offset(w * 0.42f, h * 0.22f),
        Offset(w * 0.52f, h * 0.30f),
        Offset(w * 0.62f, h * 0.39f),
        Offset(w * 0.70f, h * 0.48f),
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
// THEME 5: 疏影横斜水清浅 · Sparse Plum Shadows
// “疏影横斜水清浅，暗香浮动月黄昏。” —— 林逋
// ============================================================================
private fun DrawScope.drawPlumShadowTheme() {
    val w = size.width
    val h = size.height

    // Dusk Moon Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(InkOchre.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(w * 0.80f, h * 0.25f),
            radius = 60.dp.toPx(),
        ),
        radius = 60.dp.toPx(),
        center = Offset(w * 0.80f, h * 0.25f),
    )

    // Main Gnarly Plum Branch (苍古梅枝斜倚贯穿)
    val branch = Path().apply {
        moveTo(w, h * 0.45f)
        cubicTo(w * 0.88f, h * 0.48f, w * 0.78f, h * 0.38f, w * 0.65f, h * 0.42f)
        cubicTo(w * 0.55f, h * 0.45f, w * 0.48f, h * 0.38f, w * 0.40f, h * 0.40f)
    }
    drawPath(branch, color = InkBlack.copy(alpha = 0.55f), style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))

    // Sub branch
    val subBranch = Path().apply {
        moveTo(w * 0.78f, h * 0.38f)
        quadraticTo(w * 0.72f, h * 0.30f, w * 0.68f, h * 0.26f)
    }
    drawPath(subBranch, color = InkBlack.copy(alpha = 0.45f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

    // Cinnabar Plum Blossoms (数点朱砂梅花瓣点染)
    val petals = listOf(
        Offset(w * 0.68f, h * 0.26f),
        Offset(w * 0.65f, h * 0.42f),
        Offset(w * 0.58f, h * 0.43f),
        Offset(w * 0.48f, h * 0.38f),
        Offset(w * 0.40f, h * 0.40f),
    )
    petals.forEach { pt ->
        drawCircle(color = InkVermilion.copy(alpha = 0.65f), radius = 4.dp.toPx(), center = pt)
    }

    // Water ripple
    val ripple = Path().apply {
        moveTo(w * 0.35f, h * 0.85f)
        cubicTo(w * 0.55f, h * 0.82f, w * 0.75f, h * 0.88f, w, h * 0.84f)
    }
    drawPath(ripple, color = InkIndigo.copy(alpha = 0.15f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// THEME 6: 古道西风瘦马 · Ancient Road & West Wind
// “枯藤老树昏鸦，小桥流水人家，古道西风瘦马。” —— 马致远
// ============================================================================
private fun DrawScope.drawAncientRoadTheme() {
    val w = size.width
    val h = size.height

    // Sunset Crimson Sky Wash
    val sky = Path().apply {
        moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h * 0.5f); lineTo(0f, h * 0.5f); close()
    }
    drawPath(
        path = sky,
        brush = Brush.verticalGradient(
            colors = listOf(InkVermilion.copy(alpha = 0.14f), Color.Transparent),
            startY = 0f,
            endY = h * 0.5f,
        ),
    )

    // Ancient Mountain Ridge
    val ridge = Path().apply {
        moveTo(w * 0.35f, h)
        cubicTo(w * 0.55f, h * 0.80f, w * 0.70f, h * 0.62f, w * 0.85f, h * 0.58f)
        cubicTo(w * 0.92f, h * 0.56f, w * 0.97f, h * 0.64f, w, h * 0.60f)
        lineTo(w, h)
        close()
    }
    drawPath(ridge, color = InkOchre.copy(alpha = 0.16f))

    // Winding Trail Path (曲折古道)
    val path = Path().apply {
        moveTo(w * 0.48f, h)
        quadraticTo(w * 0.68f, h * 0.85f, w * 0.76f, h * 0.72f)
        quadraticTo(w * 0.82f, h * 0.64f, w * 0.88f, h * 0.60f)
    }
    drawPath(path, color = InkBlack.copy(alpha = 0.25f), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))

    // Old Tree / Vine (枯藤老树剪影)
    val tree = Path().apply {
        moveTo(w * 0.88f, h * 0.60f)
        quadraticTo(w * 0.84f, h * 0.45f, w * 0.89f, h * 0.35f)
        moveTo(w * 0.86f, h * 0.48f)
        quadraticTo(w * 0.78f, h * 0.42f, w * 0.74f, h * 0.44f)
    }
    drawPath(tree, color = InkBlack.copy(alpha = 0.45f), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
}

// ============================================================================
// THEME 7: 斜风细雨不须归 · Spring Drizzle & Slanted Wind
// “青箬笠，绿蓑衣，斜风细雨不须归。” —— 张志和
// ============================================================================
private fun DrawScope.drawSpringRainTheme() {
    val w = size.width
    val h = size.height

    // Misty Green Rain Hill
    val hill = Path().apply {
        moveTo(w * 0.28f, h)
        cubicTo(w * 0.50f, h * 0.86f, w * 0.72f, h * 0.70f, w * 0.88f, h * 0.65f)
        cubicTo(w * 0.94f, h * 0.63f, w * 0.98f, h * 0.70f, w, h * 0.68f)
        lineTo(w, h)
        close()
    }
    drawPath(
        path = hill,
        brush = Brush.linearGradient(
            colors = listOf(InkSage.copy(alpha = 0.16f), Color.Transparent),
            start = Offset(w * 0.6f, h * 0.6f),
            end = Offset(w * 0.6f, h),
        ),
    )

    // Slanted Drizzle Rain Lines (漫天斜风细雨)
    for (i in 0 until 16) {
        val rx = w * (0.42f + (i % 5) * 0.11f)
        val ry = h * (0.22f + (i / 5) * 0.22f)
        drawLine(
            color = InkIndigo.copy(alpha = 0.18f),
            start = Offset(rx, ry),
            end = Offset(rx - 14.dp.toPx(), ry + 32.dp.toPx()),
            strokeWidth = 1.0.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Willow tendrils at top right
    val willow1 = Path().apply {
        moveTo(w, h * 0.15f)
        quadraticTo(w * 0.88f, h * 0.25f, w * 0.82f, h * 0.42f)
    }
    drawPath(willow1, color = InkSage.copy(alpha = 0.35f), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))

    val willow2 = Path().apply {
        moveTo(w * 0.95f, h * 0.10f)
        quadraticTo(w * 0.82f, h * 0.22f, w * 0.76f, h * 0.38f)
    }
    drawPath(willow2, color = InkSage.copy(alpha = 0.28f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
}
