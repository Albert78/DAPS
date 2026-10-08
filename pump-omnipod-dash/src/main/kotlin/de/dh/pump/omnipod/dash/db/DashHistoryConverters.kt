package de.dh.pump.omnipod.dash.db

import androidx.room.TypeConverter

class DashHistoryConverters {
    @TypeConverter
    fun fromEventType(value: DashHistoryEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): DashHistoryEventType {
        return try {
            DashHistoryEventType.valueOf(value)
        } catch (_: Exception) {
            DashHistoryEventType.STATUS_UPDATE
        }
    }
}