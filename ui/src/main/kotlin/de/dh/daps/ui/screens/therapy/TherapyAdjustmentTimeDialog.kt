package de.dh.daps.ui.screens.therapy

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.AdjustmentTimeMode
import de.dh.daps.common.model.data.TherapyAdjustmentTiming
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentTimeDialog(
    initialTiming: TherapyAdjustmentTiming,
    initialMode: AdjustmentTimeMode,
    onTimingSelected: (TherapyAdjustmentTiming) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(initialMode) }

    val now = remember { Timestamp.now() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Calculate initial duration in minutes
    val initialDurationMins = remember(initialTiming) {
        val startTime = initialTiming.startTime
        val endTime = initialTiming.endTime
        if (endTime != null && startTime != null) {
            ((endTime - startTime) / 60_000L).toInt().coerceAtLeast(30)
        } else if (endTime != null) {
            ((endTime - now) / 60_000L).toInt().coerceAtLeast(30)
        } else {
            60
        }
    }

    var durationMinutes by remember { mutableIntStateOf(initialDurationMins) }

    // Start/End calendar objects
    val startCal = remember(initialTiming) {
        Calendar.getInstance().apply {
            timeInMillis = initialTiming.startTime?.ms ?: now.ms
        }
    }
    val endCal = remember(initialTiming) {
        Calendar.getInstance().apply {
            timeInMillis = initialTiming.endTime?.ms ?: (now.ms + durationMinutes * 60_000L)
        }
    }

    var startTimeMs by remember { mutableStateOf(startCal.timeInMillis) }
    var endTimeMs by remember { mutableStateOf(endCal.timeInMillis) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NormalTextButton(
                onClick = {
                    val resultTiming = when (selectedMode) {
                        AdjustmentTimeMode.AD_HOC -> TherapyAdjustmentTiming(
                            mode = AdjustmentTimeMode.AD_HOC,
                            startTime = null,
                            endTime = null
                        )
                        AdjustmentTimeMode.DURATION -> {
                            val start = Timestamp.now()
                            val end = Timestamp(start.ms + durationMinutes * 60_000L)
                            TherapyAdjustmentTiming(
                                mode = AdjustmentTimeMode.DURATION,
                                startTime = start,
                                endTime = end
                            )
                        }
                        AdjustmentTimeMode.TIME_WINDOW -> TherapyAdjustmentTiming(
                            mode = AdjustmentTimeMode.TIME_WINDOW,
                            startTime = Timestamp(startTimeMs),
                            endTime = Timestamp(endTimeMs)
                        )
                    }
                    onTimingSelected(resultTiming)
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.therapy_adjustment_time_dialog_title))
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode selector chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (selectedMode == AdjustmentTimeMode.AD_HOC),
                        onClick = { selectedMode = AdjustmentTimeMode.AD_HOC },
                        label = { Text(stringResource(R.string.therapy_adjustment_mode_adhoc)) }
                    )
                    FilterChip(
                        selected = (selectedMode == AdjustmentTimeMode.DURATION),
                        onClick = { selectedMode = AdjustmentTimeMode.DURATION },
                        label = { Text(stringResource(R.string.therapy_adjustment_mode_duration)) }
                    )
                    FilterChip(
                        selected = (selectedMode == AdjustmentTimeMode.TIME_WINDOW),
                        onClick = { selectedMode = AdjustmentTimeMode.TIME_WINDOW },
                        label = { Text(stringResource(R.string.therapy_adjustment_mode_timewindow)) }
                    )
                }

                when (selectedMode) {
                    AdjustmentTimeMode.AD_HOC -> {
                        Text(
                            text = stringResource(R.string.therapy_adjustment_adhoc_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AdjustmentTimeMode.DURATION -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.therapy_adjustment_duration_select_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 60, 90, 120, 180, 240).forEach { mins ->
                                    FilterChip(
                                        selected = (durationMinutes == mins),
                                        onClick = { durationMinutes = mins },
                                        label = {
                                            Text(
                                                if (mins < 60) "$mins Min" else "${mins / 60} Std${if (mins % 60 > 0) " ${mins % 60}m" else ""}"
                                            )
                                        }
                                    )
                                }
                            }

                            val calculatedEndTimeMs = remember(now, durationMinutes) {
                                now.ms + durationMinutes * 60_000L
                            }
                            Text(
                                text = stringResource(
                                    R.string.therapy_adjustment_duration_active_until,
                                    timeFormat.format(calculatedEndTimeMs)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    AdjustmentTimeMode.TIME_WINDOW -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.therapy_adjustment_start_time_label),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = startTimeMs
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                }
                                                startTimeMs = updated.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(timeFormat.format(startTimeMs))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.therapy_adjustment_end_time_label),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = endTimeMs }
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = endTimeMs
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                }
                                                endTimeMs = updated.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(timeFormat.format(endTimeMs))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}