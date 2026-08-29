package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SortOrderTest {

    private val today = LocalDate.of(2026, 2, 28)

    @Test
    fun sortOrderCycleTransitionsThroughAllThreeStates() {
        assertEquals(SortOrder.DATE_DESC, SortOrder.DAYS_DESC.next())
        assertEquals(SortOrder.NAME_ASC, SortOrder.DATE_DESC.next())
        assertEquals(SortOrder.DAYS_DESC, SortOrder.NAME_ASC.next())
    }

    @Test
    fun sortOrderFromIdMapsExpectedValuesAndFallsBackToDefault() {
        assertEquals(SortOrder.DAYS_DESC, SortOrder.fromId("days_desc"))
        assertEquals(SortOrder.DATE_DESC, SortOrder.fromId("date_desc"))
        assertEquals(SortOrder.NAME_ASC, SortOrder.fromId("name_asc"))
        assertEquals(SortOrder.DAYS_DESC, SortOrder.fromId(null))
        assertEquals(SortOrder.DAYS_DESC, SortOrder.fromId("unknown_mode"))
        assertEquals(SortOrder.DAYS_DESC, SortOrder.fromId(""))
    }

    @Test
    fun sortItemsEmptyAndSingleItemReturnsImmediately() {
        val empty = emptyList<CountUpItem>()
        assertEquals(empty, sortItems(empty, SortOrder.DAYS_DESC, today))

        val single = listOf(CountUpItem(id = "1", name = "Haircut", epochDay = today.toEpochDay() - 10))
        assertEquals(single, sortItems(single, SortOrder.DAYS_DESC, today))
    }

    @Test
    fun sortItemsByDaysDescOrdersLargestCountsFirst() {
        val item1 = CountUpItem(id = "1", name = "Haircut", epochDay = today.toEpochDay() - 34) // 34 days
        val item2 = CountUpItem(id = "2", name = "Plant", epochDay = today.toEpochDay() - 7) // 7 days
        val item3 = CountUpItem(id = "3", name = "Meditation", epochDay = today.toEpochDay() - 48) // 48 days
        val item4 = CountUpItem(id = "4", name = "Vacation", epochDay = today.toEpochDay() + 10) // -10 days (future)

        val list = listOf(item1, item2, item3, item4)
        val sorted = sortItems(list, SortOrder.DAYS_DESC, today)

        assertEquals("Meditation", sorted[0].name) // 48 days
        assertEquals("Haircut", sorted[1].name) // 34 days
        assertEquals("Plant", sorted[2].name) // 7 days
        assertEquals("Vacation", sorted[3].name) // -10 days
    }

    @Test
    fun sortItemsByDateDescOrdersNewestAnchorDateFirst() {
        val item1 = CountUpItem(id = "1", name = "Haircut", epochDay = LocalDate.of(2026, 1, 1).toEpochDay())
        val item2 = CountUpItem(id = "2", name = "Plant", epochDay = LocalDate.of(2026, 2, 20).toEpochDay())
        val item3 = CountUpItem(id = "3", name = "Trip", epochDay = LocalDate.of(2026, 3, 15).toEpochDay())

        val list = listOf(item1, item2, item3)
        val sorted = sortItems(list, SortOrder.DATE_DESC, today)

        assertEquals("Trip", sorted[0].name) // March 15
        assertEquals("Plant", sorted[1].name) // Feb 20
        assertEquals("Haircut", sorted[2].name) // Jan 1
    }

    @Test
    fun sortItemsByNameAscOrdersAlphabeticallyCaseInsensitive() {
        val item1 = CountUpItem(id = "1", name = "water plants", epochDay = today.toEpochDay() - 5)
        val item2 = CountUpItem(id = "2", name = "Air Filter", epochDay = today.toEpochDay() - 90)
        val item3 = CountUpItem(id = "3", name = "barber", epochDay = today.toEpochDay() - 30)
        val item4 = CountUpItem(id = "4", name = "Dentist", epochDay = today.toEpochDay() - 120)

        val list = listOf(item1, item2, item3, item4)
        val sorted = sortItems(list, SortOrder.NAME_ASC, today)

        assertEquals("Air Filter", sorted[0].name)
        assertEquals("barber", sorted[1].name)
        assertEquals("Dentist", sorted[2].name)
        assertEquals("water plants", sorted[3].name)
    }

    @Test
    fun sortItemsPreservesDeterministicTieBreaking() {
        val item1 = CountUpItem(id = "id-b", name = "Haircut", epochDay = today.toEpochDay() - 30)
        val item2 = CountUpItem(id = "id-a", name = "Haircut", epochDay = today.toEpochDay() - 30)

        val sorted = sortItems(listOf(item1, item2), SortOrder.DAYS_DESC, today)
        assertEquals("id-a", sorted[0].id)
        assertEquals("id-b", sorted[1].id)
    }
}
