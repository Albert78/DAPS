package de.dh.daps.ui.screens.openloop

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.CarbCurveComponentData
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.MealType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Carbs
import de.dh.daps.ui.common.icons.Insulin
import de.dh.daps.ui.common.icons.Syringe
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.SoftRed
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.dh.daps.common.R as CommonR

@Composable
fun OpenLoopScreen(
    viewModel: OpenLoopViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit = {},
    onNavigateToMeals: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    OpenLoopContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
        onNavigateToMeals = onNavigateToMeals,
        onApplyBolusRecommendation = { viewModel.applyBolusRecommendation(it) },
        onApplyTempBasalRecommendation = { viewModel.applyTempBasalRecommendation(it) },
        onRemoveRecommendation = { viewModel.removeRecommendation(it) },
        onClearAllRecommendations = { viewModel.clearAllRecommendations() },
        onUpdateBolusAmount = { viewModel.updateManualBolusAmount(it) },
        onDeliverBolus = { viewModel.deliverBolus() },
        onCancelBolus = { viewModel.cancelBolus() },
        onUpdateTempBasalPercent = { viewModel.updateManualTempBasalPercent(it) },
        onUpdateTempBasalDuration = { viewModel.updateManualTempBasalDuration(it) },
        onSetTempBasal = { viewModel.setTempBasal() },
        onCancelTempBasal = { viewModel.cancelTempBasal() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenLoopContent(
    uiState: OpenLoopUiState,
    onNavigateUp: () -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit = {},
    onNavigateToMeals: () -> Unit = {},
    onApplyBolusRecommendation: (ApsRecommendation.Bolus) -> Unit = {},
    onApplyTempBasalRecommendation: (ApsRecommendation.TempBasal) -> Unit = {},
    onRemoveRecommendation: (ApsRecommendation) -> Unit = {},
    onClearAllRecommendations: () -> Unit = {},
    onUpdateBolusAmount: (Double) -> Unit = {},
    onDeliverBolus: () -> Unit = {},
    onCancelBolus: () -> Unit = {},
    onUpdateTempBasalPercent: (Int) -> Unit = {},
    onUpdateTempBasalDuration: (Int) -> Unit = {},
    onSetTempBasal: () -> Unit = {},
    onCancelTempBasal: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.open_loop_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up),
                        )
                    }
                },
                actions = {
                    if (uiState.recommendations.isNotEmpty()) {
                        IconButton(onClick = onClearAllRecommendations) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = stringResource(id = R.string.open_loop_clear_all_recommendations)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Recommendations
            OpenLoopRecommendationsSection(
                recommendations = uiState.recommendations,
                onApplyBolusRecommendation = onApplyBolusRecommendation,
                onApplyTempBasalRecommendation = onApplyTempBasalRecommendation,
                onRemoveRecommendation = onRemoveRecommendation,
                onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus
            )

            // Section 2: Meals (Past & Next Planned)
            OpenLoopMealsSection(
                lastPastMeal = uiState.lastPastMeal,
                nextPlannedMeal = uiState.nextPlannedMeal,
                onNavigateToMeals = onNavigateToMeals
            )

            // Section 3: Pump Controls (Bolus & Temp-Basal)
            OpenLoopPumpControlsSection(
                uiState = uiState,
                onUpdateBolusAmount = onUpdateBolusAmount,
                onDeliverBolus = onDeliverBolus,
                onCancelBolus = onCancelBolus,
                onUpdateTempBasalPercent = onUpdateTempBasalPercent,
                onUpdateTempBasalDuration = onUpdateTempBasalDuration,
                onSetTempBasal = onSetTempBasal,
                onCancelTempBasal = onCancelTempBasal
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 1: Recommendations ---
// -----------------------------------------------------------------------------------------

@Composable
private fun OpenLoopRecommendationsSection(
    recommendations: List<ApsRecommendation>,
    onApplyBolusRecommendation: (ApsRecommendation.Bolus) -> Unit,
    onApplyTempBasalRecommendation: (ApsRecommendation.TempBasal) -> Unit,
    onRemoveRecommendation: (ApsRecommendation) -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.open_loop_recommendations_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (recommendations.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.open_loop_no_recommendations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recommendations.forEach { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    onApplyBolusRecommendation = onApplyBolusRecommendation,
                    onApplyTempBasalRecommendation = onApplyTempBasalRecommendation,
                    onRemoveRecommendation = onRemoveRecommendation,
                    onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: ApsRecommendation,
    onApplyBolusRecommendation: (ApsRecommendation.Bolus) -> Unit,
    onApplyTempBasalRecommendation: (ApsRecommendation.TempBasal) -> Unit,
    onRemoveRecommendation: (ApsRecommendation) -> Unit,
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NormalTextButton(onClick = { onRemoveRecommendation(recommendation) }) {
                            Text(stringResource(id = R.string.open_loop_dismiss_recommendation))
                        }
                    }
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
                        NormalTextButton(onClick = { onRemoveRecommendation(recommendation) }) {
                            Text(stringResource(id = R.string.open_loop_dismiss_recommendation))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PrimaryButton(
                            onClick = {
                                onApplyBolusRecommendation(recommendation)
                                onRemoveRecommendation(recommendation)
                            }
                        ) {
                            Text(stringResource(id = R.string.open_loop_apply_recommendation))
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
                        NormalTextButton(onClick = { onRemoveRecommendation(recommendation) }) {
                            Text(stringResource(id = R.string.open_loop_dismiss_recommendation))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SecondaryButton(
                            onClick = {
                                onApplyTempBasalRecommendation(recommendation)
                                onRemoveRecommendation(recommendation)
                            }
                        ) {
                            Text(stringResource(id = R.string.open_loop_apply_recommendation))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 2: Meals (Past & Next Planned) ---
// -----------------------------------------------------------------------------------------

@Composable
private fun OpenLoopMealsSection(
    lastPastMeal: MealEntry?,
    nextPlannedMeal: PlannedMealItem?,
    onNavigateToMeals: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.open_loop_meals_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            NormalTextButton(onClick = onNavigateToMeals) {
                Text(stringResource(id = R.string.meals_screen_title))
            }
        }

        // Last Past Meal
        MealInfoCard(
            title = stringResource(id = R.string.open_loop_past_meal_title),
            icon = Icons.Filled.Carbs,
            mealItem = lastPastMeal?.let { MealDisplayData.FromEntry(it) },
            emptyText = stringResource(id = R.string.open_loop_no_past_meal),
            onClick = onNavigateToMeals
        )

        // Next Planned Meal
        MealInfoCard(
            title = stringResource(id = R.string.open_loop_next_meal_title),
            icon = Icons.Default.Schedule,
            mealItem = nextPlannedMeal?.let { MealDisplayData.FromPlanned(it) },
            emptyText = stringResource(id = R.string.open_loop_no_next_meal),
            onClick = onNavigateToMeals
        )
    }
}

private sealed interface MealDisplayData {
    val timeFormatted: String
    val mainText: String
    val subText: String?

    data class FromEntry(val meal: MealEntry) : MealDisplayData {
        override val timeFormatted: String = formatTimestamp(meal.timestamp)
        override val mainText: String = "%.0f g Kohlenhydrate (%s)".format(meal.carbGrams, meal.mealType.name)
        override val subText: String? = meal.description.ifBlank { null }
    }

    data class FromPlanned(val item: PlannedMealItem) : MealDisplayData {
        override val timeFormatted: String = formatTimestamp(item.timestamp)
        override val mainText: String = when (item) {
            is PlannedMealItem.Entry -> "%.0f g Kohlenhydrate (%s)".format(item.meal.carbGrams, item.meal.mealType.name)
            is PlannedMealItem.Reminder -> item.reminder.description.ifBlank { "Geplante Mahlzeit" }
        }
        override val subText: String? = when (item) {
            is PlannedMealItem.Entry -> item.meal.description.ifBlank { null }
            is PlannedMealItem.Reminder -> null
        }
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
    mealItem: MealDisplayData?,
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
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (mealItem == null) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mealItem.mainText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        mealItem.subText?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = mealItem.timeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- Section 3: Pump Controls (Bolus & Temp-Basal) ---
// -----------------------------------------------------------------------------------------

@Composable
private fun OpenLoopPumpControlsSection(
    uiState: OpenLoopUiState,
    onUpdateBolusAmount: (Double) -> Unit,
    onDeliverBolus: () -> Unit,
    onCancelBolus: () -> Unit,
    onUpdateTempBasalPercent: (Int) -> Unit,
    onUpdateTempBasalDuration: (Int) -> Unit,
    onSetTempBasal: () -> Unit,
    onCancelTempBasal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.open_loop_pump_controls_title),
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
                        text = stringResource(id = R.string.open_loop_bolus_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val activeBolus = uiState.activeBolusStatus
                if (activeBolus?.state == BolusDeliveryState.DELIVERING) {
                    Text(
                        text = stringResource(
                            id = R.string.open_loop_bolus_delivering,
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
                        Text(stringResource(id = R.string.open_loop_cancel_bolus))
                    }
                } else {
                    EditableValueStepper(
                        currentValue = uiState.manualBolusAmount,
                        onValueChange = onUpdateBolusAmount,
                        minValue = uiState.minBolusAmount.iu,
                        maxValue = uiState.maxBolusSize.iu,
                        steppingStrategy = DefaultSteppingStrategy(step = 0.5),
                        displayStrategy = ConfigurableDisplayStrategy(suffix = " E"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5, 1.0, 2.0, 3.0, 5.0).forEach { preset ->
                            AssistChip(
                                onClick = { onUpdateBolusAmount(preset) },
                                label = { Text("+%.1f E".format(preset)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PrimaryButton(
                        onClick = onDeliverBolus,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.manualBolusAmount > 0.0
                    ) {
                        Icon(imageVector = Icons.Outlined.Syringe, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.open_loop_deliver_bolus))
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
                        text = stringResource(id = R.string.open_loop_temp_basal_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val activeBasal = uiState.activeBasalStatus
                if (activeBasal?.isTempBasal == true) {
                    val percent = activeBasal.tempBasalPercent ?: 100
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.open_loop_temp_basal_active, percent),
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
                        Text(stringResource(id = R.string.open_loop_cancel_temp_basal))
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.open_loop_rate_percent_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EditableValueStepper(
                        currentValue = uiState.manualTempBasalPercent.toDouble(),
                        onValueChange = { onUpdateTempBasalPercent(it.toInt()) },
                        minValue = 0.0,
                        maxValue = 200.0,
                        steppingStrategy = DefaultSteppingStrategy(step = 10.0),
                        displayStrategy = ConfigurableDisplayStrategy(suffix = " %"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 50, 80, 120, 150, 200).forEach { preset ->
                            AssistChip(
                                onClick = { onUpdateTempBasalPercent(preset) },
                                label = { Text("%d%%".format(preset)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.open_loop_duration_hours_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EditableValueStepper(
                        currentValue = uiState.manualTempBasalDurationHours.toDouble(),
                        onValueChange = { onUpdateTempBasalDuration(it.toInt().coerceAtLeast(1)) },
                        minValue = 1.0,
                        maxValue = 24.0,
                        steppingStrategy = DefaultSteppingStrategy(step = 1.0),
                        displayStrategy = ConfigurableDisplayStrategy(suffix = " Std"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryButton(
                        onClick = onSetTempBasal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.open_loop_set_temp_basal))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun OpenLoopScreenPreview() {
    AppTheme {
        OpenLoopContent(
            uiState = OpenLoopUiState(
                recommendations = listOf(
                    ApsRecommendation.Bolus(
                        amount = InsulinAmount(1.5),
                        correctionPart = InsulinAmount(1.0),
                        basalPart = InsulinAmount(0.5)
                    ),
                    ApsRecommendation.TempBasal(durationInHours = 2, percent = 120)
                ),
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
                nextPlannedMeal = PlannedMealItem.Entry(
                    MealEntry(
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
                isPumpConnected = true,
                manualBolusAmount = 1.5,
                manualTempBasalPercent = 120,
                manualTempBasalDurationHours = 2
            ),
            onNavigateUp = {}
        )
    }
}