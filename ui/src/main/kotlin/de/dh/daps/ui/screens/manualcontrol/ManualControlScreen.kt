package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.CarbCurveComponentData
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.MealReminder
import de.dh.daps.common.model.MealType
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.aps.TreatmentLock
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.carbsGramsValue
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.LightGreenA700
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.Red
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.Yellow
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.insulinUnitLabel
import de.dh.daps.ui.common.icons.Carbs
import de.dh.daps.ui.common.icons.Insulin
import de.dh.daps.ui.common.icons.Syringe
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.ui.common.time
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ManualControlDialog {
    data class Bolus(
        val initialAmount: Double,
        val includedDeferredBoluses: List<DeferredBolus>? = null,
        val correctionPart: InsulinAmount = InsulinAmount.ZERO,
        val basalPart: InsulinAmount = InsulinAmount.ZERO,
        val recommendationToDismiss: ApsRecommendation.Bolus? = null
    ) : ManualControlDialog

    data class TempBasal(
        val initialPercent: Int,
        val initialDurationHours: Int,
        val recommendationToDismiss: ApsRecommendation.TempBasal? = null
    ) : ManualControlDialog
}

@Composable
fun ManualControlScreen(
    viewModel: ManualControlViewModel,
    treatmentLock: TreatmentLock,
    onNavigateUp: () -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit = {},
    onNavigateToMeals: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeDialog by remember { mutableStateOf<ManualControlDialog?>(null) }

    ManualControlContent(
        uiState = uiState,
        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
        onNavigateToMeals = onNavigateToMeals,
        onOpenBolusDialog = { activeDialog = it },
        onOpenTempBasalDialog = { activeDialog = it },
        onCancelBolus = { viewModel.cancelBolus(treatmentLock) },
        onCancelTempBasal = { viewModel.cancelTempBasal(treatmentLock) }
    )

    when (val dialog = activeDialog) {
        is ManualControlDialog.Bolus -> {
            DeliverBolusDialog(
                dialogData = dialog,
                minBolusAmount = uiState.pump.minBolusAmount,
                maxBolusSize = uiState.pump.maxBolusSize,
                onDismiss = { activeDialog = null },
                onConfirm = { amount, handledDeferredBoluses, correctionPart, basalPart, recommendationToDismiss ->
                    viewModel.deliverBolus(
                        treatmentLock = treatmentLock,
                        amount = amount,
                        handledDeferredBoluses = handledDeferredBoluses,
                        correctionPart = correctionPart,
                        basalPart = basalPart,
                        recommendationToDismiss = recommendationToDismiss
                    )
                    activeDialog = null
                }
            )
        }
        is ManualControlDialog.TempBasal -> {
            SetTempBasalDialog(
                dialogData = dialog,
                onDismiss = { activeDialog = null },
                onConfirm = { durationHours, percent, recommendationToDismiss ->
                    viewModel.setTempBasal(
                        treatmentLock = treatmentLock,
                        durationHours = durationHours,
                        percent = percent,
                        recommendationToDismiss = recommendationToDismiss
                    )
                    activeDialog = null
                }
            )
        }
        null -> {}
    }
}

