package de.dh.daps.common.model.data

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.model.ApsMode

/**
 * Data model for the current system settings.
 * This is separate from therapy-specific configuration.
 */
data class CurrentSettings(
    var id: Long = ID_UNDEFINED,
    val apsMode: ApsMode = ApsMode.ApsSuspended
)