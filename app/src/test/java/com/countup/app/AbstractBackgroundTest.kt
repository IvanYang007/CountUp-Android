package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbstractBackgroundTest {

    @Test
    fun fromId_resolvesValidAndFallbackIds() {
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId("auto_daily"))
        assertEquals(BackgroundTheme.MOUNTAIN, BackgroundTheme.fromId("mountain"))
        assertEquals(BackgroundTheme.SAND_DUNES, BackgroundTheme.fromId("sand_dunes"))
        assertEquals(BackgroundTheme.SEA_HORIZON, BackgroundTheme.fromId("sea_horizon"))
        assertEquals(BackgroundTheme.SOLITARY_ISLE, BackgroundTheme.fromId("solitary_isle"))
        assertEquals(BackgroundTheme.WILLOW_LEAVES, BackgroundTheme.fromId("willow_leaves"))
        assertEquals(BackgroundTheme.ZEN_BAMBOO, BackgroundTheme.fromId("zen_bamboo"))
        assertEquals(BackgroundTheme.DREAM_BOAT, BackgroundTheme.fromId("dream_boat"))
        assertEquals(BackgroundTheme.CLEAR_SPRING, BackgroundTheme.fromId("clear_spring"))
        assertEquals(BackgroundTheme.DESERT_SUNSET, BackgroundTheme.fromId("desert_sunset"))
        assertEquals(BackgroundTheme.EGRETS_ASCENDING, BackgroundTheme.fromId("egrets_ascending"))
        assertEquals(BackgroundTheme.PLUM_SHADOW, BackgroundTheme.fromId("plum_shadow"))
        assertEquals(BackgroundTheme.ANCIENT_ROAD, BackgroundTheme.fromId("ancient_road"))
        assertEquals(BackgroundTheme.SPRING_RAIN, BackgroundTheme.fromId("spring_rain"))
        assertEquals(BackgroundTheme.LOTUS_DRAGONFLY, BackgroundTheme.fromId("lotus_dragonfly"))
        assertEquals(BackgroundTheme.CRISP_SPRING_RAIN, BackgroundTheme.fromId("crisp_spring_rain"))
        assertEquals(BackgroundTheme.SOLITARY_SAIL_RIVER, BackgroundTheme.fromId("solitary_sail_river"))
        assertEquals(BackgroundTheme.OCEAN_MOON_TIDE, BackgroundTheme.fromId("ocean_moon_tide"))
        assertEquals(BackgroundTheme.WILD_SKY_RIVER_MOON, BackgroundTheme.fromId("wild_sky_river_moon"))
        assertEquals(BackgroundTheme.GREEN_HILLS_SAIL, BackgroundTheme.fromId("green_hills_sail"))
        assertEquals(BackgroundTheme.STARS_FALL_RIVER_FLOW, BackgroundTheme.fromId("stars_fall_river_flow"))
        assertEquals(BackgroundTheme.CLOUDS_COTTAGE, BackgroundTheme.fromId("clouds_cottage"))
        assertEquals(BackgroundTheme.WINE_SPRING_MOON, BackgroundTheme.fromId("wine_spring_moon"))
        assertEquals(BackgroundTheme.APRICOT_RAIN, BackgroundTheme.fromId("apricot_rain"))
        assertEquals(BackgroundTheme.DEEP_FOREST_DEER, BackgroundTheme.fromId("deep_forest_deer"))
        assertEquals(BackgroundTheme.PEAR_BLOSSOM_WILLOW, BackgroundTheme.fromId("pear_blossom_willow"))
        assertEquals(BackgroundTheme.SPRING_WATER_SLEEP, BackgroundTheme.fromId("spring_water_sleep"))
        assertEquals(BackgroundTheme.READING_LAMP_MOON, BackgroundTheme.fromId("reading_lamp_moon"))
        assertEquals(BackgroundTheme.MOON_IN_HAND_WIND, BackgroundTheme.fromId("moon_in_hand_wind"))
        assertEquals(BackgroundTheme.MOSS_COURTYARD_PLANTAIN, BackgroundTheme.fromId("moss_courtyard_plantain"))
        assertEquals(BackgroundTheme.FISH_JUMPING_DUCKWEED, BackgroundTheme.fromId("fish_jumping_duckweed"))

        // Legacy fallbacks
        assertEquals(BackgroundTheme.WILLOW_LEAVES, BackgroundTheme.fromId("cold_river_snow"))
        assertEquals(BackgroundTheme.ZEN_BAMBOO, BackgroundTheme.fromId("misty_grove"))

        // Fallbacks
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(null))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId("unknown_theme"))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(""))
    }

    @Test
    fun next_cyclesThroughAll31EntriesInOrder() {
        var current = BackgroundTheme.AUTO_DAILY
        val sequence = mutableListOf<BackgroundTheme>()
        repeat(BackgroundTheme.entries.size) {
            current = current.next()
            sequence.add(current)
        }

        assertEquals(
            listOf(
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
                BackgroundTheme.AUTO_DAILY,
            ),
            sequence,
        )
    }

    @Test
    fun resolveActiveTheme_returnsExactThemeWhenNotAutoDaily() {
        val nonAutoThemes = BackgroundTheme.entries.filter { it != BackgroundTheme.AUTO_DAILY }
        for (theme in nonAutoThemes) {
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 100L))
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 20000L))
        }
    }

    @Test
    fun resolveActiveTheme_cyclesAll30ThemesForAutoDaily() {
        val expectedSequence = listOf(
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

        for (i in 0..59) {
            val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = i.toLong())
            assertEquals(expectedSequence[i % 30], resolved)
        }
    }

    @Test
    fun resolveActiveTheme_handlesNegativeEpochDaysGracefully() {
        val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = -1L)
        assertEquals(BackgroundTheme.FISH_JUMPING_DUCKWEED, resolved)
    }

    @Test
    fun all30ThemesHaveValidStringResources() {
        for (theme in BackgroundTheme.entries) {
            assertNotEquals(0, theme.labelRes)
            assertTrue(theme.id.isNotBlank())
        }
    }
}
