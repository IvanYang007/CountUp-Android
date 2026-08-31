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
    fun widgetCircleStyleProvidesDifferentColorsForDefaultedCards() {
        val row1 = WidgetRowData(id = "item1", name = "Item 1", count = 5, futureFlag = false, cardColor = "")
        val row2 = WidgetRowData(id = "item2", name = "Item 2", count = 10, futureFlag = false, cardColor = "")
        val row3 = WidgetRowData(id = "item3", name = "Item 3", count = 15, futureFlag = false, cardColor = "")

        val style1 = resolveWidgetCircleStyle(row1, 0)
        val style2 = resolveWidgetCircleStyle(row2, 1)
        val style3 = resolveWidgetCircleStyle(row3, 2)

        assertTrue(DEFAULT_WIDGET_PALETTE.any { it.circleColor == style1.circleColor })
        assertTrue(DEFAULT_WIDGET_PALETTE.any { it.circleColor == style2.circleColor })
        assertTrue(DEFAULT_WIDGET_PALETTE.any { it.circleColor == style3.circleColor })
        // Verify distinct adjacent styles
        org.junit.Assert.assertNotEquals(style1.circleColor, style2.circleColor)
    }

    @Test
    fun widgetCircleStyleHonorsCustomCardPresets() {
        val customRow = WidgetRowData(id = "custom", name = "Custom", count = 20, futureFlag = false, cardColor = "ink_gold")
        val style = resolveWidgetCircleStyle(customRow, 0)
        // Ochre gold badge color
        assertEquals(0xFFDEB285.toInt(), style.circleColor)
        // Ochre gold is light, so text ink is dark
        assertEquals(0xFF2C2416.toInt(), style.textInk)
    }

    @Test
    fun widgetActionAddItemMatchesConstant() {
        assertEquals("com.countup.app.ACTION_ADD_ITEM", ACTION_ADD_ITEM)
    }
}
