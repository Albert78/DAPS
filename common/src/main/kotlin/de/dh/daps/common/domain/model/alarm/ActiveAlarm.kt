package de.dh.daps.common.domain.model.alarm

import de.dh.daps.common.domain.model.data.BgValue

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

    data class Generic(override val type: AlarmType) : ActiveAlarm
}