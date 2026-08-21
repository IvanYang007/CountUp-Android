package com.ivanyang.countup

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
    fun freshInstallWidgetWasNeverRefreshed() {
        assertFalse(CountUpStore(context).widgetRefreshedOn(20667L))
    }

    @Test
    fun markWidgetRefreshedPersistsAndMatchesOnlyThatDay() {
        val store = CountUpStore(context)
        store.markWidgetRefreshed(20667L)
        // A fresh instance reads the same persisted marker.
        assertTrue(CountUpStore(context).widgetRefreshedOn(20667L))
        assertFalse(CountUpStore(context).widgetRefreshedOn(20668L))
        assertFalse(CountUpStore(context).widgetRefreshedOn(-1L))
    }
}
