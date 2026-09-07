package com.countup.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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
                val store = CountUpStore(applicationContext)
                return CountUpViewModel(DefaultCountUpRepository(store)) as T
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
                val snackbarHostState = remember { SnackbarHostState() }

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
                        }
                    }
                }

                CountUpContent(
                    state = state,
                    onEvent = viewModel::onEvent,
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
        } else if (isAdd) {
            viewModel.onEvent(CountUpUiEvent.OpenEditor(target = null))
        } else {
            val pinProviderClass = when (intent.data?.toString()) {
                "countup://pin_hero" -> HeroWidgetReceiver::class.java
                "countup://pin_pebble" -> ZenPebbleWidgetReceiver::class.java
                "countup://pin_solar" -> SolarRhythmWidgetReceiver::class.java
                else -> null
            }
            if (pinProviderClass != null) {
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
        refreshWidget()
    }

    override fun onPause() {
        super.onPause()
        refreshWidget()
    }

    private fun refreshWidget() {
        val appContext = applicationContext
        widgetReceiverScope.launch {
            runCatching { pushWidgetUpdate(appContext) }
            runCatching { pushAllHeroWidgetsUpdate(appContext) }
            runCatching { pushAllZenHorizonWidgetsUpdate(appContext) }
            runCatching { pushAllSolarRhythmWidgetsUpdate(appContext) }
            runCatching { pushAllZenPebbleWidgetsUpdate(appContext) }
            runCatching { MidnightAlarmReceiver.scheduleMidnightAlarm(appContext) }
        }
    }
}
