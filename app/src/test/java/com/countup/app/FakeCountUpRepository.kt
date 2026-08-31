package com.countup.app

import java.time.LocalDate
import java.util.UUID

/**
 * In-memory fake implementation of [CountUpRepository] for fast, hermetic unit tests.
 */
class FakeCountUpRepository(
    initialItems: List<CountUpItem> = emptyList(),
    initialTheme: BackgroundTheme = BackgroundTheme.AUTO_DAILY,
    initialSortOrder: SortOrder = SortOrder.DAYS_DESC,
) : CountUpRepository {

    private val itemsList = initialItems.toMutableList()
    private var theme: BackgroundTheme = initialTheme
    private var sortOrder: SortOrder = initialSortOrder

    var shouldFailWrite: Boolean = false

    override fun getItems(): List<CountUpItem> = itemsList.toList()

    override fun addItem(
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
    ): CountUpItem? {
        if (shouldFailWrite) return null
        val item = CountUpItem(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            comment = comment,
            icon = icon.ifBlank { "star" },
            cardColor = cardColor,
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
            showInWidget = true,
        )
        itemsList.add(item)
        return item
    }

    override fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
    ): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val existing = itemsList[idx]
        itemsList[idx] = existing.copy(
            name = name.ifBlank { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            comment = comment,
            icon = icon.ifBlank { existing.icon },
            cardColor = cardColor,
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
        )
        return true
    }

    override fun deleteItem(id: String): Boolean {
        if (shouldFailWrite) return false
        return itemsList.removeIf { it.id == id }
    }

    override fun resetTo(id: String, epochDay: Long): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        itemsList[idx] = itemsList[idx].copy(epochDay = epochDay, futureFlag = false)
        return true
    }

    override fun setWidgetVisibility(id: String, showInWidget: Boolean): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        itemsList[idx] = itemsList[idx].copy(showInWidget = showInWidget)
        return true
    }

    override fun getBackgroundTheme(): BackgroundTheme = theme

    override fun setBackgroundTheme(theme: BackgroundTheme): Boolean {
        if (shouldFailWrite) return false
        this.theme = theme
        return true
    }

    override fun getSortOrder(): SortOrder = sortOrder

    override fun setSortOrder(order: SortOrder): Boolean {
        if (shouldFailWrite) return false
        this.sortOrder = order
        return true
    }
}
