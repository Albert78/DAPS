package de.dh.daps.ui.screens.recommendations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.data.CarbsUnit
import de.dh.daps.core.orchestration.ActiveRecommendations
import de.dh.daps.core.orchestration.ApsRecommendation
import de.dh.daps.ui.R as UiR
import de.dh.daps.common.R as CommonR
import de.dh.daps.ui.common.formatCarbsValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    recommendations: ActiveRecommendations,
    carbsUnit: CarbsUnit,
    onApplyRecommendation: (ApsRecommendation) -> Unit,
    onDismissRecommendation: (ApsRecommendation) -> Unit,
    onDismissAll: () -> Unit
) {
    val context = LocalContext.current
    val carbs = recommendations.carbs
    val bolus = recommendations.bolus
    val tempBasal = recommendations.tempBasal

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(UiR.string.recommendation_notification_channel_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = onDismissAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(CommonR.string.action_close))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (carbs != null) {
                RecommendationCard(
                    title = stringResource(UiR.string.recommendation_title_carbs),
                    description = stringResource(
                        UiR.string.recommendation_text_carbs,
                        formatCarbsValue(carbs.amountInGram.toDouble(), carbsUnit, context)
                    ),
                    onApply = { onApplyRecommendation(carbs) },
                    onDismiss = { onDismissRecommendation(carbs) }
                )
            }

            if (bolus != null) {
                val hasDeferredPart = !bolus.includedDeferredBoluses.isNullOrEmpty()
                val title = if (hasDeferredPart) {
                    stringResource(UiR.string.notification_deferred_bolus_title)
                } else {
                    stringResource(UiR.string.recommendation_title_bolus)
                }
                val description = if (hasDeferredPart) {
                    stringResource(UiR.string.notification_deferred_bolus_text, bolus.amount.iu)
                } else {
                    stringResource(UiR.string.recommendation_text_bolus, bolus.amount.iu)
                }

                RecommendationCard(
                    title = title,
                    description = description,
                    onApply = { onApplyRecommendation(bolus) },
                    onDismiss = { onDismissRecommendation(bolus) }
                )
            }

            if (tempBasal != null) {
                RecommendationCard(
                    title = stringResource(UiR.string.recommendation_title_temp_basal),
                    description = stringResource(
                        UiR.string.recommendation_text_temp_basal,
                        tempBasal.percent,
                        tempBasal.durationInHours
                    ),
                    onApply = { onApplyRecommendation(tempBasal) },
                    onDismiss = { onDismissRecommendation(tempBasal) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecommendationCard(
    title: String,
    description: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(CommonR.string.action_cancel))
                }

                Button(
                    onClick = onApply
                ) {
                    Text(stringResource(UiR.string.manual_control_apply_recommendation))
                }
            }
        }
    }
}