package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.dh.daps.ui.common.R
import de.dh.daps.ui.common.theme.AppPreview
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * A dialog that prompts the user for both date and time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    dialogTitle: String,
    initialDateTime: LocalDateTime,
    minDate: LocalDate? = null,
    timeZoneId: ZoneId = ZoneId.systemDefault(),
    onDismissRequest: () -> Unit,
    onDateTimeSelected: (LocalDateTime) -> Unit,
    is24HourView: Boolean = true
) {
    // 0 for Date, 1 for Time
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val minDateUtcMillis = remember(minDate) {
        minDate
            ?.atStartOfDay(ZoneId.of("UTC")) // Picker internally works with UTC
            ?.toInstant()
            ?.toEpochMilli()
    }

    // Date State
    val datePickerState = rememberDatePickerState(
        // NOTE: Setting initialSelectedDateMillis makes the preview in Android Studio show error message:
        // Cannot invoke "java.util.Locale.toLanguageTag()" because "locale" is null
        initialSelectedDateMillis = initialDateTime
            .atZone(timeZoneId)
            .toInstant()
            .toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return minDateUtcMillis == null || utcTimeMillis >= minDateUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= (minDate?.year ?: 0)
            }
        }
    )

    // Time State
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = is24HourView
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogSurface {
            Column(modifier = Modifier
                .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {

                DialogTitle(
                    dialogTitle,
                    modifier = Modifier
                        .padding(24.dp))

                HorizontalDivider()

                // Header Tabs
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.date_time_picker_dialog_date_button))
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.date_time_picker_dialog_time_button))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Content
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTabIndex == 0) {
                            DatePicker(
                                state = datePickerState,
                                title = null,
                                showModeToggle = true
                            )
                        } else {
                            TimePicker(state = timePickerState)
                        }
                    }

                    DialogConfirmDismissButtons(
                        modifier = Modifier
                            .padding(24.dp),
                        onConfirm = {
                            val selectedDateMillis =
                                datePickerState.selectedDateMillis ?: return@DialogConfirmDismissButtons
                            val date = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(timeZoneId)
                                .toLocalDate()

                            val result = LocalDateTime.of(
                                date,
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                            onDateTimeSelected(result)
                        },
                        onDismiss = onDismissRequest
                    )
                }
            }
        }
    }
}

/**
 * A dialog that prompts the user for the date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    dialogTitle: String,
    initialDate: LocalDate,
    timeZoneId: ZoneId = ZoneId.systemDefault(),
    minDate: LocalDate? = null,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val minDateUtcMillis = remember(minDate) {
        minDate
            ?.atStartOfDay(ZoneId.of("UTC")) // Picker internally works with UTC
            ?.toInstant()
            ?.toEpochMilli()
    }

    // Date State
    val datePickerState = rememberDatePickerState(
        // NOTE: Setting initialSelectedDateMillis makes the preview in Android Studio show warning message:
        // Render issues: Cannot invoke "java.util.Locale.toLanguageTag()" because "locale" is null.
        // But the preview works and the app also works on the phone, maybe a bug in Android Studio 2025 2.2 Patch 1.
        initialSelectedDateMillis = initialDate
            .atStartOfDay()
            .atZone(timeZoneId)
            .toInstant()
            .toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return minDateUtcMillis == null || utcTimeMillis >= minDateUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= (minDate?.year ?: 0)
            }
        }
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogSurface {
            Column(modifier = Modifier
                .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {

                DialogTitle(
                    dialogTitle,
                    modifier = Modifier
                        .padding(24.dp))

                HorizontalDivider()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Content
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = null,
                            showModeToggle = true
                        )
                    }

                    DialogConfirmDismissButtons(
                        modifier = Modifier
                            .padding(24.dp),
                        onConfirm = {
                            val selectedDateMillis =
                                datePickerState.selectedDateMillis ?: return@DialogConfirmDismissButtons
                            val result = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(timeZoneId)
                                .toLocalDate()

                            onDateSelected(result)
                        },
                        onDismiss = onDismissRequest
                    )
                }
            }
        }
    }
}

/**
 * A dialog that prompts the user for the time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    dialogTitle: String,
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    is24HourView: Boolean = true
) {
    // Time State
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24HourView
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogSurface {
            Column(modifier = Modifier
                .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {

                DialogTitle(
                    dialogTitle,
                    modifier = Modifier
                        .padding(24.dp))

                HorizontalDivider()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 16.dp)
                ) {
                    // Content
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState)
                    }

                    DialogConfirmDismissButtons(
                        modifier = Modifier
                            .padding(24.dp),
                        onConfirm = {
                            val result = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            onTimeSelected(result)
                        },
                        onDismiss = onDismissRequest
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DateTimePickerDialogPreview() {
    AppPreview {
        DateTimePickerDialog(
            dialogTitle = "Zeit und Datum eintragen",
            initialDateTime = LocalDateTime.now(),
            onDismissRequest = {},
            onDateTimeSelected = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DatePickerDialogPreview() {
    AppPreview {
        DatePickerDialog(
            dialogTitle = "Datum eintragen",
            initialDate = LocalDate.now(),
            onDismissRequest = {},
            onDateSelected = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TimePickerDialogPreview() {
    AppPreview {
        TimePickerDialog(
            dialogTitle = "Zeit eintragen",
            initialTime = LocalTime.now(),
            onDismissRequest = {},
            onTimeSelected = {},
            is24HourView = true
        )
    }
}