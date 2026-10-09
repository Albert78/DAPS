package de.dh.daps.core.pump

import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver

/**
 * Central registry and manager for registered [InsulinPumpDriver] plugins.
 * Facilitates retrieving available drivers and reconnecting active drivers.
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
     * Re-establishes a pump connection for the specified [driverId].
     */
    suspend fun connect(driverId: String): Result<InsulinPump> {
        val driver = getDriver(driverId)
            ?: return Result.failure(
                IllegalArgumentException("No pump driver found matching driver ID '$driverId'.")
            )
        return driver.connect()
    }
}