package de.dh.daps.common.model

import androidx.compose.runtime.Composable

/**
 * Driver interface for CGM / blood glucose source plugins.
 * Encapsulates driver-specific scanning, transmitter setup, and connection workflows.
 */
interface CgmDriver : Plugin {
    /**
     * Unique identifier for this driver plugin (e.g. "de.dh.daps.plugin.glucose.receiver").
     */
    val driverId: String

    /**
     * Human-readable display name of the glucose source type or manufacturer (e.g. "xDrip+ Receiver").
     */
    val displayName: String

    /**
     * Renders the driver's custom UI workflow for initial setup, transmitter pairing, or configuration.
     * When setup succeeds, [onConnected] is invoked with the active [GlucoseSource] instance
     * and its persistable [CgmConnectionDescriptor].
     */
    @Composable
    fun SetupScreen(
        onConnected: (GlucoseSource, CgmConnectionDescriptor) -> Unit,
        onCancel: () -> Unit,
    )

    /**
     * Re-establishes a connection with a previously configured glucose source using its stored [descriptor].
     * Invoked upon application launch or when loading a configured glucose source.
     */
    suspend fun connect(descriptor: CgmConnectionDescriptor): Result<GlucoseSource>
}