package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetRowTest {

    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun emptyItemsYieldNoRows() {
        assertTrue(widgetRows(emptyList(), today).isEmpty())
    }

    @Test
    fun rowsCarryIdNameAndCount() {
        val items = listOf(
            CountUpItem(id = "a", name = "Haircut", epochDay = 20667), // 2026-08-02, 17 days
            CountUpItem(id = "b", name = "Today", epochDay = 20684),   // today, 0 days
        )
        val rows = widgetRows(items, today)
        assertEquals(2, rows.size)
        assertEquals("a", rows[0].id)
        assertEquals("Haircut", rows[0].name)
        assertEquals(17, rows[0].count)
        assertEquals("b", rows[1].id)
        assertEquals("Today", rows[1].name)
        assertEquals(0, rows[1].count)
    }

    @Test
    fun orderMatchesStoredOrder() {
        val items = listOf(
            CountUpItem(id = "1", name = "One", epochDay = 20000),
            CountUpItem(id = "2", name = "Two", epochDay = 30000),
            CountUpItem(id = "3", name = "Three", epochDay = 10000),
        )
        assertEquals(listOf("One", "Two", "Three"), widgetRows(items, today).map { it.name })
    }

    @Test
    fun widgetRowsWith20PlusItemsPreservesCountAndOrder() {
        val items = (1..25).map { index ->
            CountUpItem(id = "id$index", name = "Item$index", epochDay = today.toEpochDay() - index)
        }
        val rows = widgetRows(items, today)
        assertEquals(25, rows.size)
        // Verify order is preserved
        rows.forEachIndexed { index, row ->
            assertEquals("Item${index + 1}", row.name)
            assertEquals(index + 1L, row.count) // days since = index + 1
        }
    }

    @Test
    fun veryLongItemNamesPreservedIntact() {
        val longName = "This is a very long item name that might be truncated in some UI but should be preserved intact in the widget row data structure for proper display"
        val items = listOf(
            CountUpItem(id = "long", name = longName, epochDay = today.toEpochDay() - 5)
        )
        val rows = widgetRows(items, today)
        assertEquals(1, rows.size)
        assertEquals(longName, rows[0].name)
        assertEquals(5, rows[0].count)
    }

    @Test
    fun dayCountsGreaterThan9999() {
        // Test with a date over 9999 days ago (~27+ years)
        val veryOldDate = today.minusDays(12000)
        val items = listOf(
            CountUpItem(id = "old", name = "Very Old", epochDay = veryOldDate.toEpochDay())
        )
        val rows = widgetRows(items, today)
        assertEquals(1, rows.size)
        assertEquals("Very Old", rows[0].name)
        assertEquals(12000, rows[0].count)
    }

    @Test
    fun futureAnchorProducesNegativeCountAndCarriesFlag() {
        val items = listOf(
            CountUpItem(id = "f", name = "Trip", epochDay = today.toEpochDay() + 3, futureFlag = true),
            CountUpItem(id = "p", name = "Past", epochDay = today.toEpochDay() - 2, futureFlag = true),
            CountUpItem(id = "n", name = "Normal", epochDay = today.toEpochDay() - 1, futureFlag = false),
        )
        val rows = widgetRows(items, today)
        assertEquals(-3, rows[0].count)
        assertTrue(rows[0].futureFlag)
        // Arrived-future styling applies only when the flag is set AND the
        // anchor day has arrived or passed.
        assertTrue(!arrivedFuture(rows[0]))
        assertTrue(arrivedFuture(rows[1]))
        assertTrue(!arrivedFuture(rows[2]))
    }

    @Test
    fun hiddenItemsAreExcludedAndVisibleKeepOrder() {
        val items = listOf(
            CountUpItem(id = "1", name = "One", epochDay = today.toEpochDay() - 1, showInWidget = true),
            CountUpItem(id = "2", name = "Two", epochDay = today.toEpochDay() - 2, showInWidget = false),
            CountUpItem(id = "3", name = "Three", epochDay = today.toEpochDay() - 3, showInWidget = true),
        )
        val rows = widgetRows(items, today)
        assertEquals(listOf("One", "Three"), rows.map { it.name })
    }

    @Test
    fun defaultItemsAllVisibleWhenNoFlagSet() {
        val items = listOf(
            CountUpItem(id = "a", name = "A", epochDay = today.toEpochDay() - 1),
            CountUpItem(id = "b", name = "B", epochDay = today.toEpochDay() - 2),
        )
        assertEquals(listOf("A", "B"), widgetRows(items, today).map { it.name })
    }

    @Test
    fun widgetRowsCarriesCustomIconAndCardColor() {
        val items = listOf(
            CountUpItem(id = "1", name = "Meditation", epochDay = today.toEpochDay() - 10, icon = "spa", cardColor = "willow_sage"),
            CountUpItem(id = "2", name = "Workout", epochDay = today.toEpochDay() - 5, icon = "fitness_center", cardColor = "terracotta"),
        )
        val rows = widgetRows(items, today)
        assertEquals(2, rows.size)
        assertEquals("spa", rows[0].icon)
        assertEquals("willow_sage", rows[0].cardColor)
        assertEquals("fitness_center", rows[1].icon)
        assertEquals("terracotta", rows[1].cardColor)
    }

    @Test
    fun widgetRowsCarriesResetCount() {
        val items = listOf(
            CountUpItem(id = "1", name = "Reset Once", epochDay = today.toEpochDay() - 10, resetCount = 1),
            CountUpItem(id = "2", name = "Never Reset", epochDay = today.toEpochDay() - 5, resetCount = 0),
        )
        val rows = widgetRows(items, today)
        assertEquals(1, rows[0].resetCount)
        assertEquals(0, rows[1].resetCount)
    }

    @Test
    fun widgetRowsCarriesResetCountForFutureAndArrivedFutureItems() {
        val items = listOf(
            CountUpItem(id = "future", name = "Future Countdown", epochDay = today.toEpochDay() + 10, futureFlag = true, resetCount = 2),
            CountUpItem(id = "arrived", name = "Arrived Future", epochDay = today.toEpochDay() - 3, futureFlag = true, resetCount = 5),
        )
        val rows = widgetRows(items, today)
        assertEquals(2, rows.size)
        assertEquals(-10L, rows[0].count)
        assertEquals(2, rows[0].resetCount)
        assertEquals(3L, rows[1].count)
        assertEquals(5, rows[1].resetCount)
        assertTrue(arrivedFuture(rows[1]))
    }

    @Test
    fun widgetCircleStyleMatchesAppCardBadgeForDefaultAndCustomCards() {
        val defaultRow = WidgetRowData(id = "item1", name = "Default Item", count = 5, futureFlag = false, cardColor = "")
        val defaultStyleLight = resolveWidgetCircleStyle(defaultRow, isDark = false)
        val defaultStyleDark = resolveWidgetCircleStyle(defaultRow, isDark = true)
        // Default card in app has Ochre Gold badge
        assertEquals(0xFFDEB285.toInt(), defaultStyleLight.circleColor)
        assertEquals(0xFF2C2416.toInt(), defaultStyleLight.textInk)
        assertEquals(0xFFDEB285.toInt(), defaultStyleDark.circleColor)
        assertEquals(0xFF2C2416.toInt(), defaultStyleDark.textInk)

        // Custom presets match their exact in-app badges across both modes
        val sageForestRow = WidgetRowData(id = "item2", name = "Garden", count = 10, futureFlag = false, cardColor = "sage_forest")
        val sageForestLight = resolveWidgetCircleStyle(sageForestRow, isDark = false)
        val sageForestDark = resolveWidgetCircleStyle(sageForestRow, isDark = true)
        assertEquals(0xFF33523D.toInt(), sageForestLight.circleColor) // Deep Forest badge
        assertEquals(0xFFFFFFFF.toInt(), sageForestLight.textInk)
        assertEquals(0xFF6DB88A.toInt(), sageForestDark.circleColor)
        assertEquals(0xFF2C2416.toInt(), sageForestDark.textInk) // ZenInkBlack ensures > 6.0:1 contrast on bright dark badge

        val paperSageRow = WidgetRowData(id = "item3", name = "Reading", count = 15, futureFlag = false, cardColor = "paper_sage")
        val paperSageStyle = resolveWidgetCircleStyle(paperSageRow, isDark = false)
        assertEquals(0xFF5E8C6D.toInt(), paperSageStyle.circleColor) // Willow Sage badge
        assertEquals(0xFFFFFFFF.toInt(), paperSageStyle.textInk)
    }

    @Test
    fun widgetCircleStyleHonorsCustomCardPresets() {
        val customRow = WidgetRowData(id = "custom", name = "Custom", count = 20, futureFlag = false, cardColor = "ink_gold")
        val style = resolveWidgetCircleStyle(customRow, isDark = true)
        // Ochre gold badge color
        assertEquals(0xFFDEB285.toInt(), style.circleColor)
        // Ochre gold is light, so text ink is dark
        assertEquals(0xFF2C2416.toInt(), style.textInk)
    }

    @Test
    fun widgetActionAndExtraConstantsMatch() {
        assertEquals("com.countup.app.ACTION_ADD_ITEM", ACTION_ADD_ITEM)
        assertEquals("com.countup.app.EXTRA_INITIAL_CARD_STYLE_CATEGORY", EXTRA_INITIAL_CARD_STYLE_CATEGORY)
    }

    @Test
    fun widgetRowsFilteringBySuiteOnlyIncludesMatchingItems() {
        val washiItem = CountUpItem(id = "1", name = "Washi", epochDay = today.toEpochDay() - 1, cardColor = "paper_sage", showInWidget = true)
        val earthItem = CountUpItem(id = "2", name = "Earth", epochDay = today.toEpochDay() - 2, cardColor = "celadon_bamboo", showInWidget = true)
        val sumiItem = CountUpItem(id = "3", name = "Sumi", epochDay = today.toEpochDay() - 3, cardColor = "ink_gold", showInWidget = true)
        val allItems = listOf(washiItem, earthItem, sumiItem)

        val washiRows = widgetRows(allItems.filter { it.matchesStyleFilter("washi") }, today)
        assertEquals(1, washiRows.size)
        assertEquals("Washi", washiRows[0].name)

        val earthRows = widgetRows(allItems.filter { it.matchesStyleFilter("earth") }, today)
        assertEquals(1, earthRows.size)
        assertEquals("Earth", earthRows[0].name)

        val sumiRows = widgetRows(allItems.filter { it.matchesStyleFilter("sumi") }, today)
        assertEquals(1, sumiRows.size)
        assertEquals("Sumi", sumiRows[0].name)

        val allRows = widgetRows(allItems.filter { it.matchesStyleFilter(CountUpStore.OVERVIEW_FILTER_ALL) }, today)
        assertEquals(3, allRows.size)
    }

    @Test
    fun nextOverviewStyleFilterCyclesThroughAllSuites() {
        assertEquals("washi", nextOverviewStyleFilter(CountUpStore.OVERVIEW_FILTER_ALL))
        assertEquals("earth", nextOverviewStyleFilter("washi"))
        assertEquals("sumi", nextOverviewStyleFilter("earth"))
        assertEquals(CountUpStore.OVERVIEW_FILTER_ALL, nextOverviewStyleFilter("sumi"))
        assertEquals("washi", nextOverviewStyleFilter("unknown_value"))
    }

    @Test
    fun resolveOverviewDotColorReturnsSuiteColors() {
        // Day mode
        assertEquals(0xFFDEB285.toInt(), resolveOverviewDotColor("washi", night = false))
        assertEquals(0xFF6DB88A.toInt(), resolveOverviewDotColor("earth", night = false))
        assertEquals(0xFF3C3F41.toInt(), resolveOverviewDotColor("sumi", night = false))
        assertEquals(0xFF8C8275.toInt(), resolveOverviewDotColor("all", night = false))

        // Night mode
        assertEquals(0xFFE8C5A0.toInt(), resolveOverviewDotColor("washi", night = true))
        assertEquals(0xFF8BC4A2.toInt(), resolveOverviewDotColor("earth", night = true))
        assertEquals(0xFFD4A574.toInt(), resolveOverviewDotColor("sumi", night = true))
        assertEquals(0xFFA8A095.toInt(), resolveOverviewDotColor("all", night = true))
    }

    @Test
    fun cycleOverviewFilterActionConstantMatches() {
        assertEquals("com.countup.app.ACTION_CYCLE_OVERVIEW_FILTER", ACTION_CYCLE_OVERVIEW_FILTER)
    }

    @Test
    fun resolveOverviewTitleResReturnsCorrectResourcePairs() {
        // "all" must resolve to category_all (e.g. "All" / "全部")
        assertEquals(Pair(R.string.app_name, R.string.category_all), resolveOverviewTitleRes(CountUpStore.OVERVIEW_FILTER_ALL))
        // Specific suites resolve to their respective category labels
        assertEquals(Pair(R.string.app_name, R.string.color_category_washi), resolveOverviewTitleRes("washi"))
        assertEquals(Pair(R.string.app_name, R.string.color_category_earth), resolveOverviewTitleRes("earth"))
        assertEquals(Pair(R.string.app_name, R.string.color_category_sumi), resolveOverviewTitleRes("sumi"))
        // Unknown filter falls back to category_all
        assertEquals(Pair(R.string.app_name, R.string.category_all), resolveOverviewTitleRes("unknown"))
    }

    @Test
    fun resolveOverviewPaperColorReturnsCorrectPalette() {
        // Day mode: "all" must remain strictly identical to current production (#F5E6D3)
        assertEquals(0xFFF5E6D3.toInt(), resolveOverviewPaperColor(CountUpStore.OVERVIEW_FILTER_ALL, night = false))
        assertEquals(0xFFF7E3C8.toInt(), resolveOverviewPaperColor("washi", night = false))
        assertEquals(0xFFEBECE3.toInt(), resolveOverviewPaperColor("earth", night = false))
        assertEquals(0xFFECEBE8.toInt(), resolveOverviewPaperColor("sumi", night = false))
        assertEquals(0xFFF5E6D3.toInt(), resolveOverviewPaperColor("fallback", night = false))

        // Night mode: "all" must remain strictly identical to current production (#191B17)
        assertEquals(0xFF191B17.toInt(), resolveOverviewPaperColor(CountUpStore.OVERVIEW_FILTER_ALL, night = true))
        assertEquals(0xFF201B15.toInt(), resolveOverviewPaperColor("washi", night = true))
        assertEquals(0xFF161B17.toInt(), resolveOverviewPaperColor("earth", night = true))
        assertEquals(0xFF141415.toInt(), resolveOverviewPaperColor("sumi", night = true))
        assertEquals(0xFF191B17.toInt(), resolveOverviewPaperColor("fallback", night = true))
    }

    @Test
    fun resolveOverviewDividerColorReturnsCorrectPalette() {
        // Day mode
        assertEquals(0x66E3D3B8, resolveOverviewDividerColor(CountUpStore.OVERVIEW_FILTER_ALL, night = false))
        assertEquals(0x66DFBE93, resolveOverviewDividerColor("washi", night = false))
        assertEquals(0x66CEDBD1, resolveOverviewDividerColor("earth", night = false))
        assertEquals(0x66CBC7C0, resolveOverviewDividerColor("sumi", night = false))

        // Night mode
        assertEquals(0x403A3D35, resolveOverviewDividerColor(CountUpStore.OVERVIEW_FILTER_ALL, night = true))
        assertEquals(0x4045382D, resolveOverviewDividerColor("washi", night = true))
        assertEquals(0x40333C36, resolveOverviewDividerColor("earth", night = true))
        assertEquals(0x402E3033, resolveOverviewDividerColor("sumi", night = true))
    }

    @Test
    fun resolveOverviewMutedInkReturnsCorrectPalette() {
        // Day mode
        assertEquals(0xFF6B5D4F.toInt(), resolveOverviewMutedInk(CountUpStore.OVERVIEW_FILTER_ALL, night = false))
        assertEquals(0xFF68513B.toInt(), resolveOverviewMutedInk("washi", night = false))
        assertEquals(0xFF546358.toInt(), resolveOverviewMutedInk("earth", night = false))
        assertEquals(0xFF505359.toInt(), resolveOverviewMutedInk("sumi", night = false))

        // Night mode
        assertEquals(0xFFC4BEAE.toInt(), resolveOverviewMutedInk(CountUpStore.OVERVIEW_FILTER_ALL, night = true))
        assertEquals(0xFFD2C3AA.toInt(), resolveOverviewMutedInk("washi", night = true))
        assertEquals(0xFFBDC7BE.toInt(), resolveOverviewMutedInk("earth", night = true))
        assertEquals(0xFFC4C5C8.toInt(), resolveOverviewMutedInk("sumi", night = true))
    }

    @Test
    fun widgetViewsFactoryHandlesOutOfBoundsSafelyWithoutCrashing() {
        val tempDir = java.nio.file.Files.createTempDirectory("widget_factory_test").toFile()
        val context = TestContext(tempDir)
        val factory = WidgetViewsFactory(context, appWidgetId = 99)

        // Out of bounds on empty factory
        assertEquals(0, factory.count)
        assertEquals(0L, factory.getItemId(0))
        assertEquals(42L, factory.getItemId(42))
        val viewsOutOfBounds = factory.getViewAt(999)
        org.junit.Assert.assertNotNull(viewsOutOfBounds)
    }
}

