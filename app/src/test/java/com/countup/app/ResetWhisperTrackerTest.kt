package com.countup.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ResetWhisperTrackerTest {

    private val fixedToday = LocalDate.of(2026, 8, 29)

    @Test
    fun buildWhisper_createsSnapshotWithCorrectReleasedDays() {
        val item = CountUpItem(
            id = "test-item",
            name = "Yoga",
            epochDay = fixedToday.minusDays(15).toEpochDay(),
            resetCount = 2,
            totalResetDays = 30,
        )

        val whisper = ResetWhisperTracker.buildWhisper(item, fixedToday)
        assertEquals("test-item", whisper.itemId)
        assertEquals(15L, whisper.releasedDays)
        assertFalse(whisper.fromWidget)
        assertEquals(item.epochDay, whisper.snapshot.epochDay)
        assertEquals(2, whisper.snapshot.resetCount)
        assertEquals(30L, whisper.snapshot.totalResetDays)
    }

    @Test
    fun buildWidgetWhispers_mapsWidgetRecordsToWhispers() {
        val records = listOf(
            WidgetResetRecord(
                id = "rec-1",
                itemId = "item-1",
                itemName = "Item 1",
                snapshot = ResetSnapshot(epochDay = fixedToday.minusDays(25).toEpochDay(), resetCount = 1, totalResetDays = 10),
                releasedDays = 25,
                timestampMillis = 1000L,
            ),
            WidgetResetRecord(
                id = "rec-2",
                itemId = "item-2",
                itemName = "Item 2",
                snapshot = ResetSnapshot(epochDay = fixedToday.minusDays(5).toEpochDay(), resetCount = 0, totalResetDays = 0),
                releasedDays = -5,
                timestampMillis = 2000L,
            ),
        )

        val map = ResetWhisperTracker.buildWidgetWhispers(records)
        assertEquals(2, map.size)
        assertTrue(map.containsKey("item-1"))
        assertTrue(map.containsKey("item-2"))

        val w1 = map["item-1"]!!
        assertEquals(25L, w1.releasedDays)
        assertTrue(w1.fromWidget)
        assertEquals("rec-1", w1.recordId)

        val w2 = map["item-2"]!!
        assertEquals(5L, w2.releasedDays)
        assertTrue(w2.fromWidget)
        assertEquals("rec-2", w2.recordId)
    }

    @Test
    fun restoreReset_succeedsWhenItemExistsAndSnapshotValid() = runTest {
        val originalItem = CountUpItem(
            id = "item-1",
            name = "Piano",
            epochDay = fixedToday.minusDays(30).toEpochDay(),
            resetCount = 1,
            totalResetDays = 40,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(originalItem))

        // Reset the item to today
        repo.resetTo("item-1", fixedToday.toEpochDay())

        val snapshot = originalItem.toResetSnapshot()
        val result = ResetWhisperTracker.restoreReset(
            itemId = "item-1",
            snapshot = snapshot,
            recordId = null,
            repository = repo,
            today = fixedToday,
        )

        assertTrue(result is ResetRestoreResult.Success)
        val success = result as ResetRestoreResult.Success
        val restored = success.updatedItems.first { it.id == "item-1" }
        assertEquals(originalItem.epochDay, restored.epochDay)
        assertEquals(originalItem.resetCount, restored.resetCount)
        assertEquals(originalItem.totalResetDays, restored.totalResetDays)
    }

    @Test
    fun restoreReset_returnsAlreadyRestoredOrStale_whenItemWasModifiedSinceReset() = runTest {
        val originalItem = CountUpItem(
            id = "item-1",
            name = "Piano",
            epochDay = fixedToday.minusDays(30).toEpochDay(),
        )
        val repo = FakeCountUpRepository(initialItems = listOf(originalItem))
        // Item epochDay is NOT todayEpochDay, meaning it wasn't reset today or was already edited
        val snapshot = originalItem.toResetSnapshot()

        val result = ResetWhisperTracker.restoreReset(
            itemId = "item-1",
            snapshot = snapshot,
            recordId = null,
            repository = repo,
            today = fixedToday,
        )

        assertEquals(ResetRestoreResult.AlreadyRestoredOrStale, result)
    }

    @Test
    fun restoreReset_returnsFailedWithMissingTrue_whenItemNotFound() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val snapshot = ResetSnapshot(epochDay = fixedToday.toEpochDay(), resetCount = 0, totalResetDays = 0)

        val result = ResetWhisperTracker.restoreReset(
            itemId = "non-existent",
            snapshot = snapshot,
            recordId = "widget-rec-1",
            repository = repo,
            today = fixedToday,
        )

        assertTrue(result is ResetRestoreResult.Failed)
        val failed = result as ResetRestoreResult.Failed
        assertTrue(failed.itemMissing)
    }
}
