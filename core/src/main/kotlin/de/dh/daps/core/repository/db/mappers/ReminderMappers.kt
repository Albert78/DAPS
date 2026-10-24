package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.reminder.Reminder
import de.dh.daps.core.repository.db.entities.ReminderEntity

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    type = type,
    source_id = sourceId,
    title = title,
    description = description,
    reminder_timestamp = reminderTimestamp,
    expiration_timestamp = expirationTimestamp,
    target_route = targetRoute,
    payload = payload,
    status = status
)

fun ReminderEntity.toModel() = Reminder(
    id = id,
    type = type,
    sourceId = source_id,
    title = title,
    description = description,
    reminderTimestamp = reminder_timestamp,
    expirationTimestamp = expiration_timestamp,
    targetRoute = target_route,
    payload = payload,
    status = status
)