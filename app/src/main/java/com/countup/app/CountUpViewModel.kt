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
            is CountUpUiEvent.OpenTargetItem -> {
                val currentItems = _state.value.items
                val target = currentItems.find { it.id == event.itemId }
                if (target != null) {
                    _state.update { it.copy(editorTarget = target, isEditorOpen = true, pendingTargetItemId = null) }
                } else {
                    _state.update { it.copy(pendingTargetItemId = event.itemId) }
                }
            }
            CountUpUiEvent.CloseEditor -> {
                _state.update { it.copy(isEditorOpen = false, editorTarget = null) }
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
                            _state.update {
                                it.copy(
                                    items = updatedItems,
                                    isEditorOpen = false,
                                    editorTarget = null,
                                    isSaving = false,
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
                    performRestore(whisper.itemId, whisper.snapshot, recordId = whisper.recordId) { updatedItems ->
                        if (whisper.recordId != null) {
                            repository.dismissWidgetReset(whisper.recordId)
                        }
                        _state.update {
                            it.copy(
                                items = updatedItems,
                                cardWhispers = it.cardWhispers - event.id,
                                pendingWidgetResets = if (whisper.recordId != null) {
                                    it.pendingWidgetResets.filterNot { r -> r.id == whisper.recordId }
                                } else {
                                    it.pendingWidgetResets
                                },
                            )
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
            CountUpUiEvent.RequestExportBackup -> {
                _state.update { it.copy(isSearchSortMenuOpen = false) }
                val defaultName = "CountUp_Backup_${todayProvider()}.json"
                emitEffect(CountUpUiEffect.TriggerExportDocument(defaultName))
            }
            is CountUpUiEvent.ExportBackupToStream -> {
                viewModelScope.launch(ioDispatcher) {
                    try {
                        val payload = repository.exportBackupPayload()
                        val json = CountUpBackupPayload.encode(payload)
                        event.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_export_success))
                    } catch (_: Exception) {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_export_failed))
                    }
                }
            }
            CountUpUiEvent.RequestImportBackup -> {
                _state.update { it.copy(isSearchSortMenuOpen = false) }
                emitEffect(CountUpUiEffect.TriggerImportDocument())
            }
            is CountUpUiEvent.ImportBackupFromStream -> {
                viewModelScope.launch(ioDispatcher) {
                    try {
                        val maxBytes = 2 * 1024 * 1024 // 2 MB strict limit
                        val buffer = ByteArray(8192)
                        val baos = java.io.ByteArrayOutputStream()
                        var totalRead = 0
                        var exceeded = false
                        event.inputStream.use { stream ->
                            while (true) {
                                val read = stream.read(buffer)
                                if (read == -1) break
                                totalRead += read
                                if (totalRead > maxBytes) {
                                    exceeded = true
                                    break
                                }
                                baos.write(buffer, 0, read)
                            }
                        }
                        if (exceeded) {
                            emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_file_too_large))
                            return@launch
                        }
                        val raw = baos.toString(Charsets.UTF_8.name())
                        when (val result = CountUpBackupPayload.validate(raw)) {
                            is BackupValidationResult.Valid -> {
                                if (result.payload.items.isNotEmpty()) {
                                    _state.update {
                                        it.copy(
                                            pendingRestorePayload = result.payload,
                                            isRestorePayloadDamaged = false,
                                        )
                                    }
                                } else {
                                    emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_invalid_file))
                                }
                            }
                            is BackupValidationResult.Damaged -> {
                                _state.update {
                                    it.copy(
                                        pendingRestorePayload = result.salvagedPayload,
                                        isRestorePayloadDamaged = true,
                                    )
                                }
                            }
                            is BackupValidationResult.UnsupportedSchema -> {
                                emitEffect(
                                    CountUpUiEffect.ShowSnackbar(
                                        R.string.backup_restore_unsupported_version,
                                        formatArg = result.detectedVersion.toString(),
                                    )
                                )
                            }
                            BackupValidationResult.Corrupted -> {
                                emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_invalid_file))
                            }
                        }
                    } catch (_: Exception) {
                        emitEffect(CountUpUiEffect.ShowSnackbar(R.string.backup_restore_failed))
                    }
                }
            }
            is CountUpUiEvent.ConfirmRestore -> {
                val payload = _state.value.pendingRestorePayload
                val isDamaged = _state.value.isRestorePayloadDamaged
                _state.update { it.copy(pendingRestorePayload = null, isRestorePayloadDamaged = false) }
                if (payload != null) {
                    val resolvedStrategy = if (isDamaged && event.strategy == RestoreStrategy.REPLACE_ALL) {
                        RestoreStrategy.MERGE_KEEP_EXISTING
                    } else {
                        event.strategy
                    }
                    viewModelScope.launch(ioDispatcher) {
                        val success = repository.restoreBackupPayload(payload, resolvedStrategy)
                        if (success) {
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
        val widgetWhispers = pendingWidgetResets.associate { record ->
            record.itemId to CardResetWhisper(
                itemId = record.itemId,
                releasedDays = kotlin.math.abs(record.releasedDays),
                snapshot = record.snapshot,
                fromWidget = true,
                recordId = record.id,
            )
        }
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
