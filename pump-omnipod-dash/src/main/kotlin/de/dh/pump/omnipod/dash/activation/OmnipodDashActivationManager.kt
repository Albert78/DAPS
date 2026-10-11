package de.dh.pump.omnipod.dash.activation

import de.dh.daps.common.model.data.InsulinProfile
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.ble.PodBleConnectionState
import de.dh.pump.omnipod.protocol.command.GetStatusCommand
import de.dh.pump.omnipod.protocol.command.ProgramAlertsCommand
import de.dh.pump.omnipod.protocol.command.ProgramBasalCommand
import de.dh.pump.omnipod.protocol.command.ProgramBolusCommand
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.definition.AlertConfiguration
import de.dh.pump.omnipod.protocol.definition.AlertTrigger
import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.BasalProgram
import de.dh.pump.omnipod.protocol.definition.BeepRepetitionType
import de.dh.pump.omnipod.protocol.definition.BeepType
import de.dh.pump.omnipod.protocol.definition.PodStatus
import de.dh.pump.omnipod.protocol.definition.ProgramReminder
import de.dh.pump.omnipod.protocol.response.DefaultStatusResponse
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import de.dh.pump.omnipod.protocol.util.PodPulseCalculator
import de.dh.pump.omnipod.protocol.util.ProfileToBasalScheduleConverter
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages the Pod activation lifecycle, executing BLE hardware commands
 * for priming, cannula insertion, alarm programming, basal profile setup,
 * and state recovery.
 */
