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

    @Test
    fun futureFlagSurvivesRoundTrip() {
        val flagged = CountUpItem(id = "f", name = "Trip", epochDay = 30000, futureFlag = true)
        val unflagged = CountUpItem(id = "u", name = "Past", epochDay = 100, futureFlag = false)
        assertEquals(listOf(flagged, unflagged), decodeItems(encodeItems(listOf(flagged, unflagged))))
    }

    @Test
    fun missingFutureFlagFieldDecodesToFalse() {
        // Legacy items stored before the future flag existed decode with the
        // flag off, so existing installs keep their current styling.
        val decoded = decodeItems("[{\"id\":\"a\",\"name\":\"Old\",\"epochDay\":5}]")
        assertEquals(false, decoded!![0].futureFlag)
    }

    @Test
    fun oneMalformedElementDoesNotDestroyTheRest() {
        // A single corrupt element is dropped; parseable siblings survive.
        val raw = "[{\"id\":\"a\",\"name\":\"Good\",\"epochDay\":5},{\"id\":123},{\"id\":\"b\",\"name\":\"Also good\",\"epochDay\":9}]"
        val decoded = decodeItems(raw)!!
        assertEquals(listOf("Good", "Also good"), decoded.map { it.name })
    }

    @Test
    fun arrayWhereEveryElementIsMalformedDecodesToNull() {
        // Total corruption must not be masked as a valid empty list; null routes
        // the store to recovery (which quarantines the payload).
        assertNull(decodeItems("[1,2,3]"))
        assertNull(decodeItems("[{\"id\":\"a\"}]"))
    }

    @Test
    fun outOfRangeEpochDayElementIsDropped() {
        // An epochDay outside LocalDate's range would crash LocalDate.ofEpochDay
        // at render time; decode drops it while keeping valid siblings.
        val raw =
            "[{\"id\":\"a\",\"name\":\"Valid\",\"epochDay\":5},{\"id\":\"b\",\"name\":\"Insane\",\"epochDay\":9223372036854775807}]"
        val decoded = decodeItems(raw)!!
        assertEquals(listOf("Valid"), decoded.map { it.name })
        assertEquals(5L, decoded[0].epochDay)
    }
}
