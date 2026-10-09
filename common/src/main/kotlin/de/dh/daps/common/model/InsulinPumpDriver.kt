package de.dh.daps.common.model

import androidx.compose.runtime.Composable
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.StateFlow

/**
 * Driver interface for insulin pump plugins.
 * Encapsulates driver-specific scanning, pairing, and connection workflows.
 *
 * Drivers must persist their connected device information in their [PluginPreferences]
 * (provided via [PluginContext]) and must be able to restore their [DeviceLifecycleState] and their
 * state to re-establish the connection when [connect] is invoked.
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
     * Renders the driver's custom UI workflow for initial setup, device scanning, and pairing.
     * When pairing succeeds, [onConnected] is invoked with the active [InsulinPump] instance.
     */
    @Composable
    fun SetupScreen(
        onConnected: (InsulinPump) -> Unit,
        onCancel: () -> Unit,
    )

    /**
     * Re-establishes a connection with a previously configured pump using stored driver settings.
     * Invoked upon application launch or when loading a configured pump.
     */
    suspend fun connect(): Result<InsulinPump>
}