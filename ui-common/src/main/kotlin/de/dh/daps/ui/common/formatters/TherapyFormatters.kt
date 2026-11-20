package de.dh.daps.ui.common.formatters

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgReadingsInterval
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.CarbsUnit
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.ui.common.ModuloSteppingStrategy
import de.dh.daps.ui.common.SteppingStrategy
import java.util.Locale
import de.dh.daps.common.R as CommonR

val LocalGlucoseUnit = staticCompositionLocalOf<GlucoseUnit> {
    error("No GlucoseUnit provided")
}

val LocalCarbsUnit = staticCompositionLocalOf<CarbsUnit> {
    error("No CarbsUnit provided")
}

/////////////////////////////////////////////// Glucose & Therapy //////////////////////////////////////

/**
 * Composable UI function.
 * Returns the localized unit label for blood glucose (e.g. "mg/dL" or "mmol/L").
 *
 * @example glucoseUnitLabel(GlucoseUnit.MG_DL) -> "mg/dL"
 * @example glucoseUnitLabel(GlucoseUnit.MMOL) -> "mmol/L"
 */
@Composable
fun glucoseUnitLabel(unit: GlucoseUnit = LocalGlucoseUnit.current): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> stringResource(CommonR.string.glucose_unit_mgdl)
        GlucoseUnit.MMOL -> stringResource(CommonR.string.glucose_unit_mmol)
    }
}

/**
 * Composable UI function.
 * Returns the localized unit label for Insulin Sensitivity Factor (ISF) (e.g. "mg/dL/IU" or "mmol/L/IU").
 *
 * @example isfUnitLabel(GlucoseUnit.MG_DL) -> "mg/dL/IU" / "mg/dL/I.E."
 * @example isfUnitLabel(GlucoseUnit.MMOL) -> "mmol/L/IU" / "mmol/L/I.E."
 */
@Composable
fun isfUnitLabel(unit: GlucoseUnit = LocalGlucoseUnit.current): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> stringResource(CommonR.string.unit_mgdl_per_u)
        GlucoseUnit.MMOL -> stringResource(CommonR.string.unit_mmol_per_u)
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a [BgReadingsInterval] for background services, notifications, etc.
 *
 * @example formatReadingsInterval(BgReadingsInterval.FiveMinutes, resources) -> "5 Minuten"
 * @example formatReadingsInterval(null, resources) -> "--"
 */
