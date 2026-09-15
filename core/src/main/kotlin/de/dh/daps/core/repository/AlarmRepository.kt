package de.dh.daps.core.repository

import de.dh.daps.common.model.data.AlarmProfile
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    suspend fun getAllAlarmProfiles(): List<AlarmProfile>
    fun observeAllAlarmProfiles(): Flow<List<AlarmProfile>>
    suspend fun getAlarmProfileById(id: Long): AlarmProfile?
    suspend fun getActiveAlarmProfile(): AlarmProfile?
    fun observeActiveAlarmProfile(): Flow<AlarmProfile?>
    suspend fun insertAlarmProfile(profile: AlarmProfile): Long
    suspend fun updateAlarmProfile(profile: AlarmProfile)
    suspend fun deleteAlarmProfile(id: Long)
    suspend fun setActiveAlarmProfile(id: Long)
}