package com.countup.app

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * A single count-up item: a human-readable [name] anchored to a calendar day
 * represented as an [epochDay] (the value of [java.time.LocalDate.toEpochDay]).
 */
@Immutable
data class CountUpItem(
    val id: String,
    val name: String,
    val epochDay: Long,
    val comment: String = "",
    val icon: String = "",
    /** True when this item was created or last updated with a future date. */
    val futureFlag: Boolean = false,
    /** True when this item appears in the home-screen widget. Defaults to open eye. */
    val showInWidget: Boolean = true,
)

/** Default name used when a name is left blank. */
const val DEFAULT_ITEM_NAME: String = "Item"

/** Inclusive epoch-day bounds of [LocalDate]; values outside this range crash render. */
private val EPOCH_DAY_MIN: Long = LocalDate.MIN.toEpochDay()
private val EPOCH_DAY_MAX: Long = LocalDate.MAX.toEpochDay()

/**
 * Encode a list of items to a compact JSON array string for storage.
 * Pure and platform-independent so it can be unit-tested on the JVM.
 */
internal fun encodeItems(items: List<CountUpItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("epochDay", item.epochDay)
                .put("comment", item.comment)
                .put("icon", item.icon)
                .put("futureFlag", item.futureFlag)
                .put("showInWidget", item.showInWidget),
        )
    }
    return arr.toString()
}

/**
 * Decode a previously-encoded JSON array with maximum fault tolerance.
 *
 * 1. Attempts standard [JSONArray] decoding.
 * 2. If the array envelope is broken or truncated, falls back to [salvageItems]
 *    to extract and recover all self-contained valid items.
 * 3. Returns null only when the input is null/blank or completely unrecoverable,
 *    allowing callers to quarantine the raw payload without losing salvageable items.
 */
internal fun decodeItems(raw: String?): List<CountUpItem>? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(raw)
        if (arr.length() == 0) return emptyList()
        val out = ArrayList<CountUpItem>(arr.length())
        for (i in 0 until arr.length()) {
            decodeElement(arr.optJSONObject(i))?.let { out.add(it) }
        }
        if (out.isEmpty() && arr.length() > 0) null else out
    } catch (_: Exception) {
        null
    }
}

/**
 * Salvages valid [CountUpItem] objects from a syntactically malformed, corrupted,
 * or truncated JSON string by scanning and extracting balanced JSON object tokens,
 * properly respecting string literals with nested curly braces.
 */
internal fun salvageItems(raw: String?): List<CountUpItem> {
    if (raw.isNullOrBlank()) return emptyList()
    val out = ArrayList<CountUpItem>()
    val seenIds = HashSet<String>()
    val tokens = extractJsonObjects(raw)

    for (token in tokens) {
        try {
            val json = JSONObject(token)
            val item = decodeElement(json)
            if (item != null && seenIds.add(item.id)) {
                out.add(item)
            }
        } catch (_: Exception) {
            // Ignore unparseable tokens and continue scanning
        }
    }
    return out
}

/**
 * Scans [raw] and extracts all top-level balanced `{ ... }` JSON object strings,
 * properly tracking string literal boundaries (`"..."`) and escape characters (`\`).
 * This correctly preserves user comments and names that contain curly braces (e.g. `"{warmup}"`).
 */
internal fun extractJsonObjects(raw: String): List<String> {
    val objects = ArrayList<String>()
    var depth = 0
    var startIndex = -1
    var inString = false
    var escape = false

    for (i in raw.indices) {
        val c = raw[i]
        if (escape) {
            escape = false
            continue
        }
        if (c == '\\' && inString) {
            escape = true
            continue
        }
        if (c == '"') {
            inString = !inString
            continue
        }
        if (!inString) {
            if (c == '{') {
                if (depth == 0) {
                    startIndex = i
                }
                depth++
            } else if (c == '}') {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        objects.add(raw.substring(startIndex, i + 1))
                        startIndex = -1
                    }
                }
            }
        }
    }
    return objects
}

/**
 * Decodes one JSON object element defensively with type coercion and safe defaults:
 * - Coerces [epochDay] from numbers or string representations within valid LocalDate bounds.
 * - Generates a valid fallback [id] if missing or empty.
 * - Defaults [name] to [DEFAULT_ITEM_NAME] if missing or blank.
 * - Defaults [comment] to "", [icon] to "", [futureFlag] to false, and [showInWidget] to true.
 * - Safely ignores any extra/unknown future fields.
 */
internal fun decodeElement(o: JSONObject?): CountUpItem? {
    if (o == null) return null
    return try {
        val epochDay = when (val rawVal = o.opt("epochDay")) {
            is Number -> rawVal.toLong()
            is String -> rawVal.toLongOrNull() ?: rawVal.toDoubleOrNull()?.toLong()
            else -> null
        } ?: return null

        if (epochDay < EPOCH_DAY_MIN || epochDay > EPOCH_DAY_MAX) return null

        val rawId = o.optString("id", "").trim()
        val id = rawId.ifEmpty { UUID.randomUUID().toString() }

        val rawName = o.optString("name", "").trim()
        val name = rawName.ifEmpty { DEFAULT_ITEM_NAME }

        CountUpItem(
            id = id,
            name = name,
            epochDay = epochDay,
            comment = o.optString("comment", ""),
            icon = o.optString("icon", ""),
            futureFlag = o.optBoolean("futureFlag", false),
            showInWidget = o.optBoolean("showInWidget", true),
        )
    } catch (_: Exception) {
        null
    }
}
