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
        isPinned: Boolean = false,
    ): CountUpItem?
    fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String = "",
        icon: String = "",
        cardColor: String = "",
        isPinned: Boolean = false,
    ): Boolean
    fun deleteItem(id: String): Boolean
    fun resetTo(id: String, epochDay: Long = LocalDate.now().toEpochDay()): Boolean
    fun restoreReset(id: String, snapshot: ResetSnapshot): Boolean
    fun recordWidgetReset(record: WidgetResetRecord): Boolean
    fun getPendingWidgetResets(): List<WidgetResetRecord>
    fun dismissWidgetReset(recordId: String): Boolean
    fun setWidgetVisibility(id: String, showInWidget: Boolean): Boolean
    fun getBackgroundTheme(): BackgroundTheme
    fun setBackgroundTheme(theme: BackgroundTheme): Boolean
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode): Boolean
    fun getSortOrder(): SortOrder
    fun setSortOrder(order: SortOrder): Boolean
    fun exportBackupPayload(): CountUpBackupPayload
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
        isPinned: Boolean,
    ): CountUpItem? =
        store.addItem(name, epochDay, comment, icon, cardColor, isPinned)

    override fun updateItem(
        id: String,
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
        isPinned: Boolean,
    ): Boolean =
        store.updateItem(id, name, epochDay, comment, icon, cardColor, isPinned)

    override fun deleteItem(id: String): Boolean =
        store.deleteItem(id)

    override fun resetTo(id: String, epochDay: Long): Boolean =
        store.resetTo(id, epochDay)

    override fun restoreReset(id: String, snapshot: ResetSnapshot): Boolean =
        store.restoreReset(id, snapshot)

    override fun recordWidgetReset(record: WidgetResetRecord): Boolean =
        store.recordWidgetReset(record)

    override fun getPendingWidgetResets(): List<WidgetResetRecord> =
        store.getPendingWidgetResets()

    override fun dismissWidgetReset(recordId: String): Boolean =
        store.dismissWidgetReset(recordId)

    override fun setWidgetVisibility(id: String, showInWidget: Boolean): Boolean =
        store.setWidgetVisibility(id, showInWidget)

    override fun getBackgroundTheme(): BackgroundTheme =
        store.getBackgroundTheme()

    override fun setBackgroundTheme(theme: BackgroundTheme): Boolean =
        store.setBackgroundTheme(theme)

    override fun getThemeMode(): ThemeMode =
        store.getThemeMode()

    override fun setThemeMode(mode: ThemeMode): Boolean =
        store.setThemeMode(mode)

    override fun getSortOrder(): SortOrder =
        store.getSortOrder()

    override fun setSortOrder(order: SortOrder): Boolean =
        store.setSortOrder(order)

    override fun exportBackupPayload(): CountUpBackupPayload =
        store.exportBackupPayload()
}
