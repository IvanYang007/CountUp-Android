package com.ivanyang.countup

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Zen-paper multi-item screen. Displays every count-up item, lets the user add,
 * edit (name + anchor date) and delete items, and refreshes the widget after any
 * confirmed write. No navigation framework, no DI, no repository layer.
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: CountUpStore

    private var items by mutableStateOf<List<CountUpItem>>(emptyList())
    private var editorTarget by mutableStateOf<CountUpItem?>(null)
    private var showEditor by mutableStateOf(false)
    private var pendingDelete by mutableStateOf<CountUpItem?>(null)
    private var pendingReset by mutableStateOf<CountUpItem?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = CountUpStore(this)
        items = store.items()
        restoreDialogState(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CountUpTheme {
                val snackbar = remember { SnackbarHostState() }
                errorMessage?.let { message ->
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(message)
                        errorMessage = null
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { innerPadding ->
                    CountUpList(
                        items = items,
                        modifier = Modifier.padding(innerPadding),
                        onItemTap = { editorTarget = it; showEditor = true },
                        onNewItem = { editorTarget = null; showEditor = true },
                        onDeleteRequest = { pendingDelete = it },
                        onResetRequest = { pendingReset = it },
                    )
                }
            }

            if (showEditor) {
                ItemEditorDialog(
                    item = editorTarget,
                    onDismiss = { showEditor = false },
                    onSave = { name, epochDay -> if (editorTarget == null) addItem(name, epochDay) else updateItem(editorTarget!!.id, name, epochDay) },
                )
            }

            pendingDelete?.let { target ->
                DeleteConfirmDialog(
                    itemName = target.name,
                    onDismiss = { pendingDelete = null },
                    onConfirm = { deleteItem(target.id) },
                )
            }

            pendingReset?.let { target ->
                ResetConfirmDialog(
                    itemName = target.name,
                    onDismiss = { pendingReset = null },
                    onConfirm = { resetItem(target.id) },
                )
            }
        }
    }

    /**
     * Keeps the open dialog's identity across configuration change / process
     * death: editor, delete-confirm and reset-confirm state is saved as item ids
     * and re-resolved against the store on restore. Ids that no longer resolve
     * (item deleted meanwhile) drop their dialog. Typed-but-unsaved draft text is
     * not preserved.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (showEditor) {
            outState.putString(STATE_EDITOR_TARGET_ID, editorTarget?.id)
        }
        pendingDelete?.let { outState.putString(STATE_PENDING_DELETE_ID, it.id) }
        pendingReset?.let { outState.putString(STATE_PENDING_RESET_ID, it.id) }
    }

    private fun restoreDialogState(saved: Bundle?) {
        if (saved == null) return
        if (saved.containsKey(STATE_EDITOR_TARGET_ID)) {
            editorTarget = saved.getString(STATE_EDITOR_TARGET_ID)
                ?.let { id -> items.firstOrNull { it.id == id } }
            showEditor = true
        }
        saved.getString(STATE_PENDING_DELETE_ID)
            ?.let { id -> items.firstOrNull { it.id == id } }
            ?.let { pendingDelete = it }
        saved.getString(STATE_PENDING_RESET_ID)
            ?.let { id -> items.firstOrNull { it.id == id } }
            ?.let { pendingReset = it }
    }

    override fun onResume() {
        super.onResume()
        items = store.items()
        // Refresh the widget only when it is stale (day rolled over or never
        // refreshed): counts depend on LocalDate.now(), so a new day always
        // needs a rebuild, but a same-day foreground with no data change must
        // not spam Android's widget throttler.
        if (!store.widgetRefreshedOn(LocalDate.now().toEpochDay())) {
            refreshWidget()
        }
    }

    private fun addItem(name: String, epochDay: Long) {
        if (store.addItem(name, epochDay) != null) {
            items = store.items()
            refreshWidget()
            showEditor = false
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
    }

    private fun updateItem(id: String, name: String, epochDay: Long) {
        if (store.updateItem(id, name, epochDay)) {
            items = store.items()
            refreshWidget()
            showEditor = false
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
    }

    private fun deleteItem(id: String) {
        if (store.deleteItem(id)) {
            items = store.items()
            refreshWidget()
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
        pendingDelete = null
    }

    private fun resetItem(id: String) {
        if (store.resetTo(id, LocalDate.now().toEpochDay())) {
            items = store.items()
            refreshWidget()
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
        pendingReset = null
    }

    private fun refreshWidget() {
        // Classic RemoteViews push: synchronous IPC to the launcher, so an app-side
        // data change lands on the widget immediately (no composition pipeline).
        runCatching { pushWidgetUpdate(this) }
            .onSuccess {
                // Mark only after the update was sent, so a dropped push does not
                // hide a stale widget from the next resume gate.
                store.markWidgetRefreshed(LocalDate.now().toEpochDay())
            }
    }
    private companion object {
        private const val STATE_EDITOR_TARGET_ID = "state_editor_target_id"
        private const val STATE_PENDING_DELETE_ID = "state_pending_delete_id"
        private const val STATE_PENDING_RESET_ID = "state_pending_reset_id"
    }
}

/** Warm monochrome zen-paper theme. Serif for emphasis, sans for body. */
@Composable
private fun CountUpTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkZenColors() else lightZenColors()
    MaterialTheme(colorScheme = colors, content = content)
}

