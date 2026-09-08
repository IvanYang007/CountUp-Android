package com.countup.app

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * Portable self-contained backup container for CountUp.
 * Encapsulates items and global preferences while omitting device-specific widget IDs.
 */
@Immutable
data class CountUpBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = CURRENT_APP_VERSION,
    val sortOrder: SortOrder = SortOrder.DAYS_DESC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.AUTO_DAILY,
    val items: List<CountUpItem> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val CURRENT_APP_VERSION = "2.19.3"

        /**
         * Encodes [payload] into a formatted JSON string.
         * Explicitly preserves Chinese characters, Unicode symbols, and multi-line notes.
         */
        fun encode(payload: CountUpBackupPayload): String {
            return JSONObject().apply {
                put("schemaVersion", payload.schemaVersion)
                put("exportTimestamp", payload.exportTimestamp)
                put("appVersion", payload.appVersion)
                put("sortOrder", payload.sortOrder.id)
                put("themeMode", payload.themeMode.id)
                put("backgroundTheme", payload.backgroundTheme.id)
                put("itemsJson", encodeItems(payload.items))
            }.toString(2)
        }

        /**
         * Decodes a raw JSON string into a [CountUpBackupPayload].
         * Resilient to partially corrupted or truncated payloads by leveraging [salvageItems].
         */
        fun decode(raw: String?): CountUpBackupPayload? {
            if (raw.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val itemsJson = obj.optString("itemsJson", "")
                val items = decodeItems(itemsJson) ?: salvageItems(itemsJson)
                CountUpBackupPayload(
                    schemaVersion = obj.optInt("schemaVersion", CURRENT_SCHEMA_VERSION),
                    exportTimestamp = obj.optLong("exportTimestamp", 0L),
                    appVersion = obj.optString("appVersion", CURRENT_APP_VERSION),
                    sortOrder = SortOrder.fromId(obj.optString("sortOrder", "")),
                    themeMode = ThemeMode.fromId(obj.optString("themeMode", "")),
                    backgroundTheme = BackgroundTheme.fromId(obj.optString("backgroundTheme", "")),
                    items = items,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
