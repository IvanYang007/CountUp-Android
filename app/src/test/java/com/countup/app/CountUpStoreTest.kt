package com.countup.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CountUpStoreTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("countup_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun cleanInstallInitializesEmptyListAndWritesBackup() {
        val store = CountUpStore(testContext)
        val items = store.items()
        assertTrue(items.isEmpty())

        val backupFile = File(tempDir, "countup_backup.json")
        assertTrue(backupFile.exists())
    }

    @Test
    fun addItemPersistsToBothPreferencesAndDiskBackup() {
        val store = CountUpStore(testContext)
        val item = store.addItem(name = "Meditation", epochDay = 20000, comment = "Calm streak")
        assertNotNull(item)
        assertEquals("Meditation", item!!.name)
        assertEquals(20000L, item.epochDay)

        // Verify read back from new store instance
        val store2 = CountUpStore(testContext)
        val items2 = store2.items()
        assertEquals(1, items2.size)
        assertEquals(item.id, items2[0].id)
        assertEquals("Meditation", items2[0].name)

        // Verify disk backup content
        val backupFile = File(tempDir, "countup_backup.json")
        assertTrue(backupFile.exists())
        val backupContent = backupFile.readText(Charsets.UTF_8)
        assertTrue(backupContent.contains("Meditation"))
    }

    @Test
    fun legacyHaircutMigrationPreservesExistingUserData() {
        // Populate legacy haircut preference before store creation
        val legacyPrefs = testContext.getSharedPreferences("haircut_prefs", Context.MODE_PRIVATE)
        legacyPrefs.edit().putLong("last_haircut_epoch_day", 19500L).commit()

        val store = CountUpStore(testContext)
        val items = store.items()
        assertEquals(1, items.size)
        assertEquals("Haircut", items[0].name)
        assertEquals(19500L, items[0].epochDay)

        // Migration is marked done
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("migrated_v1", false))
    }

    @Test
    fun secondaryBackupAutoHealsWhenSharedPreferencesIsBlanked() {
        val store1 = CountUpStore(testContext)
        val item = store1.addItem("Workout", 20100L)!!
        assertEquals(1, store1.items().size)

        // Simulate OS clearing SharedPreferences (e.g. storage glitch)
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // Create new store instance; it should restore from countup_backup.json
        val store2 = CountUpStore(testContext)
        val recovered = store2.items()
        assertEquals(1, recovered.size)
        assertEquals(item.id, recovered[0].id)
        assertEquals("Workout", recovered[0].name)
    }

    @Test
    fun corruptedPayloadIsSalvagedAndQuarantinedWithoutDataLoss() {
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        // Corrupted/truncated payload with valid items inside
        val corrupted =
            "[{\"id\":\"c1\",\"name\":\"Piano\",\"epochDay\":19000},{\"id\":\"c2\",\"name\":\"Language\",\"epochDay\":19100},{\"id\":\"c3\""
        prefs.edit().putString("items_v1", corrupted).commit()

        val store = CountUpStore(testContext)
        val salvaged = store.items()

        // Both intact items are salvaged
        assertEquals(2, salvaged.size)
        assertEquals(listOf("Piano", "Language"), salvaged.map { it.name })

        // Corrupted raw payload was preserved in quarantine key
        val quarantined = prefs.getString("items_v1_quarantine", null)
        assertEquals(corrupted, quarantined)
    }

    @Test
    fun updateAndDeleteMutateBothPreferencesAndBackup() {
        val store = CountUpStore(testContext)
        val item = store.addItem("Plant", 100L)!!
        assertTrue(store.updateItem(item.id, "Bonsai", 150L, "New soil"))

        val updated = store.items()[0]
        assertEquals("Bonsai", updated.name)
        assertEquals(150L, updated.epochDay)
        assertEquals("New soil", updated.comment)

        assertTrue(store.deleteItem(item.id))
        assertTrue(store.items().isEmpty())

        val backupContent = File(tempDir, "countup_backup.json").readText(Charsets.UTF_8)
        assertEquals("[]", backupContent)
    }

    @Test
    fun toggleWidgetVisibilityPersistsCorrectly() {
        val store = CountUpStore(testContext)
        val item = store.addItem("Widget Habit", 100L)!!
        assertTrue(item.showInWidget)

        assertTrue(store.setWidgetVisibility(item.id, false))
        assertFalse(store.items()[0].showInWidget)

        assertTrue(store.setWidgetVisibility(item.id, true))
        assertTrue(store.items()[0].showInWidget)
    }

    @Test
    fun quarantinePruningKeepsAtMostThreeHistoricalSnapshots() {
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)

        // Simulate 5 consecutive corrupt read incidents over time
        for (i in 1..5) {
            prefs.edit().putString("items_v1", "broken_$i").commit()
            val store = CountUpStore(testContext)
            store.items()
            Thread.sleep(2) // ensure distinct timestamps
        }

        val quarantineKeys = prefs.all.keys.filter { it.startsWith("items_v1_quarantine_") }
        assertTrue("Expected <= 3 timestamped quarantine keys, found ${quarantineKeys.size}", quarantineKeys.size <= 3)
    }

    @Test
    fun sortOrderDefaultsToDaysDescAndPersistsSelection() {
        val store = CountUpStore(testContext)
        assertEquals(SortOrder.DAYS_DESC, store.getSortOrder())

        assertTrue(store.setSortOrder(SortOrder.DATE_DESC))
        assertEquals(SortOrder.DATE_DESC, store.getSortOrder())

        // Read back from a new instance
        val store2 = CountUpStore(testContext)
        assertEquals(SortOrder.DATE_DESC, store2.getSortOrder())

        assertTrue(store2.setSortOrder(SortOrder.NAME_ASC))
        assertEquals(SortOrder.NAME_ASC, store2.getSortOrder())
    }

    @Test
    fun sortOrderFallsBackGracefullyOnCorruptValue() {
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("sort_order_v1", "unrecognized_mode_xyz").commit()

        val store = CountUpStore(testContext)
        assertEquals(SortOrder.DAYS_DESC, store.getSortOrder())
    }
}
