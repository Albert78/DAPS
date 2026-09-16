package de.dh.daps.ui.screens.therapy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AbsoluteTimeStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.TimeStepper
import de.dh.daps.ui.common.composables.contentScrollIndicator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import de.dh.daps.common.R as CommonR

private enum class DurationMode {
    RELATIVE,
    ABSOLUTE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentDurationDialog(
    initialEndTime: Timestamp?,
    onEndTimeSelected: (Timestamp) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(DurationMode.RELATIVE) }

    val now = remember { Timestamp.now() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val minEndTime = remember(now) { now + Minutes(5) }
    val maxEndTime = remember(now) { now + Minutes((24 * 60).toShort()) }

    val computedInitialEndTime = remember(initialEndTime) {
        if (initialEndTime != null && initialEndTime > minEndTime) {
            initialEndTime
        } else {
            now + Minutes(60)
        }
    }

    var targetEndTime by remember { mutableStateOf(computedInitialEndTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NormalTextButton(
                onClick = {
                    onEndTimeSelected(targetEndTime)
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
            Text(text = stringResource(id = R.string.therapy_adjustment_duration_dialog_title))
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented Button Row: Relative vs. Absolute
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = (selectedMode == DurationMode.RELATIVE),
                        onClick = { selectedMode = DurationMode.RELATIVE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.therapy_adjustment_duration_mode_relative))
                    }
                    SegmentedButton(
                        selected = (selectedMode == DurationMode.ABSOLUTE),
                        onClick = { selectedMode = DurationMode.ABSOLUTE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.therapy_adjustment_duration_mode_absolute))
                    }
                }

                when (selectedMode) {
                    DurationMode.RELATIVE -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TimeStepper(
                                currentTime = targetEndTime,
                                onTimeChange = { newTime ->
                                    if (newTime >= minEndTime && newTime <= maxEndTime) {
                                        targetEndTime = newTime
                                    }
                                },
                                baseTime = now,
                                showPreposition = false,
                                stepMinutes = 15,
                                minTime = minEndTime,
                                maxTime = maxEndTime
                            )

                            // Quick selection preset chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 60, 90, 120, 180, 240).forEach { mins ->
                                    val presetEndTime = now + Minutes(mins.toShort())
                                    val isSelected = remember(targetEndTime, presetEndTime) {
                                        val diff = abs(targetEndTime.ms - presetEndTime.ms)
                                        diff < 120_000L // within 2 mins
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { targetEndTime = presetEndTime },
                                        label = {
                                            val labelText = when {
                                                mins < 60 -> stringResource(id = CommonR.string.duration_minutes_format, mins)
                                                mins % 60 == 0 -> stringResource(id = CommonR.string.duration_hours_format, mins / 60)
                                                else -> stringResource(id = CommonR.string.duration_hours_and_minutes_format, mins / 60, mins % 60)
                                            }
                                            Text(labelText)
                                        }
                                    )
                                }
                            }

                            Text(
                                text = stringResource(
                                    R.string.therapy_adjustment_duration_active_until,
                                    timeFormat.format(targetEndTime.ms)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    DurationMode.ABSOLUTE -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AbsoluteTimeStepper(
                                currentTime = targetEndTime,
                                onTimeChange = { newTime ->
                                    if (newTime >= minEndTime && newTime <= maxEndTime) {
                                        targetEndTime = newTime
                                    }
                                },
                                stepMinutes = 5,
                                minTime = minEndTime,
                                maxTime = maxEndTime
                            )

                            val diffMin = round((targetEndTime.ms - now.ms) / 60000.0).toInt().coerceAtLeast(0)
                            val hours = diffMin / 60
                            val mins = diffMin % 60
                            val durationText = when {
                                hours == 0 -> stringResource(CommonR.string.duration_minutes_format, mins)
                                mins == 0 -> stringResource(CommonR.string.duration_hours_format, hours)
                                else -> stringResource(CommonR.string.duration_hours_and_minutes_format, hours, mins)
                            }

                            Text(
                                text = stringResource(R.string.therapy_adjustment_duration_label, durationText),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}