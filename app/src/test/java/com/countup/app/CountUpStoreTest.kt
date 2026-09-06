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

    @Test
    fun addItemAndPersistCustomIconAndCardColor() {
        val store = CountUpStore(testContext)
        val item = store.addItem(
            name = "Yoga",
            epochDay = 20000L,
            comment = "Morning routine",
            icon = "spa",
            cardColor = "willow_sage",
        )
        assertNotNull(item)
        assertEquals("spa", item!!.icon)
        assertEquals("willow_sage", item.cardColor)

        // Read back from a new store instance
        val store2 = CountUpStore(testContext)
        val items = store2.items()
        assertEquals(1, items.size)
        assertEquals("spa", items[0].icon)
        assertEquals("willow_sage", items[0].cardColor)

        // Verify disk backup contains icon and cardColor
        val backupContent = File(tempDir, "countup_backup.json").readText(Charsets.UTF_8)
        assertTrue(backupContent.contains("\"icon\":\"spa\""))
        assertTrue(backupContent.contains("\"cardColor\":\"willow_sage\""))
    }

    @Test
    fun updateItemPreservesCustomIconAndCardColor() {
        val store = CountUpStore(testContext)
        val item = store.addItem(
            name = "Gardening",
            epochDay = 19000L,
            comment = "Seeds",
            icon = "plant",
            cardColor = "warm_sand",
        )!!

        assertTrue(
            store.updateItem(
                id = item.id,
                name = "Bonsai Tree",
                epochDay = 19500L,
                comment = "Pruned",
                icon = "yard",
                cardColor = "terracotta",
            )
        )

        val updated = store.items()[0]
        assertEquals("Bonsai Tree", updated.name)
        assertEquals("yard", updated.icon)
        assertEquals("terracotta", updated.cardColor)
    }

    @Test
    fun backupHealingPreservesCustomIconAndCardColor() {
        val store1 = CountUpStore(testContext)
        store1.addItem(
            name = "Space Launch",
            epochDay = 20500L,
            comment = "Orbit",
            icon = "rocket_launch",
            cardColor = "deep_ink",
        )

        // Wipe SharedPreferences
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // Re-read from disk backup
        val store2 = CountUpStore(testContext)
        val items = store2.items()
        assertEquals(1, items.size)
        assertEquals("Space Launch", items[0].name)
        assertEquals("rocket_launch", items[0].icon)
        assertEquals("deep_ink", items[0].cardColor)
    }

    @Test
    fun resetToPersistsRhythmMetricsAcrossStoreReloads() {
        val store1 = CountUpStore(testContext)
        val item = store1.addItem("Meditation Streak", 20000L)!!
        assertEquals(0, item.resetCount)
        assertEquals(0L, item.totalResetDays)

        // Reset to day 20040 (cycle of 40 days)
        val success = store1.resetTo(item.id, 20040L)
        assertTrue(success)

        val updated1 = store1.items().first { it.id == item.id }
        assertEquals(20040L, updated1.epochDay)
        assertEquals(1, updated1.resetCount)
        assertEquals(40L, updated1.totalResetDays)
        assertEquals(40, updated1.averageResetDays)

        // Re-open from disk
        val store2 = CountUpStore(testContext)
        val updated2 = store2.items().first { it.id == item.id }
        assertEquals(1, updated2.resetCount)
        assertEquals(40L, updated2.totalResetDays)
        assertEquals(40, updated2.averageResetDays)
    }

    @Test
    fun resetToDoesNotTriggerWhenTargetEpochDayMatchesCurrent() {
        val store = CountUpStore(testContext)
        val item = store.addItem("Reading", 20500L)!!

        val success = store.resetTo(item.id, 20500L)
        assertFalse(success)

        val retrieved = store.items().first { it.id == item.id }
        assertEquals(0, retrieved.resetCount)
        assertEquals(0L, retrieved.totalResetDays)
        assertEquals(20500L, retrieved.epochDay)
    }

    @Test
    fun resetToSucceedsOnNegativeDayItemAndPersistsAbsCycleDays() {
        val store1 = CountUpStore(testContext)
        // Item anchored in future (20100)
        val item = store1.addItem("Future Trip", 20100L)!!

        // Reset to today (20050) -> day count was -50
        val success = store1.resetTo(item.id, 20050L)
        assertTrue(success)

        // Verify in-memory state
        val updated1 = store1.items().first { it.id == item.id }
        assertEquals(20050L, updated1.epochDay)
        assertEquals(1, updated1.resetCount)
        assertEquals(50L, updated1.totalResetDays)
        assertEquals(50, updated1.averageResetDays)
        assertFalse(updated1.futureFlag)

        // Re-read from disk in new store instance
        val store2 = CountUpStore(testContext)
        val updated2 = store2.items().first { it.id == item.id }
        assertEquals(20050L, updated2.epochDay)
        assertEquals(1, updated2.resetCount)
        assertEquals(50L, updated2.totalResetDays)
        assertEquals(50, updated2.averageResetDays)
        assertFalse(updated2.futureFlag)
    }

    @Test
    fun resetToWhenItemIsArrivedFutureClearsFutureFlagAndPersists() {
        val store1 = CountUpStore(testContext)
        // Item anchored in past with futureFlag = true
        val item = store1.addItem("Passed Launch", 20040L)!!
        val flaggedItem = item.copy(futureFlag = true)
        // Persist the arrived future item
        val storeField = store1.javaClass.getDeclaredMethod("persist", List::class.java).apply { isAccessible = true }
        storeField.invoke(store1, listOf(flaggedItem))

        val success = store1.resetTo(item.id, 20050L)
        assertTrue(success)

        val store2 = CountUpStore(testContext)
        val reloaded = store2.items().first { it.id == item.id }
        assertEquals(20050L, reloaded.epochDay)
        assertEquals(1, reloaded.resetCount)
        assertEquals(10L, reloaded.totalResetDays)
        assertEquals(10, reloaded.averageResetDays)
        assertFalse(reloaded.futureFlag)
    }

    @Test
    fun restoreResetRestoresPreviousMetricsAndPersists() {
        val store1 = CountUpStore(testContext)
        val item = store1.addItem("Workout", 20000L)!!

        // Reset to 20050
        assertTrue(store1.resetTo(item.id, 20050L))
        val afterReset = store1.items().first { it.id == item.id }
        assertEquals(1, afterReset.resetCount)
        assertEquals(50L, afterReset.totalResetDays)

        // Restore back to original
        val restored = store1.restoreReset(
            id = item.id,
            snapshot = ResetSnapshot(
                epochDay = 20000L,
                resetCount = 0,
                totalResetDays = 0L,
                futureFlag = false,
            ),
        )
        assertTrue(restored)

        val store2 = CountUpStore(testContext)
        val afterRestore = store2.items().first { it.id == item.id }
        assertEquals(20000L, afterRestore.epochDay)
        assertEquals(0, afterRestore.resetCount)
        assertEquals(0L, afterRestore.totalResetDays)
        assertFalse(afterRestore.futureFlag)
    }

    @Test
    fun widgetResetQueueCapsAtThreeNewestAndDismissesCorrectly() {
        val store = CountUpStore(testContext)
        assertTrue(store.getPendingWidgetResets().isEmpty())

        val snap = ResetSnapshot(epochDay = 20000L, resetCount = 0, totalResetDays = 0L, futureFlag = false)
        val rec1 = WidgetResetRecord(id = "r1", itemId = "i1", itemName = "Item 1", snapshot = snap, releasedDays = 10L, timestampMillis = 1000L)
        val rec2 = WidgetResetRecord(id = "r2", itemId = "i2", itemName = "Item 2", snapshot = snap, releasedDays = 20L, timestampMillis = 2000L)
        val rec3 = WidgetResetRecord(id = "r3", itemId = "i3", itemName = "Item 3", snapshot = snap, releasedDays = 30L, timestampMillis = 3000L)
        val rec4 = WidgetResetRecord(id = "r4", itemId = "i4", itemName = "Item 4", snapshot = snap, releasedDays = 40L, timestampMillis = 4000L)

        store.recordWidgetReset(rec1)
        store.recordWidgetReset(rec2)
        store.recordWidgetReset(rec3)
        assertEquals(3, store.getPendingWidgetResets().size)

        // Adding 4th should pop oldest (rec1), keeping rec4, rec3, rec2
        store.recordWidgetReset(rec4)
        val current = store.getPendingWidgetResets()
        assertEquals(3, current.size)
        assertEquals(listOf("r4", "r3", "r2"), current.map { it.id })

        // Dismiss middle record
        assertTrue(store.dismissWidgetReset("r3"))
        val afterDismiss = store.getPendingWidgetResets()
        assertEquals(2, afterDismiss.size)
        assertEquals(listOf("r4", "r2"), afterDismiss.map { it.id })
    }

    @Test
    fun deleteItemPurgesMatchingPendingWidgetResets() {
        val store = CountUpStore(testContext)
        val item = store.addItem(name = "To Delete", epochDay = 20000L)!!

        val snap = ResetSnapshot(epochDay = 20000L, resetCount = 0, totalResetDays = 0L, futureFlag = false)
        val record = WidgetResetRecord(
            id = "rec_del",
            itemId = item.id,
            itemName = item.name,
            snapshot = snap,
            releasedDays = 15L,
            timestampMillis = 5000L,
        )
        store.recordWidgetReset(record)
        assertEquals(1, store.getPendingWidgetResets().size)

        assertTrue(store.deleteItem(item.id))
        assertTrue(store.getPendingWidgetResets().isEmpty())
    }

    @Test
    fun partialSalvageDoesNotClobberCompleteDiskBackup() {
        val store1 = CountUpStore(testContext)
        val item1 = store1.addItem("Habit 1", 20001L)!!
        val item2 = store1.addItem("Habit 2", 20002L)!!
        val item3 = store1.addItem("Habit 3", 20003L)!!
        assertEquals(3, store1.items().size)

        // Verify backup file exists and has 3 items
        val backupFile = File(tempDir, "countup_backup.json")
        assertTrue(backupFile.exists())

        // Corrupt primary preferences such that salvage can only recover 1 item
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        val partiallyCorrupted = "[{\"id\":\"c1\",\"name\":\"Salvaged Habit\",\"epochDay\":19000},{\"id\":\"broken_json"
        prefs.edit().putString("items_v1", partiallyCorrupted).commit()

        // Create a new store instance; items() must prioritize the 3 intact backup items over the 1 salvaged item
        val store2 = CountUpStore(testContext)
        val recovered = store2.items()
        assertEquals(3, recovered.size)
        assertEquals(setOf(item1.id, item2.id, item3.id), recovered.map { it.id }.toSet())

        // Verify backup file still contains all 3 items and was not clobbered
        val backupContentAfter = backupFile.readText(Charsets.UTF_8)
        assertTrue(backupContentAfter.contains("Habit 1"))
        assertTrue(backupContentAfter.contains("Habit 2"))
        assertTrue(backupContentAfter.contains("Habit 3"))
    }

    @Test
    fun concurrentMultiInstanceMutationsPreserveAllItems() {
        val threadCount = 10
        val itemsPerThread = 5
        val latch = java.util.concurrent.CountDownLatch(threadCount)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)

        for (t in 0 until threadCount) {
            executor.execute {
                try {
                    val threadStore = CountUpStore(testContext)
                    for (i in 0 until itemsPerThread) {
                        threadStore.addItem("Item_${t}_$i", 20000L + t * 10 + i)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        executor.shutdown()

        val finalStore = CountUpStore(testContext)
        val finalItems = finalStore.items()
        assertEquals(threadCount * itemsPerThread, finalItems.size)
    }

    @Test
    fun themeModeDefaultsToSystemAndPersistsAcrossStoreInstances() {
        val store1 = CountUpStore(testContext)
        assertEquals(ThemeMode.SYSTEM, store1.getThemeMode())

        // Persist DARK mode
        val saved = store1.setThemeMode(ThemeMode.DARK)
        assertTrue(saved)
        assertEquals(ThemeMode.DARK, store1.getThemeMode())

        // Read from fresh store instance
        val store2 = CountUpStore(testContext)
        assertEquals(ThemeMode.DARK, store2.getThemeMode())

        // Switch to LIGHT mode
        store2.setThemeMode(ThemeMode.LIGHT)
        val store3 = CountUpStore(testContext)
        assertEquals(ThemeMode.LIGHT, store3.getThemeMode())
    }
}
