package de.dh.daps.common.model

import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.Flow

enum class GlucoseSourceStatus {
    Ok,
    Expired,
    Error
}

/**
 * Represents a source for blood glucose data.
 * Provides a stream of [BgReading]s and metadata about the sensor and its update frequency.
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

    val status: Flow<GlucoseSourceStatus>

    /**
     * Gets a stable, technical name for the sensor, e.g. "Dexcom_G7".
     * This is used to identify the sensor in the database.
     */
    fun getSensorTypeName(): String
    fun getValues(): Flow<BgReading>

    fun start()
    fun stop()
}