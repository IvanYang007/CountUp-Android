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
    fun shuinWidgetEnforcesResizeModeNoneAndReconfigurable() {
        val candidates = listOf(
            File("app/src/main/res/xml/shuin_widget_info.xml"),
            File("src/main/res/xml/shuin_widget_info.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("shuin_widget_info.xml must exist", xmlFile)
        val xmlContent = xmlFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "shuin_widget_info.xml MUST specify android:resizeMode=\"none\" to prevent snapping to slot 0 on physical OEM launchers",
            xmlContent.contains("android:resizeMode=\"none\"")
        )
        assertTrue(
            "shuin_widget_info.xml MUST include reconfigurable",
            xmlContent.contains("reconfigurable")
        )
        assertTrue(
            "shuin_widget_info.xml MUST NOT include configuration_optional because it suppresses auto-launch of configure activity on drop",
            !xmlContent.contains("configuration_optional")
        )
        assertTrue(
            "shuin_widget_info.xml MUST specify ShuinConfigureActivity",
            xmlContent.contains("android:configure=\"com.countup.app.ShuinConfigureActivity\"")
        )
    }

    @Test
    fun allConfigurableWidgetsDeclareConfigureActivity() {
        val widgetXmls = listOf(
            "zen_pebble_widget_info.xml" to "ZenPebbleConfigureActivity",
            "zen_horizon_widget_info.xml" to "ZenHorizonConfigureActivity",
            "solar_rhythm_widget_info.xml" to "SolarRhythmConfigureActivity",
            "hero_widget_info.xml" to "HeroWidgetConfigureActivity",
            "zen_orbit_widget_info.xml" to "ZenOrbitConfigureActivity",
            "tsukimi_widget_info.xml" to "TsukimiConfigureActivity",
            "shuin_widget_info.xml" to "ShuinConfigureActivity",
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
    fun overviewWidgetUsesAmbientDropWithNoConfigureActivity() {
        val candidates = listOf(
            File("app/src/main/res/xml/haircut_widget_info.xml"),
            File("src/main/res/xml/haircut_widget_info.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
        assertNotNull("haircut_widget_info.xml must exist", xmlFile)
        val content = xmlFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "haircut_widget_info.xml must omit android:configure to enable instant ambient drop with zero setup friction",
            !content.contains("android:configure")
        )
    }

    @Test
    fun overviewWidgetDeclaresTitleContainerAndFilterDot() {
        val candidates = listOf(
            File("app/src/main/res/layout/countup_widget.xml"),
            File("src/main/res/layout/countup_widget.xml"),
        )
        val layoutFile = candidates.firstOrNull { it.exists() }
        assertNotNull("countup_widget.xml must exist", layoutFile)
        val content = layoutFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "countup_widget.xml must declare widget_title_container for in-situ suite switching",
            content.contains("android:id=\"@+id/widget_title_container\"")
        )
        assertTrue(
            "countup_widget.xml must declare widget_filter_dot affordance indicator",
            content.contains("android:id=\"@+id/widget_filter_dot\"")
        )
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
            "Pebble tag must use maxLines=2 and ellipsize with autoSizeTextType to support 2-line wrapping",
            content.contains("android:ellipsize=\"end\"") &&
                content.contains("android:maxLines=\"2\"") &&
                content.contains("android:autoSizeTextType=\"uniform\"")
        )
    }

    @Test
    fun allCompactWidgetsSupportTwoLineWrapping() {
        val widgetLayouts = listOf(
            "widget_zen_pebble_1x1.xml",
            "widget_shuin_1x1.xml",
            "widget_tsukimi_2x2.xml",
            "widget_zen_orbit_2x2.xml",
        )
        for (layoutName in widgetLayouts) {
            val candidates = listOf(
                File("app/src/main/res/layout/$layoutName"),
                File("src/main/res/layout/$layoutName"),
            )
            val file = candidates.firstOrNull { it.exists() }
            assertNotNull("$layoutName must exist", file)
            val content = file!!.readText(Charsets.UTF_8)
            assertTrue(
                "$layoutName must support 2-line wrapping with maxLines=\"2\"",
                content.contains("android:maxLines=\"2\"")
            )
        }
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

    @Test
    fun manifestEnforcesZeroPermissions() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val manifestContent = manifestFile!!.readText(Charsets.UTF_8)

        // All declared <uses-permission> elements must have tools:node="remove" to prevent transitive library permissions
        val permissionRegex = Regex("""<uses-permission\s+([^>]+)/>""")
        val matches = permissionRegex.findAll(manifestContent).toList()
        assertTrue("Manifest must have defensive permission removal elements", matches.isNotEmpty())
        for (match in matches) {
            val element = match.groupValues[1]
            assertTrue(
                "Every uses-permission element must be explicitly removed with tools:node=\"remove\" ($element)",
                element.contains("""tools:node="remove"""")
            )
        }
    }

    @Test
    fun allWidgetsDeclareValidPreviewLayoutAndPreviewImage() {
        val widgetXmls = listOf(
            Triple("zen_pebble_widget_info.xml", "widget_zen_pebble_1x1", "zen_pebble_widget_preview"),
            Triple("zen_horizon_widget_info.xml", "widget_zen_horizon_4x1", "zen_horizon_widget_preview"),
            Triple("solar_rhythm_widget_info.xml", "widget_solar_rhythm_4x2", "solar_rhythm_widget_preview"),
            Triple("hero_widget_info.xml", "countup_hero_widget_2x1", "hero_widget_preview"),
            Triple("haircut_widget_info.xml", "widget_preview_overview_4x2", "haircut_widget_preview"),
            Triple("zen_orbit_widget_info.xml", "widget_zen_orbit_2x2", "zen_orbit_widget_preview"),
            Triple("tsukimi_widget_info.xml", "widget_tsukimi_2x2", "preview_tsukimi_moon"),
            Triple("shuin_widget_info.xml", "widget_shuin_1x1", "preview_shuin_seal"),
        )

        for ((xmlName, layoutName, previewDrawableName) in widgetXmls) {
            val candidates = listOf(
                File("app/src/main/res/xml/$xmlName"),
                File("src/main/res/xml/$xmlName"),
            )
            val xmlFile = candidates.firstOrNull { it.exists() }
            assertNotNull("$xmlName must exist", xmlFile)
            val content = xmlFile!!.readText(Charsets.UTF_8)

            assertTrue(
                "$xmlName must declare android:previewLayout=\"@layout/$layoutName\"",
                content.contains("android:previewLayout=\"@layout/$layoutName\"")
            )
            assertTrue(
                "$xmlName must declare android:previewImage=\"@drawable/$previewDrawableName\"",
                content.contains("android:previewImage=\"@drawable/$previewDrawableName\"")
            )

            val layoutCandidates = listOf(
                File("app/src/main/res/layout/$layoutName.xml"),
                File("src/main/res/layout/$layoutName.xml"),
            )
            val layoutFile = layoutCandidates.firstOrNull { it.exists() }
            assertNotNull("Layout $layoutName.xml must exist", layoutFile)
            val layoutContent = layoutFile!!.readText(Charsets.UTF_8)
            assertTrue(
                "Layout $layoutName.xml must specify default populated android:text to prevent blank previews in launcher pickers",
                layoutContent.contains("android:text=")
            )

            val drawableCandidates = listOf(
                File("app/src/main/res/drawable/$previewDrawableName.xml"),
                File("src/main/res/drawable/$previewDrawableName.xml"),
            )
            assertTrue(
                "Preview drawable $previewDrawableName.xml must exist",
                drawableCandidates.any { it.exists() }
            )
        }
    }

    @Test
    fun allConfigurableWidgetsOmitConfigurationOptional() {
        val configurableWidgets = listOf(
            "zen_pebble_widget_info.xml",
            "zen_horizon_widget_info.xml",
            "solar_rhythm_widget_info.xml",
            "hero_widget_info.xml",
            "zen_orbit_widget_info.xml",
            "tsukimi_widget_info.xml",
            "shuin_widget_info.xml",
        )
        for (xmlName in configurableWidgets) {
            val candidates = listOf(
                File("app/src/main/res/xml/$xmlName"),
                File("src/main/res/xml/$xmlName"),
            )
            val xmlFile = candidates.firstOrNull { it.exists() }
            assertNotNull("$xmlName must exist", xmlFile)
            val content = xmlFile!!.readText(Charsets.UTF_8)
            assertTrue(
                "$xmlName MUST NOT include configuration_optional because it suppresses auto-launch of configure activity on drop",
                !content.contains("configuration_optional")
            )
            assertTrue(
                "$xmlName MUST declare widgetFeatures=\"reconfigurable\"",
                content.contains("reconfigurable")
            )
        }
    }

    @Test
    fun manifestStripsTransitiveStartupAndBootReceivers() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val content = manifestFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "AndroidManifest.xml must strip androidx.startup.InitializationProvider via tools:node=\"remove\" to prevent WorkManager startup crash",
            content.contains("androidx.startup.InitializationProvider") && content.contains("""tools:node="remove"""")
        )
        assertTrue(
            "AndroidManifest.xml must defensively strip RECEIVE_BOOT_COMPLETED to preserve zero-permission model",
            content.contains("android.permission.RECEIVE_BOOT_COMPLETED") && content.contains("""tools:node="remove"""")
        )
    }

    @Test
    fun manifestEnforcesZeroPermissionSpeechQueries() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val content = manifestFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "AndroidManifest.xml must declare <queries> for RecognitionService on Android 11+",
            content.contains("android.speech.RecognitionService")
        )
        assertTrue(
            "AndroidManifest.xml must declare <queries> for RECOGNIZE_SPEECH on Android 11+",
            content.contains("android.intent.action.RECOGNIZE_SPEECH")
        )
        assertTrue(
            "AndroidManifest.xml must NEVER declare RECORD_AUDIO permission",
            !content.contains("android.permission.RECORD_AUDIO")
        )
        assertTrue(
            "VoiceAddActivity must be private (exported=\"false\")",
            content.contains("""android:name=".VoiceAddActivity"""") && content.contains("""android:exported="false"""")
        )
    }

    @Test
    fun allConfigurableWidgetsDeclareExportedConfigureActivityWithFilter() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val content = manifestFile!!.readText(Charsets.UTF_8)

        val configureActivities = listOf(
            ".ZenPebbleConfigureActivity",
            ".ZenHorizonConfigureActivity",
            ".SolarRhythmConfigureActivity",
            ".HeroWidgetConfigureActivity",
            ".ZenOrbitConfigureActivity",
            ".TsukimiConfigureActivity",
            ".ShuinConfigureActivity",
        )
        for (activityName in configureActivities) {
            assertTrue(
                "AndroidManifest.xml must declare $activityName with exported=\"true\" for launcher configuration",
                content.contains(activityName) && content.contains("android.appwidget.action.APPWIDGET_CONFIGURE")
            )
        }
    }

    @Test
    fun composeCenteredLayoutEnforcesWidthInPrecedingFillSize() {
        val contentCandidates = listOf(
            File("app/src/main/java/com/countup/app/CountUpContent.kt"),
            File("src/main/java/com/countup/app/CountUpContent.kt"),
        )
        val file = contentCandidates.firstOrNull { it.exists() }
        assertNotNull("CountUpContent.kt must exist", file)
        val code = file!!.readText(Charsets.UTF_8)

        val widthInIndex = code.indexOf(".widthIn(max = 640.dp)")
        val fillMaxWidthIndex = code.indexOf(".fillMaxWidth()", startIndex = if (widthInIndex != -1) widthInIndex else 0)
        assertTrue(
            "CountUpContent layout must order .widthIn(max = 640.dp) before .fillMaxWidth() to avoid unbounded expansion on foldables/tablets",
            widthInIndex != -1 && fillMaxWidthIndex != -1 && widthInIndex < fillMaxWidthIndex
        )
    }

    @Test
    fun edgeToEdgeEliminatesDeprecatedWindowBarColorAPIs() {
        val codeCandidates = listOf(
            File("app/src/main/java/com/countup/app/EdgeToEdge.kt"),
            File("app/src/main/java/com/countup/app/MainActivity.kt"),
        )
        for (file in codeCandidates) {
            if (!file.exists()) continue
            val text = file.readText(Charsets.UTF_8)
            assertTrue(
                "${file.name} must NOT invoke deprecated setStatusBarColor for Android 15 compliance",
                !text.contains("setStatusBarColor")
            )
            assertTrue(
                "${file.name} must NOT invoke deprecated setNavigationBarColor for Android 15 compliance",
                !text.contains("setNavigationBarColor")
            )
        }
    }

    @Test
    fun proguardKeepsEnumMembersAndDataModels() {
        val proguardCandidates = listOf(
            File("app/proguard-rules.pro"),
            File("proguard-rules.pro"),
        )
        val file = proguardCandidates.firstOrNull { it.exists() }
        assertNotNull("proguard-rules.pro must exist", file)
        val content = file!!.readText(Charsets.UTF_8)

        assertTrue(
            "proguard-rules.pro must preserve TimeDisplayMode and ZenWidgetDisplayUnit fields under R8 (Lesson L4)",
            content.contains("TimeDisplayMode") &&
                content.contains("ZenWidgetDisplayUnit") &&
                content.contains("<fields>;")
        )
        assertTrue(
            "proguard-rules.pro must keep CountUpItem data model",
            content.contains("-keep class com.countup.app.CountUpItem { *; }")
        )
        assertTrue(
            "proguard-rules.pro must keep CountUpBackupPayload data model",
            content.contains("-keep class com.countup.app.CountUpBackupPayload { *; }")
        )
    }

    @Test
    fun actionRowDensityAndClippingInvariants() {
        val candidates = listOf(
            File("app/src/main/java/com/countup/app/CountUpContent.kt"),
            File("src/main/java/com/countup/app/CountUpContent.kt"),
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("CountUpContent.kt must exist", file)
        val code = file!!.readText(Charsets.UTF_8)

        // SubHeaderRow button sizing: 26.dp
        assertTrue("SubHeaderRow must use explicit 26.dp size on action buttons (Lesson L9)", code.contains(".size(26.dp)"))

        // Extract SubHeaderRow and ensure no minimumInteractiveComponentSize is used within it
        val subHeaderStart = code.indexOf("private fun SubHeaderRow(")
        val subHeaderEnd = code.indexOf("private fun MechanicalResetButton(", startIndex = subHeaderStart)
        assertTrue("SubHeaderRow function must exist", subHeaderStart != -1 && subHeaderEnd != -1)
        val subHeaderCode = code.substring(subHeaderStart, subHeaderEnd)
        assertTrue(
            "SubHeaderRow must strictly omit minimumInteractiveComponentSize to prevent horizontal layout blowout (Lesson L9)",
            !subHeaderCode.contains("minimumInteractiveComponentSize")
        )

        // MechanicalResetButton must use size(30.dp) without minimumInteractiveComponentSize
        val resetBtnStart = code.indexOf("private fun MechanicalResetButton(")
        val resetBtnEnd = code.indexOf("fun ItemCard(", startIndex = resetBtnStart)
        assertTrue("MechanicalResetButton function must exist", resetBtnStart != -1 && resetBtnEnd != -1)
        val resetBtnCode = code.substring(resetBtnStart, resetBtnEnd)
        assertTrue(
            "MechanicalResetButton must use explicit 30.dp size without minimumInteractiveComponentSize (Lesson L9)",
            resetBtnCode.contains(".size(30.dp)") && !resetBtnCode.contains("minimumInteractiveComponentSize")
        )
    }

    @Test
    fun allConfigureActivitiesEnforceIntentSecurityContracts() {
        val configureActivities = listOf(
            "ZenPebbleConfigureActivity.kt",
            "ZenHorizonConfigureActivity.kt",
            "SolarRhythmConfigureActivity.kt",
            "HeroWidgetConfigureActivity.kt",
            "ZenOrbitConfigureActivity.kt",
            "TsukimiConfigureActivity.kt",
            "ShuinConfigureActivity.kt",
        )

        for (filename in configureActivities) {
            val candidates = listOf(
                File("app/src/main/java/com/countup/app/$filename"),
                File("src/main/java/com/countup/app/$filename"),
            )
            val file = candidates.firstOrNull { it.exists() }
            assertNotNull("$filename must exist", file)
            val code = file!!.readText(Charsets.UTF_8)

            assertTrue(
                "$filename must set RESULT_CANCELED before processing to handle back navigation safely (Lesson L11)",
                code.contains("setResult(Activity.RESULT_CANCELED)")
            )
            assertTrue(
                "$filename must validate EXTRA_APPWIDGET_ID and finish on INVALID_APPWIDGET_ID (Lesson L11)",
                code.contains("EXTRA_APPWIDGET_ID") && code.contains("INVALID_APPWIDGET_ID")
            )
            assertTrue(
                "$filename must verify calling package ownership via getAppWidgetInfo (Lesson L11)",
                code.contains("widgetInfo.provider.packageName != packageName")
            )
            assertTrue(
                "$filename must return result containing only EXTRA_APPWIDGET_ID (Lesson L11)",
                code.contains("putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)") &&
                    code.contains("setResult(Activity.RESULT_OK")
            )
        }
    }

    @Test
    fun manifestRegistersMidnightAlarmReceiverWithRequiredActions() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val content = manifestFile!!.readText(Charsets.UTF_8)

        assertTrue(
            "AndroidManifest.xml must declare MidnightAlarmReceiver as non-exported",
            content.contains("""android:name=".MidnightAlarmReceiver"""") &&
                content.contains("""android:exported="false"""")
        )
        assertTrue(
            "MidnightAlarmReceiver must register ACTION_MIDNIGHT_ROLLOVER",
            content.contains("com.countup.app.ACTION_MIDNIGHT_ROLLOVER")
        )
        assertTrue(
            "MidnightAlarmReceiver must register TIME_SET and TIMEZONE_CHANGED",
            content.contains("android.intent.action.TIME_SET") &&
                content.contains("android.intent.action.TIMEZONE_CHANGED")
        )
        assertTrue(
            "MidnightAlarmReceiver must register MY_PACKAGE_REPLACED to restore rollover across updates without boot permission (Lesson L12/L14)",
            content.contains("android.intent.action.MY_PACKAGE_REPLACED")
        )
    }

    @Test
    fun allWidgetProvidersImplementOnRestoredDelegation() {
        val receiverFiles = listOf(
            "HeroWidgetReceiver.kt",
            "ZenHorizonWidgetReceiver.kt",
            "SolarRhythmWidget.kt",
            "ZenPebbleWidgetReceiver.kt",
            "CountUpWidget.kt",
            "ZenOrbitWidgetReceiver.kt",
            "TsukimiWidgetReceiver.kt",
            "ShuinWidgetReceiver.kt",
        )

        for (filename in receiverFiles) {
            val candidates = listOf(
                File("app/src/main/java/com/countup/app/$filename"),
                File("src/main/java/com/countup/app/$filename"),
            )
            val file = candidates.firstOrNull { it.exists() }
            assertNotNull("$filename must exist", file)
            val code = file!!.readText(Charsets.UTF_8)

            assertTrue(
                "$filename must implement onRestored(context, oldWidgetIds, newWidgetIds) to survive device restores (Lesson L13)",
                code.contains("override fun onRestored(")
            )
            assertTrue(
                "$filename must delegate onRestored to CountUpStore.remapWidgetBindings",
                code.contains("remapWidgetBindings(")
            )
        }
    }

    @Test
    fun allPreviewLayoutStringsExistInAllSevenLocalesAndStaticTracksPresent() {
        val localeDirs = listOf(
            "values",
            "values-b+zh+Hans",
            "values-b+zh+Hant",
            "values-zh",
            "values-zh-rCN",
            "values-zh-rHK",
            "values-zh-rTW",
        )

        val requiredPreviewStrings = listOf(
            "preview_item_title",
            "preview_item_tag",
            "preview_item_subtitle",
            "preview_solar_quote",
            "preview_solar_term_day",
            "preview_solar_event_title",
            "preview_grid_item_1",
            "preview_grid_item_2",
            "preview_grid_item_3",
            "preview_grid_item_4",
            "preview_grid_item_5",
            "preview_grid_item_6",
            "preview_zen_orbit_pill",
        )

        for (dir in localeDirs) {
            val candidates = listOf(
                File("app/src/main/res/$dir/strings.xml"),
                File("src/main/res/$dir/strings.xml"),
            )
            val file = candidates.firstOrNull { it.exists() }
            assertNotNull("Locale $dir/strings.xml must exist", file)
            val content = file!!.readText(Charsets.UTF_8)

            for (stringKey in requiredPreviewStrings) {
                assertTrue(
                    "Locale $dir/strings.xml MUST define string resource '$stringKey' for authentic widget previews (Lesson L17)",
                    content.contains("""name="$stringKey"""")
                )
            }
        }

        val trackCandidates = listOf(
            "preview_zen_horizon_track.xml",
            "preview_solar_timeline_track.xml",
        )
        for (track in trackCandidates) {
            val file = listOf(
                File("app/src/main/res/drawable/$track"),
                File("src/main/res/drawable/$track"),
            ).firstOrNull { it.exists() }
            assertNotNull("Static preview track $track must exist in res/drawable (Lesson L17)", file)
        }

        // Verify collection widget uses dedicated static preview layout
        val haircutInfo = listOf(
            File("app/src/main/res/xml/haircut_widget_info.xml"),
            File("src/main/res/xml/haircut_widget_info.xml"),
        ).firstOrNull { it.exists() }
        assertNotNull("haircut_widget_info.xml must exist", haircutInfo)
        assertTrue(
            "haircut_widget_info.xml must use dedicated static previewLayout widget_preview_overview_4x2 (Lesson L17)",
            haircutInfo!!.readText(Charsets.UTF_8).contains("android:previewLayout=\"@layout/widget_preview_overview_4x2\"")
        )
    }

    @Test
    fun edgeToEdgeEnforcesAlwaysCutoutAndDisablesContrastEnforcement() {
        val candidates = listOf(
            File("app/src/main/java/com/countup/app/EdgeToEdge.kt"),
            File("src/main/java/com/countup/app/EdgeToEdge.kt"),
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("EdgeToEdge.kt must exist", file)
        val code = file!!.readText(Charsets.UTF_8)

        assertTrue(
            "EdgeToEdge.kt must use LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS for modern cutout compliance (Lesson L18)",
            code.contains("LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS")
        )
        assertTrue(
            "EdgeToEdge.kt must NOT assign deprecated SHORT_EDGES cutout mode (Lesson L18)",
            !code.contains("= WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES")
        )
        assertTrue(
            "EdgeToEdge.kt must disable navigation bar contrast enforcement on Android 10+ (Lesson L18)",
            code.contains("window.isNavigationBarContrastEnforced = false")
        )
    }
}
