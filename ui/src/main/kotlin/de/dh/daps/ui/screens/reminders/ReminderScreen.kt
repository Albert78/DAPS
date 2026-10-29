package de.dh.daps.ui.screens.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.ui.R as UiR
import de.dh.daps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    reminders: List<Reminder>,
    onSnoozeReminder: (Reminder) -> Unit,
    onDismissReminder: (Reminder) -> Unit,
    onOpenReminder: (Reminder) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(UiR.string.reminders_screen_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(CommonR.string.action_close))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            reminders.forEach { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onSnooze = { onSnoozeReminder(reminder) },
                    onDismiss = { onDismissReminder(reminder) },
                    onOpen = { onOpenReminder(reminder) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val title = reminder.title.ifBlank { stringResource(CommonR.string.notification_reminder_title) }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(UiR.string.reminders_action_snooze))
                }

                if (reminder.targetRoute.isNotBlank()) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(CommonR.string.cd_expand))
                    }
                }

                Button(
                    onClick = onDismiss
                ) {
                    Text(stringResource(UiR.string.cd_reminders_action_dismiss))
                }
            }
        }
    }
}