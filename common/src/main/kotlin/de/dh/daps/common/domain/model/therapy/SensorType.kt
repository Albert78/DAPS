package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED

data class SensorType(
    var id: Long = ID_UNDEFINED,
    val name: String
)