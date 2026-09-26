package de.dh.daps.common.model

import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.Flow

/**
 * Represents a source for blood glucose data.
 * Provides a stream of [BgReading]s and metadata about the sensor and its update frequency.
 */
interface GlucoseSource {
    val glucoseSourceName: UiText
    val dataProviderType: String
    val readingsInterval: BgReadingsInterval

    /**
     * Gets the expected time delay the readings (and readings timestamp)
     * are behind blood glucose.
     */
    val readingsTimeDelay: Minutes
    fun getSensorTypeName(): String
    fun getValues(): Flow<BgReading>

    fun start()
    fun stop()
}