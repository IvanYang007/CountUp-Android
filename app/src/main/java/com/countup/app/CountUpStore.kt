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
    private val backupRevFile: File = File(filesDir, BACKUP_REV_FILE_NAME)
    private val preRestoreFile: File = File(filesDir, PRE_RESTORE_SAFETY_FILE_NAME)

    /** Returns the persisted schema version of local storage. */
    fun getSchemaVersion(): Int = prefs.getInt(KEY_SCHEMA_VERSION, CURRENT_STORE_SCHEMA_VERSION)

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
                val prefsRevision = prefs.getLong(KEY_REVISION, 0L)
                val backupRevision = readBackupRevision()

                quarantineRawPayload(raw)

                if (backupDecoded != null && (backupRevision > prefsRevision || (backupRevision == prefsRevision && backupCount >= salvaged.size && backupCount > 0))) {
                    // Backup has equal or more items than salvage: self-heal from backup snapshot
                    prefs.edit()
                        .putString(KEY_ITEMS, backupRaw)
                        .putLong(KEY_REVISION, backupRevision)
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
                        .putLong(KEY_REVISION, backupRevision)
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
                    val backupRevision = readBackupRevision()
                    // Self-heal SharedPreferences from backup snapshot
                    prefs.edit()
                        .putString(KEY_ITEMS, backupRaw)
                        .putLong(KEY_REVISION, backupRevision)
                        .putBoolean(KEY_MIGRATED, true)
                        .commit()
                    return@synchronized backupDecoded
                }
            }

            // Tier 3: Pre-restore safety snapshot fallback
            if (hasPreRestoreSafetySnapshot()) {
                if (restorePreRestoreSafetySnapshot()) {
                    val restored = decodeItems(prefs.getString(KEY_ITEMS, null))
                    if (!restored.isNullOrEmpty()) return@synchronized restored
                }
            }

            // Tier 4: Legacy migration from haircut_prefs
            val legacyMigrated = tryMigrateFromLegacy()
            if (legacyMigrated != null) {
                return@synchronized legacyMigrated
            }

            // Tier 5: Unrecoverable payload quarantine or clean initial install
            if (!raw.isNullOrBlank()) {
                quarantineRawPayload(raw)
            }
            if (!prefs.getBoolean(KEY_MIGRATED, false) || !backupFile.exists()) {
                persist(emptyList())
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
        id: String = newId(),
    ): CountUpItem? {
        val trimmed = name.trim()
        val resolvedIcon = icon.trim().ifEmpty { ALL_ICON_NAMES.random() }
        val item = CountUpItem(
            id = id,
            name = trimmed.ifEmpty { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            comment = comment.trim(),
            icon = resolvedIcon,
            cardColor = cardColor.trim(),
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
            pinnedTimestamp = if (isPinned) System.currentTimeMillis() else null,
        )
        return synchronized(globalStoreLock) {
            val current = items()
            val existing = current.firstOrNull { it.id == id }
            if (existing != null) return@synchronized existing
            val updated = current + item
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
            val remaining = list.filterNot { it.id == id }
            val pending = getPendingWidgetResets().filterNot { it.itemId == id }
            val revision = nextRevision()
            val encodedItems = encodeItems(remaining)
            writeBackup(encodedItems, revision)
            val editor = prefs.edit()
                .putString(KEY_ITEMS, encodedItems)
                .putString(KEY_PENDING_WIDGET_RESETS, encodeWidgetResets(pending))
                .putLong(KEY_REVISION, revision)
                .putBoolean(KEY_MIGRATED, true)
            val success = editor.commit()
            if (success) {
                purgeWidgetBindingsForItem(id)
            }
            success
        }
    }

    /**
     * Purges all widget instance bindings and mode/tag preferences bound to [itemId]
     * across all 4 configurable widget families (Hero, Zen Horizon, Zen Pebble, Solar Rhythm).
     * @return count of keys purged.
     */
    fun purgeWidgetBindingsForItem(itemId: String): Int {
        return synchronized(globalStoreLock) {
            val editor = prefs.edit()
            var purgedCount = 0
            for ((key, value) in prefs.all) {
                if (value == itemId) {
                    when {
                        key.startsWith(PREFIX_HERO_BINDING) -> {
                            val widgetId = key.removePrefix(PREFIX_HERO_BINDING).toIntOrNull()
                            editor.remove(key)
                            purgedCount++
                            if (widgetId != null) {
                                editor.remove(PREFIX_HERO_DISPLAY_MODE + widgetId)
                                purgedCount++
                            }
                        }
                        key.startsWith(PREFIX_ZEN_HORIZON_BINDING) -> {
                            val widgetId = key.removePrefix(PREFIX_ZEN_HORIZON_BINDING).toIntOrNull()
                            editor.remove(key)
                            purgedCount++
                            if (widgetId != null) {
                                editor.remove(PREFIX_ZEN_HORIZON_UNIT + widgetId)
                                purgedCount++
                            }
                        }
                        key.startsWith(PREFIX_ZEN_PEBBLE_BINDING) -> {
                            val widgetId = key.removePrefix(PREFIX_ZEN_PEBBLE_BINDING).toIntOrNull()
                            editor.remove(key)
                            purgedCount++
                            if (widgetId != null) {
                                editor.remove(PREFIX_ZEN_PEBBLE_TAG + widgetId)
                                purgedCount++
                            }
                        }
                        key.startsWith(PREFIX_SOLAR_RHYTHM_BINDING) -> {
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
     * Atomically resets the item with [id] to [epochDay] and records [record] in the
     * pending widget reset queue within a single transactional disk operation.
     * @return false if not found, already 0 accumulated days, or the write failed.
     */
    fun resetWithUndo(id: String, epochDay: Long, record: WidgetResetRecord): Boolean {
        return synchronized(globalStoreLock) {
            val list = items().toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return false
            val current = list[index]
            if (!current.isResettableOn(epochDay)) return false
            val updated = current.resetTo(epochDay)
            if (updated === current) return false
            list[index] = updated

            val currentResets = getPendingWidgetResets().filterNot { it.id == record.id }
            val updatedResets = (listOf(record) + currentResets).take(MAX_PENDING_WIDGET_RESETS)

            val revision = nextRevision()
            val encodedItems = encodeItems(list)
            writeBackup(encodedItems, revision)

            prefs.edit()
                .putString(KEY_ITEMS, encodedItems)
                .putString(KEY_PENDING_WIDGET_RESETS, encodeWidgetResets(updatedResets))
                .putLong(KEY_REVISION, revision)
                .putBoolean(KEY_MIGRATED, true)
                .commit()
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
        return TimeDisplayMode.fromCode(prefs.getString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, null))
    }

    /** Persists the [TimeDisplayMode] for a Hero Widget instance. */
    fun setHeroWidgetDisplayMode(appWidgetId: Int, mode: TimeDisplayMode): Boolean {
        return prefs.edit()
            .putString(PREFIX_HERO_DISPLAY_MODE + appWidgetId, mode.code)
            .commit()
    }

    /** Retrieves the saved [ZenWidgetDisplayUnit] for a Zen Horizon instance, defaulting to [ZenWidgetDisplayUnit.DAYS]. */
    fun getZenHorizonUnit(appWidgetId: Int): ZenWidgetDisplayUnit {
        return ZenWidgetDisplayUnit.fromCode(prefs.getString(PREFIX_ZEN_HORIZON_UNIT + appWidgetId, null))
    }

    /** Persists the [ZenWidgetDisplayUnit] for a Zen Horizon instance. */
    fun setZenHorizonUnit(appWidgetId: Int, unit: ZenWidgetDisplayUnit): Boolean {
        return prefs.edit()
            .putString(PREFIX_ZEN_HORIZON_UNIT + appWidgetId, unit.code)
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
     * ID (UUID) is not already present. Preserves existing appearance settings.
     */
    /** Writes an ephemeral pre-restore safety snapshot to disk before replacing database. */
    fun writePreRestoreSafetySnapshot(snapshot: CountUpBackupPayload = exportBackupPayload()): Boolean {
        return try {
            if (!filesDir.exists()) filesDir.mkdirs()
            preRestoreFile.writeText(CountUpBackupPayload.encode(snapshot), Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Reverts storage to the pre-restore safety snapshot if available. */
    fun restorePreRestoreSafetySnapshot(): Boolean {
        return try {
            if (!preRestoreFile.exists()) return false
            val raw = preRestoreFile.readText(Charsets.UTF_8)
            val payload = CountUpBackupPayload.decode(raw) ?: return false
            val revision = nextRevision()
            val encodedItems = encodeItems(payload.items)
            writeBackup(encodedItems, revision)
            prefs.edit()
                .putString(KEY_SORT_ORDER, payload.sortOrder.id)
                .putString(KEY_THEME_MODE, payload.themeMode.id)
                .remove(KEY_THEME_MODE_LEGACY)
                .putString(KEY_BACKGROUND_THEME, payload.backgroundTheme.id)
                .putString(KEY_ITEMS, encodedItems)
                .putLong(KEY_REVISION, revision)
                .putBoolean(KEY_MIGRATED, true)
                .commit()
        } catch (_: Exception) {
            false
        }
    }

    /** Returns true if a pre-restore safety snapshot currently exists on disk. */
    fun hasPreRestoreSafetySnapshot(): Boolean = preRestoreFile.exists() && preRestoreFile.length() > 0L

    private fun deletePreRestoreSafetySnapshot() {
        try {
            if (preRestoreFile.exists()) {
                preRestoreFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun restoreBackupPayload(payload: CountUpBackupPayload, strategy: RestoreStrategy): Boolean {
        return synchronized(globalStoreLock) {
            when (strategy) {
                RestoreStrategy.REPLACE_ALL -> {
                    writePreRestoreSafetySnapshot()
                    try {
                        val revision = nextRevision()
                        val encodedItems = encodeItems(payload.items)
                        writeBackup(encodedItems, revision)
                        val success = prefs.edit()
                            .putString(KEY_SORT_ORDER, payload.sortOrder.id)
                            .putString(KEY_THEME_MODE, payload.themeMode.id)
                            .remove(KEY_THEME_MODE_LEGACY)
                            .putString(KEY_BACKGROUND_THEME, payload.backgroundTheme.id)
                            .putString(KEY_ITEMS, encodedItems)
                            .putLong(KEY_REVISION, revision)
                            .putBoolean(KEY_MIGRATED, true)
                            .commit()

                        if (!success) {
                            restorePreRestoreSafetySnapshot()
                            return@synchronized false
                        }
                        deletePreRestoreSafetySnapshot()
                        true
                    } catch (_: Exception) {
                        restorePreRestoreSafetySnapshot()
                        false
                    }
                }
                RestoreStrategy.MERGE_KEEP_EXISTING -> {
                    val currentItems = items().toMutableList()
                    val existingIds = currentItems.map { it.id }.toMutableSet()

                    val itemsToAdd = mutableListOf<CountUpItem>()
                    for (item in payload.items) {
                        if (item.id !in existingIds) {
                            itemsToAdd.add(item)
                            existingIds.add(item.id)
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
     * Remaps widget binding preferences when the launcher restores widgets with new widget IDs
     * (e.g. across device migration or cloud backup restoration).
     * @return count of keys remapped.
     */
    fun remapWidgetBindings(oldWidgetIds: IntArray, newWidgetIds: IntArray): Int {
        if (oldWidgetIds.isEmpty() || oldWidgetIds.size != newWidgetIds.size) return 0
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
            var remappedCount = 0

            for (i in oldWidgetIds.indices) {
                val oldId = oldWidgetIds[i]
                val newId = newWidgetIds[i]
                if (oldId == newId) continue

                for (prefix in bindingPrefixes) {
                    val oldKey = "$prefix$oldId"
                    val newKey = "$prefix$newId"
                    if (prefs.contains(oldKey)) {
                        val value = prefs.all[oldKey]
                        when (value) {
                            is String -> editor.putString(newKey, value)
                            is Int -> editor.putInt(newKey, value)
                            is Long -> editor.putLong(newKey, value)
                            is Boolean -> editor.putBoolean(newKey, value)
                            is Float -> editor.putFloat(newKey, value)
                        }
                        editor.remove(oldKey)
                        remappedCount++
                    }
                }
            }
            if (remappedCount > 0) {
                editor.commit()
            }
            remappedCount
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
        return prefs.edit().putString(KEY_PENDING_WIDGET_RESETS, encodeWidgetResets(records)).commit()
    }

    private fun encodeWidgetResets(records: List<WidgetResetRecord>): String {
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
        return arr.toString()
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
        val revision = nextRevision()
        val encoded = encodeItems(items)
        writeBackup(encoded, revision)
        return prefs.edit()
            .putString(KEY_ITEMS, encoded)
            .putLong(KEY_REVISION, revision)
            .putBoolean(KEY_MIGRATED, true)
            .commit()
    }

    private fun nextRevision(): Long {
        val current = prefs.getLong(KEY_REVISION, 0L)
        val now = System.currentTimeMillis()
        return if (now > current) now else current + 1L
    }

    /** Returns the current monotonic storage revision number. */
    fun getRevision(): Long = prefs.getLong(KEY_REVISION, 0L)

    private fun writeBackup(encodedJson: String, revision: Long) {
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
            try {
                backupRevFile.writeText(revision.toString(), Charsets.UTF_8)
            } catch (_: Exception) {}
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

    private fun readBackupRevision(): Long {
        return try {
            if (backupRevFile.exists()) {
                backupRevFile.readText(Charsets.UTF_8).trim().toLongOrNull() ?: backupFile.lastModified()
            } else if (backupFile.exists()) {
                backupFile.lastModified()
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun ensureBackupInSync(encodedJson: String) {
        try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                writeBackup(encodedJson, prefs.getLong(KEY_REVISION, 0L))
            }
        } catch (_: Exception) {
            // Best effort sync
        }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        /** Serializes read-modify-write mutations across all store instances in the application process. */
        private val globalStoreLock = Any()

        @Volatile
        private var instance: CountUpStore? = null

        /**
         * Returns the process-wide cached singleton instance of [CountUpStore].
         * Thread-safe and safe for concurrent access from any thread, activity, or receiver.
         */
        fun getInstance(context: Context): CountUpStore =
            instance ?: synchronized(globalStoreLock) {
                instance ?: CountUpStore(context.applicationContext ?: context).also { instance = it }
            }

        /** Clears the singleton instance cache for unit test isolation. */
        internal fun resetInstanceForTesting() {
            synchronized(globalStoreLock) {
                instance = null
            }
        }

        private const val PREFS_NAME = "countup_prefs"
        private const val KEY_SCHEMA_VERSION = "schema_version"
        const val CURRENT_STORE_SCHEMA_VERSION = 1
        private const val KEY_ITEMS = "items_v1"
        private const val KEY_ITEMS_QUARANTINE = "items_v1_quarantine"
        private const val KEY_LEGACY_DAY_QUARANTINE = "legacy_day_quarantine"
        private const val KEY_MIGRATED = "migrated_v1"
        private const val KEY_BACKGROUND_THEME = "background_theme_v1"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_MODE_LEGACY = "theme_mode_v1"
        private const val KEY_SORT_ORDER = "sort_order_v1"
        private const val KEY_REVISION = "storage_revision_v1"
        private const val LEGACY_PREFS_NAME = "haircut_prefs"
        private const val LEGACY_KEY_EPOCH_DAY = "last_haircut_epoch_day"
        private const val BACKUP_FILE_NAME = "countup_backup.json"
        private const val BACKUP_REV_FILE_NAME = "countup_backup.rev"
        private const val PRE_RESTORE_SAFETY_FILE_NAME = "countup_pre_restore_safety.json"
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
