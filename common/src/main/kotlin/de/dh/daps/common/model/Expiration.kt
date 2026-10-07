package de.dh.daps.common.model

import de.dh.daps.common.model.data.Timestamp
import java.time.LocalDate

sealed interface ReplaceableComponentType {
    data object Sensor : ReplaceableComponentType
    data object Cannula : ReplaceableComponentType
    data object Patch : ReplaceableComponentType
}

sealed interface ExpirationDate {
    data class Approximate(
        val date: LocalDate
    ) : ExpirationDate

    data class Hard(
        val dateTime: Timestamp
    ) : ExpirationDate
}

data class Expiration(
    val type: ReplaceableComponentType,
    val date: ExpirationDate,
    val graceUntil: Timestamp? = null
)