// Zen-paper palette: warm monochrome, named so theme edits stay consistent.
private val ZenInk = Color(0xFF2F3437)
private val ZenWhite = Color(0xFFFFFFFF)
private val ZenPaperBackground = Color(0xFFF7F6F3)
private val ZenPaperSurface = Color(0xFFF1EFEA)
private val ZenPaperCard = Color(0xFFFFFFFF)
private val ZenMuted = Color(0xFF787774)
private val ZenRule = Color(0xFFEAEAEA)
private val ZenRuleVariant = Color(0xFFE3E1DC)
private val ZenError = Color(0xFF9F2F2D)
private val ZenNightEmphasis = Color(0xFFEDEBE6)
private val ZenNightBackground = Color(0xFF1B1B19)
private val ZenNightSurface = Color(0xFF242422)
private val ZenNightSurfaceVariant = Color(0xFF2C2C29)
private val ZenNightMuted = Color(0xFFA8A8A3)
private val ZenNightRule = Color(0xFF3A3A37)
private val ZenNightRuleVariant = Color(0xFF333330)
private val ZenNightError = Color(0xFFF0A6A3)

private fun lightZenColors() = lightColorScheme(
    primary = ZenInk,
    onPrimary = ZenWhite,
    background = ZenPaperBackground,
    onBackground = ZenInk,
    surface = ZenPaperCard,
    onSurface = ZenInk,
    surfaceVariant = ZenPaperSurface,
    onSurfaceVariant = ZenMuted,
    outline = ZenRule,
    outlineVariant = ZenRuleVariant,
    error = ZenError,
)

private fun darkZenColors() = darkColorScheme(
    primary = ZenNightEmphasis,
    onPrimary = ZenNightBackground,
    background = ZenNightBackground,
    onBackground = ZenNightEmphasis,
    surface = ZenNightSurface,
    onSurface = ZenNightEmphasis,
    surfaceVariant = ZenNightSurfaceVariant,
    onSurfaceVariant = ZenNightMuted,
    outline = ZenNightRule,
    outlineVariant = ZenNightRuleVariant,
    error = ZenNightError,
)

