package de.dh.pump.danai.core

import de.dh.daps.common.model.PluginPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakePluginPreferences : PluginPreferences {
    private val values = MutableStateFlow<Map<String, Any?>>(emptyMap())

    override fun getString(key: String, defaultValue: String?): Flow<String?> =
        values.map { (it[key] as? String) ?: defaultValue }

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        values.map { (it[key] as? Boolean) ?: defaultValue }

    override fun getInt(key: String, defaultValue: Int): Flow<Int> =
        values.map { (it[key] as? Int) ?: defaultValue }

    override fun getLong(key: String, defaultValue: Long): Flow<Long> =
        values.map { (it[key] as? Long) ?: defaultValue }

    override fun getFloat(key: String, defaultValue: Float): Flow<Float> =
        values.map { (it[key] as? Float) ?: defaultValue }

    override suspend fun putString(key: String, value: String?) {
        updateMap { if (value != null) put(key, value) else remove(key) }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        updateMap { put(key, value) }
    }

    override suspend fun putInt(key: String, value: Int) {
        updateMap { put(key, value) }
    }

    override suspend fun putLong(key: String, value: Long) {
        updateMap { put(key, value) }
    }

    override suspend fun putFloat(key: String, value: Float) {
        updateMap { put(key, value) }
    }

    override suspend fun remove(key: String) {
        updateMap { remove(key) }
    }

    override suspend fun clear() {
        values.value = emptyMap()
    }

    private fun updateMap(block: MutableMap<String, Any?>.() -> Unit) {
        val newMap = values.value.toMutableMap()
        newMap.block()
        values.value = newMap
    }
}

class DanaPumpPreferencesTest {

    @Test
    fun testCatheterWearDurationDays_defaultAndSave() = runTest {
        val fakePrefs = FakePluginPreferences()
        val danaPrefs = DanaPumpPreferences(fakePrefs)

        // Verify default value is 3 days
        assertEquals(3, danaPrefs.getCatheterWearDurationDays())

        // Save new duration (2 days)
        danaPrefs.saveCatheterWearDurationDays(2)

        // Verify updated value
        assertEquals(2, danaPrefs.getCatheterWearDurationDays())
    }
}