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
        assertEquals(BackgroundTheme.WILLOW_LEAVES, BackgroundTheme.fromId("cold_river_snow")) // Legacy fallback
        assertEquals(BackgroundTheme.WILLOW_LEAVES, BackgroundTheme.fromId("misty_grove")) // Legacy fallback

        // Fallbacks
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(null))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId("unknown_theme"))
        assertEquals(BackgroundTheme.AUTO_DAILY, BackgroundTheme.fromId(""))
    }

    @Test
    fun next_cyclesThroughAllThemesInOrder() {
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
        )
        for (theme in nonAutoThemes) {
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 100L))
            assertEquals(theme, resolveActiveTheme(theme, epochDay = 20000L))
        }
    }

    @Test
    fun resolveActiveTheme_cycles5ThemesForAutoDaily() {
        val expectedSequence = listOf(
            BackgroundTheme.MOUNTAIN,
            BackgroundTheme.SAND_DUNES,
            BackgroundTheme.SEA_HORIZON,
            BackgroundTheme.SOLITARY_ISLE,
            BackgroundTheme.WILLOW_LEAVES,
        )

        for (i in 0..19) {
            val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = i.toLong())
            assertEquals(expectedSequence[i % 5], resolved)
        }
    }

    @Test
    fun resolveActiveTheme_handlesNegativeEpochDaysGracefully() {
        val resolved = resolveActiveTheme(BackgroundTheme.AUTO_DAILY, epochDay = -1L)
        assertEquals(BackgroundTheme.WILLOW_LEAVES, resolved)
    }

    @Test
    fun allThemesHaveValidStringResources() {
        for (theme in BackgroundTheme.entries) {
            assertNotEquals(0, theme.labelRes)
            assertTrue(theme.id.isNotBlank())
        }
    }
}