fun formatReadingsInterval(
    interval: BgReadingsInterval?,
    resources: Resources,
    default: String = "--"
): String {
    return when (interval) {
        BgReadingsInterval.OneMinute -> resources.getString(CommonR.string.bg_readings_interval_one_minute)
        BgReadingsInterval.FiveMinutes -> resources.getString(CommonR.string.bg_readings_interval_five_minutes)
        BgReadingsInterval.AdHoc -> resources.getString(CommonR.string.bg_readings_interval_adhoc)
        null -> default
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a [BgReadingsInterval] using a [Context].
 *
 * @example formatReadingsInterval(BgReadingsInterval.FiveMinutes, context) -> "5 Minuten"
 */
fun formatReadingsInterval(
    interval: BgReadingsInterval?,
    context: Context,
    default: String = "--"
): String = formatReadingsInterval(interval, context.resources, default)

/**
 * Composable UI function.
 * Formats a [BgReadingsInterval] for display in Compose UI components.
 *
 * @example readingsInterval(BgReadingsInterval.FiveMinutes) -> "5 Minuten"
 * @example readingsInterval(null) -> "--"
 */
@Composable
fun readingsInterval(interval: BgReadingsInterval?, default: String = "--"): String {
    return when (interval) {
        BgReadingsInterval.OneMinute -> stringResource(CommonR.string.bg_readings_interval_one_minute)
        BgReadingsInterval.FiveMinutes -> stringResource(CommonR.string.bg_readings_interval_five_minutes)
        BgReadingsInterval.AdHoc -> stringResource(CommonR.string.bg_readings_interval_adhoc)
        null -> default
    }
}

/**
 * Composable UI function.
 * Formats a blood glucose value for UI display.
 *
 * @example glucoseValue(BgValue.fromMgDl(120)) -> "120"
 * @example glucoseValue(BgValue.fromMgDl(120), withUnit = true) -> "120 mg/dL"
 * @example glucoseValue(null) -> "-"
 */
@Composable
fun glucoseValue(
    value: BgValue?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    if (value == null || value.isInvalid()) return default
    val valStr = value.toString(unit)
    return if (withUnit) {
        "$valStr$NBSP${glucoseUnitLabel(unit)}"
    } else valStr
}

/**
 * Non-Composable / Backend function.
 * Returns the localized unit label for blood glucose (e.g. "mg/dL" or "mmol/L").
 *
 * @example glucoseUnitLabel(GlucoseUnit.MG_DL, context) -> "mg/dL"
 * @example glucoseUnitLabel(GlucoseUnit.MMOL, context) -> "mmol/L"
 */
fun glucoseUnitLabel(unit: GlucoseUnit, context: Context): String {
    val resId = when (unit) {
        GlucoseUnit.MG_DL -> CommonR.string.glucose_unit_mgdl
        GlucoseUnit.MMOL -> CommonR.string.glucose_unit_mmol
    }
    return context.getString(resId)
}

/**
 * Non-Composable / Backend function.
 * Formats a [BgValue] with optional unit label using a [Context].
 *
 * @example glucoseValue(BgValue.fromMgDl(120), context) -> "120"
 * @example glucoseValue(BgValue.fromMgDl(120), context, withUnit = true) -> "120 mg/dL"
 * @example glucoseValue(null, context) -> "-"
 */
fun glucoseValue(
    value: BgValue?,
    context: Context,
    unit: GlucoseUnit = GlucoseUnit.MG_DL,
    default: String = "-",
    withUnit: Boolean = false
): String {
    if (value == null || value.isInvalid()) return default
    val valStr = value.toString(unit)
    return if (withUnit) {
        "$valStr$NBSP${glucoseUnitLabel(unit, context)}"
    } else valStr
}

/**
 * Composable UI function.
 * Formats an Insulin Sensitivity Factor (ISF) value for UI display.
 *
 * @example isfValue(BgDelta.fromMgDl(40)) -> "40"
 * @example isfValue(BgDelta.fromMgDl(40), withUnit = true) -> "40 mg/dL/IU" / "40 mg/dL/I.E."
 * @example isfValue(null) -> "-"
 */
@Composable
fun isfValue(
    value: BgDelta?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    val valStr = value?.toString(unit) ?: return default
    return if (withUnit) {
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> stringResource(CommonR.string.unit_mgdl_per_u)
            GlucoseUnit.MMOL -> stringResource(CommonR.string.unit_mmol_per_u)
        }
        "$valStr$NBSP$unitStr"
    } else valStr
}

/**
 * Composable UI function.
 * Formats a blood glucose delta value with +/- sign for UI display.
 *
 * @example deltaValue(BgDelta.fromMgDl(5)) -> "+5"
 * @example deltaValue(BgDelta.fromMgDl(-12)) -> "-12"
 * @example deltaValue(BgDelta.fromMgDl(5), withUnit = true) -> "+5 mg/dL"
 * @example deltaValue(null) -> "-"
 */
@Composable
fun deltaValue(
    value: BgDelta?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    val valStr = value?.toDiff(unit) ?: return default
    return if (withUnit) {
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> stringResource(CommonR.string.glucose_unit_mgdl)
            GlucoseUnit.MMOL -> stringResource(CommonR.string.glucose_unit_mmol)
        }
        "$valStr$NBSP$unitStr"
    } else valStr
}

/**
 * Composable UI function.
 * Formats a Carb Ratio (CR) value for UI display.
 *
 * @example crValue(12.0) -> "12.0 g/IU" / "12.0 g/E"
 * @example crValue(12.0, withUnit = false) -> "12.0"
 * @example crValue(null) -> "-"
 */