@Composable
private fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/** Tactile press feedback: scale(0.96) for buttons, ~0.99 for cards. Transform only, interruptible. */
@Composable
private fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    target: Float = 0.96f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) target else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Honors the system reduce-motion setting (animator duration scale = 0). */
private fun isReducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
private fun CountUpList(
    items: List<CountUpItem>,
    modifier: Modifier = Modifier,
    onItemTap: (CountUpItem) -> Unit,
    onNewItem: () -> Unit,
    onDeleteRequest: (CountUpItem) -> Unit,
    onResetRequest: (CountUpItem) -> Unit,
) {
    val localContext = LocalContext.current
    val reduceMotion = remember(localContext) { isReducedMotion(localContext) }
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.padding(top = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.ic_zen_enso),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = (-0.5).sp),
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // New-item control: a clean bordered "+" button in the top-right corner.
            val plusInteraction = rememberPressSource()
            val newItemLabel = stringResource(R.string.new_item)
            IconButton(
                onClick = onNewItem,
                interactionSource = plusInteraction,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .pressScale(plusInteraction)
                    .semantics { contentDescription = newItemLabel },
            ) {
                Text(
                    text = "+",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.padding(top = 20.dp))

        if (items.isEmpty()) {
            EmptyState(onNewItem = onNewItem, modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onClick = { onItemTap(item) },
                        onDelete = { onDeleteRequest(item) },
                        onReset = { onResetRequest(item) },
                        modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onNewItem: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.empty_heading), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
        Spacer(Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 20.dp))
        val emptyInteraction = rememberPressSource()
        TextButton(
            onClick = onNewItem,
            interactionSource = emptyInteraction,
            modifier = Modifier.pressScale(emptyInteraction),
        ) { Text(stringResource(R.string.new_item)) }
    }
}

@Composable
fun ItemCard(
    item: CountUpItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = daysSince(LocalDate.ofEpochDay(item.epochDay), LocalDate.now())
    val cardInteraction = rememberPressSource()
    val resetInteraction = rememberPressSource()
    val deleteInteraction = rememberPressSource()
    val resetDesc = stringResource(R.string.reset)
    val deleteDesc = stringResource(R.string.delete)

    // Number + "days" share one baseline (rendered as a single annotated string),
    // so they align optically and scale together regardless of font size.
    val unitLabel = pluralStringResource(R.plurals.days_unit, count.toInt(), count.toInt())
    val countLabel = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = 44.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            append(count.toString())
        }
        append(" ")
        withStyle(
            SpanStyle(
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            append(unitLabel)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .pressScale(cardInteraction, 0.99f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconRes(item.icon.ifEmpty { DEFAULT_ICON })),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // Reset and Delete symbols, top-right of the card.
            Text(
                text = "↺",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(
                        interactionSource = resetInteraction,
                        indication = LocalIndication.current,
                        onClick = onReset,
                    )
                    .pressScale(resetInteraction)
                    .semantics { contentDescription = resetDesc }
                    .padding(4.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "−",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clickable(
                        interactionSource = deleteInteraction,
                        indication = LocalIndication.current,
                        onClick = onDelete,
                    )
                    .pressScale(deleteInteraction)
                    .semantics { contentDescription = deleteDesc }
                    .padding(4.dp),
            )
        }
        Text(
            text = countLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.since_label, formatLocalized(LocalDate.ofEpochDay(item.epochDay))),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Add (item == null) or edit (item != null) dialog: name field + date picker. */
@Composable
private fun ItemEditorDialog(
    item: CountUpItem?,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var epochDay by remember { mutableLongStateOf(item?.epochDay ?: LocalDate.now().toEpochDay()) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (item == null) R.string.add_title else R.string.edit_title)) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    placeholder = { Text(stringResource(R.string.name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(Modifier.padding(top = 16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.anchor_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(start = 10.dp))
                    TextButton(onClick = { showPicker = true }) {
                        Text(
                            formatLocalized(LocalDate.ofEpochDay(epochDay)),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, epochDay) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (showPicker) {
        DatePickerDialog(
            initialDate = LocalDate.ofEpochDay(epochDay),
            onDismiss = { showPicker = false },
            onDatePicked = { picked -> epochDay = picked.toEpochDay() },
        )
    }
}

@Composable
private fun DeleteConfirmDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message, itemName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ResetConfirmDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_message, itemName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.reset)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val selectableDates = remember(today) {
        val todayUtcNoon = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 12 * 3600_000L
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcNoon
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        // Full available dialog width: the picker's 7-column calendar needs every
        // dp it can get on narrow screens, or the Sat/Sun header columns overlap.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(),
        title = { Text(stringResource(R.string.pick_date_title)) },
        text = {
            DatePicker(
                state = state,
                // Compact the picker: drop the redundant title row and give the
                // grid the full width with a minimal inset.
                title = null,
                headline = null,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onDatePicked(datePickerMillisToLocalDate(millis))
                    onDismiss()
                },
            ) { Text(stringResource(R.string.pick_date_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
