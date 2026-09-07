package com.countup.app

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contract test enforcing the hard invariants specified in docs/RECURRING_ISSUES.md.
 * Prevents regressions where refactoring or cleanup passes inadvertently alter
 * resizeMode or widgetFeatures on widget XML definitions.
 */
class WidgetContractInvariantsTest {

    @Test
    fun zenPebbleWidgetEnforcesResizeModeNoneAndConfigurationOptional() {
        val candidates = listOf(
            File("app/src/main/res/xml/zen_pebble_widget_info.xml"),
            File("src/main/res/xml/zen_pebble_widget_info.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("zen_pebble_widget_info.xml must exist", xmlFile)
        val xmlContent = xmlFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "zen_pebble_widget_info.xml MUST specify android:resizeMode=\"none\" to prevent snapping to slot 0 on physical OEM launchers",
            xmlContent.contains("android:resizeMode=\"none\"")
        )
        assertTrue(
            "zen_pebble_widget_info.xml MUST include configuration_optional to allow direct targeted cell drops",
            xmlContent.contains("configuration_optional")
        )
        assertTrue(
            "zen_pebble_widget_info.xml MUST include reconfigurable",
            xmlContent.contains("reconfigurable")
        )
        assertTrue(
            "zen_pebble_widget_info.xml MUST specify ZenPebbleConfigureActivity",
            xmlContent.contains("android:configure=\"com.countup.app.ZenPebbleConfigureActivity\"")
        )
    }

    @Test
    fun allConfigurableWidgetsDeclareConfigureActivity() {
        val widgetXmls = listOf(
            "zen_pebble_widget_info.xml" to "ZenPebbleConfigureActivity",
            "zen_horizon_widget_info.xml" to "ZenHorizonConfigureActivity",
            "solar_rhythm_widget_info.xml" to "SolarRhythmConfigureActivity",
            "hero_widget_info.xml" to "HeroWidgetConfigureActivity",
        )

        for ((xmlName, activityName) in widgetXmls) {
            val candidates = listOf(
                File("app/src/main/res/xml/$xmlName"),
                File("src/main/res/xml/$xmlName"),
            )
            val xmlFile = candidates.firstOrNull { it.exists() }
            assertNotNull("$xmlName must exist", xmlFile)
            val content = xmlFile!!.readText(Charsets.UTF_8)

            assertTrue(
                "$xmlName must declare android:configure pointing to $activityName",
                content.contains("android:configure=\"com.countup.app.$activityName\"")
            )
        }
    }
}
