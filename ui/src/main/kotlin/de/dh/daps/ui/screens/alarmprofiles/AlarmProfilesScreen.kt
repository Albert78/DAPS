package de.dh.daps.ui.screens.alarmprofiles

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import de.dh.daps.ui.common.composables.contentScrollIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSignalConfig
import de.dh.daps.common.model.data.AlertDisplayMode
import de.dh.daps.common.model.data.SoundConfig
import de.dh.daps.common.model.data.VibrationMode
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Sound_Off
import de.dh.daps.ui.common.icons.Icon_Sound_Only
import de.dh.daps.ui.common.icons.Icon_Sound_Vibration
import de.dh.daps.ui.common.icons.Icon_Vibration_Only
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.common.R as CommonR

@Composable
fun AlarmProfilesScreen(
    viewModel: AlarmProfilesViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlarmProfilesContent(
        uiState = uiState,
        onAddProfile = { onNavigateToEditor(null) },
        onEditProfile = { profile -> onNavigateToEditor(profile.id) },
        onDeleteProfile = { viewModel.confirmDelete(it) },
        onConfirmDelete = { viewModel.deleteProfile(it) },
        onCancelDelete = { viewModel.cancelDelete() },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmProfilesContent(
    uiState: AlarmProfilesUiState,
    onAddProfile: () -> Unit,
    onEditProfile: (AlarmProfile) -> Unit,
    onDeleteProfile: (AlarmProfile) -> Unit,
    onConfirmDelete: (AlarmProfile) -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.alarm_profiles_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_alarm_profile)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Alarmprofile vorhanden",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .contentScrollIndicator(listState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.profiles, key = { it.id }) { profile ->
                    val isDefault = profile.isDefault || uiState.defaultProfile?.id == profile.id
                    val isOverride = uiState.alarmProfileOverride?.id == profile.id
                    AlarmProfileCard(
                        profile = profile,
                        isDefault = isDefault,
                        isOverride = isOverride,
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }
    }

    val deletingProfile = uiState.showDeleteConfirmation
    if (deletingProfile != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(id = R.string.alarm_profile_delete_title)) },
            text = {
                Text(stringResource(id = R.string.alarm_profile_delete_message, deletingProfile.name))
            },
            confirmButton = {
                NormalTextButton(onClick = { onConfirmDelete(deletingProfile) }) {
                    Text(stringResource(id = CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = onCancelDelete) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AlarmProfileCard(
    profile: AlarmProfile,
    isDefault: Boolean,
    isOverride: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverride) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            } else if (isDefault) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isOverride) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.alarm_profile_override_active_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                    } else if (isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.height(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = stringResource(id = R.string.alarm_profile_default_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.action_edit)
                        )
                    }
                    if (!isDefault) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = CommonR.string.action_delete)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Severity summaries
            val criticalConfig = profile.severityDefaults[AlarmSeverity.CRITICAL] ?: AlarmSignalConfig()
            val warningConfig = profile.severityDefaults[AlarmSeverity.WARNING] ?: AlarmSignalConfig()
            val infoConfig = profile.severityDefaults[AlarmSeverity.INFO] ?: AlarmSignalConfig()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SeveritySummaryItem(
                    dot = "🔴",
                    name = "Kritisch",
                    config = criticalConfig
                )
                SeveritySummaryItem(
                    dot = "🟡",
                    name = "Warnung",
                    config = warningConfig
                )
                SeveritySummaryItem(
                    dot = "🔵",
                    name = "Info",
                    config = infoConfig
                )
            }
        }
    }
}

@Composable
fun SeveritySummaryItem(
    dot: String,
    name: String,
    config: AlarmSignalConfig
) {
    val sound = config.soundConfig
    val hasSound = sound != null && sound.volume > 0
    val hasVibration = config.vibrationMode != VibrationMode.OFF

    val soundVibrationIcon = when {
        hasSound && hasVibration -> Icon_Sound_Vibration
        hasSound -> Icon_Sound_Only
        hasVibration -> Icon_Vibration_Only
        else -> Icon_Sound_Off
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dot,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector = soundVibrationIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun vibrationModeLabel(mode: VibrationMode): String {
    return when (mode) {
        VibrationMode.OFF -> stringResource(id = R.string.alarm_profile_vibration_off)
        VibrationMode.SHORT -> stringResource(id = R.string.alarm_profile_vibration_short)
        VibrationMode.LONG -> stringResource(id = R.string.alarm_profile_vibration_long)
        VibrationMode.CONTINUOUS -> stringResource(id = R.string.alarm_profile_vibration_continuous)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun AlarmProfilesPreview() {
    val sampleStandard = AlarmProfile(
        id = 1L,
        name = "Standard",
        isDefault = true,
        severityDefaults = mapOf(
            AlarmSeverity.CRITICAL to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            AlarmSeverity.WARNING to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 80)),
                vibrationMode = VibrationMode.LONG,
                overrideDnd = false
            ),
            AlarmSeverity.INFO to AlarmSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT,
                overrideDnd = false
            )
        )
    )

    val sampleQuiet = AlarmProfile(
        id = 2L,
        name = "Kino / Diskret",
        isDefault = false,
        severityDefaults = mapOf(
            AlarmSeverity.CRITICAL to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            AlarmSeverity.WARNING to AlarmSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT,
                overrideDnd = false
            ),
            AlarmSeverity.INFO to AlarmSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.OFF,
                overrideDnd = false
            )
        )
    )

    val sampleLoud = AlarmProfile(
        id = 3L,
        name = "Laut / Draußen",
        isDefault = false,
        severityDefaults = mapOf(
            AlarmSeverity.CRITICAL to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            AlarmSeverity.WARNING to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                vibrationMode = VibrationMode.LONG,
                overrideDnd = true
            ),
            AlarmSeverity.INFO to AlarmSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 80)),
                vibrationMode = VibrationMode.SHORT,
                overrideDnd = false
            )
        )
    )

    AppTheme {
        AlarmProfilesContent(
            uiState = AlarmProfilesUiState(
                profiles = listOf(sampleStandard, sampleQuiet, sampleLoud),
                defaultProfile = sampleStandard,
                isLoading = false
            ),
            onAddProfile = {},
            onEditProfile = {},
            onDeleteProfile = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onNavigateUp = {}
        )
    }
}