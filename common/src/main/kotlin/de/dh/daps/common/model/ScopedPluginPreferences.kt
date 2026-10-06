package de.dh.daps.common.model

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.dh.daps.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [PluginPreferences] that scopes preferences into an isolated key-prefix
 * inside the central [AppPreferencesRepository].
 */
class ScopedPluginPreferences(
    val pluginId: String,
    private val appPreferencesRepository: AppPreferencesRepository
) : PluginPreferences {

    private val prefix = "plugin.$pluginId."

    private fun scopedKey(key: String): String = "$prefix$key"

    override fun getString(key: String, defaultValue: String?): Flow<String?> {
        val prefKey = stringPreferencesKey(scopedKey(key))
        return appPreferencesRepository.preferences.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(scopedKey(key))
        return appPreferencesRepository.preferences.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    override fun getInt(key: String, defaultValue: Int): Flow<Int> {
        val prefKey = intPreferencesKey(scopedKey(key))
        return appPreferencesRepository.preferences.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    override fun getLong(key: String, defaultValue: Long): Flow<Long> {
        val prefKey = longPreferencesKey(scopedKey(key))
        return appPreferencesRepository.preferences.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Flow<Float> {
        val prefKey = floatPreferencesKey(scopedKey(key))
        return appPreferencesRepository.preferences.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    override suspend fun putString(key: String, value: String?) {
        val prefKey = stringPreferencesKey(scopedKey(key))
        appPreferencesRepository.editPreferences { mutablePreferences ->
            if (value != null) {
                mutablePreferences[prefKey] = value
            } else {
                mutablePreferences.remove(prefKey)
            }
        }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(scopedKey(key))
        appPreferencesRepository.editPreferences { mutablePreferences ->
            mutablePreferences[prefKey] = value
        }
    }

    override suspend fun putInt(key: String, value: Int) {
        val prefKey = intPreferencesKey(scopedKey(key))
        appPreferencesRepository.editPreferences { mutablePreferences ->
            mutablePreferences[prefKey] = value
        }
    }

    override suspend fun putLong(key: String, value: Long) {
        val prefKey = longPreferencesKey(scopedKey(key))
        appPreferencesRepository.editPreferences { mutablePreferences ->
            mutablePreferences[prefKey] = value
        }
    }

    override suspend fun putFloat(key: String, value: Float) {
        val prefKey = floatPreferencesKey(scopedKey(key))
        appPreferencesRepository.editPreferences { mutablePreferences ->
            mutablePreferences[prefKey] = value
        }
    }

    override suspend fun remove(key: String) {
        val scopedKeyName = scopedKey(key)
        appPreferencesRepository.editPreferences { mutablePreferences ->
            val existingKey = mutablePreferences.asMap().keys.find { it.name == scopedKeyName }
            if (existingKey != null) {
                mutablePreferences.remove(existingKey)
            }
        }
    }

    override suspend fun clear() {
        appPreferencesRepository.editPreferences { mutablePreferences ->
            val keysToRemove = mutablePreferences.asMap().keys.filter { it.name.startsWith(prefix) }
            keysToRemove.forEach { key ->
                mutablePreferences.remove(key)
            }
        }
    }
}