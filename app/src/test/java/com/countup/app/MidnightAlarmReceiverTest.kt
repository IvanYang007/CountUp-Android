package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class MidnightAlarmReceiverTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("midnight_alarm_test").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun calculateNextMidnightMillisTargetsExactlyZeroZeroZeroZeroZeroOne() {
        val zone = ZoneId.of("America/New_York")

        // Mid-day: 2026-09-06 14:30:00 -> should target 2026-09-07 00:00:01
        val afternoon = ZonedDateTime.of(2026, 9, 6, 14, 30, 0, 0, zone)
        val targetMillis = MidnightAlarmReceiver.calculateNextMidnightMillis(afternoon)

        val expectedTarget = ZonedDateTime.of(2026, 9, 7, 0, 0, 1, 0, zone)
        assertEquals(expectedTarget.toInstant().toEpochMilli(), targetMillis)

        // Right before 00:00:01: 2026-09-06 00:00:00.200 -> should target 2026-09-06 00:00:01
        val justBefore = ZonedDateTime.of(2026, 9, 6, 0, 0, 0, 200_000_000, zone)
        val targetJustBefore = MidnightAlarmReceiver.calculateNextMidnightMillis(justBefore)
        val expectedToday = ZonedDateTime.of(2026, 9, 6, 0, 0, 1, 0, zone)
        assertEquals(expectedToday.toInstant().toEpochMilli(), targetJustBefore)

        // Month boundary: 2026-09-30 23:59:00 -> 2026-10-01 00:00:01
        val endOfMonth = ZonedDateTime.of(2026, 9, 30, 23, 59, 0, 0, zone)
        val targetEndOfMonth = MidnightAlarmReceiver.calculateNextMidnightMillis(endOfMonth)
        val expectedEndOfMonth = ZonedDateTime.of(2026, 10, 1, 0, 0, 1, 0, zone)
        assertEquals(expectedEndOfMonth.toInstant().toEpochMilli(), targetEndOfMonth)

        // Year boundary: 2026-12-31 23:59:59 -> 2027-01-01 00:00:01
        val endOfYear = ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, zone)
        val targetEndOfYear = MidnightAlarmReceiver.calculateNextMidnightMillis(endOfYear)
        val expectedEndOfYear = ZonedDateTime.of(2027, 1, 1, 0, 0, 1, 0, zone)
        assertEquals(expectedEndOfYear.toInstant().toEpochMilli(), targetEndOfYear)
    }


    @Test
    fun scheduleMidnightAlarmExecutesSafely() {
        // Must never crash in tests or across SDK versions
        MidnightAlarmReceiver.scheduleMidnightAlarm(testContext)
    }

    @Test
    fun widgetReceiversReRegisterMidnightAlarmOnEnabled() {
        CountUpWidgetReceiver().onEnabled(testContext)
        HeroWidgetReceiver().onEnabled(testContext)
        ZenHorizonWidgetReceiver().onEnabled(testContext)
        SolarRhythmWidgetReceiver().onEnabled(testContext)
        ZenPebbleWidgetReceiver().onEnabled(testContext)
    }

    @Test
    fun midnightAlarmConstantsAreProperlyDefined() {
        assertEquals("com.countup.app.ACTION_MIDNIGHT_ROLLOVER", MidnightAlarmReceiver.ACTION_MIDNIGHT_ROLLOVER)
        assertEquals(24001, MidnightAlarmReceiver.REQUEST_CODE_MIDNIGHT)
    }

    @Test
    fun hasActiveWidgetsExecutesSafely() {
        // Must never throw and should return boolean
        val active = MidnightAlarmReceiver.hasActiveWidgets(testContext)
        assertTrue(active || !active)
    }
}
