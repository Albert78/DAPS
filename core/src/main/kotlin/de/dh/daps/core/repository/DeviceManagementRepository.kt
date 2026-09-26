package de.dh.daps.core.repository

import androidx.datastore.preferences.core.stringPreferencesKey
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.core.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

val GLUCOSE_SOURCE_DESCRIPTOR_KEY = stringPreferencesKey("glucose_source_descriptor")
val PUMP_DESCRIPTOR_KEY = stringPreferencesKey("pump_descriptor")

/**
 * Repository for managing device-related configurations, active connection descriptors,
 * and device events (like cannula or reservoir changes).
 */
class DeviceManagementRepository(
    private val appDatabase: AppDatabase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val scope: CoroutineScope
) {
    /**
     * Flow emitting the currently saved [GlucoseSourceConnectionDescriptor] for the glucose source, or null if none is configured.
     */
    val glucoseSourceDescriptor: StateFlow<GlucoseSourceConnectionDescriptor?> = appPreferencesRepository.cachedPreferences
        .map { preferences ->
            preferences?.get(GLUCOSE_SOURCE_DESCRIPTOR_KEY)?.let { jsonStr ->
                runCatching { Json.decodeFromString<GlucoseSourceConnectionDescriptor>(jsonStr) }.getOrNull()
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Flow emitting the currently saved [PumpConnectionDescriptor] for the insulin pump, or null if none is configured.
     */
    val pumpDescriptor: StateFlow<PumpConnectionDescriptor?> = appPreferencesRepository.cachedPreferences
        .map { preferences ->
            preferences?.get(PUMP_DESCRIPTOR_KEY)?.let { jsonStr ->
                runCatching { Json.decodeFromString<PumpConnectionDescriptor>(jsonStr) }.getOrNull()
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Retrieves the saved glucose source connection descriptor.
     */
    suspend fun getGlucoseSourceDescriptor(): GlucoseSourceConnectionDescriptor? {
        val jsonStr = appPreferencesRepository.getPreferences()[GLUCOSE_SOURCE_DESCRIPTOR_KEY] ?: return null
        return runCatching { Json.decodeFromString<GlucoseSourceConnectionDescriptor>(jsonStr) }.getOrNull()
    }

    /**
     * Persists or removes the glucose source connection descriptor.
     */
    suspend fun saveGlucoseSourceDescriptor(descriptor: GlucoseSourceConnectionDescriptor?) {
        appPreferencesRepository.editPreferences { mutablePreferences ->
            if (descriptor != null) {
                mutablePreferences[GLUCOSE_SOURCE_DESCRIPTOR_KEY] = Json.encodeToString(descriptor)
            } else {
                mutablePreferences.remove(GLUCOSE_SOURCE_DESCRIPTOR_KEY)
            }
        }
    }

    /**
     * Retrieves the saved pump connection descriptor.
     */
    suspend fun getPumpDescriptor(): PumpConnectionDescriptor? {
        val jsonStr = appPreferencesRepository.getPreferences()[PUMP_DESCRIPTOR_KEY] ?: return null
        return runCatching { Json.decodeFromString<PumpConnectionDescriptor>(jsonStr) }.getOrNull()
    }

    /**
     * Persists or removes the pump connection descriptor.
     */
    suspend fun savePumpDescriptor(descriptor: PumpConnectionDescriptor?) {
        appPreferencesRepository.editPreferences { mutablePreferences ->
            if (descriptor != null) {
                mutablePreferences[PUMP_DESCRIPTOR_KEY] = Json.encodeToString(descriptor)
            } else {
                mutablePreferences.remove(PUMP_DESCRIPTOR_KEY)
            }
        }
    }
}