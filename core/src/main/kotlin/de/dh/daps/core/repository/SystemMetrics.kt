package de.dh.daps.core.repository

import de.dh.daps.common.domain.model.therapy.Tick
import de.dh.daps.common.domain.model.therapy.Timestamp

data class WakeupMetric(
    val tag: String,
    val wakeupId: UInt?,
    val scheduledTime: Timestamp,
    val dispatchTime: Timestamp,
    val onWakeupStartTime: Timestamp?,
    val onWakeupEndTime: Timestamp?
)

data class TickHandlerMetric(
    val tick: Tick,
    val handlerName: String,
    val startTime: Timestamp,
    val endTime: Timestamp
)