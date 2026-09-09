package com.countup.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Active in-card undo whisper for a recently reset item.
 */
@Immutable
data class CardResetWhisper(
    val itemId: String,
    val releasedDays: Long,
    val snapshot: ResetSnapshot,
)

/**
 * Single immutable state snapshot for the CountUp main screen.
 * Follows 2026 MVI / Unidirectional Data Flow guidelines with @Immutable for Compose skipping.
 */
@Immutable
data class CountUpUiState(
    val items: List<CountUpItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DAYS_DESC,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.AUTO_DAILY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val editorTarget: CountUpItem? = null,
    val isEditorOpen: Boolean = false,
    val isSearchSortMenuOpen: Boolean = false,
    val pendingDelete: CountUpItem? = null,
    val today: LocalDate = LocalDate.now(),
    val cardWhispers: Map<String, CardResetWhisper> = emptyMap(),
    val pendingWidgetResets: List<WidgetResetRecord> = emptyList(),
    val pendingTargetItemId: String? = null,
    val pendingRestorePayload: CountUpBackupPayload? = null,
    val isRestorePayloadDamaged: Boolean = false,
    val isSaving: Boolean = false,
) {
    /**
     * Instant derived filtered & sorted list of items matching [searchQuery] in [sortOrder].
     */
    val displayItems: List<CountUpItem> by lazy {
        queryAndSortItems(items, searchQuery, sortOrder, today)
    }
}

/**
 * Sealed contract of all user interactions / intents dispatched from the UI.
 */
sealed interface CountUpUiEvent {
    data class SearchQueryChanged(val query: String) : CountUpUiEvent
    data object ClearSearch : CountUpUiEvent
    data class SortOrderSelected(val order: SortOrder) : CountUpUiEvent
    data class ThemeModeSelected(val mode: ThemeMode) : CountUpUiEvent
    data object CycleBackground : CountUpUiEvent
    data class OpenEditor(val target: CountUpItem? = null) : CountUpUiEvent
    data class OpenTargetItem(val itemId: String) : CountUpUiEvent
    data object CloseEditor : CountUpUiEvent
    data class SaveItem(
        val draft: ItemDraft,
    ) : CountUpUiEvent {
        constructor(
            name: String,
            epochDay: Long,
            comment: String = "",
            icon: String = "",
            cardColor: String = "",
            isPinned: Boolean = false,
        ) : this(
            ItemDraft(
                name = name,
                epochDay = epochDay,
                comment = comment,
                icon = icon,
                cardColor = cardColor,
                isPinned = isPinned,
            )
        )

        val name: String get() = draft.name
        val epochDay: Long get() = draft.epochDay
        val comment: String get() = draft.comment
        val icon: String get() = draft.icon
        val cardColor: String get() = draft.cardColor
        val isPinned: Boolean get() = draft.isPinned
    }
    data class RequestDelete(val target: CountUpItem) : CountUpUiEvent
    data object DismissDelete : CountUpUiEvent
    data class ConfirmDelete(val id: String) : CountUpUiEvent
    data class ConfirmReset(val id: String) : CountUpUiEvent
    data class UndoReset(val id: String) : CountUpUiEvent
    data class RestoreWidgetReset(val record: WidgetResetRecord) : CountUpUiEvent
    data class DismissWidgetReset(val recordId: String) : CountUpUiEvent
    data class ToggleWidgetVisibility(val id: String) : CountUpUiEvent
    data class SetSearchSortMenuOpen(val open: Boolean) : CountUpUiEvent
    data object RequestExportBackup : CountUpUiEvent
    data class ExportBackupToStream(val outputStream: java.io.OutputStream) : CountUpUiEvent
    data object RequestImportBackup : CountUpUiEvent
    data class ImportBackupFromStream(val inputStream: java.io.InputStream) : CountUpUiEvent
    data class ConfirmRestore(val strategy: RestoreStrategy) : CountUpUiEvent
    data object DismissRestorePreview : CountUpUiEvent
    data object Refresh : CountUpUiEvent
    data object CheckMidnight : CountUpUiEvent
}

/**
 * One-shot UI side effects (snackbars, IPC notifications) dispatched from ViewModel to UI.
 */
sealed interface CountUpUiEffect {
    data class ShowSnackbar(
        @get:StringRes val messageRes: Int,
        val formatArg: String? = null,
        @get:StringRes val formatArgRes: Int? = null,
    ) : CountUpUiEffect

    data class TriggerExportDocument(val defaultFilename: String) : CountUpUiEffect

    data class TriggerImportDocument(
        val mimeTypes: Array<String> = arrayOf("application/json", "text/plain", "text/*", "*/*")
    ) : CountUpUiEffect {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TriggerImportDocument
            return mimeTypes.contentEquals(other.mimeTypes)
        }

        override fun hashCode(): Int = mimeTypes.contentHashCode()
    }

    data object RefreshWidget : CountUpUiEffect
}
