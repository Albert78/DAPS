package de.dh.daps.core.backup

import android.content.Context
import androidx.room.withTransaction
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.carbsUnit
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.DatabaseInitializer
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.glucoseSourceDescriptor
import de.dh.daps.glucoseUnit
import de.dh.daps.pumpDescriptor
import de.dh.daps.setCarbsUnit
import de.dh.daps.setGlucoseSourceDescriptor
import de.dh.daps.setGlucoseUnit
import de.dh.daps.setPumpDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface AppDataManagementRepository {
    suspend fun exportBackup(outputStream: OutputStream, options: BackupOptions): BackupResult
    suspend fun importBackup(
        inputStream: InputStream,
        options: BackupOptions = BackupOptions(
            includeHistory = false,
            includeDiagnostics = false,
            includeDescriptors = true,
        )
    ): BackupResult
    suspend fun resetToDefaultData()
    suspend fun clearAllData()
}

class AppDataManagementRepositoryImpl(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val preferencesRepository: AppPreferencesRepository,
    private val treatmentRepository: TreatmentRepository,
    private val therapyRepository: TherapyRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository,
    private val glucoseRepository: GlucoseRepository? = null,
) : AppDataManagementRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportBackup(
        outputStream: OutputStream,
        options: BackupOptions
    ): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Fetch Preferences
            val preferences = preferencesRepository.getPreferences()
            val preferencesDto = AppPreferencesDto(
                glucoseUnit = preferences.glucoseUnit.name,
                carbsUnit = preferences.carbsUnit.name,
                glucoseSourceDescriptorJson = if (options.includeDescriptors) {
                    preferences.glucoseSourceDescriptor?.let { json.encodeToString(it) }
                } else null,
                pumpDescriptorJson = if (options.includeDescriptors) {
                    preferences.pumpDescriptor?.let { json.encodeToString(it) }
                } else null
            )

            // 2. Fetch Therapy Configuration
            val therapyDao = appDatabase.therapyDao()
            val providerDao = appDatabase.providerDao()
            val settingsDao = appDatabase.settingsDao()
            val alarmDao = appDatabase.alarmProfileDao()
            val metabolicDao = appDatabase.metabolicEventsDao()

            val therapyConfigDto = TherapyConfigDto(
                insulinProfiles = therapyDao.getAllInsulinProfiles().map { it.toDto() },
                currentTherapySettings = therapyDao.getCurrentTherapySettings()?.toDto(),
                therapyAdjustments = therapyDao.getAllTherapyAdjustments().map { it.toDto() },
                currentSettings = settingsDao.getCurrentSettings()?.toDto(),
                alarmProfiles = alarmDao.getAllAlarmProfiles().map { it.toDto() },
                mealTypes = metabolicDao.getAllMealTypes().map { it.toDto() },
                insulinTypes = metabolicDao.getAllInsulinTypes().map { it.toDto() },
                sensorTypes = providerDao.getAllSensorTypes().map { it.toDto() },
                dataProviders = providerDao.getAllDataProviders().map { it.toDto() }
            )

            // 3. Fetch Medical History (if selected)
            val medicalHistoryDto = if (options.includeHistory) {
                val minTimestampMs = options.historyDaysFilter?.let { days ->
                    System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
                }

                val glucoseReadings = if (minTimestampMs != null) {
                    providerDao.getAllGlucoseReadings().filter { it.timestamp.ms >= minTimestampMs }
                } else {
                    providerDao.getAllGlucoseReadings()
                }

                val meals = if (minTimestampMs != null) {
                    metabolicDao.getMealsSince(minTimestampMs)
                } else {
                    metabolicDao.getAllMeals()
                }

                val insulinApplications = if (minTimestampMs != null) {
                    metabolicDao.getInsulinApplicationsSince(minTimestampMs)
                } else {
                    metabolicDao.getAllInsulinApplications()
                }

                val deferredBoluses = metabolicDao.getAllDeferredBoluses()
                val scheduledTherapyAdjustments = therapyDao.getAllScheduledTherapyAdjustments()
                val mealReminders = appDatabase.mealReminderDao().getAllMealReminders()

                MedicalHistoryDto(
                    glucoseReadings = glucoseReadings.map { it.toDto() },
                    meals = meals.map { it.toDto() },
                    insulinApplications = insulinApplications.map { it.toDto() },
                    deferredBoluses = deferredBoluses.map { it.toDto() },
                    scheduledTherapyAdjustments = scheduledTherapyAdjustments.map { it.toDto() },
                    mealReminders = mealReminders.map { it.toDto() }
                )
            } else {
                MedicalHistoryDto()
            }

            // 4. Fetch Diagnostics (if selected)
            val diagnosticsDto = if (options.includeDiagnostics) {
                val insights = appDatabase.systemMetricsDao().getAllCoreInsights()
                DiagnosticsDto(insights = insights.map { it.toDto() })
            } else {
                DiagnosticsDto()
            }

            // 5. Create Manifest
            val manifestDto = BackupManifestDto(
                version = 1,
                appVersion = "1.0",
                schemaVersion = 2,
                createdAtMs = System.currentTimeMillis(),
                includeHistory = options.includeHistory,
                includeDiagnostics = options.includeDiagnostics
            )

            // 6. Write ZIP Archive
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                zipOut.putNextEntry(ZipEntry("manifest.json"))
                zipOut.write(json.encodeToString(manifestDto).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                zipOut.putNextEntry(ZipEntry("preferences.json"))
                zipOut.write(json.encodeToString(preferencesDto).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                zipOut.putNextEntry(ZipEntry("therapy_config.json"))
                zipOut.write(json.encodeToString(therapyConfigDto).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                if (options.includeHistory) {
                    zipOut.putNextEntry(ZipEntry("medical_history.json"))
                    zipOut.write(json.encodeToString(medicalHistoryDto).toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }

                if (options.includeDiagnostics) {
                    zipOut.putNextEntry(ZipEntry("diagnostics.json"))
                    zipOut.write(json.encodeToString(diagnosticsDto).toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }

            BackupResult.Success(manifestDto)
        }.getOrElse {
            BackupResult.Error(it)
        }
    }

    override suspend fun importBackup(
        inputStream: InputStream,
        options: BackupOptions
    ): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            var manifestDto: BackupManifestDto? = null
            var preferencesDto: AppPreferencesDto? = null
            var therapyConfigDto: TherapyConfigDto? = null
            var medicalHistoryDto: MedicalHistoryDto? = null
            var diagnosticsDto: DiagnosticsDto? = null

            // 1. Unzip and deserialize JSON files
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val content = zipIn.readBytes().toString(Charsets.UTF_8)
                    val entryName = entry.name.substringAfterLast('/')
                    when (entryName) {
                        "manifest.json" -> manifestDto = json.decodeFromString(content)
                        "preferences.json" -> preferencesDto = json.decodeFromString(content)
                        "therapy_config.json" -> therapyConfigDto = json.decodeFromString(content)
                        "medical_history.json" -> medicalHistoryDto = json.decodeFromString(content)
                        "diagnostics.json" -> diagnosticsDto = json.decodeFromString(content)
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            val manifest = manifestDto ?: throw IllegalArgumentException("Invalid backup file: manifest.json is missing.")
            val therapyConfig = therapyConfigDto ?: throw IllegalArgumentException("Invalid backup file: therapy_config.json is missing.")

            // 2. Restore Preferences
            preferencesDto?.let { prefs ->
                runCatching {
                    preferencesRepository.setGlucoseUnit(GlucoseUnit.valueOf(prefs.glucoseUnit))
                }
                runCatching {
                    preferencesRepository.setCarbsUnit(CarbsUnit.valueOf(prefs.carbsUnit))
                }
                if (options.includeDescriptors) {
                    prefs.glucoseSourceDescriptorJson?.let { descriptorJson ->
                        val descriptor = runCatching {
                            json.decodeFromString<GlucoseSourceConnectionDescriptor>(descriptorJson)
                        }.getOrNull()
                        preferencesRepository.setGlucoseSourceDescriptor(descriptor)
                    }
                    prefs.pumpDescriptorJson?.let { descriptorJson ->
                        val descriptor = runCatching {
                            json.decodeFromString<PumpConnectionDescriptor>(descriptorJson)
                        }.getOrNull()
                        preferencesRepository.setPumpDescriptor(descriptor)
                    }
                }
            }

            // 3. Database Restore in an atomic Room Transaction
            appDatabase.withTransaction {
                val metricsDao = appDatabase.systemMetricsDao()
                val therapyDao = appDatabase.therapyDao()
                val providerDao = appDatabase.providerDao()
                val settingsDao = appDatabase.settingsDao()
                val alarmDao = appDatabase.alarmProfileDao()
                val metabolicDao = appDatabase.metabolicEventsDao()
                val mealReminderDao = appDatabase.mealReminderDao()

                // Core metrics
                metricsDao.deleteAllCoreInsights()

                // Delete history data, regardless of whether history is imported or not
                metabolicDao.deleteAllDeferredBoluses()
                mealReminderDao.deleteAllMealReminders()
                metabolicDao.deleteAllMeals()
                metabolicDao.deleteAllInsulinApplications()
                providerDao.deleteAllGlucoseReadings()
                therapyDao.deleteAllScheduledTherapyAdjustments()

                // Delete core therapy & settings
                therapyDao.deleteAllCurrentTherapySettings()
                therapyDao.deleteAllTherapyAdjustments()
                settingsDao.deleteAllCurrentSettings()
                alarmDao.deleteAllAlarmProfiles()
                therapyDao.deleteAllInsulinProfiles()

                // Types metadata
                providerDao.deleteAllSensorTypes()
                providerDao.deleteAllDataProviders()
                metabolicDao.deleteAllMealTypes()
                metabolicDao.deleteAllInsulinTypes()


                // Insert Therapy Config in parent-first order
                if (therapyConfig.insulinTypes.isNotEmpty()) {
                    metabolicDao.insertInsulinTypes(therapyConfig.insulinTypes.map { it.toEntity() })
                }
                if (therapyConfig.mealTypes.isNotEmpty()) {
                    metabolicDao.insertMealTypes(therapyConfig.mealTypes.map { it.toEntity() })
                }
                if (therapyConfig.sensorTypes.isNotEmpty()) {
                    providerDao.insertSensorTypes(therapyConfig.sensorTypes.map { it.toEntity() })
                }
                if (therapyConfig.dataProviders.isNotEmpty()) {
                    providerDao.insertDataProviders(therapyConfig.dataProviders.map { it.toEntity() })
                }
                if (therapyConfig.insulinProfiles.isNotEmpty()) {
                    therapyDao.insertInsulinProfiles(therapyConfig.insulinProfiles.map { it.toEntity() })
                }
                if (therapyConfig.alarmProfiles.isNotEmpty()) {
                    alarmDao.insertAlarmProfiles(therapyConfig.alarmProfiles.map { it.toEntity() })
                }
                therapyConfig.currentSettings?.let {
                    settingsDao.insertCurrentSettingsList(listOf(it.toEntity()))
                }
                if (therapyConfig.therapyAdjustments.isNotEmpty()) {
                    therapyDao.insertTherapyAdjustments(therapyConfig.therapyAdjustments.map { it.toEntity() })
                }
                therapyConfig.currentTherapySettings?.let {
                    therapyDao.insertCurrentTherapySettingsList(listOf(it.toEntity()))
                }

                // Insert Medical History (if requested)
                if (options.includeHistory) {
                    medicalHistoryDto?.let { history ->
                        if (history.glucoseReadings.isNotEmpty()) {
                            providerDao.insertGlucoseReadings(history.glucoseReadings.map { it.toEntity() })
                        }
                        if (history.insulinApplications.isNotEmpty()) {
                            metabolicDao.insertInsulinApplications(history.insulinApplications.map { it.toEntity() })
                        }
                        if (history.meals.isNotEmpty()) {
                            metabolicDao.insertMeals(history.meals.map { it.toEntity() })
                        }
                        if (history.mealReminders.isNotEmpty()) {
                            mealReminderDao.insertMealReminders(history.mealReminders.map { it.toEntity() })
                        }
                        if (history.deferredBoluses.isNotEmpty()) {
                            metabolicDao.insertDeferredBoluses(history.deferredBoluses.map { it.toEntity() })
                        }
                        if (history.scheduledTherapyAdjustments.isNotEmpty()) {
                            therapyDao.insertScheduledTherapyAdjustments(history.scheduledTherapyAdjustments.map { it.toEntity() })
                        }
                    }
                }

                // Insert Diagnostics (if requested)
                if (options.includeDiagnostics) {
                    diagnosticsDto?.let { diagnostics ->
                        if (diagnostics.insights.isNotEmpty()) {
                            metricsDao.insertCoreInsights(diagnostics.insights.map { it.toEntity() })
                        }
                    }
                }
            }

            // Sync in-memory state across repositories after importing database
            treatmentRepository.load()
            therapyRepository.clearCache()
            glucoseRepository?.initialize()

            BackupResult.Success(manifest)
        }.getOrElse {
            BackupResult.Error(it)
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        wipeDatabaseInternal()
        treatmentRepository.load()
        therapyRepository.clearCache()
        glucoseRepository?.initialize()
        Unit
    }

    override suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        wipeDatabaseInternal()

        treatmentRepository.load()
        therapyRepository.clearCache()
        glucoseRepository?.initialize()

        DatabaseInitializer.initialize(
            context = context,
            treatmentRepository = treatmentRepository,
            therapyRepository = therapyRepository,
            settingsRepository = settingsRepository,
            alarmRepository = alarmRepository
        )

        // Reload in-memory state after initialization
        treatmentRepository.load()
        therapyRepository.clearCache()
        glucoseRepository?.initialize()
        Unit
    }

    private suspend fun wipeDatabaseInternal() {
        appDatabase.withTransaction {
            val metricsDao = appDatabase.systemMetricsDao()
            val therapyDao = appDatabase.therapyDao()
            val providerDao = appDatabase.providerDao()
            val settingsDao = appDatabase.settingsDao()
            val alarmDao = appDatabase.alarmProfileDao()
            val metabolicDao = appDatabase.metabolicEventsDao()
            val mealReminderDao = appDatabase.mealReminderDao()

            metricsDao.deleteAllCoreInsights()
            metabolicDao.deleteAllDeferredBoluses()
            mealReminderDao.deleteAllMealReminders()
            metabolicDao.deleteAllMeals()
            metabolicDao.deleteAllInsulinApplications()
            providerDao.deleteAllGlucoseReadings()
            therapyDao.deleteAllCurrentTherapySettings()
            therapyDao.deleteAllScheduledTherapyAdjustments()
            therapyDao.deleteAllTherapyAdjustments()
            settingsDao.deleteAllCurrentSettings()
            alarmDao.deleteAllAlarmProfiles()
            therapyDao.deleteAllInsulinProfiles()
            providerDao.deleteAllSensorTypes()
            providerDao.deleteAllDataProviders()
            metabolicDao.deleteAllMealTypes()
            metabolicDao.deleteAllInsulinTypes()
        }
    }
}