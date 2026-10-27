package de.dh.daps.ui.controls.meal

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount

/**
 * UI representation of a deferred bolus entry planned for a meal.
 */
data class PlannedBolusUiModel(
    val id: Long = ID_UNDEFINED,
    val amount: InsulinAmount,
    val timestamp: Timestamp,
    val timeFromMeal: Minutes = Minutes(0),
    val label: String = ""
)