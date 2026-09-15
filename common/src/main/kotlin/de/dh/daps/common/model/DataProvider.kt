package de.dh.daps.common.model

data class DataProvider(
    var id: Long = ID_UNDEFINED,
    val name: String,
    val type: String
)