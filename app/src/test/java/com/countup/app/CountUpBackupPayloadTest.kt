package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CountUpBackupPayloadTest {

    @Test
    fun encodeAndDecodeRoundTripPreservesAllFields() {
        val items = listOf(
            CountUpItem(
                id = "item-1",
                name = "Meditation",
                epochDay = 20000,
                comment = "Morning silence",
                icon = "lotus",
                cardColor = "#F5E6D3",
                futureFlag = false,
                showInWidget = true,
                resetCount = 3,
                totalResetDays = 90,
                pinnedTimestamp = 123456789L,
            ),
            CountUpItem(
                id = "item-2",
                name = "Guitar",
                epochDay = 19500,
                comment = "Chords practice",
                icon = "guitar",
                cardColor = "",
                futureFlag = false,
                showInWidget = false,
            ),
        )

        val original = CountUpBackupPayload(
            schemaVersion = 1,
            exportTimestamp = 1757365200000L,
            appVersion = "2.19.3",
            sortOrder = SortOrder.DATE_DESC,
            themeMode = ThemeMode.DARK,
            backgroundTheme = BackgroundTheme.DREAM_BOAT,
            items = items,
        )

        val json = CountUpBackupPayload.encode(original)
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"appVersion\": \"2.19.3\""))
        assertTrue(json.contains("Meditation"))
        assertTrue(json.contains("Guitar"))

        val decoded = CountUpBackupPayload.decode(json)
        assertNotNull(decoded)
        assertEquals(original.schemaVersion, decoded!!.schemaVersion)
        assertEquals(original.exportTimestamp, decoded.exportTimestamp)
        assertEquals(original.appVersion, decoded.appVersion)
        assertEquals(original.sortOrder, decoded.sortOrder)
        assertEquals(original.themeMode, decoded.themeMode)
        assertEquals(original.backgroundTheme, decoded.backgroundTheme)
        assertEquals(2, decoded.items.size)

        val first = decoded.items[0]
        assertEquals("item-1", first.id)
        assertEquals("Meditation", first.name)
        assertEquals(20000L, first.epochDay)
        assertEquals("Morning silence", first.comment)
        assertEquals("lotus", first.icon)
        assertEquals("#F5E6D3", first.cardColor)
        assertEquals(3, first.resetCount)
        assertEquals(90L, first.totalResetDays)
        assertEquals(123456789L, first.pinnedTimestamp)

        val second = decoded.items[1]
        assertEquals("item-2", second.id)
        assertEquals("Guitar", second.name)
        assertFalse(second.showInWidget)
    }

    @Test
    fun encodeAndDecodePreservesChineseCharactersAndSolarTerms() {
        val items = listOf(
            CountUpItem(
                id = "zh-1",
                name = "理发 · 清明",
                epochDay = 20050,
                comment = "春和景明，万物皆洁齐而清明。🧘",
                icon = "haircut",
                cardColor = "#8C6D58",
                futureFlag = false,
                showInWidget = true,
                resetCount = 5,
                totalResetDays = 150,
            ),
            CountUpItem(
                id = "zh-2",
                name = "戒烟",
                epochDay = 19800,
                comment = "坚持100天 🌟",
            ),
        )

        val original = CountUpBackupPayload(
            items = items,
            themeMode = ThemeMode.SYSTEM,
            backgroundTheme = BackgroundTheme.AUTO_DAILY,
        )

        val outputStream = ByteArrayOutputStream()
        outputStream.bufferedWriter(Charsets.UTF_8).use {
            it.write(CountUpBackupPayload.encode(original))
        }

        val jsonString = ByteArrayInputStream(outputStream.toByteArray())
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val decoded = CountUpBackupPayload.decode(jsonString)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.items.size)
        assertEquals("理发 · 清明", decoded.items[0].name)
        assertEquals("春和景明，万物皆洁齐而清明。🧘", decoded.items[0].comment)
        assertEquals("戒烟", decoded.items[1].name)
        assertEquals("坚持100天 🌟", decoded.items[1].comment)
    }

    @Test
    fun decodeReturnsNullOnBlankOrInvalidJson() {
        assertNull(CountUpBackupPayload.decode(null))
        assertNull(CountUpBackupPayload.decode(""))
        assertNull(CountUpBackupPayload.decode("   "))
        assertNull(CountUpBackupPayload.decode("not a valid json"))
    }
}
