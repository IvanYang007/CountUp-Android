package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BackupCoordinatorTest {

    @Test
    fun exportToStream_writesValidJsonPayload() {
        val repo = FakeCountUpRepository(
            initialItems = listOf(
                CountUpItem(id = "item-1", name = "Reading", epochDay = 20000L),
                CountUpItem(id = "item-2", name = "Running", epochDay = 20050L),
            ),
            initialTheme = BackgroundTheme.DREAM_BOAT,
            initialSortOrder = SortOrder.DAYS_DESC,
        )

        val outputStream = ByteArrayOutputStream()
        val success = BackupCoordinator.exportToStream(outputStream, repo)

        assertTrue(success)
        val json = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("Reading"))
        assertTrue(json.contains("Running"))
        assertTrue(json.contains("\"sortOrder\": \"days_desc\""))
    }

    @Test
    fun exportToStream_returnsFalseOnIOException() {
        val repo = FakeCountUpRepository()
        val brokenStream = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("Disk full or simulated failure")
            }
        }

        val success = BackupCoordinator.exportToStream(brokenStream, repo)
        assertFalse(success)
    }

    @Test
    fun importFromStream_parsesValidPayload() {
        val payload = CountUpBackupPayload(
            items = listOf(CountUpItem(id = "item-a", name = "Meditation", epochDay = 20000L))
        )
        val json = CountUpBackupPayload.encode(payload)
        val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        val result = BackupCoordinator.importFromStream(stream)
        assertTrue(result is BackupImportResult.Valid)
        val valid = result as BackupImportResult.Valid
        assertEquals(1, valid.payload.items.size)
        assertEquals("Meditation", valid.payload.items[0].name)
    }

    @Test
    fun importFromStream_returnsInvalidOrEmpty_whenItemsAreEmpty() {
        val payload = CountUpBackupPayload(items = emptyList())
        val json = CountUpBackupPayload.encode(payload)
        val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        val result = BackupCoordinator.importFromStream(stream)
        assertEquals(BackupImportResult.InvalidOrEmpty, result)
    }

    @Test
    fun importFromStream_returnsCorrupted_forGarbage() {
        val stream = ByteArrayInputStream("clearly not json at all".toByteArray(Charsets.UTF_8))

        val result = BackupCoordinator.importFromStream(stream)
        assertEquals(BackupImportResult.Corrupted, result)
    }

    @Test
    fun importFromStream_returnsUnsupportedSchema_whenVersionHigherThanCurrent() {
        val futureJson = """
            {
              "schemaVersion": 99,
              "appVersion": "9.9.9",
              "itemsJson": "[{\"id\":\"i1\",\"name\":\"Future Item\",\"epochDay\":25000}]"
            }
        """.trimIndent()
        val stream = ByteArrayInputStream(futureJson.toByteArray(Charsets.UTF_8))

        val result = BackupCoordinator.importFromStream(stream)
        assertTrue(result is BackupImportResult.UnsupportedSchema)
        val unsupported = result as BackupImportResult.UnsupportedSchema
        assertEquals(99, unsupported.detectedVersion)
    }

    @Test
    fun importFromStream_returnsFileTooLarge_whenExceedingBudget() {
        val oversizedBytes = ByteArray(BackupCoordinator.MAX_BACKUP_BYTES + 1024) { 'A'.code.toByte() }
        val stream = ByteArrayInputStream(oversizedBytes)

        val result = BackupCoordinator.importFromStream(stream)
        assertEquals(BackupImportResult.FileTooLarge, result)
    }

    @Test
    fun importFromStream_returnsDamaged_whenItemsTruncatedButSalvageable() {
        val truncatedJson = """
            {
              "schemaVersion": 1,
              "itemsJson": "[{\"id\":\"item-1\",\"name\":\"Meditation\",\"epochDay\":20000},{\"id\":\"item-2\",\"name\":\"Guitar"
        """.trimIndent()
        val stream = ByteArrayInputStream(truncatedJson.toByteArray(Charsets.UTF_8))

        val result = BackupCoordinator.importFromStream(stream)
        assertTrue(result is BackupImportResult.Damaged)
        val damaged = result as BackupImportResult.Damaged
        assertEquals(1, damaged.salvagedPayload.items.size)
        assertEquals("Meditation", damaged.salvagedPayload.items[0].name)
    }

    @Test
    fun importFromStream_returnsFailed_whenStreamThrowsException() {
        val brokenStream = object : InputStream() {
            override fun read(): Int = throw IOException("Read error")
        }

        val result = BackupCoordinator.importFromStream(brokenStream)
        assertEquals(BackupImportResult.Failed, result)
    }

    @Test
    fun resolveRestoreStrategy_downgradesReplaceAll_whenDamaged() {
        assertEquals(
            RestoreStrategy.MERGE_KEEP_EXISTING,
            BackupCoordinator.resolveRestoreStrategy(RestoreStrategy.REPLACE_ALL, isDamaged = true)
        )
        assertEquals(
            RestoreStrategy.MERGE_KEEP_EXISTING,
            BackupCoordinator.resolveRestoreStrategy(RestoreStrategy.MERGE_KEEP_EXISTING, isDamaged = true)
        )
        assertEquals(
            RestoreStrategy.REPLACE_ALL,
            BackupCoordinator.resolveRestoreStrategy(RestoreStrategy.REPLACE_ALL, isDamaged = false)
        )
        assertEquals(
            RestoreStrategy.MERGE_KEEP_EXISTING,
            BackupCoordinator.resolveRestoreStrategy(RestoreStrategy.MERGE_KEEP_EXISTING, isDamaged = false)
        )
    }

    @Test
    fun exportToStream_returnsFalseWhenPayloadExceedsMaxBytes() {
        val hugeComment = "x".repeat(BackupCoordinator.MAX_BACKUP_BYTES + 100)
        val repo = FakeCountUpRepository(
            initialItems = listOf(
                CountUpItem(id = "item-huge", name = "Big Item", epochDay = 20000L, comment = hugeComment)
            )
        )
        val outputStream = ByteArrayOutputStream()
        val success = BackupCoordinator.exportToStream(outputStream, repo)
        assertFalse(success)
        assertEquals(0, outputStream.size())
    }
}
