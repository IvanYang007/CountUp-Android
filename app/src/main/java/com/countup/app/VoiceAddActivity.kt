package com.countup.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * Lightweight, translucent activity dedicated to voice quick-add from widgets.
 * Launches the platform speech recognizer, sanitizes speech results, commits the
 * item anchored to today, immediately refreshes all widgets, and shows a transient
 * confirmation card with Undo and Edit actions.
 */
class VoiceAddActivity : ComponentActivity() {

    private val sessionId = UUID.randomUUID().toString()
    private var anchorEpochDay: Long = 0L
    private var createdItem: CountUpItem? by mutableStateOf(null)

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
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

        if (savedInstanceState == null) {
            launchSpeechRecognition()
        }

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        onUndo = { undoItem(item) },
                        onEdit = { editItem(item) },
                        onDismiss = { finishSilently() }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ANCHOR_DATE, anchorEpochDay)
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_add_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            // Speech engine missing: seamlessly fallback to existing Add Dialog in MainActivity
            val fallback = Intent(this, MainActivity::class.java).apply {
                action = ACTION_ADD_ITEM
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(fallback)
            finishSilently()
        }
    }

    private fun saveAndConfirm(text: String) {
        val store = CountUpStore.getInstance(applicationContext)
        val item = store.addItem(
            name = text,
            epochDay = anchorEpochDay,
            id = sessionId
        )
        if (item != null) {
            createdItem = item
            pushAllWidgetsUpdate(applicationContext)
        } else {
            finishSilently()
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val KEY_ANCHOR_DATE = "KEY_ANCHOR_DATE"
    }
}

@Composable
private fun VoiceConfirmationPill(
    item: CountUpItem,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 3.5s auto-dismiss
    LaunchedEffect(item.id) {
        delay(3500L)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.voice_added_prefix),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.action_edit))
                    }
                    FilledTonalButton(onClick = onUndo) {
                        Text(stringResource(R.string.action_undo))
                    }
                }
            }
        }
    }
}
