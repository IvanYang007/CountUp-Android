package com.countup.app

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

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
    val cardColor: String = "",
    /** True when this item was created or last updated with a future date. */
    val futureFlag: Boolean = false,
    /** True when this item appears in the home-screen widget. Defaults to open eye. */
    val showInWidget: Boolean = true,
    /** Total number of times this counter has been reset. */
    val resetCount: Int = 0,
    /** Cumulative days elapsed across all completed reset cycles. */
    val totalResetDays: Long = 0L,
    /** Epoch milliseconds when this item was pinned to the top; null indicates unpinned. */
    val pinnedTimestamp: Long? = null,
) {
    /** True when this item is pinned to the top of the list. */
    val isPinned: Boolean get() = pinnedTimestamp != null

    /** Mathematically-rounded average days per reset cycle. Returns 0 if never reset. */
    val averageResetDays: Int
        get() = if (resetCount > 0) {
            kotlin.math.round(totalResetDays.toDouble() / resetCount.toDouble()).toInt()
        } else {
            0
        }

    /**
     * Snapshot of this item's metrics prior to a reset operation.
     */
    fun toResetSnapshot(): ResetSnapshot = ResetSnapshot(
        epochDay = epochDay,
        resetCount = resetCount,
        totalResetDays = totalResetDays,
        futureFlag = futureFlag,
    )

    /**
     * Restores this counter's metrics from [snapshot].
     */
    fun restoreFrom(snapshot: ResetSnapshot): CountUpItem = copy(
        epochDay = snapshot.epochDay,
        resetCount = snapshot.resetCount,
        totalResetDays = snapshot.totalResetDays,
        futureFlag = snapshot.futureFlag,
    )

    /**
     * Determines whether this counter can be reset on [todayEpochDay].
     * Returns false if the anchor epochDay is already today, preventing redundant zero-day reset cycles.
     */
    fun isResettableOn(todayEpochDay: Long): Boolean = epochDay != todayEpochDay

    /**
     * Determines whether this counter can be reset on [today].
     */
    fun isResettableOn(today: LocalDate): Boolean = isResettableOn(today.toEpochDay())

    /**
     * Resolves an ultra-concise 1-word tag for compact 1x1 micro-widgets.
     */
    fun resolveOneWordLabel(customTag: String? = null): String {
        if (!customTag.isNullOrBlank()) {
            return customTag.trim().take(8).uppercase()
        }
        val fromComment = comment.trim().takeIf { it.isNotBlank() && !it.contains(" ") && it.length <= 8 }
        if (fromComment != null) {
            return fromComment.uppercase()
        }
        val firstWord = name.trim().split(Regex("\\s+")).firstOrNull()?.trim() ?: "ZEN"
        return firstWord.take(8).uppercase()
    }

    /**
     * Resets this counter to [newEpochDay] (typically today's date), updating the streak metrics.
     *
     * Clean domain transition:
     * - Stops trigger reset when accumulated date is 0 (or already reset today), returning unchanged item.
     * - Increments [resetCount].
     * - Accumulates elapsed cycle days into [totalResetDays].
     * - Resets [futureFlag] to false.
     */
    fun resetTo(newEpochDay: Long): CountUpItem {
        if (!isResettableOn(newEpochDay)) return this
        val cycleDays = abs(newEpochDay - epochDay)
        return copy(
            epochDay = newEpochDay,
            futureFlag = false,
            resetCount = resetCount + 1,
            totalResetDays = totalResetDays + cycleDays,
        )
    }
}

/**
 * Snapshot of an item's pre-reset state to support atomic undo/restore operations.
 */
@Immutable
data class ResetSnapshot(
    val epochDay: Long,
    val resetCount: Int,
    val totalResetDays: Long,
    val futureFlag: Boolean = false,
)

/**
 * Uncommitted draft payload representing user input in creation or edit dialogs.
 * Encapsulates the form fields to eliminate loose multi-primitive lambdas and prevent transposition bugs.
 */
@Immutable
data class ItemDraft(
    val name: String,
    val epochDay: Long,
    val comment: String = "",
    val icon: String = "",
    val cardColor: String = "",
    val isPinned: Boolean = false,
)

/**
 * Record of a counter reset triggered via the home-screen widget, preserved so that
 * opening the app can present an undo whisper stack.
 */
@Immutable
data class WidgetResetRecord(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val snapshot: ResetSnapshot,
    val releasedDays: Long,
    val timestampMillis: Long,
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
                .put("cardColor", item.cardColor)
                .put("futureFlag", item.futureFlag)
                .put("showInWidget", item.showInWidget)
                .put("resetCount", item.resetCount)
                .put("totalResetDays", item.totalResetDays)
                .apply {
                    if (item.pinnedTimestamp != null) {
                        put("pinnedTimestamp", item.pinnedTimestamp)
                    }
                },
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
        if (out.size < arr.length()) null else out
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
private fun extractJsonObjects(raw: String): List<String> {
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
private fun decodeElement(o: JSONObject?): CountUpItem? {
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

        val pinnedTimestamp = when (val raw = o.opt("pinnedTimestamp")) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }

        CountUpItem(
            id = id,
            name = name,
            epochDay = epochDay,
            comment = o.optString("comment", ""),
            icon = o.optString("icon", ""),
            cardColor = o.optString("cardColor", ""),
            futureFlag = o.optBoolean("futureFlag", false),
            showInWidget = o.optBoolean("showInWidget", true),
            resetCount = o.optInt("resetCount", 0).coerceAtLeast(0),
            totalResetDays = o.optLong("totalResetDays", 0L).coerceAtLeast(0L),
            pinnedTimestamp = pinnedTimestamp,
        )
    } catch (_: Exception) {
        null
    }
}
