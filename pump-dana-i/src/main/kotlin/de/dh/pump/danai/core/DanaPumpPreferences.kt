package de.dh.pump.danai.core

import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.infrastructure.PluginPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Type-safe wrapper over [PluginPreferences] for Dana pump driver preferences.
 */
class DanaPumpPreferences(val preferences: PluginPreferences) {

    suspend fun getInsulinTypeId(): String? =
        preferences.getString(KEY_INSULIN_TYPE_ID).first()

    suspend fun saveInsulinTypeId(insulinTypeId: String?) {
        if (insulinTypeId != null) {
            preferences.putString(KEY_INSULIN_TYPE_ID, insulinTypeId)
        } else {
            preferences.remove(KEY_INSULIN_TYPE_ID)
        }
    }

    suspend fun getInsulinConcentration(): InsulinConcentration {
        val factor = preferences.getFloat(KEY_INSULIN_CONCENTRATION, 1.0f).first().toDouble()
        return InsulinConcentration(factor)
    }

    suspend fun saveInsulinConcentration(concentration: InsulinConcentration) {
        preferences.putFloat(KEY_INSULIN_CONCENTRATION, concentration.factor.toFloat())
    }

    suspend fun getCatheterWearDurationDays(defaultDays: Int = DEFAULT_CATHETER_WEAR_DURATION_DAYS): Int =
        preferences.getInt(KEY_CATHETER_WEAR_DURATION_DAYS, defaultDays).first()

    suspend fun saveCatheterWearDurationDays(days: Int) {
        preferences.putInt(KEY_CATHETER_WEAR_DURATION_DAYS, days)
    }

    fun getCatheterWearDurationDaysFlow(defaultDays: Int = DEFAULT_CATHETER_WEAR_DURATION_DAYS): Flow<Int> =
        preferences.getInt(KEY_CATHETER_WEAR_DURATION_DAYS, defaultDays)

    suspend fun getCatheterLastChanged(): Timestamp? =
        preferences.getLong(KEY_CATHETER_LAST_CHANGED, 0L).first()
            .takeIf { it > 0L }
            ?.let { Timestamp(it) }

    suspend fun saveCatheterLastChanged(timestamp: Timestamp?) {
        if (timestamp != null && timestamp.ms > 0L) {
            preferences.putLong(KEY_CATHETER_LAST_CHANGED, timestamp.ms)
        } else {
            preferences.remove(KEY_CATHETER_LAST_CHANGED)
        }
    }

    fun getCatheterLastChangedFlow(): Flow<Timestamp?> =
        preferences.getLong(KEY_CATHETER_LAST_CHANGED, 0L).map { millis ->
            if (millis > 0L) Timestamp(millis) else null
        }

    suspend fun getLastName(): String =
        preferences.getString(KEY_LAST_NAME, "").first() ?: ""

    suspend fun getLastAddress(): String =
        preferences.getString(KEY_LAST_ADDRESS, "").first() ?: ""

    suspend fun savePumpAddress(name: String, address: String) {
        preferences.putString(KEY_LAST_NAME, name)
        preferences.putString(KEY_LAST_ADDRESS, address)
    }

    suspend fun getBle5PairingKey(): String? =
        preferences.getString(KEY_BLE5_PAIRING_KEY).first()

    suspend fun saveHandshakeData(pairingKey: String?, hardwareModel: Int?, protocol: Int?) {
        pairingKey?.let { preferences.putString(KEY_BLE5_PAIRING_KEY, it) } ?: preferences.remove(KEY_BLE5_PAIRING_KEY)
        hardwareModel?.let { preferences.putInt(KEY_HARDWARE_MODEL, it) } ?: preferences.remove(KEY_HARDWARE_MODEL)
        protocol?.let { preferences.putInt(KEY_PROTOCOL, it) } ?: preferences.remove(KEY_PROTOCOL)
    }

    suspend fun saveActiveBolus(bolusId: String?, targetAmount: Double?, startTime: Long?) {
        bolusId?.let { preferences.putString(KEY_ACTIVE_BOLUS_ID, it) } ?: preferences.remove(KEY_ACTIVE_BOLUS_ID)
        targetAmount?.let { preferences.putFloat(KEY_ACTIVE_BOLUS_TARGET_AMOUNT, it.toFloat()) } ?: preferences.remove(KEY_ACTIVE_BOLUS_TARGET_AMOUNT)
        startTime?.let { preferences.putLong(KEY_ACTIVE_BOLUS_START_TIME, it) } ?: preferences.remove(KEY_ACTIVE_BOLUS_START_TIME)
    }

    suspend fun getActiveBolusTargetAmount(): Double? =
        preferences.getFloat(KEY_ACTIVE_BOLUS_TARGET_AMOUNT, -1f).first().takeIf { it >= 0 }?.toDouble()

    suspend fun getActiveBolusStartTime(): Long? =
        preferences.getLong(KEY_ACTIVE_BOLUS_START_TIME, -1L).first().takeIf { it != -1L }

    suspend fun getActiveBolusId(): String? =
        preferences.getString(KEY_ACTIVE_BOLUS_ID).first()

    suspend fun clear() {
        preferences.clear()
    }

    companion object {
        const val KEY_INSULIN_TYPE_ID = "insulin_type_id"
        const val KEY_INSULIN_CONCENTRATION = "insulin_concentration"
        const val KEY_CATHETER_WEAR_DURATION_DAYS = "catheter_wear_duration_days"
        const val DEFAULT_CATHETER_WEAR_DURATION_DAYS = 3
        const val KEY_CATHETER_LAST_CHANGED = "catheter_last_changed"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_LAST_ADDRESS = "last_address"
        const val KEY_BLE5_PAIRING_KEY = "ble5_pairing_key"
        const val KEY_HARDWARE_MODEL = "hardware_model"
        const val KEY_PROTOCOL = "protocol"
        const val KEY_ACTIVE_BOLUS_ID = "active_bolus_id"
        const val KEY_ACTIVE_BOLUS_TARGET_AMOUNT = "active_bolus_target_amount"
        const val KEY_ACTIVE_BOLUS_START_TIME = "active_bolus_start_time"
    }
}