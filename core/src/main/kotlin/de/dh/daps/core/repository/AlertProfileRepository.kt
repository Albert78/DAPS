package de.dh.daps.core.repository

import de.dh.daps.common.domain.model.alarm.AlertProfile
import kotlinx.coroutines.flow.Flow

interface AlertProfileRepository {
    suspend fun getAllAlertProfiles(): List<AlertProfile>
    fun observeAllAlertProfiles(): Flow<List<AlertProfile>>
    suspend fun getAlertProfileById(id: Long): AlertProfile?
    suspend fun getDefaultAlertProfile(): AlertProfile?
    fun observeDefaultAlertProfile(): Flow<AlertProfile?>
    suspend fun getActiveAlertProfile(): AlertProfile?
    fun observeActiveAlertProfile(): Flow<AlertProfile?>
    suspend fun insertAlertProfile(profile: AlertProfile): Long
    suspend fun updateAlertProfile(profile: AlertProfile)
    suspend fun deleteAlertProfile(id: Long)
    suspend fun setDefaultAlertProfile(id: Long)
    suspend fun setActiveAlertProfile(id: Long)
}