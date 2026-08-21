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
    fun rowsCarryNameCountAndEpochDay() {
        val items = listOf(
            CountUpItem(id = "a", name = "Haircut", epochDay = 20667), // 2026-08-02, 17 days
            CountUpItem(id = "b", name = "Today", epochDay = 20684),   // today, 0 days
        )
        val rows = widgetRows(items, today)
        assertEquals(2, rows.size)
        assertEquals("a", rows[0].id)
        assertEquals("Haircut", rows[0].name)
        assertEquals(17, rows[0].count)
        assertEquals(20667, rows[0].epochDay)
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
}
