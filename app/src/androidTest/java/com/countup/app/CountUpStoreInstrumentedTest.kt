package com.countup.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device tests for the multi-item [CountUpStore] and migration from the
 * legacy single-value format. Reaches directly into the same private pref files
 * the store uses to set up states.
 */
@RunWith(AndroidJUnit4::class)
class CountUpStoreInstrumentedTest {

    private lateinit var context: Context

    private fun countupPrefs() = context.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
    private fun legacyPrefs() = context.getSharedPreferences("haircut_prefs", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countupPrefs().edit().clear().commit()
        legacyPrefs().edit().clear().commit()
        java.io.File(context.filesDir, "countup_backup.json").delete()
        java.io.File(context.filesDir, "countup_backup.json.tmp").delete()
    }

    @Test
    fun freshInstallStartsEmpty() {
        assertTrue(CountUpStore(context).items().isEmpty())
    }

    @Test
    fun addPersistsItemAndReturnsIt() {
        val store = CountUpStore(context)
        val created = store.addItem("Haircut", 20667)
        assertNotNull(created)
        assertEquals("Haircut", created!!.name)
        assertEquals(20667, created.epochDay)
        assertEquals(listOf(created), store.items())
    }

    @Test
    fun addAssignsARandomSocialIcon() {
        val created = CountUpStore(context).addItem("New", 20684)!!
        assertTrue(created.icon.isNotEmpty())
        assertTrue(created.icon in SOCIAL_ICON_NAMES)
        assertTrue(iconRes(created.icon) != iconRes("person") || created.icon == "person")
    }

    @Test
    fun legacyItemWithoutIconDefaultsToEmptyIcon() {
        // Simulate a pre-icon stored item (JSON with no "icon" key).
        countupPrefs().edit().putString("items_v1", "[{\"id\":\"old\",\"name\":\"Old\",\"epochDay\":20000}]").commit()
        val item = CountUpStore(context).items().single()
        assertEquals("", item.icon)
    }

    @Test
    fun blankNameFallsBackToDefault() {
        val created = CountUpStore(context).addItem("   ", 20000)
        assertEquals(DEFAULT_ITEM_NAME, created!!.name)
    }

    @Test
    fun addMultipleItemsKeepsAllInOrder() {
        val store = CountUpStore(context)
        val a = store.addItem("A", 100)!!
        val b = store.addItem("B", 200)!!
        val c = store.addItem("C", 300)!!
        assertEquals(listOf(a, b, c), store.items())
    }

    @Test
    fun updateChangesFieldsButKeepsId() {
        val store = CountUpStore(context)
        val item = store.addItem("Old", 100)!!
        assertTrue(store.updateItem(item.id, "New", 999))
        val updated = store.items().single()
        assertEquals(item.id, updated.id)
        assertEquals("New", updated.name)
        assertEquals(999, updated.epochDay)
    }

    @Test
    fun updateMissingIdReturnsFalse() {
        val store = CountUpStore(context)
        store.addItem("X", 1)
        assertFalse(store.updateItem("no-such-id", "Y", 2))
    }

    @Test
    fun deleteRemovesItem() {
        val store = CountUpStore(context)
        val a = store.addItem("A", 100)!!
        val b = store.addItem("B", 200)!!
        assertTrue(store.deleteItem(a.id))
        assertEquals(listOf(b), store.items())
    }

    @Test
    fun deleteMissingIdReturnsFalse() {
        val store = CountUpStore(context)
        store.addItem("A", 100)
        assertFalse(store.deleteItem("nope"))
    }

    @Test
    fun resetToChangesDateButKeepsNameAndId() {
        val store = CountUpStore(context)
        val item = store.addItem("Grow", 100)!!
        assertTrue(store.resetTo(item.id, 999))
        val reset = store.items().single()
        assertEquals(item.id, reset.id)
        assertEquals("Grow", reset.name)
        assertEquals(999, reset.epochDay)
    }

    @Test
    fun resetToMissingIdReturnsFalse() {
        val store = CountUpStore(context)
        store.addItem("A", 100)
        assertFalse(store.resetTo("no-such-id", 5))
    }

    @Test
    fun dataSurvivesNewStoreInstance() {
        val store = CountUpStore(context)
        val item = store.addItem("Persist", 777)!!
        // A fresh instance reads the same persisted file.
        assertEquals(listOf(item), CountUpStore(context).items())
    }

    @Test
    fun corruptJsonRecoversWithoutCrashing() {
        countupPrefs().edit().putString("items_v1", "garbage{{").commit()
        assertTrue(CountUpStore(context).items().isEmpty())
    }

    @Test
    fun legacySingleValueMigratesToOneHaircutItem() {
        legacyPrefs().edit().putLong("last_haircut_epoch_day", 20667L).commit()
        val items = CountUpStore(context).items()
        assertEquals(1, items.size)
        assertEquals("Haircut", items[0].name)
        assertEquals(20667, items[0].epochDay)
        // already-migrated state is now marked; a second read is stable.
        assertEquals(items, CountUpStore(context).items())
    }

    @Test
    fun legacyValueDoesNotReimportAfterUserDeletesAll() {
        legacyPrefs().edit().putLong("last_haircut_epoch_day", 20667L).commit()
        val store = CountUpStore(context)
        assertEquals(1, store.items().size)
        assertTrue(store.deleteItem(store.items()[0].id))
        // deleted and migrated flag set -> does not resurrect the legacy value.
        assertTrue(store.items().isEmpty())
        assertTrue(CountUpStore(context).items().isEmpty())
    }

