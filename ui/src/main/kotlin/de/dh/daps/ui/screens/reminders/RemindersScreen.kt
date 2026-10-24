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
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.reminder.Reminder
import de.dh.daps.common.model.reminder.ReminderType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.ScreenTitle
import de.dh.daps.ui.common.icons.Icon_Check_Yes
import de.dh.daps.ui.common.icons.Icon_Delete
import de.dh.daps.ui.common.icons.Icon_Meal
import de.dh.daps.ui.common.icons.Icon_Pump_Device
import de.dh.daps.ui.common.icons.Icon_Reminders
import de.dh.daps.ui.common.icons.Icon_Screen_Back
import de.dh.daps.ui.common.icons.Icon_Therapy_Adjustment
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.shortRelativeTimeUntil
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    RemindersContent(
        uiState = uiState,
        onSnooze = { id, minutes -> viewModel.snoozeReminder(id, minutes) },
        onDismiss = { id -> viewModel.dismissReminder(id) },
        onDelete = { id -> viewModel.deleteReminder(id) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersContent(
    uiState: RemindersUiState,
    onSnooze: (Long, Int) -> Unit,
    onDismiss: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(text = stringResource(R.string.reminders_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icon_Screen_Back,
                            contentDescription = stringResource(CommonR.string.cd_navigate_up)
                        )
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
            if (uiState.activeReminders.isEmpty()) {
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
                        text = stringResource(R.string.reminders_empty_state_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.reminders_empty_state_description),
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
                        items = uiState.activeReminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onSnooze = { minutes -> onSnooze(reminder.id, minutes) },
                            onDismiss = { onDismiss(reminder.id) },
                            onDelete = { onDelete(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (reminder.type) {
        ReminderType.MEAL -> Icon_Meal
        ReminderType.PLUGIN -> Icon_Pump_Device
        ReminderType.THERAPY -> Icon_Therapy_Adjustment
        ReminderType.CUSTOM -> Icon_Reminders
    }

    val titleText = reminder.title.ifBlank { stringResource(CommonR.string.notification_meal_reminder_title) }
    val formattedTime = shortDateTime(reminder.reminderTimestamp)
    val relativeTime = shortRelativeTimeUntil(reminder.reminderTimestamp)

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
                        text = "$formattedTime ($relativeTime)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icon_Delete,
                        contentDescription = stringResource(R.string.reminders_action_delete),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { onSnooze(15) },
                        label = { Text(stringResource(R.string.reminders_snooze_15m)) }
                    )
                    AssistChip(
                        onClick = { onSnooze(30) },
                        label = { Text(stringResource(R.string.reminders_snooze_30m)) }
                    )
                    AssistChip(
                        onClick = { onSnooze(60) },
                        label = { Text(stringResource(R.string.reminders_snooze_1h)) }
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icon_Check_Yes,
                        contentDescription = stringResource(R.string.reminders_action_dismiss),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Empty State - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Empty State - Dark")
@Composable
fun RemindersEmptyPreview() {
    AppPreview {
        RemindersContent(
            uiState = RemindersUiState(activeReminders = emptyList()),
            onSnooze = { _, _ -> },
            onDismiss = {},
            onDelete = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "With Reminders - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "With Reminders - Dark")
@Composable
fun RemindersWithDataPreview() {
    val sampleReminders = listOf(
        Reminder(
            id = 1,
            type = ReminderType.MEAL,
            title = "Nahrungserinnerung",
            description = "Mahlzeit mit 45g Kohlenhydraten",
            reminderTimestamp = Timestamp.now().plusMinutes(15)
        ),
        Reminder(
            id = 2,
            type = ReminderType.PLUGIN,
            title = "Pod-Wechsel fällig",
            description = "Reservoir fast leer. Bitte Pod tauschen.",
            reminderTimestamp = Timestamp.now().plusMinutes(45)
        ),
        Reminder(
            id = 3,
            type = ReminderType.THERAPY,
            title = "Basalrate kontrollieren",
            description = "Basalraten-Anpassung überprüfen",
            reminderTimestamp = Timestamp.now().plusHours(2)
        )
    )

    AppPreview {
        RemindersContent(
            uiState = RemindersUiState(activeReminders = sampleReminders),
            onSnooze = { _, _ -> },
            onDismiss = {},
            onDelete = {},
            onNavigateUp = {}
        )
    }
}