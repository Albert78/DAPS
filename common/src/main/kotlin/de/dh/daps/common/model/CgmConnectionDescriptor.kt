package de.dh.daps.common.model

import kotlinx.serialization.Serializable

/**
 * Unique, persistable description of a blood glucose / CGM source connection.
 * Can be stored by DAPS (e.g. DataStore or JSON) and reloaded on app startup
 * to re-establish a connection with a glucose data source.
 */
@Serializable
data class CgmConnectionDescriptor(
    /**
     * Unique ID of the driver handling this CGM / glucose source type (e.g. "de.dh.daps.plugin.glucose.receiver").
     */
    val driverId: String,

    /**
     * Unique source or transmitter identifier (e.g. transmitter ID, receiver type, or sensor serial number).
     */
    val sourceId: String,

    /**
     * Display name of the glucose source shown to the user (e.g. "Dexcom G6 (8G1234)").
     */
    val displayName: String,

    /**
     * Driver-specific connection parameters (e.g. API keys, receiver configurations, channel settings).
     */
    val connectionParameters: Map<String, String> = emptyMap(),
)