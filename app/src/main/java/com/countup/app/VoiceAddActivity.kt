package com.countup.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

/**
 * Lightweight, translucent activity dedicated to voice quick-add from widgets.
 * Launches the native platform speech recognizer (zero permissions required),
 * sanitizes speech results, commits the item anchored to today, immediately refreshes
 * all widgets, and shows a transient Zen confirmation pill with light/dark theme aesthetics.
 */
class VoiceAddActivity : ComponentActivity() {

    private var anchorEpochDay: Long = 0L
    private var createdItem: CountUpItem? by mutableStateOf(null)

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = VoiceTextSanitizer.sanitize(candidates)
            if (!recognizedText.isNullOrBlank()) {
                saveAndConfirm(recognizedText)
                return@registerForActivityResult
            }
        }
        // User cancelled, spoke nothing, or recognition failed: finish silently without ghost screens
        finishSilently()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Lock anchor date at session launch time to avoid midnight clock jitter
        anchorEpochDay = savedInstanceState?.getLong(KEY_ANCHOR_DATE) ?: LocalDate.now().toEpochDay()

        val store = CountUpStore.getInstance(applicationContext)
        val savedItemId = savedInstanceState?.getString(KEY_CREATED_ITEM_ID)
        if (savedItemId != null) {
            createdItem = store.items().find { it.id == savedItemId }
        }

        if (savedInstanceState == null) {
            launchSpeechRecognition()
        }

        val themeMode = store.getThemeMode()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDark = themeMode.isDark(systemDark)

            ZenTheme(darkTheme = isDark) {
                val zenColors = LocalZenColors.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (createdItem != null) Color.Black.copy(alpha = 0.40f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finishSilently() }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    createdItem?.let { item ->
                        VoiceConfirmationPill(
                            item = item,
                            zenColors = zenColors,
                            onUndo = { undoItem(item) },
                            onEdit = { editItem(item) },
                            onDismiss = { finishSilently() }
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ANCHOR_DATE, anchorEpochDay)
        createdItem?.let { outState.putString(KEY_CREATED_ITEM_ID, it.id) }
    }

    private fun fallbackToManualAdd() {
        val fallback = Intent(this, MainActivity::class.java).apply {
            action = ACTION_ADD_ITEM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(fallback)
        finishSilently()
    }

    private fun launchSpeechRecognition() {
        val localeTag = Locale.getDefault().toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_listening_hint))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        if (intent.resolveActivity(packageManager) == null) {
            fallbackToManualAdd()
            return
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            // Speech engine missing: seamlessly fallback to existing Add Dialog in MainActivity
            fallbackToManualAdd()
        }
    }

    private fun saveAndConfirm(text: String) {
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val store = CountUpStore.getInstance(appContext)
            val item = store.addItem(
                name = text,
                epochDay = anchorEpochDay,
            )
            withContext(Dispatchers.Main) {
                if (item != null) {
                    createdItem = item
                    pushAllWidgetsUpdate(appContext)
                } else {
                    finishSilently()
                }
            }
        }
    }

    private fun undoItem(item: CountUpItem) {
        val appContext = applicationContext
        widgetReceiverScope.launch {
            CountUpStore.getInstance(appContext).deleteItem(item.id)
            pushAllWidgetsUpdate(appContext)
        }
        Toast.makeText(this, R.string.voice_item_undone, Toast.LENGTH_SHORT).show()
        finishSilently()
    }

    private fun editItem(item: CountUpItem) {
        startActivity(WidgetNavigationContract.createLaunchIntent(this, item.id))
        finishSilently()
    }

    private fun finishSilently() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val KEY_ANCHOR_DATE = "KEY_ANCHOR_DATE"
        private const val KEY_CREATED_ITEM_ID = "KEY_CREATED_ITEM_ID"
    }
}

@Composable
private fun VoiceConfirmationPill(
    item: CountUpItem,
    zenColors: ZenColorScheme,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dismissTimeoutMs = remember(item.id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val am = context.getSystemService(AccessibilityManager::class.java)
            am?.getRecommendedTimeoutMillis(
                3500,
                AccessibilityManager.FLAG_CONTENT_CONTROLS or AccessibilityManager.FLAG_CONTENT_ICONS
            )?.toLong() ?: 3500L
        } else {
            3500L
        }
    }

    // Auto-dismiss with accessibility considerations
    LaunchedEffect(item.id, dismissTimeoutMs) {
        delay(dismissTimeoutMs)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume touch events inside pill
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = zenColors.paperCard),
            border = BorderStroke(1.dp, zenColors.hairlineRule),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.voice_added_prefix),
                        style = MaterialTheme.typography.labelSmall,
                        color = zenColors.inkMuted
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = zenColors.inkBlack,
                        maxLines = 1
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_edit),
                            color = zenColors.inkMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = onUndo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (zenColors.isDark) ZenDarkSage else zenColors.willowSage,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_undo),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
