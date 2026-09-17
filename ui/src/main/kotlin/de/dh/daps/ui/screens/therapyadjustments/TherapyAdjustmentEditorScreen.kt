package de.dh.daps.ui.screens.therapyadjustments

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.model.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.model.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.model.LOW_THRESHOLD_MAX
import de.dh.daps.common.model.LOW_THRESHOLD_MIN
import de.dh.daps.common.model.TARGET_MAX
import de.dh.daps.common.model.TARGET_MIN
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.ModuloSteppingStrategy
import de.dh.daps.ui.common.ValueDisplayStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.common.R as CommonR

private data class InitialTherapyAdjustmentValues(
    val id: Long?,
    val name: String,
    val percentage: Int,
    val targetBgMgDl: Short?,
    val lowThresholdMgDl: Short?,
    val alarmProfileOverrideId: Long?
)

@Composable
fun TherapyAdjustmentEditorScreen(
    viewModel: TherapyAdjustmentEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    TherapyAdjustmentEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onPercentageChange = viewModel::onPercentageChange,
        onTargetBgChange = viewModel::onTargetBgChange,
        onLowThresholdChange = viewModel::onLowThresholdChange,
        onAlarmProfileOverrideChange = viewModel::onAlarmProfileOverrideChange,
        onSave = { viewModel.save(onNavigateUp) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentEditorContent(
    uiState: TherapyAdjustmentEditorUiState,
    onNameChange: (String) -> Unit,
    onPercentageChange: (Int) -> Unit,
    onTargetBgChange: (Short?) -> Unit,
    onLowThresholdChange: (Short?) -> Unit,
    onAlarmProfileOverrideChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val initialValues = remember(uiState.isLoading) {
        if (!uiState.isLoading) {
            InitialTherapyAdjustmentValues(
                id = uiState.id,
                name = uiState.name,
                percentage = uiState.percentage,
                targetBgMgDl = uiState.targetBgMgDl,
                lowThresholdMgDl = uiState.lowThresholdMgDl,
                alarmProfileOverrideId = uiState.alarmProfileOverrideId
            )
        } else null
    }

    val hasChanges = remember(
        uiState.name,
        uiState.percentage,
        uiState.targetBgMgDl,
        uiState.lowThresholdMgDl,
        uiState.alarmProfileOverrideId,
        initialValues
    ) {
        if (initialValues == null) false
        else {
            uiState.name != initialValues.name ||
                    uiState.percentage != initialValues.percentage ||
                    uiState.targetBgMgDl != initialValues.targetBgMgDl ||
                    uiState.lowThresholdMgDl != initialValues.lowThresholdMgDl ||
                    uiState.alarmProfileOverrideId != initialValues.alarmProfileOverrideId
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

    CompositionLocalProvider(LocalGlucoseUnit provides uiState.glucoseUnit) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopAppBar(
                    title = screenTitle(
                        if (uiState.id == null) stringResource(R.string.therapy_adjustment_editor_title_new)
                        else stringResource(R.string.therapy_adjustment_editor_title_edit)
                    ),
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
            val listState = rememberLazyListState()

            val steppingStrategyInsulin = remember { ModuloSteppingStrategy(5.0) }
            val steppingStrategyBg = remember { ModuloSteppingStrategy(5.0) }

            val displayStrategyInsulin = remember {
                ConfigurableDisplayStrategy(
                    positiveColor = SoftRed,
                    negativeColor = SoftBlue,
                    neutralColor = NeutralGrey,
                    positivePrefix = "+",
                    neutralLabel = "0 %"
                )
            }

            val glucoseUnit = uiState.glucoseUnit
            val displayStrategyBg = remember(glucoseUnit) {
                object : ValueDisplayStrategy {
                    override fun format(value: Double): String = BgValue.fromMgDl(value).toString(glucoseUnit)
                    override fun color(value: Double): Color = Color.Unspecified
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .contentScrollIndicator(listState),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.therapy_adjustment_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                // Insulin percentage
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.therapy_adjustment_percentage_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        EditableValueStepper(
                            currentValue = uiState.percentage.toDouble(),
                            onValueChange = { onPercentageChange(it.toInt()) },
                            steppingStrategy = steppingStrategyInsulin,
                            displayStrategy = displayStrategyInsulin,
                            minValue = ADJUSTMENT_PERCENTAGE_MIN.toDouble(),
                            maxValue = ADJUSTMENT_PERCENTAGE_MAX.toDouble(),
                            suffix = "%",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Target BG override
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val targetMgDl = uiState.targetBgMgDl
                        val hasTargetOverride = targetMgDl != null
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.therapy_adjustment_target_bg_override_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = hasTargetOverride,
                                onClick = {
                                    if (hasTargetOverride) {
                                        onTargetBgChange(null)
                                    } else {
                                        onTargetBgChange(DEFAULT_BG_TARGET_MGDL)
                                    }
                                },
                                label = {
                                    Text(
                                        if (targetMgDl != null) glucoseValue(BgValue.fromMgDl(targetMgDl.toInt()), withUnit = true)
                                        else stringResource(R.string.therapy_adjustment_override_none)
                                    )
                                }
                            )
                        }

                        if (targetMgDl != null) {
                            EditableValueStepper(
                                currentValue = targetMgDl.toDouble(),
                                onValueChange = { newVal ->
                                    onTargetBgChange(newVal.toInt().toShort())
                                },
                                steppingStrategy = steppingStrategyBg,
                                displayStrategy = displayStrategyBg,
                                minValue = TARGET_MIN.toDouble(),
                                maxValue = TARGET_MAX.toDouble(),
                                suffix = glucoseUnitLabel(uiState.glucoseUnit),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Low threshold override
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val lowMgDl = uiState.lowThresholdMgDl
                        val hasLowOverride = lowMgDl != null
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.therapy_adjustment_low_threshold_override_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            FilterChip(
                                selected = hasLowOverride,
                                onClick = {
                                    if (hasLowOverride) {
                                        onLowThresholdChange(null)
                                    } else {
                                        onLowThresholdChange(DEFAULT_BG_LOW_THRESHOLD_MGDL)
                                    }
                                },
                                label = {
                                    Text(
                                        if (lowMgDl != null) glucoseValue(BgValue.fromMgDl(lowMgDl.toInt()), withUnit = true)
                                        else stringResource(R.string.therapy_adjustment_override_none)
                                    )
                                }
                            )
                        }

                        if (lowMgDl != null) {
                            EditableValueStepper(
                                currentValue = lowMgDl.toDouble(),
                                onValueChange = { newVal ->
                                    onLowThresholdChange(newVal.toInt().toShort())
                                },
                                steppingStrategy = steppingStrategyBg,
                                displayStrategy = displayStrategyBg,
                                minValue = LOW_THRESHOLD_MIN.toDouble(),
                                maxValue = LOW_THRESHOLD_MAX.toDouble(),
                                suffix = glucoseUnitLabel(uiState.glucoseUnit),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Alarm Profile Override
                if (uiState.availableAlarmProfiles.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.therapy_adjustment_alarm_profile_override_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.alarmProfileOverrideId == null,
                                    onClick = { onAlarmProfileOverrideChange(null) },
                                    label = { Text(stringResource(R.string.therapy_adjustment_override_none)) }
                                )

                                uiState.availableAlarmProfiles.forEach { profile ->
                                    FilterChip(
                                        selected = uiState.alarmProfileOverrideId == profile.id,
                                        onClick = { onAlarmProfileOverrideChange(profile.id) },
                                        label = { Text(profile.name) }
                                    )
                                }
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
            title = { Text(stringResource(id = R.string.therapy_adjustment_editor_discard_title)) },
            text = { Text(stringResource(id = R.string.therapy_adjustment_editor_discard_message)) },
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

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun TherapyAdjustmentEditorPreview() {
    AppTheme {
        TherapyAdjustmentEditorContent(
            uiState = TherapyAdjustmentEditorUiState(
                name = "Fahrrad fahren",
                percentage = -30,
                targetBgMgDl = 150,
                lowThresholdMgDl = 100,
                availableAlarmProfiles = listOf(
                    AlarmProfile(id = 1L, name = "Standard"),
                    AlarmProfile(id = 2L, name = "Kino / Diskret")
                )
            ),
            onNameChange = {},
            onPercentageChange = {},
            onTargetBgChange = {},
            onLowThresholdChange = {},
            onAlarmProfileOverrideChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}