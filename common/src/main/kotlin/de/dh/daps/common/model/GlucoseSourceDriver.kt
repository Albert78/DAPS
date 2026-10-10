package de.dh.daps.common.model

import androidx.compose.runtime.Composable
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.StateFlow

/**
 * Driver interface for CGM / blood glucose source plugins.
 * Encapsulates driver-specific scanning, transmitter setup, and binding workflows.
 *
 * Drivers must persist their bound device information in their [PluginPreferences]
 * (provided via [PluginContext]) and must be able to restore their [DeviceLifecycleState] and their
 * state to re-establish the connection when [attach] is invoked.
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
     * Renders the driver's custom UI workflow for initial setup, transmitter pairing, or configuration.
     * When setup finishes or is cancelled, [onNavigateUp] is invoked to return to the parent screen.
     */
    @Composable
    fun SetupScreen(
        onNavigateUp: () -> Unit,
    )

    /**
     * Re-establishes a connection with a previously configured glucose source using stored driver settings.
     * Invoked upon application launch or when loading a configured glucose source.
     */
    suspend fun attach(): Result<GlucoseSource>
}