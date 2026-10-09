package de.dh.daps.common.model

import de.dh.daps.common.ui.UiText

/**
 * Represents the current setup and lifecycle state of a hardware device driver.
 */
sealed class DeviceLifecycleState {
    /**
     * No device setup or configuration has been initiated for this driver.
     */
    data object NoDevice : DeviceLifecycleState()

    /**
     * Setup flow has been started or interrupted, but is not yet fully completed.
     */
    data object SetupInProgress : DeviceLifecycleState()

    /**
     * Device setup is completed and ready for operational connection.
     */
    data class DeviceBound(
        /**
         * Optional device id to be shown in the UI.
         */
        val deviceId: String?,
        val displayName: UiText
    ) : DeviceLifecycleState()
}