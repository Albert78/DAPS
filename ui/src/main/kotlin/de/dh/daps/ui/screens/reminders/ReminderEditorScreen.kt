package de.dh.daps.ui.screens.reminders

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AbsoluteTimeStepper
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.ScreenTitle
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.shortRelativeTimeUntil
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

private data class InitialReminderValues(
    val id: Long?,
    val title: String,
    val description: String,
    val type: ReminderType,
    val reminderTimestamp: Timestamp
)

@Composable
fun ReminderEditorScreen(
    viewModel: ReminderEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ReminderEditorContent(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onReminderTimestampChange = viewModel::onReminderTimestampChange,
        onSave = { viewModel.save(onNavigateUp) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorContent(
    uiState: ReminderEditorUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onReminderTimestampChange: (Timestamp) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val initialValues = remember(uiState.isLoading) {
        if (!uiState.isLoading) {
            InitialReminderValues(
                id = uiState.id,
                title = uiState.title,
                description = uiState.description,
                type = uiState.type,
                reminderTimestamp = uiState.reminderTimestamp
            )
        } else null
    }

    val hasChanges = remember(
        uiState.title,
        uiState.description,
        uiState.type,
        uiState.reminderTimestamp,
        initialValues
    ) {
        if (initialValues == null) false
        else {
            uiState.title != initialValues.title ||
                    uiState.description != initialValues.description ||
                    uiState.type != initialValues.type ||
                    uiState.reminderTimestamp != initialValues.reminderTimestamp
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

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    ScreenTitle(
                        text = if (uiState.id == null) stringResource(R.string.reminder_editor_title_new)
                        else stringResource(R.string.reminder_editor_title_edit)
                    )
                },
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
                            contentDescription = stringResource(CommonR.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.reminder_editor_label_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.reminder_editor_label_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.reminder_editor_label_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FramedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val formattedTime = shortDateTime(uiState.reminderTimestamp)
                            val relativeTime = shortRelativeTimeUntil(uiState.reminderTimestamp)

                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "($relativeTime)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val now = Timestamp.now()
                            AbsoluteTimeStepper(
                                currentTime = uiState.reminderTimestamp,
                                onTimeChange = onReminderTimestampChange,
                                minTime = now,
                                maxTime = now + Minutes((1440 * 20).toShort())
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = { onReminderTimestampChange(now + Minutes(15)) },
                                    label = { Text("+15 Min") }
                                )
                                AssistChip(
                                    onClick = { onReminderTimestampChange(now + Minutes(30)) },
                                    label = { Text("+30 Min") }
                                )
                                AssistChip(
                                    onClick = { onReminderTimestampChange(now + Minutes(60)) },
                                    label = { Text("+1 Std") }
                                )
                                AssistChip(
                                    onClick = { onReminderTimestampChange(now + Minutes(120)) },
                                    label = { Text("+2 Std") }
                                )
                                AssistChip(
                                    onClick = { onReminderTimestampChange(now + Minutes(1440)) },
                                    label = { Text("+1 Tag") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(id = R.string.reminder_editor_discard_title)) },
            text = { Text(stringResource(id = R.string.reminder_editor_discard_message)) },
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

@Preview(showBackground = true, name = "New Reminder - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "New Reminder - Dark Mode")
@Composable
fun ReminderEditorNewPreview() {
    AppPreview {
        ReminderEditorContent(
            uiState = ReminderEditorUiState(
                title = "Blutzucker messen",
                description = "Kontrolle nach dem Essen",
                type = ReminderType.CUSTOM
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onReminderTimestampChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Reminder - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Edit Reminder - Dark Mode")
@Composable
fun ReminderEditorEditPreview() {
    AppPreview {
        ReminderEditorContent(
            uiState = ReminderEditorUiState(
                id = 1L,
                title = "Mahlzeit-Erinnerung",
                description = "Spritz-Ess-Abstand beachten",
                type = ReminderType.MEAL
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onReminderTimestampChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}