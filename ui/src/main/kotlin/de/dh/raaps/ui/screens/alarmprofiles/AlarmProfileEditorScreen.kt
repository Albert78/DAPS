package de.dh.raaps.ui.screens.alarmprofiles

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.VibrationMode
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.NormalTextButton
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.common.R as CommonR

private data class InitialAlarmProfileValues(
    val name: String,
    val severityDefaults: Map<AlarmSeverity, AlarmSoundConfig>
)

@Composable
fun AlarmProfileEditorScreen(
    viewModel: AlarmProfileEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlarmProfileEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onSeverityConfigChange = viewModel::onSeverityConfigChange,
        onPlayPreview = viewModel::playPreviewSound,
        onSave = {
            viewModel.save(onSuccess = onNavigateUp)
        },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmProfileEditorContent(
    uiState: AlarmProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onSeverityConfigChange: (AlarmSeverity, AlarmSoundConfig) -> Unit,
    onPlayPreview: (AlarmSoundConfig) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val initialValues = remember(uiState.isLoading) {
        if (!uiState.isLoading) {
            InitialAlarmProfileValues(
                name = uiState.name,
                severityDefaults = uiState.severityDefaults
            )
        } else null
    }

    val hasChanges = remember(
        uiState.name,
        uiState.severityDefaults,
        initialValues
    ) {
        if (initialValues == null) false
        else {
            uiState.name != initialValues.name ||
                    uiState.severityDefaults != initialValues.severityDefaults
        }
    }

    fun handleBack() {
        if (hasChanges) {
            showDiscardConfirmation = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = ::handleBack)

    val titleRes = if (uiState.profileId == null) {
        R.string.alarm_profile_editor_title_new
    } else {
        R.string.alarm_profile_editor_title_edit
    }

    val severityTitles = listOf(
        AlarmSeverity.CRITICAL to ("🔴 " + stringResource(id = R.string.alarm_profile_severity_critical)),
        AlarmSeverity.WARNING to ("🟡 " + stringResource(id = R.string.alarm_profile_severity_warning)),
        AlarmSeverity.INFO to ("🔵 " + stringResource(id = R.string.alarm_profile_severity_info))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = titleRes)),
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
                        onClick = onSave,
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(id = CommonR.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .contentScrollIndicator(listState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(id = R.string.alarm_profile_name_label)) },
                    isError = uiState.name.isNotBlank() && !uiState.isValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Severity Sections
            items(severityTitles) { (severity, title) ->
                val currentConfig = uiState.severityDefaults[severity] ?: AlarmSoundConfig()
                SeverityEditorSectionCard(
                    title = title,
                    config = currentConfig,
                    onConfigChanged = { updated ->
                        onSeverityConfigChange(severity, updated)
                    },
                    onPlayPreview = {
                        onPlayPreview(currentConfig)
                    }
                )
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(id = R.string.alarm_profile_editor_discard_title)) },
            text = { Text(stringResource(id = R.string.alarm_profile_editor_discard_message)) },
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
}

@Composable
fun SeverityEditorSectionCard(
    title: String,
    config: AlarmSoundConfig,
    onConfigChanged: (AlarmSoundConfig) -> Unit,
    onPlayPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onPlayPreview) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(id = R.string.cd_test_alarm_sound)
                    )
                }
            }

            // Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_profile_volume_label, config.volume),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Slider(
                value = config.volume.toFloat(),
                onValueChange = { onConfigChanged(config.copy(volume = it.toInt())) },
                valueRange = 0f..100f,
                steps = 19
            )

            // Vibration Mode
            Text(
                text = stringResource(id = R.string.alarm_profile_vibration_label),
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VibrationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.vibrationMode == mode,
                        onClick = { onConfigChanged(config.copy(vibrationMode = mode)) },
                        label = {
                            Text(
                                text = vibrationModeLabel(mode),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            // DND Override Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_profile_override_dnd),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.overrideDnd,
                    onCheckedChange = { onConfigChanged(config.copy(overrideDnd = it)) }
                )
            }

            // Full Screen Alarm Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_profile_show_full_screen),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.showFullScreen,
                    onCheckedChange = { onConfigChanged(config.copy(showFullScreen = it)) }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun AlarmProfileEditorPreview() {
    AppTheme {
        AlarmProfileEditorContent(
            uiState = AlarmProfileEditorUiState(
                profileId = 1L,
                name = "Standard"
            ),
            onNameChange = {},
            onSeverityConfigChange = { _, _ -> },
            onPlayPreview = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}