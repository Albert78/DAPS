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

    override suspend fun getDefaultAlarmProfile(): AlarmProfile? {
        return (alarmProfileDao.getDefaultAlarmProfile() ?: alarmProfileDao.getAllAlarmProfiles().firstOrNull())?.toModel()
    }

    override fun observeDefaultAlarmProfile(): Flow<AlarmProfile?> {
        return alarmProfileDao.observeDefaultAlarmProfile().map { entity ->
            entity?.toModel() ?: alarmProfileDao.getAllAlarmProfiles().firstOrNull()?.toModel()
        }
    }

    override suspend fun getActiveAlarmProfile(): AlarmProfile? {
        return alarmProfileDao.getActiveAlarmProfile()?.toModel() ?: getDefaultAlarmProfile()
    }

    override fun observeActiveAlarmProfile(): Flow<AlarmProfile?> {
        return alarmProfileDao.observeActiveAlarmProfile().map { it?.toModel() }
    }

    override suspend fun insertAlarmProfile(profile: AlarmProfile): Long {
        val id = alarmProfileDao.insertAlarmProfile(profile.toEntity())
        if (profile.isDefault) {
            alarmProfileDao.setDefaultAlarmProfile(id)
        }
        return id
    }

    override suspend fun updateAlarmProfile(profile: AlarmProfile) {
        val existing = alarmProfileDao.getAlarmProfileById(profile.id)
        val isActive = existing?.is_active ?: false
        if (profile.isDefault) {
            alarmProfileDao.setDefaultAlarmProfile(profile.id)
        }
        alarmProfileDao.updateAlarmProfile(profile.toEntity(isActive = isActive))
    }

    override suspend fun deleteAlarmProfile(id: Long) {
        alarmProfileDao.deleteAlarmProfile(id)
    }

    override suspend fun setDefaultAlarmProfile(id: Long) {
        alarmProfileDao.setDefaultAlarmProfile(id)
    }

    override suspend fun setActiveAlarmProfile(id: Long) {
        alarmProfileDao.setDefaultAlarmProfile(id)
        alarmProfileDao.setActiveAlarmProfile(id)
    }
}