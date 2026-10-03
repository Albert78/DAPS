package de.dh.daps.common

import de.dh.daps.common.model.InsulinAmount

// Phone Battery Thresholds (%)
const val PHONE_BATTERY_LOW_THRESHOLD = 15
const val PHONE_BATTERY_WARNING_THRESHOLD = 30

// Pump Battery Thresholds (%)
const val PUMP_BATTERY_LOW_THRESHOLD = 15
const val PUMP_BATTERY_WARNING_THRESHOLD = 20

// Pump Reservoir Thresholds (IU)
val PUMP_RESERVOIR_LOW_THRESHOLD = InsulinAmount(10.0)
val PUMP_RESERVOIR_WARNING_THRESHOLD = InsulinAmount(20.0)

const val ID_UNDEFINED = 0L

const val SECONDS_PER_MINUTE = 60
const val MINUTES_PER_HOUR = 60
const val HOURS_PER_DAY = 24
const val MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY

const val MS_PER_MINUTE = SECONDS_PER_MINUTE * 1000L
const val MS_PER_HOUR = MINUTES_PER_HOUR * MS_PER_MINUTE
const val MS_PER_DAY = MS_PER_HOUR * HOURS_PER_DAY

// Connection Thresholds (Minutes)
const val CONNECTION_WARNING_THRESHOLD_MINUTES = 20
const val CONNECTION_BAD_THRESHOLD_MINUTES = 60

// Core Calculation Thresholds (Minutes)
const val CORE_CALCULATION_WARNING_THRESHOLD_MINUTES = 10
const val CORE_CALCULATION_BAD_THRESHOLD_MINUTES = 20

// Glucose Reading Thresholds (Minutes)
const val BG_READING_WARNING_THRESHOLD_MINUTES = 10
const val BG_READING_BAD_THRESHOLD_MINUTES = 20

// Sensor Expiration Thresholds (Hours)
const val SENSOR_EXPIRATION_WARNING_THRESHOLD_HOURS = 24

// Pod Change Thresholds (Hours)
const val CANNULA_CHANGE_WARNING_THRESHOLD_HOURS = 24

const val ID_INSULIN_NOVORAPID = "9d860e7e-8c88-466d-a7f4-3e91851e3c88"
const val ID_INSULIN_FIASP = "4e0e9803-0c48-433b-8f7d-2b4f2c96791a"
const val ID_INSULIN_HUMALOG = "2c5a3d4f-1234-4567-89ab-cdef01234567"
const val ID_INSULIN_LYUMJEV = "3d6b4e5f-2345-5678-9abc-def012345678"
const val ID_INSULIN_APIDRA = "4e7c5f6a-3456-6789-abcd-ef0123456789"

const val ID_MEAL_FAST = "b13c3b03-4f9e-4e4b-8e1e-1f8d4c96791a"
const val ID_MEAL_STANDARD = "f2a7a403-4f9e-4e4b-8e1e-2f8d4c96791a"
const val ID_MEAL_HIGH_FAT = "a3b8b503-4f9e-4e4b-8e1e-3f8d4c96791a"
const val ID_MEAL_SLOW = "d4c9c603-4f9e-4e4b-8e1e-4f8d4c96791a"

const val BASAL_MIN = 0.1
const val BASAL_MAX = 10.0

const val ISF_MIN = 10.0
const val ISF_MAX = 300.0

const val CR_MIN = 1.0
const val CR_MAX = 100.0

const val TARGET_MIN = 70
const val TARGET_MAX = 180

const val LOW_THRESHOLD_MIN = 50
const val LOW_THRESHOLD_MAX = 150

const val ADJUSTMENT_PERCENTAGE_MIN = -100
const val ADJUSTMENT_PERCENTAGE_MAX = 200

const val CARBS_KE_MIN = 0.0
const val CARBS_KE_MAX = 30.0
const val CARBS_GRAMS_MIN = CARBS_KE_MIN * 10.0
const val CARBS_GRAMS_MAX = CARBS_KE_MAX * 10.0

const val BOLUS_MIN = 0.05
const val BOLUS_MAX = 50.0

const val BG_DELTA_MAX = 1000.0

const val DEFAULT_BASAL_UNITS_PER_HOUR = 0.5
const val DEFAULT_ISF_MGDL_PER_UNIT = 50.0
const val DEFAULT_CR_GRAM_PER_UNIT = 10.0
const val DEFAULT_BG_TARGET_MGDL: Short = 100
const val DEFAULT_BG_LOW_THRESHOLD_MGDL: Short = 70

const val DEFAULT_DIA_MINUTES = 300
const val DEFAULT_PEAK_MINUTES = 75

const val MEAL_EDIT_THRESHOLD_HOURS = 12
const val MEAL_ADD_THRESHOLD_HOURS = 8

const val HISTORICAL_MEAL_MAX_PAST_MINUTES = 5
const val MEAL_REMINDER_MIN_FUTURE_MINUTES = 2

const val METABOLIC_EVENTS_HISTORY_HOURS = 10