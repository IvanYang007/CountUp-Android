package com.countup.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Modal dialog for creating or editing a [CountUpItem].
 */
@Composable
fun ItemEditorDialog(
    item: CountUpItem?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (ItemDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(item?.name ?: "") }
    var comment by rememberSaveable { mutableStateOf(item?.comment ?: "") }
    var epochDay by rememberSaveable { mutableLongStateOf(item?.epochDay ?: today.toEpochDay()) }
    var selectedIcon by rememberSaveable {
        mutableStateOf(
            if (item != null && item.icon.isNotBlank()) item.icon
            else ALL_ICON_NAMES.random()
        )
    }
    var selectedCardColor by rememberSaveable {
        mutableStateOf(
            if (item != null) item.cardColor
            else randomWhiteCardColor()
        )
    }
    var isCustomizationExpanded by rememberSaveable {
        mutableStateOf(item != null && (item.cardColor.isNotBlank() || item.icon.isNotBlank()))
    }
    var hasCustomizedManually by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = Color(0xFFFCF8F2),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(if (item == null) R.string.add_title else R.string.edit_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Notion/Bear Style Interactive Header Avatar
                val avatarInteraction = rememberPressSource()
                val activeStyle = resolveCardStyle(selectedCardColor)
                val avatarDesc = stringResource(
                    if (isCustomizationExpanded) R.string.cd_collapse_customization else R.string.cd_expand_customization
                )

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clickable(
                            interactionSource = avatarInteraction,
                            indication = LocalIndication.current,
                            onClick = { isCustomizationExpanded = !isCustomizationExpanded },
                        )
                        .pressScale(avatarInteraction)
                        .semantics { contentDescription = avatarDesc },
                ) {
                    // Main Avatar Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(activeStyle.badgeBg)
                            .border(
                                width = if (isCustomizationExpanded) 2.dp else 1.dp,
                                color = if (isCustomizationExpanded) MaterialTheme.colorScheme.primary else Color(0x2B000000),
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(iconRes(selectedIcon)),
                            contentDescription = null,
                            tint = activeStyle.badgeTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    // Edit indicator badge at bottom-right of avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ZenVermilion)
                            .border(1.5.dp, ZenWhite, CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isCustomizationExpanded) R.drawable.ic_chevron_up_mini else R.drawable.ic_pencil_edit
                            ),
                            contentDescription = null,
                            tint = ZenWhite,
                            modifier = Modifier.size(9.5.dp),
                        )
                    }
                }
            }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(scrollState),
            ) {
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (item == null && !hasCustomizedManually) {
                            val match = matchKeywordStyle(it)
                            if (match != null) {
                                selectedIcon = match.icon
                                selectedCardColor = match.cardColor
                            }
                        }
                    },
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
                Spacer(Modifier.padding(top = 14.dp))
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

                AnimatedVisibility(
                    visible = isCustomizationExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section 1: Card Background Color Selection (Fixed 4x2 Grid, No Nested Scrolling)
                        Spacer(Modifier.padding(top = 14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.card_color_label).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val activePreset = CARD_COLOR_PRESETS.firstOrNull { it.id == selectedCardColor }
                            if (activePreset != null) {
                                Text(
                                    text = stringResource(activePreset.nameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                        val colorRows = CARD_COLOR_PRESETS.chunked(4)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            colorRows.forEach { rowPresets ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    rowPresets.forEach { preset ->
                                        val isSelected = selectedCardColor == preset.id
                                        val colorInteraction = rememberPressSource()
                                        val colorName = stringResource(preset.nameRes)
                                        val colorDescription = if (preset.id == DEFAULT_CARD_COLOR) {
                                            stringResource(R.string.cd_color_default)
                                        } else {
                                            stringResource(R.string.cd_color_option, colorName)
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(preset.cardBg)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) {
                                                        if (preset.isDark) Color(0xFFDEB285) else MaterialTheme.colorScheme.primary
                                                    } else {
                                                        if (preset.isDark) Color(0x44FFFFFF) else Color(0x332C2416)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                )
                                                .clickable(
                                                    interactionSource = colorInteraction,
                                                    indication = LocalIndication.current,
                                                    onClick = {
                                                        selectedCardColor = preset.id
                                                        hasCustomizedManually = true
                                                    },
                                                )
                                                .pressScale(colorInteraction)
                                                .semantics { contentDescription = colorDescription },
                                        ) {
                                            // Mini badge circle representing the coupled icon badge
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .background(preset.badgeBg, CircleShape)
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = if (preset.isDark) Color(0x33FFFFFF) else Color(0x22000000),
                                                        shape = CircleShape,
                                                    ),
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(preset.badgeTint, CircleShape),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: Icon Picker with Categories
                        Spacer(Modifier.padding(top = 16.dp))
                        Text(
                            text = stringResource(R.string.icon_label).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(top = 6.dp))

                        // Category Tabs
                        val categoryScroll = rememberScrollState()
                        val categories = listOf("all" to R.string.category_all) + ICON_CATEGORIES.map { it.id to it.labelRes }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(categoryScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            categories.forEachIndexed { index, (catId, labelRes) ->
                                val isCatSelected = selectedCategoryIndex == index
                                val catInteraction = rememberPressSource()
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCatSelected) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        )
                                        .clickable(
                                            interactionSource = catInteraction,
                                            indication = LocalIndication.current,
                                            onClick = { selectedCategoryIndex = index },
                                        )
                                        .pressScale(catInteraction)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        color = if (isCatSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.padding(top = 8.dp))

                        // Displayed icons for selected category
                        val currentIcons = if (selectedCategoryIndex == 0) {
                            ALL_ICON_NAMES
                        } else {
                            ICON_CATEGORIES[selectedCategoryIndex - 1].iconNames
                        }

                        // Grid layout (6 columns per row)
                        val chunkedIcons = currentIcons.chunked(6)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            chunkedIcons.forEach { rowIcons ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    rowIcons.forEach { iconName ->
                                        val isIconSelected = selectedIcon == iconName
                                        val iconInteraction = rememberPressSource()
                                        val iconDesc = stringResource(iconDescriptionRes(iconName))

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isIconSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                                                )
                                                .border(
                                                    width = if (isIconSelected) 1.8.dp else 0.dp,
                                                    color = if (isIconSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp),
                                                )
                                                .clickable(
                                                    interactionSource = iconInteraction,
                                                    indication = LocalIndication.current,
                                                    onClick = {
                                                        selectedIcon = iconName
                                                        hasCustomizedManually = true
                                                    },
                                                )
                                                .pressScale(iconInteraction)
                                                .semantics { contentDescription = iconDesc },
                                        ) {
                                            Icon(
                                                painter = painterResource(iconRes(iconName)),
                                                contentDescription = null,
                                                tint = if (isIconSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    // Fill remaining slots in incomplete row
                                    if (rowIcons.size < 6) {
                                        repeat(6 - rowIcons.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ItemDraft(
                            name = name,
                            epochDay = epochDay,
                            comment = comment,
                            icon = selectedIcon,
                            cardColor = selectedCardColor,
                        )
                    )
                },
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

/**
 * Confirmation dialog for item deletion.
 */
@Composable
fun DeleteConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
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

/**
 * Fullscreen date picker dialog for selecting an anchor date.
 */
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
