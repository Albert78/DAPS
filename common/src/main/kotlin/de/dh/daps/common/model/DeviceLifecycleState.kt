package de.dh.daps.common.model

import kotlinx.serialization.Serializable

/**
 * Represents the current setup and lifecycle state of a hardware device driver.
 */
@Serializable
sealed class DeviceLifecycleState {
    /**
     * No device setup or configuration has been initiated for this driver.
     */
    @Serializable
    data object Unconfigured : DeviceLifecycleState()

    /**
     * Setup flow has been started or interrupted, but is not yet fully completed.
     */
    @Serializable
    data class SetupInProgress(
        val stepName: String? = null
    ) : DeviceLifecycleState()

    /**
     * Device setup is completed and ready for operational connection.
     */
    @Serializable
    data class Configured(
        val deviceId: String,
        val displayName: String
    ) : DeviceLifecycleState()
}