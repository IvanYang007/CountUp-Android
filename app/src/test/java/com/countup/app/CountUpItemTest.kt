package com.countup.app

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
    fun showInWidgetSurvivesRoundTrip() {
        val visible = CountUpItem(id = "v", name = "Visible", epochDay = 100, showInWidget = true)
        val hidden = CountUpItem(id = "h", name = "Hidden", epochDay = 200, showInWidget = false)
        assertEquals(listOf(visible, hidden), decodeItems(encodeItems(listOf(visible, hidden))))
    }

    @Test
    fun missingShowInWidgetFieldDecodesToVisible() {
        // Legacy items stored before the eye toggle existed stay in the widget.
        val decoded = decodeItems("[{\"id\":\"a\",\"name\":\"Old\",\"epochDay\":5}]")
        assertEquals(true, decoded!![0].showInWidget)
    }

    @Test
    fun commentSurvivesRoundTrip() {
        val withComment = CountUpItem(id = "c", name = "Gym", epochDay = 100, comment = "Leg day focus\nFelt great")
        assertEquals(listOf(withComment), decodeItems(encodeItems(listOf(withComment))))
    }

    @Test
    fun missingCommentFieldDecodesToEmpty() {
        // Legacy items stored before the comment field existed decode with empty comment.
        val decoded = decodeItems("[{\"id\":\"a\",\"name\":\"Old\",\"epochDay\":5}]")
        assertEquals("", decoded!![0].comment)
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

    @Test
    fun missingIdFieldGeneratesValidFallbackId() {
        val raw = "[{\"name\":\"No Id Item\",\"epochDay\":100}]"
        val decoded = decodeItems(raw)!!
        assertEquals(1, decoded.size)
        assertEquals("No Id Item", decoded[0].name)
        assertTrue(decoded[0].id.isNotBlank())
    }

    @Test
    fun missingOrBlankNameFallsBackToDefaultItemName() {
        val raw = "[{\"id\":\"x1\",\"name\":\"\",\"epochDay\":100},{\"id\":\"x2\",\"epochDay\":200}]"
        val decoded = decodeItems(raw)!!
        assertEquals(2, decoded.size)
        assertEquals(DEFAULT_ITEM_NAME, decoded[0].name)
        assertEquals(DEFAULT_ITEM_NAME, decoded[1].name)
    }

    @Test
    fun stringEncodedEpochDayDecodesSafely() {
        val raw = "[{\"id\":\"s1\",\"name\":\"String Day\",\"epochDay\":\"20500\"}]"
        val decoded = decodeItems(raw)!!
        assertEquals(1, decoded.size)
        assertEquals(20500L, decoded[0].epochDay)
    }

    @Test
    fun unknownFutureFieldsAreIgnoredGracefully() {
        // Future versions may add keys like "tags", "priority", "syncVersion".
        // Current code must decode the core item cleanly without failing.
        val raw =
            "[{\"id\":\"f1\",\"name\":\"Future Item\",\"epochDay\":300,\"extraFutureField\":\"v2\",\"priority\":1,\"tags\":[\"health\"]}]"
        val decoded = decodeItems(raw)!!
        assertEquals(1, decoded.size)
        assertEquals("Future Item", decoded[0].name)
        assertEquals(300L, decoded[0].epochDay)
    }

    @Test
    fun truncatedJsonArraySalvagesAllCompleteItems() {
        // Simulates an app kill / power cut mid-write that truncated the JSON array
        val truncated =
            "[{\"id\":\"1\",\"name\":\"Item 1\",\"epochDay\":100},{\"id\":\"2\",\"name\":\"Item 2\",\"epochDay\":200},{\"id\":\"3\",\"name\":\"Cut off"
        val salvaged = salvageItems(truncated)
        assertEquals(2, salvaged.size)
        assertEquals("Item 1", salvaged[0].name)
        assertEquals("Item 2", salvaged[1].name)
    }

    @Test
    fun brokenOuterSyntaxWithEmbeddedObjectsSalvagesValidItems() {
        val broken =
            "Garbage prefix {invalid json} then {\"id\":\"ok1\",\"name\":\"Recovered\",\"epochDay\":500} and suffix"
        val salvaged = salvageItems(broken)
        assertEquals(1, salvaged.size)
        assertEquals("Recovered", salvaged[0].name)
        assertEquals(500L, salvaged[0].epochDay)
    }

    @Test
    fun extremeBoundsMinAndMaxEpochDaySurvive() {
        val minItem = CountUpItem(id = "min", name = "Min Date", epochDay = java.time.LocalDate.MIN.toEpochDay())
        val maxItem = CountUpItem(id = "max", name = "Max Date", epochDay = java.time.LocalDate.MAX.toEpochDay())
        val encoded = encodeItems(listOf(minItem, maxItem))
        val decoded = decodeItems(encoded)!!
        assertEquals(2, decoded.size)
        assertEquals(java.time.LocalDate.MIN.toEpochDay(), decoded[0].epochDay)
        assertEquals(java.time.LocalDate.MAX.toEpochDay(), decoded[1].epochDay)
    }

    @Test
    fun commentsWithNestedCurlyBracesAreSalvagedCorrectly() {
        val payloadWithBraces =
            "[{\"id\":\"b1\",\"name\":\"Gym\",\"epochDay\":100,\"comment\":\"Bench {warmup + 3 sets}\"},{\"id\":\"b2\",\"name\":\"Study\",\"epochDay\":200,\"comment\":\"Math {ch1, ch2}\"}"
        val salvaged = salvageItems(payloadWithBraces)
        assertEquals(2, salvaged.size)
        assertEquals("Bench {warmup + 3 sets}", salvaged[0].comment)
        assertEquals("Math {ch1, ch2}", salvaged[1].comment)
    }

    @Test
    fun hundredItemsStressTestRoundTripsAccurately() {
        val items = (1..150).map { i ->
            CountUpItem(
                id = "id_$i",
                name = "Habit #$i \u2022 \u6c34\u58a8",
                epochDay = 20000L + i,
                comment = "Comment for habit $i",
                icon = "circle_check",
                futureFlag = (i % 2 == 0),
                showInWidget = (i % 3 != 0),
            )
        }
        val encoded = encodeItems(items)
        val decoded = decodeItems(encoded)!!
        assertEquals(150, decoded.size)
        assertEquals(items, decoded)
    }
}
