package de.dh.pump.danai.ui

import android.content.SharedPreferences
import androidx.core.content.edit
import de.dh.daps.common.model.PluginPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SharedPreferencesPluginPreferences(
    private val prefs: SharedPreferences
) : PluginPreferences {

    override fun getString(key: String, defaultValue: String?): Flow<String?> = flow {
        emit(if (prefs.contains(key)) prefs.getString(key, defaultValue) else defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> = flow {
        emit(if (prefs.contains(key)) prefs.getBoolean(key, defaultValue) else defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Flow<Int> = flow {
        emit(if (prefs.contains(key)) prefs.getInt(key, defaultValue) else defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Flow<Long> = flow {
        emit(if (prefs.contains(key)) prefs.getLong(key, defaultValue) else defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Flow<Float> = flow {
        emit(if (prefs.contains(key)) prefs.getFloat(key, defaultValue) else defaultValue)
    }

    override suspend fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
        } else {
            prefs.edit { putString(key, value) }
        }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    override suspend fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    override suspend fun putLong(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    override suspend fun putFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    override suspend fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    override suspend fun clear() {
        prefs.edit { clear() }
    }
}