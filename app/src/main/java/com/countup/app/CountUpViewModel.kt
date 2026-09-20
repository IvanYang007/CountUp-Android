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

    private val isSaving = java.util.concurrent.atomic.AtomicBoolean(false)

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
            CountUpUiEvent.CycleThemeMode -> {
                val next = _state.value.themeMode.next()
                viewModelScope.launch(ioDispatcher) {
                    repository.setThemeMode(next)
                    _state.update { it.copy(themeMode = next) }
                    emitEffect(
                        CountUpUiEffect.ShowSnackbar(
                            messageRes = R.string.theme_switched_toast,
                            formatArgRes = next.labelRes,
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
                _state.update {
                    it.copy(
                        editorTarget = event.target,
                        editorInitialCategory = event.initialCategory,
                        isEditorOpen = true,
                    )
                }
            }
            is CountUpUiEvent.OpenTargetItem -> {
                val currentItems = _state.value.items
                val target = currentItems.find { it.id == event.itemId }
                if (target != null) {
                    _state.update { it.copy(editorTarget = target, editorInitialCategory = null, isEditorOpen = true, pendingTargetItemId = null) }
                } else {
                    _state.update { it.copy(pendingTargetItemId = event.itemId) }
                }
            }
            CountUpUiEvent.CloseEditor -> {
                _state.update { it.copy(isEditorOpen = false, editorTarget = null, editorInitialCategory = null) }
            }
            is CountUpUiEvent.SaveItem -> {
                if (!isSaving.compareAndSet(false, true)) return
                _state.update { it.copy(isSaving = true) }
                val target = _state.value.editorTarget
                viewModelScope.launch(ioDispatcher) {
                    try {
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
                            if (target != null) {
                                repository.getPendingWidgetResets().filter { it.itemId == target.id }.forEach {
                                    repository.dismissWidgetReset(it.id)
                                }
                            }
                            val remainingResets = repository.getPendingWidgetResets()
                            _state.update {
                                it.copy(
                                    items = updatedItems,
                                    isEditorOpen = false,
                                    editorTarget = null,
                                    editorInitialCategory = null,
                                    isSaving = false,
                                    cardWhispers = if (target != null) it.cardWhispers - target.id else it.cardWhispers,
                                    pendingWidgetResets = remainingResets,
                                )
                            }
                            emitEffect(CountUpUiEffect.RefreshWidget)
                        } else {
                            _state.update { it.copy(isSaving = false) }
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                        }
                    } finally {
                        isSaving.set(false)
                        _state.update { it.copy(isSaving = false) }
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
                // Stop trigger reset when the accumulate date is already 0
                if (!targetItem.isResettableOn(todayLocalDate)) {
                    return
                }

                viewModelScope.launch(ioDispatcher) {
                    if (repository.resetTo(event.id, todayEpochDay)) {
                        val updatedItems = repository.getItems()
                        val whisper = ResetWhisperTracker.buildWhisper(targetItem, todayLocalDate)
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                cardWhispers = it.cardWhispers + (event.id to whisper),
                            )
                        }
                        emitEffect(CountUpUiEffect.RefreshWidget)

                        // Whisper line auto-dismiss after 5,000 ms
                        launch {
                            kotlinx.coroutines.delay(ResetWhisperTracker.WHISPER_DISMISS_DELAY_MS)
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
                    when (val result = ResetWhisperTracker.restoreReset(
                        itemId = whisper.itemId,
                        snapshot = whisper.snapshot,
                        recordId = whisper.recordId,
                        repository = repository,
                        today = todayProvider(),
                    )) {
                        is ResetRestoreResult.Success -> {
                            if (whisper.recordId != null) {
                                repository.dismissWidgetReset(whisper.recordId)
                            }
                            _state.update {
                                it.copy(
                                    items = result.updatedItems,
                                    cardWhispers = it.cardWhispers - event.id,
                                    pendingWidgetResets = if (whisper.recordId != null) {
                                        it.pendingWidgetResets.filterNot { r -> r.id == whisper.recordId }
                                    } else {
                                        it.pendingWidgetResets
                                    },
                                )
                            }
                            emitEffect(CountUpUiEffect.RefreshWidget)
                        }
                        ResetRestoreResult.AlreadyRestoredOrStale -> {
                            _state.update { it.copy(cardWhispers = it.cardWhispers - event.id) }
                        }
                        is ResetRestoreResult.Failed -> {
                            if (result.itemMissing && whisper.recordId != null) {
                                repository.dismissWidgetReset(whisper.recordId)
                                val pending = repository.getPendingWidgetResets()
                                _state.update { it.copy(pendingWidgetResets = pending) }
                            }
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.error_save_failed))
                        }
                    }
                }
            }
            is CountUpUiEvent.DismissCardWhisper -> {
                val whisper = _state.value.cardWhispers[event.itemId]
                _state.update {
                    it.copy(
                        cardWhispers = it.cardWhispers - event.itemId,
                        pendingWidgetResets = if (whisper?.recordId != null) {
                            it.pendingWidgetResets.filterNot { r -> r.id == whisper.recordId }
                        } else {
                            it.pendingWidgetResets
                        },
                    )
                }
                if (whisper?.recordId != null) {
                    viewModelScope.launch(ioDispatcher) {
                        repository.dismissWidgetReset(whisper.recordId)
                    }
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
            is CountUpUiEvent.SetSettingsDialogOpen -> {
                _state.update { it.copy(isSettingsDialogOpen = event.open) }
            }
            CountUpUiEvent.RequestExportBackup -> {
                _state.update { it.copy(isSearchSortMenuOpen = false, isSettingsDialogOpen = false) }
                val defaultName = "CountUp_Backup_${todayProvider()}.json"
                emitEffect(CountUpUiEffect.TriggerExportDocument(defaultName))
            }
            is CountUpUiEvent.ExportBackupToStream -> {
                viewModelScope.launch(ioDispatcher) {
                    if (BackupCoordinator.exportToStream(event.outputStream, repository)) {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_export_success))
                    } else {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_export_failed))
                    }
                }
            }
            CountUpUiEvent.RequestImportBackup -> {
                _state.update { it.copy(isSearchSortMenuOpen = false, isSettingsDialogOpen = false) }
                emitEffect(CountUpUiEffect.TriggerImportDocument())
            }
            is CountUpUiEvent.ImportBackupFromStream -> {
                viewModelScope.launch(ioDispatcher) {
                    when (val result = BackupCoordinator.importFromStream(event.inputStream)) {
                        is BackupImportResult.Valid -> {
                            _state.update {
                                it.copy(
                                    pendingRestorePayload = result.payload,
                                    isRestorePayloadDamaged = false,
                                )
                            }
                        }
                        is BackupImportResult.Damaged -> {
                            _state.update {
                                it.copy(
                                    pendingRestorePayload = result.salvagedPayload,
                                    isRestorePayloadDamaged = true,
                                )
                            }
                        }
                        is BackupImportResult.UnsupportedSchema -> {
                            emitEffect(
                                CountUpUiEffect.ShowSnackbar(
                                    R.string.backup_restore_unsupported_version,
                                    formatArg = result.detectedVersion.toString(),
                                )
                            )
                        }
                        BackupImportResult.FileTooLarge -> {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_file_too_large))
                        }
                        BackupImportResult.InvalidOrEmpty,
                        BackupImportResult.Corrupted -> {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_invalid_file))
                        }
                        BackupImportResult.Failed -> {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_failed))
                        }
                    }
                }
            }
            is CountUpUiEvent.ConfirmRestore -> {
                val payload = _state.value.pendingRestorePayload
                val isDamaged = _state.value.isRestorePayloadDamaged
                _state.update { it.copy(pendingRestorePayload = null, isRestorePayloadDamaged = false) }
                if (payload != null) {
                    val strategy = BackupCoordinator.resolveRestoreStrategy(event.strategy, isDamaged)
                    viewModelScope.launch(ioDispatcher) {
                        if (repository.restoreBackupPayload(payload, strategy)) {
                            refreshState()
                            emitEffect(CountUpUiEffect.RefreshWidget)
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_success))
                        } else {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_failed))
                        }
                    }
                }
            }
            CountUpUiEvent.DismissRestorePreview -> {
                _state.update { it.copy(pendingRestorePayload = null, isRestorePayloadDamaged = false) }
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
        val widgetWhispers = ResetWhisperTracker.buildWidgetWhispers(pendingWidgetResets)
        // Mark as acknowledged in persistent storage so next launch is clean (Active Session pattern)
        if (pendingWidgetResets.isNotEmpty()) {
            for (record in pendingWidgetResets) {
                repository.dismissWidgetReset(record.id)
            }
        }
        _state.update { current ->
            val pendingId = current.pendingTargetItemId
            val target = if (pendingId != null) items.find { it.id == pendingId } else null
            current.copy(
                items = items,
                isLoading = false,
                sortOrder = sortOrder,
                backgroundTheme = backgroundTheme,
                themeMode = themeMode,
                today = today,
                cardWhispers = widgetWhispers + current.cardWhispers,
                pendingWidgetResets = if (pendingWidgetResets.isNotEmpty()) pendingWidgetResets else current.pendingWidgetResets,
                editorTarget = target ?: current.editorTarget,
                isEditorOpen = if (target != null) true else current.isEditorOpen,
                pendingTargetItemId = if (target != null) null else pendingId,
            )
        }
    }

    private fun emitEffect(effect: CountUpUiEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
