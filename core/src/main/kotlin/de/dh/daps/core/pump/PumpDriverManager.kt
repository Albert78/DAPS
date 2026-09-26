package de.dh.daps.core.pump

import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PumpConnectionDescriptor

/**
 * Central registry and manager for registered [InsulinPumpDriver] plugins.
 * Facilitates retrieving available drivers and reconnecting using a stored [PumpConnectionDescriptor].
 */
class PumpDriverManager(
    val drivers: List<InsulinPumpDriver> = emptyList(),
) {
    /**
     * Retrieves a driver by its unique [driverId].
     */
    fun getDriver(driverId: String): InsulinPumpDriver? {
        return drivers.find { it.driverId == driverId }
    }

    /**
     * Re-establishes a pump connection using the provided [descriptor].
     */
    suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        val driver = getDriver(descriptor.driverId)
            ?: return Result.failure(
                IllegalArgumentException("No pump driver found matching driver ID '${descriptor.driverId}'.")
            )
        return driver.connect(descriptor)
    }
}