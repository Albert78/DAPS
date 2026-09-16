package de.dh.daps.ui.screens.therapy

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.AdjustmentTimeMode
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.TherapyAdjustmentTiming
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Locale
import de.dh.daps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyAdjustmentScreen(
    viewModel: CurrentTherapyViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTherapyStatus = uiState.activeTherapyStatus
    val draftAdjustment by viewModel.draftAdjustment.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initDraftAdjustment()
    }

    val currentDraft = draftAdjustment ?: activeTherapyStatus.adjustment
    val isDirty = viewModel.isDraftDirty()

    val formState = TherapyAdjustmentFormState(
        percentage = currentDraft.percentage,
        targetBgOverride = currentDraft.targetBgOverride,
        lowThresholdOverride = currentDraft.lowThresholdOverride,
        activeAlarmProfileId = currentDraft.activeAlarmProfileId,
        activeAlarmProfileName = currentDraft.activeAlarmProfileName,
        adjustmentHint = currentDraft.adjustmentHint,
        timing = currentDraft.timing
    )

    TherapyAdjustmentContent(
        formState = formState,
        baseTarget = activeTherapyStatus.baseTarget,
        baseLow = activeTherapyStatus.baseLow,
        isDirty = isDirty,
        onValuesChange = { p, t, l, a, h ->
            viewModel.setDraftValues(p, t, l, a, h)
        },
        onTimingChange = { timing ->
            viewModel.setDraftTiming(timing)
        },
        availableAlarmProfiles = uiState.availableAlarmProfiles,
        presets = uiState.therapyAdjustmentPresets,
        onPresetApplied = { preset ->
            viewModel.applyPreset(preset)
        },
        onApplyClicked = {
            viewModel.applyDraftAdjustment()
            onNavigateUp()
        },
        onDiscardClicked = {
            viewModel.resetDraft()
            onNavigateUp()
        },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyAdjustmentContent(
    formState: TherapyAdjustmentFormState,
    baseTarget: BgValue,
    baseLow: BgValue,
    isDirty: Boolean,
    onValuesChange: (percentage: Int, targetBg: BgValue?, lowThreshold: BgValue?, alarmProfileId: Long?, adjustmentHint: String?) -> Unit,
    onTimingChange: (TherapyAdjustmentTiming) -> Unit,
    onPresetApplied: (TherapyAdjustment) -> Unit,
    onApplyClicked: () -> Unit,
    onDiscardClicked: () -> Unit,
    onNavigateUp: () -> Unit,
    availableAlarmProfiles: List<AlarmProfile> = emptyList(),
    presets: List<TherapyAdjustment> = emptyList()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }

    fun handleBack() {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = screenTitle(stringResource(id = R.string.therapy_adjustment_title)),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()

            val isAdjustmentActive = remember(formState) {
                formState.percentage != 0 ||
                formState.targetBgOverride != null ||
                formState.lowThresholdOverride != null ||
                formState.activeAlarmProfileId != null
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentScrollIndicator(scrollState)
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
                if (!isDirty && isAdjustmentActive) {
                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val endTime = formState.timing.endTime
                    val activeHintText = if (endTime != null) {
                        stringResource(R.string.therapy_adjustment_active_until_format, timeFormat.format(endTime.ms))
                    } else {
                        stringResource(R.string.therapy_adjustment_active_now)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = activeHintText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Reusable Upper Form Section
                TherapyAdjustmentUpperForm(
                    formState = formState,
                    baseTarget = baseTarget,
                    baseLow = baseLow,
                    availableAlarmProfiles = availableAlarmProfiles,
                    presets = presets,
                    onValuesChange = onValuesChange,
                    onPresetApplied = onPresetApplied
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Action Buttons for Current Therapy Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            onTimingChange(TherapyAdjustmentTiming(mode = AdjustmentTimeMode.AD_HOC))
                            onApplyClicked()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.therapy_adjustment_unlimited),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showDurationDialog = true }
                    ) {
                        Text(
                            text = stringResource(R.string.therapy_adjustment_duration),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showDiscardDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDurationDialog) {
        TherapyAdjustmentDurationDialog(
            initialTiming = formState.timing,
            onTimingSelected = { newTiming ->
                onTimingChange(newTiming)
                showDurationDialog = false
                onApplyClicked()
            },
            onDismiss = { showDurationDialog = false }
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun TherapyAdjustmentPreviewValues() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                TherapyAdjustmentContent(
                    formState = TherapyAdjustmentFormState(
                        percentage = -10,
                        targetBgOverride = BgValue.fromMgDl(120),
                        lowThresholdOverride = BgValue.fromMgDl(80),
                        timing = TherapyAdjustmentTiming(
                            mode = AdjustmentTimeMode.DURATION,
                            endTime = Timestamp(System.currentTimeMillis() + 3600_000)
                        )
                    ),
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    isDirty = false,
                    onValuesChange = { _, _, _, _, _ -> },
                    onTimingChange = {},
                    onPresetApplied = {},
                    onApplyClicked = {},
                    onDiscardClicked = {},
                    onNavigateUp = {},
                    presets = listOf(
                        TherapyAdjustment("Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
                        TherapyAdjustment("Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
                    )
                )
            }
        }
    }
}