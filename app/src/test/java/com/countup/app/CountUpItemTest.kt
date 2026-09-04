package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun cardColorSurvivesRoundTrip() {
        val withColor = CountUpItem(id = "cc", name = "Spa", epochDay = 150, cardColor = "terracotta")
        assertEquals(listOf(withColor), decodeItems(encodeItems(listOf(withColor))))
    }

    @Test
    fun missingCardColorFieldDecodesToEmpty() {
        // Legacy items stored before cardColor existed decode with empty string (defaults to paper white).
        val decoded = decodeItems("[{\"id\":\"a\",\"name\":\"Old\",\"epochDay\":5}]")
        assertEquals("", decoded!![0].cardColor)
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

    @Test
    fun itemDraftEncapsulatesAllFieldsWithDefaults() {
        val defaultDraft = ItemDraft(name = "Meditation", epochDay = 20500L)
        assertEquals("Meditation", defaultDraft.name)
        assertEquals(20500L, defaultDraft.epochDay)
        assertEquals("", defaultDraft.comment)
        assertEquals("", defaultDraft.icon)
        assertEquals("", defaultDraft.cardColor)

        val customDraft = ItemDraft(
            name = "Zen Garden",
            epochDay = 20600L,
            comment = "Daily pruning",
            icon = "spa",
            cardColor = "sage_forest",
        )
        val copyDraft = customDraft.copy(comment = "Weekly pruning")
        assertEquals("Weekly pruning", copyDraft.comment)
        assertEquals("spa", copyDraft.icon)
        assertEquals("sage_forest", copyDraft.cardColor)
    }

    @Test
    fun resetCountAndTotalResetDaysSurviveJsonRoundTrip() {
        val item = CountUpItem(
            id = "r1",
            name = "Quit Smoking",
            epochDay = 20000L,
            resetCount = 4,
            totalResetDays = 112L,
        )
        val decoded = decodeItems(encodeItems(listOf(item)))!!
        assertEquals(1, decoded.size)
        assertEquals(4, decoded[0].resetCount)
        assertEquals(112L, decoded[0].totalResetDays)
        assertEquals(28, decoded[0].averageResetDays)
    }

    @Test
    fun missingResetFieldsDecodeWithSafeDefaults() {
        val legacyJson = "[{\"id\":\"legacy1\",\"name\":\"Old Item\",\"epochDay\":19000}]"
        val decoded = decodeItems(legacyJson)!!
        assertEquals(1, decoded.size)
        assertEquals(0, decoded[0].resetCount)
        assertEquals(0L, decoded[0].totalResetDays)
        assertEquals(0, decoded[0].averageResetDays)
    }

    @Test
    fun negativeResetFieldsAreClampedToZero() {
        val malformedJson = "[{\"id\":\"bad1\",\"name\":\"Bad Item\",\"epochDay\":19000,\"resetCount\":-5,\"totalResetDays\":-100}]"
        val decoded = decodeItems(malformedJson)!!
        assertEquals(1, decoded.size)
        assertEquals(0, decoded[0].resetCount)
        assertEquals(0L, decoded[0].totalResetDays)
        assertEquals(0, decoded[0].averageResetDays)
    }

    @Test
    fun averageResetDaysCalculatesCorrectlyWithMathematicalRounding() {
        val maiden = CountUpItem(id = "m", name = "Maiden", epochDay = 20000L, resetCount = 0, totalResetDays = 0L)
        assertEquals(0, maiden.averageResetDays)

        val single = CountUpItem(id = "s", name = "Single", epochDay = 20000L, resetCount = 1, totalResetDays = 42L)
        assertEquals(42, single.averageResetDays)

        // 100 / 3 = 33.33 -> rounds to 33
        val roundDown = CountUpItem(id = "rd", name = "Round Down", epochDay = 20000L, resetCount = 3, totalResetDays = 100L)
        assertEquals(33, roundDown.averageResetDays)

        // 101 / 3 = 33.67 -> rounds to 34
        val roundUp = CountUpItem(id = "ru", name = "Round Up", epochDay = 20000L, resetCount = 3, totalResetDays = 101L)
        assertEquals(34, roundUp.averageResetDays)
    }

    @Test
    fun resetToDomainMethodAccumulatesCycleDaysCorrectly() {
        val initial = CountUpItem(id = "habit", name = "Meditation", epochDay = 20000L)
        assertEquals(0, initial.resetCount)
        assertEquals(0L, initial.totalResetDays)

        val firstReset = initial.resetTo(20030L)
        assertEquals(20030L, firstReset.epochDay)
        assertEquals(1, firstReset.resetCount)
        assertEquals(30L, firstReset.totalResetDays)
        assertEquals(30, firstReset.averageResetDays)
        assertEquals(false, firstReset.futureFlag)

        val secondReset = firstReset.resetTo(20050L)
        assertEquals(20050L, secondReset.epochDay)
        assertEquals(2, secondReset.resetCount)
        assertEquals(50L, secondReset.totalResetDays) // 30 + 20
        assertEquals(25, secondReset.averageResetDays) // 50 / 2
    }

    @Test
    fun resetToOnNegativeDayItemSucceedsAndUsesAbsDaysForCycleAverage() {
        val futureItem = CountUpItem(id = "f", name = "Future Event", epochDay = 20100L, futureFlag = true)
        val resetItem = futureItem.resetTo(20050L)
        assertEquals(20050L, resetItem.epochDay)
        assertEquals(1, resetItem.resetCount)
        assertEquals(50L, resetItem.totalResetDays)
        assertEquals(50, resetItem.averageResetDays)
        assertEquals(false, resetItem.futureFlag)
    }

    @Test
    fun resetToWhenEpochDayEqualsNewEpochDayReturnsSameItemWithoutIncrementingResetCount() {
        val item = CountUpItem(
            id = "same_day",
            name = "Same Day Event",
            epochDay = 20500L,
            resetCount = 3,
            totalResetDays = 90L,
        )
        val result = item.resetTo(20500L)
        assertEquals(item, result)
        assertEquals(3, result.resetCount)
        assertEquals(90L, result.totalResetDays)
        assertEquals(30, result.averageResetDays)
    }

    @Test
    fun toResetSnapshotCapturesCurrentState() {
        val item = CountUpItem(
            id = "s1",
            name = "Snapshot Test",
            epochDay = 20000L,
            resetCount = 2,
            totalResetDays = 40L,
            futureFlag = false,
        )
        val snap = item.toResetSnapshot()
        assertEquals(20000L, snap.epochDay)
        assertEquals(2, snap.resetCount)
        assertEquals(40L, snap.totalResetDays)
        assertEquals(false, snap.futureFlag)
    }

    @Test
    fun resetToOnNegativeDayItemCalculatesRoundedAverageCorrectlyOverMultipleCycles() {
        // Cycle 1: Started 15 days in future (anchor = 20065). Reset on day 20050.
        // Elapsed cycle = abs(20050 - 20065) = 15. Average = 15 / 1 = 15.
        val initial = CountUpItem(id = "cycle_test", name = "Future Project", epochDay = 20065L, futureFlag = true)
        val reset1 = initial.resetTo(20050L)
        assertEquals(20050L, reset1.epochDay)
        assertEquals(1, reset1.resetCount)
        assertEquals(15L, reset1.totalResetDays)
        assertEquals(15, reset1.averageResetDays)
        assertEquals(false, reset1.futureFlag)

        // Cycle 2: Advanced 30 days (anchor is now 20050, resetting on day 20080).
        // Cycle = abs(20080 - 20050) = 30. Total = 15 + 30 = 45. Average = round(45 / 2 = 22.5) -> rounds half to even 22.
        val reset2 = reset1.resetTo(20080L)
        assertEquals(20080L, reset2.epochDay)
        assertEquals(2, reset2.resetCount)
        assertEquals(45L, reset2.totalResetDays)
        assertEquals(22, reset2.averageResetDays)

        // Cycle 3: Advanced 40 days (resetting on day 20120).
        // Cycle = abs(20120 - 20080) = 40. Total = 45 + 40 = 85. Average = round(85 / 3) = 28.
        val reset3 = reset2.resetTo(20120L)
        assertEquals(20120L, reset3.epochDay)
        assertEquals(3, reset3.resetCount)
        assertEquals(85L, reset3.totalResetDays)
        assertEquals(28, reset3.averageResetDays)
    }

    @Test
    fun resetToOnArrivedFutureItemClearsFutureFlagAndAccumulatesPositiveDays() {
        // Item was created with futureFlag, but the target date was 5 days ago (anchor = 20045, today = 20050).
        val arrivedItem = CountUpItem(id = "arr", name = "Arrived Birthday", epochDay = 20045L, futureFlag = true)
        val resetItem = arrivedItem.resetTo(20050L)
        assertEquals(20050L, resetItem.epochDay)
        assertEquals(1, resetItem.resetCount)
        assertEquals(5L, resetItem.totalResetDays)
        assertEquals(5, resetItem.averageResetDays)
        assertEquals(false, resetItem.futureFlag)
    }

    @Test
    fun restoreFromSnapshotRestoresNegativeItemStateAccurately() {
        val original = CountUpItem(
            id = "neg_restore",
            name = "Trip Countdown",
            epochDay = 20100L,
            futureFlag = true,
            resetCount = 0,
            totalResetDays = 0L,
        )
        val snapshot = original.toResetSnapshot()
        val afterReset = original.resetTo(20050L)
        assertEquals(1, afterReset.resetCount)
        assertEquals(50L, afterReset.totalResetDays)
        assertEquals(false, afterReset.futureFlag)

        val restored = afterReset.restoreFrom(snapshot)
        assertEquals(original.epochDay, restored.epochDay)
        assertEquals(original.futureFlag, restored.futureFlag)
        assertEquals(original.resetCount, restored.resetCount)
        assertEquals(original.totalResetDays, restored.totalResetDays)
        assertEquals(original, restored)
    }

    @Test
    fun isResettableOnReturnsFalseWhenItemEpochDayMatchesToday() {
        val item = CountUpItem(id = "same_day", name = "Test", epochDay = 20050L)
        assertFalse(item.isResettableOn(20050L))
        assertFalse(item.isResettableOn(LocalDate.ofEpochDay(20050L)))
    }

    @Test
    fun isResettableOnReturnsTrueWhenItemEpochDayIsPastOrFuture() {
        val pastItem = CountUpItem(id = "past", name = "Past", epochDay = 20040L)
        val futureItem = CountUpItem(id = "future", name = "Future", epochDay = 20060L, futureFlag = true)
        val today = LocalDate.ofEpochDay(20050L)

        assertTrue(pastItem.isResettableOn(20050L))
        assertTrue(pastItem.isResettableOn(today))
        assertTrue(futureItem.isResettableOn(20050L))
        assertTrue(futureItem.isResettableOn(today))
    }
}
