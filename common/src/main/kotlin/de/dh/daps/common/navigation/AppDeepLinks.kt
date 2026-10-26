package de.dh.daps.common.navigation

object AppDeepLinks {
    const val SCHEME = "app"
    const val HOST = "daps.dh.de"
    const val BASE_URL = "$SCHEME://$HOST"

    const val PATH_DASHBOARD = "/dashboard"
    const val PATH_MEAL_PREFIX = "/meal/"
    const val PATH_MEAL_CORRECTION_BOLUS = "/mealcorrectionbolus"
    const val PATH_MANUAL_CONTROL = "/manualcontrol"
    const val PATH_SYSTEM_CONTROL = "/systemcontrol"
    const val PATH_SYSTEM_CONTROL_PUMP = "/systemcontrol/pump"

    const val DASHBOARD = "$BASE_URL$PATH_DASHBOARD"
    const val MEAL_CORRECTION_BOLUS = "$BASE_URL$PATH_MEAL_CORRECTION_BOLUS"
    const val MANUAL_CONTROL = "$BASE_URL$PATH_MANUAL_CONTROL"
    const val SYSTEM_CONTROL = "$BASE_URL$PATH_SYSTEM_CONTROL"
    const val SYSTEM_CONTROL_PUMP = "$BASE_URL$PATH_SYSTEM_CONTROL_PUMP"

    fun meal(mealId: Long): String = "$BASE_URL/meal/$mealId"
}