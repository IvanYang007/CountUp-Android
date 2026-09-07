package com.countup.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.SizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SolarRhythmWidgetTest {

    @Test
    fun solarRhythmWidgetDeclaresResponsiveSizesFor4x2And2x2() {
        val widget = SolarRhythmWidget()
        val sizes = widget.sizeMode.sizes
        assertEquals(2, sizes.size)
        assertTrue(sizes.contains(DpSize(140.dp, 110.dp))) // 2x2
        assertTrue(sizes.contains(DpSize(260.dp, 110.dp))) // 4x2
    }

    @Test
    fun solarRhythmReceiverProvidesValidWidgetInstance() {
        val receiver = SolarRhythmWidgetReceiver()
        assertNotNull(receiver.glanceAppWidget)
        assertTrue(receiver.glanceAppWidget is SolarRhythmWidget)
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
}
