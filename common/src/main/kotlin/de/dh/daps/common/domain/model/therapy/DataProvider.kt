package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED

data class DataProvider(
    val id: Long = ID_UNDEFINED,
    val name: String
)