package de.dh.daps.common.model

import kotlinx.coroutines.flow.Flow

/**
 * Isolated key-value preference storage for a plugin.
 * Allows reading and writing values scoped to a specific plugin instance.
 */
interface PluginPreferences {
    fun getString(key: String, defaultValue: String? = null): Flow<String?>
    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>
    fun getInt(key: String, defaultValue: Int = 0): Flow<Int>
    fun getLong(key: String, defaultValue: Long = 0L): Flow<Long>
    fun getFloat(key: String, defaultValue: Float = 0f): Flow<Float>

    suspend fun putString(key: String, value: String?)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putInt(key: String, value: Int)
    suspend fun putLong(key: String, value: Long)
    suspend fun putFloat(key: String, value: Float)

    suspend fun remove(key: String)
    suspend fun clear()
}