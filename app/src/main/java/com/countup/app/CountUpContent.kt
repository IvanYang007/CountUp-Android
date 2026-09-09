package com.countup.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

/**
 * Pure stateless root composable rendering the CountUp main screen.
 * Follows 2026 container/content separation guidelines.
 */
@Composable
fun CountUpContent(
    state: CountUpUiState,
    onEvent: (CountUpUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val localContext = LocalContext.current
    val reduceMotion = remember(localContext) { isReducedMotion(localContext) }
    val displayItems = state.displayItems

    LaunchedEffect(Unit) {
        while (isActive) {
            kotlinx.coroutines.delay(60_000L)
            onEvent(CountUpUiEvent.CheckMidnight)
        }
    }

    val zenColors = LocalZenColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawAbstractBackground(state.backgroundTheme, state.today.toEpochDay(), isDark = zenColors.isDark),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.padding(top = 16.dp))
                HeaderRow(
                    backgroundTheme = state.backgroundTheme,
                    onCycleBackground = { onEvent(CountUpUiEvent.CycleBackground) },
                    onNewItem = { onEvent(CountUpUiEvent.OpenEditor(null)) },
                    today = state.today,
                )
                Spacer(Modifier.padding(top = 16.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (state.items.isEmpty()) {
                    EmptyState(
                        onNewItem = { onEvent(CountUpUiEvent.OpenEditor(null)) },
                        onImportBackup = { onEvent(CountUpUiEvent.RequestImportBackup) },
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                } else {
                    SubHeaderRow(
                        itemCount = displayItems.size,
                        sortOrder = state.sortOrder,
                        themeMode = state.themeMode,
                        searchQuery = state.searchQuery,
                        isMenuOpen = state.isSearchSortMenuOpen,
                        onToggleMenu = { onEvent(CountUpUiEvent.SetSearchSortMenuOpen(it)) },
                        onSearchChanged = { onEvent(CountUpUiEvent.SearchQueryChanged(it)) },
                        onClearSearch = { onEvent(CountUpUiEvent.ClearSearch) },
                        onSelectSortOrder = { onEvent(CountUpUiEvent.SortOrderSelected(it)) },
                        onSelectThemeMode = { onEvent(CountUpUiEvent.ThemeModeSelected(it)) },
                        onExportBackup = { onEvent(CountUpUiEvent.RequestExportBackup) },
                        onImportBackup = { onEvent(CountUpUiEvent.RequestImportBackup) },
                    )
                    Spacer(Modifier.padding(top = 8.dp))

                    if (displayItems.isEmpty() && state.searchQuery.isNotEmpty()) {
                        EmptySearchState(
                            onClearSearch = { onEvent(CountUpUiEvent.ClearSearch) },
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    } else {
                        val pinnedItems = remember(displayItems) { displayItems.filter { it.isPinned } }
                        val unpinnedItems = remember(displayItems) { displayItems.filter { !it.isPinned } }

                        val renderItemCard: @Composable LazyItemScope.(CountUpItem) -> Unit = { item ->
                            ItemCard(
                                item = item,
                                onClick = { onEvent(CountUpUiEvent.OpenEditor(item)) },
                                onDelete = { onEvent(CountUpUiEvent.RequestDelete(item)) },
                                onReset = { onEvent(CountUpUiEvent.ConfirmReset(item.id)) },
                                onToggleWidget = { onEvent(CountUpUiEvent.ToggleWidgetVisibility(item.id)) },
                                whisper = state.cardWhispers[item.id],
                                onUndoReset = { onEvent(CountUpUiEvent.UndoReset(item.id)) },
                                onDismissWhisper = { onEvent(CountUpUiEvent.DismissCardWhisper(item.id)) },
                                modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                                today = state.today,
                                reduceMotion = reduceMotion,
                            )
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                        ) {
                            items(
                                items = pinnedItems,
                                key = { it.id },
                                contentType = { "item_card" },
                            ) { item ->
                                renderItemCard(item)
                            }

                            if (pinnedItems.isNotEmpty() && unpinnedItems.isNotEmpty()) {
                                item(
                                    key = "pinned_section_divider",
                                    contentType = "pinned_section_divider",
                                ) {
                                    PinnedSectionDivider(
                                        modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                                    )
                                }
                            }

                            items(
                                items = unpinnedItems,
                                key = { it.id },
                                contentType = { "item_card" },
                            ) { item ->
                                renderItemCard(item)
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (state.isEditorOpen) {
            ItemEditorDialog(
                item = state.editorTarget,
                today = state.today,
                isSaving = state.isSaving,
                onDismiss = { onEvent(CountUpUiEvent.CloseEditor) },
                onSave = { draft ->
                    onEvent(CountUpUiEvent.SaveItem(draft))
                },
            )
        }

        state.pendingDelete?.let { target ->
            DeleteConfirmDialog(
                itemName = target.name,
                onDismiss = { onEvent(CountUpUiEvent.DismissDelete) },
                onConfirm = { onEvent(CountUpUiEvent.ConfirmDelete(target.id)) },
            )
        }

        state.pendingRestorePayload?.let { payload ->
            BackupRestorePreviewDialog(
                payload = payload,
                onDismiss = { onEvent(CountUpUiEvent.DismissRestorePreview) },
                onConfirmRestore = { strategy ->
                    onEvent(CountUpUiEvent.ConfirmRestore(strategy))
                },
                isDamaged = state.isRestorePayloadDamaged,
            )
        }
    }
}

@Composable
private fun SolarTermCapsule(
    solarTerm: SolarTerm,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val zenColors = LocalZenColors.current
    val context = LocalContext.current
    val reduceMotion = remember(context) { isReducedMotion(context) }
    var morphStep by rememberSaveable { mutableIntStateOf(0) }
    val capsuleInteraction = rememberPressSource()

    LaunchedEffect(morphStep) {
        if (morphStep != 0) {
            delay(4500L)
            morphStep = 0
        }
    }

    val seasonColor = remember(solarTerm.seasonRes, zenColors) {
        getSeasonColor(solarTerm.seasonRes, zenColors)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (reduceMotion) 0.7f else 0.35f,
        targetValue = if (reduceMotion) 0.7f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (reduceMotion) 1f else 0.85f,
        targetValue = if (reduceMotion) 1f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val celestialCountdown = remember(today) {
        SolarTermCalendar.getNextCardinalAnchor(today)
    }

    val degreeText = stringResource(R.string.solar_term_degree, solarTerm.degree)
    val countdownTarget = stringResource(celestialCountdown.targetNameRes)
    val countdownText = stringResource(R.string.solar_term_countdown, celestialCountdown.days, countdownTarget)
    val whisperLine1 = stringResource(solarTerm.line1Res)
    val whisperLine2 = stringResource(solarTerm.line2Res)
    val seasonName = stringResource(solarTerm.seasonRes)
    val termName = stringResource(solarTerm.nameRes)

    val sealInk = if (zenColors.isDark) zenColors.inkMuted else ZenSealInk

    val configuration = LocalConfiguration.current
    val isChinese = remember(configuration) {
        val locale = if (!configuration.locales.isEmpty) configuration.locales[0] else java.util.Locale.getDefault()
        locale.language.equals("zh", ignoreCase = true)
    }
    val subtitleFontFamily = if (isChinese) FontFamily.Serif else NotoSerifItalicFontFamily
    val subtitleFontStyle = if (isChinese) FontStyle.Normal else FontStyle.Italic

    val contentDesc = when (morphStep) {
        0 -> "$seasonName, $termName"
        1 -> "$degreeText, $countdownText"
        else -> "$whisperLine1, $whisperLine2"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = capsuleInteraction,
                indication = null,
                onClick = { morphStep = (morphStep + 1) % 3 },
            )
            .pressScale(capsuleInteraction, targetScale = ZenTactileHierarchy.Level1Card)
            .semantics { contentDescription = contentDesc }
            .widthIn(max = 220.dp)
            .padding(vertical = 2.dp),
    ) {
        AnimatedContent(
            targetState = morphStep,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                } else {
                    (fadeIn(animationSpec = tween(180, easing = LinearEasing)) +
                        slideInVertically(animationSpec = tween(180)) { height -> -height / 4 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(150, easing = LinearEasing)) +
                                slideOutVertically(animationSpec = tween(150)) { height -> height / 4 },
                        )
                }
            },
            label = "solarTermMorph",
        ) { step ->
            when (step) {
                0 -> {
                    // State 0 (Resting Seal): Marcellus 12.5sp, season in seasonal vermilion/ochre/sage/indigo, hairline pipe, term name, breathing pulse dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = seasonName,
                            style = TextStyle(
                                fontFamily = MarcellusFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                letterSpacing = 1.1.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
                            ),
                            color = seasonColor,
                        )

                        Text(
                            text = "|",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.5.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = zenColors.hairlineRuleVariant,
                            modifier = Modifier.padding(horizontal = 4.5.dp),
                        )

                        Text(
                            text = termName,
                            style = TextStyle(
                                fontFamily = MarcellusFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                letterSpacing = 0.9.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
                            ),
                            color = sealInk,
                        )

                        Box(
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .size(3.5.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .clip(CircleShape)
                                .background(seasonColor),
                        )
                    }
                }
                1 -> {
                    // State 1 (Tap 1 Reveal): 100% ONE SINGLE UNIFORM INK COLOR (ZenSealInk) at 12sp
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        Text(
                            text = degreeText,
                            style = TextStyle(
                                fontFamily = MarcellusFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.9.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = sealInk,
                        )
                        Text(
                            text = countdownText,
                            style = TextStyle(
                                fontFamily = subtitleFontFamily,
                                fontStyle = subtitleFontStyle,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                letterSpacing = if (isChinese) 0.3.sp else 0.2.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = sealInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                else -> {
                    // State 2 (Tap 2 Reveal: Soft Whisper haiku in 2 lines): 100% ONE SINGLE UNIFORM INK COLOR (ZenSealInk) at 12sp
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = whisperLine1,
                            style = TextStyle(
                                fontFamily = subtitleFontFamily,
                                fontStyle = subtitleFontStyle,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                letterSpacing = if (isChinese) 0.3.sp else 0.15.sp,
                                lineHeight = 16.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = sealInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = whisperLine2,
                            style = TextStyle(
                                fontFamily = subtitleFontFamily,
                                fontStyle = subtitleFontStyle,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                letterSpacing = if (isChinese) 0.3.sp else 0.15.sp,
                                lineHeight = 16.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = sealInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    backgroundTheme: BackgroundTheme,
    onCycleBackground: () -> Unit,
    onNewItem: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val ensoInteraction = rememberPressSource()
    val themeLabel = stringResource(backgroundTheme.labelRes)
    val newItemLabel = stringResource(R.string.new_item)
    val plusInteraction = rememberPressSource()
    val activeSolarTerm = remember(today) { SolarTermCalendar.getActiveSolarTerm(today) }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = ensoInteraction,
                    indication = LocalIndication.current,
                    onClick = onCycleBackground,
                )
                .pressScale(ensoInteraction)
                .semantics { contentDescription = themeLabel },
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_enso),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(3.dp))
            SolarTermCapsule(solarTerm = activeSolarTerm, today = today)
        }
        val zenColors = LocalZenColors.current
        val plusContainerColor = if (zenColors.isDark) Color(0xFF2A3A2C) else MaterialTheme.colorScheme.tertiary
        val plusContentColor = if (zenColors.isDark) ZenDarkSage else MaterialTheme.colorScheme.onTertiary
        val plusBorderModifier = if (zenColors.isDark) {
            Modifier.border(width = 1.dp, color = ZenDarkSage, shape = CircleShape)
        } else {
            Modifier
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 2.dp)
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .clip(CircleShape)
                .then(plusBorderModifier)
                .background(plusContainerColor)
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
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = plusContentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SubHeaderRow(
    itemCount: Int,
    sortOrder: SortOrder,
    themeMode: ThemeMode,
    searchQuery: String,
    isMenuOpen: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (itemCount == 1) {
                stringResource(R.string.items_count_one)
            } else {
                stringResource(R.string.items_count, itemCount)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight.Medium,
            ),
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val zenColors = LocalZenColors.current
        val sortInteraction = rememberPressSource()
        val sortDescription = stringResource(R.string.cd_sort_search_pill, stringResource(sortOrder.labelRes))
        val clearSearchDesc = stringResource(R.string.search_clear)
        val searchIconColor = MaterialTheme.colorScheme.onSurfaceVariant

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = sortInteraction,
                        indication = LocalIndication.current,
                        onClick = { onToggleMenu(!isMenuOpen) },
                    )
                    .pressScale(sortInteraction)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .semantics { contentDescription = sortDescription },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val themeGlyph = themeMode.symbol

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
                        text = "·",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.5.dp))
                    Text(
                        text = themeGlyph,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "▾",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
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

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { onToggleMenu(false) },
                shape = RoundedCornerShape(12.dp),
                containerColor = if (zenColors.isDark) zenColors.paperSurface else zenColors.paperBackground,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, zenColors.hairlineRule),
                shadowElevation = if (zenColors.isDark) 0.dp else 6.dp,
                modifier = Modifier
                    .width(260.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(zenColors.paperCard)
                        .border(1.dp, zenColors.hairlineRule, RoundedCornerShape(8.dp))
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
                            onValueChange = onSearchChanged,
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
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(zenColors.paperSurface)
                                    .clickable(onClick = onClearSearch)
                                    .semantics { contentDescription = clearSearchDesc },
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 11.sp,
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
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Spacer(Modifier.padding(top = 2.dp))

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
                            .clickable { onSelectSortOrder(option) }
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

                Spacer(Modifier.padding(top = 10.dp))
                Text(
                    text = stringResource(R.string.theme_section_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Spacer(Modifier.padding(top = 4.dp))

                // Segmented Theme Selector (Auto / Sun / Moon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(zenColors.hairlineRule.copy(alpha = if (zenColors.isDark) 0.35f else 0.45f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val themeItems = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_tab_auto),
                        ThemeMode.LIGHT to stringResource(R.string.theme_tab_light),
                        ThemeMode.DARK to stringResource(R.string.theme_tab_dark),
                    )

                    themeItems.forEach { (mode, label) ->
                        val isSelected = mode == themeMode
                        val itemInteraction = rememberPressSource()
                        val optionDesc = stringResource(mode.labelRes)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isSelected) {
                                        if (zenColors.isDark) zenColors.paperSurface else Color.White
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = LocalIndication.current,
                                    onClick = { onSelectThemeMode(mode) },
                                )
                                .padding(vertical = 6.dp)
                                .semantics { contentDescription = optionDesc },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = mode.symbol,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.width(3.5.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.padding(top = 10.dp))
                Text(
                    text = stringResource(R.string.backup_section_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Spacer(Modifier.padding(top = 2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onExportBackup)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_action_export),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.backup_action_export_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "↗",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.padding(top = 2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onImportBackup)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_action_import),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.backup_action_import_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "↙",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MechanicalResetButton(
    onResetConfirmed: () -> Unit,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    isDarkCard: Boolean = false,
    enabled: Boolean = true,
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val holdProgress = remember { Animatable(0f) }

    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            try {
                coroutineScope {
                    val detentJob = launch {
                        delay(300L)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    holdProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                    )
                    detentJob.cancel()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResetConfirmed()
                    holdProgress.snapTo(0f)
                    isPressed = false
                }
            } finally {
                withContext(NonCancellable) {
                    if (holdProgress.value < 1f && holdProgress.value > 0f) {
                        holdProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    }
                }
            }
        } else {
            if (holdProgress.value > 0f) {
                holdProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "resetScale",
    )

    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.35f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(30.dp)
            .zIndex(if (isPressed) 1f else 0f)
            .minimumInteractiveComponentSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                )
            }
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled) {
                    disabled()
                } else {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = contentDescription,
                            action = {
                                onResetConfirmed()
                                true
                            },
                        ),
                    )
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Canvas(modifier = Modifier.requiredSize(72.dp)) {
                val progress = holdProgress.value
                if (progress <= 0.01f) return@Canvas
                val strokeWidth = 3.dp.toPx()
                val sweepColor = if (isDarkCard) Color(0xFFDEB285) else Color(0xFFD97642)
                val trackColor = sweepColor.copy(alpha = 0.22f)
                // Background track ring
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                // Active progression sweep
                drawArc(
                    color = sweepColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                tint = effectiveTint,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = holdProgress.value * 360f
                    },
            )
        }
    }
}

/**
 * Renders decomposed time intervals (e.g. "1 WEEKS 2 DAYS" or "1 YEAR 3 MONTHS 10 DAYS") into an [AnnotatedString]
 * that emphasizes digits with semi-bold primary ink while keeping interval unit characters (WEEKS, DAYS, YEARS, etc.)
 * in a small, muted 12sp font to harmonize with the Mid-Century Zen design language.
 */
fun buildDecomposedAnnotatedString(
    text: String,
    mode: TimeDisplayMode,
    primaryColor: Color,
    unitColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        if (mode == TimeDisplayMode.DAYS) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
            ) {
                append(text)
            }
        } else {
            var i = 0
            while (i < text.length) {
                val ch = text[i]
                if (ch.isDigit()) {
                    val start = i
                    while (i < text.length && text[i].isDigit()) {
                        i++
                    }
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                        )
                    ) {
                        append(text.substring(start, i))
                    }
                } else if (ch.isLetter()) {
                    val start = i
                    while (i < text.length && text[i].isLetter()) {
                        i++
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = unitColor,
                        )
                    ) {
                        append(text.substring(start, i))
                    }
                } else {
                    append(ch)
                    i++
                }
            }
        }
    }
}

