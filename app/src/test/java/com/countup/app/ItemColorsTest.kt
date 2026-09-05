package com.countup.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemColorsTest {

    @Test
    fun `all 9 curated presets have valid cardBg, badgeBg, and nameRes`() {
        assertEquals(9, CARD_COLOR_PRESETS.size)
        CARD_COLOR_PRESETS.forEach { preset ->
            assertNotNull(preset.id)
            assertTrue(preset.nameRes > 0)
            assertNotNull(preset.cardBg)
            assertNotNull(preset.badgeBg)
            assertNotNull(preset.badgeTint)
            assertNotNull(preset.primaryInk)
            assertNotNull(preset.mutedInk)
        }
    }

    @Test
    fun `empty id resolves to default Paper and Gold style`() {
        val style = resolveCardStyle("")
        assertEquals(Color(0xFFFFFFFF), style.cardBg)
        assertEquals(Color(0xFFDEB285), style.badgeBg)
        assertEquals(ZenInkBlack, style.badgeTint)
    }

    @Test
    fun `null id resolves to default Paper and Gold style`() {
        val style = resolveCardStyle(null)
        assertEquals(Color(0xFFFFFFFF), style.cardBg)
        assertEquals(Color(0xFFDEB285), style.badgeBg)
    }

    @Test
    fun `legacy color ids map to refined modern combinations`() {
        assertEquals("sage_forest", resolveCardStyle("willow_sage").id)
        assertEquals("ink_gold", resolveCardStyle("deep_ink").id)
        assertEquals("paper_terracotta", resolveCardStyle("terracotta").id)
        assertEquals("paper_indigo", resolveCardStyle("dusty_indigo").id)
        assertEquals("ink_gold", resolveCardStyle("ochre_gold").id)
        assertEquals("ink_crimson", resolveCardStyle("rose_clay").id)
        assertEquals("paper_terracotta", resolveCardStyle("warm_sand").id)
    }

    @Test
    fun `isDarkCardBackground identifies light vs dark backgrounds correctly`() {
        assertTrue(isDarkCardBackground(Color(0xFF24201A))) // Sumi Ink
        assertTrue(isDarkCardBackground(Color(0xFF5E8C6D))) // Willow Sage
        assertFalse(isDarkCardBackground(Color.White))
        assertFalse(isDarkCardBackground(Color(0xFFFAFAF7))) // Paper Linen
    }

    @Test
    fun `cardPrimaryInk provides high contrast on light and dark cards`() {
        val darkInk = cardPrimaryInk(Color.White)
        val lightInk = cardPrimaryInk(Color(0xFF24201A))

        assertEquals(ZenInkBlack, darkInk)
        assertEquals(Color(0xFFFAF7F2), lightInk)
    }

    @Test
    fun `cardMutedInk provides accessible secondary contrast`() {
        val lightBgMuted = cardMutedInk(Color.White)
        val darkBgMuted = cardMutedInk(Color(0xFF24201A))

        assertEquals(ZenInkMuted, lightBgMuted)
        assertEquals(Color(0xFFD6C8B7), darkBgMuted)
    }

    @Test
    fun `cardBadgeColor resolves badge color from preset`() {
        val defaultBadge = cardBadgeColor("")
        assertEquals(Color(0xFFDEB285), defaultBadge)

        val inkGoldBadge = cardBadgeColor("ink_gold")
        assertEquals(Color(0xFFDEB285), inkGoldBadge)

        val paperTerracottaBadge = cardBadgeColor("paper_terracotta")
        assertEquals(Color(0xFFD87A4F), paperTerracottaBadge)
    }

    @Test
    fun `cardBadgeTint provides high contrast on bright vs dark badges`() {
        val darkBadgeTint = cardBadgeTint(Color(0xFF33523D)) // Deep forest
        assertEquals(Color.White, darkBadgeTint)

        val brightBadgeTint = cardBadgeTint(Color(0xFFDEB285)) // Ochre gold
        assertEquals(ZenInkBlack, brightBadgeTint)
    }

    @Test
    fun `default white card presets contain only light paper background styles`() {
        assertEquals(3, DEFAULT_WHITE_CARD_COLOR_IDS.size)
        assertEquals(listOf("", "paper_terracotta", "paper_sage"), DEFAULT_WHITE_CARD_COLOR_IDS)

        DEFAULT_WHITE_CARD_COLOR_IDS.forEach { id ->
            val style = resolveCardStyle(id)
            assertFalse(style.isDark)
            assertFalse(isDarkCardBackground(style.cardBg))
        }
    }

    @Test
    fun `randomWhiteCardColor returns valid white card preset`() {
        repeat(50) {
            val randomId = randomWhiteCardColor()
            assertTrue(DEFAULT_WHITE_CARD_COLOR_IDS.contains(randomId))
            val style = resolveCardStyle(randomId)
            assertFalse(style.isDark)
        }
    }
}
