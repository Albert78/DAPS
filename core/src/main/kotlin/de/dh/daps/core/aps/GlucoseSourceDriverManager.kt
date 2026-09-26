package de.dh.daps.core.aps

import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.GlucoseSourceDriver

/**
 * Central registry and manager for registered [GlucoseSourceDriver] plugins.
 * Facilitates retrieving available drivers and reconnecting using a stored [GlucoseSourceConnectionDescriptor].
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
     * Re-establishes a glucose source connection using the provided [descriptor].
     */
    suspend fun connect(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        val driver = getDriver(descriptor.driverId)
            ?: return Result.failure(
                IllegalArgumentException("No glucose source driver found matching driver ID '${descriptor.driverId}'.")
            )
        return driver.connect(descriptor)
    }
}