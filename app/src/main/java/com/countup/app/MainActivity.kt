package com.countup.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
 * Mid-century-modern multi-item screen. Displays every count-up item, lets the
 * user add, edit (name + anchor date) and delete items, and refreshes the widget
 * after any confirmed write. No navigation framework, no DI, no repository layer.
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: CountUpStore

    private var items by mutableStateOf<List<CountUpItem>>(emptyList())
    private var backgroundTheme by mutableStateOf(BackgroundTheme.AUTO_DAILY)
    private var sortOrder by mutableStateOf(SortOrder.DAYS_DESC)
    private var editorTarget by mutableStateOf<CountUpItem?>(null)
    private var showEditor by mutableStateOf(false)
    private var pendingDelete by mutableStateOf<CountUpItem?>(null)
    private var pendingReset by mutableStateOf<CountUpItem?>(null)
    private var errorMessage by mutableStateOf<String?>(null)
    private var feedbackMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = CountUpStore(this)
        items = store.items()
        backgroundTheme = store.getBackgroundTheme()
        sortOrder = store.getSortOrder()
        restoreDialogState(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CountUpTheme {
                val snackbar = remember { SnackbarHostState() }
                feedbackMessage?.let { message ->
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(message)
                        feedbackMessage = null
                    }
                }
                errorMessage?.let { message ->
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(message)
                        errorMessage = null
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    },
                ) { innerPadding ->
                    CountUpList(
                        items = items,
                        sortOrder = sortOrder,
                        backgroundTheme = backgroundTheme,
                        modifier = Modifier.padding(innerPadding),
                        onItemTap = { editorTarget = it; showEditor = true },
                        onNewItem = { editorTarget = null; showEditor = true },
                        onDeleteRequest = { pendingDelete = it },
                        onResetRequest = { pendingReset = it },
                        onToggleWidgetVisibility = { toggleWidgetVisibility(it.id) },
                        onCycleBackground = { cycleBackground() },
                        onSelectSortOrder = { selectSortOrder(it) },
                    )
                }
                if (showEditor) {
                    ItemEditorDialog(
                        item = editorTarget,
                        onDismiss = { showEditor = false },
                        onSave = { name, epochDay, comment ->
                            if (editorTarget == null) addItem(name, epochDay, comment)
                            else updateItem(editorTarget!!.id, name, epochDay, comment)
                        },
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
        backgroundTheme = store.getBackgroundTheme()
        sortOrder = store.getSortOrder()
        refreshWidget()
    }

    private fun addItem(name: String, epochDay: Long, comment: String = "") {
        if (store.addItem(name, epochDay, comment) != null) {
            items = store.items()
            refreshWidget()
            showEditor = false
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
    }

    private fun updateItem(id: String, name: String, epochDay: Long, comment: String = "") {
        if (store.updateItem(id, name, epochDay, comment)) {
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

    private fun toggleWidgetVisibility(id: String) {
        val target = items.firstOrNull { it.id == id } ?: return
        val newVisibility = !target.showInWidget
        if (store.setWidgetVisibility(id, newVisibility)) {
            items = store.items()
            refreshWidget()
            feedbackMessage = getString(
                if (newVisibility) R.string.toast_shown_in_widget else R.string.toast_hidden_from_widget,
                target.name,
            )
        } else {
            errorMessage = getString(R.string.error_save_failed)
        }
    }

    private fun cycleBackground() {
        val next = backgroundTheme.next()
        backgroundTheme = next
        store.setBackgroundTheme(next)
        feedbackMessage = getString(R.string.bg_switched_toast, getString(next.labelRes))
        refreshWidget()
    }

    private fun cycleSortOrder() {
        val next = sortOrder.next()
        sortOrder = next
        store.setSortOrder(next)
        refreshWidget()
    }

    private fun selectSortOrder(order: SortOrder) {
        sortOrder = order
        store.setSortOrder(order)
        refreshWidget()
    }

    private fun refreshWidget() {
        // Classic RemoteViews push: synchronous IPC to the launcher, so an app-side
        // data change lands on the widget immediately (no composition pipeline).
        runCatching { pushWidgetUpdate(this) }
    }
    private companion object {
        private const val STATE_EDITOR_TARGET_ID = "state_editor_target_id"
        private const val STATE_PENDING_DELETE_ID = "state_pending_delete_id"
        private const val STATE_PENDING_RESET_ID = "state_pending_reset_id"
    }
}

/** Mid-century-modern theme: warm beige paper, deep-brown ink, sans-serif type. */
@Composable
private fun CountUpTheme(content: @Composable () -> Unit) {
    // Light theme only (design decision): the system dark mode is ignored.
    MaterialTheme(colorScheme = lightMcmColors(), content = content)
}

// Mid-century-modern palette: warm beige paper (#F5E6D3), deep-brown ink
// (#2C2416), matte accent pops, walnut for secondary text. Named so theme edits
// stay consistent.
private val McmInk = Color(0xFF2C2416)
private val McmWhite = Color(0xFFFFFFFF)
private val ArrivedFutureGreen = Color(0xFF66BB6A)
private val ArrivedFutureRed = Color(0xFFB71C1C)
private val McmPaperBackground = Color(0xFFF5E6D3)
private val McmPaperSurface = Color(0xFFEBDCC3)
private val McmPaperCard = Color(0xFFFFFFFF)
private val McmMuted = Color(0xFF6B5D4F)
private val McmRule = Color(0xFFE3D3B8)
private val McmRuleVariant = Color(0xFFD9C6A6)
private val McmOrange = Color(0xFFD97642)
private val McmOlive = Color(0xFF4A7C59)
private val McmError = Color(0xFFA64942)

// Item accent chips: orange, mustard, olive, dusty blue, coral, teak — picked
// deterministically by id hash so an item keeps its colour across launches.
private val McmDayAccents = listOf(
    Color(0xFFD97642), Color(0xFFD4A574), Color(0xFF4A7C59),
    Color(0xFF7D9BA8), Color(0xFFE57A77), Color(0xFF8B7355),
)

private fun lightMcmColors() = lightColorScheme(
    primary = McmOrange,
    onPrimary = McmWhite,
    tertiary = McmOlive,
    onTertiary = McmWhite,
    background = McmPaperBackground,
    onBackground = McmInk,
    surface = McmPaperCard,
    onSurface = McmInk,
    surfaceVariant = McmPaperSurface,
    onSurfaceVariant = McmMuted,
    outline = McmRule,
    outlineVariant = McmRuleVariant,
    error = McmError,
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
    sortOrder: SortOrder,
    backgroundTheme: BackgroundTheme,
    modifier: Modifier = Modifier,
    onItemTap: (CountUpItem) -> Unit,
    onNewItem: () -> Unit,
    onDeleteRequest: (CountUpItem) -> Unit,
    onResetRequest: (CountUpItem) -> Unit,
    onToggleWidgetVisibility: (CountUpItem) -> Unit,
    onCycleBackground: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
) {
    val localContext = LocalContext.current
    val reduceMotion = remember(localContext) { isReducedMotion(localContext) }
    val ensoInteraction = rememberPressSource()
    val themeLabel = stringResource(backgroundTheme.labelRes)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }
    val filteredAndSortedItems = remember(items, searchQuery, sortOrder) {
        queryAndSortItems(items, searchQuery, sortOrder)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawAbstractBackground(backgroundTheme),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.padding(top = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.ic_zen_enso),
                    contentDescription = themeLabel,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = ensoInteraction,
                            indication = LocalIndication.current,
                            onClick = onCycleBackground,
                        )
                        .pressScale(ensoInteraction),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // New-item control: filled olive circle with a white "+" in the
                // top-right corner — the MCM accent pop.
                val plusInteraction = rememberPressSource()
                val newItemLabel = stringResource(R.string.new_item)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .clickable(
                            interactionSource = plusInteraction,
                            indication = LocalIndication.current,
                            onClick = onNewItem,
                        )
                        .pressScale(plusInteraction)
                        .semantics { contentDescription = newItemLabel },
                ) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.padding(top = 16.dp))

            if (items.isEmpty()) {
                EmptyState(onNewItem = onNewItem, modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
            } else {
                // Secondary sub-header: item count on the left, Sort & Search popover trigger on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (filteredAndSortedItems.size == 1) {
                            stringResource(R.string.items_count_one)
                        } else {
                            stringResource(R.string.items_count, filteredAndSortedItems.size)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            letterSpacing = 0.2.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val sortInteraction = rememberPressSource()
                    val sortDescription = stringResource(R.string.cd_sort_search_pill, stringResource(sortOrder.labelRes))
                    val searchIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Box {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = sortInteraction,
                                    indication = LocalIndication.current,
                                    onClick = { isMenuOpen = !isMenuOpen },
                                )
                                .pressScale(sortInteraction)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .semantics { contentDescription = sortDescription },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.sort_prefix),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(sortOrder.labelRes),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(6.dp))
                                // Subtle magnifying glass outline icon at the end of the right side
                                Canvas(modifier = Modifier.size(13.dp)) {
                                    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                                    val radius = size.width * 0.32f
                                    val centerOffset = Offset(size.width * 0.40f, size.height * 0.40f)
                                    drawCircle(color = searchIconColor, radius = radius, center = centerOffset, style = stroke)
                                    val lineStart = Offset(size.width * 0.64f, size.height * 0.64f)
                                    val lineEnd = Offset(size.width * 0.95f, size.height * 0.95f)
                                    drawLine(color = searchIconColor, start = lineStart, end = lineEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
                                }
                            }
                        }

                        // Anchored Dropdown Popover
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            modifier = Modifier
                                .width(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            // Search Input Row
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Canvas(modifier = Modifier.size(12.dp)) {
                                        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                                        val radius = size.width * 0.32f
                                        val centerOffset = Offset(size.width * 0.40f, size.height * 0.40f)
                                        drawCircle(color = searchIconColor, radius = radius, center = centerOffset, style = stroke)
                                        val lineStart = Offset(size.width * 0.64f, size.height * 0.64f)
                                        val lineEnd = Offset(size.width * 0.95f, size.height * 0.95f)
                                        drawLine(color = searchIconColor, start = lineStart, end = lineEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 12.5.sp,
                                        ),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.search_placeholder),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        fontSize = 12.5.sp,
                                                    ),
                                                )
                                            }
                                            innerTextField()
                                        },
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { searchQuery = "" },
                                        ) {
                                            Text(
                                                text = "✕",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.padding(top = 8.dp))
                            Text(
                                text = stringResource(R.string.sort_section_title),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                            Spacer(Modifier.padding(top = 2.dp))

                            // Sort Options List
                            SortOrder.entries.forEach { option ->
                                val isSelected = option == sortOrder
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            onSelectSortOrder(option)
                                            isMenuOpen = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(option.labelRes),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = stringResource(option.descriptionRes),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(top = 8.dp))

                if (filteredAndSortedItems.isEmpty() && searchQuery.isNotEmpty()) {
                    EmptySearchState(
                        onClearSearch = { searchQuery = "" },
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    ) {
                        items(filteredAndSortedItems, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onClick = { onItemTap(item) },
                                onDelete = { onDeleteRequest(item) },
                                onReset = { onResetRequest(item) },
                                onToggleWidget = { onToggleWidgetVisibility(item) },
                                modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(onClearSearch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.padding(top = 16.dp))
        Text(
            text = stringResource(R.string.search_no_matches),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            text = stringResource(R.string.search_no_matches_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 16.dp))
        val clearInteraction = rememberPressSource()
        OutlinedButton(
            onClick = onClearSearch,
            interactionSource = clearInteraction,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.pressScale(clearInteraction),
        ) {
            Text(stringResource(R.string.search_clear))
        }
    }
}

@Composable
private fun EmptyState(onNewItem: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            stringResource(R.string.empty_heading),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 20.dp))
        val emptyInteraction = rememberPressSource()
        Button(
            onClick = onNewItem,
            interactionSource = emptyInteraction,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            shape = RoundedCornerShape(percent = 50),
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
    onToggleWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = daysSince(LocalDate.ofEpochDay(item.epochDay), LocalDate.now())
    val cardInteraction = rememberPressSource()
    val resetInteraction = rememberPressSource()
    val deleteInteraction = rememberPressSource()
    val resetDesc = stringResource(R.string.reset)
    val deleteDesc = stringResource(R.string.delete)

    // Deterministic MCM accent chip for this item's icon; the glyph ink follows
    // the chip's luminance so it stays readable on every accent.
    val accents = McmDayAccents
    // An item created/updated with a future date, once its day has arrived:
    // solid green circle and a red bold count instead of the MCM accent chip.
    val arrivedFuture = item.futureFlag && count >= 0
    val accent = if (arrivedFuture) ArrivedFutureGreen else accents[(item.id.hashCode() % accents.size + accents.size) % accents.size]
    val onAccent = if (accent.luminance() > 0.35f) McmInk else McmWhite

    // Number + "days" share one baseline (rendered as a single annotated string),
    // so they align optically and scale together regardless of font size.
    val pluralSelector = kotlin.math.abs(count).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val unitLabel = pluralStringResource(R.plurals.days_unit, pluralSelector, count)
    val countLabel = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = 44.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = if (arrivedFuture) ArrivedFutureRed else MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            append(count.toString())
        }
        append(" ")
        withStyle(
            SpanStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            append(unitLabel)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Clean, soft drop shadow with no harsh clipping.
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = Color(0x1A2C2416),
                spotColor = Color(0x262C2416),
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .pressScale(cardInteraction, 0.99f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp).background(accent, CircleShape),
            ) {
                Icon(
                    painter = painterResource(iconRes(item.icon.ifEmpty { DEFAULT_ICON })),
                    contentDescription = null,
                    tint = onAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = item.name.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // Modular widget grid: filled = shown in the widget (default),
            // outline = excluded. Sits before Reset and Delete, top-right.
            val widgetInteraction = rememberPressSource()
            val widgetVisible = item.showInWidget
            val widgetDesc = stringResource(if (widgetVisible) R.string.widget_hide else R.string.widget_show)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = widgetInteraction,
                        indication = LocalIndication.current,
                        onClick = onToggleWidget,
                    )
                    .pressScale(widgetInteraction),
            ) {
                Icon(
                    painter = painterResource(if (widgetVisible) R.drawable.ic_widget_grid_filled else R.drawable.ic_widget_grid_outline),
                    contentDescription = widgetDesc,
                    tint = if (widgetVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            // Reset and Delete symbols, top-right of the card. Vertically centered.
            val resetDesc = stringResource(R.string.reset)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = resetInteraction,
                        indication = LocalIndication.current,
                        onClick = onReset,
                    )
                    .pressScale(resetInteraction)
                    .semantics { contentDescription = resetDesc },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            val deleteDesc = stringResource(R.string.delete)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = deleteInteraction,
                        indication = LocalIndication.current,
                        onClick = onDelete,
                    )
                    .pressScale(deleteInteraction)
                    .semantics { contentDescription = deleteDesc },
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 2.2.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(1.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = countLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatAnchorDateSubLabel(
                        count = count,
                        date = LocalDate.ofEpochDay(item.epochDay),
                        sinceTemplate = stringResource(R.string.since_label),
                        untilTemplate = stringResource(R.string.until_label),
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.comment.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = item.comment,
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = 15.sp,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Add (item == null) or edit (item != null) dialog: name field + comment field + date picker. */
@Composable
private fun ItemEditorDialog(
    item: CountUpItem?,
    onDismiss: () -> Unit,
    onSave: (String, Long, String) -> Unit,
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var comment by remember { mutableStateOf(item?.comment ?: "") }
    var epochDay by remember { mutableLongStateOf(item?.epochDay ?: LocalDate.now().toEpochDay()) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFCF8F2),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(if (item == null) R.string.add_title else R.string.edit_title).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            val textFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    placeholder = { Text(stringResource(R.string.name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 10.dp))
                TextField(
                    value = comment,
                    onValueChange = { if (it.lines().size <= 2) comment = it },
                    label = { Text(stringResource(R.string.item_comment)) },
                    placeholder = { Text(stringResource(R.string.comment_placeholder)) },
                    maxLines = 2,
                    singleLine = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.anchor_date).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val pickerInteraction = rememberPressSource()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .clickable(
                                interactionSource = pickerInteraction,
                                indication = LocalIndication.current,
                                onClick = { showPicker = true },
                            )
                            .pressScale(pickerInteraction)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = formatLocalized(LocalDate.ofEpochDay(epochDay)),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, epochDay, comment) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = FontFamily.SansSerif,
                )
            }
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
        containerColor = Color(0xFFFCF8F2),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.delete_title).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_message, itemName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = FontFamily.SansSerif,
                )
            }
        },
    )
}

@Composable
private fun ResetConfirmDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFCF8F2),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.reset_title).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.reset_message, itemName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.reset),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = FontFamily.SansSerif,
                )
            }
        },
    )
}

@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
) {
    // No SelectableDates restriction: future dates are allowed so an item can
    // count down to its anchor day.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        // Full available dialog width: the picker's 7-column calendar needs every
        // dp it can get on narrow screens, or the Sat/Sun header columns overlap.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        containerColor = Color(0xFFFCF8F2),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.pick_date_title).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            val datePickerColors = DatePickerDefaults.colors(
                containerColor = Color(0xFFFCF8F2),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                subheadContentColor = MaterialTheme.colorScheme.onSurface,
                yearContentColor = MaterialTheme.colorScheme.onSurface,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                dividerColor = MaterialTheme.colorScheme.outline,
            )
            DatePicker(
                state = state,
                title = null,
                headline = null,
                colors = datePickerColors,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onDatePicked(datePickerMillisToLocalDate(millis))
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.pick_date_confirm),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = FontFamily.SansSerif,
                )
            }
        },
    )
}

