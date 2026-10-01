package de.dh.daps.common.model

import android.content.Context
import de.dh.daps.common.DEFAULT_BASAL_UNITS_PER_HOUR
import de.dh.daps.common.DEFAULT_CR_GRAM_PER_UNIT
import de.dh.daps.common.DEFAULT_ISF_MGDL_PER_UNIT
import de.dh.daps.common.ID_INSULIN_ACTRAPID
import de.dh.daps.common.ID_INSULIN_APIDRA
import de.dh.daps.common.ID_INSULIN_FIASP
import de.dh.daps.common.ID_INSULIN_HUMALOG
import de.dh.daps.common.ID_INSULIN_LYUMJEV
import de.dh.daps.common.ID_INSULIN_NOVORAPID
import de.dh.daps.common.ID_MEAL_FAST
import de.dh.daps.common.ID_MEAL_HIGH_FAT
import de.dh.daps.common.ID_MEAL_SLOW
import de.dh.daps.common.ID_MEAL_STANDARD
import de.dh.daps.common.R
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.TherapyAdjustment

val FAST_KE_DEFAULT_PEAK = Minutes(25)

fun getDefaultInsulinTypes(context: Context): List<InsulinType> = listOf(
    InsulinType(
        id = ID_INSULIN_NOVORAPID,
        name = context.getString(R.string.insulin_type_novorapid_name),
        dia = Minutes.ofHours(5),
        peak = Minutes(75),
        defaultConcentration = InsulinConcentration.U100,
    ),
    InsulinType(
        id = ID_INSULIN_FIASP,
        name = context.getString(R.string.insulin_type_fiasp_name),
        dia = Minutes.ofHours(4),
        peak = Minutes(55),
        defaultConcentration = InsulinConcentration.U100,
    ),
    InsulinType(
        id = ID_INSULIN_HUMALOG,
        name = context.getString(R.string.insulin_type_humalog_name),
        dia = Minutes.ofHours(5),
        peak = Minutes(60),
        defaultConcentration = InsulinConcentration.U100,
    ),
    InsulinType(
        id = ID_INSULIN_LYUMJEV,
        name = context.getString(R.string.insulin_type_lyumjev_name),
        dia = Minutes.ofHours(5),
        peak = Minutes(45),
        defaultConcentration = InsulinConcentration.U100,
    ),
    InsulinType(
        id = ID_INSULIN_APIDRA,
        name = context.getString(R.string.insulin_type_apidra_name),
        dia = Minutes.ofHours(5),
        peak = Minutes(60),
        defaultConcentration = InsulinConcentration.U100,
    ),
    InsulinType(
        id = ID_INSULIN_ACTRAPID,
        name = context.getString(R.string.insulin_type_actrapid_name),
        dia = Minutes.ofHours(8),
        peak = Minutes(120),
        defaultConcentration = InsulinConcentration.U100,
    )
)

fun getDefaultFastMealType(context: Context): MealType = MealType(
    id = ID_MEAL_FAST,
    name = context.getString(R.string.meal_type_fast_carbs_name),
    components = listOf(
        CarbCurveComponentData(weight = 100, peakMinutes = FAST_KE_DEFAULT_PEAK)
    ),
    cat = Minutes(90),
    sortOrder = 0
)

fun getDefaultStandardMealType(context: Context): MealType = MealType(
    id = ID_MEAL_STANDARD,
    name = context.getString(R.string.meal_type_standard_meal_name),
    components = listOf(
        CarbCurveComponentData(weight = 70, peakMinutes = Minutes(75)),
        CarbCurveComponentData(weight = 30, peakMinutes = Minutes(150))
    ),
    cat = Minutes.ofHours(4),
    sortOrder = 1
)

fun getDefaultHighFatMealType(context: Context): MealType = MealType(
    id = ID_MEAL_HIGH_FAT,
    name = context.getString(R.string.meal_type_high_fat_meal_name),
    components = listOf(
        CarbCurveComponentData(weight = 35, peakMinutes = Minutes(60)),
        CarbCurveComponentData(weight = 65, peakMinutes = Minutes(240))
    ),
    cat = Minutes.ofHours(6),
    sortOrder = 2
)

fun getDefaultSlowMealType(context: Context): MealType = MealType(
    id = ID_MEAL_SLOW,
    name = context.getString(R.string.meal_type_slow_meal_name),
    components = listOf(
        CarbCurveComponentData(weight = 40, peakMinutes = Minutes(120)),
        CarbCurveComponentData(weight = 60, peakMinutes = Minutes(300))
    ),
    cat = Minutes.ofHours(8),
    sortOrder = 3
)

fun getDefaultMealTypes(context: Context): List<MealType> = listOf(
    getDefaultFastMealType(context),
    getDefaultStandardMealType(context),
    getDefaultHighFatMealType(context),
    getDefaultSlowMealType(context)
)

fun getDefaultInsulinProfile(context: Context, insulinType: InsulinType): InsulinProfile = InsulinProfile(
    name = context.getString(R.string.profile_default_normal_name),
    basalBlocks = listOf(
        Block(
            Minutes.ofHours(24),
            DEFAULT_BASAL_UNITS_PER_HOUR
        )
    ),
    isfBlocks = listOf(Block(Minutes.ofHours(24), DEFAULT_ISF_MGDL_PER_UNIT)),
    crBlocks = listOf(Block(Minutes.ofHours(24), DEFAULT_CR_GRAM_PER_UNIT)),
    insulinType = insulinType,
    insulinConcentration = insulinType.defaultConcentration,
    dia = insulinType.dia,
    peak = insulinType.peak
)

fun getDefaultTherapyAdjustments(context: Context): List<TherapyAdjustment> = listOf(
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_neutral)),
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_biking), percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_climbing), percentage = -40, targetBgMgDl = 160, lowThresholdMgDl = 110),
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_alcohol), percentage = -15, targetBgMgDl = 120, lowThresholdMgDl = 80),
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_sick), percentage = 30, targetBgMgDl = 100, lowThresholdMgDl = 70),
    TherapyAdjustment(name = context.getString(R.string.therapy_adjustment_preset_stress), percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
)