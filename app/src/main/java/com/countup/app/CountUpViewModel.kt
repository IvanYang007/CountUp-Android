package com.countup.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * 2. Side effects (Snackbars, Widget Refresh IPC) emitted via buffered Channel.
 * 3. Repository operations isolated and testable with fake repositories.
 */
class CountUpViewModel(
    private val repository: CountUpRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _state = MutableStateFlow(CountUpUiState(today = todayProvider()))
    val state: StateFlow<CountUpUiState> = _state.asStateFlow()

    private val _effects = Channel<CountUpUiEffect>(Channel.BUFFERED)
    val effects: Flow<CountUpUiEffect> = _effects.receiveAsFlow()

    init {
        refreshState()
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
                repository.setSortOrder(event.order)
                _state.update { it.copy(sortOrder = event.order, isSearchSortMenuOpen = false) }
                emitEffect(CountUpUiEffect.RefreshWidget)
            }
            CountUpUiEvent.CycleBackground -> {
                val next = _state.value.backgroundTheme.next()
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
            is CountUpUiEvent.OpenEditor -> {
                _state.update { it.copy(editorTarget = event.target, isEditorOpen = true) }
            }
            CountUpUiEvent.CloseEditor -> {
                _state.update { it.copy(isEditorOpen = false, editorTarget = null) }
            }
            is CountUpUiEvent.SaveItem -> {
                val target = _state.value.editorTarget
                val success = if (target == null) {
                    repository.addItem(
                        name = event.name,
                        epochDay = event.epochDay,
                        comment = event.comment,
                        icon = event.icon,
                        cardColor = event.cardColor,
                    ) != null
                } else {
                    repository.updateItem(
                        id = target.id,
                        name = event.name,
                        epochDay = event.epochDay,
                        comment = event.comment,
                        icon = event.icon,
                        cardColor = event.cardColor,
                    )
                }
                if (success) {
                    _state.update {
                        it.copy(
                            items = repository.getItems(),
                            isEditorOpen = false,
                            editorTarget = null,
                        )
                    }
                    emitEffect(CountUpUiEffect.RefreshWidget)
                } else {
                    emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                }
            }
            is CountUpUiEvent.RequestDelete -> {
                _state.update { it.copy(pendingDelete = event.target) }
            }
            CountUpUiEvent.DismissDelete -> {
                _state.update { it.copy(pendingDelete = null) }
            }
            is CountUpUiEvent.ConfirmDelete -> {
                if (repository.deleteItem(event.id)) {
                    _state.update {
                        it.copy(
                            items = repository.getItems(),
                            pendingDelete = null,
                        )
                    }
                    emitEffect(CountUpUiEffect.RefreshWidget)
                } else {
                    _state.update { it.copy(pendingDelete = null) }
                    emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                }
            }
            is CountUpUiEvent.ConfirmReset -> {
                val targetItem = _state.value.items.firstOrNull { it.id == event.id }
                if (repository.resetTo(event.id, todayProvider().toEpochDay())) {
                    _state.update {
                        it.copy(
                            items = repository.getItems(),
                        )
                    }
                    if (targetItem != null) {
                        emitEffect(
                            CountUpUiEffect.ShowSnackbar(
                                messageRes = R.string.widget_reset_toast,
                                formatArg = targetItem.name,
                            )
                        )
                    }
                    emitEffect(CountUpUiEffect.RefreshWidget)
                } else {
                    emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                }
            }
            is CountUpUiEvent.ToggleWidgetVisibility -> {
                val target = _state.value.items.firstOrNull { it.id == event.id }
                if (target != null) {
                    val newVisibility = !target.showInWidget
                    if (repository.setWidgetVisibility(event.id, newVisibility)) {
                        _state.update { it.copy(items = repository.getItems()) }
                        val res = if (newVisibility) R.string.toast_shown_in_widget else R.string.toast_hidden_from_widget
                        emitEffect(CountUpUiEffect.ShowSnackbar(res, target.name))
                        emitEffect(CountUpUiEffect.RefreshWidget)
                    } else {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                    }
                }
            }
            is CountUpUiEvent.SetSearchSortMenuOpen -> {
                _state.update { it.copy(isSearchSortMenuOpen = event.open) }
            }
            CountUpUiEvent.Refresh -> {
                refreshState()
            }
        }
    }

    private fun refreshState() {
        val today = todayProvider()
        _state.update {
            it.copy(
                items = repository.getItems(),
                sortOrder = repository.getSortOrder(),
                backgroundTheme = repository.getBackgroundTheme(),
                today = today,
            )
        }
    }

    private fun emitEffect(effect: CountUpUiEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
