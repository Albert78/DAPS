package de.dh.daps.core.aps

import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount

/**
 * Recommendations for manual treatments, Open-Loop decisions, and emergency carbs.
 */
sealed class ApsRecommendation {
    data class Carbs(val amountInGram: Int) : ApsRecommendation()

    data class Bolus(
        val amount: InsulinAmount,
        val handledDeferredBoluses: List<DeferredBolus>? = null,
        val correctionPart: InsulinAmount = InsulinAmount.ZERO,
        val basalPart: InsulinAmount = InsulinAmount.ZERO
    ) : ApsRecommendation()

    data class TempBasal(
        val durationInHours: Int,
        val percent: Int
    ) : ApsRecommendation()
}