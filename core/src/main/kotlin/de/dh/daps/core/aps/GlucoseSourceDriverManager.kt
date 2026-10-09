package de.dh.daps.core.aps

import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver

/**
 * Central registry and manager for registered [GlucoseSourceDriver] plugins.
 * Facilitates retrieving available drivers and reconnecting active drivers.
 */
class GlucoseSourceDriverManager(
    val drivers: List<GlucoseSourceDriver> = emptyList(),
) {
    /**
     * Retrieves a driver by its unique [driverId].
     */
    fun getDriver(driverId: String): GlucoseSourceDriver? {
        return drivers.find { it.driverId == driverId }
    }

    /**
     * Re-establishes a glucose source connection for the specified [driverId].
     */
    suspend fun connect(driverId: String): Result<GlucoseSource> {
        val driver = getDriver(driverId)
            ?: return Result.failure(
                IllegalArgumentException("No glucose source driver found matching driver ID '$driverId'.")
            )
        return driver.connect()
    }
}