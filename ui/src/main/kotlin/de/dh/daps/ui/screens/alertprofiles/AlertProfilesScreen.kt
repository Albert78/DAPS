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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlertDisplayMode
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.RecommendationType
import de.dh.daps.common.domain.model.alarm.SoundConfig
import de.dh.daps.common.domain.model.alarm.VibrationMode
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Sound_Off
import de.dh.daps.ui.common.icons.Icon_Sound_Only
import de.dh.daps.ui.common.icons.Icon_Sound_Vibration
import de.dh.daps.ui.common.icons.Icon_Vibration_Only
import de.dh.daps.ui.common.theme.AppPreview
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import de.dh.daps.common.R as CommonR

@Composable
fun AlertProfilesScreen(
    viewModel: AlertProfilesViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlertProfilesContent(
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
fun AlertProfilesContent(
    uiState: AlertProfilesUiState,
    onAddProfile: () -> Unit,
    onEditProfile: (AlertProfile) -> Unit,
    onDeleteProfile: (AlertProfile) -> Unit,
    onConfirmDelete: (AlertProfile) -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.alert_profiles_screen_title)),
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
                    contentDescription = stringResource(id = R.string.cd_add_alert_profile)
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
                    text = stringResource(id = R.string.alert_profile_short_label),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val listState = rememberLazyListState()

            var previousSize by remember { mutableIntStateOf(uiState.profiles.size) }

            LaunchedEffect(uiState.profiles.size) {
                if (uiState.profiles.size > previousSize) {
                    delay(100.milliseconds)
                    val targetIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(targetIndex)
                }
                previousSize = uiState.profiles.size
            }

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
                    val isActive = uiState.activeProfile?.id == profile.id
                    AlertProfileCard(
                        profile = profile,
                        isDefault = isDefault,
                        isActive = isActive,
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }
    }

    val deletingProfile = uiState.showDeleteConfirmation
    if (deletingProfile != null) {
        AppAlertDialog(
            titleText = stringResource(id = R.string.alert_profile_delete_title),
            onDismissRequest = onCancelDelete,
            confirmButtonText = stringResource(id = CommonR.string.action_delete),
            onConfirm = { onConfirmDelete(deletingProfile) },
            dismissButtonText = stringResource(id = android.R.string.cancel),
        ) {
            Text(stringResource(id = R.string.alert_profile_delete_message, deletingProfile.name))
        }
    }
}

@Composable
fun AlertProfileCard(
    profile: AlertProfile,
    isDefault: Boolean,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    FramedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        containerColor = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else if (isDefault) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
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
                                    text = stringResource(id = R.string.alert_profile_active_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    if (isDefault) {
                        Surface(
                            color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_default_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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

            // 1. Alarms Section
            Text(
                text = stringResource(id = R.string.alert_profile_section_alarms),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            val criticalConfig = profile.alarmDefaults[AlarmSeverity.CRITICAL] ?: AlertSignalConfig()
            val warningConfig = profile.alarmDefaults[AlarmSeverity.WARNING] ?: AlertSignalConfig()
            val infoConfig = profile.alarmDefaults[AlarmSeverity.INFO] ?: AlertSignalConfig()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SeveritySummaryItem(
                    dot = "🔴",
                    name = stringResource(id = R.string.alert_profile_severity_critical),
                    config = criticalConfig
                )
                SeveritySummaryItem(
                    dot = "🟡",
                    name = stringResource(id = R.string.alert_profile_severity_warning),
                    config = warningConfig
                )
                SeveritySummaryItem(
                    dot = "🔵",
                    name = stringResource(id = R.string.alert_profile_severity_info),
                    config = infoConfig
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // 2. APS Recommendations & Reminders Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SignalSummaryItem(
                    label = stringResource(id = R.string.alert_profile_section_recommendations),
                    config = profile.recommendationDefault,
                    overridesCount = profile.recommendationOverrides.size
                )
                SignalSummaryItem(
                    label = stringResource(id = R.string.alert_profile_section_reminders),
                    config = profile.reminderDefault,
                    overridesCount = profile.reminderOverrides.size
                )
            }
        }
    }
}

@Composable
fun SeveritySummaryItem(
    dot: String,
    name: String,
    config: AlertSignalConfig
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
fun SignalSummaryItem(
    label: String,
    config: AlertSignalConfig,
    overridesCount: Int = 0
) {
    val sound = config.soundConfig
    val hasSound = config.enabled && sound != null && sound.volume > 0
    val hasVibration = config.enabled && config.vibrationMode != VibrationMode.OFF

    val icon = when {
        !config.enabled -> Icon_Sound_Off
        hasSound && hasVibration -> Icon_Sound_Vibration
        hasSound -> Icon_Sound_Only
        hasVibration -> Icon_Vibration_Only
        else -> Icon_Sound_Off
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (overridesCount > 0) {
                Text(
                    text = "+$overridesCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun AlertProfilesPreview() {
    val sampleStandard = AlertProfile(
        id = 1L,
        name = "Standard",
        isDefault = true,
        alarmDefaults = mapOf(
            AlarmSeverity.CRITICAL to AlertSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 80)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            AlarmSeverity.WARNING to AlertSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 50)),
                vibrationMode = VibrationMode.LONG,
                overrideDnd = false
            ),
            AlarmSeverity.INFO to AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT,
                overrideDnd = false
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
    )

    val sampleOutdoor = AlertProfile(
        id = 2L,
        name = "Draußen / Laut",
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
    )

    val sampleSilent = AlertProfile(
        id = 3L,
        name = "Stumm",
        isDefault = true,
        alarmDefaults = mapOf(
            AlarmSeverity.CRITICAL to AlertSignalConfig(
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 10)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            AlarmSeverity.WARNING to AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.LONG,
                overrideDnd = false
            ),
            AlarmSeverity.INFO to AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT,
                overrideDnd = false
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
                displayMode = AlertDisplayMode.NotificationOnly,
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
    )

    AppPreview {
        AlertProfilesContent(
            uiState = AlertProfilesUiState(
                profiles = listOf(sampleStandard, sampleOutdoor, sampleSilent),
                defaultProfile = sampleStandard,
                isLoading = false
            ),
            onAddProfile = {},
            onEditProfile = {},
            onDeleteProfile = {},
            onCancelDelete = {},
            onConfirmDelete = {},
            onNavigateUp = {}
        )
    }
}