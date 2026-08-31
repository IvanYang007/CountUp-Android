package com.countup.app

import java.time.LocalDate

/**
 * Repository interface defining data access operations for CountUp items,
 * sort order preferences, and Zen background themes.
 */
interface CountUpRepository {
    fun getItems(): List<CountUpItem>
    fun addItem(
        name: String,
        epochDay: Long,
        comment: String = "",
        icon: String = "",
        cardColor: String = "",
    ): CountUpItem?
    fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String = "",
        icon: String = "",
        cardColor: String = "",
    ): Boolean
    fun deleteItem(id: String): Boolean
    fun resetTo(id: String, epochDay: Long = LocalDate.now().toEpochDay()): Boolean
    fun setWidgetVisibility(id: String, showInWidget: Boolean): Boolean
    fun getBackgroundTheme(): BackgroundTheme
    fun setBackgroundTheme(theme: BackgroundTheme): Boolean
    fun getSortOrder(): SortOrder
    fun setSortOrder(order: SortOrder): Boolean
}

/**
 * Production implementation of [CountUpRepository] backed by [CountUpStore].
 * Maintains zero data loss and multi-tier fail-safe recovery guarantees.
 */
class DefaultCountUpRepository(
    private val store: CountUpStore
) : CountUpRepository {
    override fun getItems(): List<CountUpItem> = store.items()

    override fun addItem(
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
    ): CountUpItem? =
        store.addItem(name, epochDay, comment, icon, cardColor)

    override fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
    ): Boolean =
        store.updateItem(id, name, epochDay, comment, icon, cardColor)

    override fun deleteItem(id: String): Boolean =
        store.deleteItem(id)

    override fun resetTo(id: String, epochDay: Long): Boolean =
        store.resetTo(id, epochDay)

    override fun setWidgetVisibility(id: String, showInWidget: Boolean): Boolean =
        store.setWidgetVisibility(id, showInWidget)

    override fun getBackgroundTheme(): BackgroundTheme =
        store.getBackgroundTheme()

    override fun setBackgroundTheme(theme: BackgroundTheme): Boolean =
        store.setBackgroundTheme(theme)

    override fun getSortOrder(): SortOrder =
        store.getSortOrder()

    override fun setSortOrder(order: SortOrder): Boolean =
        store.setSortOrder(order)
}
