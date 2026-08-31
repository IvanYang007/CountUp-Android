package com.countup.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawAbstractBackground(state.backgroundTheme, state.today.toEpochDay()),
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
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.padding(top = 16.dp))
                HeaderRow(
                    backgroundTheme = state.backgroundTheme,
                    onCycleBackground = { onEvent(CountUpUiEvent.CycleBackground) },
                    onNewItem = { onEvent(CountUpUiEvent.OpenEditor(null)) },
                )
                Spacer(Modifier.padding(top = 16.dp))

                if (state.items.isEmpty()) {
                    EmptyState(
                        onNewItem = { onEvent(CountUpUiEvent.OpenEditor(null)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                } else {
                    SubHeaderRow(
                        itemCount = displayItems.size,
                        sortOrder = state.sortOrder,
                        searchQuery = state.searchQuery,
                        isMenuOpen = state.isSearchSortMenuOpen,
                        onToggleMenu = { onEvent(CountUpUiEvent.SetSearchSortMenuOpen(it)) },
                        onSearchChanged = { onEvent(CountUpUiEvent.SearchQueryChanged(it)) },
                        onClearSearch = { onEvent(CountUpUiEvent.ClearSearch) },
                        onSelectSortOrder = { onEvent(CountUpUiEvent.SortOrderSelected(it)) },
                    )
                    Spacer(Modifier.padding(top = 8.dp))

                    if (displayItems.isEmpty() && state.searchQuery.isNotEmpty()) {
                        EmptySearchState(
                            onClearSearch = { onEvent(CountUpUiEvent.ClearSearch) },
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                        ) {
                            items(
                                items = displayItems,
                                key = { it.id },
                                contentType = { "item_card" },
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    onClick = { onEvent(CountUpUiEvent.OpenEditor(item)) },
                                    onDelete = { onEvent(CountUpUiEvent.RequestDelete(item)) },
                                    onReset = { onEvent(CountUpUiEvent.RequestReset(item)) },
                                    onToggleWidget = { onEvent(CountUpUiEvent.ToggleWidgetVisibility(item.id)) },
                                    modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                                    today = state.today,
                                    reduceMotion = reduceMotion,
                                )
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

        state.pendingReset?.let { target ->
            ResetConfirmDialog(
                itemName = target.name,
                onDismiss = { onEvent(CountUpUiEvent.DismissReset) },
                onConfirm = { onEvent(CountUpUiEvent.ConfirmReset(target.id)) },
            )
        }
    }
}

@Composable
private fun HeaderRow(
    backgroundTheme: BackgroundTheme,
    onCycleBackground: () -> Unit,
    onNewItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ensoInteraction = rememberPressSource()
    val themeLabel = stringResource(backgroundTheme.labelRes)
    val newItemLabel = stringResource(R.string.new_item)
    val plusInteraction = rememberPressSource()

    Row(
        verticalAlignment = Alignment.CenterVertically,
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
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
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SubHeaderRow(
    itemCount: Int,
    sortOrder: SortOrder,
    searchQuery: String,
    isMenuOpen: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
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
                        onClick = { onToggleMenu(!isMenuOpen) },
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .width(260.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
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
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(onClick = onClearSearch),
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
            }
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
    today: LocalDate = LocalDate.now(),
    reduceMotion: Boolean = false,
) {
    val count = daysSince(LocalDate.ofEpochDay(item.epochDay), today)
    val cardInteraction = rememberPressSource()
    val resetInteraction = rememberPressSource()
    val deleteInteraction = rememberPressSource()
    val widgetInteraction = rememberPressSource()

    val resetDesc = stringResource(R.string.reset)
    val deleteDesc = stringResource(R.string.delete)
    val widgetVisible = item.showInWidget
    val widgetDesc = stringResource(if (widgetVisible) R.string.widget_hide else R.string.widget_show)

    val style = resolveCardStyle(item.cardColor)
    val isCustomCardColor = item.cardColor.isNotBlank()
    val baseBgColor = style.cardBg
    val isDarkCard = style.isDark
    val primaryInk = style.primaryInk
    val mutedInk = style.mutedInk

    val arrivedFuture = item.futureFlag && count >= 0
    val accent = if (arrivedFuture) ZenArrivedGreen else style.badgeBg
    val onAccent = if (arrivedFuture) ZenWhite else style.badgeTint

    val pluralSelector = kotlin.math.abs(count).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val unitLabel = pluralStringResource(R.plurals.days_unit, pluralSelector, count)

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
                Color.White.copy(alpha = 0.90f),
                Color(0xFFD8C7B0).copy(alpha = 0.65f),
            ),
        )
    }

    val cardModifier = if (isCustomCardColor) {
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDarkCard) 4.dp else 2.5.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x182C2416),
                spotColor = Color(0x222C2416),
            )
            .border(
                width = 1.dp,
                color = cardBorderColor(baseBgColor),
                shape = cardShape,
            )
            .background(
                color = baseBgColor,
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
                width = 1.dp,
                brush = borderBrush,
                shape = cardShape,
            )
            .background(
                brush = surfaceBrush,
                shape = cardShape,
            )
    }

    Column(
        modifier = cardModifier
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .pressScale(cardInteraction, 0.985f)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
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
                color = mutedInk,
                modifier = Modifier.weight(1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = widgetInteraction,
                        indication = LocalIndication.current,
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
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
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
                    tint = if (isDarkCard) Color(0xFFFAF7F2) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
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
                // Tactile Zen mechanical odometer digit roll
                Row(verticalAlignment = Alignment.Bottom) {
                    if (reduceMotion) {
                        Text(
                            text = count.toString(),
                            fontSize = 44.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = if (arrivedFuture) ZenArrivedRed else primaryInk,
                        )
                    } else {
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically(tween(220)) { height -> height } + fadeIn(tween(220))).togetherWith(
                                        slideOutVertically(tween(220)) { height -> -height } + fadeOut(tween(220))
                                    )
                                } else {
                                    (slideInVertically(tween(220)) { height -> -height } + fadeIn(tween(220))).togetherWith(
                                        slideOutVertically(tween(220)) { height -> height } + fadeOut(tween(220))
                                    )
                                }.using(SizeTransform(clip = false))
                            },
                            label = "day_odometer",
                        ) { targetCount ->
                            Text(
                                text = targetCount.toString(),
                                fontSize = 44.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = if (arrivedFuture) ZenArrivedRed else primaryInk,
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = unitLabel,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = mutedInk,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
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
                        date = LocalDate.ofEpochDay(item.epochDay),
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
        ) {
            Text(stringResource(R.string.new_item))
        }
    }
}

