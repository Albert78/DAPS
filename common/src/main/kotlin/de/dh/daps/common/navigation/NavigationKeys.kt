package de.dh.daps.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable object DashboardRoute : NavKey
@Serializable object HistoryRoute : NavKey
@Serializable object PermissionsRoute : NavKey
@Serializable object PreferencesMainRoute : NavKey
@Serializable object AppDataRoute : NavKey
@Serializable object InsulinProfileEditorRoute : NavKey
@Serializable object CurrentTherapySettingsRoute : NavKey
@Serializable data class MealCorrectionBolusRoute(
    val prefilledCarbsInG: Double? = null
) : NavKey
@Serializable data class HistoricalMealRoute(val mealId: Long) : NavKey
@Serializable object MealsRoute : NavKey
@Serializable object MealTypesRoute : NavKey
@Serializable data class MealTypeEditorRoute(val mealTypeId: String? = null) : NavKey
@Serializable object InsulinHistoryRoute : NavKey
@Serializable object RemindersRoute : NavKey
@Serializable object PastRemindersRoute : NavKey
@Serializable object FoodDatabaseRoute : NavKey
@Serializable object BgEditorRoute : NavKey
@Serializable object TherapyAdjustmentRoute : NavKey
@Serializable object ScheduledTherapyAdjustmentRoute : NavKey
@Serializable object TherapyAdjustmentPresetsRoute : NavKey
@Serializable data class TherapyAdjustmentEditorRoute(val adjustmentId: Long? = null) : NavKey
const val SYSTEM_CONTROL_TAB_OVERVIEW = 0
const val SYSTEM_CONTROL_TAB_GLUCOSE_SOURCE = 1
const val SYSTEM_CONTROL_TAB_PUMP = 2

@Serializable data class SystemControlRoute(val initialTab: Int = SYSTEM_CONTROL_TAB_OVERVIEW) : NavKey
@Serializable object PumpSetupRoute : NavKey
@Serializable object GlucoseSourceSetupRoute : NavKey
@Serializable object PumpSettingsRoute : NavKey
@Serializable object GlucoseSourceSettingsRoute : NavKey
@Serializable object CoreDecisionsRoute : NavKey
@Serializable object MasterDataRoute : NavKey
@Serializable object InsulinTypesRoute : NavKey
@Serializable data class InsulinTypeEditorRoute(val insulinTypeId: String? = null) : NavKey
@Serializable object AlarmProfilesRoute : NavKey
@Serializable data class AlarmProfileEditorRoute(val profileId: Long? = null) : NavKey
enum class ManualControlInitialDialog { NONE, BOLUS, TEMP_BASAL }

@Serializable data class ManualControlRoute(
    val initialDialog: ManualControlInitialDialog = ManualControlInitialDialog.NONE
) : NavKey