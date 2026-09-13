package com.countup.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Mid-century-modern multi-item screen container.
 * Follows 2026 Android MVI guidelines: Activity acts as a lifecycle host, delegating
 * business logic and state to [CountUpViewModel] and UI rendering to [CountUpContent].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CountUpViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val store = CountUpStore.getInstance(applicationContext)
                return CountUpViewModel(DefaultCountUpRepository(store)) as T
            }
        }
    }

    private val snackbarHostState = SnackbarHostState()

    private val appVersion: String by lazy {
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "3.0.0"
        } catch (_: Exception) {
            "3.0.0"
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.let { stream ->
                    viewModel.onEvent(CountUpUiEvent.ExportBackupToStream(stream))
                } ?: run {
                    lifecycleScope.launch {
                        snackbarHostState.showSnackbar(getString(R.string.backup_io_error))
                    }
                }
            } catch (_: Exception) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(getString(R.string.backup_io_error))
                }
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.let { stream ->
                    viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(stream))
                } ?: run {
                    lifecycleScope.launch {
                        snackbarHostState.showSnackbar(getString(R.string.backup_io_error))
                    }
                }
            } catch (_: Exception) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(getString(R.string.backup_io_error))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = state.themeMode.isDark(systemDark)

            ZenTheme(darkTheme = isDark) {
                LaunchedEffect(Unit) {
                    handleIntent(intent)
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is CountUpUiEffect.ShowSnackbar -> {
                                launch {
                                    val message = when {
                                        effect.formatArgRes != null -> getString(effect.messageRes, getString(effect.formatArgRes))
                                        effect.formatArg != null -> getString(effect.messageRes, effect.formatArg)
                                        else -> getString(effect.messageRes)
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                            CountUpUiEffect.RefreshWidget -> {
                                refreshWidget()
                            }
                            is CountUpUiEffect.TriggerExportDocument -> {
                                try {
                                    exportBackupLauncher.launch(effect.defaultFilename)
                                } catch (_: ActivityNotFoundException) {
                                    launch {
                                        snackbarHostState.showSnackbar(getString(R.string.error_no_file_picker))
                                    }
                                }
                            }
                            is CountUpUiEffect.TriggerImportDocument -> {
                                try {
                                    importBackupLauncher.launch(effect.mimeTypes)
                                } catch (_: ActivityNotFoundException) {
                                    launch {
                                        snackbarHostState.showSnackbar(getString(R.string.error_no_file_picker))
                                    }
                                } catch (_: SecurityException) {
                                    launch {
                                        snackbarHostState.showSnackbar(getString(R.string.error_no_file_picker))
                                    }
                                }
                            }
                        }
                    }
                }

                CountUpContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    appVersion = appVersion,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val targetItemId = intent.getStringExtra(WidgetNavigationContract.EXTRA_TARGET_ITEM_ID)
        val isAdd = intent.action == ACTION_ADD_ITEM ||
            intent.data?.toString() == "countup://new"
        if (targetItemId != null) {
            viewModel.onEvent(CountUpUiEvent.OpenTargetItem(targetItemId))
            intent.removeExtra(WidgetNavigationContract.EXTRA_TARGET_ITEM_ID)
        } else if (isAdd) {
            viewModel.onEvent(CountUpUiEvent.OpenEditor(target = null))
            intent.action = null
            intent.data = null
        } else {
            val pinProviderClass = when (intent.data?.toString()) {
                "countup://pin_hero" -> HeroWidgetReceiver::class.java
                "countup://pin_pebble" -> ZenPebbleWidgetReceiver::class.java
                "countup://pin_solar" -> SolarRhythmWidgetReceiver::class.java
                else -> null
            }
            if (pinProviderClass != null) {
                intent.data = null
                val manager = getSystemService(android.appwidget.AppWidgetManager::class.java)
                if (manager.isRequestPinAppWidgetSupported) {
                    val myProvider = android.content.ComponentName(this, pinProviderClass)
                    manager.requestPinAppWidget(myProvider, null, null)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(CountUpUiEvent.Refresh)
        refreshWidget(immediate = true)
    }

    override fun onPause() {
        super.onPause()
        refreshWidget(immediate = true)
    }

    private var refreshWidgetJob: kotlinx.coroutines.Job? = null

    private fun refreshWidget(immediate: Boolean = false) {
        val appContext = applicationContext
        refreshWidgetJob?.cancel()
        refreshWidgetJob = widgetReceiverScope.launch {
            if (!immediate) {
                kotlinx.coroutines.delay(350L)
            }
            runCatching { CountUpStore.getInstance(appContext).sanitizeOrphanedWidgetBindings(appContext) }
            runCatching { pushWidgetUpdate(appContext) }
            runCatching { pushAllHeroWidgetsUpdate(appContext) }
            runCatching { pushAllZenHorizonWidgetsUpdate(appContext) }
            runCatching { pushAllSolarRhythmWidgetsUpdate(appContext) }
            runCatching { pushAllZenPebbleWidgetsUpdate(appContext) }
            runCatching { MidnightAlarmReceiver.scheduleMidnightAlarm(appContext) }
        }
    }
}
