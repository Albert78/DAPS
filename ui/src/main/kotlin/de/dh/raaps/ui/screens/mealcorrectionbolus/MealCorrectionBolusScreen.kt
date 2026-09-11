package de.dh.raaps.ui.screens.mealcorrectionbolus

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.BOLUS_MAX
import de.dh.raaps.common.model.BOLUS_MIN
import de.dh.raaps.common.model.CARBS_KE_MAX
import de.dh.raaps.common.model.CARBS_KE_MIN
import de.dh.raaps.common.model.CarbCurveComponentData
import de.dh.raaps.common.model.ID_MEAL_FAST
import de.dh.raaps.common.model.ID_MEAL_HIGH_FAT
import de.dh.raaps.common.model.ID_MEAL_SLOW
import de.dh.raaps.common.model.ID_MEAL_STANDARD
import de.dh.raaps.common.model.InsulinAmount
import de.dh.raaps.common.model.MealType
import de.dh.raaps.common.model.data.BgDelta
import de.dh.raaps.common.model.data.BgValue
import de.dh.raaps.common.model.data.GlucoseUnit
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.common.model.data.Timestamp
import de.dh.raaps.core.aps.BolusProjections
import de.dh.raaps.core.aps.ProjectedBg
import de.dh.raaps.core.aps.TreatmentLock
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.DefaultSteppingStrategy
import de.dh.raaps.ui.common.LocalGlucoseUnit
import de.dh.raaps.ui.common.ModuloSteppingStrategy
import de.dh.raaps.ui.common.ValueDisplayStrategy
import de.dh.raaps.ui.common.carbsGramsValue
import de.dh.raaps.ui.common.carbsKeUnitLabel
import de.dh.raaps.ui.common.composables.AppColorBlue
import de.dh.raaps.ui.common.composables.EditableValueStepper
import de.dh.raaps.ui.common.composables.ImageCaptionWithSwitch
import de.dh.raaps.ui.common.composables.LightGreenA700
import de.dh.raaps.ui.common.composables.PrimaryButton
import de.dh.raaps.ui.common.composables.Red
import de.dh.raaps.ui.common.composables.StepperDefaults
import de.dh.raaps.ui.common.composables.TimeStepper
import de.dh.raaps.ui.common.composables.TimeStepperDefaults
import de.dh.raaps.ui.common.composables.Yellow
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.crValue
import de.dh.raaps.ui.common.glucoseUnitLabel
import de.dh.raaps.ui.common.glucoseValue
import de.dh.raaps.ui.common.insulinUnitLabel
import de.dh.raaps.ui.common.insulinValue
import de.dh.raaps.ui.common.isfValue
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.common.time
import de.dh.raaps.ui.common.withinTimeDescription
import de.dh.raaps.ui.controls.meal.BolusPlanEditorDialog
import de.dh.raaps.ui.controls.meal.FoodTypeSelector
import de.dh.raaps.ui.controls.meal.PlannedBolusUiModel
import java.util.Locale
import kotlin.math.abs
import de.dh.raaps.common.R as CommonR

@Composable
fun MealCorrectionBolusScreen(
    viewModel: MealCorrectionBolusViewModel,
    treatmentLock: TreatmentLock,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MealCorrectionBolusContent(
        uiState = uiState,
        onCarbsChange = { viewModel.onCarbsChange(it) },
        onMealTimeChange = { viewModel.onMealTimeChange(it) },
        onApplySuggestedCarbs = { viewModel.onApplySuggestedCarbs() },
        onApplySuggestedImi = { viewModel.onApplySuggestedImi() },
        onMealTypeChange = { viewModel.onMealTypeChange(it) },
        onManualBolusChange = { viewModel.onManualBolusChange(it) },
        onOpenBolusPlanDialog = { viewModel.onOpenBolusPlanDialog() },
        onCloseBolusPlanDialog = { viewModel.onCloseBolusPlanDialog() },
        onUpdateBolusTime = { index, time -> viewModel.onUpdateBolusTime(index, time) },
        onUpdateBolusAmount = { index, amount -> viewModel.onUpdateBolusAmount(index, amount) },
        onAddDeferredBolus = { viewModel.onAddDeferredBolus() },
        onRemoveDeferredBolus = { viewModel.onRemoveDeferredBolus(it) },
        onToggleMealReminder = { viewModel.onToggleMealReminder() },
        onRefreshProjections = { viewModel.onRefreshProjections() },
        onClose = onNavigateUp,
        onSubmit = { viewModel.submit(treatmentLock, onNavigateUp) }
    )
}

