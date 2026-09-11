package com.countup.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemColorsTest {

    @Test
    fun `all 12 curated presets have valid cardBg, badgeBg, and nameRes`() {
        assertEquals(12, CARD_COLOR_PRESETS.size)
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
    fun `zen categories contain exactly 4 presets each and partition the 12 presets`() {
        assertEquals(3, CARD_COLOR_CATEGORIES.size)
        val categoryIds = CARD_COLOR_CATEGORIES.map { it.id }
        assertEquals(listOf("washi", "earth", "sumi"), categoryIds)

        CARD_COLOR_CATEGORIES.forEach { category ->
            assertTrue(category.labelRes > 0)
            assertEquals(4, category.presetIds.size)
            category.presetIds.forEach { presetId ->
                val preset = resolveCardStyle(presetId)
                assertNotNull(preset)
                assertTrue(CARD_COLOR_PRESETS.any { it.id == preset.id })
            }
        }

        val allCategorizedIds = CARD_COLOR_CATEGORIES.flatMap { it.presetIds }
        assertEquals(12, allCategorizedIds.size)
        assertEquals(12, allCategorizedIds.distinct().size)
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
    fun `default card style settles into warm sumi stone in dark mode with ochre gold badge`() {
        val darkDefault = resolveCardStyle("", isDark = true)
        assertEquals(ZenDarkCard, darkDefault.cardBg)
        assertEquals(Color(0xFF26231E), darkDefault.cardBg)
        assertEquals(Color(0xFFDEB285), darkDefault.badgeBg)
        assertEquals(ZenInkBlack, darkDefault.badgeTint)
        assertEquals(ZenDarkTextPrimary, darkDefault.primaryInk)
        assertEquals(ZenDarkTextSecondary, darkDefault.mutedInk)
        assertTrue(darkDefault.isDark)

        val nullDarkDefault = resolveCardStyle(null, isDark = true)
        assertEquals(ZenDarkCard, nullDarkDefault.cardBg)
        assertTrue(nullDarkDefault.isDark)
    }

    @Test
    fun `all custom card presets preserve badge identity while calibrating surfaces in dark mode`() {
        val customPresets = CARD_COLOR_PRESETS.filter { it.id.isNotEmpty() }
        assertEquals(11, customPresets.size)

        customPresets.forEach { preset ->
            val resolvedLight = resolveCardStyle(preset.id, isDark = false)
            val resolvedDark = resolveCardStyle(preset.id, isDark = true)

            // Light mode cardBg is strictly untouched
            assertEquals("Preset ${preset.id} cardBg must match in light mode", preset.cardBg, resolvedLight.cardBg)
            // Dark mode always flags isDark = true
            assertTrue("Preset ${preset.id} must be marked isDark in dark mode", resolvedDark.isDark)
            // Badge background must be non-null and high contrast
            assertNotNull(resolvedDark.badgeBg)
        }

        // Specifically verify sage_forest night calibration
        val sageLight = resolveCardStyle("sage_forest", isDark = false)
        val sageDark = resolveCardStyle("sage_forest", isDark = true)
        assertEquals(Color(0xFF5E8C6D), sageLight.cardBg) // Original light sage untouched
        assertEquals(Color(0xFF2E4D3A), sageDark.cardBg) // Deep night willow sage green
        assertEquals(Color(0xFF6DB88A), sageDark.badgeBg) // Forest leaf jade badge
        assertEquals(Color(0xFFFAF7F2), sageDark.primaryInk) // Crisp warm white text

        // Specifically verify sage_ochre night calibration
        val sageOchreLight = resolveCardStyle("sage_ochre", isDark = false)
        val sageOchreDark = resolveCardStyle("sage_ochre", isDark = true)
        assertEquals(Color(0xFF5E8C6D), sageOchreLight.cardBg)
        assertEquals(Color(0xFF3E482A), sageOchreDark.cardBg) // Deep olive tea green
        assertEquals(Color(0xFFDEB285), sageOchreDark.badgeBg)

        // Specifically verify all Washi paper cards reflect distinct moonlit paper tones in dark mode
        listOf(
            "paper_sage" to Color(0xFF232724),
            "paper_terracotta" to Color(0xFF2A2421),
            "paper_indigo" to Color(0xFF21252C),
        ).forEach { (washiId, expectedDarkBg) ->
            val washiLight = resolveCardStyle(washiId, isDark = false)
            val washiDark = resolveCardStyle(washiId, isDark = true)
            assertTrue("Light mode washi card must be white or off-white", washiLight.cardBg.luminance() > 0.85f)
            assertEquals("Dark mode washi card must match distinct moonlit paper tone", expectedDarkBg, washiDark.cardBg)
            assertNotNull(washiDark.badgeBg)
        }
    }

    @Test
    fun `all 12 dark presets have distinct background colors and lift off canvas`() {
        assertEquals(12, DARK_CARD_COLOR_PRESETS.size)
        val darkBgColors = DARK_CARD_COLOR_PRESETS.map { it.cardBg }
        assertEquals(
            "All 12 nocturnal presets must have distinct cardBg to eliminate category flattening",
            12,
            darkBgColors.distinct().size,
        )

        DARK_CARD_COLOR_PRESETS.forEach { preset ->
            assertTrue(preset.isDark)
            assertTrue(preset.cardBg.luminance() > ZenDarkCanvas.luminance())
            // Text must have high contrast (> 7:1 AAA) on all cards
            val textLum = preset.primaryInk.luminance()
            val cardLum = preset.cardBg.luminance()
            val contrast = (textLum + 0.05) / (cardLum + 0.05)
            assertTrue("Preset ${preset.id} text contrast must exceed 7.0 (AAA)", contrast >= 7.0)
        }
    }

    @Test
    fun `cardBackgroundColor respects isDark for default card and calibrates surfaces in dark mode`() {
        assertEquals(Color(0xFFFFFFFF), cardBackgroundColor("", isDark = false))
        assertEquals(ZenDarkCard, cardBackgroundColor("", isDark = true))
        assertEquals(Color(0xFF5E8C6D), cardBackgroundColor("sage_forest", isDark = false))
        assertEquals(Color(0xFF2E4D3A), cardBackgroundColor("sage_forest", isDark = true))
        assertEquals(Color(0xFF191613), cardBackgroundColor("ink_gold", isDark = true))
    }

    @Test
    fun `cardBorderColor provides hairline rule for light backgrounds in dark mode`() {
        assertEquals(ZenDarkHairline, cardBorderColor(Color.White, isDark = true))
        assertEquals(ZenDarkHairline, cardBorderColor(Color(0xFF24201A), isDark = true))
        assertEquals(Color(0x242C2416), cardBorderColor(Color.White, isDark = false))
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

    @Test
    fun `new zen presets resolve with correct backgrounds and contrast`() {
        val celadon = resolveCardStyle("celadon_bamboo")
        assertEquals(Color(0xFFE1E9E4), celadon.cardBg)
        assertEquals(Color(0xFF3B5B46), celadon.badgeBg)
        assertEquals(Color.White, celadon.badgeTint)
        assertFalse(celadon.isDark)
        assertFalse(isDarkCardBackground(celadon.cardBg))

        val linen = resolveCardStyle("linen_sandalwood")
        assertEquals(Color(0xFFECE4D5), linen.cardBg)
        assertEquals(Color(0xFF8A6B4E), linen.badgeBg)
        assertEquals(Color.White, linen.badgeTint)
        assertFalse(linen.isDark)
        assertFalse(isDarkCardBackground(linen.cardBg))

        val nightMist = resolveCardStyle("night_mist")
        assertEquals(Color(0xFF1F262E), nightMist.cardBg)
        assertEquals(Color(0xFF7A91A1), nightMist.badgeBg)
        assertEquals(Color.White, nightMist.badgeTint)
        assertTrue(nightMist.isDark)
        assertTrue(isDarkCardBackground(nightMist.cardBg))
    }

    @Test
    fun `all presets have badgeTint consistent with cardBadgeTint helper`() {
        CARD_COLOR_PRESETS.forEach { preset ->
            val expectedTint = cardBadgeTint(preset.badgeBg)
            assertEquals(
                "Preset ${preset.id} badgeTint should match cardBadgeTint",
                expectedTint,
                preset.badgeTint,
            )
        }
    }
}
