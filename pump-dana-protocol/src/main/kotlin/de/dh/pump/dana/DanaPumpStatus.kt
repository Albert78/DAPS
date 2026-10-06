package de.dh.pump.dana

import de.dh.pump.PumpStatus

/**
 * Dana-specific response status codes (0x00..0x05) mapped to generic domain [PumpStatus].
 */
enum class DanaPumpStatus(
    val code: Int,
    val domainStatus: PumpStatus,
) {
    OK(0x00, PumpStatus.OK),
    REJECTED(0x01, PumpStatus.REJECTED),
    BUSY(0x02, PumpStatus.BUSY),
    INVALID_PARAMETER(0x03, PumpStatus.INVALID_PARAMETER),
    NOT_AUTHORIZED(0x04, PumpStatus.NOT_AUTHORIZED),
    DEVICE_ERROR(0x05, PumpStatus.DEVICE_ERROR);

    companion object {
        const val UNKNOWN_CODE = 0xFF

        fun fromCode(code: Int): PumpStatus =
            entries.firstOrNull { it.code == code }?.domainStatus ?: PumpStatus.UNKNOWN

        fun parse(code: Int): DanaPumpStatus? =
            entries.firstOrNull { it.code == code }
    }
}