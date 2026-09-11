package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
 * 30 Classical Chinese Poetic & Zen Landscape (国画诗词水墨意境) Abstract Background Themes.
 *
 * Grand Collection of:
 * - 6 Classic Zen Archetypes: Mountain, Sand Dunes, Sea Horizon, Solitary Isle, Willow Leaves, Zen Bamboo
 * - 24 Classical Verse Landscapes: Dream Boat, Clear Spring, Desert Sunset, Egrets, Plum Shadow, Ancient Road,
 *   Spring Rain, Lotus & Dragonfly, Crisp Rain, Solitary Sail, Ocean Moon, Wild Horizon, Green Hills, Stars & River,
 *   Clouds & Cottage, Window Moon, Apricot Rain, Forest Deer, Pear Blossom, Spring Water Sleep, Reading Lamp,
 *   Moon in Hand, Plantain Courtyard, Leaping Fish.
 *
 * Core Design Principles:
 * - Landscape contours anchor seamlessly to screen borders (right & bottom).
 * - Visual weight concentrated in the bottom-right corner (右下聚景).
 * - Generous negative space on the left (左侧大面积留白 · 计白当黑) to keep habit cards legible.
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
    SPRING_RAIN("spring_rain", R.string.bg_spring_rain),
    LOTUS_DRAGONFLY("lotus_dragonfly", R.string.bg_lotus_dragonfly),
    CRISP_SPRING_RAIN("crisp_spring_rain", R.string.bg_crisp_spring_rain),
    SOLITARY_SAIL_RIVER("solitary_sail_river", R.string.bg_solitary_sail_river),
    OCEAN_MOON_TIDE("ocean_moon_tide", R.string.bg_ocean_moon_tide),
    WILD_SKY_RIVER_MOON("wild_sky_river_moon", R.string.bg_wild_sky_river_moon),
    GREEN_HILLS_SAIL("green_hills_sail", R.string.bg_green_hills_sail),
    STARS_FALL_RIVER_FLOW("stars_fall_river_flow", R.string.bg_stars_fall_river_flow),
    CLOUDS_COTTAGE("clouds_cottage", R.string.bg_clouds_cottage),
    WINE_SPRING_MOON("wine_spring_moon", R.string.bg_wine_spring_moon),
    APRICOT_RAIN("apricot_rain", R.string.bg_apricot_rain),
    DEEP_FOREST_DEER("deep_forest_deer", R.string.bg_deep_forest_deer),
    PEAR_BLOSSOM_WILLOW("pear_blossom_willow", R.string.bg_pear_blossom_willow),
    SPRING_WATER_SLEEP("spring_water_sleep", R.string.bg_spring_water_sleep),
    READING_LAMP_MOON("reading_lamp_moon", R.string.bg_reading_lamp_moon),
    MOON_IN_HAND_WIND("moon_in_hand_wind", R.string.bg_moon_in_hand_wind),
    MOSS_COURTYARD_PLANTAIN("moss_courtyard_plantain", R.string.bg_moss_courtyard_plantain),
    FISH_JUMPING_DUCKWEED("fish_jumping_duckweed", R.string.bg_fish_jumping_duckweed);

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
 * Resolves [theme] to one of the 30 concrete themes.
 * When set to [BackgroundTheme.AUTO_DAILY], cycles predictably based on [epochDay] across all 30 daily landscapes (a full month of daily poetry).
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
        BackgroundTheme.LOTUS_DRAGONFLY,
        BackgroundTheme.CRISP_SPRING_RAIN,
        BackgroundTheme.SOLITARY_SAIL_RIVER,
        BackgroundTheme.OCEAN_MOON_TIDE,
        BackgroundTheme.WILD_SKY_RIVER_MOON,
        BackgroundTheme.GREEN_HILLS_SAIL,
        BackgroundTheme.STARS_FALL_RIVER_FLOW,
        BackgroundTheme.CLOUDS_COTTAGE,
        BackgroundTheme.WINE_SPRING_MOON,
        BackgroundTheme.APRICOT_RAIN,
        BackgroundTheme.DEEP_FOREST_DEER,
        BackgroundTheme.PEAR_BLOSSOM_WILLOW,
        BackgroundTheme.SPRING_WATER_SLEEP,
        BackgroundTheme.READING_LAMP_MOON,
        BackgroundTheme.MOON_IN_HAND_WIND,
        BackgroundTheme.MOSS_COURTYARD_PLANTAIN,
        BackgroundTheme.FISH_JUMPING_DUCKWEED,
    )
    val index = Math.floorMod(epochDay, cycle.size.toLong()).toInt()
    return cycle[index]
}

// ponytail: deliberate simplification using ThreadLocal to dynamically supply dark-mode moonlight mineral
// pigments without rewriting 30+ standalone Canvas drawing functions. Safe because all Android Compose UI
// drawing executes on the main thread and the draw block is strictly try/finally bounded.
// Upgrade path if multi-threaded drawing is ever introduced: pass a Theme/Palette data class into DrawScope.
private val isDarkLandscapeDraw = ThreadLocal.withInitial { false }