@Composable
private fun OdometerDisplay(
    decomposed: DecomposedTime,
    fontSize: TextUnit,
    arrivedFuture: Boolean,
    primaryInk: Color,
    mutedInk: Color,
    modifier: Modifier = Modifier,
) {
    val unitLabel = if (decomposed.unitLabelRes != 0) stringResource(decomposed.unitLabelRes) else ""
    val effectivePrimary = if (arrivedFuture) ZenArrivedRed else primaryInk
    val effectiveUnit = if (arrivedFuture) ZenArrivedRed.copy(alpha = 0.8f) else mutedInk
    val annotated = remember(decomposed.valueText, decomposed.mode, effectivePrimary, effectiveUnit) {
        buildDecomposedAnnotatedString(
            text = decomposed.valueText,
            mode = decomposed.mode,
            primaryColor = effectivePrimary,
            unitColor = effectiveUnit,
        )
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Text(
            text = annotated,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.alignByBaseline(),
        )
        if (unitLabel.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = unitLabel,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = effectiveUnit,
                modifier = Modifier.alignByBaseline(),
            )
        }
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
    whisper: CardResetWhisper? = null,
    onUndoReset: () -> Unit = {},
    onDismissWhisper: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
    reduceMotion: Boolean = false,
) {
    val anchorDate = remember(item.epochDay) { LocalDate.ofEpochDay(item.epochDay) }
    val count = remember(anchorDate, today) { daysSince(anchorDate, today) }
    val cardInteraction = rememberPressSource()
    val deleteInteraction = rememberPressSource()
    val widgetInteraction = rememberPressSource()
    val countRowInteraction = rememberPressSource()

    val canShowWeeks = abs(count) >= 7L
    var displayMode by rememberSaveable(item.id) { mutableStateOf(TimeDisplayMode.DAYS) }
    val effectiveMode = if (!canShowWeeks && displayMode == TimeDisplayMode.TOTAL_WEEKS) {
        TimeDisplayMode.DAYS
    } else {
        displayMode
    }
    val decomposed = remember(anchorDate, today, effectiveMode) {
        decomposeTime(anchorDate, today, effectiveMode)
    }

    val resetDesc = stringResource(R.string.reset)
    val deleteDesc = stringResource(R.string.delete)
    val widgetVisible = item.showInWidget
    val widgetDesc = stringResource(if (widgetVisible) R.string.widget_hide else R.string.widget_show)

    val zenColors = LocalZenColors.current
    val isSystemDark = zenColors.isDark
    val style = resolveCardStyle(item.cardColor, isDark = isSystemDark)
    val isCustomCardColor = item.cardColor.isNotBlank()
    val baseBgColor = style.cardBg
    val isDarkCard = style.isDark
    val primaryInk = style.primaryInk
    val mutedInk = style.mutedInk

    val arrivedFuture = item.futureFlag && count >= 0
    val accent = if (arrivedFuture) ZenArrivedGreen else style.badgeBg
    val onAccent = if (arrivedFuture) ZenWhite else style.badgeTint

    val currentUnitLabel = if (decomposed.unitLabelRes != 0) stringResource(decomposed.unitLabelRes) else ""
    val fullOdometerText = if (currentUnitLabel.isNotBlank()) "${decomposed.valueText} $currentUnitLabel" else decomposed.valueText

    val tapToDecomposeDesc = stringResource(R.string.cd_tap_to_decompose, fullOdometerText)

    val fontSize = when (effectiveMode) {
        TimeDisplayMode.DAYS -> 36.sp
        TimeDisplayMode.ELAPSED_BREAKDOWN -> 24.sp
        TimeDisplayMode.TOTAL_WEEKS -> 24.sp
    }

    val cardShape = RoundedCornerShape(20.dp)
    val surfaceBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFAF5EE),
            ),
        )
    }
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color(0xFFDEB285).copy(alpha = 0.60f),
            ),
        )
    }

    val isFutureEvent = item.futureFlag && count < 0
    val patina = remember(count, isDarkCard) { resolvePatina(count, isDark = isDarkCard) }

    val cardModifier = if (isCustomCardColor) {
        val customBorder = if (isSystemDark) {
            Modifier.border(width = 1.dp, color = cardBorderColor(baseBgColor, isDark = true), shape = cardShape)
        } else {
            if (isFutureEvent) {
                Modifier.border(width = 1.5.dp, color = cardBorderColor(baseBgColor), shape = cardShape)
            } else {
                Modifier.border(width = 1.5.dp, brush = patina.borderBrush, shape = cardShape)
            }
        }
        val customShadow = if (isSystemDark) {
            Modifier
        } else {
            Modifier.shadow(
                elevation = if (isDarkCard) 4.dp else 2.5.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x182C2416),
                spotColor = Color(0x222C2416),
            )
        }
        modifier
            .fillMaxWidth()
            .then(customShadow)
            .then(customBorder)
            .background(
                color = baseBgColor,
                shape = cardShape,
            )
    } else {
        if (isSystemDark) {
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = zenColors.hairlineRule,
                    shape = cardShape,
                )
                .background(
                    color = style.cardBg,
                    shape = cardShape,
                )
        } else {
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.5.dp,
                    shape = cardShape,
                    clip = false,
                    ambientColor = Color(0x182C2416),
                    spotColor = Color(0x222C2416),
                )
                .border(
                    width = 1.5.dp,
                    brush = if (isFutureEvent) borderBrush else patina.borderBrush,
                    shape = cardShape,
                )
                .background(
                    brush = surfaceBrush,
                    shape = cardShape,
                )
        }
    }

    val cardRipple = ripple(color = ZenTactileHierarchy.CardPressHighlight)

    Column(
        modifier = cardModifier
            .clip(cardShape)
            .clickable(
                interactionSource = cardInteraction,
                indication = cardRipple,
                onClick = onClick,
            )
            .pressScale(cardInteraction, ZenTactileHierarchy.Level1Card)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                    fontFamily = FontFamily.SansSerif,
                    color = mutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.isPinned) {
                    Spacer(Modifier.width(5.dp))
                    val pinDesc = stringResource(R.string.cd_pinned)
                    Icon(
                        painter = painterResource(R.drawable.ic_seal_pin),
                        contentDescription = pinDesc,
                        tint = if (isDarkCard) Color(0xFFDEB285) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp),
                    )
                }
                if (count >= 0 && item.resetCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    ResetRhythmBadge(
                        item = item,
                        isDarkCard = isDarkCard,
                        patina = patina,
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = widgetInteraction,
                        indication = null,
                        onClick = onToggleWidget,
                    )
                    .pressScale(widgetInteraction)
                    .semantics { contentDescription = widgetDesc },
            ) {
                Icon(
                    painter = painterResource(if (widgetVisible) R.drawable.ic_widget_grid_filled else R.drawable.ic_widget_grid_outline),
                    contentDescription = null,
                    tint = if (widgetVisible) {
                        if (isDarkCard) ZenOchre else MaterialTheme.colorScheme.primary
                    } else {
                        mutedInk.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(2.dp))
            val canReset = item.isResettableOn(today)
            MechanicalResetButton(
                onResetConfirmed = onReset,
                contentDescription = if (canReset) resetDesc else stringResource(R.string.cd_reset_cannot_reset_zero),
                tint = if (isDarkCard) Color(0xFFFAF7F2) else MaterialTheme.colorScheme.primary,
                isDarkCard = isDarkCard,
                enabled = canReset,
            )
            Spacer(Modifier.width(2.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = deleteInteraction,
                        indication = null,
                        onClick = onDelete,
                    )
                    .pressScale(deleteInteraction)
                    .semantics { contentDescription = deleteDesc },
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 2.4.dp)
                        .background(
                            if (isDarkCard) Color(0xFFE57A77)
                            else if (item.cardColor == "terracotta" || item.cardColor == "rose_clay") Color(0xFF6B1D19)
                            else MaterialTheme.colorScheme.error,
                            RoundedCornerShape(1.dp),
                        ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                // Tactile Zen mechanical odometer digit roll with tap-to-decompose
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = countRowInteraction,
                            indication = cardRipple,
                            onClick = {
                                displayMode = displayMode.next(count)
                            },
                        )
                        .pressScale(countRowInteraction, ZenTactileHierarchy.Level1Card)
                        .semantics {
                            contentDescription = tapToDecomposeDesc
                        },
                ) {
                    if (reduceMotion) {
                        OdometerDisplay(
                            decomposed = decomposed,
                            fontSize = fontSize,
                            arrivedFuture = arrivedFuture,
                            primaryInk = primaryInk,
                            mutedInk = mutedInk,
                        )
                    } else {
                        AnimatedContent(
                            targetState = effectiveMode,
                            transitionSpec = {
                                (slideInVertically(tween(220)) { height -> height } + fadeIn(tween(220))).togetherWith(
                                    slideOutVertically(tween(220)) { height -> -height } + fadeOut(tween(220))
                                ).using(SizeTransform(clip = false))
                            },
                            label = "day_odometer",
                        ) { targetMode ->
                            val targetDecomposed = remember(anchorDate, today, targetMode) {
                                decomposeTime(anchorDate, today, targetMode)
                            }
                            val targetFontSize = when (targetMode) {
                                TimeDisplayMode.DAYS -> 36.sp
                                TimeDisplayMode.ELAPSED_BREAKDOWN -> 24.sp
                                TimeDisplayMode.TOTAL_WEEKS -> 24.sp
                            }
                            OdometerDisplay(
                                decomposed = targetDecomposed,
                                fontSize = targetFontSize,
                                arrivedFuture = arrivedFuture,
                                primaryInk = primaryInk,
                                mutedInk = mutedInk,
                            )
                        }
                    }
                    val isMilestone = remember(count) { isMilestoneDay(count) }
                    if (isMilestone) {
                        val milestoneDesc = stringResource(R.string.cd_milestone_reached, count)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(6.dp)
                                .background(
                                    if (isDarkCard) ZenOchre else ZenVermilion,
                                    CircleShape,
                                )
                                .semantics {
                                    contentDescription = milestoneDesc
                                },
                        )
                    }
                }

                Text(
                    text = formatAnchorDateSubLabel(
                        count = count,
                        date = anchorDate,
                        sinceTemplate = stringResource(R.string.since_label),
                        untilTemplate = stringResource(R.string.until_label),
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = mutedInk,
                )
            }

            if (item.comment.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDarkCard) Color(0x24FFFFFF)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = item.comment,
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = 15.sp,
                            fontSize = 11.sp,
                        ),
                        color = if (isDarkCard) Color(0xFFFAF7F2) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = whisper != null,
            enter = if (reduceMotion) fadeIn() else expandVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = if (reduceMotion) fadeOut() else shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
        ) {
            if (whisper != null) {
                val undoInteraction = rememberPressSource()
                val dismissInteraction = rememberPressSource()
                val whisperBg = if (isDarkCard) Color(0x28FAF7F2) else Color(0x18D97642)
                val whisperBorder = if (isDarkCard) Color(0x40FAF7F2) else Color(0x30D97642)
                val whisperText = if (whisper.fromWidget) {
                    stringResource(R.string.widget_reset_whisper)
                } else {
                    stringResource(R.string.reset_undo_whisper, whisper.releasedDays)
                }
                val undoLabel = stringResource(R.string.action_undo)
                val dismissLabel = stringResource(R.string.action_dismiss)

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(whisperBg)
                        .border(BorderStroke(0.6.dp, whisperBorder), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = whisperText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            letterSpacing = 0.3.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (isDarkCard) Color(0xFFFAF7F2).copy(alpha = 0.9f) else primaryInk.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isDarkCard) ZenOchre.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)
                                )
                                .clickable(
                                    interactionSource = undoInteraction,
                                    indication = LocalIndication.current,
                                    onClick = onUndoReset,
                                )
                                .pressScale(undoInteraction, ZenTactileHierarchy.Level2PrimaryAction)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = undoLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = if (isDarkCard) ZenOchre else MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (whisper.fromWidget) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = dismissInteraction,
                                        indication = LocalIndication.current,
                                        onClick = onDismissWhisper,
                                    )
                                    .pressScale(dismissInteraction)
                                    .semantics { contentDescription = dismissLabel },
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 11.sp,
                                    color = if (isDarkCard) Color(0xFFFAF7F2).copy(alpha = 0.7f) else primaryInk.copy(alpha = 0.6f),
                                )
                            }
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
private fun EmptyState(
    onNewItem: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        ) {
            Text(stringResource(R.string.new_item))
        }
        Spacer(Modifier.padding(top = 10.dp))
        val restoreInteraction = rememberPressSource()
        OutlinedButton(
            onClick = onImportBackup,
            interactionSource = restoreInteraction,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.pressScale(restoreInteraction),
        ) {
            Text(stringResource(R.string.backup_action_import))
        }
    }
}

/**
 * Minimalist Zen hairline divider separating pinned cards from standard chronological cards.
 * Provides subtle negative space and a refined washi/stone rule with zero visual distraction.
 */
@Composable
private fun PinnedSectionDivider(
    modifier: Modifier = Modifier,
) {
    val zenColors = LocalZenColors.current
    val dividerDescription = stringResource(R.string.pinned_timeline_divider)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clearAndSetSemantics {
                contentDescription = dividerDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            val strokeWidth = 0.8.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.15f to zenColors.hairlineRule.copy(alpha = 0.5f),
                    0.5f to zenColors.hairlineRule.copy(alpha = 0.85f),
                    0.85f to zenColors.hairlineRule.copy(alpha = 0.5f),
                    1.0f to Color.Transparent,
                ),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

