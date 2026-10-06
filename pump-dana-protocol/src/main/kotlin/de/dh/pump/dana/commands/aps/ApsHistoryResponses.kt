package de.dh.pump.dana.commands.aps

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsResponse
import de.dh.daps.common.model.data.PumpTimestamp

sealed interface ApsHistoryEventsChunk : DanaRsResponse

/**
 * Wire-level APS history event types used by DanaRS-compatible pumps.
 */
enum class ApsHistoryEventKind(val wireValue: Int) {
    TEMP_START(1),
    TEMP_STOP(2),
    EXTENDED_START(3),
    EXTENDED_STOP(4),
    BOLUS(5),
    DUAL_BOLUS(6),
    DUAL_EXTENDED_START(7),
    DUAL_EXTENDED_STOP(8),
    SUSPEND_ON(9),
    SUSPEND_OFF(10),
    REFILL(11),
    PRIME(12),
    PROFILE_CHANGE(13),
    CARBS(14),
    PRIME_CANNULA(15),
    TIME_CHANGE(16),
    UNKNOWN(-1);

    companion object {
        fun fromWireValue(value: Int): ApsHistoryEventKind =
            entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

/**
 * One decoded APS history event.
 *
 * The pump always provides two 16-bit parameters. Their semantic meaning depends on [kind], so the
 * raw values are retained and common derived values are exposed where the reference protocol defines
 * them.
 */
data class ApsHistoryEvent(
    /**
     * Category of the event.
     */
    val kind: ApsHistoryEventKind,
    /**
     * Raw record code from the pump.
     */
    val recordCode: Int,
    /**
     * Event time on the pump's clock.
     */
    val timestamp: PumpTimestamp,
    /**
     * ID of the pump (typically serial number based).
     */
    val pumpId: Long,
    /**
     * Raw 16-bit parameter 1.
     */
    val param1: Int,
    /**
     * Raw 16-bit parameter 2.
     */
    val param2: Int,
    /**
     * Decoded insulin amount in Units (U).
     *
     * Set for: BOLUS, DUAL_BOLUS, EXTENDED_START, EXTENDED_STOP, DUAL_EXTENDED_START,
     * DUAL_EXTENDED_STOP, REFILL, PRIME, PRIME_CANNULA.
     */
    val insulinUnits: Double?,
    /**
     * Decoded duration in minutes.
     *
     * Set for: TEMP_START, EXTENDED_START, EXTENDED_STOP, DUAL_BOLUS, DUAL_EXTENDED_START,
     * DUAL_EXTENDED_STOP.
     */
    val durationMinutes: Int?,
    /**
     * Decoded ratio in percent.
     *
     * Set for: TEMP_START.
     */
    val ratioPercent: Int?,
    /**
     * Decoded carbohydrate amount in grams (g).
     *
     * Set for: CARBS.
     */
    val carbohydrateGrams: Int?,
    /**
     * Decoded delivery rate in Units per Hour (U/h).
     *
     * Set for: PROFILE_CHANGE.
     */
    val currentRateUnitsPerHour: Double?,
    /**
     * Previous pump timestamp.
     *
     * Set for: TIME_CHANGE.
     */
    val previousTimestamp: PumpTimestamp?,
)

data class ApsHistoryEventChunk(
    override val status: PumpStatus,
    val events: List<ApsHistoryEvent>,
) : ApsHistoryEventsChunk

data class ApsHistoryEndChunk(
    override val status: PumpStatus = PumpStatus.OK,
) : ApsHistoryEventsChunk

/**
 * Final APS history result after all event packets have been received and normalized.
 */
data class ApsHistoryEventsResult(
    val events: List<ApsHistoryEvent>,
)