// Chinese Ink & Mineral Pigment Palette (国画水墨与矿物色)
// Light: Traditional rich Sumi inks on washi paper.
// Dark: Ethereal moonlight & mineral pigments on aged kiln slate ("月映青砖 · 五墨六彩").
private val InkBlack: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFF7A8B99) else Color(0xFF1E2124)       // 霜天寒雾 / 焦墨
private val InkMuted: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFF5B7E94) else Color(0xFF4A4E54)       // 寒潭碧月 / 淡墨
private val InkOchre: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFFE5A869) else Color(0xFFC48B58)       // 孤灯暖光 / 赭石
private val InkVermilion: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFFC75A4C) else Color(0xFFD3523B)   // 晚霞渔火 / 朱砂
private val InkIndigo: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFF6B8FA3) else Color(0xFF4E6B7A)      // 寒江黛蓝 / 花青
private val InkSage: Color get() = if (isDarkLandscapeDraw.get() == true) Color(0xFF5F8C6E) else Color(0xFF4A7C59)        // 露华竹影 / 柳绿

/**
 * Renders the chosen abstract Zen/Ink Wash background behind content.
 */
fun Modifier.drawAbstractBackground(
    theme: BackgroundTheme,
    epochDay: Long = LocalDate.now().toEpochDay(),
    isDark: Boolean = false,
): Modifier = this.drawWithCache {
    val active = resolveActiveTheme(theme, epochDay)
    onDrawBehind {
        isDarkLandscapeDraw.set(isDark)
        try {
            if (isDark) {
                drawNightZenWash()
            }
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
            BackgroundTheme.LOTUS_DRAGONFLY -> drawLotusDragonflyTheme()
            BackgroundTheme.CRISP_SPRING_RAIN -> drawCrispSpringRainTheme()
            BackgroundTheme.SOLITARY_SAIL_RIVER -> drawSolitarySailRiverTheme()
            BackgroundTheme.OCEAN_MOON_TIDE -> drawOceanMoonTideTheme()
            BackgroundTheme.WILD_SKY_RIVER_MOON -> drawWildSkyRiverMoonTheme()
            BackgroundTheme.GREEN_HILLS_SAIL -> drawGreenHillsSailTheme()
            BackgroundTheme.STARS_FALL_RIVER_FLOW -> drawStarsFallRiverFlowTheme()
            BackgroundTheme.CLOUDS_COTTAGE -> drawCloudsCottageTheme()
            BackgroundTheme.WINE_SPRING_MOON -> drawWineSpringMoonTheme()
            BackgroundTheme.APRICOT_RAIN -> drawApricotRainTheme()
            BackgroundTheme.DEEP_FOREST_DEER -> drawDeepForestDeerTheme()
            BackgroundTheme.PEAR_BLOSSOM_WILLOW -> drawPearBlossomWillowTheme()
            BackgroundTheme.SPRING_WATER_SLEEP -> drawSpringWaterSleepTheme()
            BackgroundTheme.READING_LAMP_MOON -> drawReadingLampMoonTheme()
            BackgroundTheme.MOON_IN_HAND_WIND -> drawMoonInHandWindTheme()
            BackgroundTheme.MOSS_COURTYARD_PLANTAIN -> drawMossCourtyardPlantainTheme()
            BackgroundTheme.FISH_JUMPING_DUCKWEED -> drawFishJumpingDuckweedTheme()
            BackgroundTheme.AUTO_DAILY -> drawInkMountainTheme()
        }
    } finally {
        isDarkLandscapeDraw.set(false)
    }
    }
}

// ----------------------------------------------------------------------------
// 1. 远山含黛 · Mountain Peaks
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkMountainTheme() {
    val w = size.width; val h = size.height
    drawCircle(brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.11f), Color.Transparent), center = Offset(w * 0.84f, h * 0.20f), radius = 56.dp.toPx()), radius = 56.dp.toPx(), center = Offset(w * 0.84f, h * 0.20f))
    val distantMountain = Path().apply {
        moveTo(w * 0.35f, h); cubicTo(w * 0.50f, h * 0.72f, w * 0.65f, h * 0.56f, w * 0.80f, h * 0.50f); cubicTo(w * 0.90f, h * 0.46f, w * 0.96f, h * 0.56f, w, h * 0.52f); lineTo(w, h); close()
    }
    drawPath(distantMountain, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.14f), Color.Transparent), start = Offset(w * 0.6f, h * 0.5f), end = Offset(w * 0.6f, h)))
    val midMountain = Path().apply {
        moveTo(w * 0.48f, h); cubicTo(w * 0.62f, h * 0.78f, w * 0.74f, h * 0.64f, w * 0.86f, h * 0.60f); cubicTo(w * 0.92f, h * 0.58f, w * 0.98f, h * 0.66f, w, h * 0.64f); lineTo(w, h); close()
    }
    drawPath(midMountain, brush = Brush.linearGradient(listOf(InkMuted.copy(alpha = 0.18f), Color.Transparent), start = Offset(w * 0.7f, h * 0.6f), end = Offset(w * 0.7f, h)))
}

// ----------------------------------------------------------------------------
// 2. 平沙落雁 · Sand Dunes
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkSandDunesTheme() {
    val w = size.width; val h = size.height
    val dune1 = Path().apply {
        moveTo(w, h * 0.46f); cubicTo(w * 0.76f, h * 0.55f, w * 0.54f, h * 0.66f, w * 0.44f, h * 0.82f); cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.96f, w * 0.48f, h); lineTo(w, h); close()
    }
    drawPath(dune1, brush = Brush.linearGradient(listOf(InkOchre.copy(alpha = 0.20f), Color.Transparent), start = Offset(w, h * 0.46f), end = Offset(w * 0.42f, h * 0.85f)))
}

