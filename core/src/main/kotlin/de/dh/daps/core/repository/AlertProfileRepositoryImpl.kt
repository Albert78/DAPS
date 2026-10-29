package de.dh.daps.core.repository

import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.core.repository.db.dao.AlertProfileDao
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlertProfileRepositoryImpl(
    private val alertProfileDao: AlertProfileDao
) : AlertProfileRepository {

    override suspend fun getAllAlertProfiles(): List<AlertProfile> {
        return alertProfileDao.getAllAlertProfiles().map { it.toModel() }
    }

    override fun observeAllAlertProfiles(): Flow<List<AlertProfile>> {
        return alertProfileDao.observeAllAlertProfiles().map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun getAlertProfileById(id: Long): AlertProfile? {
        return alertProfileDao.getAlertProfileById(id)?.toModel()
    }

    override suspend fun getDefaultAlertProfile(): AlertProfile? {
        return (alertProfileDao.getDefaultAlertProfile() ?: alertProfileDao.getAllAlertProfiles().firstOrNull())?.toModel()
    }

    override fun observeDefaultAlertProfile(): Flow<AlertProfile?> {
        return alertProfileDao.observeDefaultAlertProfile().map { entity ->
            entity?.toModel() ?: alertProfileDao.getAllAlertProfiles().firstOrNull()?.toModel()
        }
    }

    override suspend fun getActiveAlertProfile(): AlertProfile? {
        return alertProfileDao.getActiveAlertProfile()?.toModel() ?: getDefaultAlertProfile()
    }

    override fun observeActiveAlertProfile(): Flow<AlertProfile?> {
        return alertProfileDao.observeActiveAlertProfile().map { it?.toModel() }
    }

    override suspend fun insertAlertProfile(profile: AlertProfile): Long {
        val id = alertProfileDao.insertAlertProfile(profile.toEntity())
        if (profile.isDefault) {
            alertProfileDao.setDefaultAlertProfile(id)
        }
        return id
    }

    override suspend fun updateAlertProfile(profile: AlertProfile) {
        val existing = alertProfileDao.getAlertProfileById(profile.id)
        val isActive = existing?.isActive ?: false
        if (profile.isDefault) {
            alertProfileDao.setDefaultAlertProfile(profile.id)
        }
        alertProfileDao.updateAlertProfile(profile.toEntity(isActive = isActive))
    }

    override suspend fun deleteAlertProfile(id: Long) {
        alertProfileDao.deleteAlertProfile(id)
    }

    override suspend fun setDefaultAlertProfile(id: Long) {
        alertProfileDao.setDefaultAlertProfile(id)
    }

    override suspend fun setActiveAlertProfile(id: Long) {
        alertProfileDao.setDefaultAlertProfile(id)
        alertProfileDao.setActiveAlertProfile(id)
    }
}