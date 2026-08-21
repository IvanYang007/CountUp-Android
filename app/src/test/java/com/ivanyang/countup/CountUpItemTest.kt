package com.ivanyang.countup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountUpItemTest {

    private fun sample(): List<CountUpItem> = listOf(
        CountUpItem(id = "a", name = "Haircut", epochDay = 20667),
        CountUpItem(id = "b", name = "Kitten", epochDay = 21000),
    )

    @Test
    fun encodeThenDecodeRoundTrips() {
        val encoded = encodeItems(sample())
        val decoded = decodeItems(encoded)
        assertEquals(sample(), decoded)
    }

    @Test
    fun emptyListRoundTripsToEmptyNotNull() {
        val decoded = decodeItems(encodeItems(emptyList()))
        assertTrue(decoded != null && decoded.isEmpty())
    }

    @Test
    fun nullOrBlankInputDecodesToNull() {
        assertNull(decodeItems(null))
        assertNull(decodeItems(""))
        assertNull(decodeItems("   "))
    }

    @Test
    fun malformedInputDecodesToNull() {
        assertNull(decodeItems("not json at all"))
        assertNull(decodeItems("[1,2,3]"))                    // wrong element shape
        assertNull(decodeItems("{\"id\":\"x\"}"))             // object, not array
        assertNull(decodeItems("[{\"id\":\"a\"}]"))           // missing fields
    }

    @Test
    fun namesWithQuotesAndUnicodeSurviveJsonEscaping() {
        val tricky = CountUpItem(id = "z", name = "Say \"hi\"\u2024 \u00e9\u00e8", epochDay = 1)
        val decoded = decodeItems(encodeItems(listOf(tricky)))
        assertEquals(listOf(tricky), decoded)
    }

    @Test
    fun blankNameFallsBackToDefaultAtStoreLevelImplicitInModel() {
        // The blank->default rule lives in CountUpStore; here we just confirm the
        // unambiguous DEFAULT constant is used to keep it consistent.
        assertEquals("Item", DEFAULT_ITEM_NAME)
    }

    @Test
    fun iconSurvivesRoundTrip() {
        val withIcon = CountUpItem(id = "x", name = "Plant", epochDay = 1, icon = "mood")
        assertEquals(listOf(withIcon), decodeItems(encodeItems(listOf(withIcon))))
    }

    @Test
    fun missingIconFieldDecodesToEmpty() {
        // Legacy items stored without an "icon" key decode to an empty icon
        // (the UI falls back to DEFAULT_ICON).
        val decoded = decodeItems("[{\"id\":\"a\",\"name\":\"Old\",\"epochDay\":5}]")
        assertEquals("", decoded!![0].icon)
    }
}