// ----------------------------------------------------------------------------
// 3. 烟波浩渺 · Calm Sea Horizon
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkSeaHorizonTheme() {
    val w = size.width; val h = size.height
    drawCircle(brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.16f), Color.Transparent), center = Offset(w * 0.75f, h * 0.45f), radius = 50.dp.toPx()), radius = 50.dp.toPx(), center = Offset(w * 0.75f, h * 0.45f))
    val sea = Path().apply {
        moveTo(w * 0.25f, h); cubicTo(w * 0.50f, h * 0.85f, w * 0.70f, h * 0.65f, w, h * 0.68f); lineTo(w, h); close()
    }
    drawPath(sea, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.18f), Color.Transparent), start = Offset(w * 0.6f, h * 0.65f), end = Offset(w * 0.6f, h)))
}

// ----------------------------------------------------------------------------
// 4. 太湖石秀 · Solitary Isle
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkSolitaryIsleTheme() {
    val w = size.width; val h = size.height
    val rockCenterX = w * 0.80f; val rockBaseY = h * 0.88f
    val stonePath = Path().apply {
        moveTo(rockCenterX - 45.dp.toPx(), rockBaseY); cubicTo(rockCenterX - 38.dp.toPx(), rockBaseY - 32.dp.toPx(), rockCenterX - 18.dp.toPx(), rockBaseY - 52.dp.toPx(), rockCenterX + 8.dp.toPx(), rockBaseY - 48.dp.toPx()); cubicTo(rockCenterX + 32.dp.toPx(), rockBaseY - 44.dp.toPx(), rockCenterX + 48.dp.toPx(), rockBaseY - 22.dp.toPx(), rockCenterX + 42.dp.toPx(), rockBaseY); close()
    }
    drawPath(stonePath, brush = Brush.radialGradient(listOf(InkBlack.copy(alpha = 0.22f), InkSage.copy(alpha = 0.12f), Color.Transparent), center = Offset(rockCenterX - 8.dp.toPx(), rockBaseY - 15.dp.toPx()), radius = 55.dp.toPx()))
    drawPath(stonePath, color = InkBlack.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(color = InkBlack.copy(alpha = 0.32f), radius = 2.dp.toPx(), center = Offset(rockCenterX - 12.dp.toPx(), rockBaseY - 36.dp.toPx()))
    drawCircle(color = InkSage.copy(alpha = 0.25f), radius = 2.5.dp.toPx(), center = Offset(rockCenterX + 15.dp.toPx(), rockBaseY - 30.dp.toPx()))
}

// ----------------------------------------------------------------------------
// 5. 柳浪闻莺 · Willow Leaves
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkWillowLeavesTheme() {
    val w = size.width; val h = size.height
    val stem = Path().apply { moveTo(w, h * 0.35f); cubicTo(w * 0.86f, h * 0.45f, w * 0.78f, h * 0.60f, w * 0.74f, h * 0.75f) }
    drawPath(stem, color = InkBlack.copy(alpha = 0.30f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
    listOf(Offset(w * 0.92f, h * 0.40f), Offset(w * 0.86f, h * 0.48f), Offset(w * 0.80f, h * 0.58f), Offset(w * 0.75f, h * 0.70f)).forEach { pt ->
        drawOval(color = InkSage.copy(alpha = 0.32f), topLeft = pt, size = Size(14.dp.toPx(), 6.dp.toPx()))
    }
}

// ----------------------------------------------------------------------------
// 6. 幽竹虚心 · Zen Bamboo
// ----------------------------------------------------------------------------
private fun DrawScope.drawInkZenBambooTheme() {
    val w = size.width; val h = size.height
    val stalk1 = Path().apply { moveTo(w * 0.86f, h); lineTo(w * 0.88f, h * 0.35f) }
    drawPath(stalk1, color = InkSage.copy(alpha = 0.35f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    listOf(Offset(w * 0.84f, h * 0.45f), Offset(w * 0.82f, h * 0.52f), Offset(w * 0.90f, h * 0.38f)).forEach { pt ->
        val lf = Path().apply {
            moveTo(pt.x, pt.y); quadraticTo(pt.x - 18.dp.toPx(), pt.y + 4.dp.toPx(), pt.x - 28.dp.toPx(), pt.y + 14.dp.toPx()); quadraticTo(pt.x - 14.dp.toPx(), pt.y + 10.dp.toPx(), pt.x, pt.y)
        }
        drawPath(lf, color = InkSage.copy(alpha = 0.38f))
    }
}

// ----------------------------------------------------------------------------
// 7. 满船清梦压星河 · Dream Boat
// ----------------------------------------------------------------------------
private fun DrawScope.drawDreamBoatTheme() {
    val w = size.width; val h = size.height
    drawCircle(brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.12f), Color.Transparent), center = Offset(w * 0.82f, h * 0.20f), radius = 64.dp.toPx()), radius = 64.dp.toPx(), center = Offset(w * 0.82f, h * 0.20f))
    val water = Path().apply { moveTo(w * 0.30f, h); cubicTo(w * 0.50f, h * 0.82f, w * 0.70f, h * 0.68f, w * 0.88f, h * 0.60f); lineTo(w, h); close() }
    drawPath(water, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.16f), Color.Transparent), start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h)))
    val bx = w * 0.76f; val by = h * 0.68f
    val boat = Path().apply { moveTo(bx - 32.dp.toPx(), by); quadraticTo(bx, by + 10.dp.toPx(), bx + 32.dp.toPx(), by - 4.dp.toPx()); quadraticTo(bx + 12.dp.toPx(), by + 4.dp.toPx(), bx - 32.dp.toPx(), by) }
    drawPath(boat, color = InkBlack.copy(alpha = 0.40f))
}

