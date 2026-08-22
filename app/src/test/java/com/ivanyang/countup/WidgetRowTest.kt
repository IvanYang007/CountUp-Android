package com.ivanyang.countup

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
}
