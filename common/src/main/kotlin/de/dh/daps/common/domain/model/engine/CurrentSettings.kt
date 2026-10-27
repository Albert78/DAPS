package de.dh.daps.common.domain.model.engine

import de.dh.daps.common.ID_UNDEFINED

/**
 * Data model for the current system settings.
 * This is separate from therapy-specific configuration.
 */
data class CurrentSettings(
    var id: Long = ID_UNDEFINED,
    val apsMode: ApsMode = ApsMode.Suspend
)