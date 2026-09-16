package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VoiceAddFlowTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private lateinit var store: CountUpStore

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("voice_flow_test").toFile()
        testContext = TestContext(tempDir)
        store = CountUpStore(testContext)
    }

    @Test
    fun voiceAddSavesSanitizedItemWithExplicitSessionId() {
        val rawCandidates = listOf("   ", "\"Practice Guitar\"   ", "Practice guitar")
        val sanitized = VoiceTextSanitizer.sanitize(rawCandidates)
        assertNotNull(sanitized)
        assertEquals("Practice Guitar", sanitized)

        val sessionId = "voice-session-uuid-999"
        val anchorDate = 20100L

        val item = store.addItem(
            name = sanitized!!,
            epochDay = anchorDate,
            id = sessionId
        )
        assertNotNull(item)
        assertEquals(sessionId, item!!.id)
        assertEquals("Practice Guitar", item.name)
        assertEquals(anchorDate, item.epochDay)

        // Retrying the same session returns existing without duplicates
        val retry = store.addItem(
            name = sanitized,
            epochDay = anchorDate,
            id = sessionId
        )
        assertEquals(1, store.items().size)
        assertEquals(item, retry)
    }

    @Test
    fun undoOperationRemovesItemFromStore() {
        val sessionId = "voice-session-uuid-undo"
        val item = store.addItem(
            name = "Cold Shower",
            epochDay = 20100L,
            id = sessionId
        )
        assertNotNull(item)
        assertEquals(1, store.items().size)

        // Undo action deletes the specific created item
        val deleted = store.deleteItem(item!!.id)
        assertTrue(deleted)
        assertTrue(store.items().isEmpty())
    }

    @Test
    fun emptyOrWhitespaceOnlySpeechCreatesNothing() {
        val rawCandidates = listOf("  ", "\t\n", "")
        val sanitized = VoiceTextSanitizer.sanitize(rawCandidates)
        assertNull(sanitized)

        // Verifies no item was created
        assertTrue(store.items().isEmpty())
    }
}
