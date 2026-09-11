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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
 * Resolves the surface color and border modifier for Zen modal dialogs.
 */
@Composable
private fun zenDialogStyle(shape: RoundedCornerShape): Pair<Color, Modifier> {
    val zenColors = LocalZenColors.current
    val surface = if (zenColors.isDark) zenColors.paperSurface else Color(0xFFFCF8F2)
    val border = if (zenColors.isDark) Modifier.border(1.dp, zenColors.hairlineRule, shape) else Modifier
    return Pair(surface, border)
}

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
    isSaving: Boolean = false,
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
            item?.cardColor ?: DEFAULT_CARD_COLOR
        )
    }
    var isPinned by rememberSaveable { mutableStateOf(item?.isPinned ?: false) }
    var isCustomizationExpanded by rememberSaveable {
        mutableStateOf(item != null && (item.cardColor.isNotBlank() || item.icon.isNotBlank()))
    }
    var hasCustomizedManually by rememberSaveable { mutableStateOf(false) }
    val isDarkTheme = LocalZenColors.current.isDark
    var selectedColorCategoryIndex by rememberSaveable {
        val initialPresetId = resolveCardStyle(selectedCardColor, isDark = isDarkTheme).id
        val idx = CARD_COLOR_CATEGORIES.indexOfFirst { cat -> cat.presetIds.contains(initialPresetId) }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val (dialogSurface, dialogBorder) = zenDialogStyle(RoundedCornerShape(20.dp))

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(dialogBorder),
        containerColor = dialogSurface,
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
            val pinContentDesc = stringResource(R.string.cd_pin_to_top)
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
                    .imePadding()
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
                                val matchedPresetId = resolveCardStyle(match.cardColor).id
                                val catIdx = CARD_COLOR_CATEGORIES.indexOfFirst { cat -> cat.presetIds.contains(matchedPresetId) }
                                if (catIdx >= 0) {
                                    selectedColorCategoryIndex = catIdx
                                }
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

                // Pin to top toggle row (Zen minimalist switch)
                Spacer(Modifier.padding(top = 10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isPinned = !isPinned }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.pin_to_top).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        modifier = Modifier.semantics {
                            contentDescription = pinContentDesc
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenWhite,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    )
                }

                AnimatedVisibility(
                    visible = isCustomizationExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section 1: Card Style Selection with Zen Categorized Tabs (Washi, Earth, Sumi)
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
                            val activePreset = resolveCardStyle(selectedCardColor, isDark = isDarkTheme)
                            Text(
                                text = stringResource(activePreset.nameRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.padding(top = 6.dp))

                        // Category Tabs (Washi, Earth, Sumi)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CARD_COLOR_CATEGORIES.forEachIndexed { index, category ->
                                key(category.id) {
                                    val isCatSelected = selectedColorCategoryIndex == index
                                    val catInteraction = rememberPressSource()
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCatSelected) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            )
                                            .clickable(
                                                interactionSource = catInteraction,
                                                indication = LocalIndication.current,
                                                role = Role.Tab,
                                                onClick = { selectedColorCategoryIndex = index },
                                            )
                                            .pressScale(catInteraction)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .semantics {
                                                selected = isCatSelected
                                            },
                                    ) {
                                        Text(
                                            text = stringResource(category.labelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = if (isCatSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.padding(top = 8.dp))

                        // Single clean row of 4 chips for active category
                        val activeColorCategory = CARD_COLOR_CATEGORIES.getOrElse(selectedColorCategoryIndex) { CARD_COLOR_CATEGORIES[0] }
                        val currentCategoryPresets = activeColorCategory.presetIds.map { resolveCardStyle(it, isDark = isDarkTheme) }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            currentCategoryPresets.forEach { preset ->
                                key(preset.id) {
                                    val isSelected = resolveCardStyle(selectedCardColor, isDark = isDarkTheme).id == preset.id
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
                                                    if (preset.isDark) Color(0x24FFFFFF) else Color(0x332C2416)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                            .clickable(
                                                interactionSource = colorInteraction,
                                                indication = LocalIndication.current,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    selectedCardColor = preset.id
                                                    hasCustomizedManually = true
                                                },
                                            )
                                            .pressScale(colorInteraction)
                                            .semantics {
                                                contentDescription = colorDescription
                                                selected = isSelected
                                            },
                                    ) {
                                        // Mini badge circle representing the coupled icon badge
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(preset.badgeBg, CircleShape)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = if (preset.isDark) Color(0x26FFFFFF) else Color(0x22000000),
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
            val saveInteraction = rememberPressSource()
            Button(
                onClick = {
                    onSave(
                        ItemDraft(
                            name = name,
                            epochDay = epochDay,
                            comment = comment,
                            icon = selectedIcon,
                            cardColor = selectedCardColor,
                            isPinned = isPinned,
                        )
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.pressScale(saveInteraction, ZenTactileHierarchy.Level2PrimaryAction),
                interactionSource = saveInteraction,
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
    val (dialogSurface, dialogBorder) = zenDialogStyle(RoundedCornerShape(20.dp))

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(dialogBorder),
        containerColor = dialogSurface,
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
            val deleteInteraction = rememberPressSource()
            Button(
                onClick = onConfirm,
                modifier = Modifier.pressScale(
                    interactionSource = deleteInteraction,
                    targetScale = ZenTactileHierarchy.Level3Destructive,
                    hapticFeedbackType = HapticFeedbackType.LongPress,
                ),
                interactionSource = deleteInteraction,
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
    val (dialogSurface, dialogBorder) = zenDialogStyle(RoundedCornerShape(24.dp))

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).then(dialogBorder),
        containerColor = dialogSurface,
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
                containerColor = dialogSurface,
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
            val dateConfirmInteraction = rememberPressSource()
            Button(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onDatePicked(datePickerMillisToLocalDate(millis))
                    onDismiss()
                },
                modifier = Modifier.pressScale(
                    interactionSource = dateConfirmInteraction,
                    targetScale = ZenTactileHierarchy.Level2PrimaryAction,
                    enabled = state.selectedDateMillis != null,
                ),
                interactionSource = dateConfirmInteraction,
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

/**
 * Modal dialog for previewing an imported backup file and choosing a restore strategy.
 */
@Composable
fun BackupRestorePreviewDialog(
    payload: CountUpBackupPayload,
    onDismiss: () -> Unit,
    onConfirmRestore: (RestoreStrategy) -> Unit,
    modifier: Modifier = Modifier,
    isDamaged: Boolean = false,
) {
    var selectedStrategy by rememberSaveable { mutableStateOf(RestoreStrategy.MERGE_KEEP_EXISTING) }
    val (dialogSurface, dialogBorder) = zenDialogStyle(RoundedCornerShape(20.dp))
    val zenColors = LocalZenColors.current

    val formattedDate = remember(payload.exportTimestamp) {
        if (payload.exportTimestamp > 0L) {
            try {
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
                    .format(java.util.Date(payload.exportTimestamp))
            } catch (_: Exception) {
                null
            }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(dialogBorder),
        containerColor = dialogSurface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.backup_restore_preview_title).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (isDamaged) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.backup_restore_damaged_notice, payload.items.size),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(zenColors.paperCard)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            text = if (payload.items.size == 1) {
                                stringResource(R.string.backup_restore_items_count_one)
                            } else {
                                stringResource(R.string.backup_restore_items_count, payload.items.size)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.backup_restore_version, payload.appVersion),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (formattedDate != null) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.backup_restore_date, formattedDate),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                RestoreStrategyOptionCard(
                    title = stringResource(R.string.backup_restore_strategy_merge),
                    description = stringResource(R.string.backup_restore_strategy_merge_desc),
                    isSelected = selectedStrategy == RestoreStrategy.MERGE_KEEP_EXISTING,
                    onClick = { selectedStrategy = RestoreStrategy.MERGE_KEEP_EXISTING },
                )

                Spacer(Modifier.height(8.dp))

                RestoreStrategyOptionCard(
                    title = stringResource(R.string.backup_restore_strategy_replace),
                    description = stringResource(R.string.backup_restore_strategy_replace_desc),
                    isSelected = !isDamaged && selectedStrategy == RestoreStrategy.REPLACE_ALL,
                    enabled = !isDamaged,
                    onClick = { if (!isDamaged) selectedStrategy = RestoreStrategy.REPLACE_ALL },
                )
            }
        },
        confirmButton = {
            val confirmInteraction = rememberPressSource()
            Button(
                onClick = { onConfirmRestore(if (isDamaged) RestoreStrategy.MERGE_KEEP_EXISTING else selectedStrategy) },
                modifier = Modifier.pressScale(
                    interactionSource = confirmInteraction,
                    targetScale = ZenTactileHierarchy.Level2PrimaryAction,
                ),
                interactionSource = confirmInteraction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.backup_restore_confirm),
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
private fun RestoreStrategyOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val zenColors = LocalZenColors.current
    val borderModifier = if (isSelected && enabled) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, zenColors.hairlineRule, RoundedCornerShape(12.dp))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(borderModifier)
            .background(if (isSelected && enabled) zenColors.paperCard else Color.Transparent)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                selected = isSelected
                if (!enabled) disabled()
            }
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected && enabled) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (isSelected && enabled) "●" else "○",
            fontSize = 14.sp,
            color = if (isSelected && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .clearAndSetSemantics { },
        )
    }
}

/**
 * Modal dialog presenting offline data backup export and restore actions.
 */
@Composable
fun DataBackupSettingsDialog(
    appVersion: String,
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (dialogSurface, dialogBorder) = zenDialogStyle(RoundedCornerShape(22.dp))
    val zenColors = LocalZenColors.current
    val cancelDesc = stringResource(R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(dialogBorder),
        containerColor = dialogSurface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_dialog_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val closeInteraction = rememberPressSource()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = LocalIndication.current,
                            onClick = onDismiss,
                        )
                        .pressScale(closeInteraction)
                        .semantics { contentDescription = cancelDesc },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(zenColors.paperCard)
                            .border(1.dp, zenColors.hairlineRule, CircleShape),
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Export Backup Card
                val exportInteraction = rememberPressSource()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, zenColors.hairlineRule, RoundedCornerShape(12.dp))
                        .background(zenColors.paperCard)
                        .clickable(
                            interactionSource = exportInteraction,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = {
                                onDismiss()
                                onExportBackup()
                            },
                        )
                        .pressScale(exportInteraction)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_action_export),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.backup_action_export_desc),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "↗",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Restore Backup Card
                val restoreInteraction = rememberPressSource()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, zenColors.hairlineRule, RoundedCornerShape(12.dp))
                        .background(zenColors.paperCard)
                        .clickable(
                            interactionSource = restoreInteraction,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = {
                                onDismiss()
                                onRestoreBackup()
                            },
                        )
                        .pressScale(restoreInteraction)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_action_import),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.backup_action_import_desc),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "↙",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Versioning & Privacy Footer
                Text(
                    text = stringResource(R.string.settings_footer_offline, appVersion),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
    )
}
