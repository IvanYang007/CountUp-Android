package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SolarRhythmWidgetTest {

    @Test
    fun solarRhythmReceiverIsInstantiable() {
        val receiver = SolarRhythmWidgetReceiver()
        assertNotNull(receiver)
    }

    @Test
    fun solarRhythmTrackRendererHandlesProgressGracefullyWithoutExceptions() {
        for (progress in listOf(0.0f, 0.25f, 0.5f, 0.72f, 1.0f, -0.1f, 1.5f)) {
            val result = SolarRhythmTrackRenderer.renderTrack(
                progress = progress,
                progressColor = 0xFF8A5A36.toInt(),
                trackColor = 0x1F000000,
                widthPx = 200,
                heightPx = 16,
            )
            if (result != null) {
                assertEquals(200, result.width)
                assertEquals(16, result.height)
            }
        }
    }

    @Test
    fun seasonalTimelineCalculationIntegratesProperlyAcrossDates() {
        // Benchmark: Autumn Equinox 2026-09-23
        val autumnDate = LocalDate.of(2026, 9, 23)
        val transition = SolarTermCalendar.getSolarTermTransition(autumnDate)
        assertEquals(16, transition.currentTerm.id) // Autumnal Equinox
        assertEquals(17, transition.nextTerm.id) // Cold Dew

        // Palette resolution
        val lightPalette = SolarTermPoetryBridge.resolveSeasonalPalette(transition.currentTerm.seasonRes, false)
        val darkPalette = SolarTermPoetryBridge.resolveSeasonalPalette(transition.currentTerm.seasonRes, true)

        assertEquals(0xFF8A5A36.toInt(), lightPalette.primaryTint)
        assertEquals(0xFFD69F7E.toInt(), darkPalette.primaryTint)

        // Snapshot display resolution
        val display = SolarTermPoetryBridge.resolveWidgetDisplay(transition.currentTerm, false)
        assertEquals(R.string.solar_term_autumnal_equinox, display.nameRes)
        assertEquals(R.string.solar_term_whisper_16_line1, display.line1Res)
        assertEquals(R.string.solar_term_whisper_16_line2, display.line2Res)
    }

    @Test
    fun solarRhythmResourceAssetsAreConfigured() {
        assertTrue(R.xml.solar_rhythm_widget_info != 0)
        assertTrue(R.string.solar_rhythm_widget_label != 0)
        assertTrue(R.string.solar_rhythm_widget_description != 0)
        assertTrue(R.string.solar_rhythm_day_of != 0)
        assertTrue(R.layout.widget_solar_rhythm_4x2 != 0)
        assertTrue(R.layout.widget_solar_rhythm_2x2 != 0)
    }

    @Test
    fun whisperQuoteFormatsPunctuationAppropriatelyForChineseAndEnglish() {
        val zhQuote = formatSolarWhisperQuote("暑气渐消秋意静", "蜻蜓低掠晚霞明")
        assertEquals("“暑气渐消秋意静，蜻蜓低掠晚霞明”", zhQuote)

        val enQuote = formatSolarWhisperQuote("Cool breezes arrive", "morning dew glistens quietly on grass")
        assertEquals("\"Cool breezes arrive;\nmorning dew glistens quietly on grass\"", enQuote)

        assertEquals("", formatSolarWhisperQuote("", ""))
        assertEquals("Single line", formatSolarWhisperQuote("Single line", ""))
    }

    @Test
    fun solarRhythmDateSubLabelAdheresToStandardsContract() {
        val today = LocalDate.of(2026, 9, 23)
        val pastDate = LocalDate.of(2025, 1, 20)
        val futureDate = LocalDate.of(2026, 12, 31)

        val pastCount = daysSince(pastDate, today)
        assertTrue(pastCount > 0)
        val pastSubLabel = formatAnchorDateSubLabel(pastCount, pastDate, "SINCE %s", "UNTIL %s")
        assertTrue(pastSubLabel.startsWith("SINCE "))

        val futureCount = daysSince(futureDate, today)
        assertTrue(futureCount < 0)
        val futureSubLabel = formatAnchorDateSubLabel(futureCount, futureDate, "SINCE %s", "UNTIL %s")
        assertTrue(futureSubLabel.startsWith("UNTIL "))
    }

    @Test
    fun safeDimensionsCalculationRespectsStrictIpcLimits() {
        // Under limit: unchanged
        val (w1, h1) = ZenHorizonTrackRenderer.computeSafeDimensions(200, 16, 32 * 1024)
        assertEquals(200, w1)
        assertEquals(16, h1)

        // Massive dimensions: scaled down to fit within limit
        val (w2, h2) = ZenHorizonTrackRenderer.computeSafeDimensions(2000, 1000, 32 * 1024)
        val bytes = w2 * h2 * 4
        assertTrue(bytes <= 32 * 1024)
    }
}