@Composable
fun ManualControlContextInfo(
    contextInfo: ManualControlContextInfoUiModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayBgValue = contextInfo.lastBgReading?.value ?: BgValue.INVALID
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

            val timestamp = contextInfo.lastBgReading?.timestamp
            val timeText = if (timestamp != null && timestamp.isValid()) {
                stringResource(R.string.at_time_format, time(timestamp))
            } else {
                "--"
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.meal_correction_bolus_active_carbs_format, carbsGramsValue(contextInfo.cob)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.meal_correction_bolus_active_insulin_format, insulinValue(contextInfo.iob.iu)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ManualControlContent(
    uiState: ManualControlUiState,
    onNavigateToMealCorrectionBolus: () -> Unit = {},
    onNavigateToMeals: () -> Unit = {},
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit = {},
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit = {},
    onCancelBolus: () -> Unit = {},
    onCancelTempBasal: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ManualControlContextInfo(contextInfo = uiState.contextInfo)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Recommendations
                ManualControlRecommendationsSection(
                    recommendations = uiState.recommendations,
                    onOpenBolusDialog = onOpenBolusDialog,
                    onOpenTempBasalDialog = onOpenTempBasalDialog,
                    onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus
                )

                // Section 2: Active Meal Reminders
                ManualControlMealRemindersSection(
                    reminders = uiState.activeMealReminders,
                    onNavigateToMeals = onNavigateToMeals
                )

                // Section 3: Meals (Past & Next Planned)
                ManualControlMealsSection(
                    lastPastMeal = uiState.contextInfo.lastPastMeal,
                    nextPlannedMeal = uiState.contextInfo.nextPlannedMeal,
                    onNavigateToMeals = onNavigateToMeals
                )

                // Section 4: Pump Controls (Bolus & Temp-Basal)
                ManualControlPumpControlsSection(
                    pump = uiState.pump,
                    onOpenBolusDialog = onOpenBolusDialog,
                    onCancelBolus = onCancelBolus,
                    onOpenTempBasalDialog = onOpenTempBasalDialog,
                    onCancelTempBasal = onCancelTempBasal
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 1: Recommendations ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlRecommendationsSection(
    recommendations: List<ApsRecommendation>,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.manual_control_recommendations_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (recommendations.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.manual_control_no_recommendations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recommendations.forEach { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    onOpenBolusDialog = onOpenBolusDialog,
                    onOpenTempBasalDialog = onOpenTempBasalDialog,
                    onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: ApsRecommendation,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (recommendation) {
                is ApsRecommendation.Carbs -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_carbs_info_title, recommendation.amountInGram),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.recommendation_carbs_info_text, recommendation.amountInGram),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is ApsRecommendation.Bolus -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Syringe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_bolus_info_title, recommendation.amount.iu),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.recommendation_bolus_info_text, recommendation.amount.iu),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryButton(
                            onClick = {
                                onOpenBolusDialog(
                                    ManualControlDialog.Bolus(
                                        initialAmount = recommendation.amount.iu,
                                        includedDeferredBoluses = recommendation.includedDeferredBoluses,
                                        correctionPart = recommendation.correctionPart,
                                        basalPart = recommendation.basalPart,
                                        recommendationToDismiss = recommendation
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(id = R.string.manual_control_apply_recommendation))
                        }
                    }
                }

                is ApsRecommendation.TempBasal -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_temp_basal_info_title, recommendation.percent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.recommendation_temp_basal_info_text, recommendation.percent, recommendation.durationInHours),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            onClick = {
                                onOpenTempBasalDialog(
                                    ManualControlDialog.TempBasal(
                                        initialPercent = recommendation.percent,
                                        initialDurationHours = recommendation.durationInHours,
                                        recommendationToDismiss = recommendation
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(id = R.string.manual_control_apply_recommendation))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 2: Active Meal Reminders ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlMealRemindersSection(
    reminders: List<MealReminder>,
    onNavigateToMeals: () -> Unit
) {
    if (reminders.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.meal_correction_bolus_reminder_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        reminders.forEach { reminder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToMeals),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = reminder.description.ifBlank { stringResource(R.string.notification_meal_reminder_title) },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.at_time_format, time(reminder.reminderTimestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 3: Meals (Past & Next Planned) ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlMealsSection(
    lastPastMeal: MealEntry?,
    nextPlannedMeal: MealEntry?,
    onNavigateToMeals: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.manual_control_meals_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            NormalTextButton(onClick = onNavigateToMeals) {
                Text(stringResource(id = R.string.meals_screen_title))
            }
        }

        // Last Past Meal
        MealInfoCard(
            title = stringResource(id = R.string.manual_control_past_meal_title),
            icon = Icons.Filled.Carbs,
            mealEntry = lastPastMeal,
            emptyText = stringResource(id = R.string.manual_control_no_past_meal),
            onClick = onNavigateToMeals
        )

        // Next Planned Meal
        MealInfoCard(
            title = stringResource(id = R.string.manual_control_next_meal_title),
            icon = Icons.Default.Schedule,
            mealEntry = nextPlannedMeal,
            emptyText = stringResource(id = R.string.manual_control_no_next_meal),
            onClick = onNavigateToMeals
        )
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.getDefault())
    return Instant.ofEpochMilli(timestamp.ms)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

@Composable
private fun MealInfoCard(
    title: String,
    icon: ImageVector,
    mealEntry: MealEntry?,
    emptyText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (mealEntry != null) {
                Text(
                    text = "%.0f g Kohlenhydrate (%s)".format(mealEntry.carbGrams, mealEntry.mealType.name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(mealEntry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mealEntry.description.isNotBlank()) {
                    Text(
                        text = mealEntry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 4: Pump Controls ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlPumpControlsSection(
    pump: ManualControlPumpUiModel,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onCancelBolus: () -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onCancelTempBasal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.manual_control_pump_controls_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Card 1: Bolus
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Insulin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.manual_control_bolus_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val activeBolus = pump.bolusStatus
                if (activeBolus?.state == BolusDeliveryState.DELIVERING) {
                    Text(
                        text = stringResource(
                            id = R.string.manual_control_bolus_delivering,
                            activeBolus.deliveredAmount.iu,
                            activeBolus.targetAmount.iu,
                            activeBolus.progressPercent
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { activeBolus.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NormalButton(
                        onClick = onCancelBolus,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SoftRed
                        ),
                        border = BorderStroke(1.dp, SoftRed)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_cancel_bolus))
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.manual_control_no_active_bolus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        onClick = {
                            onOpenBolusDialog(
                                ManualControlDialog.Bolus(
                                    initialAmount = pump.minBolusAmount.iu.coerceAtLeast(1.0)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Outlined.Syringe, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_deliver_bolus))
                    }
                }
            }
        }

        // Card 2: Temp Basal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.manual_control_temp_basal_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val activeBasal = pump.basalStatus
                if (activeBasal?.isTempBasal == true) {
                    val percent = activeBasal.tempBasalPercent ?: 100
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.manual_control_temp_basal_active, percent),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NormalButton(
                        onClick = onCancelTempBasal,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SoftRed
                        ),
                        border = BorderStroke(1.dp, SoftRed)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_cancel_temp_basal))
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.manual_control_no_active_temp_basal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        onClick = {
                            onOpenTempBasalDialog(
                                ManualControlDialog.TempBasal(
                                    initialPercent = 100,
                                    initialDurationHours = 1
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_set_temp_basal))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Dialogs ---
// -----------------------------------------------------------------------------------------

@Composable
private fun DeliverBolusDialog(
    dialogData: ManualControlDialog.Bolus,
    minBolusAmount: InsulinAmount,
    maxBolusSize: InsulinAmount,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>?,
        correctionPart: InsulinAmount,
        basalPart: InsulinAmount,
        recommendationToDismiss: ApsRecommendation.Bolus?
    ) -> Unit
) {
    var amountState by remember { mutableDoubleStateOf(dialogData.initialAmount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.manual_control_dialog_deliver_bolus_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dialogData.includedDeferredBoluses?.takeIf { it.isNotEmpty() }?.let { deferredList ->
                    val totalDeferred = deferredList.sumOf { it.amount.iu }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.manual_control_deferred_boluses_info,
                                deferredList.size,
                                totalDeferred
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                EditableValueStepper(
                    currentValue = amountState,
                    onValueChange = { amountState = it },
                    minValue = minBolusAmount.iu,
                    maxValue = maxBolusSize.iu,
                    steppingStrategy = DefaultSteppingStrategy(step = minBolusAmount.iu),
                    displayStrategy = ConfigurableDisplayStrategy(suffix = " ${insulinUnitLabel()}"),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = {
                    onConfirm(
                        InsulinAmount(amountState),
                        dialogData.includedDeferredBoluses,
                        dialogData.correctionPart,
                        dialogData.basalPart,
                        dialogData.recommendationToDismiss
                    )
                }
            ) {
                Text(stringResource(id = R.string.manual_control_dialog_confirm_deliver))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.manual_control_dialog_cancel))
            }
        }
    )
}

@Composable
private fun SetTempBasalDialog(
    dialogData: ManualControlDialog.TempBasal,
    onDismiss: () -> Unit,
    onConfirm: (durationHours: Int, percent: Int, recommendationToDismiss: ApsRecommendation.TempBasal?) -> Unit
) {
    var percent by remember { mutableIntStateOf(dialogData.initialPercent) }
    var durationHours by remember { mutableIntStateOf(dialogData.initialDurationHours) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.manual_control_dialog_set_temp_basal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.manual_control_rate_percent_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EditableValueStepper(
                    currentValue = percent.toDouble(),
                    onValueChange = { percent = it.toInt().coerceIn(0, 500) },
                    minValue = 0.0,
                    maxValue = 500.0,
                    steppingStrategy = DefaultSteppingStrategy(step = 10.0),
                    displayStrategy = ConfigurableDisplayStrategy(suffix = "%"),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 50, 80, 120, 150, 200).forEach { preset ->
                        AssistChip(
                            onClick = { percent = preset },
                            label = { Text("%d%%".format(preset)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.manual_control_duration_hours_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EditableValueStepper(
                    currentValue = durationHours.toDouble(),
                    onValueChange = { durationHours = it.toInt().coerceAtLeast(1) },
                    minValue = 1.0,
                    maxValue = 24.0,
                    steppingStrategy = DefaultSteppingStrategy(step = 1.0),
                    displayStrategy = ConfigurableDisplayStrategy(suffix = " Std"),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            SecondaryButton(
                onClick = {
                    onConfirm(durationHours, percent, dialogData.recommendationToDismiss)
                }
            ) {
                Text(stringResource(id = R.string.manual_control_dialog_confirm_set))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.manual_control_dialog_cancel))
            }
        }
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun ManualControlScreenPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                ManualControlContent(
                    uiState = ManualControlUiState(
                        contextInfo = ManualControlContextInfoUiModel(
                            lastBgReading = BgReading(
                                value = BgValue.fromMgDl(125),
                                sampleKind = BgSampleKind.Value,
                                timestamp = Timestamp.now()
                            ),
                            iob = InsulinAmount(1.2),
                            cob = 25.0,
                            lastPastMeal = MealEntry(
                                id = 1L,
                                timestamp = Timestamp.now().minusHours(2),
                                carbGrams = 45.0,
                                mealType = MealType(
                                    name = "Mittagessen",
                                    components = listOf(CarbCurveComponentData(100, Minutes(30))),
                                    cat = Minutes(180)
                                ),
                                description = "Pasta mit Tomatensauce",
                                administeredInsulinAmount = InsulinAmount(3.5)
                            ),
                            nextPlannedMeal = MealEntry(
                                id = 2L,
                                timestamp = Timestamp.now().plusHours(3),
                                carbGrams = 60.0,
                                mealType = MealType(
                                    name = "Abendessen",
                                    components = listOf(CarbCurveComponentData(100, Minutes(30))),
                                    cat = Minutes(180)
                                ),
                                description = "Pizza"
                            )
                        ),
                        recommendations = listOf(
                            ApsRecommendation.Bolus(
                                amount = InsulinAmount(1.5),
                                correctionPart = InsulinAmount(1.0),
                                basalPart = InsulinAmount(0.5)
                            ),
                            ApsRecommendation.TempBasal(durationInHours = 2, percent = 120)
                        ),
                        activeMealReminders = listOf(
                            MealReminder(
                                id = 10L,
                                mealTimestamp = Timestamp.now().plusMinutes(15),
                                description = "Snack nach dem Sport"
                            )
                        ),
                        pump = ManualControlPumpUiModel(
                            isConnected = true
                        )
                    )
                )
            }
        }
    }
}