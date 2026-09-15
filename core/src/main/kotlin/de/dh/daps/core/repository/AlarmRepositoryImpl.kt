package de.dh.daps.core.repository

import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.core.repository.db.dao.AlarmProfileDao
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val alarmProfileDao: AlarmProfileDao
) : AlarmRepository {

    override suspend fun getAllAlarmProfiles(): List<AlarmProfile> {
        return alarmProfileDao.getAllAlarmProfiles().map { it.toModel() }
    }

    override fun observeAllAlarmProfiles(): Flow<List<AlarmProfile>> {
        return alarmProfileDao.observeAllAlarmProfiles().map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun getAlarmProfileById(id: Long): AlarmProfile? {
        return alarmProfileDao.getAlarmProfileById(id)?.toModel()
    }

    override suspend fun getActiveAlarmProfile(): AlarmProfile? {
        return alarmProfileDao.getActiveAlarmProfile()?.toModel()
    }

    override fun observeActiveAlarmProfile(): Flow<AlarmProfile?> {
        return alarmProfileDao.observeActiveAlarmProfile().map { it?.toModel() }
    }

    override suspend fun insertAlarmProfile(profile: AlarmProfile): Long {
        return alarmProfileDao.insertAlarmProfile(profile.toEntity())
    }

    override suspend fun updateAlarmProfile(profile: AlarmProfile) {
        val existing = alarmProfileDao.getAlarmProfileById(profile.id)
        val isActive = existing?.is_active ?: false
        alarmProfileDao.updateAlarmProfile(profile.toEntity(isActive = isActive))
    }

    override suspend fun deleteAlarmProfile(id: Long) {
        alarmProfileDao.deleteAlarmProfile(id)
    }

    override suspend fun setActiveAlarmProfile(id: Long) {
        alarmProfileDao.setActiveAlarmProfile(id)
    }
}