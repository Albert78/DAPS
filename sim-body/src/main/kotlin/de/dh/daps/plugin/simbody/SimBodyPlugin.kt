package de.dh.daps.plugin.simbody

import android.app.Application
import androidx.room.withTransaction
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.SystemRegistry
import de.dh.daps.plugin.simbody.backup.BodyProfileBackupDto
import de.dh.daps.plugin.simbody.backup.PumpHistoryBackupDto
import de.dh.daps.plugin.simbody.backup.PumpStateBackupDto
import de.dh.daps.plugin.simbody.backup.SimBodyBackupDto
import de.dh.daps.plugin.simbody.backup.SimEventBackupDto
import de.dh.daps.plugin.simbody.backup.SimHistoryBackupDto
import de.dh.daps.plugin.simbody.backup.SimulationStateBackupDto
import de.dh.daps.plugin.simbody.repository.db.BodyProfileEntity
import de.dh.daps.plugin.simbody.repository.db.PumpDeliveryType
import de.dh.daps.plugin.simbody.repository.db.PumpHistoryEntity
import de.dh.daps.plugin.simbody.repository.db.PumpStateEntity
import de.dh.daps.plugin.simbody.repository.db.SimBodyDatabase
import de.dh.daps.plugin.simbody.repository.db.SimEventEntity
import de.dh.daps.plugin.simbody.repository.db.SimHistoryEntity
import de.dh.daps.plugin.simbody.repository.db.SimulationStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A plugin which provides a glucose source and a pump instance which are connected to a
 * "virtual human body", simulating the influence of meals and insulin.
 */
