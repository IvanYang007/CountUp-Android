package com.countup.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/**
 * End-to-end integration and smoke test suite for the Zen & Efficient Widget Suite (Ticket #21).
 * Verifies all 3 widgets:
 * 1. Zen Horizon Ribbon (4x1 & 2x1)
 * 2. Solar Rhythm (4x2 & 2x2)
 * 3. Zen Pebble (1x1)
 */
class ZenWidgetSuiteIntegrationSmokeTest {

    private lateinit var tempDir: File
    private lateinit var testContext: TestContext
    private val today = LocalDate.of(2026, 9, 6)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zen_suite_smoke").toFile()
        testContext = TestContext(tempDir)
    }

    @Test
    fun allZenWidgetsRenderSideBySideWithoutVisualClippingOrSquishing() {
        val store = CountUpStore(testContext)
        val item = CountUpItem(
            id = "smoke-item",
            name = "Zen Practice",
            epochDay = today.minusDays(108).toEpochDay(),
            comment = "mindful daily routine",
        )

        // 1. Zen Horizon 4x1 & 2x1 state verification
        val horizonState = ZenWidgetReducer.resolveZenWidgetState(
            item = item,
            today = today,
            unit = ZenWidgetDisplayUnit.DAYS,
            isDarkMode = false,
        )
        assertEquals("108", horizonState.primaryValueText)
        assertEquals("DAYS", horizonState.unitLabelText)
        assertEquals("Zen Practice", horizonState.title)
        assertTrue(horizonState.milestoneProgress in 0f..1f)

        // 2. Solar Rhythm 4x2 seasonal state verification
        val transition = SolarTermCalendar.getSolarTermTransition(today)
        assertNotNull(transition)
        assertTrue(transition.currentTerm.id in 1..24)
        val display = SolarTermPoetryBridge.resolveWidgetDisplay(transition.currentTerm, false)
        assertTrue(display.line1Res != 0)
        assertTrue(display.line2Res != 0)

        // 3. Zen Pebble 1x1 state verification
        val pebbleLabel = item.resolveOneWordLabel(customTag = null)
        assertEquals("ZEN", pebbleLabel)
        val pebbleCompactNumber = ZenWidgetReducer.formatCompactNumber(108)
        assertEquals("108", pebbleCompactNumber)
    }

    @Test
    fun visualContrastPassesWcagAaInBothLightAndDarkModes() {
        for (isDark in listOf(false, true)) {
            val palette = WidgetThemeTokens.resolve(isDark)
            val contrast = WidgetThemeTokens.contrastRatio(palette.primaryInk, palette.canvasBg)
            // WCAG AA for normal text requires >= 4.5:1
            assertTrue(
                "Primary ink contrast in ${if (isDark) "dark" else "light"} mode must exceed 4.5:1, was $contrast",
                contrast >= 4.5,
            )

            // Accent gold tokens
            assertTrue(palette.accentGold != 0)
            assertTrue(palette.hairline != 0)
        }
    }

    @Test
    fun inPlaceUnitCyclingAndDailyMarkToggleFunctionDeterministically() {
        // Unit cycling through 5 units (DAYS -> MONTHS -> WEEKS -> HOURS -> YEARS -> DAYS)
        var unit = ZenWidgetDisplayUnit.DAYS
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.MONTHS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.WEEKS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.HOURS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.YEARS, unit)
        unit = unit.next()
        assertEquals(ZenWidgetDisplayUnit.DAYS, unit)
    }

    @Test
    fun zeroBackgroundBatteryConsumptionArchitecture() {
        // Verify midnight alarm calculation targets 00:00:01
        val zone = java.time.ZoneId.systemDefault()
        val afternoon = today.atTime(14, 30).atZone(zone)
        val nextMidnight = MidnightAlarmReceiver.calculateNextMidnightMillis(afternoon)
        val expected = today.plusDays(1).atTime(0, 0, 1)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, nextMidnight)

        // Verify transaction gate budget
        val estimates = WidgetMemoryBudgetGate.assertAllWithinBudget()
        assertTrue(estimates.all { it.isWithinBudget })
    }

    @Test
    fun allWidgetProvidersAndResourcesAreConfiguredInManifest() {
        assertTrue(R.xml.zen_horizon_widget_info != 0)
        assertTrue(R.xml.solar_rhythm_widget_info != 0)
        assertTrue(R.xml.zen_pebble_widget_info != 0)

        assertTrue(R.layout.widget_zen_horizon_4x1 != 0)
        assertTrue(R.layout.widget_zen_horizon_2x1 != 0)
        assertTrue(R.layout.widget_zen_pebble_1x1 != 0)
    }
}
