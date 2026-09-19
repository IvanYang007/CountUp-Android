package com.countup.app

import java.time.LocalDate

/**
 * Outcome of attempting to restore an item from a reset whisper snapshot.
 */
sealed interface ResetRestoreResult {
    data class Success(val updatedItems: List<CountUpItem>) : ResetRestoreResult
    data object AlreadyRestoredOrStale : ResetRestoreResult
    data class Failed(val itemMissing: Boolean) : ResetRestoreResult
}

/**
 * Domain tracker managing in-memory reset whispers, widget reset conversions,
 * and snapshot restoration logic.
 */
object ResetWhisperTracker {

    const val WHISPER_DISMISS_DELAY_MS: Long = 5000L

    /**
     * Builds a [CardResetWhisper] snapshot for an item reset in the active app session.
     */
    fun buildWhisper(item: CountUpItem, today: LocalDate): CardResetWhisper {
        val releasedDays = daysSince(LocalDate.ofEpochDay(item.epochDay), today)
        return CardResetWhisper(
            itemId = item.id,
            releasedDays = kotlin.math.abs(releasedDays),
            snapshot = item.toResetSnapshot(),
        )
    }

    /**
     * Converts pending widget reset records into interactive [CardResetWhisper] entries.
     */
    fun buildWidgetWhispers(records: List<WidgetResetRecord>): Map<String, CardResetWhisper> {
        return records.associate { record ->
            record.itemId to CardResetWhisper(
                itemId = record.itemId,
                releasedDays = kotlin.math.abs(record.releasedDays),
                snapshot = record.snapshot,
                fromWidget = true,
                recordId = record.id,
            )
        }
    }

    /**
     * Validates and performs snapshot restoration against [repository].
     */
    suspend fun restoreReset(
        itemId: String,
        snapshot: ResetSnapshot,
        recordId: String?,
        repository: CountUpRepository,
        today: LocalDate,
    ): ResetRestoreResult {
        val currentItems = repository.getItems()
        val currentItem = currentItems.firstOrNull { it.id == itemId }
        if (currentItem == null) {
            return ResetRestoreResult.Failed(itemMissing = true)
        }

        val todayEpochDay = today.toEpochDay()
        if (currentItem.resetCount != snapshot.resetCount + 1 || (recordId == null && currentItem.epochDay != todayEpochDay)) {
            return ResetRestoreResult.AlreadyRestoredOrStale
        }

        return if (repository.restoreReset(itemId, snapshot)) {
            ResetRestoreResult.Success(repository.getItems())
        } else {
            val exists = repository.getItems().any { it.id == itemId }
            ResetRestoreResult.Failed(itemMissing = !exists)
        }
    }
}