class OmnipodDashActivationManager(
    private val podStateManager: OmnipodDashPodStateManagerImpl,
    private val bleManager: OmnipodDashBleManager
) {
    private var sequenceNumber: Short = 1

    private fun nextSequenceNumber(): Short {
        val seq = sequenceNumber
        sequenceNumber = ((sequenceNumber + 1) and 0x0F).toShort()
        return seq
    }

    private fun getNonce(): Int = podStateManager.lastNonce ?: DEFAULT_NONCE

    private suspend fun ensureBleConnected(): OmnipodDashBleManager {
        val address = podStateManager.bluetoothAddress
        if (address.isNullOrEmpty()) {
            throw IllegalStateException("No Pod found: Please fill the Pod with at least 85 U of insulin and place it next to your device.")
        }
        if (bleManager.connectionState.value != PodBleConnectionState.CONNECTED) {
            val connectResult = bleManager.connect(address)
            if (connectResult.isFailure) {
                throw connectResult.exceptionOrNull()
                    ?: IllegalStateException("Connection to Pod ($address) failed.")
            }
        }
        return bleManager
    }

    /**
     * Executes Part 1 of pod setup: Priming bolus, status polling
     * for PRIME_COMPLETED / CLUTCH_DRIVE_ENGAGED, and programming initial alarms.
     */
    suspend fun performFillAndPrime(): Result<Unit> {
        try {
            val manager = ensureBleConnected()

            // 2.1 Send Priming Bolus
            if (podStateManager.activationProgress.isBefore(ActivationProgress.PRIMING)) {
                val uniqueId = podStateManager.uniqueId ?: 0
                val primePulses = PodPulseCalculator.unitsToPulses(PRIMING_BOLUS_UNITS)
                val cmd = ProgramBolusCommand(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber(),
                    nonce = getNonce(),
                    pulseCount = primePulses
                )
                manager.sendCommand(cmd).getOrThrow()
                podStateManager.activationProgress = ActivationProgress.PRIMING
                podStateManager.saveToPreferences()
            }

            // 2.2 Priming Verification via status polling
            if (podStateManager.activationProgress.isBefore(ActivationProgress.PRIME_COMPLETED)) {
                pollPodStatusUntil { status ->
                    status == PodStatus.CLUTCH_DRIVE_ENGAGED ||
                            status == PodStatus.BASAL_PROGRAM_SET ||
                            status.isRunning()
                }
                podStateManager.activationProgress = ActivationProgress.PRIME_COMPLETED
                podStateManager.saveToPreferences()
            }

            // Program initial alerts (Lump of coal / Low reservoir)
            if (podStateManager.activationProgress.isBefore(ActivationProgress.PROGRAMMED_LOW_RESERVOIR_ALERTS)) {
                val uniqueId = podStateManager.uniqueId ?: 0
                val alerts = listOf(
                    AlertConfiguration(
                        type = AlertType.EXPIRATION,
                        enabled = true,
                        durationInMinutes = 55,
                        autoOff = false,
                        trigger = AlertTrigger.TimerTrigger(5),
                        beepType = BeepType.FOUR_TIMES_BIP_BEEP,
                        beepRepetition = BeepRepetitionType.REPEAT_5
                    ),
                    AlertConfiguration(
                        type = AlertType.LOW_RESERVOIR,
                        enabled = true,
                        durationInMinutes = 0,
                        autoOff = false,
                        trigger = AlertTrigger.ReservoirVolumeTrigger(20.toShort()),
                        beepType = BeepType.FOUR_TIMES_BIP_BEEP,
                        beepRepetition = BeepRepetitionType.ONCE
                    )
                )
                val alertsCmd = ProgramAlertsCommand(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber(),
                    nonce = getNonce(),
                    alertConfigurations = alerts
                )
                manager.sendCommand(alertsCmd).getOrThrow()
                podStateManager.activationProgress = ActivationProgress.PROGRAMMED_LOW_RESERVOIR_ALERTS
                podStateManager.saveToPreferences()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Executes Part 2 of pod setup: Cannula insertion bolus
     * and status polling for CANNULA_INSERTED / BASAL_PROGRAM_SET.
     */
    suspend fun performApplyAndInsert(): Result<Unit> {
        try {
            val manager = ensureBleConnected()

            // 2.3 Send Cannula Insertion Bolus
            if (podStateManager.activationProgress.isBefore(ActivationProgress.INSERTING_CANNULA)) {
                val uniqueId = podStateManager.uniqueId ?: 0
                val insertPulses = PodPulseCalculator.unitsToPulses(CANNULA_INSERTION_BOLUS_UNITS)
                val cmd = ProgramBolusCommand(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber(),
                    nonce = getNonce(),
                    pulseCount = insertPulses
                )
                manager.sendCommand(cmd).getOrThrow()
                podStateManager.activationProgress = ActivationProgress.INSERTING_CANNULA
                podStateManager.saveToPreferences()
            }

            // 2.4 Cannula Verification via status polling
            if (podStateManager.activationProgress.isBefore(ActivationProgress.CANNULA_INSERTED)) {
                pollPodStatusUntil { status ->
                    status == PodStatus.BASAL_PROGRAM_SET || status.isRunning()
                }
                podStateManager.activationProgress = ActivationProgress.CANNULA_INSERTED
                podStateManager.saveToPreferences()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Finalizes pod activation: Program expiration alerts, active basal schedule,
     * and transition to COMPLETED.
     */
    suspend fun performConfirmAndActivate(profile: InsulinProfile? = null): Result<Unit> {
        try {
            val manager = ensureBleConnected()

            val uniqueId = podStateManager.uniqueId ?: 0

            // 2.5 Initial Basal Profile & Expiration Alarms
            if (podStateManager.activationProgress.isBefore(ActivationProgress.UPDATED_EXPIRATION_ALERTS)) {
                val alerts = listOf(
                    AlertConfiguration(
                        type = AlertType.EXPIRATION,
                        enabled = true,
                        durationInMinutes = (71 * 60).toShort(),
                        autoOff = false,
                        trigger = AlertTrigger.TimerTrigger((71 * 60).toShort()),
                        beepType = BeepType.FOUR_TIMES_BIP_BEEP,
                        beepRepetition = BeepRepetitionType.REPEAT_3
                    ),
                    AlertConfiguration(
                        type = AlertType.EXPIRATION_IMMINENT,
                        enabled = true,
                        durationInMinutes = 0,
                        autoOff = false,
                        trigger = AlertTrigger.TimerTrigger((72 * 60).toShort()),
                        beepType = BeepType.FOUR_TIMES_BIP_BEEP,
                        beepRepetition = BeepRepetitionType.REPEAT_4
                    )
                )
                val alertCmd = ProgramAlertsCommand(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber(),
                    nonce = getNonce(),
                    alertConfigurations = alerts,
                    multiCommandFlag = true
                )
                manager.sendCommand(alertCmd).getOrThrow()
                podStateManager.activationProgress = ActivationProgress.UPDATED_EXPIRATION_ALERTS
                podStateManager.saveToPreferences()
            }

            if (podStateManager.activationProgress.isBefore(ActivationProgress.PROGRAMMED_BASAL)) {
                val basalProgram = createBasalProgramFromProfileOrDefault(profile)
                val basalCmd = ProgramBasalCommand.create(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber(),
                    nonce = getNonce(),
                    basalProgram = basalProgram,
                    programReminder = ProgramReminder(atStart = false, atEnd = false, atInterval = 0),
                    currentTime = Date()
                )
                manager.sendCommand(basalCmd).getOrThrow()
                podStateManager.basalProgram = basalProgram
                podStateManager.activationProgress = ActivationProgress.PROGRAMMED_BASAL
                podStateManager.saveToPreferences()
            }

            podStateManager.activatedAtTimestampMs = System.currentTimeMillis()
            podStateManager.activationProgress = ActivationProgress.COMPLETED
            podStateManager.saveToPreferences()

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * 2.6 Activation Recovery: Query current Pod status via BLE and restore
     * activation progress state using recoverActivationFromPodStatus.
     */
    suspend fun recoverActivationProgress(): ActivationProgress {
        if (!podStateManager.bluetoothAddress.isNullOrEmpty()) {
            try {
                ensureBleConnected()
                val uniqueId = podStateManager.uniqueId ?: 0
                val cmd = GetStatusCommand(
                    uniqueId = uniqueId,
                    sequenceNumber = nextSequenceNumber()
                )
                val sendResult = bleManager.sendCommand(cmd)
                if (sendResult.isSuccess) {
                    val bytes = sendResult.getOrThrow()
                    if (bytes.size >= 10) {
                        val response = DefaultStatusResponse(bytes)
                        podStateManager.updateFromDefaultStatusResponse(response)
                        podStateManager.saveToPreferences()
                    }
                }
            } catch (_: Exception) {
                // Return persisted activation progress on network/BLE failure
            }
        }
        return podStateManager.activationProgress
    }

    private suspend fun pollPodStatusUntil(
        maxAttempts: Int = 5,
        delayMs: Long = 1000L,
        predicate: (PodStatus) -> Boolean
    ) {
        if (podStateManager.bluetoothAddress.isNullOrEmpty()) return

        val uniqueId = podStateManager.uniqueId ?: 0
        for (attempt in 1..maxAttempts) {
            val cmd = GetStatusCommand(
                uniqueId = uniqueId,
                sequenceNumber = nextSequenceNumber()
            )
            val sendResult = bleManager.sendCommand(cmd)
            if (sendResult.isSuccess) {
                val bytes = sendResult.getOrThrow()
                if (bytes.size >= 10) {
                    val response = DefaultStatusResponse(bytes)
                    podStateManager.updateFromDefaultStatusResponse(response)
                    if (predicate(response.podStatus)) {
                        return
                    }
                }
            }
            delay(delayMs.milliseconds)
        }
    }

    private fun createBasalProgramFromProfileOrDefault(profile: InsulinProfile?): BasalProgram {
        if (profile != null) {
            val pulses = ProfileToBasalScheduleConverter.convertProfileToHalfHourPulses(profile)
            val segments = pulses.mapIndexed { index, pulseCount ->
                BasalProgram.Segment(
                    startSlotIndex = (index).toShort(),
                    endSlotIndex = (index + 1).toShort(),
                    basalRateInHundredthUnitsPerHour = ((pulseCount.toDouble() / 20.0) * 100).toInt()
                )
            }
            return BasalProgram(segments)
        } else {
            val defaultSegments = listOf(
                BasalProgram.Segment(0.toShort(), 48.toShort(), 5)
            )
            return BasalProgram(defaultSegments)
        }
    }

    companion object {
        private const val PRIMING_BOLUS_UNITS = 2.6
        private const val CANNULA_INSERTION_BOLUS_UNITS = 0.5
        private const val DEFAULT_NONCE = 0x49534141
    }
}