@Composable
fun crValue(value: Double?, default: String = "-", withUnit: Boolean = true): String {
    return value?.let {
        val valStr = String.format(Locale.getDefault(), "%.1f", it)
        if (withUnit) "$valStr$NBSP${stringResource(CommonR.string.unit_g_per_u)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for insulin ("IU" / "I.E.").
 *
 * @example formatInsulinUnitLabel(resources) -> "IU" / "I.E."
 */
fun formatInsulinUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.insulin_unit)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for insulin ("IU" / "I.E.").
 *
 * @example formatInsulinUnitLabel(context) -> "IU" / "I.E."
 */
fun formatInsulinUnitLabel(context: Context): String = formatInsulinUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Formats an insulin value in units (IU / I.E.) for background services, notifications, etc.
 *
 * @example formatInsulinValue(1.5, resources) -> "1.50 IU" / "1.50 I.E."
 * @example formatInsulinValue(1.5, resources, withUnit = false) -> "1.50"
 * @example formatInsulinValue(1.5, resources, signed = true) -> "+1.50 IU" / "+1.50 I.E."
 * @example formatInsulinValue(null, resources) -> "-"
 */
fun formatInsulinValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        val format = if (signed) "%+.2f" else "%.2f"
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr$NBSP${formatInsulinUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats an insulin value in units (IU / I.E.) using a [Context].
 *
 * @example formatInsulinValue(1.5, context) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(value, context.resources, default = default, withUnit = withUnit, signed = signed)

/**
 * Non-Composable / Backend function.
 * Formats an [InsulinAmount] in units (IU / I.E.) for background services, notifications, etc.
 *
 * @example formatInsulinValue(amount, resources) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    amount: InsulinAmount?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(amount?.iu, resources, default = default, withUnit = withUnit, signed = signed)

/**
 * Non-Composable / Backend function.
 * Formats an [InsulinAmount] in units (IU / I.E.) using a [Context].
 *
 * @example formatInsulinValue(amount, context) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    amount: InsulinAmount?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(amount?.iu, context, default = default, withUnit = withUnit, signed = signed)

/**
 * Composable UI function.
 * Returns the unit label for insulin ("IU" / "I.E.") in Compose UI components.
 *
 * @example insulinUnitLabel() -> "IU" / "I.E."
 */
@Composable
fun insulinUnitLabel(): String {
    return stringResource(CommonR.string.insulin_unit)
}

/**
 * Composable UI function.
 * Formats an insulin value in units (IU / I.E.) for display in Compose UI components.
 *
 * @example insulinValue(1.5) -> "1.50 IU" / "1.50 I.E."
 * @example insulinValue(1.5, withUnit = false) -> "1.50"
 * @example insulinValue(1.5, signed = true) -> "+1.50 IU" / "+1.50 I.E."
 * @example insulinValue(null) -> "-"
 */
@Composable
fun insulinValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatInsulinValue(value, context = LocalContext.current, default = default, withUnit = withUnit, signed = signed)
}

/**
 * Composable UI function.
 * Formats an [InsulinAmount] in units (IU / I.E.) for display in Compose UI components.
 *
 * @example insulinValue(amount) -> "1.50 IU" / "1.50 I.E."
 */
@Composable
fun insulinValue(amount: InsulinAmount?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return insulinValue(amount?.iu, default = default, withUnit = withUnit, signed = signed)
}

/**
 * Default step size for insulin amounts in units (IU).
 */
const val INSULIN_STEP_SIZE = 0.1

/**
 * Creates a [de.dh.daps.ui.common.SteppingStrategy] tailored for insulin amounts, using [de.dh.daps.ui.common.ModuloSteppingStrategy]
 * with the standard [INSULIN_STEP_SIZE] (0.1 IU) by default.
 *
 * @example insulinSteppingStrategy() -> ModuloSteppingStrategy(step = 0.1)
 */
fun insulinSteppingStrategy(step: Double = INSULIN_STEP_SIZE): SteppingStrategy =
    ModuloSteppingStrategy(step)

/**
 * Non-Composable / Backend function.
 * Returns the unit label for grams ("g").
 *
 * @example formatCarbsGramsUnitLabel(resources) -> "g"
 */
fun formatCarbsGramsUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.unit_g)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for grams ("g").
 *
 * @example formatCarbsGramsUnitLabel(context) -> "g"
 */
