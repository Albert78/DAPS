package de.dh.daps.common.model

import kotlinx.serialization.Serializable

/**
 * Unique, persistable description of a pump connection.
 * Can be stored by DAPS (e.g. DataStore or JSON) and reloaded on app startup
 * to re-establish a connection with an insulin pump.
 */
@Serializable
data class PumpConnectionDescriptor(
    /**
     * Unique ID of the driver handling this pump type (e.g. "de.dh.daps.plugin.ypso").
     */
    val driverId: String,

    /**
     * Unique hardware or device identifier (e.g. Bluetooth MAC address or hardware serial number).
     */
    val deviceId: String,

    /**
     * Display name of the pump device shown to the user (e.g. "Dana-i 12345678").
     */
    val displayName: String,

    /**
     * Driver-specific connection parameters (e.g. tokens, pairing keys, channel configurations).
     */
    val connectionParameters: Map<String, String> = emptyMap(),
)