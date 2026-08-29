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

        // Legacy fallbacks
        assertEquals(BackgroundTheme.WILLOW_LEAVES, BackgroundTheme.fromId("cold_river_snow"))
        assertEquals(BackgroundTheme.ZEN_BAMBOO, BackgroundTheme.fromId("misty_grove"))

        // Fallbacks
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(null))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId("unknown_theme"))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(""))
    }

    @Test
    fun next_cyclesThroughAll14EntriesInOrder() {
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
                BackgroundTheme.AUTO_DAILY,
            ),
            sequence,
        )
    }

    @Test
    fun resolveActiveTheme_returnsExactThemeWhenNotAutoDaily() {
        val nonAutoThemes = listOf(
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
        for (theme in nonAutoThemes) {
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 100L))
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 20000L))
        }
    }

    @Test
    fun resolveActiveTheme_cyclesAll13ThemesForAutoDaily() {
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
        )

        for (i in 0..39) {
            val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = i.toLong())
            assertEquals(expectedSequence[i % 13], resolved)
        }
    }

    @Test
    fun resolveActiveTheme_handlesNegativeEpochDaysGracefully() {
        val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = -1L)
        assertEquals(BackgroundTheme.SPRING_RAIN, resolved)
    }

    @Test
    fun all13ThemesHaveValidStringResources() {
        for (theme in BackgroundTheme.entries) {
            assertNotEquals(0, theme.labelRes)
            assertTrue(theme.id.isNotBlank())
        }
    }
}
