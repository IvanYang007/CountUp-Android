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

    /** Serializes read-modify-write mutations so concurrent writers cannot lose updates. */
    private val lock = Any()

    /** Current items, never null; recovers to migrated-or-empty on missing/corrupt data. */
    fun items(): List<CountUpItem> {
        val raw = prefs.getString(KEY_ITEMS, null)
        if (raw != null) {
            decodeItems(raw)?.let { return it }
            // malformed: fall through to recovery (which quarantines the payload)
            return recover(raw)
        }
        return recover(null)
    }

    /**
     * Adds an [item] and confirms the write. The future flag is set when the
     * chosen anchor day is after today.
     * @return the persisted item, or null if the write failed.
     */
    fun addItem(name: String, epochDay: Long): CountUpItem? {
        val trimmed = name.trim()
        val item = CountUpItem(
            id = newId(),
            name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            icon = SOCIAL_ICON_NAMES.random(),
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
        )
        return synchronized(lock) {
            val updated = items() + item
            if (persist(updated)) item else null
        }
    }

    /**
     * Updates the name and/or anchor day of the item with [id], keeping its id.
     * The future flag is recomputed from the new anchor day, so saving a past
     * date clears it and saving a future date sets it.
     * @return false if [id] was not found or the write failed.
     */
    fun updateItem(id: String, name: String, epochDay: Long): Boolean {
        val trimmed = name.trim()
        return synchronized(lock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            list[index] = list[index].copy(
                name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
                epochDay = epochDay,
                futureFlag = epochDay > LocalDate.now().toEpochDay(),
            )
            persist(list)
        }
    }

    /** Removes the item with [id]. @return false if not found or the write failed. */
    fun deleteItem(id: String): Boolean {
        return synchronized(lock) {
            val list = items()
            if (list.none { it.id == id }) return false
            persist(list.filterNot { it.id == id })
        }
    }

    /**
     * Resets the item with [id] to a new anchor [epochDay] (e.g. today), keeping
     * its name and id. Resetting is not a future-date edit, so the future flag
     * clears. @return false if not found or the write failed.
     */
    fun resetTo(id: String, epochDay: Long): Boolean {
        return synchronized(lock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            if (current.epochDay == epochDay && !current.futureFlag) return true // no change needed
            list[index] = current.copy(epochDay = epochDay, futureFlag = false)
            persist(list)
        }
    }

    /**
     * Toggles whether the item with [id] appears in the home-screen widget,
     * keeping every other field. No-op when the flag already matches.
     * @return false if [id] was not found or the write failed.
     */
    fun setWidgetVisibility(id: String, visible: Boolean): Boolean {
        return synchronized(lock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            if (current.showInWidget == visible) return true // no change needed
            list[index] = current.copy(showInWidget = visible)
            persist(list)
        }
    }

    private fun recover(undecodableRaw: String?): List<CountUpItem> {
        if (!undecodableRaw.isNullOrBlank()) {
            // Quarantine the undecodable payload before any overwrite so it stays
            // recoverable; a silent empty-list overwrite would destroy it.
            prefs.edit().putString(KEY_ITEMS_QUARANTINE, undecodableRaw).commit()
        }
        return synchronized(lock) { tryMigrateFromLegacy() }
            ?: run {
                // Nothing to migrate (or already migrated): persist an explicit empty
                // list so this string is never misread as a pending migration.
                prefs.edit().putString(KEY_ITEMS, encodeItems(emptyList())).putBoolean(KEY_MIGRATED, true).commit()
                emptyList()
            }
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
            // Preserve the unusable legacy value for manual recovery instead of
            // discarding it silently, then mark the migration done.
            prefs.edit().putLong(KEY_LEGACY_DAY_QUARANTINE, rawDay).commit()
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
        private const val KEY_ITEMS_QUARANTINE = "items_v1_quarantine"
        private const val KEY_LEGACY_DAY_QUARANTINE = "legacy_day_quarantine"
        private const val KEY_MIGRATED = "migrated_v1"
        private const val KEY_LAST_WIDGET_REFRESH_DAY = "widget_last_refresh_day"
        private const val NO_REFRESH_DAY = -1L
        private const val LEGACY_PREFS_NAME = "haircut_prefs"
        private const val LEGACY_KEY_EPOCH_DAY = "last_haircut_epoch_day"
    }
}
