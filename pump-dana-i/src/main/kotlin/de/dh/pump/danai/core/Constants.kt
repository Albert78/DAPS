package de.dh.pump.danai.core

import de.dh.daps.common.model.InsulinAmount

const val WARNING_THRESHOLD_BATTERY_REMAINING_PERCENT = 20
val WARNING_THRESHOLD_RESERVOIR_REMAINING_UNITS = InsulinAmount(20.0)

const val DANA_I_MANUFACTURER = "Sooil"
val DANA_I_MIN_BASAL_RATE = InsulinAmount(0.04)
val DANA_I_MIN_BASAL_INCREMENT = InsulinAmount(0.01)
val DANA_I_MIN_BOLUS_AMOUNT = InsulinAmount(0.01)
val DANA_I_MIN_BOLUS_INCREMENT = InsulinAmount(0.01)
val DANA_I_MAX_BOLUS_VALUE = InsulinAmount(80.0)