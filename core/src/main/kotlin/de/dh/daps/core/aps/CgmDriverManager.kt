package de.dh.daps.core.aps

import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.CgmDriver
import de.dh.daps.common.model.GlucoseSource

/**
 * Central registry and manager for registered [CgmDriver] plugins.
 * Facilitates retrieving available drivers and reconnecting using a stored [CgmConnectionDescriptor].
 */
class CgmDriverManager(
    val drivers: List<CgmDriver> = emptyList(),
) {
    /**
     * Retrieves a driver by its unique [driverId].
     */
    fun getDriver(driverId: String): CgmDriver? {
        return drivers.find { it.driverId == driverId }
    }

    /**
     * Re-establishes a glucose source connection using the provided [descriptor].
     */
    suspend fun connect(descriptor: CgmConnectionDescriptor): Result<GlucoseSource> {
        val driver = getDriver(descriptor.driverId)
            ?: return Result.failure(
                IllegalArgumentException("No CGM driver found matching driver ID '${descriptor.driverId}'.")
            )
        return driver.connect(descriptor)
    }
}