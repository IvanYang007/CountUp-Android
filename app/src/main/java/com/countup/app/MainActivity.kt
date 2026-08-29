package com.countup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Mid-century-modern multi-item screen container.
 * Follows 2026 Android MVI guidelines: Activity acts as a lifecycle host, delegating
 * business logic and state to [CountUpViewModel] and UI rendering to [CountUpContent].
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: CountUpStore
    private var activeViewModel: CountUpViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        store = CountUpStore(this)

        setContent {
            ZenTheme {
                val viewModel: CountUpViewModel = viewModel {
                    CountUpViewModel(DefaultCountUpRepository(store))
                }
                activeViewModel = viewModel

                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
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

    override fun onResume() {
        super.onResume()
        activeViewModel?.onEvent(CountUpUiEvent.Refresh)
        refreshWidget()
    }

    private fun refreshWidget() {
        runCatching { pushWidgetUpdate(this) }
    }
}
