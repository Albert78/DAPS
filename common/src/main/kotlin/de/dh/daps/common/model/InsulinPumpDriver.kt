package de.dh.daps.common.model

import androidx.compose.runtime.Composable
import de.dh.daps.common.ui.UiText

/**
 * Driver interface for insulin pump plugins.
 * Encapsulates driver-specific scanning, pairing, and connection workflows.
 */
interface InsulinPumpDriver {
    /**
     * Unique identifier for this driver plugin (e.g. "de.dh.daps.plugin.ypso").
     */
    val driverId: String

    /**
     * Human-readable display name of the pump type or manufacturer (e.g. "Ypsomed YpsoPump").
     */
    val driverDisplayName: UiText

    /**
     * Renders the driver's custom UI workflow for initial setup, device scanning, and pairing.
     * When pairing succeeds, [onConnected] is invoked with the active [InsulinPump] instance
     * and its persistable [PumpConnectionDescriptor].
     */
    @Composable
    fun SetupScreen(
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
        onCancel: () -> Unit,
    )

    /**
     * Re-establishes a connection with a previously paired pump using its stored [descriptor].
     * Invoked upon application launch or when loading a configured pump.
     */
    suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump>
}