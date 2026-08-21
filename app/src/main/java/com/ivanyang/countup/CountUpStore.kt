package com.ivanyang.countup

import android.content.Context
import java.time.LocalDate
import java.util.UUID

/**
 * Persistence for multiple count-up items. A small list of
 * [CountUpItem]s is stored as one JSON array string in a private
 * [android.content.SharedPreferences] file.
 *
 * Responsibility boundaries stay minimal and flat:
 *  - [items] is a defensive read: missing/corrupt data recovers to a migrated or
 *    empty list and never crashes.
 *  - [addItem]/[updateItem]/[deleteItem] are synchronous writes via `commit()`,
 *    so the caller can confirm persistence before refreshing UI or the widget.
 *  - [CountUpItem] owns serialization ([encodeItems]/[decodeItems]).
 *
 * A one-time migration imports the legacy single value (`last_haircut_epoch_day`
 * from the old `haircut_prefs` file) as the first item, so an existing install
 * upgrades without data loss.
 */
class CountUpStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    /** Current items, never null; recovers to migrated-or-empty on missing/corrupt data. */
    fun items(): List<CountUpItem> {
        prefs.getString(KEY_ITEMS, null)?.let { raw ->
            decodeItems(raw)?.let { return it }
            // malformed: fall through to recovery
        }
        return recover()
    }

    /**
     * Adds an [item] and confirms the write.
     * @return true only if the item was persisted.
     */
    fun addItem(name: String, epochDay: Long): CountUpItem? {
        val trimmed = name.trim()
        val item = CountUpItem(
            id = newId(),
            name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            icon = SOCIAL_ICON_NAMES.random(),
        )
        val updated = items() + item
        return if (persist(updated)) item else null
    }

    /**
     * Updates the name and/or anchor day of the item with [id], keeping its id.
     * @return false if [id] was not found or the write failed.
     */
    fun updateItem(id: String, name: String, epochDay: Long): Boolean {
        val trimmed = name.trim()
        val list = items().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return false
        list[index] = list[index].copy(name = trimmed.ifEmpty { list[index].name }, epochDay = epochDay)
        return persist(list)
    }

    /** Removes the item with [id]. @return false if not found or the write failed. */
    fun deleteItem(id: String): Boolean {
        val list = items()
        if (list.none { it.id == id }) return false
        return persist(list.filterNot { it.id == id })
    }

    /**
     * Resets the item with [id] to a new anchor [epochDay] (e.g. today), keeping
     * its name and id. @return false if not found or the write failed.
     */
    fun resetTo(id: String, epochDay: Long): Boolean {
        val list = items().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return false
        if (list[index].epochDay == epochDay) return true // no change needed
        list[index] = list[index].copy(epochDay = epochDay)
        return persist(list)
    }

    private fun recover(): List<CountUpItem> {
        tryMigrateFromLegacy()?.let { return it }
        // Nothing to migrate (or already migrated): persist an explicit empty list
        // so this string is never misread as a pending migration.
        prefs.edit().putString(KEY_ITEMS, encodeItems(emptyList())).putBoolean(KEY_MIGRATED, true).commit()
        return emptyList()
    }

    /**
     * One-time import of the legacy single value. Returns a non-null list only
     * when a real legacy value existed and has now been migrated.
     */
    private fun tryMigrateFromLegacy(): List<CountUpItem>? {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return null
        if (!legacyPrefs.contains(LEGACY_KEY_EPOCH_DAY)) {
            prefs.edit().putBoolean(KEY_MIGRATED, true).commit()
            return null
        }
        val rawDay = legacyPrefs.getLong(LEGACY_KEY_EPOCH_DAY, -1L)
        val date = try {
            LocalDate.ofEpochDay(rawDay)
        } catch (_: Exception) {
            null
        }
        if (date == null) {
            prefs.edit().putBoolean(KEY_MIGRATED, true).commit()
            return null
        }
        val migrated = listOf(CountUpItem(id = newId(), name = "Haircut", epochDay = rawDay))
        persist(migrated)
        return migrated
    }

    /**
     * Records the epoch day on which the home-screen widget was last refreshed,
     * so [widgetRefreshedOn] can gate redundant refreshes. Android throttles
     * frequent widget updates, so a plain foreground with no data change must
     * not trigger another rebuild.
     */
    fun markWidgetRefreshed(epochDay: Long) {
        prefs.edit().putLong(KEY_LAST_WIDGET_REFRESH_DAY, epochDay).commit()
    }

    /**
     * True when the widget was already refreshed on [epochDay]. Defaults to
     * false on a fresh install, so the first foreground of a day still refreshes.
     */
    fun widgetRefreshedOn(epochDay: Long): Boolean =
        prefs.getLong(KEY_LAST_WIDGET_REFRESH_DAY, NO_REFRESH_DAY) == epochDay

    private fun persist(items: List<CountUpItem>): Boolean {
        return prefs.edit()
            .putString(KEY_ITEMS, encodeItems(items))
            .putBoolean(KEY_MIGRATED, true)
            .commit()
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val PREFS_NAME = "countup_prefs"
        private const val KEY_ITEMS = "items_v1"
        private const val KEY_MIGRATED = "migrated_v1"
        private const val KEY_LAST_WIDGET_REFRESH_DAY = "widget_last_refresh_day"
        private const val NO_REFRESH_DAY = -1L
        private const val LEGACY_PREFS_NAME = "haircut_prefs"
        private const val LEGACY_KEY_EPOCH_DAY = "last_haircut_epoch_day"
    }
}
