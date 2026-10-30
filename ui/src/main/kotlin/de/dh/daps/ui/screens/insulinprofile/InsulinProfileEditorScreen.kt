package de.dh.daps.ui.screens.insulinprofile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.BASAL_MAX
import de.dh.daps.common.BASAL_MIN
import de.dh.daps.common.CR_MAX
import de.dh.daps.common.CR_MIN
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.ISF_MAX
import de.dh.daps.common.ISF_MIN
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.Block
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.ProfileInsulinSetting
import de.dh.daps.ui.R
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.ValueDisplayStrategy
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.StepperDefaults
import de.dh.daps.ui.common.composables.TimeHourSelector
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.isfUnitLabel
import de.dh.daps.ui.common.theme.AppPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import de.dh.daps.common.R as CommonR

data class EditableInsulinSettingState(
    val id: String = UUID.randomUUID().toString(),
    val insulinType: InsulinType,
    val dia: String,
    val peak: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinProfileDetailEditor(
    profile: InsulinProfile,
    insulinTypes: List<InsulinType>,
    onSave: (InsulinProfile) -> Unit,
    onCancel: () -> Unit,
    isNameUnique: (String, Long) -> Boolean = { _, _ -> true }
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var basalBlocks by remember(profile) { mutableStateOf(profile.basalBlocks) }
    var isfBlocks by remember(profile) { mutableStateOf(profile.isfBlocks) }
    var crBlocks by remember(profile) { mutableStateOf(profile.crBlocks) }

    val initialSettings = remember(profile) {
        if (profile.insulinSettings.isNotEmpty()) {
            profile.insulinSettings.map { setting ->
                EditableInsulinSettingState(
                    insulinType = setting.insulinType,
                    dia = setting.dia.value.toString(),
                    peak = setting.peak.value.toString()
                )
            }
        } else if (insulinTypes.isNotEmpty()) {
            insulinTypes.map { type ->
                EditableInsulinSettingState(
                    insulinType = type,
                    dia = type.dia.value.toString(),
                    peak = type.peak.value.toString()
                )
            }
        } else emptyList()
    }
    var insulinSettings by remember(profile) { mutableStateOf(initialSettings) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val isNameValid = name.trim().isNotBlank() && isNameUnique(name.trim(), profile.id)
    val areSettingsValid = insulinSettings.isNotEmpty() && insulinSettings.all { setting ->
        val diaVal = setting.dia.toIntOrNull() ?: 0
        val peakVal = setting.peak.toIntOrNull() ?: 0
        diaVal > 0 && peakVal > 0
    }

    val currentSettingsMapped = remember(insulinSettings) {
        insulinSettings.map { setting ->
            val diaVal = setting.dia.toIntOrNull() ?: 0
            val peakVal = setting.peak.toIntOrNull() ?: 0
            ProfileInsulinSetting(
                insulinType = setting.insulinType,
                dia = Minutes(diaVal.toShort()),
                peak = Minutes(peakVal.toShort())
            )
        }
    }

    val hasChanges = name != profile.name ||
            basalBlocks != profile.basalBlocks ||
            isfBlocks != profile.isfBlocks ||
            crBlocks != profile.crBlocks ||
            currentSettingsMapped != profile.insulinSettings

    fun handleBack() {
        if (hasChanges) {
            showDiscardConfirmation = true
        } else {
            onCancel()
        }
    }

    fun saveChanges() {
        onSave(
            profile.copy(
                name = name.trim(),
                basalBlocks = basalBlocks,
                isfBlocks = isfBlocks,
                crBlocks = crBlocks,
                insulinSettings = currentSettingsMapped
            )
        )
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(
                    if (profile.id == ID_UNDEFINED) stringResource(id = R.string.insulin_profile_editor_new_profile)
                    else stringResource(id = R.string.insulin_profile_editor_edit_profile)
                ),
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            imageVector = if (hasChanges) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                id = if (hasChanges) CommonR.string.cd_cancel
                                else CommonR.string.cd_navigate_up
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::saveChanges,
                        enabled = isNameValid && areSettingsValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(id = R.string.cd_save_insulin_profile)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        InsulinProfileEditorContent(
            name = name,
            onNameChange = { name = it },
            isNameValid = isNameValid,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            insulinTypes = insulinTypes,
            insulinSettings = insulinSettings,
            onInsulinSettingsChanged = { insulinSettings = it },
            basalBlocks = basalBlocks,
            onBasalBlocksChanged = { basalBlocks = it },
            isfBlocks = isfBlocks,
            onIsfBlocksChanged = { isfBlocks = it },
            crBlocks = crBlocks,
            onCrBlocksChanged = { crBlocks = it },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showDiscardConfirmation) {
        AppAlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(id = R.string.insulin_profile_editor_discard_title)) },
            content = { Text(stringResource(id = R.string.insulin_profile_editor_discard_message)) },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        saveChanges()
                    },
                    enabled = isNameValid && areSettingsValid
                ) {
                    Text(stringResource(id = CommonR.string.action_save))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = {
                    showDiscardConfirmation = false
                    onCancel()
                }) {
                    Text(stringResource(id = R.string.discard_confirm_button))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinProfileEditorContent(
    name: String,
    onNameChange: (String) -> Unit,
    insulinTypes: List<InsulinType>,
    insulinSettings: List<EditableInsulinSettingState>,
    onInsulinSettingsChanged: (List<EditableInsulinSettingState>) -> Unit,
    basalBlocks: List<Block>,
    onBasalBlocksChanged: (List<Block>) -> Unit,
    isfBlocks: List<Block>,
    onIsfBlocksChanged: (List<Block>) -> Unit,
    crBlocks: List<Block>,
    onCrBlocksChanged: (List<Block>) -> Unit,
    modifier: Modifier = Modifier,
    isNameValid: Boolean = true,
    selectedTab: Int = 0,
    onTabSelected: ((Int) -> Unit)? = null
) {
    var internalSelectedTab by remember { mutableIntStateOf(selectedTab) }
    val currentTab = if (onTabSelected != null) selectedTab else internalSelectedTab

    val tabs = listOf(
        stringResource(id = R.string.insulin_profile_editor_tab_insulin),
        stringResource(id = R.string.insulin_profile_editor_tab_basal),
        stringResource(id = R.string.insulin_profile_editor_tab_isf),
        stringResource(id = R.string.insulin_profile_editor_tab_cr)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(id = R.string.insulin_profile_editor_name_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            isError = !isNameValid,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        SecondaryTabRow(selectedTabIndex = currentTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = {
                        internalSelectedTab = index
                        onTabSelected?.invoke(index)
                    },
                    text = {
                        Text(
                            text = title,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true,
                            style = MaterialTheme.typography.labelLarge.copy(
                                hyphens = Hyphens.Auto,
                                lineBreak = LineBreak.Heading
                            )
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                0 -> InsulinSettingsEditor(
                    insulinTypes = insulinTypes,
                    insulinSettings = insulinSettings,
                    onInsulinSettingsChanged = onInsulinSettingsChanged
                )
                1 -> TherapyBlockListEditor(
                    title = stringResource(id = R.string.insulin_profile_editor_basal_title),
                    description = stringResource(id = R.string.insulin_profile_editor_basal_desc) + " " + stringResource(R.string.insulin_profile_editor_unit_info_format, stringResource(CommonR.string.unit_u_per_h)),
                    blocks = basalBlocks,
                    onBlocksChanged = onBasalBlocksChanged,
                    step = 0.05,
                    format = "%.2f",
                    minValue = BASAL_MIN,
                    maxValue = BASAL_MAX
                )
                2 -> {
                    val glucoseUnit = LocalGlucoseUnit.current
                    val isfStep = if (glucoseUnit == GlucoseUnit.MG_DL) 1.0 else 0.1
                    val isfFormat = if (glucoseUnit == GlucoseUnit.MG_DL) "%.0f" else "%.1f"
                    val isfMin = if (glucoseUnit == GlucoseUnit.MG_DL) ISF_MIN else BgDelta.fromMgDl(ISF_MIN.toInt().toShort()).mmol
                    val isfMax = if (glucoseUnit == GlucoseUnit.MG_DL) ISF_MAX else BgDelta.fromMgDl(ISF_MAX.toInt().toShort()).mmol

                    val displayBlocks = remember(isfBlocks, glucoseUnit) {
                        isfBlocks.map { block ->
                            block.copy(amount = if (glucoseUnit == GlucoseUnit.MG_DL) block.amount else BgDelta.fromMgDl(block.amount.toInt().toShort()).mmol)
                        }
                    }

                    TherapyBlockListEditor(
                        title = stringResource(id = R.string.insulin_profile_editor_isf_title),
                        description = stringResource(id = R.string.insulin_profile_editor_isf_desc) + " " + stringResource(R.string.insulin_profile_editor_unit_info_format, isfUnitLabel()),
                        blocks = displayBlocks,
                        onBlocksChanged = { newDisplayBlocks ->
                            val updatedBlocks = newDisplayBlocks.map { block ->
                                block.copy(amount = if (glucoseUnit == GlucoseUnit.MG_DL) block.amount else BgDelta.fromMmol(block.amount).mgdl)
                            }
                            onIsfBlocksChanged(updatedBlocks)
                        },
                        step = isfStep,
                        format = isfFormat,
                        minValue = isfMin,
                        maxValue = isfMax
                    )
                }
                3 -> TherapyBlockListEditor(
                    title = stringResource(id = R.string.insulin_profile_editor_cr_title),
                    description = stringResource(id = R.string.insulin_profile_editor_cr_desc) + " " + stringResource(R.string.insulin_profile_editor_unit_info_format, stringResource(CommonR.string.unit_g_per_u)),
                    blocks = crBlocks,
                    onBlocksChanged = onCrBlocksChanged,
                    step = 0.1,
                    format = "%.1f",
                    minValue = CR_MIN,
                    maxValue = CR_MAX
                )
            }
        }
    }
}

@Composable
fun InsulinSettingsEditor(
    insulinTypes: List<InsulinType>,
    insulinSettings: List<EditableInsulinSettingState>,
    onInsulinSettingsChanged: (List<EditableInsulinSettingState>) -> Unit
) {
    val scrollState = rememberScrollState()
    var showAddMenu by remember { mutableStateOf(false) }

    val unconfiguredTypes = remember(insulinTypes, insulinSettings) {
        insulinTypes.filter { type ->
            insulinSettings.none { it.insulinType.id == type.id }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .contentScrollIndicator(scrollState)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.insulin_profile_editor_multi_insulin_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.insulin_profile_editor_multi_insulin_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        insulinSettings.forEachIndexed { index, setting ->
            InsulinSettingCard(
                setting = setting,
                onSettingChanged = { updatedSetting ->
                    val newList = insulinSettings.toMutableList()
                    newList[index] = updatedSetting
                    onInsulinSettingsChanged(newList)
                },
                onDelete = {
                    val newList = insulinSettings.toMutableList()
                    newList.removeAt(index)
                    onInsulinSettingsChanged(newList)
                }
            )
        }

        val buttonText = if (unconfiguredTypes.isNotEmpty()) {
            stringResource(id = R.string.insulin_profile_editor_add_insulin_setting, unconfiguredTypes.size)
        } else {
            stringResource(id = R.string.insulin_profile_editor_all_insulins_configured)
        }
        val buttonIcon = if (unconfiguredTypes.isNotEmpty()) Icons.Default.Add else Icons.Default.Check

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showAddMenu = true },
                enabled = unconfiguredTypes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = buttonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DropdownMenu(
                expanded = showAddMenu && unconfiguredTypes.isNotEmpty(),
                onDismissRequest = { showAddMenu = false }
            ) {
                unconfiguredTypes.forEach { type ->
                    val itemText = if (!type.activeSubstance.isNullOrBlank()) {
                        "${type.name} (${type.activeSubstance})"
                    } else {
                        type.name
                    }
                    DropdownMenuItem(
                        text = { Text(itemText) },
                        onClick = {
                            showAddMenu = false
                            val newSetting = EditableInsulinSettingState(
                                insulinType = type,
                                dia = type.dia.value.toString(),
                                peak = type.peak.value.toString()
                            )
                            onInsulinSettingsChanged(insulinSettings + newSetting)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InsulinSettingCard(
    setting: EditableInsulinSettingState,
    onSettingChanged: (EditableInsulinSettingState) -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setting.insulinType.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val activeSubstance = setting.insulinType.activeSubstance
                    if (!activeSubstance.isNullOrBlank()) {
                        Text(
                            text = activeSubstance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.cd_delete_insulin_setting)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = setting.dia,
                    onValueChange = { newDia ->
                        onSettingChanged(setting.copy(dia = newDia))
                    },
                    label = { Text(stringResource(id = R.string.insulin_profile_editor_dia_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = setting.peak,
                    onValueChange = { newPeak ->
                        onSettingChanged(setting.copy(peak = newPeak))
                    },
                    label = { Text(stringResource(id = R.string.insulin_profile_editor_peak_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }

            Text(
                text = stringResource(
                    id = R.string.insulin_profile_editor_default_values_hint,
                    setting.insulinType.dia.value.toInt(),
                    setting.insulinType.peak.value.toInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TherapyBlockListEditor(
    title: String,
    description: String,
    blocks: List<Block>,
    onBlocksChanged: (List<Block>) -> Unit,
    step: Double,
    format: String,
    minValue: Double,
    maxValue: Double
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val startHours = remember(blocks) {
        var currentHour = 0
        blocks.map { block ->
            val hour = currentHour
            currentHour += block.duration.value / 60
            hour
        }
    }

    fun updateHour(index: Int, newHour: Int) {
        val newHours = startHours.toMutableList()
        newHours[index] = newHour

        val newBlocks = mutableListOf<Block>()
        for (i in 0 until newHours.size) {
            val durationHours = if (i < newHours.size - 1) newHours[i+1] - newHours[i] else 24 - newHours[i]
            newBlocks.add(blocks[i].copy(duration = Minutes.ofHours(durationHours)))
        }
        onBlocksChanged(newBlocks)
    }

    fun addBlock(atIndex: Int) {
        if (atIndex == 0) return
        val splitIndex = atIndex - 1
        val splitBlock = blocks[splitIndex]
        val splitDuration = splitBlock.duration.value / 60

        val isAddingAtEnd = (atIndex == blocks.size)

        val newBlocks = blocks.toMutableList()
        newBlocks[splitIndex] = splitBlock.copy(duration = Minutes.ofHours(1))
        newBlocks.add(atIndex, splitBlock.copy(duration = Minutes.ofHours(splitDuration - 1)))
        onBlocksChanged(newBlocks)

        if (isAddingAtEnd) {
            coroutineScope.launch {
                delay(100)
                val targetIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    fun removeBlock(index: Int) {
        if (blocks.size <= 1) return
        val newBlocks = blocks.toMutableList()
        val removed = newBlocks.removeAt(index)
        val targetIndex = if (index > 0) index - 1 else 0
        val target = newBlocks[targetIndex]
        newBlocks[targetIndex] = target.copy(duration = Minutes((target.duration.value + removed.duration.value).toShort()))
        onBlocksChanged(newBlocks)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().contentScrollIndicator(listState),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(id = R.string.cd_help),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        blocks.forEachIndexed { index, block ->
            val currentHour = startHours[index]
            val prevHour = if (index > 0) startHours[index - 1] else -1
            val nextHour = if (index < blocks.size - 1) startHours[index + 1] else 24

            item(key = "block_$index") {
                BlockRow(
                    hour = currentHour,
                    minHour = prevHour + 1,
                    maxHour = nextHour - 1,
                    value = block.amount,
                    onHourChanged = { updateHour(index, it) },
                    onValueChanged = { newVal ->
                        val updated = blocks.toMutableList()
                        updated[index] = block.copy(amount = newVal)
                        onBlocksChanged(updated)
                    },
                    onDelete = if (index > 0) { { removeBlock(index) } } else null,
                    step = step,
                    format = format,
                    isFixed = index == 0,
                    minValue = minValue,
                    maxValue = maxValue
                )
            }

            item(key = "insert_$index") {
                InsertButton(canInsert = nextHour - currentHour > 1) { addBlock(index + 1) }
            }
        }
    }

    if (showHelpDialog) {
        InsulinProfileHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun InsulinProfileHelpDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.insulin_profile_editor_help_title)) },
        content = {
            Text(
                stringResource(id = R.string.help_24h_profile_general) +
                        "\n\n" +
                        stringResource(id = R.string.insulin_profile_editor_help_specific)
            )
        },
        confirmButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    )
}

@Composable
fun BlockRow(
    hour: Int,
    minHour: Int,
    maxHour: Int,
    value: Double,
    onHourChanged: (Int) -> Unit,
    onValueChanged: (Double) -> Unit,
    onDelete: (() -> Unit)?,
    step: Double,
    format: String,
    isFixed: Boolean,
    minValue: Double,
    maxValue: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeHourSelector(
                hour = hour,
                enabled = !isFixed,
                minHour = minHour,
                maxHour = maxHour,
                onHourChanged = onHourChanged,
                modifier = Modifier.width(100.dp)
            )

            val locale = LocalLocale.current
            EditableValueStepper(
                currentValue = value,
                onValueChange = onValueChanged,
                modifier = Modifier.weight(1f),
                minValue = minValue,
                maxValue = maxValue,
                steppingStrategy = DefaultSteppingStrategy(step = step),
                displayStrategy = object : ValueDisplayStrategy {
                    override fun format(value: Double): String = String.format(locale.platformLocale, format, value)
                    override fun color(value: Double): Color = Color.Unspecified
                },
                style = StepperDefaults.compactStyle().copy(suffixBelowValue = false, valueWidth = 64.dp)
            )

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = CommonR.string.action_delete))
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun InsertButton(canInsert: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        val backgroundColor = if (canInsert) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
        val iconTint = if (canInsert) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .then(if (canInsert) Modifier.clickable { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (canInsert) stringResource(id = R.string.cd_insert_block) else null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsulinProfileDetailEditorPreview() {
    val sampleInsulinType = InsulinType(name = "Humalog", activeSubstance = "Insulin lispro", dia = Minutes.ofHours(5), peak = Minutes.ofHours(1))
    val sampleProfile = InsulinProfile(
        id = 1,
        name = "Normal",
        basalBlocks = listOf(
            Block(Minutes.ofHours(8), 0.8),
            Block(Minutes.ofHours(16), 1.0)
        ),
        isfBlocks = listOf(Block(Minutes.ofHours(24), 50.0)),
        crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
        insulinSettings = listOf(
            ProfileInsulinSetting(
                insulinType = sampleInsulinType,
                dia = sampleInsulinType.dia,
                peak = sampleInsulinType.peak
            )
        )
    )

    AppPreview {
        InsulinProfileDetailEditor(
            profile = sampleProfile,
            insulinTypes = listOf(
                sampleInsulinType,
                InsulinType(name = "NovoRapid", activeSubstance = "Insulin aspart", dia = Minutes.ofHours(5), peak = Minutes.ofHours(1))
            ),
            onSave = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InsulinProfileEditorContentPreview() {
    val sampleInsulinType = InsulinType(name = "Humalog", activeSubstance = "Insulin lispro", dia = Minutes.ofHours(5), peak = Minutes.ofHours(1))
    AppPreview {
        InsulinProfileEditorContent(
            name = "Normal",
            onNameChange = {},
            insulinTypes = listOf(sampleInsulinType),
            insulinSettings = listOf(
                EditableInsulinSettingState(
                    insulinType = sampleInsulinType,
                    dia = "300",
                    peak = "60"
                )
            ),
            onInsulinSettingsChanged = {},
            basalBlocks = listOf(Block(Minutes.ofHours(24), 1.0)),
            onBasalBlocksChanged = {},
            isfBlocks = listOf(Block(Minutes.ofHours(24), 50.0)),
            onIsfBlocksChanged = {},
            crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
            onCrBlocksChanged = {}
        )
    }
}