// ----------------------------------------------------------------------------
// 8. 清泉石上流 · Clear Spring
// ----------------------------------------------------------------------------
private fun DrawScope.drawClearSpringTheme() {
    val w = size.width; val h = size.height
    val stream = Path().apply { moveTo(w * 0.25f, h); cubicTo(w * 0.45f, h * 0.90f, w * 0.70f, h * 0.65f, w, h * 0.70f) }
    drawPath(stream, color = InkIndigo.copy(alpha = 0.16f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
    drawOval(color = InkSage.copy(alpha = 0.20f), topLeft = Offset(w * 0.74f, h * 0.80f), size = Size(56.dp.toPx(), 32.dp.toPx()))
    drawOval(color = InkOchre.copy(alpha = 0.22f), topLeft = Offset(w * 0.62f, h * 0.88f), size = Size(42.dp.toPx(), 24.dp.toPx()))
}

// ----------------------------------------------------------------------------
// 9. 长河落日圆 · Desert Sunset
// ----------------------------------------------------------------------------
private fun DrawScope.drawDesertSunsetTheme() {
    val w = size.width; val h = size.height
    drawCircle(color = InkVermilion.copy(alpha = 0.22f), radius = 54.dp.toPx(), center = Offset(w * 0.78f, h * 0.42f))
    val dunes = Path().apply { moveTo(w * 0.20f, h); cubicTo(w * 0.45f, h * 0.85f, w * 0.65f, h * 0.65f, w, h * 0.64f); lineTo(w, h); close() }
    drawPath(dunes, brush = Brush.linearGradient(listOf(InkOchre.copy(alpha = 0.22f), Color.Transparent), start = Offset(w * 0.5f, h * 0.6f), end = Offset(w * 0.5f, h)))
}

// ----------------------------------------------------------------------------
// 10. 一行白鹭上青天 · Egrets Ascending
// ----------------------------------------------------------------------------
private fun DrawScope.drawEgretsAscendingTheme() {
    val w = size.width; val h = size.height
    val marsh = Path().apply { moveTo(w * 0.40f, h); cubicTo(w * 0.60f, h * 0.85f, w * 0.75f, h * 0.72f, w, h * 0.75f); lineTo(w, h); close() }
    drawPath(marsh, color = InkSage.copy(alpha = 0.15f))
    listOf(Offset(w * 0.42f, h * 0.22f), Offset(w * 0.52f, h * 0.30f), Offset(w * 0.62f, h * 0.39f)).forEach { pt ->
        val egretPath = Path().apply { moveTo(pt.x - 10.dp.toPx(), pt.y + 3.dp.toPx()); quadraticTo(pt.x - 3.dp.toPx(), pt.y - 5.dp.toPx(), pt.x, pt.y); quadraticTo(pt.x + 3.dp.toPx(), pt.y - 5.dp.toPx(), pt.x + 10.dp.toPx(), pt.y + 3.dp.toPx()) }
        drawPath(egretPath, color = InkBlack.copy(alpha = 0.65f), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ----------------------------------------------------------------------------
// 11. 疏影横斜水清浅 · Plum Shadow
// ----------------------------------------------------------------------------
private fun DrawScope.drawPlumShadowTheme() {
    val w = size.width; val h = size.height
    val branch = Path().apply { moveTo(w, h * 0.45f); cubicTo(w * 0.88f, h * 0.48f, w * 0.78f, h * 0.38f, w * 0.65f, h * 0.42f); cubicTo(w * 0.55f, h * 0.45f, w * 0.48f, h * 0.38f, w * 0.40f, h * 0.40f) }
    drawPath(branch, color = InkBlack.copy(alpha = 0.55f), style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))
    listOf(Offset(w * 0.68f, h * 0.26f), Offset(w * 0.65f, h * 0.42f), Offset(w * 0.58f, h * 0.43f)).forEach { pt ->
        drawCircle(color = InkVermilion.copy(alpha = 0.65f), radius = 4.dp.toPx(), center = pt)
    }
}

// ----------------------------------------------------------------------------
// 12. 古道西风瘦马 · Ancient Road
// ----------------------------------------------------------------------------
private fun DrawScope.drawAncientRoadTheme() {
    val w = size.width; val h = size.height
    val ridge = Path().apply { moveTo(w * 0.35f, h); cubicTo(w * 0.55f, h * 0.80f, w * 0.70f, h * 0.62f, w, h * 0.60f); lineTo(w, h); close() }
    drawPath(ridge, color = InkOchre.copy(alpha = 0.16f))
    val path = Path().apply { moveTo(w * 0.48f, h); quadraticTo(w * 0.68f, h * 0.85f, w * 0.76f, h * 0.72f); quadraticTo(w * 0.82f, h * 0.64f, w * 0.88f, h * 0.60f) }
    drawPath(path, color = InkBlack.copy(alpha = 0.25f), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))
}

// ----------------------------------------------------------------------------
// 13. 斜风细雨不须归 · Spring Rain
// ----------------------------------------------------------------------------
private fun DrawScope.drawSpringRainTheme() {
    val w = size.width; val h = size.height
    val hill = Path().apply { moveTo(w * 0.28f, h); cubicTo(w * 0.50f, h * 0.86f, w * 0.72f, h * 0.70f, w, h * 0.68f); lineTo(w, h); close() }
    drawPath(hill, brush = Brush.linearGradient(listOf(InkSage.copy(alpha = 0.16f), Color.Transparent), start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h)))
    for (i in 0 until 12) {
        val rx = w * (0.45f + (i % 4) * 0.13f); val ry = h * (0.25f + (i / 4) * 0.22f)
        drawLine(color = InkIndigo.copy(alpha = 0.18f), start = Offset(rx, ry), end = Offset(rx - 14.dp.toPx(), ry + 32.dp.toPx()), strokeWidth = 1.0.dp.toPx(), cap = StrokeCap.Round)
    }
}

// ----------------------------------------------------------------------------
// 14. 小荷才露尖尖角，早有蜻蜓立上头 · Lotus & Dragonfly
// ----------------------------------------------------------------------------
private fun DrawScope.drawLotusDragonflyTheme() {
    val w = size.width; val h = size.height
    val water = Path().apply { moveTo(w * 0.35f, h); cubicTo(w * 0.55f, h * 0.88f, w * 0.75f, h * 0.78f, w, h * 0.80f); lineTo(w, h); close() }
    drawPath(water, brush = Brush.linearGradient(listOf(InkSage.copy(alpha = 0.15f), Color.Transparent), start = Offset(w * 0.6f, h * 0.8f), end = Offset(w * 0.6f, h)))

    // Lotus stem & bud tip
    val stem = Path().apply { moveTo(w * 0.82f, h); quadraticTo(w * 0.80f, h * 0.72f, w * 0.76f, h * 0.58f) }
    drawPath(stem, color = InkSage.copy(alpha = 0.45f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    val bud = Path().apply { moveTo(w * 0.76f, h * 0.58f); quadraticTo(w * 0.74f, h * 0.50f, w * 0.76f, h * 0.46f); quadraticTo(w * 0.78f, h * 0.50f, w * 0.76f, h * 0.58f) }
    drawPath(bud, color = InkVermilion.copy(alpha = 0.50f))

    // Dragonfly on tip
    val dfx = w * 0.76f; val dfy = h * 0.46f
    drawLine(color = InkBlack.copy(alpha = 0.60f), start = Offset(dfx - 6.dp.toPx(), dfy - 8.dp.toPx()), end = Offset(dfx + 6.dp.toPx(), dfy + 4.dp.toPx()), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = InkIndigo.copy(alpha = 0.40f), start = Offset(dfx - 12.dp.toPx(), dfy - 12.dp.toPx()), end = Offset(dfx + 8.dp.toPx(), dfy - 4.dp.toPx()), strokeWidth = 1.dp.toPx())
}

// ----------------------------------------------------------------------------
// 15. 天街小雨润如酥 · Crisp Spring Rain
// ----------------------------------------------------------------------------
private fun DrawScope.drawCrispSpringRainTheme() {
    val w = size.width; val h = size.height
    val cobble = Path().apply { moveTo(w * 0.42f, h); cubicTo(w * 0.60f, h * 0.88f, w * 0.80f, h * 0.82f, w, h * 0.85f); lineTo(w, h); close() }
    drawPath(cobble, color = InkSage.copy(alpha = 0.16f))
    for (i in 0 until 18) {
        val rx = w * (0.38f + (i % 6) * 0.10f); val ry = h * (0.15f + (i / 6) * 0.26f)
        drawLine(color = InkIndigo.copy(alpha = 0.15f), start = Offset(rx, ry), end = Offset(rx - 8.dp.toPx(), ry + 22.dp.toPx()), strokeWidth = 0.9.dp.toPx(), cap = StrokeCap.Round)
    }
    // Tender sprout
    val sprout = Path().apply { moveTo(w * 0.78f, h * 0.88f); quadraticTo(w * 0.74f, h * 0.84f, w * 0.73f, h * 0.80f) }
    drawPath(sprout, color = InkSage.copy(alpha = 0.55f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
}

// ----------------------------------------------------------------------------
// 16. 孤帆远影碧空尽，唯见长江天际流 · Solitary Sail on Yangtze
// ----------------------------------------------------------------------------
private fun DrawScope.drawSolitarySailRiverTheme() {
    val w = size.width; val h = size.height
    val river = Path().apply { moveTo(w * 0.15f, h); cubicTo(w * 0.45f, h * 0.75f, w * 0.70f, h * 0.55f, w, h * 0.45f); lineTo(w, h); close() }
    drawPath(river, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.15f), Color.Transparent), start = Offset(w * 0.6f, h * 0.45f), end = Offset(w * 0.6f, h)))
    // Distant tiny sail near sky
    val sx = w * 0.86f; val sy = h * 0.46f
    val sail = Path().apply { moveTo(sx, sy); lineTo(sx + 8.dp.toPx(), sy + 14.dp.toPx()); lineTo(sx - 4.dp.toPx(), sy + 14.dp.toPx()); close() }
    drawPath(sail, color = InkBlack.copy(alpha = 0.45f))
}

// ----------------------------------------------------------------------------
// 17. 海上明月共潮生 · Ocean Moon & Rising Tide
// ----------------------------------------------------------------------------
private fun DrawScope.drawOceanMoonTideTheme() {
    val w = size.width; val h = size.height
    // Glowing Full Moon
    drawCircle(brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * 0.80f, h * 0.35f), radius = 60.dp.toPx()), radius = 60.dp.toPx(), center = Offset(w * 0.80f, h * 0.35f))
    drawCircle(color = InkVermilion.copy(alpha = 0.18f), radius = 32.dp.toPx(), center = Offset(w * 0.80f, h * 0.35f))
    // Tide swells
    val tide1 = Path().apply { moveTo(w * 0.20f, h); cubicTo(w * 0.48f, h * 0.82f, w * 0.72f, h * 0.68f, w, h * 0.65f); lineTo(w, h); close() }
    drawPath(tide1, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.18f), Color.Transparent), start = Offset(w * 0.6f, h * 0.65f), end = Offset(w * 0.6f, h)))
}

