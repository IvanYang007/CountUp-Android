package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class CountUpRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private lateinit var store: CountUpStore
    private lateinit var repository: DefaultCountUpRepository

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("countup_repo_test").toFile()
        testContext = TestContext(tempDir)
        store = CountUpStore(testContext)
        repository = DefaultCountUpRepository(store)
    }

    @Test
    fun getItemsReturnsEmptyInitiallyAndReflectsAddedItems() {
        assertTrue(repository.getItems().isEmpty())

        val item = repository.addItem(name = "Meditation", epochDay = 20000, comment = "Morning streak")
        assertNotNull(item)
        assertEquals("Meditation", item!!.name)

        val items = repository.getItems()
        assertEquals(1, items.size)
        assertEquals(item.id, items[0].id)
    }

    @Test
    fun updateItemModifiesPersistedItem() {
        val item = repository.addItem(name = "Original", epochDay = 20000, comment = "Old comment")!!
        val success = repository.updateItem(id = item.id, name = "Updated Name", epochDay = 20050, comment = "New note")

        assertTrue(success)
        val items = repository.getItems()
        assertEquals("Updated Name", items[0].name)
        assertEquals(20050L, items[0].epochDay)
        assertEquals("New note", items[0].comment)
    }

    @Test
    fun deleteItemRemovesFromPersistence() {
        val item1 = repository.addItem(name = "Item 1", epochDay = 20000)!!
        val item2 = repository.addItem(name = "Item 2", epochDay = 20010)!!
        assertEquals(2, repository.getItems().size)

        val success = repository.deleteItem(item1.id)
        assertTrue(success)

        val remaining = repository.getItems()
        assertEquals(1, remaining.size)
        assertEquals(item2.id, remaining[0].id)
    }

    @Test
    fun resetToUpdatesAnchorDate() {
        val pastDay = LocalDate.now().minusDays(30).toEpochDay()
        val today = LocalDate.now().toEpochDay()
        val item = repository.addItem(name = "Habit", epochDay = pastDay)!!

        val success = repository.resetTo(item.id, today)
        assertTrue(success)

        val updated = repository.getItems().first { it.id == item.id }
        assertEquals(today, updated.epochDay)
    }

    @Test
    fun setWidgetVisibilityTogglesState() {
        val item = repository.addItem(name = "Widget Target", epochDay = 20000)!!
        assertTrue(item.showInWidget)

        assertTrue(repository.setWidgetVisibility(item.id, false))
        assertFalse(repository.getItems().first { it.id == item.id }.showInWidget)

        assertTrue(repository.setWidgetVisibility(item.id, true))
        assertTrue(repository.getItems().first { it.id == item.id }.showInWidget)
    }

    @Test
    fun backgroundThemePersistsAndRetrieves() {
        assertEquals(BackgroundTheme.AUTO_DAILY, repository.getBackgroundTheme())

        assertTrue(repository.setBackgroundTheme(BackgroundTheme.DREAM_BOAT))
        assertEquals(BackgroundTheme.DREAM_BOAT, repository.getBackgroundTheme())

        assertTrue(repository.setBackgroundTheme(BackgroundTheme.CLEAR_SPRING))
        assertEquals(BackgroundTheme.CLEAR_SPRING, repository.getBackgroundTheme())
    }

    @Test
    fun sortOrderPersistsAndRetrieves() {
        assertEquals(SortOrder.DAYS_DESC, repository.getSortOrder())

        assertTrue(repository.setSortOrder(SortOrder.DATE_DESC))
        assertEquals(SortOrder.DATE_DESC, repository.getSortOrder())

        assertTrue(repository.setSortOrder(SortOrder.NAME_ASC))
        assertEquals(SortOrder.NAME_ASC, repository.getSortOrder())
    }

    @Test
    fun addItemWithCustomIconAndCardColor() {
        val item = repository.addItem(
            name = "Anniversary",
            epochDay = 18000L,
            comment = "Dinner",
            icon = "cake",
            cardColor = "rose_clay",
        )
        assertNotNull(item)
        assertEquals("cake", item!!.icon)
        assertEquals("rose_clay", item.cardColor)

        val retrieved = repository.getItems().first { it.id == item.id }
        assertEquals("cake", retrieved.icon)
        assertEquals("rose_clay", retrieved.cardColor)
    }

    @Test
    fun updateItemWithCustomIconAndCardColor() {
        val item = repository.addItem(
            name = "Project",
            epochDay = 20000L,
            comment = "Initial",
            icon = "lightbulb",
            cardColor = "ochre_gold",
        )!!

        assertTrue(
            repository.updateItem(
                id = item.id,
                name = "Project Launch",
                epochDay = 20100L,
                comment = "Shipped",
                icon = "rocket_launch",
                cardColor = "dusty_indigo",
            )
        )

        val updated = repository.getItems().first { it.id == item.id }
        assertEquals("Project Launch", updated.name)
        assertEquals("rocket_launch", updated.icon)
        assertEquals("dusty_indigo", updated.cardColor)
    }

    @Test
    fun restoreResetAndWidgetResetRepositoryDelegation() {
        val pastDay = LocalDate.now().minusDays(30).toEpochDay()
        val item = repository.addItem(name = "Habit", epochDay = pastDay)!!

        // Reset
        val today = LocalDate.now().toEpochDay()
        assertTrue(repository.resetTo(item.id, today))

        // Restore
        assertTrue(
            repository.restoreReset(
                id = item.id,
                snapshot = ResetSnapshot(
                    epochDay = pastDay,
                    resetCount = 0,
                    totalResetDays = 0L,
                    futureFlag = false,
                ),
            )
        )
        val restored = repository.getItems().first { it.id == item.id }
        assertEquals(pastDay, restored.epochDay)

        // Widget reset record queue
        val record = WidgetResetRecord(
            id = "w1",
            itemId = item.id,
            itemName = item.name,
            snapshot = ResetSnapshot(
                epochDay = pastDay,
                resetCount = 0,
                totalResetDays = 0L,
                futureFlag = false,
            ),
            releasedDays = 30L,
            timestampMillis = System.currentTimeMillis(),
        )
        assertTrue(repository.recordWidgetReset(record))
        assertEquals(1, repository.getPendingWidgetResets().size)
        assertEquals("w1", repository.getPendingWidgetResets().first().id)

        assertTrue(repository.dismissWidgetReset("w1"))
        assertTrue(repository.getPendingWidgetResets().isEmpty())
    }
}
