package de.dh.daps.ui.screens.insulinhistory

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.BOLUS_MAX
import de.dh.daps.common.BOLUS_MIN
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinApplication
import de.dh.daps.common.domain.model.therapy.InsulinOrigin
import de.dh.daps.common.domain.model.therapy.InsulinStatus
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AbsoluteTimeStepper
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.AppColorBlue
import de.dh.daps.ui.common.composables.InsulinAmountStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.dh.daps.common.R as CommonR

@Composable
fun InsulinHistoryScreen(
    viewModel: InsulinHistoryViewModel,
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    InsulinHistoryContent(
        uiState = uiState,
        onAddManualInsulin = { amount, type, timestamp, basal, correction, meal ->
            viewModel.addManualInsulin(amount, type, timestamp, basal, correction, meal)
        },
        onUpdateManualInsulin = { app, amount, type, timestamp, basal, correction, meal ->
            viewModel.updateManualInsulin(app, amount, type, timestamp, basal, correction, meal)
        },
        onDeleteInsulin = { viewModel.deleteInsulin(it) },
        onToggleInvalidateInsulin = { viewModel.toggleInvalidated(it) },
        onSetFilters = { categories, statuses -> viewModel.setFilters(categories, statuses) },
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinHistoryContent(
    uiState: InsulinHistoryUiState,
    onAddManualInsulin: (InsulinAmount, InsulinType, Timestamp, Boolean, Boolean, Boolean) -> Unit,
    onUpdateManualInsulin: (InsulinApplication, InsulinAmount, InsulinType, Timestamp, Boolean, Boolean, Boolean) -> Unit,
    onDeleteInsulin: (InsulinApplication) -> Unit,
    onToggleInvalidateInsulin: (InsulinApplication) -> Unit = {},
    onSetFilters: (Set<InsulinCategoryFilter>, Set<InsulinStatus>) -> Unit = { _, _ -> },
    onNavigateUp: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var editingInsulin by remember { mutableStateOf<InsulinApplication?>(null) }
    var deletingInsulin by remember { mutableStateOf<InsulinApplication?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.insulin_history_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = stringResource(id = R.string.cd_filter)
                            )
                            if (uiState.isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AppColorBlue, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_meal)
                )
            }
        }
    ) { innerPadding ->
        if (showFilterDialog) {
            FilterDialog(
                selectedCategories = uiState.selectedCategories,
                selectedStatuses = uiState.selectedStatuses,
                onDismiss = { showFilterDialog = false },
                onApplyFilters = { categories, statuses ->
                    onSetFilters(categories, statuses)
                    showFilterDialog = false
                }
            )
        }

        if (showAddDialog) {
            AddManualInsulinDialog(
                availableInsulinTypes = uiState.insulinTypes,
                defaultInsulinType = uiState.defaultInsulinType,
                onDismiss = { showAddDialog = false },
                onConfirm = { amount, type, timestamp, basal, correction, meal ->
                    onAddManualInsulin(amount, type, timestamp, basal, correction, meal)
                    showAddDialog = false
                }
            )
        }

        editingInsulin?.let { insulin ->
            AddManualInsulinDialog(
                availableInsulinTypes = uiState.insulinTypes,
                defaultInsulinType = insulin.insulinType,
                initialAmount = insulin.amount,
                initialTimestamp = insulin.timestamp,
                initialBasal = insulin.basal,
                initialCorrection = insulin.correction,
                initialMeal = insulin.meal,
                isEditMode = true,
                onDismiss = { editingInsulin = null },
                onConfirm = { amount, type, timestamp, basal, correction, meal ->
                    onUpdateManualInsulin(insulin, amount, type, timestamp, basal, correction, meal)
                    editingInsulin = null
                }
            )
        }

        deletingInsulin?.let { insulin ->
            AppAlertDialog(
                onDismissRequest = { deletingInsulin = null },
                titleText = stringResource(id = R.string.delete_insulin_title),
                content = {
                    Text(text = stringResource(id = R.string.delete_insulin_message))
                },
                confirmButton = {
                    NormalTextButton(
                        onClick = {
                            onDeleteInsulin(insulin)
                            deletingInsulin = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = stringResource(id = CommonR.string.action_delete))
                    }
                },
                dismissButton = {
                    NormalTextButton(onClick = { deletingInsulin = null }) {
                        Text(text = stringResource(id = CommonR.string.cd_cancel))
                    }
                }
            )
        }

        if (uiState.insulinEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.insulin_history_empty_list),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val listState = rememberLazyListState()
            val groupedEntries = remember(uiState.insulinEntries) {
                uiState.insulinEntries.groupBy {
                    Instant.ofEpochMilli(it.timestamp.ms)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
            }
            val totalGroupedEntries = remember(uiState.allInsulinEntries) {
                uiState.allInsulinEntries.groupBy {
                    Instant.ofEpochMilli(it.timestamp.ms)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
            }
            val dailyTddMap = remember(totalGroupedEntries) {
                totalGroupedEntries.mapValues { (_, entries) ->
                    entries
                        .filter { it.status == InsulinStatus.Confirmed }
                        .sumOf { it.amount.iu }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .contentScrollIndicator(
                        scrollableState = listState,
                        showTopGradient = false,
                        topIconPadding = 48.dp
                    )
            ) {
                groupedEntries.forEach { (date, entries) ->
                    val totalCount = totalGroupedEntries[date]?.size ?: entries.size
                    val tdd = dailyTddMap[date]
                    stickyHeader {
                        DateHeader(
                            date = date,
                            displayedCount = entries.size,
                            totalCount = totalCount,
                            isFilterActive = uiState.isFilterActive,
                            tdd = tdd
                        )
                    }
                    items(entries) { entry ->
                        val isEditable = remember(entry.timestamp, entry.origin, uiState.editThresholdHours) {
                            (entry.origin == InsulinOrigin.Manual) && (entry.timestamp >= Timestamp.now().minusHours(uiState.editThresholdHours))
                        }

                        InsulinItem(
                            entry = entry,
                            isEditable = isEditable,
                            onEditClick = { editingInsulin = entry },
                            onDeleteClick = { deletingInsulin = entry },
                            onToggleInvalidateClick = { onToggleInvalidateInsulin(entry) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(
    date: LocalDate,
    displayedCount: Int = 0,
    totalCount: Int = 0,
    isFilterActive: Boolean = false,
    tdd: Double? = null
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.getDefault()) }
    val dateString = remember(date) { date.format(formatter) }

    val titleText = if (isFilterActive) {
        "$dateString " + stringResource(id = R.string.insulin_history_filter_header_count, displayedCount, totalCount)
    } else {
        dateString
    }

    val formattedTdd = insulinValue(tdd)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.insulin_history_tdd_label, formattedTdd),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterDialog(
    selectedCategories: Set<InsulinCategoryFilter>,
    selectedStatuses: Set<InsulinStatus>,
    onDismiss: () -> Unit,
    onApplyFilters: (Set<InsulinCategoryFilter>, Set<InsulinStatus>) -> Unit
) {
    var tempCategories by remember { mutableStateOf(selectedCategories) }
    var tempStatuses by remember { mutableStateOf(selectedStatuses) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        titleText = stringResource(id = R.string.insulin_history_filter_title),
        content = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(id = R.string.insulin_history_filter_category_section),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                InsulinCategoryFilter.entries.forEach { category ->
                    val label = when (category) {
                        InsulinCategoryFilter.BASAL -> stringResource(id = R.string.insulin_category_basal)
                        InsulinCategoryFilter.CORRECTION -> stringResource(id = R.string.insulin_category_correction)
                        InsulinCategoryFilter.MEAL -> stringResource(id = R.string.insulin_category_meal)
                    }
                    val isChecked = tempCategories.contains(category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempCategories = if (isChecked) {
                                    tempCategories - category
                                } else {
                                    tempCategories + category
                                }
                            }
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                tempCategories = if (checked) {
                                    tempCategories + category
                                } else {
                                    tempCategories - category
                                }
                            }
                        )
                        Text(text = label)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.insulin_history_filter_status_section),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                InsulinStatus.entries.forEach { status ->
                    val label = when (status) {
                        InsulinStatus.Confirmed -> stringResource(id = R.string.insulin_status_confirmed)
                        InsulinStatus.Scheduled -> stringResource(id = R.string.insulin_status_scheduled)
                        InsulinStatus.Cancelled -> stringResource(id = R.string.insulin_status_cancelled)
                        InsulinStatus.Invalidated -> stringResource(id = R.string.insulin_status_invalidated)
                    }
                    val isChecked = tempStatuses.contains(status)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempStatuses = if (isChecked) {
                                    tempStatuses - status
                                } else {
                                    tempStatuses + status
                                }
                            }
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                tempStatuses = if (checked) {
                                    tempStatuses + status
                                } else {
                                    tempStatuses - status
                                }
                            }
                        )
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                NormalTextButton(
                    onClick = {
                        tempCategories = emptySet()
                        tempStatuses = emptySet()
                    }
                ) {
                    Text(text = stringResource(id = R.string.insulin_history_filter_action_reset))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NormalTextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = CommonR.string.cd_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PrimaryButton(
                        onClick = {
                            onApplyFilters(tempCategories, tempStatuses)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.insulin_history_filter_action_apply))
                    }
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun InsulinItem(
    entry: InsulinApplication,
    isEditable: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleInvalidateClick: (() -> Unit)? = null
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    val timeString = remember(entry.timestamp) {
        Instant.ofEpochMilli(entry.timestamp.ms)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    val originString = when (entry.origin) {
        InsulinOrigin.Pump -> stringResource(id = R.string.insulin_origin_pump)
        InsulinOrigin.Manual -> stringResource(id = R.string.insulin_origin_manual)
    }

    ListItem(
        modifier = if (isEditable) Modifier.clickable(onClick = onEditClick) else Modifier,
        headlineContent = {
            Text(
                text = stringResource(id = R.string.insulin_unit_label_format, entry.amount.iu),
                style = if (entry.status == InsulinStatus.Invalidated) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )
        },
        supportingContent = {
            Column {
                Row {
                    Text(text = originString)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.insulinType.name,
                        color = Color.DarkGray
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(entry.status)
                    if (entry.basal) CategoryBadge(stringResource(R.string.insulin_category_basal))
                    if (entry.correction) CategoryBadge(stringResource(R.string.insulin_category_correction))
                    if (entry.meal) CategoryBadge(stringResource(R.string.insulin_category_meal))
                }
            }
        },
        trailingContent = {
            var menuExpanded by remember { mutableStateOf(false) }
            val isInvalidated = entry.status == InsulinStatus.Invalidated
            val canInvalidate = onToggleInvalidateClick != null && (entry.status == InsulinStatus.Confirmed || isInvalidated)
            val hasActions = isEditable || canInvalidate

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (hasActions) {
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(id = CommonR.string.cd_more_options),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (isEditable) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = CommonR.string.cd_edit)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onEditClick()
                                    }
                                )
                            }
                            if (canInvalidate) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isInvalidated) stringResource(R.string.insulin_history_action_restore)
                                            else stringResource(R.string.insulin_history_action_invalidate)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = if (isInvalidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleInvalidateClick()
                                    }
                                )
                            }
                            if (isEditable) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = CommonR.string.action_delete)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StatusBadge(status: InsulinStatus) {
    if (status == InsulinStatus.Confirmed) return

    val (text, containerColor, contentColor) = when (status) {
        InsulinStatus.Scheduled -> Triple(
            stringResource(R.string.insulin_status_scheduled),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        InsulinStatus.Cancelled -> Triple(
            stringResource(R.string.insulin_status_cancelled),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        InsulinStatus.Invalidated -> Triple(
            stringResource(R.string.insulin_status_invalidated),
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        InsulinStatus.Confirmed -> return
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = contentColor
        )
    }
}

@Composable
fun CategoryBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualInsulinDialog(
    availableInsulinTypes: List<InsulinType>,
    defaultInsulinType: InsulinType?,
    initialAmount: InsulinAmount = InsulinAmount.ZERO,
    initialTimestamp: Timestamp = Timestamp.now(),
    initialBasal: Boolean = false,
    initialCorrection: Boolean = false,
    initialMeal: Boolean = false,
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (InsulinAmount, InsulinType, Timestamp, Boolean, Boolean, Boolean) -> Unit
) {
    var amount by remember { mutableDoubleStateOf(initialAmount.iu) }
    var timestamp by remember { mutableStateOf(initialTimestamp) }
    var basal by remember { mutableStateOf(initialBasal) }
    var correction by remember { mutableStateOf(initialCorrection) }
    var meal by remember { mutableStateOf(initialMeal) }
    var selectedInsulinType by remember { mutableStateOf(defaultInsulinType ?: availableInsulinTypes.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        titleText = if (isEditMode) stringResource(id = R.string.insulin_history_edit_manual_title)
        else stringResource(id = R.string.insulin_history_add_manual_title),
        content = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(id = R.string.insulin_history_add_manual_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableInsulinTypes.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedTypeText = selectedInsulinType?.let { type ->
                            if (!type.activeSubstance.isNullOrBlank()) "${type.name} (${type.activeSubstance})" else type.name
                        } ?: ""
                        OutlinedTextField(
                            value = selectedTypeText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.insulin_profile_editor_insulin_type_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableInsulinTypes.forEach { type ->
                                val typeText = if (!type.activeSubstance.isNullOrBlank()) "${type.name} (${type.activeSubstance})" else type.name
                                DropdownMenuItem(
                                    text = { Text(typeText) },
                                    onClick = {
                                        selectedInsulinType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.insulin_history_add_manual_time_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    val now = remember { Timestamp.now() }
                    val minTime = remember(now) { now.minusHours(10) }
                    val maxTime = remember(now) { now.plusHours(1) }
                    AbsoluteTimeStepper(
                        currentTime = timestamp,
                        onTimeChange = { timestamp = it },
                        minTime = minTime,
                        maxTime = maxTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.insulin_history_add_manual_amount_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    InsulinAmountStepper(
                        currentValue = amount,
                        onValueChange = { amount = it },
                        minValue = BOLUS_MIN,
                        maxValue = BOLUS_MAX
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { basal = !basal }) {
                        Checkbox(checked = basal, onCheckedChange = { basal = it })
                        Text(text = stringResource(R.string.insulin_category_basal))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { correction = !correction }) {
                        Checkbox(checked = correction, onCheckedChange = { correction = it })
                        Text(text = stringResource(R.string.insulin_category_correction))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { meal = !meal }) {
                        Checkbox(checked = meal, onCheckedChange = { meal = it })
                        Text(text = stringResource(R.string.insulin_category_meal))
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = {
                    val type = selectedInsulinType
                    if (amount > 0 && type != null) {
                        onConfirm(InsulinAmount(amount), type, timestamp, basal, correction, meal)
                    }
                },
                enabled = amount > 0 && selectedInsulinType != null
            ) {
                Text(text = stringResource(id = CommonR.string.action_save))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(text = stringResource(id = CommonR.string.cd_cancel))
            }
        }
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun InsulinHistoryPreview() {
    AppPreview {
        InsulinHistoryContent(
            uiState = InsulinHistoryUiState(),
            onAddManualInsulin = { _, _, _, _, _, _ -> },
            onUpdateManualInsulin = { _, _, _, _, _, _, _ -> },
            onDeleteInsulin = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "With Data")
@Composable
fun InsulinHistoryWithDataPreview() {
    val sampleInsulinType = InsulinType(name = "Fiasp", activeSubstance = "Insulin aspart", peak = Minutes(50.toShort()), dia = Minutes(300.toShort()))
    val sampleEntries = listOf(
        InsulinApplication(id = 1, timestamp = Timestamp.now().minusHours(8), amount = InsulinAmount(5.0), insulinType = sampleInsulinType, origin = InsulinOrigin.Pump, meal = true, correction = true),
        InsulinApplication(id = 2, timestamp = Timestamp.now().minusHours(5), amount = InsulinAmount(2.5), insulinType = sampleInsulinType, origin = InsulinOrigin.Manual, meal = true),
        InsulinApplication(id = 3, timestamp = Timestamp.now().minusHours(1), amount = InsulinAmount(3.0), insulinType = sampleInsulinType, origin = InsulinOrigin.Manual, basal = true)
    )

    AppPreview {
        InsulinHistoryContent(
            uiState = InsulinHistoryUiState(insulinEntries = sampleEntries),
            onAddManualInsulin = { _, _, _, _, _, _ -> },
            onUpdateManualInsulin = { _, _, _, _, _, _, _ -> },
            onDeleteInsulin = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showSystemUi = true, name = "Add Manual Insulin Dialog")
@Composable
private fun AddManualInsulinDialogPreview() {
    val sampleInsulinType = InsulinType(
        name = "Fiasp",
        activeSubstance = "Insulin aspart",
        peak = Minutes(50.toShort()),
        dia = Minutes(300.toShort())
    )
    AppPreview {
        AddManualInsulinDialog(
            availableInsulinTypes = listOf(sampleInsulinType),
            defaultInsulinType = sampleInsulinType,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showSystemUi = true, name = "Filter Dialog")
@Composable
private fun FilterDialogPreview() {
    AppPreview {
        FilterDialog(
            selectedCategories = setOf(InsulinCategoryFilter.BASAL),
            selectedStatuses = setOf(InsulinStatus.Confirmed, InsulinStatus.Scheduled),
            onDismiss = {},
            onApplyFilters = { _, _ -> }
        )
    }
}