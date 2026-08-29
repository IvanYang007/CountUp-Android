package com.countup.app

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class CountUpStoreTest {

    private lateinit var tempDir: File
    private lateinit var fakeContext: FakeContext

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("countup_test").toFile()
        fakeContext = FakeContext(tempDir)
    }

    @Test
    fun cleanInstallInitializesEmptyListAndWritesBackup() {
        val store = CountUpStore(fakeContext)
        val items = store.items()
        assertTrue(items.isEmpty())

        val backupFile = File(tempDir, "countup_backup.json")
        assertTrue(backupFile.exists())
    }

    @Test
    fun addItemPersistsToBothPreferencesAndDiskBackup() {
        val store = CountUpStore(fakeContext)
        val item = store.addItem(name = "Meditation", epochDay = 20000, comment = "Calm streak")
        assertNotNull(item)
        assertEquals("Meditation", item!!.name)
        assertEquals(20000L, item.epochDay)

        // Verify read back from new store instance
        val store2 = CountUpStore(fakeContext)
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
        val legacyPrefs = fakeContext.getSharedPreferences("haircut_prefs", Context.MODE_PRIVATE)
        legacyPrefs.edit().putLong("last_haircut_epoch_day", 19500L).commit()

        val store = CountUpStore(fakeContext)
        val items = store.items()
        assertEquals(1, items.size)
        assertEquals("Haircut", items[0].name)
        assertEquals(19500L, items[0].epochDay)

        // Migration is marked done
        val prefs = fakeContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("migrated_v1", false))
    }

    @Test
    fun secondaryBackupAutoHealsWhenSharedPreferencesIsBlanked() {
        val store1 = CountUpStore(fakeContext)
        val item = store1.addItem("Workout", 20100L)!!
        assertEquals(1, store1.items().size)

        // Simulate OS clearing SharedPreferences (e.g. storage glitch)
        val prefs = fakeContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // Create new store instance; it should restore from countup_backup.json
        val store2 = CountUpStore(fakeContext)
        val recovered = store2.items()
        assertEquals(1, recovered.size)
        assertEquals(item.id, recovered[0].id)
        assertEquals("Workout", recovered[0].name)
    }

    @Test
    fun corruptedPayloadIsSalvagedAndQuarantinedWithoutDataLoss() {
        val prefs = fakeContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        // Corrupted/truncated payload with valid items inside
        val corrupted =
            "[{\"id\":\"c1\",\"name\":\"Piano\",\"epochDay\":19000},{\"id\":\"c2\",\"name\":\"Language\",\"epochDay\":19100},{\"id\":\"c3\""
        prefs.edit().putString("items_v1", corrupted).commit()

        val store = CountUpStore(fakeContext)
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
        val store = CountUpStore(fakeContext)
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
        val store = CountUpStore(fakeContext)
        val item = store.addItem("Widget Habit", 100L)!!
        assertTrue(item.showInWidget)

        assertTrue(store.setWidgetVisibility(item.id, false))
        assertFalse(store.items()[0].showInWidget)

        assertTrue(store.setWidgetVisibility(item.id, true))
        assertTrue(store.items()[0].showInWidget)
    }

    @Test
    fun quarantinePruningKeepsAtMostThreeHistoricalSnapshots() {
        val prefs = fakeContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)

        // Simulate 5 consecutive corrupt read incidents over time
        for (i in 1..5) {
            prefs.edit().putString("items_v1", "broken_$i").commit()
            val store = CountUpStore(fakeContext)
            store.items()
            Thread.sleep(2) // ensure distinct timestamps
        }

        val quarantineKeys = prefs.all.keys.filter { it.startsWith("items_v1_quarantine_") }
        assertTrue("Expected <= 3 timestamped quarantine keys, found ${quarantineKeys.size}", quarantineKeys.size <= 3)
    }

    // --- In-Memory Test Harness for Android Context & SharedPreferences ---

    private class FakeContext(private val baseFilesDir: File) : android.content.ContextWrapper(null) {
        private val prefsMap = ConcurrentHashMap<String, FakeSharedPreferences>()

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return prefsMap.computeIfAbsent(name) { FakeSharedPreferences() }
        }

        override fun getFilesDir(): File = baseFilesDir
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = ConcurrentHashMap<String, Any>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)

        override fun getString(key: String?, defValue: String?): String? =
            data[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Number)?.toInt() ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Number)?.toLong() ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Number)?.toFloat() ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            data[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val backingMap: ConcurrentHashMap<String, Any>) : SharedPreferences.Editor {
            private val pending = HashMap<String, Any?>()
            private var clearPending = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = value
                }
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = values
                }
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = value
                }
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = value
                }
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = value
                }
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = value
                }
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) {
                    pending[key] = null
                }
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearPending = true
            }

            override fun commit(): Boolean {
                if (clearPending) {
                    backingMap.clear()
                    clearPending = false
                }
                for ((k, v) in pending) {
                    if (v == null) {
                        backingMap.remove(k)
                    } else {
                        backingMap[k] = v
                    }
                }
                pending.clear()
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
