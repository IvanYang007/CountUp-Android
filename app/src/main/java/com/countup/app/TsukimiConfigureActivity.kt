package com.countup.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Minimalist Compose configuration screen for the Tsukimi (月相之镜 · 2x2) Widget.
 * Allows the user to select which specific counter to mirror with celestial moon phases.
 */
class TsukimiConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var store: CountUpStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set RESULT_CANCELED first so back button cancels widget placement
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val widgetInfo = try {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }
        if (widgetInfo == null || widgetInfo.provider.packageName != packageName) {
            finish()
            return
        }

        store = CountUpStore.getInstance(this)
        val today = LocalDate.now()
        val itemsState = mutableStateOf<List<CountUpItem>>(emptyList())

        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = store.items()
            withContext(Dispatchers.Main) {
                itemsState.value = loaded
            }
        }

        setContent {
            ZenTheme {
                val items by itemsState
                TsukimiConfigureScreen(
                    items = items,
                    today = today,
                    onSelect = { selectedItem ->
                        val appContext = applicationContext
                        lifecycleScope.launch(Dispatchers.IO) {
                            store.setTsukimiBinding(appWidgetId, selectedItem.id)
                            pushTsukimiWidgetUpdate(appContext, appWidgetId)

                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            withContext(Dispatchers.Main) {
                                setResult(Activity.RESULT_OK, resultValue)
                                finish()
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TsukimiConfigureScreen(
    items: List<CountUpItem>,
    today: LocalDate,
    onSelect: (CountUpItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalZenColors.current.paperBackground)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.tsukimi_configure_title).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tsukimi_configure_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_body),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = items,
                    key = { it.id },
                    contentType = { "tsukimi_configure_item" },
                ) { item ->
                    ItemCard(
                        item = item,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(item)
                        },
                        onDelete = {},
                        onReset = {},
                        today = today,
                        reduceMotion = true,
                    )
                }
            }
        }
    }
}
