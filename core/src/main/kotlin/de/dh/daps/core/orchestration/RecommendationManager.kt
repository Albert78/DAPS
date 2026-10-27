package de.dh.daps.core.orchestration

import de.dh.daps.common.domain.model.therapy.DeferredBolus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.MealEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Central manager for treatment recommendations in the APS system.
 *
 * Manages:
 * - Emergency carbs recommendations
 * - Core decisions for Open-Loop mode (Temp-Basal, Bolus)
 */
class RecommendationManager {
    private val _recommendations = MutableStateFlow(ActiveRecommendations())
    val recommendations: StateFlow<ActiveRecommendations> = _recommendations.asStateFlow()

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
        _recommendations.update {
            it.copy(carbs = ApsRecommendation.Carbs(amountInGram))
        }
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
        _recommendations.update {
            it.copy(
                bolus = ApsRecommendation.Bolus(
                    amount = roundedAmount,
                    includedDeferredBoluses = includedDeferredBoluses,
                    associatedMeal = associatedMeal,
                    correctionPart = correctionPart.roundToTwoDecimals(),
                    basalPart = basalPart.roundToTwoDecimals()
                )
            )
        }
    }

    /**
     * Records a recommendation for temp basal rate in Open-Loop mode.
     */
    fun addTempBasalRecommendation(durationInHours: Int, percent: Int) {
        _recommendations.update {
            it.copy(
                tempBasal = ApsRecommendation.TempBasal(
                    durationInHours = durationInHours,
                    percent = percent
                )
            )
        }
    }

    /**
     * Clears any active temp basal recommendation.
     */
    fun clearTempBasalRecommendation() {
        _recommendations.update { it.copy(tempBasal = null) }
    }

    /**
     * Clears all active treatment recommendations.
     */
    fun clearRecommendations() {
        _recommendations.value = ActiveRecommendations()
    }

    /**
     * Removes a specific recommendation.
     */
    fun removeRecommendation(recommendation: ApsRecommendation) {
        _recommendations.update { current ->
            when (recommendation) {
                is ApsRecommendation.Carbs -> current.copy(carbs = null)
                is ApsRecommendation.Bolus -> current.copy(bolus = null)
                is ApsRecommendation.TempBasal -> current.copy(tempBasal = null)
            }
        }
    }

    companion object {
        val TAG = RecommendationManager::class.simpleName
    }
}