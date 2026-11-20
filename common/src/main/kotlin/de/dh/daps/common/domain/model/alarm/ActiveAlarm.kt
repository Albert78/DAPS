package de.dh.daps.common.domain.model.alarm

import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import java.time.LocalDate

/**
 * Represents an active instance of an alarm, encapsulating both its type and any runtime details (payload).
 */
sealed interface ActiveAlarm {
    val type: AlarmType
    val severity: AlarmSeverity get() = type.defaultSeverity
    val category: AlarmCategory get() = type.category
    val isSafetyCritical: Boolean get() = type.isSafetyCritical

    sealed interface GlucoseAlarm : ActiveAlarm {
        val bgValue: BgValue
    }

    data class CriticalLowBg(override val bgValue: BgValue) : GlucoseAlarm {
        override val type = AlarmType.CRITICAL_LOW_BG
    }

    data class LowBg(override val bgValue: BgValue) : GlucoseAlarm {
        override val type = AlarmType.LOW_BG
    }

    data class HighBg(override val bgValue: BgValue) : GlucoseAlarm {
        override val type = AlarmType.HIGH_BG
    }

    data class CgmSignalLoss(val minutes: Int) : ActiveAlarm {
        override val type = AlarmType.CGM_SIGNAL_LOSS
    }

    data class PumpMaintenanceExceeded(val minutes: Int) : ActiveAlarm {
        override val type = AlarmType.PUMP_MAINTENANCE_EXCEEDED
    }

    data class PumpLowInsulin(val amount: InsulinAmount) : ActiveAlarm {
        override val type = AlarmType.PUMP_LOW_INSULIN
    }

    sealed interface CatheterChangeAlarm : ActiveAlarm {
        val recommendedChangeDate: LocalDate?
    }

    data class PumpCatheterChangeWarn(
        override val recommendedChangeDate: LocalDate?
    ) : CatheterChangeAlarm {
        override val type = AlarmType.PUMP_CATHETER_CHANGE_WARN
    }

    data class PumpCatheterChangeExpired(
        override val recommendedChangeDate: LocalDate?
    ) : CatheterChangeAlarm {
        override val type = AlarmType.PUMP_CATHETER_CHANGE_EXPIRED
    }

    sealed interface PatchChangeAlarm : ActiveAlarm {
        val expirationTimestamp: Timestamp?
    }

    data class PumpPatchChangeWarn(
        override val expirationTimestamp: Timestamp?
    ) : PatchChangeAlarm {
        override val type = AlarmType.PUMP_PATCH_CHANGE_WARN
    }

    data class PumpPatchChangeExpired(
        override val expirationTimestamp: Timestamp?
    ) : PatchChangeAlarm {
        override val type = AlarmType.PUMP_PATCH_CHANGE_EXPIRED
    }

    data class Generic(override val type: AlarmType) : ActiveAlarm
}