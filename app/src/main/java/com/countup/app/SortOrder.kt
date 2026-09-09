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

    /** Fewest days elapsed first (smallest count at the top). */
    DAYS_ASC("days_asc", R.string.sort_days_asc, R.string.cd_sort_days_asc),

    /** Most recent calendar anchor date first. */
    DATE_DESC("date_desc", R.string.sort_date_desc, R.string.cd_sort_date_desc),

    /** Oldest calendar anchor date first. */
    DATE_ASC("date_asc", R.string.sort_date_asc, R.string.cd_sort_date_asc),

    /** Alphabetical order by item name (A to Z). */
    NAME_ASC("name_asc", R.string.sort_name_asc, R.string.cd_sort_name_asc),

    /** Reverse alphabetical order by item name (Z to A). */
    NAME_DESC("name_desc", R.string.sort_name_desc, R.string.cd_sort_name_desc);

    /**
     * Toggles between ascending and descending for the current sort criterion.
     */
    fun toggleDirection(): SortOrder = when (this) {
        DAYS_DESC -> DAYS_ASC
        DAYS_ASC -> DAYS_DESC
        DATE_DESC -> DATE_ASC
        DATE_ASC -> DATE_DESC
        NAME_ASC -> NAME_DESC
        NAME_DESC -> NAME_ASC
    }

    /**
     * Cycles to the next sorting criterion in the loop:
     * DAYS -> DATE -> NAME -> DAYS.
     */
    fun next(): SortOrder = when (this) {
        DAYS_DESC, DAYS_ASC -> DATE_DESC
        DATE_DESC, DATE_ASC -> NAME_ASC
        NAME_ASC, NAME_DESC -> DAYS_DESC
    }

    companion object {
        fun fromId(raw: String?): SortOrder = when (raw) {
            DAYS_ASC.id -> DAYS_ASC
            DATE_DESC.id -> DATE_DESC
            DATE_ASC.id -> DATE_ASC
            NAME_ASC.id -> NAME_ASC
            NAME_DESC.id -> NAME_DESC
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

    val orderComparator = when (sortOrder) {
        SortOrder.DAYS_DESC -> compareByDescending<CountUpItem> { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOrder.DAYS_ASC -> compareBy<CountUpItem> { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOrder.DATE_DESC -> compareByDescending<CountUpItem> { it.epochDay }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOrder.DATE_ASC -> compareBy<CountUpItem> { it.epochDay }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOrder.NAME_ASC -> compareBy<CountUpItem> { it.name.lowercase() }
            .thenByDescending { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
            .thenBy { it.id }
        SortOrder.NAME_DESC -> compareByDescending<CountUpItem> { it.name.lowercase() }
            .thenByDescending { daysSince(LocalDate.ofEpochDay(it.epochDay), today) }
            .thenBy { it.id }
    }

    return items.sortedWith(
        compareByDescending<CountUpItem> { it.isPinned }
            .then(orderComparator)
    )
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
