package de.dh.daps.ui.screens.therapy

import android.app.TimePickerDialog
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import de.dh.daps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTherapyAdjustmentScreen(
    viewModel: ScheduledTherapyViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formStateHolder.formState.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val isDirty = viewModel.formStateHolder.isDirty()

    ScheduledTherapyAdjustmentContent(
        formState = formState,
        startTime = startTime,
        endTime = endTime,
        baseTarget = uiState.baseTarget,
        baseLow = uiState.baseLow,
        isDirty = isDirty,
        onValuesChange = { p, t, l, a, h ->
            viewModel.setFormValues(p, t, l, a, h)
        },
        onStartTimeChange = { viewModel.setStartTime(it) },
        onEndTimeChange = { viewModel.setEndTime(it) },
        onPresetApplied = { preset ->
            viewModel.applyPreset(preset)
        },
        onScheduleClicked = {
            viewModel.saveScheduledAdjustment {
                onNavigateUp()
            }
        },
        onDiscardClicked = {
            viewModel.reset()
            onNavigateUp()
        },
        onNavigateUp = onNavigateUp,
        availableAlarmProfiles = uiState.availableAlarmProfiles,
        presets = uiState.therapyAdjustmentPresets
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduledTherapyAdjustmentContent(
    formState: TherapyAdjustmentFormState,
    startTime: Timestamp,
    endTime: Timestamp,
    baseTarget: BgValue,
    baseLow: BgValue,
    isDirty: Boolean,
    onValuesChange: (percentage: Int, targetBg: BgValue?, lowThreshold: BgValue?, alarmProfileId: Long?, adjustmentHint: String?) -> Unit,
    onStartTimeChange: (Timestamp) -> Unit,
    onEndTimeChange: (Timestamp) -> Unit,
    onPresetApplied: (TherapyAdjustment) -> Unit,
    onScheduleClicked: () -> Unit,
    onDiscardClicked: () -> Unit,
    onNavigateUp: () -> Unit,
    availableAlarmProfiles: List<AlarmProfile> = emptyList(),
    presets: List<TherapyAdjustment> = emptyList()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun handleBack() {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = ::handleBack)

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val startCal = remember(startTime) {
        Calendar.getInstance().apply { timeInMillis = startTime.ms }
    }
    val endCal = remember(endTime) {
        Calendar.getInstance().apply { timeInMillis = endTime.ms }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = screenTitle(stringResource(id = R.string.scheduled_therapy_adjustment_screen_title)),
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            imageVector = if (isDirty) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isDirty) stringResource(id = R.string.therapy_adjustment_discard_dialog_title) else stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .contentScrollIndicator(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                        }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Shared Inner Form
                    TherapyAdjustmentInnerForm(
                        formState = formState,
                        baseTarget = baseTarget,
                        baseLow = baseLow,
                        availableAlarmProfiles = availableAlarmProfiles,
                        onValuesChange = onValuesChange
                    )

                    if (presets.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        TherapyAdjustmentPresetsSection(
                            presets = presets,
                            onPresetApplied = onPresetApplied
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Time Range Picker Section for Scheduled Activity
                    Text(
                        text = stringResource(R.string.therapy_adjustment_timing_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = startTime.ms
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                            }
                                            onStartTimeChange(Timestamp(newCal.timeInMillis))
                                        },
                                        startCal.get(Calendar.HOUR_OF_DAY),
                                        startCal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.therapy_adjustment_start_time_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = timeFormat.format(startTime.ms),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // End Time Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = endTime.ms
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                            }
                                            onEndTimeChange(Timestamp(newCal.timeInMillis))
                                        },
                                        endCal.get(Calendar.HOUR_OF_DAY),
                                        endCal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.therapy_adjustment_end_time_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = timeFormat.format(endTime.ms),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Quick Duration Preset Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        val durationOptions = listOf(30, 60, 120, 180, 240)
                        durationOptions.forEach { mins ->
                            val durationMs = mins * 60_000L
                            val isSelected = (endTime.ms - startTime.ms) == durationMs
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onEndTimeChange(Timestamp(startTime.ms + durationMs))
                                },
                                label = {
                                    val label = if (mins >= 60) "${mins / 60} h" else "$mins min"
                                    Text(label)
                                }
                            )
                        }
                    }
                }
            }

            // Single Primary "Planen" Action Button outside contentScrollIndicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val durationMins = remember(startTime, endTime) {
                    ((endTime.ms - startTime.ms) / 60_000L).coerceAtLeast(0)
                }
                val durationText = remember(durationMins) {
                    if (durationMins >= 60) {
                        "${durationMins / 60}h ${durationMins % 60} min"
                    } else {
                        "$durationMins min"
                    }
                }
                val timeRangeText = remember(startTime, endTime, durationText) {
                    "${timeFormat.format(startTime.ms)} - ${timeFormat.format(endTime.ms)} ($durationText)"
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onScheduleClicked
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.therapy_adjustment_schedule_button),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = timeRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(text = stringResource(R.string.therapy_adjustment_discard_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.therapy_adjustment_discard_dialog_message))
            },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDiscardClicked()
                    }
                ) {
                    Text(text = stringResource(R.string.discard_confirm_button))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showDiscardDialog = false }) {
                    Text(text = stringResource(R.string.discard_dismiss_button))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Light Mode", heightDp = 1200)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", heightDp = 1200)
@Composable
private fun ScheduledTherapyAdjustmentPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                ScheduledTherapyAdjustmentContent(
                    formState = TherapyAdjustmentFormState(
                        percentage = -20,
                        targetBgOverride = BgValue.fromMgDl(130)
                    ),
                    startTime = Timestamp.now(),
                    endTime = Timestamp(System.currentTimeMillis() + 3600_000),
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    isDirty = true,
                    onValuesChange = { _, _, _, _, _ -> },
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onPresetApplied = {},
                    onScheduleClicked = {},
                    onDiscardClicked = {},
                    onNavigateUp = {}
                )
            }
        }
    }
}