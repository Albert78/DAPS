package de.dh.daps.core.aps

import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central manager for treatment recommendations in the APS system.
 *
 * Manages:
 * - Emergency carbs recommendations
 * - Core decisions for Open-Loop mode (Temp-Basal, Bolus)
 */
class RecommendationManager {

    private val _recommendations = MutableStateFlow<List<ApsRecommendation>>(emptyList())
    val recommendations: StateFlow<List<ApsRecommendation>> = _recommendations.asStateFlow()

    /**
     * Clears all active treatment recommendations.
     */
    fun stop() {
        clearRecommendations()
    }

    // -----------------------------------------------------------------------------------------
    // --- Treatment Recommendations ---
    // -----------------------------------------------------------------------------------------

    /**
     * Records a recommendation for emergency carb intake.
     */
    fun addCarbsRecommendation(amountInGram: Int) {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.Carbs } + ApsRecommendation.Carbs(amountInGram)
    }

    /**
     * Records a recommendation for bolus delivery in Open-Loop mode.
     */
    fun addBolusRecommendation(
        amount: InsulinAmount,
        includedDeferredBoluses: List<DeferredBolus>? = null,
        associatedMeal: MealEntry? = null,
        correctionPart: InsulinAmount = InsulinAmount.ZERO,
        basalPart: InsulinAmount = InsulinAmount.ZERO
    ) {
        val roundedAmount = amount.roundToTwoDecimals()
        if (roundedAmount <= InsulinAmount.EPSILON) return
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.Bolus } + ApsRecommendation.Bolus(
            amount = roundedAmount,
            includedDeferredBoluses = includedDeferredBoluses,
            associatedMeal = associatedMeal,
            correctionPart = correctionPart.roundToTwoDecimals(),
            basalPart = basalPart.roundToTwoDecimals()
        )
    }

    /**
     * Records a recommendation for temp basal rate in Open-Loop mode.
     */
    fun addTempBasalRecommendation(durationInHours: Int, percent: Int) {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.TempBasal } + ApsRecommendation.TempBasal(
            durationInHours = durationInHours,
            percent = percent
        )
    }

    /**
     * Clears any active temp basal recommendation.
     */
    fun clearTempBasalRecommendation() {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.TempBasal }
    }

    /**
     * Clears all active treatment recommendations.
     */
    fun clearRecommendations() {
        _recommendations.value = emptyList()
    }

    /**
     * Removes a specific recommendation from the list.
     */
    fun removeRecommendation(recommendation: ApsRecommendation) {
        _recommendations.value = _recommendations.value - recommendation
    }

    companion object {
        val TAG = RecommendationManager::class.simpleName
    }
}