// ----------------------------------------------------------------------------
// 18. 野旷天低树，江清月近人 · Wild Horizon & River Moon
// ----------------------------------------------------------------------------
private fun DrawScope.drawWildSkyRiverMoonTheme() {
    val w = size.width; val h = size.height
    val river = Path().apply { moveTo(w * 0.30f, h); cubicTo(w * 0.55f, h * 0.85f, w * 0.75f, h * 0.75f, w, h * 0.74f); lineTo(w, h); close() }
    drawPath(river, color = InkIndigo.copy(alpha = 0.14f))
    // Close moon in water reflection
    drawCircle(color = InkOchre.copy(alpha = 0.20f), radius = 24.dp.toPx(), center = Offset(w * 0.74f, h * 0.85f))
    // Low tree silhouette
    val tree = Path().apply { moveTo(w * 0.88f, h * 0.74f); lineTo(w * 0.88f, h * 0.62f); cubicTo(w * 0.82f, h * 0.58f, w * 0.94f, h * 0.54f, w * 0.88f, h * 0.62f) }
    drawPath(tree, color = InkBlack.copy(alpha = 0.40f), style = Stroke(width = 1.8.dp.toPx()))
}

// ----------------------------------------------------------------------------
// 19. 两岸青山相对出，孤帆一片日边来 · Green Hills & Approaching Sail
// ----------------------------------------------------------------------------
private fun DrawScope.drawGreenHillsSailTheme() {
    val w = size.width; val h = size.height
    // Morning Sun
    drawCircle(color = InkVermilion.copy(alpha = 0.20f), radius = 30.dp.toPx(), center = Offset(w * 0.82f, h * 0.38f))
    // Twin Karst cliffs
    val cliff1 = Path().apply { moveTo(w * 0.50f, h); cubicTo(w * 0.62f, h * 0.70f, w * 0.72f, h * 0.52f, w * 0.78f, h * 0.65f); lineTo(w * 0.78f, h); close() }
    drawPath(cliff1, color = InkSage.copy(alpha = 0.20f))
    val cliff2 = Path().apply { moveTo(w * 0.82f, h); cubicTo(w * 0.86f, h * 0.58f, w * 0.94f, h * 0.48f, w, h * 0.55f); lineTo(w, h); close() }
    drawPath(cliff2, color = InkIndigo.copy(alpha = 0.22f))
    // Approaching sail between gorge
    val sx = w * 0.78f; val sy = h * 0.62f
    val sail = Path().apply { moveTo(sx, sy); lineTo(sx + 6.dp.toPx(), sy + 10.dp.toPx()); lineTo(sx - 3.dp.toPx(), sy + 10.dp.toPx()); close() }
    drawPath(sail, color = InkBlack.copy(alpha = 0.50f))
}

