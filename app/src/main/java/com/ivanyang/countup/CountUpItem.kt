package com.ivanyang.countup

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single count-up item: a human-readable [name] anchored to a calendar day
 * represented as an [epochDay] (the value of [java.time.LocalDate.toEpochDay]).
 */
data class CountUpItem(
    val id: String,
    val name: String,
    val epochDay: Long,
    val icon: String = "",
)

/** Default name used when a name is left blank. */
const val DEFAULT_ITEM_NAME: String = "Item"

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
                .put("icon", item.icon),
        )
    }
    return arr.toString()
}

/**
 * Decode a previously-encoded JSON array. Returns null for null input or for
 * malformed/structurally-wrong data; callers treat null as "recover".
 */
internal fun decodeItems(raw: String?): List<CountUpItem>? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(raw)
        val out = ArrayList<CountUpItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                CountUpItem(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    epochDay = o.getLong("epochDay"),
                    icon = o.optString("icon", ""),
                ),
            )
        }
        out
    } catch (_: Exception) {
        null
    }
}
