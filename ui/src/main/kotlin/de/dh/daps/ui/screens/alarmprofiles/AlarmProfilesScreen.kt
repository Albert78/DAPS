package de.dh.raaps.ui.screens.alarmprofiles

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import de.dh.raaps.ui.common.composables.contentScrollIndicator
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
import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.VibrationMode
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.NormalTextButton
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.common.R as CommonR

@Composable
fun AlarmProfilesScreen(
    viewModel: AlarmProfilesViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlarmProfilesContent(
        uiState = uiState,
        onSetActiveProfile = { viewModel.setActiveProfile(it) },
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
    onSetActiveProfile: (AlarmProfile) -> Unit,
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
                    val isActive = uiState.activeProfile?.id == profile.id
                    AlarmProfileCard(
                        profile = profile,
                        isActive = isActive,
                        onSetActive = { onSetActiveProfile(profile) },
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
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
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
                    if (isActive) {
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
                                    text = stringResource(id = R.string.alarm_profile_active_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Row {
                    if (!isActive) {
                        IconButton(onClick = onSetActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(id = R.string.alarm_profile_set_active)
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.action_edit)
                        )
                    }
                    if (!profile.isDefault) {
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
            val criticalConfig = profile.severityDefaults[AlarmSeverity.CRITICAL] ?: AlarmSoundConfig()
            val warningConfig = profile.severityDefaults[AlarmSeverity.WARNING] ?: AlarmSoundConfig()
            val infoConfig = profile.severityDefaults[AlarmSeverity.INFO] ?: AlarmSoundConfig()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SeveritySummaryItem(
                    label = "🔴 Kritisch",
                    config = criticalConfig
                )
                SeveritySummaryItem(
                    label = "🟡 Warnung",
                    config = warningConfig
                )
                SeveritySummaryItem(
                    label = "🔵 Info",
                    config = infoConfig
                )
            }
        }
    }
}

@Composable
fun SeveritySummaryItem(
    label: String,
    config: AlarmSoundConfig
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${config.volume}% • ${vibrationModeLabel(config.vibrationMode)}",
            style = MaterialTheme.typography.bodySmall,
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
    AppTheme {
        AlarmProfilesContent(
            uiState = AlarmProfilesUiState(),
            onSetActiveProfile = {},
            onAddProfile = {},
            onEditProfile = {},
            onDeleteProfile = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onNavigateUp = {}
        )
    }
}