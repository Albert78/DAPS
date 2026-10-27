package de.dh.daps.ui.screens.reminders

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import de.dh.daps.common.domain.model.therapy.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderStatus
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.ScreenTitle
import de.dh.daps.ui.common.icons.Icon_Cgm_Sensor
import de.dh.daps.ui.common.icons.Icon_Delete
import de.dh.daps.ui.common.icons.Icon_Meal
import de.dh.daps.ui.common.icons.Icon_Plugin
import de.dh.daps.ui.common.icons.Icon_Pump_Device
import de.dh.daps.ui.common.icons.Icon_Reminders
import de.dh.daps.ui.common.icons.Icon_Screen_Back
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun PastRemindersScreen(
    viewModel: PastRemindersViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    PastRemindersContent(
        uiState = uiState,
        onReactivate = { reminder -> viewModel.reactivateReminder(reminder) },
        onDelete = { id -> viewModel.deleteReminder(id) },
        onClearAll = { viewModel.clearAllPastReminders() },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastRemindersContent(
    uiState: PastRemindersUiState,
    onReactivate: (Reminder) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(text = stringResource(R.string.past_reminders_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icon_Screen_Back,
                            contentDescription = stringResource(CommonR.string.cd_navigate_up)
                        )
                    }
                },
                actions = {
                    if (uiState.pastReminders.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(CommonR.string.cd_more_options)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.past_reminders_menu_clear_all)) },
                                    onClick = {
                                        showMenu = false
                                        showClearAllDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.pastReminders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icon_Reminders,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.past_reminders_empty_state_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.past_reminders_empty_state_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.pastReminders,
                        key = { it.id }
                    ) { reminder ->
                        PastReminderCard(
                            reminder = reminder,
                            onReactivate = { onReactivate(reminder) },
                            onDelete = { reminderToDelete = reminder }
                        )
                    }
                }
            }
        }
    }

    reminderToDelete?.let { reminder ->
        val titleText = reminder.title.ifBlank { stringResource(CommonR.string.notification_meal_reminder_title) }
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = {
                Text(text = stringResource(R.string.delete_reminder_title))
            },
            text = {
                Text(text = stringResource(R.string.delete_reminder_message, titleText))
            },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        onDelete(reminder.id)
                        reminderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { reminderToDelete = null }) {
                    Text(text = stringResource(CommonR.string.action_cancel))
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(text = stringResource(R.string.past_reminders_clear_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.past_reminders_clear_dialog_message))
            },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        onClearAll()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showClearAllDialog = false }) {
                    Text(text = stringResource(CommonR.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun PastReminderCard(
    reminder: Reminder,
    onReactivate: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (reminder.type) {
        ReminderType.MEAL -> Icon_Meal
        ReminderType.PLUGIN -> Icon_Plugin
        ReminderType.PUMP -> Icon_Pump_Device
        ReminderType.CGM -> Icon_Cgm_Sensor
        ReminderType.CUSTOM -> Icon_Reminders
    }

    val titleText = reminder.title.ifBlank { stringResource(CommonR.string.notification_meal_reminder_title) }
    val formattedTime = shortDateTime(reminder.reminderTimestamp)

    val now = Timestamp.now()
    val canReactivate = reminder.reminderTimestamp > now.plusMinutes(1)

    val statusText = when (reminder.status) {
        ReminderStatus.DISMISSED -> stringResource(R.string.past_reminders_status_dismissed)
        ReminderStatus.EXPIRED -> stringResource(R.string.past_reminders_status_expired)
        ReminderStatus.TRIGGERED -> stringResource(R.string.past_reminders_status_triggered)
        ReminderStatus.ACTIVE -> stringResource(R.string.past_reminders_status_active)
        ReminderStatus.SNOOZED -> stringResource(R.string.past_reminders_status_snoozed)
    }

    FramedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icon_Delete,
                        contentDescription = stringResource(R.string.cd_reminders_action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            if (reminder.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(statusText) }
                )

                SecondaryButton(
                    onClick = onReactivate,
                    enabled = canReactivate
                ) {
                    Text(stringResource(R.string.past_reminders_action_reactivate))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Past Reminders Empty - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Past Reminders Empty - Dark")
@Composable
fun PastRemindersEmptyPreview() {
    AppPreview {
        PastRemindersContent(
            uiState = PastRemindersUiState(pastReminders = emptyList()),
            onReactivate = {},
            onDelete = {},
            onClearAll = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Past Reminders - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Past Reminders - Dark")
@Composable
fun PastRemindersWithDataPreview() {
    val samplePastReminders = listOf(
        Reminder(
            id = 1,
            type = ReminderType.MEAL,
            title = "Mahlzeit-Erinnerung",
            description = "Mahlzeit mit 60g Kohlenhydraten",
            reminderTimestamp = Timestamp.now().plusMinutes(15),
            status = ReminderStatus.DISMISSED
        ),
        Reminder(
            id = 2,
            type = ReminderType.PUMP,
            title = "Katheter wechseln",
            description = "Katheterwechsel fällig",
            reminderTimestamp = Timestamp.now().minusHours(3),
            status = ReminderStatus.EXPIRED
        )
    )

    AppPreview {
        PastRemindersContent(
            uiState = PastRemindersUiState(pastReminders = samplePastReminders),
            onReactivate = {},
            onDelete = {},
            onClearAll = {},
            onNavigateUp = {}
        )
    }
}