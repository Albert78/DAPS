package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.getDefaultStandardMealType
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.ui.R
import de.dh.daps.ui.common.carbsValue
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.getBolusRecommendationText
import de.dh.daps.ui.common.icons.Icon_Meal_Fast
import de.dh.daps.ui.common.icons.Icon_Temp_Basal
import de.dh.daps.ui.common.icons.Syringe
import de.dh.daps.ui.common.theme.AppPreview

@Composable
fun ManualControlRecommendationsSection(
    recommendations: List<ApsRecommendation>,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onNavigateToMealCorrectionBolus: (Double?) -> Unit,
    onDismissRecommendation: (ApsRecommendation) -> Unit = {}
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
                    onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
                    onDismissRecommendation = onDismissRecommendation
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: ApsRecommendation,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onNavigateToMealCorrectionBolus: (Double?) -> Unit,
    onDismissRecommendation: (ApsRecommendation) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (recommendation) {
                is ApsRecommendation.Carbs -> {
                    val carbText = carbsValue(recommendation.amountInGram.toDouble())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                text = stringResource(R.string.recommendation_carbs_info_title, carbText),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onDismissRecommendation(recommendation) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.cd_manual_control_dismiss_recommendation),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recommendation_carbs_info_text, carbText),
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
                                val carbsInG = recommendation.amountInGram.toDouble()
                                onNavigateToMealCorrectionBolus(carbsInG)
                            }
                        ) {
                            Text(stringResource(id = R.string.manual_control_apply_recommendation))
                        }
                    }
                }

                is ApsRecommendation.Bolus -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                        IconButton(
                            onClick = { onDismissRecommendation(recommendation) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.cd_manual_control_dismiss_recommendation),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getBolusRecommendationText(recommendation),
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
                                        initialAmount = recommendation.amount,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icon_Temp_Basal,
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
                        IconButton(
                            onClick = { onDismissRecommendation(recommendation) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.cd_manual_control_dismiss_recommendation),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

@Preview(showBackground = true, name = "Recommendations - All Types")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Recommendations - Dark Mode")
@Composable
fun ManualControlRecommendationsPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        ManualControlRecommendationsSection(
            recommendations = listOf(
                ApsRecommendation.Carbs(amountInGram = 20),
                ApsRecommendation.Bolus(
                    amount = InsulinAmount(2.5),
                    correctionPart = InsulinAmount(1.5),
                    basalPart = InsulinAmount(1.0)
                ),
                ApsRecommendation.TempBasal(durationInHours = 2, percent = 150)
            ),
            onOpenBolusDialog = {},
            onOpenTempBasalDialog = {},
            onNavigateToMealCorrectionBolus = {}
        )
    }
}

@Preview(showBackground = true, name = "Recommendation - Carbs")
@Composable
fun RecommendationCarbsPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        RecommendationCard(
            recommendation = ApsRecommendation.Carbs(amountInGram = 25),
            onOpenBolusDialog = {},
            onOpenTempBasalDialog = {},
            onNavigateToMealCorrectionBolus = {}
        )
    }
}

@Preview(showBackground = true, name = "Recommendation - Bolus")
@Composable
fun RecommendationBolusPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        RecommendationCard(
            recommendation = ApsRecommendation.Bolus(
                amount = InsulinAmount(1.8),
                correctionPart = InsulinAmount(1.8)
            ),
            onOpenBolusDialog = {},
            onOpenTempBasalDialog = {},
            onNavigateToMealCorrectionBolus = {}
        )
    }
}

@Preview(showBackground = true, name = "Recommendation - Deferred Bolus")
@Composable
fun RecommendationDeferredBolusPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        RecommendationCard(
            recommendation = ApsRecommendation.Bolus(
                amount = InsulinAmount(1.8),
                includedDeferredBoluses = listOf(
                    DeferredBolus(
                        amount = InsulinAmount(1.8),
                        timestamp = Timestamp.now()
                    )
                ),
                associatedMeal = MealEntry(
                    timestamp = Timestamp.now().minusMinutes(30),
                    carbGrams = 30.0,
                    mealType = getDefaultStandardMealType(LocalContext.current)
                ),
                correctionPart = InsulinAmount(1.8)
            ),
            onOpenBolusDialog = {},
            onOpenTempBasalDialog = {},
            onNavigateToMealCorrectionBolus = {}
        )
    }
}

@Preview(showBackground = true, name = "Recommendation - Temp Basal")
@Composable
fun RecommendationTempBasalPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        RecommendationCard(
            recommendation = ApsRecommendation.TempBasal(durationInHours = 1, percent = 0),
            onOpenBolusDialog = {},
            onOpenTempBasalDialog = {},
            onNavigateToMealCorrectionBolus = {}
        )
    }
}