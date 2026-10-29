package de.dh.daps.core.alerts

import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.RecommendationType
import de.dh.daps.core.orchestration.ApsRecommendation

/**
 * Extension function to resolve the effective [AlertSignalConfig] for an [ApsRecommendation].
 */
fun AlertProfile.getConfigFor(recommendation: ApsRecommendation): AlertSignalConfig {
    val type = when (recommendation) {
        is ApsRecommendation.Carbs -> RecommendationType.CARBS
        is ApsRecommendation.Bolus -> RecommendationType.BOLUS
        is ApsRecommendation.TempBasal -> RecommendationType.TEMP_BASAL
    }
    return getConfigFor(type)
}