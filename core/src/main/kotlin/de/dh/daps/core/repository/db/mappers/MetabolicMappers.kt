package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.CarbCurveComponentData
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinApplication
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.MealType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.core.repository.db.entities.DeferredBolusEntity
import de.dh.daps.core.repository.db.entities.InsulinEntity
import de.dh.daps.core.repository.db.entities.InsulinTypeEntity
import de.dh.daps.core.repository.db.entities.MealEntity
import de.dh.daps.core.repository.db.entities.MealTypeEntity

// Meal Converters
fun carbCurveComponentListToString(components: List<CarbCurveComponentData>): String {
    return components.joinToString(";", prefix = "[", postfix = "]") {
        "(weight=${it.weight};peak=${it.peakMinutes.value})"
    }
}

fun stringToCarbCurveComponentList(value: String): List<CarbCurveComponentData> {
    val componentsStr = value.removeSurrounding("[", "]")
    if (componentsStr.isBlank()) return emptyList()
    return componentsStr.split(");(").map { componentStr ->
        val cleanStr = componentStr.removePrefix("(").removeSuffix(")")
        val props = cleanStr.split(";")
        val weight = props[0].substringAfter("weight=").toInt()
        val peak = props[1].substringAfter("peak=").toShort()
        CarbCurveComponentData(weight, Minutes(peak))
    }
}

fun MealType.toEntity() = MealTypeEntity(
    id = this.id,
    name = this.name,
    symbol = this.symbol,
    curve_components = carbCurveComponentListToString(this.components),
    cat = this.cat,
    sortOrder = this.sortOrder
)

fun MealTypeEntity.toModel() = MealType(
    id = this.id,
    name = this.name,
    symbol = this.symbol,
    components = stringToCarbCurveComponentList(this.curve_components),
    cat = this.cat,
    sortOrder = this.sortOrder
)

fun MealEntry.toEntity() = MealEntity(
    id = this.id,
    meal_type_id = this.mealType.id,
    timestamp = this.timestamp,
    carbGrams = this.carbGrams,
    description = this.description,
    administeredInsulinAmount = this.administeredInsulinAmount
)

fun MealEntity.toModel(type: MealType) = MealEntry(
    id = this.id,
    timestamp = this.timestamp,
    carbGrams = this.carbGrams,
    mealType = type,
    description = this.description,
    administeredInsulinAmount = this.administeredInsulinAmount
)

// Insulin Converters
fun InsulinType.toEntity() = InsulinTypeEntity(
    id = this.id,
    name = this.name,
    peak = this.peak,
    dia = this.dia,
    default_concentration = this.defaultConcentration.factor
)

fun InsulinTypeEntity.toModel() = InsulinType(
    id = this.id,
    name = this.name,
    peak = this.peak,
    dia = this.dia,
    defaultConcentration = InsulinConcentration(this.default_concentration)
)

fun InsulinApplication.toEntity() = InsulinEntity(
    id = this.id,
    timestamp = this.timestamp,
    amount = this.amount,
    insulin_type_id = this.insulinType.id,
    origin = this.origin,
    basal = this.basal,
    correction = this.correction,
    meal = this.meal,
    status = this.status,
    pump_id = this.pumpId
)

fun InsulinEntity.toModel(type: InsulinType) = InsulinApplication(
    id = this.id,
    timestamp = this.timestamp,
    amount = this.amount,
    insulinType = type,
    origin = this.origin,
    basal = this.basal,
    correction = this.correction,
    meal = this.meal,
    status = this.status,
    pumpId = this.pump_id
)

// Deferred Bolus Converters
fun DeferredBolus.toEntity() = DeferredBolusEntity(
    id = this.id,
    timestamp = this.timestamp,
    amount = this.amount.iu,
    meal_id = this.mealId
)

fun DeferredBolusEntity.toModel() = DeferredBolus(
    id = this.id,
    amount = InsulinAmount(this.amount),
    timestamp = this.timestamp,
    mealId = this.meal_id
)