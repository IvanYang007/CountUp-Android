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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ZenTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    handleIntent(intent)
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is CountUpUiEffect.ShowSnackbar -> {
                                val message = when {
                                    effect.formatArgRes != null -> getString(effect.messageRes, getString(effect.formatArgRes))
                                    effect.formatArg != null -> getString(effect.messageRes, effect.formatArg)
                                    else -> getString(effect.messageRes)
                                }
                                snackbarHostState.showSnackbar(message)
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
        val isAdd = intent.action == ACTION_ADD_ITEM ||
            intent.data?.toString() == "countup://new"
        if (isAdd) {
            viewModel.onEvent(CountUpUiEvent.OpenEditor(target = null))
        } else if (intent.data?.toString() == "countup://pin_hero") {
            val manager = getSystemService(android.appwidget.AppWidgetManager::class.java)
            if (manager.isRequestPinAppWidgetSupported) {
                val myProvider = android.content.ComponentName(this, HeroWidgetReceiver::class.java)
                manager.requestPinAppWidget(myProvider, null, null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(CountUpUiEvent.Refresh)
        refreshWidget()
    }

    private fun refreshWidget() {
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.Default) {
            runCatching {
                pushWidgetUpdate(appContext)
                pushAllHeroWidgetsUpdate(appContext)
            }
        }
    }
}