@Composable
fun MealCorrectionBolusContent(
    uiState: MealCorrectionBolusUiState,
    onCarbsChange: (Double) -> Unit,
    onMealTimeChange: (Timestamp) -> Unit,
    onApplySuggestedCarbs: () -> Unit,
    onApplySuggestedImi: () -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onManualBolusChange: (Double) -> Unit,
    onOpenBolusPlanDialog: () -> Unit,
    onCloseBolusPlanDialog: () -> Unit,
    onUpdateBolusTime: (Int, Timestamp) -> Unit,
    onUpdateBolusAmount: (Int, InsulinAmount) -> Unit,
    onAddDeferredBolus: () -> Unit,
    onRemoveDeferredBolus: (Int) -> Unit,
    onToggleMealReminder: () -> Unit,
    onRefreshProjections: () -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .contentScrollIndicator(scrollState)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MealCorrectionBolusContextInfo(uiState = uiState, onRefresh = onRefreshProjections)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.showCloseBanner) {
                    CloseScreenBanner(onClose = onClose)
                }

                // Mahlzeit Card (Carbs + Food Type + Meal Time)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.meal_correction_bolus_carbs_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.suggestedCarbsKe > 0.0) {
                                Spacer(Modifier.height(4.dp))
                                val isCarbsBelowSuggestion = uiState.input.carbsKe < uiState.suggestedCarbsKe - 0.01
                                SuggestionBadge(
                                    label = stringResource(
                                        R.string.meal_correction_bolus_suggested_carbs_format,
                                        uiState.suggestedCarbsKe,
                                        carbsKeUnitLabel()
                                    ),
                                    isHighlighted = isCarbsBelowSuggestion,
                                    onClick = onApplySuggestedCarbs
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            EditableValueStepper(
                                currentValue = uiState.input.carbsKe,
                                onValueChange = onCarbsChange,
                                minValue = CARBS_KE_MIN,
                                maxValue = CARBS_KE_MAX,
                                steppingStrategy = DefaultSteppingStrategy(0.5), // 0.5 KE steps
                                displayStrategy = object : ValueDisplayStrategy {
                                    override fun format(value: Double): String =
                                        String.format(Locale.getDefault(), "%.1f", value)

                                    override fun color(value: Double): Color = Color.Unspecified
                                },
                                suffix = " ${carbsKeUnitLabel()}",
                                style = StepperDefaults.defaultStyle()
                            )
                        }

                        if (uiState.input.carbsKe > 0.0) {
                            FoodTypeSelector(
                                mealTypes = uiState.mealTypes,
                                selectedType = uiState.input.selectedMealType,
                                onTypeSelected = onMealTypeChange,
                                isMandatory = true
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.meal_correction_bolus_meal_time_label),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val suggestedMinutes = uiState.suggestedImi.value.toInt()
                                if (suggestedMinutes > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    val now = Timestamp.now()
                                    val suggestedTimestamp = now + Minutes(suggestedMinutes.toShort())
                                    val isSuggestedTimeApplied = abs(uiState.input.mealTimestamp.ms - suggestedTimestamp.ms) < 60000

                                    SuggestionBadge(
                                        label = stringResource(R.string.meal_correction_bolus_suggested_time_format, suggestedMinutes),
                                        isHighlighted = !isSuggestedTimeApplied,
                                        onClick = onApplySuggestedImi
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                TimeStepper(
                                    currentTime = uiState.input.mealTimestamp,
                                    onTimeChange = onMealTimeChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = TimeStepperDefaults.defaultStyle()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.approx_time_format, time(uiState.input.mealTimestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            ImageCaptionWithSwitch(
                                imageVector = Icons.Default.Notifications,
                                text = stringResource(R.string.meal_correction_bolus_reminder_label),
                                checked = uiState.isMealReminderEnabled,
                                onCheckedChange = { onToggleMealReminder() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Insulin Card (Final Insulin Stepper)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.meal_correction_bolus_insulin_label),
                                style = MaterialTheme.typography.titleMedium
                            )

                            val suggestedBolus = uiState.calculation.proposedTotal
                            var showCalculationDialog by remember { mutableStateOf(false) }

                            if (suggestedBolus.iu >= 0.0) {
                                Spacer(Modifier.height(4.dp))
                                val isSuggestedBolusApplied = abs(uiState.input.manualBolus.iu - suggestedBolus.iu) < 0.01

                                SuggestionBadge(
                                    label = stringResource(
                                        R.string.meal_correction_bolus_suggested_insulin_format,
                                        insulinValue(suggestedBolus.iu)
                                    ),
                                    isHighlighted = !isSuggestedBolusApplied,
                                    onClick = { onManualBolusChange(suggestedBolus.iu) },
                                    onInfoClick = { showCalculationDialog = true }
                                )

                                if (showCalculationDialog) {
                                    CalculationDetailsDialog(
                                        uiState = uiState,
                                        onDismiss = { showCalculationDialog = false },
                                        onApply = {
                                            onManualBolusChange(suggestedBolus.iu)
                                            showCalculationDialog = false
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            EditableValueStepper(
                                currentValue = uiState.input.manualBolus.iu,
                                onValueChange = onManualBolusChange,
                                minValue = BOLUS_MIN,
                                maxValue = BOLUS_MAX,
                                steppingStrategy = ModuloSteppingStrategy(0.1), // 0.1 U steps
                                displayStrategy = object : ValueDisplayStrategy {
                                    override fun format(value: Double): String =
                                        String.format(Locale.getDefault(), "%.2f", value)

                                    override fun color(value: Double): Color = Color.Unspecified
                                },
                                suffix = " ${insulinUnitLabel()}",
                                style = StepperDefaults.defaultStyle()
                            )
                        }
                    }
                }

                // Insulin Plan Card
                if (uiState.input.manualBolus > InsulinAmount.ZERO || uiState.insulinPlan.isNotEmpty()) {
                    InsulinPlanCard(
                        plan = uiState.insulinPlan,
                        onOpenBolusPlanDialog = onOpenBolusPlanDialog
                    )
                }

                if (uiState.isBolusPlanDialogOpen) {
                    BolusPlanEditorDialog(
                        title = stringResource(R.string.bolus_plan_editor_title_planning),
                        administeredInsulinAmount = InsulinAmount.ZERO,
                        plannedBoluses = uiState.insulinPlan.map { item ->
                            PlannedBolusUiModel(
                                amount = item.amount,
                                timestamp = item.timestamp,
                                timeFromMeal = item.timeFromMeal,
                                label = ""
                            )
                        },
                        baseTime = uiState.input.mealTimestamp,
                        onUpdateBolusTime = onUpdateBolusTime,
                        onUpdateBolusAmount = onUpdateBolusAmount,
                        onAddDeferredBolus = onAddDeferredBolus,
                        onRemoveDeferredBolus = onRemoveDeferredBolus,
                        onDismissRequest = onCloseBolusPlanDialog,
                        onConfirm = onCloseBolusPlanDialog
                    )
                }

                // Bottom Button
                val isInputValid = if (uiState.input.carbsKe > 0.0) {
                    uiState.input.selectedMealType != null
                } else {
                    uiState.input.manualBolus > InsulinAmount.ZERO
                }

                PrimaryButton(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.submissionStatus == SubmissionStatus.NotSubmitted && isInputValid
                ) {
                    Text(
                        if (uiState.submissionStatus == SubmissionStatus.Success) stringResource(R.string.meal_correction_bolus_administer_button_submitted)
                        else stringResource(R.string.meal_correction_bolus_administer_button)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun MealCorrectionBolusContextInfo(
    uiState: MealCorrectionBolusUiState,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.projections.isProjected) {
                        Text(
                            text = stringResource(R.string.approx_prefix),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    val displayBgValue = uiState.projections.bg
                    val bgText = glucoseValue(displayBgValue, default = "??")
                    val textColor = if (displayBgValue.isInvalid()) {
                        Color.Gray
                    } else when {
                        displayBgValue.mgdl < 70 -> Red
                        displayBgValue.mgdl < 180 -> LightGreenA700
                        else -> Yellow
                    }
                    Text(
                        text = bgText,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    Text(
                        text = glucoseUnitLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .padding(bottom = 12.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.at_time_format, time(uiState.projections.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.meal_correction_bolus_active_carbs_format, carbsGramsValue(uiState.projections.cob)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.meal_correction_bolus_active_insulin_format, insulinValue(uiState.projections.iob.iu)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.isProjectionsStale) {
                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh_calculations),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CloseScreenBanner(
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Column {
                    Text(
                        text = stringResource(R.string.meal_correction_bolus_close_banner_title),
                        style = MaterialTheme.typography.labelLarge.copy(
                            hyphens = Hyphens.Auto,
                            lineBreak = LineBreak.Paragraph
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.meal_correction_bolus_close_banner_message),
                        style = MaterialTheme.typography.bodySmall.copy(
                            hyphens = Hyphens.Auto,
                            lineBreak = LineBreak.Paragraph
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.meal_correction_bolus_close_banner_button),
                    style = MaterialTheme.typography.labelMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CalculationDetailsDialog(
    uiState: MealCorrectionBolusUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.meal_correction_bolus_calculation_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.meal_correction_bolus_calc_factors_label,
                        isfValue(uiState.isf),
                        crValue(uiState.cr, withUnit = false)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val lowBgProjection = uiState.projections.impendingLow ?:
                    ProjectedBg(uiState.projections.bg, uiState.projections.timestamp)
                if (lowBgProjection.bg.isInvalid()) {
                    Text(
                        text = stringResource(R.string.meal_correction_bolus_calc_no_bg_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (lowBgProjection.bg <= uiState.lowThreshold) {
                    Text(
                        text = stringResource(
                            R.string.meal_correction_bolus_calc_low_bg_warning,
                            glucoseValue(uiState.lowThreshold, withUnit = true),
                            time(lowBgProjection.timestamp)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.meal_correction_bolus_calc_meal_part, insulinValue(uiState.calculation.mealPart.iu, signed = true)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.meal_correction_bolus_calc_correction_part, insulinValue(uiState.calculation.correctionPart.iu, signed = true)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.calculation.iobPart > InsulinAmount.ZERO) {
                        Text(
                            text = stringResource(R.string.meal_correction_bolus_calc_iob_part, insulinValue(-uiState.calculation.iobPart.iu, signed = true)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (uiState.calculation.cobPart > InsulinAmount.ZERO) {
                        Text(
                            text = stringResource(R.string.meal_correction_bolus_calc_cob_part, insulinValue(uiState.calculation.cobPart.iu, signed = true)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.calculation.futureCarbsPart > InsulinAmount.ZERO) {
                        Text(
                            text = stringResource(R.string.meal_correction_bolus_calc_future_carbs_part, insulinValue(uiState.calculation.futureCarbsPart.iu, signed = true)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.calculation.deferredBolusPart > InsulinAmount.ZERO) {
                        Text(
                            text = stringResource(R.string.meal_correction_bolus_calc_deferred_part, insulinValue(-uiState.calculation.deferredBolusPart.iu, signed = true)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                Text(
                    text = stringResource(R.string.meal_correction_bolus_calc_result_label, insulinValue(uiState.calculation.proposedTotal.iu)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text(stringResource(R.string.meal_correction_bolus_apply_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CommonR.string.action_close))
            }
        }
    )
}

@Composable
fun InsulinPlanCard(
    plan: List<PlannedInsulinUiModel>,
    onOpenBolusPlanDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenBolusPlanDialog() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColorBlue.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.meal_correction_bolus_insulin_plan_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val planSummary = if (plan.isEmpty()) {
                    stringResource(R.string.bolus_plan_editor_no_deferred_boluses)
                } else {
                    buildString {
                        plan.forEachIndexed { index, item ->
                            append(insulinValue(item.amount.iu))
                            if (index < plan.size - 1) append(" + ")
                        }

                        val lastOffset = plan.lastOrNull()?.timeFromNow ?: Minutes(0)
                        append(" (")
                        append(withinTimeDescription(lastOffset))
                        append(")")
                    }
                }
                Text(
                    text = planSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onOpenBolusPlanDialog) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(R.string.action_edit))
            }
        }
    }
}

@Composable
fun SuggestionBadge(
    label: String,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        },
        contentColor = if (isHighlighted) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = if (onInfoClick != null) 4.dp else 10.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (onInfoClick != null) {
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onInfoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.meal_correction_bolus_calculation_title),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Loading State")
@Composable
fun MealCorrectionBolusLoadingPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                MealCorrectionBolusContent(
                    uiState = MealCorrectionBolusUiState(
                        isLoading = true,
                    ),
                    onCarbsChange = {},
                    onMealTimeChange = {},
                    onApplySuggestedCarbs = {},
                    onApplySuggestedImi = {},
                    onMealTypeChange = {},
                    onManualBolusChange = {},
                    onOpenBolusPlanDialog = {},
                    onCloseBolusPlanDialog = {},
                    onUpdateBolusTime = { _, _ -> },
                    onUpdateBolusAmount = { _, _ -> },
                    onAddDeferredBolus = {},
                    onRemoveDeferredBolus = {},
                    onToggleMealReminder = {},
                    onRefreshProjections = {},
                    onClose = {},
                    onSubmit = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "0 KE Mode")
@Composable
fun MealCorrectionBolusZeroKePreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                MealCorrectionBolusContent(
                    uiState = MealCorrectionBolusUiState(
                        isLoading = false,
                        suggestedImi = Minutes(10),
                        suggestedCarbsKe = 4.5,
                        input = MealInput(
                            carbsKe = 0.0,
                            manualBolus = InsulinAmount(0.8),
                        ),
                        mealTypes = emptyList(),
                        projections = BolusProjections(
                            bg = BgValue.fromMgDl(140),
                        ),
                        targetBg = BgValue.fromMgDl(100),
                        isf = BgDelta.fromMgDl(50),
                        cr = 10.0,
                        calculation = BolusCalculationDetails(
                            proposedTotal = InsulinAmount(0.8)
                        ),
                        submissionStatus = SubmissionStatus.NotSubmitted
                    ),
                    onCarbsChange = {},
                    onMealTimeChange = {},
                    onApplySuggestedCarbs = {},
                    onApplySuggestedImi = {},
                    onMealTypeChange = {},
                    onManualBolusChange = {},
                    onOpenBolusPlanDialog = {},
                    onCloseBolusPlanDialog = {},
                    onUpdateBolusTime = { _, _ -> },
                    onUpdateBolusAmount = { _, _ -> },
                    onAddDeferredBolus = {},
                    onRemoveDeferredBolus = {},
                    onToggleMealReminder = {},
                    onRefreshProjections = {},
                    onClose = {},
                    onSubmit = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Default Mode")
@Preview(showBackground = true, name = "Default Mode - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MealCorrectionBolusDefaultPreview() {
    val sampleMealTypes = listOf(
        MealType(id = ID_MEAL_FAST, name = "Schnell", components = listOf(CarbCurveComponentData(100, Minutes(30))), cat = Minutes(120)),
        MealType(id = ID_MEAL_STANDARD, name = "Standard", components = listOf(CarbCurveComponentData(100, Minutes(60))), cat = Minutes(180)),
        MealType(id = ID_MEAL_HIGH_FAT, name = "Fettreiches Essen", components = listOf(CarbCurveComponentData(100, Minutes(60))), cat = Minutes(180)),
        MealType(id = ID_MEAL_SLOW, name = "Langsam", components = listOf(CarbCurveComponentData(100, Minutes(90))), cat = Minutes(240)),
    )
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                MealCorrectionBolusContent(
                    uiState = MealCorrectionBolusUiState(
                        isLoading = false,
                        suggestedImi = Minutes(15),
                        suggestedCarbsKe = 3.0,
                        input = MealInput(
                            carbsKe = 4.5,
                            selectedMealType = sampleMealTypes[0],
                            manualBolus = InsulinAmount(5.3),
                            mealTimestamp = Timestamp.now().plusMinutes(15),
                        ),
                        mealTypes = sampleMealTypes,
                        projections = BolusProjections(
                            timestamp = Timestamp.now().plusMinutes(15),
                            bg = BgValue.fromMgDl(145),
                            isProjected = true,
                            iob = InsulinAmount(1.2),
                            cob = 25.0,
                            futureCarbs = 10.0
                        ),
                        targetBg = BgValue.fromMgDl(100),
                        isf = BgDelta.fromMgDl(50),
                        cr = 10.0,
                        calculation = BolusCalculationDetails(
                            mealPart = InsulinAmount(4.5),
                            correctionPart = InsulinAmount(0.8),
                            proposedTotal = InsulinAmount(5.3),
                        ),
                        submissionStatus = SubmissionStatus.NotSubmitted
                    ),
                    onCarbsChange = {},
                    onMealTimeChange = {},
                    onApplySuggestedCarbs = {},
                    onApplySuggestedImi = {},
                    onMealTypeChange = {},
                    onManualBolusChange = {},
                    onOpenBolusPlanDialog = {},
                    onCloseBolusPlanDialog = {},
                    onUpdateBolusTime = { _, _ -> },
                    onUpdateBolusAmount = { _, _ -> },
                    onAddDeferredBolus = {},
                    onRemoveDeferredBolus = {},
                    onToggleMealReminder = {},
                    onRefreshProjections = {},
                    onClose = {},
                    onSubmit = {}
                )
            }
        }
    }
}