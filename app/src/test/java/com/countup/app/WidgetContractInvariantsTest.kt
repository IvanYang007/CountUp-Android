package com.countup.app

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contract test enforcing the hard invariants specified in docs/RECURRING_ISSUES.md.
 * Prevents regressions where refactoring or cleanup passes inadvertently alter
 * resizeMode, widgetFeatures, corner radius, or content sizing on widget XML definitions.
 */
class WidgetContractInvariantsTest {

    @Test
    fun zenPebbleWidgetEnforcesResizeModeNoneAndReconfigurable() {
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
            "zen_pebble_widget_info.xml MUST include reconfigurable",
            xmlContent.contains("reconfigurable")
        )
        assertTrue(
            "zen_pebble_widget_info.xml MUST NOT include configuration_optional because it suppresses auto-launch of configure activity on drop",
            !xmlContent.contains("configuration_optional")
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

    @Test
    fun zenPebbleBackgroundCornerRadiusMustNotExceed16dp() {
        val candidates = listOf(
            File("app/src/main/res/drawable/widget_zen_bg.xml"),
            File("src/main/res/drawable/widget_zen_bg.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("widget_zen_bg.xml must exist", xmlFile)
        val content = xmlFile!!.readText(Charsets.UTF_8)

        // Extract the radius value and ensure it's <= 16dp
        val radiusMatch = Regex("""android:radius="(\d+)dp"""").find(content)
        assertNotNull("widget_zen_bg.xml must declare a corner radius", radiusMatch)
        val radiusValue = radiusMatch!!.groupValues[1].toInt()
        assertTrue(
            "Corner radius must be <= 16dp for universal OEM launcher compatibility (was ${radiusValue}dp)",
            radiusValue <= 16
        )
    }

    @Test
    fun zenPebbleLayoutUsesAutoSizeText() {
        val candidates = listOf(
            File("app/src/main/res/layout/widget_zen_pebble_1x1.xml"),
            File("src/main/res/layout/widget_zen_pebble_1x1.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("widget_zen_pebble_1x1.xml must exist", xmlFile)
        val content = xmlFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "Pebble number TextView must use autoSizeTextType=\"uniform\" for universal OEM cell fit",
            content.contains("android:autoSizeTextType=\"uniform\"")
        )
        assertTrue(
            "Pebble tag must use singleLine and ellipsize to prevent overflow on tight cells",
            content.contains("android:ellipsize=\"end\"") && content.contains("android:singleLine=\"true\"")
        )
    }

    @Test
    fun manifestAndBackupRulesEnforceSecureAutoBackup() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val manifestContent = manifestFile!!.readText(Charsets.UTF_8)
        assertTrue(
            "AndroidManifest.xml must set android:allowBackup=\"true\" for cross-device transfer",
            manifestContent.contains("android:allowBackup=\"true\"")
        )
        assertTrue(
            "AndroidManifest.xml must point to @xml/data_extraction_rules",
            manifestContent.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\"")
        )
        assertTrue(
            "AndroidManifest.xml must point to @xml/backup_rules",
            manifestContent.contains("android:fullBackupContent=\"@xml/backup_rules\"")
        )

        val extractionCandidates = listOf(
            File("app/src/main/res/xml/data_extraction_rules.xml"),
            File("src/main/res/xml/data_extraction_rules.xml"),
        )
        val extractionFile = extractionCandidates.firstOrNull { it.exists() }
        assertNotNull("data_extraction_rules.xml must exist", extractionFile)
        val extractionContent = extractionFile!!.readText(Charsets.UTF_8)
        assertTrue(extractionContent.contains("<include domain=\"sharedpref\" path=\"countup_prefs.xml\" />"))
        assertTrue(extractionContent.contains("<include domain=\"file\" path=\"countup_backup.json\" />"))

        val backupRulesCandidates = listOf(
            File("app/src/main/res/xml/backup_rules.xml"),
            File("src/main/res/xml/backup_rules.xml"),
        )
        val backupRulesFile = backupRulesCandidates.firstOrNull { it.exists() }
        assertNotNull("backup_rules.xml must exist", backupRulesFile)
        val backupRulesContent = backupRulesFile!!.readText(Charsets.UTF_8)
        assertTrue(backupRulesContent.contains("<include domain=\"sharedpref\" path=\"countup_prefs.xml\" />"))
        assertTrue(backupRulesContent.contains("<include domain=\"file\" path=\"countup_backup.json\" />"))
    }
}
