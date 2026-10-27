package de.dh.daps.core.glucosesource

import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.therapy.GlucoseSource
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Central registry and manager for registered [GlucoseSourceDriver] plugins.
 * Facilitates retrieving available drivers and streaming the active glucose source.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlucoseSourceDriverManager(
    val drivers: List<GlucoseSourceDriver> = emptyList(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * Flow emitting the currently active glucose source driver, or null if no driver is configured.
     */
    val activeDriver: StateFlow<GlucoseSourceDriver?> = if (drivers.isEmpty()) {
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
     * Flow emitting the ID of the currently active glucose source driver, or null if no driver is configured.
     */
    val activeDriverId: StateFlow<String?> = activeDriver
        .map { it?.driverId }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = getActiveDriver()?.driverId
        )

    /**
     * Flow emitting the currently active [GlucoseSource] provided by the active driver,
     * or null if no driver or device is active.
     */
    val activeGlucoseSource: StateFlow<GlucoseSource?> = activeDriver
        .flatMapLatest { driver -> driver?.activeGlucoseSource ?: flowOf(null) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = getActiveDriver()?.activeGlucoseSource?.value
        )

    /**
     * Retrieves a driver by its unique [driverId].
     */
    fun getDriver(driverId: String): GlucoseSourceDriver? {
        return drivers.find { it.driverId == driverId }
    }

    /**
     * Retrieves the currently active glucose source driver (the first driver whose lifecycle state is not NoDevice).
     */
    fun getActiveDriver(): GlucoseSourceDriver? {
        return drivers.firstOrNull { it.lifecycleState.value !is DeviceLifecycleState.NoDevice }
    }
}