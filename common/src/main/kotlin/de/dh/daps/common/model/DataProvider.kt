package de.dh.daps.common.model

import de.dh.daps.common.ID_UNDEFINED

data class DataProvider(
    var id: Long = ID_UNDEFINED,
    val name: String
)