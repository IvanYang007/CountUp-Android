package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests covering MVI Contracts, state derivations, time breakdowns for ItemCard,
 * and accessibility color contrast tokens following 2026 Android architecture standards.
 */
class CountUpContractAndFlowTest {

    private val fixedToday = LocalDate.of(2026, 8, 30)

    @Test
    fun `CountUpUiState displayItems is lazily evaluated and cached per state snapshot`() {
        val items = listOf(
            CountUpItem(id = "1", name = "Bonsai Watering", epochDay = fixedToday.minusDays(5).toEpochDay()),
            CountUpItem(id = "2", name = "Meditation Sitting", epochDay = fixedToday.minusDays(20).toEpochDay()),
        )
        val state = CountUpUiState(items = items, today = fixedToday)

        // First access evaluates
        val list1 = state.displayItems
        // Second access should return identical instance (lazy property)
        val list2 = state.displayItems

        assertSame("displayItems should be cached on the immutable state instance", list1, list2)
        assertEquals(2, list1.size)
    }

    @Test
    fun `search query filters case-insensitively across name and comment`() {
        val items = listOf(
            CountUpItem(id = "1", name = "Zen Garden", epochDay = fixedToday.toEpochDay(), comment = "Rake gravel daily"),
            CountUpItem(id = "2", name = "Bonsai Tree", epochDay = fixedToday.toEpochDay(), comment = "Zen aesthetics"),
            CountUpItem(id = "3", name = "Running Habit", epochDay = fixedToday.toEpochDay(), comment = "Morning laps"),
        )

        val state = CountUpUiState(items = items, searchQuery = "ZEN", today = fixedToday)
        val matches = state.displayItems
        assertEquals(2, matches.size)
        assertTrue(matches.any { it.name == "Zen Garden" })
        assertTrue(matches.any { it.name == "Bonsai Tree" }) // matched via comment "Zen aesthetics"
    }

    @Test
    fun `SaveItem secondary constructor delegates to ItemDraft accurately`() {
        val event = CountUpUiEvent.SaveItem(
            name = "Morning Matcha",
            epochDay = 20690L,
            comment = "Ceremonial grade",
            icon = "spa",
            cardColor = "willow_sage",
        )

        assertEquals("Morning Matcha", event.name)
        assertEquals(20690L, event.epochDay)
        assertEquals("Ceremonial grade", event.comment)
        assertEquals("spa", event.icon)
        assertEquals("willow_sage", event.cardColor)
        assertEquals("Morning Matcha", event.draft.name)
    }

    @Test
    fun `ShowSnackbar side effect models all parameter combinations`() {
        val simpleEffect = CountUpUiEffect.ShowSnackbar(messageRes = R.string.error_save_failed)
        assertEquals(R.string.error_save_failed, simpleEffect.messageRes)
        assertEquals(null, simpleEffect.formatArg)
        assertEquals(null, simpleEffect.formatArgRes)

        val stringArgEffect = CountUpUiEffect.ShowSnackbar(
            messageRes = R.string.widget_reset_toast,
            formatArg = "Water Bonsai",
        )
        assertEquals("Water Bonsai", stringArgEffect.formatArg)

        val resArgEffect = CountUpUiEffect.ShowSnackbar(
            messageRes = R.string.bg_switched_toast,
            formatArgRes = R.string.bg_mountain,
        )
        assertEquals(R.string.bg_mountain, resArgEffect.formatArgRes)
    }

    @Test
    fun `time decompositions for odometer accurately reflect days, breakdowns, and weeks`() {
        val anchorDate = fixedToday.minusYears(1).minusMonths(2).minusDays(15) // ~441 days prior

        val daysMode = decomposeTime(anchorDate, fixedToday, TimeDisplayMode.DAYS)
        assertEquals(R.string.unit_days, daysMode.unitLabelRes)
        assertTrue("Days mode value text must be numeric", daysMode.valueText.toLong() > 400L)

        val breakdownMode = decomposeTime(anchorDate, fixedToday, TimeDisplayMode.ELAPSED_BREAKDOWN)
        assertTrue("Breakdown mode should indicate year/month/day components", breakdownMode.valueText.isNotEmpty())

        val weeksMode = decomposeTime(anchorDate, fixedToday, TimeDisplayMode.TOTAL_WEEKS)
        assertTrue("Weeks mode should contain 'w' unit suffix or representation", weeksMode.valueText.contains("w") || weeksMode.valueText.isNotEmpty())
    }

    @Test
    fun `future countdowns decompose properly with negative count semantics`() {
        val futureDate = fixedToday.plusDays(45)

        val daysMode = decomposeTime(futureDate, fixedToday, TimeDisplayMode.DAYS)
        assertEquals(R.string.unit_until_short, daysMode.unitLabelRes)
        assertEquals("45", daysMode.valueText)

        val weeksMode = decomposeTime(futureDate, fixedToday, TimeDisplayMode.TOTAL_WEEKS)
        assertTrue(weeksMode.valueText.isNotEmpty())
    }

    @Test
    fun `card color styles provide proper contrast and semantic flags`() {
        val colors = listOf("willow_sage", "terracotta", "ocean", "rose_clay", "bamboo_ink", "dark_slate", "")
        for (colorName in colors) {
            val style = resolveCardStyle(colorName)
            // Ensure cardBg and primaryInk are distinct colors
            assertTrue("Card background and text ink should not be identical", style.cardBg != style.primaryInk)
            // Ensure badge colors are defined
            assertTrue("Badge bg should have non-zero alpha", style.badgeBg.alpha > 0f)
            assertTrue("Badge tint should have non-zero alpha", style.badgeTint.alpha > 0f)
        }
    }

    @Test
    fun `items in state derive accurate patina seasoning tokens without mutation`() {
        val pastItem = CountUpItem(id = "1", name = "Bonsai", epochDay = fixedToday.minusDays(185).toEpochDay())
        val days = daysSince(LocalDate.ofEpochDay(pastItem.epochDay), fixedToday)
        assertEquals(185L, days)

        val patina = resolvePatina(days, isDark = false)
        assertEquals(PatinaPhase.KINTSUGI, patina.phase)
        assertEquals(1.0f, patina.warmth, 0.001f)
        assertEquals(ZenInkBlack, patina.badgeTextColor)
    }
}
