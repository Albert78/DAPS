package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.MealReminder
import de.dh.daps.core.repository.db.entities.MealReminderEntity

fun MealReminder.toEntity() = MealReminderEntity(
    id = id,
    meal_id = mealId,
    meal_timestamp = mealTimestamp,
    reminder_timestamp = reminderTimestamp,
    description = description
)

fun MealReminderEntity.toModel() = MealReminder(
    id = id,
    mealId = meal_id,
    mealTimestamp = meal_timestamp,
    reminderTimestamp = reminder_timestamp,
    description = description
)