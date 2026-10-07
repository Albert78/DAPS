package de.dh.daps.common.model

import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manufacturer and hardware information about a glucose source.
 */
data class SourceHardwareInformation(
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
)

enum class GlucoseSourceStatus {
    Ok,
    Expired,
    Error
}

/**
 * Represents a source for blood glucose data.
 * Provides a stream of [BgReading]s and metadata about the sensor and its update frequency.
 * To support glucose source specific commands to the user, this object can implement [GlucoseSourcePluginUiProvider].
 */
interface GlucoseSource {
    /**
     * Gets a stable, technical name for the data provider (e.g. "Dexcom_Source_Plugin").
     * This is used to identify the data provider in the database.
     */
    val glucoseSourceId: String

    /**
     * Human-readable display name of the glucose source.
     */
    val sourceDisplayName: UiText

    val readingsInterval: BgReadingsInterval

    /**
     * Gets the expected time delay the readings (and readings timestamp)
     * are behind blood glucose.
     */
    val readingsTimeDelay: Minutes

    val status: StateFlow<GlucoseSourceStatus>

    /**
     * Activation / insertion start timestamp of the glucose source, or null if unknown or not applicable.
     */
    val startDate: StateFlow<Timestamp?>

    /**
     * Scheduled expiration / replacement end timestamp of the glucose source, or null if unknown or not applicable.
     */
    val endDate: StateFlow<Timestamp?>

    /**
     * Flag indicating whether the glucose source is completely expired and no longer functioning.
     */
    val isExpired: StateFlow<Boolean>

    val lastConnection: StateFlow<Timestamp?>

    /**
     * Gets a stable, technical name for the sensor, e.g. "Dexcom_G7".
     * This is used to identify the sensor in the database.
     */
    val sensorType: StateFlow<String>

    val hardwareInformation: StateFlow<SourceHardwareInformation?>

    fun getValues(): Flow<BgReading>

    /**
     * Starts the values flow.
     */
    fun start()

    /**
     * Stops the values flow.
     */
    fun stop()
}