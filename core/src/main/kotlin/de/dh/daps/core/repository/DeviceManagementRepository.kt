package de.dh.daps.core.repository

import de.dh.daps.AppPreferencesRepository
import de.dh.daps.activeGlucoseSourceDriverId
import de.dh.daps.activePumpDriverId
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.glucoseSourceDescriptor
import de.dh.daps.pumpDescriptor
import de.dh.daps.setActiveGlucoseSourceDriverId
import de.dh.daps.setActivePumpDriverId
import de.dh.daps.setGlucoseSourceDescriptor
import de.dh.daps.setPumpDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Repository for managing diabetes-device-related configurations (CGM, pump), active driver identifiers,
 * connection descriptors, and device events.
 */
class DeviceManagementRepository(
    private val appDatabase: AppDatabase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val scope: CoroutineScope
) {
    /**
     * Flow emitting the currently active glucose source driver ID, or null if none is selected.
     */
    val activeGlucoseSourceDriverId: StateFlow<String?> = appPreferencesRepository.preferences
        .map { it.activeGlucoseSourceDriverId }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Flow emitting the currently active insulin pump driver ID, or null if none is selected.
     */
    val activePumpDriverId: StateFlow<String?> = appPreferencesRepository.preferences
        .map { it.activePumpDriverId }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Flow emitting the currently saved [GlucoseSourceConnectionDescriptor] for the glucose source, or null if none is configured.
     */
    val glucoseSourceDescriptor: StateFlow<GlucoseSourceConnectionDescriptor?> = appPreferencesRepository.preferences
        .map { it.glucoseSourceDescriptor }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Flow emitting the currently saved [PumpConnectionDescriptor] for the insulin pump, or null if none is configured.
     */
    val pumpDescriptor: StateFlow<PumpConnectionDescriptor?> = appPreferencesRepository.preferences
        .map { it.pumpDescriptor }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    suspend fun getActiveGlucoseSourceDriverId(): String? {
        return appPreferencesRepository.getPreferences().activeGlucoseSourceDriverId
    }

    suspend fun setActiveGlucoseSourceDriverId(driverId: String?) {
        appPreferencesRepository.setActiveGlucoseSourceDriverId(driverId)
    }

    suspend fun getActivePumpDriverId(): String? {
        return appPreferencesRepository.getPreferences().activePumpDriverId
    }

    suspend fun setActivePumpDriverId(driverId: String?) {
        appPreferencesRepository.setActivePumpDriverId(driverId)
    }

    /**
     * Retrieves the saved glucose source connection descriptor.
     */
    suspend fun getGlucoseSourceDescriptor(): GlucoseSourceConnectionDescriptor? {
        return appPreferencesRepository.getPreferences().glucoseSourceDescriptor
    }

    /**
     * Persists or removes the glucose source connection descriptor.
     */
    suspend fun saveGlucoseSourceDescriptor(descriptor: GlucoseSourceConnectionDescriptor?) {
        appPreferencesRepository.setGlucoseSourceDescriptor(descriptor)
    }

    /**
     * Retrieves the saved pump connection descriptor.
     */
    suspend fun getPumpDescriptor(): PumpConnectionDescriptor? {
        return appPreferencesRepository.getPreferences().pumpDescriptor
    }

    /**
     * Persists or removes the pump connection descriptor.
     */
    suspend fun savePumpDescriptor(descriptor: PumpConnectionDescriptor?) {
        appPreferencesRepository.setPumpDescriptor(descriptor)
    }
}