fun formatCarbsGramsUnitLabel(context: Context): String = formatCarbsGramsUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Returns the unit label for carbohydrate units ("KE").
 *
 * @example formatCarbsKeUnitLabel(resources) -> "KE"
 */
fun formatCarbsKeUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.unit_ke)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for carbohydrate units ("KE").
 *
 * @example formatCarbsKeUnitLabel(context) -> "KE"
 */
fun formatCarbsKeUnitLabel(context: Context): String = formatCarbsKeUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Formats a carbohydrate value explicitly in grams ("g") for background services, notifications, etc.
 *
 * @example formatCarbsGramsValue(20.0, resources) -> "20 g"
 * @example formatCarbsGramsValue(20.0, resources, signed = true) -> "+20 g"
 * @example formatCarbsGramsValue(null, resources) -> "-"
 */
fun formatCarbsGramsValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        val format = if (signed) "%+.0f" else "%.0f"
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr$NBSP${formatCarbsGramsUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats a value explicitly in carbohydrate units ("KE") for background services, notifications, etc.
 * The value must already be in KE (e.g. 2.0 KE for 20g).
 *
 * @example formatCarbsKeValue(2.0, resources) -> "2 KE"
 * @example formatCarbsKeValue(1.5, resources) -> "1.5 KE"
 * @example formatCarbsKeValue(null, resources) -> "-"
 */
fun formatCarbsKeValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        var format = if (signed) "%+" else "%"
        format += if (value % 1.0 == 0.0) {
            ".0f"
        } else {
            ".1f"
        }
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr$NBSP${formatCarbsKeUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats the given carbohydrate value (in grams) dynamically according to the selected [CarbsUnit]
 * (GRAMS or KE) for background services, notifications, etc.
 *
 * @example formatCarbsValue(20.0, CarbsUnit.GRAMS, resources) -> "20 g"
 * @example formatCarbsValue(20.0, CarbsUnit.KE, resources) -> "2 KE"
 * @example formatCarbsValue(15.0, CarbsUnit.KE, resources) -> "1.5 KE"
 * @example formatCarbsValue(null, CarbsUnit.GRAMS, resources) -> "-"
 */
fun formatCarbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return valueInGrams?.let {
        when (unit) {
            CarbsUnit.GRAMS -> formatCarbsGramsValue(valueInGrams, resources = resources, default = default, withUnit = withUnit, signed = signed)
            CarbsUnit.KE -> {
                val keValue = valueInGrams / 10.0
                formatCarbsKeValue(keValue, resources = resources, default = default, withUnit = withUnit, signed = signed)
            }
        }
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats the given carbohydrate value (in grams) dynamically according to the selected [CarbsUnit] using a [Context].
 *
 * @example formatCarbsValue(20.0, CarbsUnit.GRAMS, context) -> "20 g"
 * @example formatCarbsValue(20.0, CarbsUnit.KE, context) -> "2 KE"
 */
fun formatCarbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsValue(valueInGrams, unit, context.resources, default, withUnit, signed)

/**
 * Non-Composable / Backend function.
 * Formats a carbohydrate value explicitly in grams ("g") using a [Context].
 *
 * @example formatCarbsGramsValue(20.0, context) -> "20 g"
 */
fun formatCarbsGramsValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsGramsValue(value, context.resources, default, withUnit, signed)

/**
 * Non-Composable / Backend function.
 * Formats a value explicitly in KE ("KE") using a [Context].
 *
 * @example formatCarbsKeValue(2.0, context) -> "2 KE"
 */
fun formatCarbsKeValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsKeValue(value, context.resources, default, withUnit, signed)

/**
 * Composable UI function.
 * Returns the unit label for the configured or provided [CarbsUnit] in Compose UI components.
 *
 * @example carbsUnitLabel(CarbsUnit.GRAMS) -> "g"
 * @example carbsUnitLabel(CarbsUnit.KE) -> "KE"
 */
@Composable
fun carbsUnitLabel(unit: CarbsUnit = LocalCarbsUnit.current): String {
    return when (unit) {
        CarbsUnit.GRAMS -> stringResource(CommonR.string.unit_g)
        CarbsUnit.KE -> stringResource(CommonR.string.unit_ke)
    }
}

/**
 * Composable UI function.
 * Formats a carbohydrate value (provided in grams) dynamically according to the provided or CompositionLocal
 * configured [CarbsUnit] (GRAMS or KE) for display in Compose UI components.
 *
 * @example carbsValue(20.0) -> "20 g" (with CarbsUnit.GRAMS)
 * @example carbsValue(20.0) -> "2 KE" (with CarbsUnit.KE)
 * @example carbsValue(15.0) -> "1.5 KE" (with CarbsUnit.KE)
 * @example carbsValue(null) -> "-"
 */
@Composable
fun carbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit = LocalCarbsUnit.current,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return formatCarbsValue(
        valueInGrams = valueInGrams,
        unit = unit,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Composable UI function.
 * Returns the unit label for KE ("KE") in Compose UI components.
 *
 * @example carbsKeUnitLabel() -> "KE"
 */
@Composable
fun carbsKeUnitLabel(): String {
    return stringResource(CommonR.string.unit_ke)
}

/**
 * Composable UI function.
 * Formats a value in KE (e.g. 2.0 for 2 KE) for display in Compose UI components.
 *
 * @example carbsKeValue(2.0) -> "2 KE"
 * @example carbsKeValue(1.5) -> "1.5 KE"
 * @example carbsKeValue(null) -> "-"
 */
@Composable
fun carbsKeValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatCarbsKeValue(
        value = value,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Composable UI function.
 * Returns the unit label for grams ("g") in Compose UI components.
 *
 * @example carbsGramsUnitLabel() -> "g"
 */
@Composable
fun carbsGramsUnitLabel(): String {
    return stringResource(CommonR.string.unit_g)
}

/**
 * Composable UI function.
 * Formats a carbohydrate value in grams for display in Compose UI components.
 *
 * @example carbsGramsValue(20.0) -> "20 g"
 * @example carbsGramsValue(null) -> "-"
 */
@Composable
fun carbsGramsValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatCarbsGramsValue(
        value = value,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Default step size for carbohydrate units (KE / Bread Units).
 */
const val CARBS_KE_STEP_SIZE = 0.5

/**
 * Creates a [SteppingStrategy] tailored for carbohydrate units (KE), using [ModuloSteppingStrategy]
 * with the standard [CARBS_KE_STEP_SIZE] (0.5 KE) by default.
 *
 * @example carbsKeSteppingStrategy() -> ModuloSteppingStrategy(step = 0.5)
 */
fun carbsKeSteppingStrategy(step: Double = CARBS_KE_STEP_SIZE): SteppingStrategy =
    ModuloSteppingStrategy(step)

/**
 * Default step size for carbohydrate grams (g).
 */
const val CARBS_GRAMS_STEP_SIZE = 5.0

/**
 * Creates a [SteppingStrategy] tailored for carbohydrate grams, using [ModuloSteppingStrategy]
 * with the standard [CARBS_GRAMS_STEP_SIZE] (5.0 g) by default.
 *
 * @example carbsGramsSteppingStrategy() -> ModuloSteppingStrategy(step = 5.0)
 */
fun carbsGramsSteppingStrategy(step: Double = CARBS_GRAMS_STEP_SIZE): SteppingStrategy =
    ModuloSteppingStrategy(step)