// ----------------------------------------------------------------------------
// 20. 星垂平野阔，月涌大江流 · Falling Stars & Surging River
// ----------------------------------------------------------------------------
private fun DrawScope.drawStarsFallRiverFlowTheme() {
    val w = size.width; val h = size.height
    val surge = Path().apply { moveTo(w * 0.20f, h); cubicTo(w * 0.50f, h * 0.75f, w * 0.75f, h * 0.85f, w, h * 0.60f); lineTo(w, h); close() }
    drawPath(surge, brush = Brush.linearGradient(listOf(InkIndigo.copy(alpha = 0.20f), Color.Transparent), start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h)))
    listOf(Offset(w * 0.55f, h * 0.35f), Offset(w * 0.70f, h * 0.28f), Offset(w * 0.85f, h * 0.22f), Offset(w * 0.92f, h * 0.36f)).forEach { pt ->
        drawCircle(color = InkIndigo.copy(alpha = 0.35f), radius = 2.dp.toPx(), center = pt)
    }
}

// ----------------------------------------------------------------------------
// 21. 白云生处有人家 · Cottage in Billowing Clouds
// ----------------------------------------------------------------------------
private fun DrawScope.drawCloudsCottageTheme() {
    val w = size.width; val h = size.height
    // Mountain Ridge with Thatched Roof
    val ridge = Path().apply { moveTo(w * 0.35f, h); cubicTo(w * 0.55f, h * 0.75f, w * 0.75f, h * 0.55f, w, h * 0.58f); lineTo(w, h); close() }
    drawPath(ridge, color = InkIndigo.copy(alpha = 0.16f))
    // Cottage Roof silhouette
    val rx = w * 0.76f; val ry = h * 0.62f
    val roof = Path().apply { moveTo(rx, ry); lineTo(rx + 14.dp.toPx(), ry - 8.dp.toPx()); lineTo(rx + 28.dp.toPx(), ry); lineTo(rx, ry); close() }
    drawPath(roof, color = InkOchre.copy(alpha = 0.45f))
    drawPath(roof, color = InkBlack.copy(alpha = 0.35f), style = Stroke(width = 1.4.dp.toPx()))
}

