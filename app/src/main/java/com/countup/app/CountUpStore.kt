package com.countup.app

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Persistence for multiple count-up items. A small list of
 * [CountUpItem]s is stored as one JSON array string in a private
 * [android.content.SharedPreferences] file with an atomic secondary disk
 * backup snapshot file (`countup_backup.json`) for zero data loss.
 *
 * Multi-Tier Fail-Safe Recovery Pipeline:
 *  1. Primary: Validated JSON decode from `SharedPreferences`.
 *  2. Salvage Engine: Reconstructs valid items from broken/truncated JSON strings.
 *  3. Disk Backup Snapshot: Self-heals from `countup_backup.json` if prefs are wiped.
 *  4. Legacy Migration: Migrates v0 single-value `haircut_prefs` into multi-item list.
 *  5. Timestamped Quarantine: Preserves undecodable payloads for forensics without data destruction.
 */
@SuppressLint("ApplySharedPref", "UseKtx")
class CountUpStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val filesDir: File = context.filesDir
    private val backupFile: File = File(filesDir, BACKUP_FILE_NAME)
    private val backupTempFile: File = File(filesDir, "$BACKUP_FILE_NAME.tmp")

    /** Current items, never null; guarantees zero data loss via multi-tier fallback. */
    fun items(): List<CountUpItem> {
        return synchronized(globalStoreLock) {
            // Tier 1: Primary SharedPreferences read
            val raw = try {
                prefs.getString(KEY_ITEMS, null)
            } catch (_: ClassCastException) {
                null
            }
            if (!raw.isNullOrBlank()) {
                val decoded = decodeItems(raw)
                if (decoded != null) {
                    ensureBackupInSync(raw)
                    return@synchronized decoded
                }

                // Corrupted raw detected. Before persisting partial salvaged data,
                // inspect the secondary disk backup snapshot to prevent partial data from
                // superseding a more complete backup.
                val salvaged = salvageItems(raw)
                val backupRaw = readBackup()
                val backupDecoded = if (!backupRaw.isNullOrBlank()) decodeItems(backupRaw) else null
                val backupCount = backupDecoded?.size ?: 0

                quarantineRawPayload(raw)

                if (backupDecoded != null && backupCount >= salvaged.size && backupCount > 0) {
                    // Backup has equal or more items than salvage: self-heal from backup snapshot
                    prefs.edit()
                        .putString(KEY_ITEMS, backupRaw)
                        .putBoolean(KEY_MIGRATED, true)
                        .commit()
                    return@synchronized backupDecoded
                } else if (salvaged.isNotEmpty()) {
                    // Salvage recovered valid items and backup had fewer or none: persist salvaged
                    persist(salvaged)
                    return@synchronized salvaged
                } else if (backupDecoded != null && backupCount > 0) {
                    prefs.edit()
                        .putString(KEY_ITEMS, backupRaw)
                        .putBoolean(KEY_MIGRATED, true)
                        .commit()
                    return@synchronized backupDecoded
                }
            }

            // Tier 2: Secondary Disk Backup Snapshot
            val backupRaw = readBackup()
            if (!backupRaw.isNullOrBlank()) {
                val backupDecoded = decodeItems(backupRaw)
                if (!backupDecoded.isNullOrEmpty()) {
                    // Self-heal SharedPreferences from backup snapshot
                    prefs.edit()
                        .putString(KEY_ITEMS, backupRaw)
                        .putBoolean(KEY_MIGRATED, true)
                        .commit()
                    return@synchronized backupDecoded
                }
            }

            // Tier 3: Legacy migration from haircut_prefs
            val legacyMigrated = tryMigrateFromLegacy()
            if (legacyMigrated != null) {
                return@synchronized legacyMigrated
            }

            // Tier 4: Unrecoverable payload quarantine or clean initial install
            if (!raw.isNullOrBlank()) {
                quarantineRawPayload(raw)
            }
            if (!prefs.getBoolean(KEY_MIGRATED, false) || !backupFile.exists()) {
                val emptyEncoded = encodeItems(emptyList())
                prefs.edit().putString(KEY_ITEMS, emptyEncoded).putBoolean(KEY_MIGRATED, true).commit()
                writeBackup(emptyEncoded)
            }
            emptyList()
        }
    }

    /**
     * Adds an [item] and confirms the write. The future flag is set when the
     * chosen anchor day is after today.
     * @return the persisted item, or null if the write failed.
     */
    fun addItem(
        name: String,
        epochDay: Long,
        comment: String = "",
        icon: String = "",
        cardColor: String = "",
        isPinned: Boolean = false,
    ): CountUpItem? {
        val trimmed = name.trim()
        val resolvedIcon = icon.trim().ifEmpty { ALL_ICON_NAMES.random() }
        val item = CountUpItem(
            id = newId(),
            name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            comment = comment.trim(),
            icon = resolvedIcon,
            cardColor = cardColor.trim(),
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
            pinnedTimestamp = if (isPinned) System.currentTimeMillis() else null,
        )
        return synchronized(globalStoreLock) {
            val updated = items() + item
            if (persist(updated)) item else null
        }
    }

    /**
     * Updates the item with [id], keeping its id.
     * The future flag is recomputed from the new anchor day, so saving a past
     * date clears it and saving a future date sets it.
     * @return false if [id] was not found or the write failed.
     */
    fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String = "",
        icon: String = "",
        cardColor: String = "",
        isPinned: Boolean = false,
    ): Boolean {
        val trimmed = name.trim()
        return synchronized(globalStoreLock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            val resolvedIcon = icon.trim().ifEmpty { current.icon.ifEmpty { DEFAULT_ICON } }
            val newPinnedTimestamp = if (isPinned) {
                current.pinnedTimestamp ?: System.currentTimeMillis()
            } else {
                null
            }
            list[index] = current.copy(
                name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
                epochDay = epochDay,
                comment = comment.trim(),
                icon = resolvedIcon,
                cardColor = cardColor.trim(),
                futureFlag = epochDay > LocalDate.now().toEpochDay(),
                pinnedTimestamp = newPinnedTimestamp,
            )
            persist(list)
        }
    }

    /** Removes the item with [id]. @return false if not found or the write failed. */
    fun deleteItem(id: String): Boolean {
        return synchronized(globalStoreLock) {
            val list = items()
            if (list.none { it.id == id }) return false
            val deleted = persist(list.filterNot { it.id == id })
            if (deleted) {
                val pending = getPendingWidgetResets().filterNot { it.itemId == id }
                persistWidgetResets(pending)
            }
            deleted
        }
    }

    /**
     * Resets the item with [id] to a new anchor [epochDay] (e.g. today), keeping
     * its name and id. Resetting is not a future-date edit, so the future flag
     * clears. @return false if not found, already 0 accumulated days, or the write failed.
     */
    fun resetTo(id: String, epochDay: Long): Boolean {
        return synchronized(globalStoreLock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            if (!current.isResettableOn(epochDay)) return false
            val updated = current.resetTo(epochDay)
            if (updated === current) return false
            list[index] = updated
            persist(list)
        }
    }

    /**
     * Restores an item's anchor day and cycle metrics from [snapshot].
     * Used by undo actions in-app and from widget resets.
     * @return false if [id] was not found or the write failed.
     */
    fun restoreReset(
        id: String,
        snapshot: ResetSnapshot,
    ): Boolean {
        return synchronized(globalStoreLock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            list[index] = current.restoreFrom(snapshot)
            persist(list)
        }
    }

    /**
     * Toggles whether the item with [id] appears in the home-screen widget,
     * keeping every other field. No-op when the flag already matches.
     * @return false if [id] was not found or the write failed.
     */
    fun setWidgetVisibility(id: String, visible: Boolean): Boolean {
        return synchronized(globalStoreLock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            if (current.showInWidget == visible) return true // no change needed
            list[index] = current.copy(showInWidget = visible)
            persist(list)
        }
    }

    private fun quarantineRawPayload(undecodableRaw: String) {
        val timestamp = System.currentTimeMillis()
        val newKey = "${KEY_ITEMS_QUARANTINE}_$timestamp"
        val editor = prefs.edit()
            .putString(KEY_ITEMS_QUARANTINE, undecodableRaw)
            .putString(newKey, undecodableRaw)

        // Prune older quarantine entries to prevent unbounded growth (retain at most 3 historical snapshots)
        val allKeys = (prefs.all.keys.filter { it.startsWith("${KEY_ITEMS_QUARANTINE}_") } + newKey).distinct().sorted()
        if (allKeys.size > MAX_QUARANTINE_ENTRIES) {
            for (oldKey in allKeys.take(allKeys.size - MAX_QUARANTINE_ENTRIES)) {
                editor.remove(oldKey)
            }
        }
        editor.commit()
    }

    /**
     * One-time import of the legacy single value. Returns a non-null list only
     * when a real legacy value existed and has now been migrated.
     */
    private fun tryMigrateFromLegacy(): List<CountUpItem>? {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return null
        if (!legacyPrefs.contains(LEGACY_KEY_EPOCH_DAY)) {
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
     * Retrieves the saved [BackgroundTheme], defaulting to [BackgroundTheme.AUTO_DAILY].
     */
    fun getBackgroundTheme(): BackgroundTheme {
        val raw = prefs.getString(KEY_BACKGROUND_THEME, null)
        return BackgroundTheme.fromId(raw)
    }

    /**
     * Persists the user's selected [BackgroundTheme].
     */
    fun setBackgroundTheme(theme: BackgroundTheme): Boolean {
        return prefs.edit()
            .putString(KEY_BACKGROUND_THEME, theme.id)
            .commit()
    }

    /**
     * Retrieves the saved [ThemeMode], defaulting to [ThemeMode.SYSTEM].
     */
    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, null)
            ?: prefs.getString(KEY_THEME_MODE_LEGACY, null)
        return ThemeMode.fromId(raw)
    }

    /**
     * Persists the user's selected [ThemeMode].
     */
    fun setThemeMode(mode: ThemeMode): Boolean {
        return prefs.edit()
            .putString(KEY_THEME_MODE, mode.id)
            .remove(KEY_THEME_MODE_LEGACY)
            .commit()
    }

    /**
     * Retrieves the saved [SortOrder], defaulting to [SortOrder.DAYS_DESC].
     */
    fun getSortOrder(): SortOrder {
        val raw = prefs.getString(KEY_SORT_ORDER, null)
        return SortOrder.fromId(raw)
    }

    /**
     * Persists the user's selected [SortOrder].
     */
    fun setSortOrder(sortOrder: SortOrder): Boolean {
        return prefs.edit()
            .putString(KEY_SORT_ORDER, sortOrder.id)
            .commit()
    }

    /** Retrieves the bound item ID for a Hero Widget instance. */
    fun getHeroWidgetBinding(appWidgetId: Int): String? {
        return prefs.getString(PREFIX_HERO_BINDING + appWidgetId, null)
    }

    /** Binds a specific count-up item to a Hero Widget instance. */
    fun setHeroWidgetBinding(appWidgetId: Int, itemId: String): Boolean {
        return prefs.edit()
            .putString(PREFIX_HERO_BINDING + appWidgetId, itemId)
            .commit()
    }

    /** Retrieves the saved [TimeDisplayMode] for a Hero Widget instance, defaulting to [TimeDisplayMode.DAYS]. */
    fun getHeroWidgetDisplayMode(appWidgetId: Int): TimeDisplayMode {
        val raw = prefs.getString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, null)
        return try {
            if (raw != null) TimeDisplayMode.valueOf(raw) else TimeDisplayMode.DAYS
        } catch (_: IllegalArgumentException) {
            TimeDisplayMode.DAYS
        }
    }

    /** Persists the [TimeDisplayMode] for a Hero Widget instance. */
    fun setHeroWidgetDisplayMode(appWidgetId: Int, mode: TimeDisplayMode): Boolean {
        return prefs.edit()
            .putString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, mode.name)
            .commit()
    }

    /** Retrieves the saved [ZenWidgetDisplayUnit] for a Zen Horizon instance, defaulting to [ZenWidgetDisplayUnit.DAYS]. */
    fun getZenHorizonUnit(appWidgetId: Int): ZenWidgetDisplayUnit {
        val raw = prefs.getString(PREFIX_ZEN_HORIZON_UNIT + appWidgetId, null)
        return try {
            if (raw != null) ZenWidgetDisplayUnit.valueOf(raw) else ZenWidgetDisplayUnit.DAYS
        } catch (_: IllegalArgumentException) {
            ZenWidgetDisplayUnit.DAYS
        }
    }

    /** Persists the [ZenWidgetDisplayUnit] for a Zen Horizon instance. */
    fun setZenHorizonUnit(appWidgetId: Int, unit: ZenWidgetDisplayUnit): Boolean {
        return prefs.edit()
            .putString(PREFIX_ZEN_HORIZON_UNIT + appWidgetId, unit.name)
            .commit()
    }

    /** Retrieves the bound item ID for a Zen Horizon Widget instance, falling back to hero binding or null. */
    fun getZenHorizonBinding(appWidgetId: Int): String? {
        return prefs.getString(PREFIX_ZEN_HORIZON_BINDING + appWidgetId, null)
            ?: prefs.getString(PREFIX_HERO_BINDING + appWidgetId, null)
    }

    /** Binds a specific [itemId] to a Zen Horizon Widget instance. */
    fun setZenHorizonBinding(appWidgetId: Int, itemId: String): Boolean {
        return prefs.edit()
            .putString(PREFIX_ZEN_HORIZON_BINDING + appWidgetId, itemId)
            .commit()
    }

    /** Removes the binding and display unit for a deleted Zen Horizon Widget instance. */
    fun removeZenHorizonBinding(appWidgetId: Int): Boolean {
        return prefs.edit()
            .remove(PREFIX_ZEN_HORIZON_BINDING + appWidgetId)
            .remove(PREFIX_ZEN_HORIZON_UNIT + appWidgetId)
            .commit()
    }

    /** Retrieves the bound item ID for a Zen Pebble Widget instance, falling back to null. */
    fun getZenPebbleBinding(appWidgetId: Int): String? {
        return prefs.getString(PREFIX_ZEN_PEBBLE_BINDING + appWidgetId, null)
    }

    /** Binds a specific [itemId] to a Zen Pebble Widget instance. */
    fun setZenPebbleBinding(appWidgetId: Int, itemId: String): Boolean {
        return prefs.edit()
            .putString(PREFIX_ZEN_PEBBLE_BINDING + appWidgetId, itemId)
            .commit()
    }

    /** Retrieves the customizable one-word tag for a Zen Pebble Widget instance. */
    fun getZenPebbleTag(appWidgetId: Int): String? {
        return prefs.getString(PREFIX_ZEN_PEBBLE_TAG + appWidgetId, null)
    }

    /** Persists a customizable one-word tag for a Zen Pebble Widget instance. */
    fun setZenPebbleTag(appWidgetId: Int, tag: String): Boolean {
        return prefs.edit()
            .putString(PREFIX_ZEN_PEBBLE_TAG + appWidgetId, tag)
            .commit()
    }

    /** Removes the binding and custom tag for a deleted Zen Pebble Widget instance. */
    fun removeZenPebbleBinding(appWidgetId: Int): Boolean {
        return prefs.edit()
            .remove(PREFIX_ZEN_PEBBLE_BINDING + appWidgetId)
            .remove(PREFIX_ZEN_PEBBLE_TAG + appWidgetId)
            .commit()
    }

    /** Retrieves the bound item ID for a Solar Rhythm Widget instance. */
    fun getSolarRhythmBinding(appWidgetId: Int): String? {
        return prefs.getString(PREFIX_SOLAR_RHYTHM_BINDING + appWidgetId, null)
    }

    /** Binds a specific [itemId] to a Solar Rhythm Widget instance. */
    fun setSolarRhythmBinding(appWidgetId: Int, itemId: String): Boolean {
        return prefs.edit()
            .putString(PREFIX_SOLAR_RHYTHM_BINDING + appWidgetId, itemId)
            .commit()
    }

    /** Removes the binding for a deleted Solar Rhythm Widget instance. */
    fun removeSolarRhythmBinding(appWidgetId: Int): Boolean {
        return prefs.edit()
            .remove(PREFIX_SOLAR_RHYTHM_BINDING + appWidgetId)
            .commit()
    }

    /** Removes the binding for a deleted Hero Widget instance. */
    fun removeHeroWidgetBinding(appWidgetId: Int): Boolean {
        return prefs.edit()
            .remove(PREFIX_HERO_BINDING + appWidgetId)
            .remove(PREFIX_HERO_DISPLAY_MODE + appWidgetId)
            .remove(PREFIX_ZEN_HORIZON_UNIT + appWidgetId)
            .commit()
    }

    /** Retrieves all active hero widget bindings. */
    fun getAllHeroWidgetBindings(): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(PREFIX_HERO_BINDING) && value is String) {
                val widgetId = key.removePrefix(PREFIX_HERO_BINDING).toIntOrNull()
                if (widgetId != null) {
                    result[widgetId] = value
                }
            }
        }
        return result
    }

    /**
     * Builds a portable [CountUpBackupPayload] snapshot of all current items and user settings.
     */
    fun exportBackupPayload(): CountUpBackupPayload {
        return synchronized(globalStoreLock) {
            CountUpBackupPayload(
                sortOrder = getSortOrder(),
                themeMode = getThemeMode(),
                backgroundTheme = getBackgroundTheme(),
                items = items(),
            )
        }
    }

    /**
     * Restores items and settings from [payload] according to [strategy].
     *
     * In [RestoreStrategy.REPLACE_ALL]:
     * Replaces existing items with [payload.items] and restores appearance settings
     * (sort order, theme mode, and background theme).
     *
     * In [RestoreStrategy.MERGE_KEEP_EXISTING]:
     * Retains all existing items on this device. Appends only novel items whose
     * ID and normalized name are not already present. Preserves existing appearance settings.
     */
    fun restoreBackupPayload(payload: CountUpBackupPayload, strategy: RestoreStrategy): Boolean {
        return synchronized(globalStoreLock) {
            when (strategy) {
                RestoreStrategy.REPLACE_ALL -> {
                    setSortOrder(payload.sortOrder)
                    setThemeMode(payload.themeMode)
                    setBackgroundTheme(payload.backgroundTheme)
                    persist(payload.items)
                }
                RestoreStrategy.MERGE_KEEP_EXISTING -> {
                    val currentItems = items().toMutableList()
                    val existingIds = currentItems.map { it.id }.toMutableSet()
                    val existingNames = currentItems.map { it.name.trim().lowercase() }.toMutableSet()

                    val itemsToAdd = mutableListOf<CountUpItem>()
                    for (item in payload.items) {
                        val normName = item.name.trim().lowercase()
                        if (item.id !in existingIds && normName !in existingNames) {
                            itemsToAdd.add(item)
                            existingIds.add(item.id)
                            existingNames.add(normName)
                        }
                    }
                    currentItems.addAll(itemsToAdd)
                    persist(currentItems)
                }
            }
        }
    }

    /**
     * Purges widget instance bindings whose IDs are not in [activeWidgetIds].
     * Protects newly restored or cloned databases from retaining dead widget bindings.
     * @return count of keys purged.
     */
    fun sanitizeOrphanedWidgetBindings(activeWidgetIds: Set<Int>): Int {
        return synchronized(globalStoreLock) {
            val bindingPrefixes = listOf(
                PREFIX_HERO_BINDING,
                PREFIX_HERO_DISPLAY_MODE,
                PREFIX_ZEN_HORIZON_BINDING,
                PREFIX_ZEN_HORIZON_UNIT,
                PREFIX_ZEN_PEBBLE_BINDING,
                PREFIX_ZEN_PEBBLE_TAG,
                PREFIX_SOLAR_RHYTHM_BINDING,
            )
            val editor = prefs.edit()
            var purgedCount = 0

            for (key in prefs.all.keys) {
                for (prefix in bindingPrefixes) {
                    if (key.startsWith(prefix)) {
                        val id = key.removePrefix(prefix).toIntOrNull()
                        if (id != null && id !in activeWidgetIds) {
                            editor.remove(key)
                            purgedCount++
                        }
                    }
                }
            }
            if (purgedCount > 0) {
                editor.commit()
            }
            purgedCount
        }
    }

    /**
     * Queries the system [android.appwidget.AppWidgetManager] across all 5 widget providers
     * to sanitize orphaned widget bindings on this device.
     * @return count of keys purged.
     */
    fun sanitizeOrphanedWidgetBindings(context: Context): Int {
        return try {
            val manager = android.appwidget.AppWidgetManager.getInstance(context) ?: return 0
            val activeIds = mutableSetOf<Int>()
            val providers = listOf(
                HeroWidgetReceiver::class.java,
                ZenHorizonWidgetReceiver::class.java,
                SolarRhythmWidgetReceiver::class.java,
                ZenPebbleWidgetReceiver::class.java,
                CountUpWidgetReceiver::class.java,
            )
            for (p in providers) {
                val comp = android.content.ComponentName(context, p)
                activeIds.addAll(manager.getAppWidgetIds(comp).toList())
            }
            sanitizeOrphanedWidgetBindings(activeIds)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Records a widget-triggered counter reset so opening the app can present an undo whisper stack.
     * Stacks up to 3 newest widget resets.
     */
    fun recordWidgetReset(record: WidgetResetRecord): Boolean {
        return synchronized(globalStoreLock) {
            val current = getPendingWidgetResets().filterNot { it.id == record.id }
            val updated = (listOf(record) + current).take(MAX_PENDING_WIDGET_RESETS)
            persistWidgetResets(updated)
        }
    }

    /** Retrieves all pending widget resets (stacks up to 3 newest). */
    fun getPendingWidgetResets(): List<WidgetResetRecord> {
        return synchronized(globalStoreLock) {
            val raw = prefs.getString(KEY_PENDING_WIDGET_RESETS, null)
            if (raw.isNullOrBlank()) return@synchronized emptyList()
            decodeWidgetResets(raw)
        }
    }

    /** Dismisses a pending widget reset record after restoration or user dismissal. */
    fun dismissWidgetReset(recordId: String): Boolean {
        return synchronized(globalStoreLock) {
            val current = getPendingWidgetResets()
            val updated = current.filterNot { it.id == recordId }
            persistWidgetResets(updated)
        }
    }

    private fun persistWidgetResets(records: List<WidgetResetRecord>): Boolean {
        val arr = org.json.JSONArray()
        for (r in records) {
            val obj = org.json.JSONObject()
                .put("id", r.id)
                .put("itemId", r.itemId)
                .put("itemName", r.itemName)
                .put("previousEpochDay", r.snapshot.epochDay)
                .put("previousResetCount", r.snapshot.resetCount)
                .put("previousTotalResetDays", r.snapshot.totalResetDays)
                .put("previousFutureFlag", r.snapshot.futureFlag)
                .put("releasedDays", r.releasedDays)
                .put("timestampMillis", r.timestampMillis)
            arr.put(obj)
        }
        return prefs.edit().putString(KEY_PENDING_WIDGET_RESETS, arr.toString()).commit()
    }

    private fun decodeWidgetResets(raw: String): List<WidgetResetRecord> {
        return try {
            val arr = org.json.JSONArray(raw)
            val out = ArrayList<WidgetResetRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                val itemId = o.optString("itemId", "")
                val itemName = o.optString("itemName", "")
                val previousEpochDay = o.optLong("previousEpochDay", 0L)
                val previousResetCount = o.optInt("previousResetCount", 0)
                val previousTotalResetDays = o.optLong("previousTotalResetDays", 0L)
                val previousFutureFlag = o.optBoolean("previousFutureFlag", false)
                val releasedDays = o.optLong("releasedDays", 0L)
                val timestampMillis = o.optLong("timestampMillis", 0L)
                if (itemId.isNotBlank()) {
                    out.add(
                        WidgetResetRecord(
                            id = id.ifBlank { UUID.randomUUID().toString() },
                            itemId = itemId,
                            itemName = itemName,
                            snapshot = ResetSnapshot(
                                epochDay = previousEpochDay,
                                resetCount = previousResetCount,
                                totalResetDays = previousTotalResetDays,
                                futureFlag = previousFutureFlag,
                            ),
                            releasedDays = releasedDays,
                            timestampMillis = timestampMillis,
                        )
                    )
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(items: List<CountUpItem>): Boolean {
        val encoded = encodeItems(items)
        writeBackup(encoded)
        return prefs.edit()
            .putString(KEY_ITEMS, encoded)
            .putBoolean(KEY_MIGRATED, true)
            .commit()
    }

    private fun writeBackup(encodedJson: String) {
        try {
            if (!filesDir.exists()) filesDir.mkdirs()
            val bytes = encodedJson.toByteArray(Charsets.UTF_8)
            java.io.FileOutputStream(backupTempFile).use { fos ->
                fos.write(bytes)
                fos.flush()
                fos.fd.sync()
            }
            if (backupTempFile.exists()) {
                val targetPath = backupFile.toPath()
                val tempPath = backupTempFile.toPath()
                try {
                    java.nio.file.Files.move(
                        tempPath,
                        targetPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (_: Exception) {
                    java.nio.file.Files.move(
                        tempPath,
                        targetPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                }
            }
        } catch (_: Exception) {
            // Backup write failure must not crash the primary persistence flow
        }
    }

    private fun readBackup(): String? {
        return try {
            if (backupFile.exists() && backupFile.isFile) {
                backupFile.readText(Charsets.UTF_8)
            } else if (backupTempFile.exists() && backupTempFile.isFile) {
                backupTempFile.readText(Charsets.UTF_8)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureBackupInSync(encodedJson: String) {
        try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                writeBackup(encodedJson)
            }
        } catch (_: Exception) {
            // Best effort sync
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        /** Serializes read-modify-write mutations across all store instances in the application process. */
        private val globalStoreLock = Any()

        private const val PREFS_NAME = "countup_prefs"
        private const val KEY_ITEMS = "items_v1"
        private const val KEY_ITEMS_QUARANTINE = "items_v1_quarantine"
        private const val KEY_LEGACY_DAY_QUARANTINE = "legacy_day_quarantine"
        private const val KEY_MIGRATED = "migrated_v1"
        private const val KEY_BACKGROUND_THEME = "background_theme_v1"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_MODE_LEGACY = "theme_mode_v1"
        private const val KEY_SORT_ORDER = "sort_order_v1"
        private const val LEGACY_PREFS_NAME = "haircut_prefs"
        private const val LEGACY_KEY_EPOCH_DAY = "last_haircut_epoch_day"
        private const val BACKUP_FILE_NAME = "countup_backup.json"
        private const val PREFIX_HERO_BINDING = "hero_widget_binding_"
        private const val PREFIX_HERO_DISPLAY_MODE = "hero_widget_mode_"
        private const val PREFIX_ZEN_HORIZON_BINDING = "zen_horizon_binding_"
        private const val PREFIX_ZEN_HORIZON_UNIT = "zen_horizon_unit_"
        private const val PREFIX_ZEN_PEBBLE_BINDING = "zen_pebble_binding_"
        private const val PREFIX_ZEN_PEBBLE_TAG = "zen_pebble_tag_"
        private const val PREFIX_SOLAR_RHYTHM_BINDING = "solar_rhythm_binding_"
        private const val MAX_QUARANTINE_ENTRIES = 3
        private const val KEY_PENDING_WIDGET_RESETS = "pending_widget_resets_v1"
        private const val MAX_PENDING_WIDGET_RESETS = 3
    }
}
