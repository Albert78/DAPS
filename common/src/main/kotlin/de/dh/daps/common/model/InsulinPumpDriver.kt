package de.dh.daps.common.model

import androidx.compose.runtime.Composable
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.StateFlow

/**
 * Driver interface for insulin pump plugins.
 * Encapsulates driver-specific scanning, pairing, and binding workflows.
 *
 * Drivers must persist their bound device information in their [PluginPreferences]
 * (provided via [PluginContext]) and restore their [DeviceLifecycleState] and [activePump]
 * when [initState] is invoked.
 */
interface InsulinPumpDriver {
    /**
     * Unique identifier for this driver plugin (e.g. "de.dh.daps.plugin.dana").
     */
    val driverId: String

    /**
     * Human-readable display name of the pump type or manufacturer (e.g. "Sooil Dana-i").
     */
    val displayName: UiText

    /**
     * Current setup and lifecycle state reported by this driver.
     */
    val lifecycleState: StateFlow<DeviceLifecycleState>

    /**
     * Flow emitting the currently active [InsulinPump] instance provided by this driver,
     * or null if no device is connected/bound.
     */
    val activePump: StateFlow<InsulinPump?>

    /**
     * Restores the internal lifecycle state of the driver (e.g. checking persisted preferences,
     * restoring [lifecycleState] and instantiating [activePump] if a device is configured).
     */
    suspend fun initState()

    /**
     * Renders the driver's custom UI workflow for initial setup, device scanning, and pairing.
     * When the driver is explicitly reset, canceled, or unbound by the user, [onDriverReset] is invoked
     * to notify the host container to restore the driver selection list.
     */
    @Composable
    fun SetupScreen(
        onDriverReset: () -> Unit,
    )

    /**
     * Returns optional Composable UI content to display in the Pump tab of the System Control screen for [pump],
     * or null if this driver provides no custom control section.
     */
    fun getPumpTabSection(
        pump: InsulinPump,
    ): (@Composable () -> Unit)? = null

    /**
     * Returns optional Composable UI content for driver-specific settings for [pump],
     * or null if this driver provides no custom settings screen.
     * When displayed, [onClose] is invoked when the user dismisses the settings view.
     */
    fun getSettingsScreen(
        pump: InsulinPump,
        onClose: () -> Unit,
    ): (@Composable () -> Unit)? = null
}