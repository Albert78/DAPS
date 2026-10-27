package de.dh.daps.core.orchestration

/**
 * Holds at most one active recommendation for each recommendation type.
 */
data class ActiveRecommendations(
    val carbs: ApsRecommendation.Carbs? = null,
    val bolus: ApsRecommendation.Bolus? = null,
    val tempBasal: ApsRecommendation.TempBasal? = null
) {
    /**
     * List of all active recommendations.
     */
    val all: List<ApsRecommendation>
        get() = listOfNotNull(carbs, bolus, tempBasal)

    val isEmpty: Boolean
        get() = carbs == null && bolus == null && tempBasal == null

    val isNotEmpty: Boolean
        get() = !isEmpty
}