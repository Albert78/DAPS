package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.getDefaultSlowMealType
import de.dh.daps.common.model.getDefaultStandardMealType
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.carbsGramsValue
import de.dh.daps.ui.common.carbsKeValue
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.LightGreenA700
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.Red
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.Yellow
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.icons.Icon_Meal_Fast
import de.dh.daps.ui.common.icons.Syringe
import de.dh.daps.ui.common.insulinUnitLabel
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.ui.common.time
import de.dh.daps.ui.common.timeWithUnit
import de.dh.daps.ui.screens.mealtypes.MealTypeIcon
import de.dh.daps.common.R as CommonR

sealed interface ManualControlDialog {
    data class Bolus(
        val initialAmount: Double,
        val includedDeferredBoluses: List<DeferredBolus>? = null,
        val correctionPart: InsulinAmount = InsulinAmount.ZERO,
        val basalPart: InsulinAmount = InsulinAmount.ZERO,
        val recommendationToDismiss: ApsRecommendation.Bolus? = null,
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
    onNavigateUp: () -> Unit,
    onNavigateToMealCorrectionBolus: (Double?) -> Unit = {},
    onEditMeal: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ManualControlContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
        onEditMeal = onEditMeal,
        onDeliverBolus = { amount, handledDeferredBoluses, correctionPart, basalPart, recommendationToDismiss ->
            viewModel.deliverBolus(
                amount = amount,
                handledDeferredBoluses = handledDeferredBoluses,
                correctionPart = correctionPart,
                basalPart = basalPart,
                recommendationToDismiss = recommendationToDismiss
            )
        },
        onSetTempBasal = { durationHours, percent, recommendationToDismiss ->
            viewModel.setTempBasal(
                durationHours = durationHours,
                percent = percent,
                recommendationToDismiss = recommendationToDismiss
            )
        },
        onCancelBolus = { viewModel.cancelBolus() },
        onCancelTempBasal = { viewModel.cancelTempBasal() },
        onDismissLockError = { viewModel.dismissLockError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlContent(
    uiState: ManualControlUiState,
    onNavigateUp: () -> Unit = {},
    onNavigateToMealCorrectionBolus: (Double?) -> Unit = {},
    onEditMeal: (Long) -> Unit = {},
    onDeliverBolus: (
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>?,
        correctionPart: InsulinAmount,
        basalPart: InsulinAmount,
        recommendationToDismiss: ApsRecommendation.Bolus?
    ) -> Unit = { _, _, _, _, _ -> },
    onSetTempBasal: (
        durationHours: Int,
        percent: Int,
        recommendationToDismiss: ApsRecommendation.TempBasal?
    ) -> Unit = { _, _, _ -> },
    onCancelBolus: () -> Unit = {},
    onCancelTempBasal: () -> Unit = {},
    onDismissLockError: () -> Unit = {}
) {
    var activeDialog by remember { mutableStateOf<ManualControlDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.manual_control_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Context Info Card
                ManualControlContextInfo(contextInfo = uiState.contextInfo)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Meals Overview
                    ManualControlMealsSection(
                        lastPastMeal = uiState.contextInfo.lastPastMeal,
                        nextPlannedMeal = uiState.contextInfo.nextPlannedMeal,
                        hasNextPlannedMealReminder = uiState.contextInfo.hasNextPlannedMealReminder,
                        onEditMeal = onEditMeal
                    )

                    // Recommendations
                    ManualControlRecommendationsSection(
                        recommendations = uiState.recommendations,
                        onOpenBolusDialog = { activeDialog = it },
                        onOpenTempBasalDialog = { activeDialog = it },
                        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus
                    )

                    // Manual Pump Control
                    ManualControlPumpControlsSection(
                        pump = uiState.pump,
                        onOpenBolusDialog = { activeDialog = it },
                        onCancelBolus = onCancelBolus,
                        onOpenTempBasalDialog = { activeDialog = it },
                        onCancelTempBasal = onCancelTempBasal
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        is ManualControlDialog.Bolus -> {
            DeliverBolusDialog(
                dialogData = dialog,
                minBolusAmount = uiState.pump.minBolusAmount,
                maxBolusSize = uiState.pump.maxBolusSize,
                onDismiss = { activeDialog = null },
                onConfirm = { amount, handledDeferredBoluses, correctionPart, basalPart, recommendationToDismiss ->
                    onDeliverBolus(
                        amount,
                        handledDeferredBoluses,
                        correctionPart,
                        basalPart,
                        recommendationToDismiss
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
                    onSetTempBasal(
                        durationHours,
                        percent,
                        recommendationToDismiss
                    )
                    activeDialog = null
                }
            )
        }
        null -> {}
    }

    if (uiState.showLockError) {
        AlertDialog(
            onDismissRequest = onDismissLockError,
            title = { Text(stringResource(id = R.string.core_issue_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.treatment_lock_error_message,
                        uiState.lockErrorOwner ?: stringResource(id = R.string.manual_control_screen_title)
                    )
                )
            },
            confirmButton = {
                PrimaryButton(onClick = onDismissLockError) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// --- Header: Context info ---
// -----------------------------------------------------------------------------------------

@Composable
fun ManualControlContextInfo(
    contextInfo: ManualControlContextInfoUiModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(bottom = 10.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "COB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = carbsGramsValue(contextInfo.cob),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "IOB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = insulinValue(contextInfo.iob.iu),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section: Meals ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlMealsSection(
    lastPastMeal: MealEntry?,
    nextPlannedMeal: MealEntry?,
    hasNextPlannedMealReminder: Boolean = false,
    onEditMeal: (Long) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(id = R.string.manual_control_meals_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Last Past Meal (Compact)
            CompactMealInfoCard(
                title = stringResource(id = R.string.manual_control_past_meal_title),
                mealEntry = lastPastMeal,
                emptyText = stringResource(id = R.string.manual_control_no_past_meal, ManualControlViewModel.PAST_MEAL_LOOKBACK_HOURS),
                modifier = Modifier.weight(1f),
                onClick = lastPastMeal?.let { meal -> { onEditMeal(meal.id) } }
            )

            // Next Planned Meal (Compact)
            CompactMealInfoCard(
                title = stringResource(id = R.string.manual_control_next_meal_title),
                mealEntry = nextPlannedMeal,
                emptyText = stringResource(id = R.string.manual_control_no_next_meal),
                hasReminder = hasNextPlannedMealReminder,
                modifier = Modifier.weight(1f),
                onClick = nextPlannedMeal?.let { meal -> { onEditMeal(meal.id) } }
            )
        }
    }
}

@Composable
private fun CompactMealInfoCard(
    title: String,
    mealEntry: MealEntry?,
    emptyText: String,
    modifier: Modifier = Modifier,
    hasReminder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    OutlinedCard(
        onClick = { onClick?.invoke() },
        enabled = mealEntry != null && onClick != null,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasReminder && mealEntry != null) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(id = R.string.meal_correction_bolus_reminder_label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (mealEntry != null) {
                val keValue = mealEntry.carbGrams / 10.0
                val keText = carbsKeValue(keValue)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = keText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        MealTypeIcon(
                            mealType = mealEntry.mealType,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = timeWithUnit(mealEntry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mealEntry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mealEntry.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section: Recommendations ---
// -----------------------------------------------------------------------------------------

@Composable
private fun ManualControlRecommendationsSection(
    recommendations: List<ApsRecommendation>,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onNavigateToMealCorrectionBolus: (Double?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.manual_control_recommendations_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (recommendations.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = recommendations.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (recommendations.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(id = R.string.manual_control_no_recommendations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
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
    onNavigateToMealCorrectionBolus: (Double?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (recommendation) {
                is ApsRecommendation.Carbs -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icon_Meal_Fast,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_carbs_info_title, recommendation.amountInGram),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recommendation_carbs_info_text, recommendation.amountInGram),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        PrimaryButton(
                            onClick = {
                                val carbsKe = recommendation.amountInGram / 10.0
                                onNavigateToMealCorrectionBolus(carbsKe)
                            }
                        ) {
                            Text(stringResource(id = R.string.manual_control_apply_recommendation))
                        }
                    }
                }

                is ApsRecommendation.Bolus -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Syringe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_bolus_info_title, recommendation.amount.iu),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recommendation_bolus_info_text, recommendation.amount.iu),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.recommendation_temp_basal_info_title, recommendation.percent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recommendation_temp_basal_info_text, recommendation.percent, recommendation.durationInHours),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
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
// --- Section: Pump Controls ---
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Card 1: Bolus (Primary Color Scheme - Cool Blue tone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Syringe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.manual_control_bolus_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Schnellwirksame Einmalgabe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Bolus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
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
                    Spacer(modifier = Modifier.height(14.dp))
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

        // Card 2: Basal (Tertiary Color Scheme - Warm Amber/Peach tone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val activeBasal = pump.basalStatus
                val isTempActive = activeBasal?.isTempBasal == true

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.manual_control_basal_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Laufende Hintergrundabgabe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isTempActive) "Temp-Basal" else "Basal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeBasal != null) {
                    val activeRateFormatted = insulinValue(activeBasal.activeRate.iu)
                    if (activeBasal.isSuspended) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Basalabgabe ist unterbrochen (Suspended)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onOpenTempBasalDialog(
                                    ManualControlDialog.TempBasal(
                                        initialPercent = 100,
                                        initialDurationHours = 1
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.manual_control_set_temp_basal))
                        }
                    } else if (activeBasal.isTempBasal) {
                        val percent = activeBasal.tempBasalPercent ?: 100
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(id = R.string.manual_control_temp_basal_active, percent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Aktuelle Rate: $activeRateFormatted I.E./h",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
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
                            text = stringResource(id = R.string.manual_control_normal_basal_active, activeRateFormatted),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onOpenTempBasalDialog(
                                    ManualControlDialog.TempBasal(
                                        initialPercent = 100,
                                        initialDurationHours = 1
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.manual_control_set_temp_basal))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.manual_control_no_active_temp_basal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onOpenTempBasalDialog(
                                ManualControlDialog.TempBasal(
                                    initialPercent = 100,
                                    initialDurationHours = 1
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
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

@Preview(showBackground = true, name = "Pump Controls Preview")
@Composable
fun ManualControlPumpControlsPreview() {
    AppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ManualControlPumpControlsSection(
                pump = ManualControlPumpUiModel(isConnected = true),
                onOpenBolusDialog = {},
                onCancelBolus = {},
                onOpenTempBasalDialog = {},
                onCancelTempBasal = {}
            )
        }
    }
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
                                value = BgValue.fromMgDl(80),
                                sampleKind = BgSampleKind.Value,
                                timestamp = Timestamp.now()
                            ),
                            iob = InsulinAmount(1.2),
                            cob = 25.0,
                            lastPastMeal = MealEntry(
                                id = 1L,
                                timestamp = Timestamp.now().minusHours(2),
                                carbGrams = 45.0,
                                mealType = getDefaultStandardMealType(LocalContext.current),
                                administeredInsulinAmount = InsulinAmount(3.5)
                            ),
                            nextPlannedMeal = MealEntry(
                                id = 2L,
                                timestamp = Timestamp.now().plusHours(3),
                                carbGrams = 60.0,
                                mealType = getDefaultSlowMealType(LocalContext.current),
                                description = "Pizza"
                            ),
                            hasNextPlannedMealReminder = true
                        ),
                        recommendations = listOf(
                            ApsRecommendation.Bolus(
                                amount = InsulinAmount(1.5),
                                correctionPart = InsulinAmount(1.0),
                                basalPart = InsulinAmount(0.0)
                            ),
                            ApsRecommendation.TempBasal(durationInHours = 2, percent = 80)
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