class SimBodyPlugin(
    val application: Application
) : Plugin {
    private val database = SimBodyDatabase.getInstance(application)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val bodyModel = BodyModel(DEFAULT_SIM_BODY_PROFILE, database.impactDao(), application)
    val pumpDevice = SimBodyPumpDevice(bodyModel, DEFAULT_SIM_INSULIN_PROFILE, database.pumpDao())
    val pumpDriver = SimBodyInsulinPumpDriver(this)
    val glucoseSourceDriver = SimBodyGlucoseSourceDriver(this)

    private val _glucoseReadings = MutableSharedFlow<BgReading>(
        replay = 1,
        extraBufferCapacity = 16
    )

    private var heartbeat: SimBodyHeartbeat? = null

    override val pluginId: String = PLUGIN_ID
    override val neededPermissions: Collection<String> = emptyList()

    override fun setup(context: PluginContext) {
        val registry = context as SystemRegistry
        bodyModel.loadState()
        pumpDevice.loadState()
        heartbeat = SimBodyHeartbeat(
            wakeService = registry.wakeService,
            bodyModel = bodyModel,
            pumpDevice = pumpDevice,
            onBgReading = { _glucoseReadings.tryEmit(it) }
        )
    }

    override fun initialize(context: PluginContext) {
        heartbeat?.start()
    }

    override suspend fun onResetToFactorySettings() = withContext(Dispatchers.IO) {
        heartbeat?.stop()
        database.clearAllTables()
        bodyModel.loadState()
        pumpDevice.loadState()
    }

    override suspend fun onSeedDefaultData() = withContext(Dispatchers.IO) {
        bodyModel.loadState()
        pumpDevice.loadState()
    }

    override suspend fun exportBackupData(
        includeHistory: Boolean,
        includeDiagnostics: Boolean
    ): Map<String, String>? = withContext(Dispatchers.IO) {
        val simBodyDao = database.impactDao()
        val pumpDao = database.pumpDao()

        val simulationState = simBodyDao.getSimulationState()?.let {
            SimulationStateBackupDto(
                lastSimulationTimestampMs = it.lastSimulationTimestamp.ms,
                exerciseIntensity = it.exerciseIntensity,
                stressLevel = it.stressLevel,
                illnessFactor = it.illnessFactor,
                isSensorEnabled = it.isSensorEnabled,
                sensorNoiseFactor = it.sensorNoiseFactor,
                sensorDrift = it.sensorDrift
            )
        }

        val pumpState = pumpDao.getPumpState()?.let {
            PumpStateBackupDto(
                batteryLevel = it.batteryLevel,
                reservoirLevel = it.reservoirLevel,
                isOccluded = it.isOccluded,
                isPrimed = it.isPrimed,
                hasHardwareError = it.hasHardwareError,
                isBroken = it.isBroken,
                isSuspended = it.isSuspended,
                lastBasalDeliveryTimestampMs = it.lastBasalDeliveryTimestamp.ms,
                tempBasalPercent = it.tempBasalPercent,
                tempBasalExpiryMs = it.tempBasalExpiry?.ms
            )
        }

        val bodyProfiles = simBodyDao.getActiveBodyProfile()?.let {
            listOf(
                BodyProfileBackupDto(
                    id = it.id,
                    name = it.name,
                    isfBlocks = it.isfBlocks,
                    crBlocks = it.crBlocks,
                    liverGlucoseOutputBlocks = it.liverGlucoseOutputBlocks,
                    isActive = it.isActive
                )
            )
        } ?: emptyList()

        val simHistory = if (includeHistory) {
            simBodyDao.getHistorySince(Timestamp(0)).map {
                SimHistoryBackupDto(
                    timestampMs = it.timestamp.ms,
                    bgMgDl = it.bgMgDl,
                    carbImpact = it.carbImpact,
                    insulinImpact = it.insulinImpact,
                    endogenousImpact = it.endogenousImpact,
                    exerciseImpact = it.exerciseImpact,
                    stressImpact = it.stressImpact
                )
            }
        } else emptyList()

        val simEvents = if (includeHistory) {
            simBodyDao.getEventsSince(Timestamp(0)).map {
                SimEventBackupDto(
                    id = it.id,
                    type = it.type,
                    timestampMs = it.timestamp.ms,
                    amountIu = it.amount.iu,
                    detailId = it.detailId,
                    insulinOriginName = it.insulinOrigin?.name
                )
            }
        } else emptyList()

        val pumpHistory = if (includeHistory) {
            pumpDao.getHistorySince(Timestamp(0)).map {
                PumpHistoryBackupDto(
                    id = it.id,
                    timestampMs = it.timestamp.ms,
                    amountIu = it.amount.iu,
                    deliveryTypeName = it.deliveryType.name
                )
            }
        } else emptyList()

        val backupDto = SimBodyBackupDto(
            simulationState = simulationState,
            pumpState = pumpState,
            bodyProfiles = bodyProfiles,
            simHistory = simHistory,
            simEvents = simEvents,
            pumpHistory = pumpHistory
        )

        val jsonString = json.encodeToString(backupDto)
        mapOf("simbody.json" to jsonString)
    }

    override suspend fun importBackupData(
        backupData: Map<String, String>,
        includeHistory: Boolean,
        includeDiagnostics: Boolean
    ) = withContext(Dispatchers.IO) {
        val jsonString = backupData["simbody.json"] ?: return@withContext
        val dto = runCatching {
            json.decodeFromString<SimBodyBackupDto>(jsonString)
        }.getOrNull() ?: return@withContext

        database.withTransaction {
            database.clearAllTables()

            dto.simulationState?.let { state ->
                database.impactDao().updateSimulationState(
                    SimulationStateEntity(
                        id = 0,
                        lastSimulationTimestamp = Timestamp(state.lastSimulationTimestampMs),
                        exerciseIntensity = state.exerciseIntensity,
                        stressLevel = state.stressLevel,
                        illnessFactor = state.illnessFactor,
                        isSensorEnabled = state.isSensorEnabled,
                        sensorNoiseFactor = state.sensorNoiseFactor,
                        sensorDrift = state.sensorDrift
                    )
                )
            }

            dto.pumpState?.let { state ->
                database.pumpDao().updatePumpState(
                    PumpStateEntity(
                        id = 0,
                        batteryLevel = state.batteryLevel,
                        reservoirLevel = state.reservoirLevel,
                        isOccluded = state.isOccluded,
                        isPrimed = state.isPrimed,
                        hasHardwareError = state.hasHardwareError,
                        isBroken = state.isBroken,
                        isSuspended = state.isSuspended,
                        lastBasalDeliveryTimestamp = Timestamp(state.lastBasalDeliveryTimestampMs),
                        tempBasalPercent = state.tempBasalPercent,
                        tempBasalExpiry = state.tempBasalExpiryMs?.let { Timestamp(it) }
                    )
                )
            }

            dto.bodyProfiles.forEach { profile ->
                database.impactDao().insertBodyProfile(
                    BodyProfileEntity(
                        id = profile.id,
                        name = profile.name,
                        isfBlocks = profile.isfBlocks,
                        crBlocks = profile.crBlocks,
                        liverGlucoseOutputBlocks = profile.liverGlucoseOutputBlocks,
                        isActive = profile.isActive
                    )
                )
            }

            if (includeHistory) {
                dto.simHistory.forEach { history ->
                    database.impactDao().insertHistory(
                        SimHistoryEntity(
                            timestamp = Timestamp(history.timestampMs),
                            bgMgDl = history.bgMgDl,
                            carbImpact = history.carbImpact,
                            insulinImpact = history.insulinImpact,
                            endogenousImpact = history.endogenousImpact,
                            exerciseImpact = history.exerciseImpact,
                            stressImpact = history.stressImpact
                        )
                    )
                }

                dto.simEvents.forEach { event ->
                    database.impactDao().insertEvent(
                        SimEventEntity(
                            id = event.id,
                            type = event.type,
                            timestamp = Timestamp(event.timestampMs),
                            amount = InsulinAmount(event.amountIu),
                            detailId = event.detailId,
                            insulinOrigin = event.insulinOriginName?.let { runCatching { InsulinOrigin.valueOf(it) }.getOrNull() }
                        )
                    )
                }

                dto.pumpHistory.forEach { history ->
                    database.pumpDao().insertHistoryEntry(
                        PumpHistoryEntity(
                            id = history.id,
                            timestamp = Timestamp(history.timestampMs),
                            amount = InsulinAmount(history.amountIu),
                            deliveryType = PumpDeliveryType.valueOf(history.deliveryTypeName)
                        )
                    )
                }
            }
        }

        bodyModel.loadState()
        pumpDevice.loadState()
    }

    fun getGlucoseSource(): GlucoseSource = SimBodyGlucoseSource(
        glucoseReadings = _glucoseReadings.asSharedFlow(),
        onStart = { heartbeat?.triggerImmediateStep() }
    )
    fun getInsulinPump(): InsulinPump = SimBodyInsulinPump(pumpDevice)

    companion object {
        const val PLUGIN_ID = "de.dh.daps.plugin.simbody"
    }
}