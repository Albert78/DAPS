package de.dh.daps.common.model.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A memory-efficient, type-safe representation of a timestamp on the pump's internal clock (in milliseconds).
 *
 * This represents raw hardware time from or sent to the insulin pump. It MUST be converted
 * to a system [Timestamp] using [toSystemTimestamp] before being used in application domain logic.
 */
@JvmInline
value class PumpTimestamp(val rawMs: Long) : Comparable<PumpTimestamp> {
    override fun compareTo(other: PumpTimestamp): Int = rawMs.compareTo(other.rawMs)

    operator fun minus(other: PumpTimestamp): Long = rawMs - other.rawMs
    operator fun plus(ms: Long): PumpTimestamp = PumpTimestamp(rawMs + ms)
    operator fun minus(ms: Long): PumpTimestamp = PumpTimestamp(rawMs - ms)

    /**
     * Converts this pump-local timestamp into a system [Timestamp].
     *
     * @param pumpTimeOffsetMs The offset between pump UTC time and phone system time:
     *                         (pumpUtcMillis - sysTime)
     */
    fun toSystemTimestamp(pumpTimeOffsetMs: Long): Timestamp {
        return Timestamp(rawMs - pumpTimeOffsetMs)
    }

    override fun toString(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(rawMs))
    }

    companion object {
        val ZERO = PumpTimestamp(0)

        /**
         * Creates a [PumpTimestamp] from a system [Timestamp] and the known [pumpTimeOffsetMs].
         */
        fun fromSystemTimestamp(systemTimestamp: Timestamp, pumpTimeOffsetMs: Long): PumpTimestamp {
            return PumpTimestamp(systemTimestamp.ms + pumpTimeOffsetMs)
        }
    }
}