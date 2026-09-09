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
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
) : CountUpRepository {

    private val itemsList = initialItems.toMutableList()
    private var theme: BackgroundTheme = initialTheme
    private var sortOrder: SortOrder = initialSortOrder
    private var themeMode: ThemeMode = initialThemeMode

    var shouldFailWrite: Boolean = false

    override fun getItems(): List<CountUpItem> = itemsList.toList()

    override fun addItem(
        name: String,
        epochDay: Long,
        comment: String,
        icon: String,
        cardColor: String,
        isPinned: Boolean,
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
            pinnedTimestamp = if (isPinned) System.currentTimeMillis() else null,
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
        isPinned: Boolean,
    ): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val existing = itemsList[idx]
        val newPinnedTimestamp = when {
            isPinned && existing.pinnedTimestamp == null -> System.currentTimeMillis()
            !isPinned -> null
            else -> existing.pinnedTimestamp
        }
        itemsList[idx] = existing.copy(
            name = name.ifBlank { DEFAULT_ITEM_NAME },
            epochDay = epochDay,
            comment = comment,
            icon = icon.ifBlank { existing.icon },
            cardColor = cardColor,
            futureFlag = epochDay > LocalDate.now().toEpochDay(),
            pinnedTimestamp = newPinnedTimestamp,
        )
        return true
    }

    override fun deleteItem(id: String): Boolean {
        if (shouldFailWrite) return false
        val removed = itemsList.removeIf { it.id == id }
        if (removed) {
            pendingWidgetResets.removeIf { it.itemId == id }
        }
        return removed
    }

    private val pendingWidgetResets = mutableListOf<WidgetResetRecord>()

    override fun resetTo(id: String, epochDay: Long): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val current = itemsList[idx]
        if (!current.isResettableOn(epochDay)) return false
        val updated = current.resetTo(epochDay)
        if (updated === current) return false
        itemsList[idx] = updated
        return true
    }

    override fun restoreReset(
        id: String,
        snapshot: ResetSnapshot,
    ): Boolean {
        if (shouldFailWrite) return false
        val idx = itemsList.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val current = itemsList[idx]
        itemsList[idx] = current.restoreFrom(snapshot)
        return true
    }

    override fun recordWidgetReset(record: WidgetResetRecord): Boolean {
        if (shouldFailWrite) return false
        pendingWidgetResets.removeAll { it.id == record.id }
        pendingWidgetResets.add(0, record)
        while (pendingWidgetResets.size > 3) {
            pendingWidgetResets.removeAt(pendingWidgetResets.lastIndex)
        }
        return true
    }

    override fun getPendingWidgetResets(): List<WidgetResetRecord> = pendingWidgetResets.toList()

    override fun dismissWidgetReset(recordId: String): Boolean {
        if (shouldFailWrite) return false
        return pendingWidgetResets.removeAll { it.id == recordId }
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

    override fun getThemeMode(): ThemeMode = themeMode

    override fun setThemeMode(mode: ThemeMode): Boolean {
        if (shouldFailWrite) return false
        this.themeMode = mode
        return true
    }

    override fun getSortOrder(): SortOrder = sortOrder

    override fun setSortOrder(order: SortOrder): Boolean {
        if (shouldFailWrite) return false
        this.sortOrder = order
        return true
    }

    override fun exportBackupPayload(): CountUpBackupPayload =
        CountUpBackupPayload(
            sortOrder = sortOrder,
            themeMode = themeMode,
            backgroundTheme = theme,
            items = itemsList.toList(),
        )

    override fun restoreBackupPayload(payload: CountUpBackupPayload, strategy: RestoreStrategy): Boolean {
        if (shouldFailWrite) return false
        when (strategy) {
            RestoreStrategy.REPLACE_ALL -> {
                itemsList.clear()
                itemsList.addAll(payload.items)
                sortOrder = payload.sortOrder
                themeMode = payload.themeMode
                theme = payload.backgroundTheme
            }
            RestoreStrategy.MERGE_KEEP_EXISTING -> {
                val existingIds = itemsList.map { it.id }.toMutableSet()
                for (item in payload.items) {
                    if (item.id !in existingIds) {
                        itemsList.add(item)
                        existingIds.add(item.id)
                    }
                }
            }
        }
        return true
    }
}
