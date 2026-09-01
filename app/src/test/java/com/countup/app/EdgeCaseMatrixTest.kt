package com.countup.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Aggressive Edge Case & Adversarial Quality Assurance Matrix.
 * Covers concurrency races, corrupted preference types, extreme unicode/escaping,
 * date math leap years, float epoch coercion, and double-tap state machines.
 */
class EdgeCaseMatrixTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("edge_case_matrix").toFile()
        testContext = TestContext(tempDir)
    }

    // ==========================================
    // 1. CONCURRENCY & MULTI-THREADED STRESS TEST
    // ==========================================

    @Test
    fun concurrentMutationsMaintainCompleteDataIntegrity() {
        val store = CountUpStore(testContext)
        val threadCount = 12
        val opsPerThread = 25
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val errors = AtomicInteger(0)

        for (t in 0 until threadCount) {
            executor.execute {
                try {
                    for (op in 0 until opsPerThread) {
                        val item = store.addItem(
                            name = "Thread-$t Item-$op",
                            epochDay = 20000L + op,
                            comment = "Comment $op",
                        )
                        if (item == null) {
                            errors.incrementAndGet()
                        } else {
                            store.updateItem(item.id, "Updated-$t-$op", 20050L, "New comment")
                            store.setWidgetVisibility(item.id, (op % 2 == 0))
                        }
                    }
                } catch (_: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        assertEquals(0, errors.get())

        val finalItems = store.items()
        assertEquals(threadCount * opsPerThread, finalItems.size)

        // Verify backup file matches SharedPreferences
        val backupFile = File(tempDir, "countup_backup.json")
        assertTrue(backupFile.exists())
        val backupItems = decodeItems(backupFile.readText(Charsets.UTF_8))
        assertNotNull(backupItems)
        assertEquals(finalItems.size, backupItems!!.size)
    }

    // ==========================================
    // 2. CORRUPTED PREFERENCE TYPE INJECTION
    // ==========================================

    @Test
    fun corruptedTypeInSharedPreferencesFallsBackToDiskBackup() {
        val store1 = CountUpStore(testContext)
        val item = store1.addItem("Saved Goal", 19500L, "Important")!!
        assertEquals(1, store1.items().size)

        // Inject an invalid type (Int instead of String) for items_v1 into SharedPreferences
        val prefs = testContext.getSharedPreferences("countup_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("items_v1", 99999).commit()

        // Create new store instance; items() must not throw ClassCastException and self-heal
        val store2 = CountUpStore(testContext)
        val recovered = store2.items()
        assertEquals(1, recovered.size)
        assertEquals("Saved Goal", recovered[0].name)
        assertEquals(item.id, recovered[0].id)
    }

    // ==========================================
    // 3. UNICODE, ESCAPES, & BRACE PARSING
    // ==========================================

    @Test
    fun complexUnicodeEmojiAndEscapesSurviveRoundTrip() {
        val complex = CountUpItem(
            id = "u1",
            name = "🎯 禅 • 专注 \uD83D\uDE00 \"quotes\" & \\slashes\\",
            epochDay = 20500,
            comment = "Multi-line:\nLine 1\r\nLine 2\tTabbed {nested brace} \\\"escaped\\\"",
            icon = "lotus",
        )
        val encoded = encodeItems(listOf(complex))
        val decoded = decodeItems(encoded)!!
        assertEquals(1, decoded.size)
        assertEquals(complex.name, decoded[0].name)
        assertEquals(complex.comment, decoded[0].comment)
    }

    @Test
    fun salvageHandlesComplexCommentsWithQuotesAndBraces() {
        val broken =
            "Garbage prefix [{\"id\":\"ok\",\"name\":\"Habit\",\"epochDay\":100,\"comment\":\"Bench {warmup: 10kg, sets: \\\"3x10\\\"}\"},{\"id\":\"cut"
        val salvaged = salvageItems(broken)
        assertEquals(1, salvaged.size)
        assertEquals("Habit", salvaged[0].name)
        assertEquals("Bench {warmup: 10kg, sets: \"3x10\"}", salvaged[0].comment)
    }

    // ==========================================
    // 4. FLOAT AND STRING EPOCH COERCION
    // ==========================================

    @Test
    fun floatAndNumericStringEpochDaysCoerceCorrectly() {
        val raw = "[{\"id\":\"f1\",\"name\":\"Float String\",\"epochDay\":\"20667.0\"},{\"id\":\"f2\",\"name\":\"Float Num\",\"epochDay\":20667.9}]"
        val decoded = decodeItems(raw)!!
        assertEquals(2, decoded.size)
        assertEquals(20667L, decoded[0].epochDay)
        assertEquals(20667L, decoded[1].epochDay)
    }

    @Test
    fun extremeAndInvalidEpochDaysHandledSafely() {
        val minEpoch = LocalDate.MIN.toEpochDay()
        val maxEpoch = LocalDate.MAX.toEpochDay()

        // Valid boundaries
        val validMin = "[{\"id\":\"min\",\"name\":\"Min\",\"epochDay\":$minEpoch}]"
        val validMax = "[{\"id\":\"max\",\"name\":\"Max\",\"epochDay\":$maxEpoch}]"
        assertEquals(minEpoch, decodeItems(validMin)!![0].epochDay)
        assertEquals(maxEpoch, decodeItems(validMax)!![0].epochDay)

        // Out of bounds values must be dropped
        val invalidUnderflow = "[{\"id\":\"bad1\",\"name\":\"Under\",\"epochDay\":${minEpoch - 1}}]"
        val invalidOverflow = "[{\"id\":\"bad2\",\"name\":\"Over\",\"epochDay\":${maxEpoch + 1}}]"
        assertNull(decodeItems(invalidUnderflow))
        assertNull(decodeItems(invalidOverflow))
    }

    // ==========================================
    // 5. DATE MATH & LEAP YEARS
    // ==========================================

    @Test
    fun leapYearAndCenturyRulesComputeDaysAccurately() {
        // Leap year 2024 (29 days in Feb)
        val leapStart = LocalDate.of(2024, 2, 28)
        val leapEnd = LocalDate.of(2024, 3, 1)
        assertEquals(2L, daysSince(leapStart, leapEnd))

        // Common year 2023 (28 days in Feb)
        val commonStart = LocalDate.of(2023, 2, 28)
        val commonEnd = LocalDate.of(2023, 3, 1)
        assertEquals(1L, daysSince(commonStart, commonEnd))

        // Century non-leap year (1900 was not a leap year)
        val centuryStart = LocalDate.of(1900, 2, 28)
        val centuryEnd = LocalDate.of(1900, 3, 1)
        assertEquals(1L, daysSince(centuryStart, centuryEnd))

        // Quad-century leap year (2000 was a leap year)
        val quadStart = LocalDate.of(2000, 2, 28)
        val quadEnd = LocalDate.of(2000, 3, 1)
        assertEquals(2L, daysSince(quadStart, quadEnd))
    }

    // ==========================================
    // 6. NEGATIVE COUNT SUB-LABELING
    // ==========================================

    @Test
    fun formatAnchorDateSubLabelSwitchesSinceAndUntil() {
        val testDate = LocalDate.of(2026, 9, 23)
        val sinceTemplate = "since %1\$s"
        val untilTemplate = "until %1\$s"

        // Future count (count < 0) -> until
        val futureLabel = formatAnchorDateSubLabel(-5L, testDate, sinceTemplate, untilTemplate)
        assertTrue(futureLabel.startsWith("UNTIL "))
        assertTrue(futureLabel.contains("2026"))

        // Today (count == 0) -> since
        val todayLabel = formatAnchorDateSubLabel(0L, testDate, sinceTemplate, untilTemplate)
        assertTrue(todayLabel.startsWith("SINCE "))
        assertTrue(todayLabel.contains("2026"))

        // Past count (count > 0) -> since
        val pastLabel = formatAnchorDateSubLabel(42L, testDate, sinceTemplate, untilTemplate)
        assertTrue(pastLabel.startsWith("SINCE "))
        assertTrue(pastLabel.contains("2026"))
    }

    // ==========================================
    // 7. WIDGET DOUBLE TAP STATE MACHINE
    // ==========================================

    @Test
    fun widgetDoubleTapStateMachineHonorsTimeoutsAndTargetSwitching() {
        var simulatedTime = 10000L
        ResetCountReceiver.clock = { simulatedTime }

        val id1 = "item_alpha"
        val id2 = "item_beta"

        // Initial state is disarmed
        assertFalse(ResetCountReceiver.isArmed(id1))
        assertFalse(ResetCountReceiver.isArmed(id2))

        // Arm item 1 at t=10000
        ResetCountReceiver.arm(id1)
        assertTrue(ResetCountReceiver.isArmed(id1))
        assertFalse(ResetCountReceiver.isArmed(id2))

        // At t=13000 (within 4000ms), item 1 is still armed
        simulatedTime = 13000L
        assertTrue(ResetCountReceiver.isArmed(id1))

        // At t=14500 (exceeded 4000ms), item 1 timed out
        simulatedTime = 14500L
        assertFalse(ResetCountReceiver.isArmed(id1))

        // Arm item 2: switches armed target immediately
        simulatedTime = 15000L
        ResetCountReceiver.arm(id2)
        assertFalse(ResetCountReceiver.isArmed(id1))
        assertTrue(ResetCountReceiver.isArmed(id2))

        // Disarm clears everything
        ResetCountReceiver.disarm()
        assertFalse(ResetCountReceiver.isArmed(id1))
        assertFalse(ResetCountReceiver.isArmed(id2))
    }

    // ==========================================
    // 8. THEME CYCLING WITH EXTREME EPOCH DAYS
    // ==========================================

    @Test
    fun resolveActiveThemeNeverThrowsOnNegativeOrExtremeEpochDays() {
        val themes = listOf(
            resolveActiveTheme(BackgroundTheme.AUTO_DAILY, 0L),
            resolveActiveTheme(BackgroundTheme.AUTO_DAILY, -1L),
            resolveActiveTheme(BackgroundTheme.AUTO_DAILY, -100L),
            resolveActiveTheme(BackgroundTheme.AUTO_DAILY, Long.MIN_VALUE),
            resolveActiveTheme(BackgroundTheme.AUTO_DAILY, Long.MAX_VALUE),
        )

        for (theme in themes) {
            assertTrue(theme != BackgroundTheme.AUTO_DAILY)
            assertNotNull(theme.id)
        }
    }

    // =========================================================================
    // 9. CARD COLOR CONTRAST & ADVERSARIAL TOKEN RESOLUTION MATRIX
    // =========================================================================

    @Test
    fun cardBackgroundColorAdversarialInputsGracefullyResolve() {
        // Null, empty, whitespace, and corrupt strings
        assertEquals(Color.White, cardBackgroundColor(null))
        assertEquals(Color.White, cardBackgroundColor(""))
        assertEquals(Color.White, cardBackgroundColor("   "))
        assertEquals(Color.White, cardBackgroundColor("invalid_token_12345"))
        assertEquals(Color.White, cardBackgroundColor("not_a_hex"))
        assertEquals(Color.White, cardBackgroundColor("#XYZ123"))

        // Valid presets
        for (preset in CARD_COLOR_PRESETS) {
            assertEquals(preset.cardBg, cardBackgroundColor(preset.id))
        }

        // Custom hex strings (with and without hash)
        assertEquals(Color(0xFFE88B58), cardBackgroundColor("#E88B58"))
        assertEquals(Color(0xFF5E8C6D), cardBackgroundColor("#5e8c6d"))
        assertEquals(Color(0xFF2C2416), cardBackgroundColor("2C2416"))
    }

    @Test
    fun cardInkContrastMeetsWcagLuminanceThresholdsAcrossAllPresets() {
        for (preset in CARD_COLOR_PRESETS) {
            val bg = preset.cardBg
            val isDark = isDarkCardBackground(bg)
            val primaryInk = cardPrimaryInk(bg)
            val mutedInk = cardMutedInk(bg)
            val border = cardBorderColor(bg)

            if (isDark) {
                // On dark cards, ink must be light/white
                assertTrue("Dark card ${preset.id} primary ink should be light", primaryInk.luminance() > 0.8f)
                assertTrue("Dark card ${preset.id} muted ink should be light", mutedInk.luminance() > 0.5f)
            } else {
                // On light cards, ink must be dark
                assertTrue("Light card ${preset.id} primary ink should be dark", primaryInk.luminance() < 0.2f)
                assertTrue("Light card ${preset.id} muted ink should be dark", mutedInk.luminance() < 0.35f)
            }

            assertNotNull(border)
        }
    }

    // =========================================================================
    // 10. EXPANDED ICON RESILIENCE & PHOSPHOR MAPPING MATRIX
    // =========================================================================

    @Test
    fun allNinetyOneIconsResolveToValidNonZeroResourceIds() {
        assertEquals(91, ALL_ICON_NAMES.size)

        for (iconName in ALL_ICON_NAMES) {
            val resId = iconRes(iconName)
            assertTrue("Icon '$iconName' must resolve to a valid resource ID > 0", resId > 0)
        }
    }

    @Test
    fun iconResFallbackHandlesCorruptedAndAdversarialStrings() {
        val defaultRes = R.drawable.ic_person
        val testInputs = listOf(
            null to defaultRes,
            "" to defaultRes,
            "   " to defaultRes,
            "\n\t" to defaultRes,
            "../../etc/passwd" to defaultRes,
            "<script>alert(1)</script>" to defaultRes,
            "🔥🚀🎉" to defaultRes,
            "unknown_icon" to defaultRes,
        )

        for ((input, expected) in testInputs) {
            assertEquals("Input '$input' should resolve to default person icon", expected, iconRes(input ?: ""))
        }
    }
}
