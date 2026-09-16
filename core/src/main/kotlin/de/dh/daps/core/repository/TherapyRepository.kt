package de.dh.daps.core.repository

import de.dh.daps.common.model.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.model.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CurrentTherapySettings
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.TherapyAdjustmentTiming
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.repository.db.MetabolicEventsDao
import de.dh.daps.core.repository.db.TherapyDao
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.mappers.toDb
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Repository for therapy settings, profiles and active therapy data.
 */
class TherapyRepository(
    appDatabase: AppDatabase
) {
    private val therapyDao: TherapyDao = appDatabase.therapyDao()
    private val metabolicEventsDao: MetabolicEventsDao = appDatabase.metabolicEventsDao()

    @Volatile
    private var cachedCurrentTherapySettings: CurrentTherapySettings? = null

    // --- Insulin Types (Lookup Helper) ---

    /**
     * Internal lookup to resolve InsulinType for CurrentTherapySettings.
     * Master CRUD for InsulinTypes is in TreatmentRepository.
     */
    private suspend fun getInsulinTypeById(id: String): InsulinType? {
        return metabolicEventsDao.getInsulinTypeById(id)?.toModel()
    }

    /**
     * Helper for initialization or scenarios where only TherapyRepository is available.
     */
    suspend fun getAllInsulinTypes(): List<InsulinType> {
        return metabolicEventsDao.getAllInsulinTypes().map { it.toModel() }
    }

    // --- Profile Operations ---

    suspend fun getAllInsulinProfiles(): List<InsulinProfile> {
        return therapyDao.getAllInsulinProfiles().mapNotNull { entity ->
            val insulinType = getInsulinTypeById(entity.insulin_type_id)
            insulinType?.let { entity.toModel(it) }
        }
    }

    fun observeAllInsulinProfiles(): Flow<List<InsulinProfile>> {
        return therapyDao.observeAllInsulinProfiles().map { entities ->
            entities.mapNotNull { entity ->
                val insulinType = getInsulinTypeById(entity.insulin_type_id)
                insulinType?.let { entity.toModel(it) }
            }
        }
    }

    suspend fun getInsulinProfileById(id: Long): InsulinProfile? {
        val entity = therapyDao.getInsulinProfileById(id) ?: return null
        val insulinType = getInsulinTypeById(entity.insulin_type_id) ?: return null
        return entity.toModel(insulinType)
    }

    suspend fun insertInsulinProfile(profile: InsulinProfile): Long {
        val id = therapyDao.insertInsulinProfile(profile.toEntity())
        if (id != -1L) {
            profile.id = id
        }
        return id
    }

    suspend fun updateInsulinProfile(profile: InsulinProfile) {
        therapyDao.updateInsulinProfile(profile.toEntity())
        clearCache()
    }

    suspend fun deleteInsulinProfile(profile: InsulinProfile) {
        therapyDao.deleteInsulinProfile(profile.id)
        clearCache()
    }

    private val alarmProfileDao = appDatabase.alarmProfileDao()

    // --- Current Therapy Settings Operations ---

    /**
     * Clears the in-memory cache for CurrentTherapySettings.
     */
    fun clearCache() {
        cachedCurrentTherapySettings = null
    }

    fun observeCurrentTherapySettings(): Flow<CurrentTherapySettings> = combine(
        therapyDao.observeCurrentTherapySettings(),
        therapyDao.observeAllInsulinProfiles(),
        alarmProfileDao.observeAllAlarmProfiles()
    ) { _, _, _ ->
        fetchCurrentTherapySettingsFromDb()?.also { fresh ->
            cachedCurrentTherapySettings = fresh
        }
    }.mapNotNull { it }

    suspend fun getCurrentTherapySettings(): CurrentTherapySettings {
        return getCurrentTherapySettingsOrNull()
            ?: throw IllegalStateException("No current therapy settings found in database")
    }

    suspend fun getCurrentTherapySettingsOrNull(forceRefresh: Boolean = false): CurrentTherapySettings? {
        if (!forceRefresh) {
            cachedCurrentTherapySettings?.let { return it }
        }
        return fetchCurrentTherapySettingsFromDb()?.also {
            cachedCurrentTherapySettings = it
        }
    }

    private suspend fun fetchCurrentTherapySettingsFromDb(): CurrentTherapySettings? {
        val entity = therapyDao.getCurrentTherapySettings() ?: return null
        val profile = getInsulinProfileById(entity.insulin_profile_id) ?: return null
        val alarmProfile = entity.active_alarm_profile_id?.let { alarmProfileId ->
            alarmProfileDao.getAlarmProfileById(alarmProfileId)?.toModel()
        }
        val settings = entity.toModel(profile, alarmProfile)

        return if (settings.defaultBgBlocks.isEmpty()) {
            settings.copy(
                defaultBgBlocks = listOf(
                    BgBlock(
                        Minutes.ofHours(24),
                        BgValue.fromMgDl(DEFAULT_BG_TARGET_MGDL),
                        BgValue.fromMgDl(DEFAULT_BG_LOW_THRESHOLD_MGDL)
                    )
                )
            )
        } else {
            settings
        }
    }

    suspend fun updateCurrentTherapySettings(
        insulinProfileId: Long,
        defaultBgBlocks: List<BgBlock> = emptyList(),
        insulinAdjustmentPercentage: Int = 0,
        targetBgOverride: BgValue? = null,
        lowThresholdOverride: BgValue? = null,
        activeAlarmProfileId: Long? = null,
        adjustmentHint: String? = null,
        timing: TherapyAdjustmentTiming = TherapyAdjustmentTiming()
    ) {
        val existing = therapyDao.getCurrentTherapySettings()
        val entity = CurrentTherapySettingsEntity(
            id = existing?.id ?: ID_UNDEFINED,
            insulin_profile_id = insulinProfileId,
            default_bg_blocks = defaultBgBlocks.map { it.toDb() },
            insulin_adjustment_percentage = insulinAdjustmentPercentage,
            target_bg_override = targetBgOverride?.mgdlInt?.toShort(),
            low_threshold_override = lowThresholdOverride?.mgdlInt?.toShort(),
            active_alarm_profile_id = activeAlarmProfileId,
            adjustment_hint = adjustmentHint,
            adjustment_time_mode = timing.mode.name,
            adjustment_start_time_ms = timing.startTime?.ms,
            adjustment_end_time_ms = timing.endTime?.ms
        )
        if (existing == null) {
            therapyDao.insertCurrentTherapySettings(entity)
        } else {
            therapyDao.updateCurrentTherapySettings(entity)
        }
        clearCache()
    }
}