// ----------------------------------------------------------------------------
// 22. 酒浓春入梦，窗破月寻人 · Spring Dream & Window Moon
// ----------------------------------------------------------------------------
private fun DrawScope.drawWineSpringMoonTheme() {
    val w = size.width; val h = size.height
    // Lattice window frame
    val wx = w * 0.72f; val wy = h * 0.32f
    drawRect(color = InkBlack.copy(alpha = 0.25f), topLeft = Offset(wx, wy), size = Size(64.dp.toPx(), 64.dp.toPx()), style = Stroke(width = 1.8.dp.toPx()))
    drawLine(color = InkBlack.copy(alpha = 0.20f), start = Offset(wx + 32.dp.toPx(), wy), end = Offset(wx + 32.dp.toPx(), wy + 64.dp.toPx()), strokeWidth = 1.2.dp.toPx())
    drawLine(color = InkBlack.copy(alpha = 0.20f), start = Offset(wx, wy + 32.dp.toPx()), end = Offset(wx + 64.dp.toPx(), wy + 32.dp.toPx()), strokeWidth = 1.2.dp.toPx())
    // Moonbeam shining through
    drawCircle(brush = Brush.radialGradient(listOf(InkVermilion.copy(alpha = 0.18f), Color.Transparent), center = Offset(wx + 32.dp.toPx(), wy + 32.dp.toPx()), radius = 40.dp.toPx()), radius = 40.dp.toPx(), center = Offset(wx + 32.dp.toPx(), wy + 32.dp.toPx()))
}

