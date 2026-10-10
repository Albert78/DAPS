package de.dh.daps.core.pump

import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Central registry and manager for registered [InsulinPumpDriver] plugins.
 * Facilitates retrieving available drivers and attaching bound drivers.
 */
class PumpDriverManager(
    val drivers: List<InsulinPumpDriver> = emptyList(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * Flow emitting the currently active insulin pump driver, or null if no driver is configured.
     */
    val activeDriver: StateFlow<InsulinPumpDriver?> = if (drivers.isEmpty()) {
        MutableStateFlow(null)
    } else {
        combine(drivers.map { it.lifecycleState }) { states ->
            val index = states.indexOfFirst { it !is DeviceLifecycleState.NoDevice }
            if (index != -1) drivers[index] else null
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = getActiveDriver()
        )
    }

    /**
     * Flow emitting the ID of the currently active insulin pump driver, or null if no driver is configured.
     */
    val activeDriverId: StateFlow<String?> = activeDriver
        .map { it?.driverId }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = getActiveDriver()?.driverId
        )

    /**
     * Retrieves a driver by its unique [driverId].
     */
    fun getDriver(driverId: String): InsulinPumpDriver? {
        return drivers.find { it.driverId == driverId }
    }

    /**
     * Retrieves the currently active insulin pump driver (the first driver whose lifecycle state is not NoDevice).
     */
    fun getActiveDriver(): InsulinPumpDriver? {
        return drivers.firstOrNull { it.lifecycleState.value !is DeviceLifecycleState.NoDevice }
    }

    /**
     * Re-establishes a pump connection for the specified [driverId].
     */
    suspend fun attachToPump(driverId: String): Result<InsulinPump> {
        val driver = getDriver(driverId)
            ?: return Result.failure(
                IllegalArgumentException("No pump driver found matching driver ID '$driverId'.")
            )
        return driver.attach()
    }
}