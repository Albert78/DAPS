package de.dh.daps.core.repository

import android.content.Context
import de.dh.daps.common.model.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.model.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CurrentSettings
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.getDefaultAlarmProfiles
import de.dh.daps.common.model.getDefaultInsulinProfile
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.common.model.getDefaultMealTypes

object DatabaseInitializer {
    suspend fun initialize(
        context: Context,
        treatmentRepository: TreatmentRepository,
        therapyRepository: TherapyRepository,
        settingsRepository: SettingsRepository,
        alarmRepository: AlarmRepository
    ) {
        initializeInsulinTypes(context, treatmentRepository)
        initializeMealTypes(context, treatmentRepository)
        initializeDefaultInsulinProfileAndCurrentTherapy(context, therapyRepository)
        initializeSettings(settingsRepository)
        initializeDefaultAlarmProfiles(context, alarmRepository)
    }

    private suspend fun initializeInsulinTypes(context: Context, repository: TreatmentRepository) {
        if (repository.getAllInsulinTypes().isNotEmpty()) {
            return
        }
        getDefaultInsulinTypes(context).forEach {
            repository.insertInsulinType(it)
        }
    }

    private suspend fun initializeMealTypes(context: Context, repository: TreatmentRepository) {
        if (repository.getAllMealTypes().isNotEmpty()) {
            return
        }
        getDefaultMealTypes(context).forEach {
            repository.insertMealType(it)
        }
    }

    private suspend fun initializeDefaultInsulinProfileAndCurrentTherapy(context: Context, repository: TherapyRepository) {
        val insulinTypes = repository.getAllInsulinTypes()
        if (insulinTypes.isEmpty()) return

        val defaultInsulinType = insulinTypes.first()

        var profiles = repository.getAllInsulinProfiles()
        if (profiles.isEmpty()) {
            val normalProfile = getDefaultInsulinProfile(context, defaultInsulinType)
            repository.insertInsulinProfile(normalProfile)
            profiles = listOf(normalProfile)
        }

        if (repository.getCurrentTherapySettingsOrNull() == null) {
            val activeProfile = profiles.first()

            repository.updateCurrentTherapySettings(
                insulinProfileId = activeProfile.id,
                defaultBgBlocks = listOf(
                    BgBlock(
                        Minutes.ofHours(24),
                        BgValue.fromMgDl(DEFAULT_BG_TARGET_MGDL),
                        BgValue.fromMgDl(DEFAULT_BG_LOW_THRESHOLD_MGDL)
                    )
                )
            )
        }
    }

    private suspend fun initializeSettings(repository: SettingsRepository) {
        if (repository.getCurrentSettings() == null) {
            repository.updateCurrentSettings(CurrentSettings())
        }
    }

    private suspend fun initializeDefaultAlarmProfiles(context: Context, repository: AlarmRepository) {
        if (repository.getAllAlarmProfiles().isNotEmpty()) {
            return
        }
        getDefaultAlarmProfiles(context).forEach { profile ->
            val id = repository.insertAlarmProfile(profile)
            if (profile.isDefault) {
                repository.setActiveAlarmProfile(id)
            }
        }
    }
}