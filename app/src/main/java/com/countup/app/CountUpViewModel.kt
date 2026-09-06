package com.countup.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel managing the MVI lifecycle, unidirectional data flow, and business logic
 * for the CountUp main screen.
 *
 * Rules:
 * 1. State mutations strictly through atomic `_state.update { it.copy(...) }`.
 * 2. Repository write operations offloaded to background IO dispatcher.
 * 3. Side effects (Snackbars, Widget Refresh IPC) emitted via buffered Channel.
 * 4. Repository operations isolated and testable with fake repositories.
 */
class CountUpViewModel(
    private val repository: CountUpRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(CountUpUiState(isLoading = true, today = todayProvider()))
    val state: StateFlow<CountUpUiState> = _state.asStateFlow()

    private val _effects = Channel<CountUpUiEffect>(Channel.BUFFERED)
    val effects: Flow<CountUpUiEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            refreshState()
        }
    }

    fun onEvent(event: CountUpUiEvent) {
        when (event) {
            is CountUpUiEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            CountUpUiEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
            }
            is CountUpUiEvent.SortOrderSelected -> {
                viewModelScope.launch(ioDispatcher) {
                    repository.setSortOrder(event.order)
                    _state.update { it.copy(sortOrder = event.order, isSearchSortMenuOpen = false) }
                    emitEffect(CountUpUiEffect.RefreshWidget)
                }
            }
            is CountUpUiEvent.ThemeModeSelected -> {
                viewModelScope.launch(ioDispatcher) {
                    repository.setThemeMode(event.mode)
                    _state.update { it.copy(themeMode = event.mode, isSearchSortMenuOpen = false) }
                    emitEffect(
                        CountUpUiEffect.ShowSnackbar(
                            messageRes = R.string.theme_switched_toast,
                            formatArgRes = event.mode.labelRes,
                        )
                    )
                    emitEffect(CountUpUiEffect.RefreshWidget)
                }
            }
            CountUpUiEvent.CycleBackground -> {
                val next = _state.value.backgroundTheme.next()
                viewModelScope.launch(ioDispatcher) {
                    repository.setBackgroundTheme(next)
                    _state.update { it.copy(backgroundTheme = next) }
                    emitEffect(
                        CountUpUiEffect.ShowSnackbar(
                            messageRes = R.string.bg_switched_toast,
                            formatArgRes = next.labelRes,
                        )
                    )
                    emitEffect(CountUpUiEffect.RefreshWidget)
                }
            }
            is CountUpUiEvent.OpenEditor -> {
                _state.update { it.copy(editorTarget = event.target, isEditorOpen = true) }
            }
            CountUpUiEvent.CloseEditor -> {
                _state.update { it.copy(isEditorOpen = false, editorTarget = null) }
            }
            is CountUpUiEvent.SaveItem -> {
                val target = _state.value.editorTarget
                viewModelScope.launch(ioDispatcher) {
                    val success = if (target == null) {
                        repository.addItem(
                            name = event.name,
                            epochDay = event.epochDay,
                            comment = event.comment,
                            icon = event.icon,
                            cardColor = event.cardColor,
                            isPinned = event.isPinned,
                        ) != null
                    } else {
                        repository.updateItem(
                            id = target.id,
                            name = event.name,
                            epochDay = event.epochDay,
                            comment = event.comment,
                            icon = event.icon,
                            cardColor = event.cardColor,
                            isPinned = event.isPinned,
                        )
                    }
                    if (success) {
                        val updatedItems = repository.getItems()
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                isEditorOpen = false,
                                editorTarget = null,
                            )
                        }
                        emitEffect(CountUpUiEffect.RefreshWidget)
                    } else {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                    }
                }
            }
            is CountUpUiEvent.RequestDelete -> {
                _state.update { it.copy(pendingDelete = event.target) }
            }
            CountUpUiEvent.DismissDelete -> {
                _state.update { it.copy(pendingDelete = null) }
            }
            is CountUpUiEvent.ConfirmDelete -> {
                viewModelScope.launch(ioDispatcher) {
                    if (repository.deleteItem(event.id)) {
                        val updatedItems = repository.getItems()
                        val pending = repository.getPendingWidgetResets()
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                pendingDelete = null,
                                pendingWidgetResets = pending,
                                cardWhispers = it.cardWhispers - event.id,
                            )
                        }
                        emitEffect(CountUpUiEffect.RefreshWidget)
                    } else {
                        _state.update { it.copy(pendingDelete = null) }
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                    }
                }
            }
            is CountUpUiEvent.ConfirmReset -> {
                val targetItem = _state.value.items.firstOrNull { it.id == event.id } ?: return
                val todayLocalDate = todayProvider()
                val todayEpochDay = todayLocalDate.toEpochDay()
                val releasedDays = daysSince(LocalDate.ofEpochDay(targetItem.epochDay), todayLocalDate)
                // Stop trigger reset when the accumulate date is already 0
                if (!targetItem.isResettableOn(todayLocalDate)) {
                    return
                }

                viewModelScope.launch(ioDispatcher) {
                    if (repository.resetTo(event.id, todayEpochDay)) {
                        val updatedItems = repository.getItems()
                        val whisper = CardResetWhisper(
                            itemId = targetItem.id,
                            releasedDays = kotlin.math.abs(releasedDays),
                            snapshot = targetItem.toResetSnapshot(),
                        )
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                cardWhispers = it.cardWhispers + (event.id to whisper),
                            )
                        }
                        emitEffect(CountUpUiEffect.RefreshWidget)

                        // Whisper line auto-dismiss after 5,000 ms
                        launch {
                            kotlinx.coroutines.delay(5000L)
                            _state.update { current ->
                                if (current.cardWhispers[event.id] == whisper) {
                                    current.copy(cardWhispers = current.cardWhispers - event.id)
                                } else {
                                    current
                                }
                            }
                        }
                    } else {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                    }
                }
            }
            is CountUpUiEvent.UndoReset -> {
                val whisper = _state.value.cardWhispers[event.id] ?: return
                viewModelScope.launch(ioDispatcher) {
                    performRestore(whisper.itemId, whisper.snapshot) { updatedItems ->
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                cardWhispers = it.cardWhispers - event.id,
                            )
                        }
                    }
                }
            }
            is CountUpUiEvent.RestoreWidgetReset -> {
                val record = event.record
                viewModelScope.launch(ioDispatcher) {
                    performRestore(record.itemId, record.snapshot, recordId = record.id) { updatedItems ->
                        repository.dismissWidgetReset(record.id)
                        val pending = repository.getPendingWidgetResets()
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                pendingWidgetResets = pending,
                            )
                        }
                    }
                }
            }
            is CountUpUiEvent.DismissWidgetReset -> {
                viewModelScope.launch(ioDispatcher) {
                    repository.dismissWidgetReset(event.recordId)
                    val pending = repository.getPendingWidgetResets()
                    _state.update { it.copy(pendingWidgetResets = pending) }
                }
            }
            is CountUpUiEvent.ToggleWidgetVisibility -> {
                val target = _state.value.items.firstOrNull { it.id == event.id }
                if (target != null) {
                    val newVisibility = !target.showInWidget
                    viewModelScope.launch(ioDispatcher) {
                        if (repository.setWidgetVisibility(event.id, newVisibility)) {
                            val updatedItems = repository.getItems()
                            _state.update { it.copy(items = updatedItems) }
                            val res = if (newVisibility) R.string.toast_shown_in_widget else R.string.toast_hidden_from_widget
                            emitEffect(CountUpUiEffect.ShowSnackbar(res, target.name))
                            emitEffect(CountUpUiEffect.RefreshWidget)
                        } else {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                        }
                    }
                }
            }
            is CountUpUiEvent.SetSearchSortMenuOpen -> {
                _state.update { it.copy(isSearchSortMenuOpen = event.open) }
            }
            CountUpUiEvent.Refresh -> {
                viewModelScope.launch(ioDispatcher) {
                    refreshState()
                }
            }
            CountUpUiEvent.CheckMidnight -> {
                val currentToday = todayProvider()
                if (currentToday != _state.value.today) {
                    viewModelScope.launch(ioDispatcher) {
                        refreshState()
                        emitEffect(CountUpUiEffect.RefreshWidget)
                    }
                }
            }
        }
    }

    private fun refreshState() {
        val today = todayProvider()
        val items = repository.getItems()
        val sortOrder = repository.getSortOrder()
        val backgroundTheme = repository.getBackgroundTheme()
        val themeMode = repository.getThemeMode()
        val pendingWidgetResets = repository.getPendingWidgetResets()
        _state.update {
            it.copy(
                items = items,
                isLoading = false,
                sortOrder = sortOrder,
                backgroundTheme = backgroundTheme,
                themeMode = themeMode,
                today = today,
                pendingWidgetResets = pendingWidgetResets,
            )
        }
    }

    private fun emitEffect(effect: CountUpUiEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private suspend fun performRestore(
        itemId: String,
        snapshot: ResetSnapshot,
        recordId: String? = null,
        onSuccess: suspend (List<CountUpItem>) -> Unit,
    ) {
        val currentItems = repository.getItems()
        val currentItem = currentItems.firstOrNull { it.id == itemId }
        if (currentItem == null) {
            if (recordId != null) {
                repository.dismissWidgetReset(recordId)
                val pending = repository.getPendingWidgetResets()
                _state.update { it.copy(pendingWidgetResets = pending) }
            }
            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
            return
        }

        val todayEpochDay = todayProvider().toEpochDay()
        if (recordId == null && currentItem.epochDay != todayEpochDay) {
            _state.update { it.copy(cardWhispers = it.cardWhispers - itemId) }
            return
        }

        if (repository.restoreReset(itemId, snapshot)) {
            val updatedItems = repository.getItems()
            onSuccess(updatedItems)
            emitEffect(CountUpUiEffect.RefreshWidget)
        } else {
            val exists = repository.getItems().any { it.id == itemId }
            if (!exists && recordId != null) {
                repository.dismissWidgetReset(recordId)
                val pending = repository.getPendingWidgetResets()
                _state.update { it.copy(pendingWidgetResets = pending) }
            }
            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
        }
    }
}