    @Test
    fun sortOrderPersistsAcrossInstances() {
        val store = CountUpStore(context)
        assertEquals(SortOrder.DAYS_DESC, store.getSortOrder())

        assertTrue(store.setSortOrder(SortOrder.NAME_ASC))
        assertEquals(SortOrder.NAME_ASC, CountUpStore(context).getSortOrder())
    }

    @Test
    fun backgroundThemePersistsAcrossInstances() {
        val store = CountUpStore(context)
        assertEquals(BackgroundTheme.AUTO_DAILY, store.getBackgroundTheme())

        assertTrue(store.setBackgroundTheme(BackgroundTheme.SAND_DUNES))
        assertEquals(BackgroundTheme.SAND_DUNES, CountUpStore(context).getBackgroundTheme())
    }

    // Finding #8: Tests for deleting items and empty state operations
    @Test
    fun deletingTheOnlyItemLeavesAnEmptyList() {
        val store = CountUpStore(context)
        val item = store.addItem("Only Item", 20000)!!
        assertEquals(1, store.items().size)
        
        assertTrue(store.deleteItem(item.id))
        assertTrue(store.items().isEmpty())
    }

    @Test
    fun storeReadAddOperationsWorkOnEmptyState() {
        val store = CountUpStore(context)
        
        // Fresh empty state
        assertTrue(store.items().isEmpty())
        
        // Add operation works on empty state
        val item = store.addItem("First Item", 20000)
        assertNotNull(item)
        assertEquals(1, store.items().size)
        assertEquals("First Item", store.items()[0].name)
    }

    // Finding #9: Migration edge cases
    @Test
    fun legacyPrefsWithInvalidEpochDayFailsLocalDateOfEpochDay() {
        // Test with Long.MAX_VALUE / 10000000000L which will fail LocalDate.ofEpochDay
        val invalidEpochDay = Long.MAX_VALUE
        legacyPrefs().edit().putLong("last_haircut_epoch_day", invalidEpochDay).commit()
        
        val store = CountUpStore(context)
        val items = store.items()
        
        // Migration should complete with empty list, migrated flag should be set
        assertTrue(items.isEmpty())
        // Verify migrated flag is set by checking a second read doesn't try migration again
        assertEquals(items, CountUpStore(context).items())
    }

    @Test
    fun bothLegacyKeyAndItemsV1PresentExistingItemsWin() {
        // Set up both legacy and current data
        legacyPrefs().edit().putLong("last_haircut_epoch_day", 20000L).commit()
        countupPrefs().edit()
            .putString("items_v1", "[{\"id\":\"existing\",\"name\":\"Existing Item\",\"epochDay\":20500}]")
            .putBoolean("migrated_v1", true)
            .commit()
        
        val store = CountUpStore(context)
        val items = store.items()
        
        // Should have only the existing item, not duplicate migration
        assertEquals(1, items.size)
        assertEquals("Existing Item", items[0].name)
        assertEquals(20500, items[0].epochDay)
        assertEquals("existing", items[0].id)
    }

    @Test
    fun noLegacyPrefsAtAllResultsInEmptyList() {
        // Explicitly verify no legacy key exists
        assertFalse(legacyPrefs().contains("last_haircut_epoch_day"))
        
        val store = CountUpStore(context)
        val items = store.items()
        
        // Should result in empty list
        assertTrue(items.isEmpty())
        
        // Should still be empty on subsequent reads
        assertTrue(CountUpStore(context).items().isEmpty())
    }

    // Review follow-up: non-destructive recovery and migration quarantine
    @Test
    fun undecodableItemsStringIsQuarantinedNotOverwritten() {
        countupPrefs().edit().putString("items_v1", "[{corrupt").commit()
        val store = CountUpStore(context)
        assertTrue(store.items().isEmpty())
        // The undecodable payload survives under the quarantine key.
        assertEquals("[{corrupt", countupPrefs().getString("items_v1_quarantine", null))
    }

    @Test
    fun partiallyCorruptArrayQuarantinesAndRecoversParseableItems() {
        // One malformed element: the valid element is salvaged, and raw payload is safely quarantined
        countupPrefs().edit()
            .putString(
                "items_v1",
                "[{\"id\":\"good\",\"name\":\"Good\",\"epochDay\":5},{\"id\":123}]",
            )
            .commit()
        val items = CountUpStore(context).items()
        assertEquals(1, items.size)
        assertEquals("Good", items[0].name)
        assertTrue(countupPrefs().contains("items_v1_quarantine"))
    }

    @Test
    fun invalidLegacyDayIsQuarantinedNotDiscarded() {
        val invalidEpochDay = Long.MAX_VALUE
        legacyPrefs().edit().putLong("last_haircut_epoch_day", invalidEpochDay).commit()
        val store = CountUpStore(context)
        assertTrue(store.items().isEmpty())
        // The unusable legacy value is preserved for manual recovery.
        assertEquals(invalidEpochDay, countupPrefs().getLong("legacy_day_quarantine", Long.MIN_VALUE))
    }

    @Test
    fun updateBlankNameFallsBackToDefault() {
        val store = CountUpStore(context)
        val item = store.addItem("Old", 100)!!
        assertTrue(store.updateItem(item.id, "   ", 200))
        assertEquals(DEFAULT_ITEM_NAME, store.items().single().name)
        assertEquals(200L, store.items().single().epochDay)
    }
}