// ----------------------------------------------------------------------------
// 23. 红杏开时，一霎清明雨 · Apricot Blossom & Spring Rain
// ----------------------------------------------------------------------------
private fun DrawScope.drawApricotRainTheme() {
    val w = size.width; val h = size.height
    val branch = Path().apply { moveTo(w, h * 0.32f); cubicTo(w * 0.88f, h * 0.36f, w * 0.80f, h * 0.48f, w * 0.72f, h * 0.44f) }
    drawPath(branch, color = InkBlack.copy(alpha = 0.40f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    listOf(Offset(w * 0.72f, h * 0.44f), Offset(w * 0.80f, h * 0.46f), Offset(w * 0.86f, h * 0.35f)).forEach { pt ->
        drawCircle(color = InkVermilion.copy(alpha = 0.60f), radius = 5.dp.toPx(), center = pt)
    }
    for (i in 0 until 10) {
        val rx = w * (0.50f + (i % 4) * 0.12f); val ry = h * (0.28f + (i / 4) * 0.25f)
        drawLine(color = InkIndigo.copy(alpha = 0.14f), start = Offset(rx, ry), end = Offset(rx - 8.dp.toPx(), ry + 20.dp.toPx()), strokeWidth = 0.9.dp.toPx(), cap = StrokeCap.Round)
    }
}

// ----------------------------------------------------------------------------
// 24. 树深时见鹿 · Deep Forest Deer
// ----------------------------------------------------------------------------
private fun DrawScope.drawDeepForestDeerTheme() {
    val w = size.width; val h = size.height
    // Forest trunks
    listOf(w * 0.70f, w * 0.82f, w * 0.92f).forEach { tx ->
        drawLine(color = InkBlack.copy(alpha = 0.22f), start = Offset(tx, h), end = Offset(tx - 10.dp.toPx(), h * 0.45f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    }
    // Stag antler silhouette
    val dx = w * 0.78f; val dy = h * 0.74f
    drawCircle(color = InkOchre.copy(alpha = 0.45f), radius = 8.dp.toPx(), center = Offset(dx, dy))
    val antler = Path().apply { moveTo(dx, dy - 8.dp.toPx()); lineTo(dx - 6.dp.toPx(), dy - 20.dp.toPx()); lineTo(dx - 12.dp.toPx(), dy - 16.dp.toPx()); moveTo(dx, dy - 8.dp.toPx()); lineTo(dx + 6.dp.toPx(), dy - 20.dp.toPx()) }
    drawPath(antler, color = InkBlack.copy(alpha = 0.50f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
}

// ----------------------------------------------------------------------------
// 25. 梨花淡白柳深青，柳絮飞时花满城 · Pear Blossoms & Weeping Willow
// ----------------------------------------------------------------------------
private fun DrawScope.drawPearBlossomWillowTheme() {
    val w = size.width; val h = size.height
    val willow = Path().apply { moveTo(w, h * 0.28f); cubicTo(w * 0.86f, h * 0.40f, w * 0.80f, h * 0.58f, w * 0.76f, h * 0.76f) }
    drawPath(willow, color = InkSage.copy(alpha = 0.45f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    // Pear blossom white/pale petals
    listOf(Offset(w * 0.74f, h * 0.50f), Offset(w * 0.82f, h * 0.44f), Offset(w * 0.88f, h * 0.60f), Offset(w * 0.68f, h * 0.65f)).forEach { pt ->
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = pt)
        drawCircle(color = InkOchre.copy(alpha = 0.40f), radius = 1.5.dp.toPx(), center = pt)
    }
}

// ----------------------------------------------------------------------------
// 26. 半篙春水一蓑烟，抱月怀中枕斗眠 · Spring Water & Moon Pillow
// ----------------------------------------------------------------------------
private fun DrawScope.drawSpringWaterSleepTheme() {
    val w = size.width; val h = size.height
    // Slanted bamboo pole
    drawLine(color = InkBlack.copy(alpha = 0.35f), start = Offset(w * 0.65f, h * 0.50f), end = Offset(w * 0.88f, h), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    // Moored boat
    val bx = w * 0.78f; val by = h * 0.80f
    val boat = Path().apply { moveTo(bx - 26.dp.toPx(), by); quadraticTo(bx, by + 8.dp.toPx(), bx + 26.dp.toPx(), by - 4.dp.toPx()); quadraticTo(bx + 8.dp.toPx(), by + 4.dp.toPx(), bx - 26.dp.toPx(), by) }
    drawPath(boat, color = InkBlack.copy(alpha = 0.40f))
}

// ----------------------------------------------------------------------------
// 27. 吹灭读书灯，一身都是月 · Extinguished Lamp & Moonlight
// ----------------------------------------------------------------------------
private fun DrawScope.drawReadingLampMoonTheme() {
    val w = size.width; val h = size.height
    // Vast serene moonlight flood
    drawCircle(brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.30f), InkIndigo.copy(alpha = 0.08f), Color.Transparent), center = Offset(w * 0.80f, h * 0.50f), radius = 100.dp.toPx()), radius = 100.dp.toPx(), center = Offset(w * 0.80f, h * 0.50f))
    // Tiny lamp outline
    val lx = w * 0.84f; val ly = h * 0.82f
    val lamp = Path().apply { moveTo(lx - 8.dp.toPx(), ly); lineTo(lx + 8.dp.toPx(), ly); lineTo(lx + 4.dp.toPx(), ly - 12.dp.toPx()); lineTo(lx - 4.dp.toPx(), ly - 12.dp.toPx()); close() }
    drawPath(lamp, color = InkBlack.copy(alpha = 0.40f))
}

// ----------------------------------------------------------------------------
// 28. 揖让月在手，动摇风满怀 · Moon in Hand & Breeze in Sleeve
// ----------------------------------------------------------------------------
private fun DrawScope.drawMoonInHandWindTheme() {
    val w = size.width; val h = size.height
    // Cupped water basin ripple holding moon
    val rx = w * 0.78f; val ry = h * 0.75f
    drawOval(color = InkIndigo.copy(alpha = 0.16f), topLeft = Offset(rx - 36.dp.toPx(), ry - 18.dp.toPx()), size = Size(72.dp.toPx(), 36.dp.toPx()), style = Stroke(width = 1.4.dp.toPx()))
    drawCircle(color = InkOchre.copy(alpha = 0.30f), radius = 12.dp.toPx(), center = Offset(rx, ry))
    // Sweeping wind trail
    val wind = Path().apply { moveTo(w * 0.45f, h * 0.65f); cubicTo(w * 0.65f, h * 0.58f, w * 0.82f, h * 0.68f, w, h * 0.60f) }
    drawPath(wind, color = InkIndigo.copy(alpha = 0.20f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
}

// ----------------------------------------------------------------------------
// 29. 绿芜墙绕青苔院，中庭日淡芭蕉卷 · Mossy Courtyard & Plantain Leaf
// ----------------------------------------------------------------------------
private fun DrawScope.drawMossCourtyardPlantainTheme() {
    val w = size.width; val h = size.height
    val wall = Path().apply { moveTo(w * 0.65f, h); lineTo(w * 0.65f, h * 0.65f); lineTo(w, h * 0.65f); lineTo(w, h); close() }
    drawPath(wall, color = InkSage.copy(alpha = 0.12f))
    // Curling Plantain Leaf (芭蕉叶)
    val leaf = Path().apply {
        moveTo(w * 0.72f, h); cubicTo(w * 0.70f, h * 0.62f, w * 0.82f, h * 0.48f, w * 0.90f, h * 0.42f)
        cubicTo(w * 0.96f, h * 0.55f, w * 0.86f, h * 0.75f, w * 0.84f, h); close()
    }
    drawPath(leaf, color = InkSage.copy(alpha = 0.35f))
}

// ----------------------------------------------------------------------------
// 30. 小鱼跳出绿萍中 · Leaping Fish in Duckweed
// ----------------------------------------------------------------------------
private fun DrawScope.drawFishJumpingDuckweedTheme() {
    val w = size.width; val h = size.height
    // Duckweed patches
    listOf(Offset(w * 0.68f, h * 0.85f), Offset(w * 0.78f, h * 0.82f), Offset(w * 0.88f, h * 0.86f)).forEach { pt ->
        drawOval(color = InkSage.copy(alpha = 0.35f), topLeft = pt, size = Size(20.dp.toPx(), 10.dp.toPx()))
    }
    // Arched leaping carp
    val fx = w * 0.76f; val fy = h * 0.70f
    val fish = Path().apply {
        moveTo(fx - 14.dp.toPx(), fy + 12.dp.toPx())
        quadraticTo(fx, fy - 16.dp.toPx(), fx + 14.dp.toPx(), fy + 6.dp.toPx())
        quadraticTo(fx + 2.dp.toPx(), fy - 4.dp.toPx(), fx - 14.dp.toPx(), fy + 12.dp.toPx())
    }
    drawPath(fish, color = InkVermilion.copy(alpha = 0.60f))
    // Splash rings
    drawOval(color = InkIndigo.copy(alpha = 0.22f), topLeft = Offset(fx - 18.dp.toPx(), fy + 12.dp.toPx()), size = Size(36.dp.toPx(), 14.dp.toPx()), style = Stroke(width = 1.2.dp.toPx()))
}

private fun DrawScope.drawNightZenWash() {
    val w = size.width
    val h = size.height
    // Soft celestial moonlight wash across the night sky ("月光如水")
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x18D6E2EC), Color(0x0AE5A869), Color.Transparent),
            center = Offset(w * 0.82f, h * 0.22f),
            radius = w * 0.80f,
        ),
    )
}
