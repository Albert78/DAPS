package de.dh.daps.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.ui.R

/**
 * Returns the formatted detail text for a bolus recommendation depending on whether
 * deferred boluses and an associated meal are present.
 */
@Composable
fun getBolusRecommendationText(recommendation: ApsRecommendation.Bolus): String {
    val hasDeferred = !recommendation.includedDeferredBoluses.isNullOrEmpty()
    return if (hasDeferred) {
        val meal = recommendation.associatedMeal
        if (meal != null) {
            val carbText = carbsValue(meal.carbGrams)
            val timeText = timeWithUnit(meal.timestamp)
            stringResource(
                R.string.recommendation_bolus_deferred_info_text,
                recommendation.amount.iu,
                timeText,
                carbText
            )
        } else {
            stringResource(
                R.string.recommendation_bolus_deferred_info_text_no_meal,
                recommendation.amount.iu
            )
        }
    } else {
        stringResource(
            R.string.recommendation_bolus_info_text,
            recommendation.amount.iu
        )
    }
}