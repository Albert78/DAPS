package de.dh.daps.core.orchestration

sealed interface RecommendationTargetAction {
    /** Opens the meal logging screen (automatically pulls the active Carbs recommendation). */
    data object OpenMealScreen : RecommendationTargetAction

    /** Opens the bolus dialog in manual control (automatically pulls the active Bolus recommendation). */
    data object OpenBolusDialog : RecommendationTargetAction

    /** Opens the temporary basal rate dialog in manual control (automatically pulls the active TempBasal recommendation). */
    data object OpenTempBasalDialog : RecommendationTargetAction
}

/** Deterministically maps a specific recommendation to its target action. */
fun ApsRecommendation.toTargetAction(): RecommendationTargetAction = when (this) {
    is ApsRecommendation.Carbs -> RecommendationTargetAction.OpenMealScreen
    is ApsRecommendation.Bolus -> RecommendationTargetAction.OpenBolusDialog
    is ApsRecommendation.TempBasal -> RecommendationTargetAction.OpenTempBasalDialog
}