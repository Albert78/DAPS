package de.dh.daps.common.model

import androidx.compose.runtime.Composable
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.StateFlow

/**
 * Driver interface for CGM / blood glucose source plugins.
 * Encapsulates driver-specific scanning, transmitter setup, and binding workflows.
 *
 * Drivers must persist their bound device information in their [PluginPreferences]
 * (provided via [PluginContext]) and restore their [DeviceLifecycleState] and [activeGlucoseSource]
 * when [initState] is invoked.
 */
interface GlucoseSourceDriver {
    /**
     * Unique identifier for this driver plugin (e.g. "de.dh.daps.plugin.glucose.receiver").
     */
    val driverId: String

    /**
     * Human-readable display name of the glucose source type or manufacturer (e.g. "xDrip+ Receiver").
     */
    val displayName: UiText

    /**
     * Current setup and lifecycle state reported by this driver.
     */
    val lifecycleState: StateFlow<DeviceLifecycleState>

    /**
     * Flow emitting the currently active [GlucoseSource] instance provided by this driver,
     * or null if no device is connected/bound.
     */
    val activeGlucoseSource: StateFlow<GlucoseSource?>

    /**
     * Restores the internal lifecycle state of the driver (e.g. checking persisted preferences,
     * restoring [lifecycleState] and instantiating [activeGlucoseSource] if a device is configured).
     */
    suspend fun initState()

    /**
     * Renders the driver's custom UI workflow for initial setup, transmitter pairing, or configuration.
     * When the driver is explicitly reset, cancelled, or unbound by the user, [onDriverReset] is invoked
     * to notify the host container to restore the driver selection list.
     */
    @Composable
    fun SetupScreen(
        onDriverReset: () -> Unit,
    )
}