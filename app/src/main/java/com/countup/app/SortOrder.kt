package com.countup.app

import androidx.annotation.StringRes
import java.time.LocalDate

/**
 * Supported sorting modes for count-up items in the app list.
 */
enum class SortOrder(
    val id: String,
    @get:StringRes val labelRes: Int,
    @get:StringRes val descriptionRes: Int,
) {
    /** Most days elapsed first (largest count at the top). */
    DAYS_DESC("days_desc", R.string.sort_days_desc, R.string.cd_sort_days_desc),

    /** Most recent calendar anchor date first. */
    DATE_DESC("date_desc", R.string.sort_date_desc, R.string.cd_sort_date_desc),

    /** Alphabetical order by item name (A to Z). */
    NAME_ASC("name_asc", R.string.sort_name_asc, R.string.cd_sort_name_asc);

    /**
     * Cycles to the next sorting mode in the 3-step loop:
     * DAYS_DESC -> DATE_DESC -> NAME_ASC -> DAYS_DESC.
     */
    fun next(): SortOrder = when (this) {
        DAYS_DESC -> DATE_DESC
        DATE_DESC -> NAME_ASC
        NAME_ASC -> DAYS_DESC
    }

    companion object {
        fun fromId(raw: String?): SortOrder = when (raw) {
            DATE_DESC.id -> DATE_DESC
            NAME_ASC.id -> NAME_ASC
            else -> DAYS_DESC
        }
    }
}

/**
 * Pure deterministic sort function over a list of [CountUpItem]s.
 * Preserves stable tie-breaking using item name and unique id.
 */
fun sortItems(
    items: List<CountUpItem>,
    sortOrder: SortOrder,
    today: LocalDate = LocalDate.now(),
): List<CountUpItem> {
    if (items.size <= 1) return items
    return when (sortOrder) {
        SortOrder.DAYS_DESC -> items.sortedWith(
            compareByDescending<CountUpItem> { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }
        )
        SortOrder.DATE_DESC -> items.sortedWith(
            compareByDescending<CountUpItem> { it.epochDay }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }
        )
        SortOrder.NAME_ASC -> items.sortedWith(
            compareBy<CountUpItem> { it.name.lowercase() }
                .thenByDescending { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
                .thenBy { it.id }
        )
    }
}

/**
 * Pure case-insensitive substring filter matching against item name and comment.
 * If [query] is empty or whitespace, returns all items untouched.
 */
fun filterItems(
    items: List<CountUpItem>,
    query: String,
): List<CountUpItem> {
    val q = query.trim()
    if (q.isEmpty()) return items
    return items.filter { item ->
        item.name.contains(q, ignoreCase = true) || item.comment.contains(q, ignoreCase = true)
    }
}

/**
 * Convenience pipeline combining instant in-memory filtering and deterministic sorting.
 */
fun queryAndSortItems(
    items: List<CountUpItem>,
    query: String,
    sortOrder: SortOrder,
    today: LocalDate = LocalDate.now(),
): List<CountUpItem> {
    val filtered = filterItems(items, query)
    return sortItems(filtered, sortOrder, today)
}
