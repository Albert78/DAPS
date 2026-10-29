package de.dh.daps.ui.screens.alertprofiles

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.alarm.AlertDisplayMode
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.RecommendationType
import de.dh.daps.common.domain.model.alarm.SoundConfig
import de.dh.daps.common.domain.model.alarm.VibrationMode
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

sealed interface SoundPickerTarget {
    data class AlarmDefault(val severity: AlarmSeverity) : SoundPickerTarget
    data class AlarmOverride(val alarmType: AlarmType) : SoundPickerTarget
    data object RecommendationDefault : SoundPickerTarget
    data class RecommendationOverride(val recommendationType: RecommendationType) : SoundPickerTarget
    data object ReminderDefault : SoundPickerTarget
    data class ReminderOverride(val reminderType: ReminderType) : SoundPickerTarget
}

@Composable
fun AlertProfileEditorScreen(
    viewModel: AlertProfileEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlertProfileEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onIsDefaultChange = viewModel::onIsDefaultChange,
        onAlarmDefaultChange = viewModel::onAlarmDefaultChange,
        onAlarmOverrideChange = viewModel::onAlarmOverrideChange,
        onAlarmOverrideRemove = viewModel::onAlarmOverrideRemove,
        onRecommendationDefaultChange = viewModel::onRecommendationDefaultChange,
        onRecommendationOverrideChange = viewModel::onRecommendationOverrideChange,
        onRecommendationOverrideRemove = viewModel::onRecommendationOverrideRemove,
        onReminderDefaultChange = viewModel::onReminderDefaultChange,
        onReminderOverrideChange = viewModel::onReminderOverrideChange,
        onReminderOverrideRemove = viewModel::onReminderOverrideRemove,
        onAlarmOverrideAdd = viewModel::onAlarmOverrideAdd,
        onRecommendationOverrideAdd = viewModel::onRecommendationOverrideAdd,
        onReminderOverrideAdd = viewModel::onReminderOverrideAdd,
        onPlayPreview = viewModel::playPreviewSound,
        onUpdateVolume = viewModel::updatePreviewVolume,
        onStopPreview = viewModel::stopPreviewSound,
        onSave = { viewModel.save(onSuccess = onNavigateUp) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertProfileEditorContent(
    uiState: AlertProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onIsDefaultChange: (Boolean) -> Unit,
    onAlarmDefaultChange: (AlarmSeverity, AlertSignalConfig) -> Unit,
    onAlarmOverrideChange: (AlarmType, AlertSignalConfig) -> Unit,
    onAlarmOverrideRemove: (AlarmType) -> Unit,
    onRecommendationDefaultChange: (AlertSignalConfig) -> Unit,
    onRecommendationOverrideChange: (RecommendationType, AlertSignalConfig) -> Unit,
    onRecommendationOverrideRemove: (RecommendationType) -> Unit,
    onReminderDefaultChange: (AlertSignalConfig) -> Unit,
    onReminderOverrideChange: (ReminderType, AlertSignalConfig) -> Unit,
    onReminderOverrideRemove: (ReminderType) -> Unit,
    onAlarmOverrideAdd: (AlarmType, AlertSignalConfig) -> Unit,
    onRecommendationOverrideAdd: (RecommendationType, AlertSignalConfig) -> Unit,
    onReminderOverrideAdd: (ReminderType, AlertSignalConfig) -> Unit,
    onPlayPreview: (AlertSignalConfig) -> Unit,
    onUpdateVolume: (Int) -> Unit,
    onStopPreview: () -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var initialValues by remember { mutableStateOf<AlertProfileEditorUiState?>(null) }
    if (initialValues == null && !uiState.isLoading) {
        initialValues = uiState
    }

    val hasChanges = remember(uiState, initialValues) {
        initialValues != null && uiState != initialValues
    }

    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var activeSoundPickerTarget by remember { mutableStateOf<SoundPickerTarget?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val isEditing = uiState.profileId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(
                    if (isEditing) {
                        stringResource(id = R.string.alert_profile_editor_title_edit)
                    } else {
                        stringResource(id = R.string.alert_profile_editor_title_new)
                    }
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showDiscardConfirmation = true
                            } else {
                                onNavigateUp()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(id = CommonR.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile Name and Default Toggle Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(id = R.string.alert_profile_name_label)) },
                    isError = uiState.name.isNotBlank() && !uiState.isValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )

                FramedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.alert_profile_set_default),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.isDefault) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_is_default_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isDefault,
                            onCheckedChange = onIsDefaultChange,
                            enabled = initialValues?.isDefault != true
                        )
                    }
                }
            }

            // Category Tab Row (Alarme, APS-Empfehlungen, Erinnerungen)
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(id = R.string.alert_profile_section_alarms)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(id = R.string.alert_profile_section_recommendations)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(stringResource(id = R.string.alert_profile_section_reminders)) }
                )
            }

            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(listState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // 1. Alarms Tab: Severity Defaults & Overrides
                        item {
                            Text(
                                text = stringResource(id = R.string.alert_profile_default_signal_config),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            SignalConfigCard(
                                title = stringResource(id = R.string.alert_profile_severity_critical),
                                subtitle = "🔴 Default",
                                config = uiState.alarmDefaults[AlarmSeverity.CRITICAL] ?: AlertSignalConfig(),
                                onConfigChanged = { onAlarmDefaultChange(AlarmSeverity.CRITICAL, it) },
                                showEnabledToggle = false,
                                onPlayPreview = { onPlayPreview(uiState.alarmDefaults[AlarmSeverity.CRITICAL] ?: AlertSignalConfig()) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.AlarmDefault(AlarmSeverity.CRITICAL) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }

                        item {
                            SignalConfigCard(
                                title = stringResource(id = R.string.alert_profile_severity_warning),
                                subtitle = "🟡 Default",
                                config = uiState.alarmDefaults[AlarmSeverity.WARNING] ?: AlertSignalConfig(),
                                onConfigChanged = { onAlarmDefaultChange(AlarmSeverity.WARNING, it) },
                                showEnabledToggle = false,
                                onPlayPreview = { onPlayPreview(uiState.alarmDefaults[AlarmSeverity.WARNING] ?: AlertSignalConfig()) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.AlarmDefault(AlarmSeverity.WARNING) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }

                        item {
                            SignalConfigCard(
                                title = stringResource(id = R.string.alert_profile_severity_info),
                                subtitle = "🔵 Default",
                                config = uiState.alarmDefaults[AlarmSeverity.INFO] ?: AlertSignalConfig(),
                                onConfigChanged = { onAlarmDefaultChange(AlarmSeverity.INFO, it) },
                                showEnabledToggle = false,
                                onPlayPreview = { onPlayPreview(uiState.alarmDefaults[AlarmSeverity.INFO] ?: AlertSignalConfig()) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.AlarmDefault(AlarmSeverity.INFO) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_specific_overrides),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                AddAlarmOverrideButton(
                                    existingOverrides = uiState.alarmOverrides.keys,
                                    onAdd = { onAlarmOverrideAdd(it, AlertSignalConfig()) }
                                )
                            }
                        }

                        items(uiState.alarmOverrides.toList(), key = { it.first.name }) { (alarmType, config) ->
                            SignalConfigCard(
                                title = getAlarmTypeLabel(alarmType),
                                config = config,
                                onConfigChanged = { onAlarmOverrideChange(alarmType, it) },
                                showEnabledToggle = true,
                                onDelete = { onAlarmOverrideRemove(alarmType) },
                                onPlayPreview = { onPlayPreview(config) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.AlarmOverride(alarmType) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }
                    }

                    1 -> {
                        // 2. Recommendations Tab: Default & Overrides
                        item {
                            Text(
                                text = stringResource(id = R.string.alert_profile_default_signal_config),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            SignalConfigCard(
                                title = stringResource(id = R.string.alert_profile_section_recommendations),
                                subtitle = stringResource(id = R.string.alert_profile_default_signal_config),
                                config = uiState.recommendationDefault,
                                onConfigChanged = onRecommendationDefaultChange,
                                showEnabledToggle = true,
                                onPlayPreview = { onPlayPreview(uiState.recommendationDefault) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.RecommendationDefault },
                                onUpdateVolume = onUpdateVolume
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_specific_overrides),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                AddRecommendationOverrideButton(
                                    existingOverrides = uiState.recommendationOverrides.keys,
                                    onAdd = { onRecommendationOverrideAdd(it, AlertSignalConfig()) }
                                )
                            }
                        }

                        items(uiState.recommendationOverrides.toList(), key = { it.first.name }) { (recType, config) ->
                            SignalConfigCard(
                                title = getRecommendationTypeLabel(recType),
                                config = config,
                                onConfigChanged = { onRecommendationOverrideChange(recType, it) },
                                showEnabledToggle = true,
                                onDelete = { onRecommendationOverrideRemove(recType) },
                                onPlayPreview = { onPlayPreview(config) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.RecommendationOverride(recType) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }
                    }

                    2 -> {
                        // 3. Reminders Tab: Default & Overrides
                        item {
                            Text(
                                text = stringResource(id = R.string.alert_profile_default_signal_config),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            SignalConfigCard(
                                title = stringResource(id = R.string.alert_profile_section_reminders),
                                subtitle = stringResource(id = R.string.alert_profile_default_signal_config),
                                config = uiState.reminderDefault,
                                onConfigChanged = onReminderDefaultChange,
                                showEnabledToggle = true,
                                onPlayPreview = { onPlayPreview(uiState.reminderDefault) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.ReminderDefault },
                                onUpdateVolume = onUpdateVolume
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_specific_overrides),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                AddReminderOverrideButton(
                                    existingOverrides = uiState.reminderOverrides.keys,
                                    onAdd = { onReminderOverrideAdd(it, AlertSignalConfig()) }
                                )
                            }
                        }

                        items(uiState.reminderOverrides.toList(), key = { it.first.name }) { (reminderType, config) ->
                            SignalConfigCard(
                                title = getReminderTypeLabel(reminderType),
                                config = config,
                                onConfigChanged = { onReminderOverrideChange(reminderType, it) },
                                showEnabledToggle = true,
                                onDelete = { onReminderOverrideRemove(reminderType) },
                                onPlayPreview = { onPlayPreview(config) },
                                onSelectSound = { activeSoundPickerTarget = SoundPickerTarget.ReminderOverride(reminderType) },
                                onUpdateVolume = onUpdateVolume
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(id = R.string.alert_profile_editor_discard_title)) },
            text = { Text(stringResource(id = R.string.alert_profile_editor_discard_message)) },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onSave()
                    },
                    enabled = uiState.isValid && !uiState.isSaving
                ) {
                    Text(stringResource(id = CommonR.string.action_save))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = {
                    showDiscardConfirmation = false
                    onNavigateUp()
                }) {
                    Text(stringResource(id = R.string.discard_confirm_button))
                }
            }
        )
    }

    val target = activeSoundPickerTarget
    if (target != null) {
        val currentConfig: AlertSignalConfig = when (target) {
            is SoundPickerTarget.AlarmDefault -> uiState.alarmDefaults[target.severity] ?: AlertSignalConfig()
            is SoundPickerTarget.AlarmOverride -> uiState.alarmOverrides[target.alarmType] ?: AlertSignalConfig()
            SoundPickerTarget.RecommendationDefault -> uiState.recommendationDefault
            is SoundPickerTarget.RecommendationOverride -> uiState.recommendationOverrides[target.recommendationType] ?: AlertSignalConfig()
            SoundPickerTarget.ReminderDefault -> uiState.reminderDefault
            is SoundPickerTarget.ReminderOverride -> uiState.reminderOverrides[target.reminderType] ?: AlertSignalConfig()
        }
        val currentSound = currentConfig.soundConfig ?: SoundConfig()

        RingtonePickerDialog(
            currentUri = currentSound.soundUri,
            currentVolume = currentSound.volume,
            onSoundSelected = { selectedUri, selectedVolume ->
                val updatedSound = currentSound.copy(soundUri = selectedUri, volume = selectedVolume)
                val updatedConfig = currentConfig.copy(displayMode = AlertDisplayMode.FullScreen(updatedSound))
                when (target) {
                    is SoundPickerTarget.AlarmDefault -> onAlarmDefaultChange(target.severity, updatedConfig)
                    is SoundPickerTarget.AlarmOverride -> onAlarmOverrideChange(target.alarmType, updatedConfig)
                    SoundPickerTarget.RecommendationDefault -> onRecommendationDefaultChange(updatedConfig)
                    is SoundPickerTarget.RecommendationOverride -> onRecommendationOverrideChange(target.recommendationType, updatedConfig)
                    SoundPickerTarget.ReminderDefault -> onReminderDefaultChange(updatedConfig)
                    is SoundPickerTarget.ReminderOverride -> onReminderOverrideChange(target.reminderType, updatedConfig)
                }
            },
            onPlayPreview = { soundUri, volume ->
                val previewSound = currentSound.copy(soundUri = soundUri, volume = volume)
                onPlayPreview(currentConfig.copy(displayMode = AlertDisplayMode.FullScreen(previewSound)))
            },
            onUpdateVolume = onUpdateVolume,
            onStopPreview = onStopPreview,
            onDismiss = {
                activeSoundPickerTarget = null
            }
        )
    }
}

@Composable
private fun AddAlarmOverrideButton(
    existingOverrides: Set<AlarmType>,
    onAdd: (AlarmType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableTypes = remember(existingOverrides) {
        AlarmType.entries.filterNot { existingOverrides.contains(it) }
    }

    if (availableTypes.isNotEmpty()) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(id = R.string.alert_profile_add_override))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(getAlarmTypeLabel(type)) },
                        onClick = {
                            expanded = false
                            onAdd(type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRecommendationOverrideButton(
    existingOverrides: Set<RecommendationType>,
    onAdd: (RecommendationType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableTypes = remember(existingOverrides) {
        RecommendationType.entries.filterNot { existingOverrides.contains(it) }
    }

    if (availableTypes.isNotEmpty()) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(id = R.string.alert_profile_add_override))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(getRecommendationTypeLabel(type)) },
                        onClick = {
                            expanded = false
                            onAdd(type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddReminderOverrideButton(
    existingOverrides: Set<ReminderType>,
    onAdd: (ReminderType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableTypes = remember(existingOverrides) {
        ReminderType.entries.filterNot { existingOverrides.contains(it) }
    }

    if (availableTypes.isNotEmpty()) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(id = R.string.alert_profile_add_override))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(getReminderTypeLabel(type)) },
                        onClick = {
                            expanded = false
                            onAdd(type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun getAlarmTypeLabel(type: AlarmType): String = when (type) {
    AlarmType.CRITICAL_LOW_BG -> stringResource(id = R.string.alarm_type_critical_low_bg)
    AlarmType.LOW_BG -> stringResource(id = R.string.alarm_type_low_bg)
    AlarmType.HIGH_BG -> stringResource(id = R.string.alarm_type_high_bg)
    AlarmType.PUMP_OCCLUSION -> stringResource(id = R.string.alarm_type_pump_occlusion)
    AlarmType.PUMP_SUSPENDED -> stringResource(id = R.string.alarm_type_pump_suspended)
    AlarmType.PUMP_LOW_INSULIN -> stringResource(id = R.string.alarm_type_pump_low_insulin)
    AlarmType.PUMP_LOW_BATTERY -> stringResource(id = R.string.alarm_type_pump_low_battery)
    AlarmType.PUMP_STALE_BOND -> stringResource(id = R.string.alarm_type_pump_stale_bond)
    AlarmType.PUMP_CATHETER_CHANGE_WARN -> stringResource(id = R.string.alarm_type_pump_catheter_change_warn)
    AlarmType.PUMP_PATCH_CHANGE_WARN -> stringResource(id = R.string.alarm_type_pump_patch_change_warn)
    AlarmType.CGM_SIGNAL_LOSS -> stringResource(id = R.string.alarm_type_cgm_signal_loss)
    AlarmType.CGM_EXPIRED -> stringResource(id = R.string.alarm_type_cgm_expired)
    AlarmType.CGM_SENSOR_ERROR -> stringResource(id = R.string.alarm_type_cgm_sensor_error)
    AlarmType.CGM_STALE_BOND -> stringResource(id = R.string.alarm_type_cgm_stale_bond)
    AlarmType.SYSTEM_BATTERY_LOW -> stringResource(id = R.string.alarm_type_system_battery_low)
    AlarmType.SYSTEM_BLUETOOTH_OFF -> stringResource(id = R.string.alarm_type_system_bluetooth_off)
    AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> stringResource(id = R.string.alarm_type_system_therapy_settings_missing)
}

@Composable
fun getRecommendationTypeLabel(type: RecommendationType): String = when (type) {
    RecommendationType.CARBS -> stringResource(id = R.string.alert_profile_recommendation_carbs)
    RecommendationType.BOLUS -> stringResource(id = R.string.alert_profile_recommendation_bolus)
    RecommendationType.TEMP_BASAL -> stringResource(id = R.string.alert_profile_recommendation_temp_basal)
}

@Composable
fun getReminderTypeLabel(type: ReminderType): String = when (type) {
    ReminderType.MEAL -> stringResource(id = R.string.alert_profile_reminder_meal)
    ReminderType.PUMP -> stringResource(id = R.string.alert_profile_reminder_pump)
    ReminderType.CGM -> stringResource(id = R.string.alert_profile_reminder_cgm)
    ReminderType.PLUGIN -> stringResource(id = R.string.alert_profile_reminder_plugin)
    ReminderType.CUSTOM -> stringResource(id = R.string.alert_profile_reminder_custom)
}

@Preview(showBackground = true, heightDp = 1800, name = "Light Mode")
@Preview(showBackground = true, heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun AlertProfileEditorPreview() {
    AppPreview {
        AlertProfileEditorContent(
            uiState = AlertProfileEditorUiState(
                profileId = 1L,
                name = "Standard",
                isDefault = true,
                alarmDefaults = mapOf(
                    AlarmSeverity.CRITICAL to AlertSignalConfig(
                        displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                        vibrationMode = VibrationMode.CONTINUOUS,
                        overrideDnd = true
                    ),
                    AlarmSeverity.WARNING to AlertSignalConfig(
                        displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 80)),
                        vibrationMode = VibrationMode.LONG,
                        overrideDnd = false
                    ),
                    AlarmSeverity.INFO to AlertSignalConfig(
                        displayMode = AlertDisplayMode.NotificationOnly,
                        vibrationMode = VibrationMode.SHORT,
                        overrideDnd = false
                    )
                ),
                alarmOverrides = mapOf(
                    AlarmType.CRITICAL_LOW_BG to AlertSignalConfig(
                        displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                        vibrationMode = VibrationMode.CONTINUOUS,
                        overrideDnd = true
                    )
                ),
                recommendationDefault = AlertSignalConfig(
                    enabled = true,
                    displayMode = AlertDisplayMode.NotificationOnly,
                    vibrationMode = VibrationMode.SHORT,
                    overrideDnd = false
                ),
                recommendationOverrides = mapOf(
                    RecommendationType.BOLUS to AlertSignalConfig(
                        enabled = true,
                        displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 70)),
                        vibrationMode = VibrationMode.LONG,
                        overrideDnd = false
                    )
                ),
                reminderDefault = AlertSignalConfig(
                    enabled = true,
                    displayMode = AlertDisplayMode.NotificationOnly,
                    vibrationMode = VibrationMode.SHORT,
                    overrideDnd = false
                ),
                reminderOverrides = mapOf(
                    ReminderType.MEAL to AlertSignalConfig(
                        enabled = true,
                        displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 60)),
                        vibrationMode = VibrationMode.SHORT,
                        overrideDnd = false
                    )
                )
            ),
            onNameChange = {},
            onIsDefaultChange = {},
            onAlarmDefaultChange = { _, _ -> },
            onAlarmOverrideChange = { _, _ -> },
            onAlarmOverrideRemove = {},
            onRecommendationDefaultChange = {},
            onRecommendationOverrideChange = { _, _ -> },
            onRecommendationOverrideRemove = {},
            onReminderDefaultChange = {},
            onReminderOverrideChange = { _, _ -> },
            onReminderOverrideRemove = {},
            onAlarmOverrideAdd = { _, _ -> },
            onRecommendationOverrideAdd = { _, _ -> },
            onReminderOverrideAdd = { _, _ -> },
            onPlayPreview = {},
            onUpdateVolume = {},
            onStopPreview = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}