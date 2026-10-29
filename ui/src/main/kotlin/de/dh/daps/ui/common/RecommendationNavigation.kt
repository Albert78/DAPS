package de.dh.daps.ui.common

import de.dh.daps.common.navigation.ManualControlInitialDialog
import de.dh.daps.core.orchestration.ApsRecommendation
import de.dh.daps.core.orchestration.RecommendationTargetAction
import de.dh.daps.core.orchestration.toTargetAction

/**
 * Executes the navigation action associated with the given APS recommendation.
 */
fun executeRecommendationAction(
    recommendation: ApsRecommendation,
    onNavigateToMealCorrectionBolus: () -> Unit,
    onNavigateToManualControl: (ManualControlInitialDialog) -> Unit
) {
    when (recommendation.toTargetAction()) {
        RecommendationTargetAction.OpenMealScreen -> onNavigateToMealCorrectionBolus()
        RecommendationTargetAction.OpenBolusDialog -> onNavigateToManualControl(ManualControlInitialDialog.BOLUS)
        RecommendationTargetAction.OpenTempBasalDialog -> onNavigateToManualControl(ManualControlInitialDialog.TEMP_BASAL)
    }
}