package de.dh.daps.core.orchestration

import de.dh.daps.common.domain.model.therapy.DeferredBolus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.MealEntry

/**
 * Recommendations for manual treatments, Open-Loop decisions, and emergency carbs.
 */
sealed class ApsRecommendation {
    data class Carbs(val amountInGram: Int) : ApsRecommendation()

    data class Bolus(
        val amount: InsulinAmount,
        val includedDeferredBoluses: List<DeferredBolus>? = null,
        val associatedMeal: MealEntry? = null,
        val correctionPart: InsulinAmount = InsulinAmount.ZERO,
        val basalPart: InsulinAmount = InsulinAmount.ZERO
    ) : ApsRecommendation()

    data class TempBasal(
        val durationInHours: Int,
        val percent: Int
    ) : ApsRecommendation()
}