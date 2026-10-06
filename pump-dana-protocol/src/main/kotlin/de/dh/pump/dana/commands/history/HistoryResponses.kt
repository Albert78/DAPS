package de.dh.pump.dana.commands.history

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsResponse
import de.dh.daps.common.model.data.PumpTimestamp

/**
 * Base type for one packet returned by a classic Dana history command.
 *
 * History transfers are multi-packet streams: each notification is either one decoded record or an
 * end marker with the pump's result code.
 */
sealed interface DanaRsHistoryResponse : DanaRsResponse

/**
 * Terminal packet for a history transfer.
 */
data class HistoryEndResponse(
    override val status: PumpStatus,
    val errorCode: Int,
    val totalCount: Int?,
) : DanaRsHistoryResponse

/**
 * Wire-level DanaRS history record types used by the classic history commands.
 */
enum class DanaRsHistoryRecordKind(val wireValue: Int) {
    BOLUS(0x02),
    DAILY(0x03),
    PRIME(0x04),
    REFILL(0x05),
    GLUCOSE(0x06),
    CARBOHYDRATE(0x07),
    TEMP_BASAL(0x08),
    SUSPEND(0x09),
    ALARM(0x0a),
    BASAL_HOUR(0x0b),
    TEMP_BASAL_APS(0x99),
    UNKNOWN(-1);

    companion object {
        fun fromWireValue(value: Int): DanaRsHistoryRecordKind =
            entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

/**
 * Types of bolus delivery in classic history.
 */
enum class DanaRsHistoryBolusType {
    STEP,
    EXTENDED,
    DUAL_STEP,
    DUAL_EXTENDED,
    NONE
}

/**
 * Types of pump alarms in classic history.
 */
enum class DanaRsHistoryAlarmType {
    BASAL_COMPARE,
    EMPTY_RESERVOIR,
    CHECK,
    OCCLUSION,
    BASAL_MAX,
    DAILY_MAX,
    LOW_BATTERY,
    SHUTDOWN,
    NONE
}

/**
 * Generic ON/OFF states for history records (e.g. Suspend or Temp Basal).
 */
enum class DanaRsHistoryOnOffState {
    ON,
    OFF
}

/**
 * Decoded classic history record.
 *
 * The original format reuses several bytes for different record types. Fields that are not meaningful
 * for the current [kind] are left null instead of inventing placeholder values.
 */
data class DanaRsHistoryRecord(
    /**
     * Raw record code from the pump.
     */
    val recordCode: Int,
    /**
     * Type of the history record.
     */
    val kind: DanaRsHistoryRecordKind,
    /**
     * Local timestamp of the record on the pump clock.
     */
    val timestamp: PumpTimestamp,
    /**
     * Primary numeric value.
     *
     * - Units (U): BOLUS, PRIME, REFILL, BASAL_HOUR, TEMP_BASAL (remaining reservoir capacity), ALARM.
     * - mg/dL: GLUCOSE.
     * - Grams (g): CARBOHYDRATE.
     */
    val value: Double?,
    /**
     * Duration in minutes.
     *
     * Set for: BOLUS (for extended/dual portions).
     */
    val durationMinutes: Int?,
    /**
     * Type of bolus.
     *
     * Set for: BOLUS.
     */
    val bolusType: DanaRsHistoryBolusType?,
    /**
     * Alarm type.
     *
     * Set for: ALARM.
     */
    val alarm: DanaRsHistoryAlarmType?,
    /**
     * Generic state (On/Off).
     *
     * Set for: SUSPEND, TEMP_BASAL.
     */
    val state: DanaRsHistoryOnOffState?,
    /**
     * Daily basal total in Units (U).
     *
     * Set for: DAILY.
     */
    val dailyBasalUnits: Double?,
    /**
     * Daily bolus total in Units (U).
     *
     * Set for: DAILY.
     */
    val dailyBolusUnits: Double?,
    /**
     * Raw 8-bit history sub-code.
     */
    val rawHistoryCode: Int,
    /**
     * Raw 16-bit primary value.
     */
    val rawValue: Int,
)

/**
 * One non-terminal history packet.
 */
data class HistoryRecordResponse(
    override val status: PumpStatus,
    val record: DanaRsHistoryRecord,
) : DanaRsHistoryResponse

/**
 * Complete result of a classic history transfer after the terminal packet has been received.
 */
data class DanaRsHistoryResult(
    val records: List<DanaRsHistoryRecord>,
    val end: HistoryEndResponse,
)