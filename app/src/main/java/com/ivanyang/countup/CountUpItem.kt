package com.ivanyang.countup

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * A single count-up item: a human-readable [name] anchored to a calendar day
 * represented as an [epochDay] (the value of [java.time.LocalDate.toEpochDay]).
 */
data class CountUpItem(
    val id: String,
    val name: String,
    val epochDay: Long,
    val icon: String = "",
    /** True when this item was created or last updated with a future date. */
    val futureFlag: Boolean = false,
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
                .put("icon", item.icon)
                .put("futureFlag", item.futureFlag),
        )
    }
    return arr.toString()
}

/**
 * Decode a previously-encoded JSON array. Returns null for null input, for
 * non-array data, or for an array whose every element is malformed (callers
 * treat null as "recover"). Elements that are individually malformed or carry
 * an out-of-range epochDay are dropped, keeping the remaining parseable items
 * so one bad element never destroys the whole list.
 */
internal fun decodeItems(raw: String?): List<CountUpItem>? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(raw)
        val out = ArrayList<CountUpItem>(arr.length())
        for (i in 0 until arr.length()) {
            decodeElement(arr.optJSONObject(i))?.let { out.add(it) }
        }
        // Every element failed to decode: treat the whole payload as corrupt so
        // the store can quarantine it instead of masking it as "no items".
        if (out.isEmpty() && arr.length() > 0) return null
        out
    } catch (_: Exception) {
        null
    }
}

/** Decodes one array element, or null when the element is malformed or its epochDay is out of range. */
private fun decodeElement(o: JSONObject?): CountUpItem? {
    if (o == null) return null
    return try {
        val epochDay = o.getLong("epochDay")
        if (epochDay < EPOCH_DAY_MIN || epochDay > EPOCH_DAY_MAX) return null
        CountUpItem(
            id = o.getString("id"),
            name = o.getString("name"),
            epochDay = epochDay,
            icon = o.optString("icon", ""),
            futureFlag = o.optBoolean("futureFlag", false),
        )
    } catch (_: Exception) {
        null
    }
}
