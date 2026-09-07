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

    data object RefreshWidget : CountUpUiEffect
}

/**
 * Shared contract constants and navigation utilities for widget-to-app intents.
 */
object WidgetNavigationContract {
    const val EXTRA_TARGET_ITEM_ID = "EXTRA_TARGET_ITEM_ID"

    // Request code partition offsets per widget family
    const val HERO_PENDING_INTENT_OFFSET = 101
    const val ZEN_HORIZON_PENDING_INTENT_OFFSET = 150
    const val SOLAR_RHYTHM_PENDING_INTENT_OFFSET = 202
    const val ZEN_PEBBLE_PENDING_INTENT_OFFSET = 303

    fun createLaunchIntent(context: android.content.Context, targetItemId: String? = null): android.content.Intent {
        return android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetItemId != null) {
                putExtra(EXTRA_TARGET_ITEM_ID, targetItemId)
            }
        }
    }

    fun resolveRequestCode(targetItemId: String?, appWidgetId: Int, offset: Int): Int {
        return if (targetItemId != null) {
            (targetItemId.hashCode() and 0x7FFFFFFF) + offset
        } else {
            appWidgetId
        }
    }
}
