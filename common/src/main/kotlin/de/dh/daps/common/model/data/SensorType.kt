package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED

data class SensorType(
    var id: Long = ID_UNDEFINED,
    val name: String
)