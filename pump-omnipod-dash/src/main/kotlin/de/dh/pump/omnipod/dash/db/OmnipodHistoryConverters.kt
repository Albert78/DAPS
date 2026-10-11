package de.dh.pump.omnipod.dash.db

import androidx.room.TypeConverter

class OmnipodHistoryConverters {
    @TypeConverter
    fun fromEventType(value: OmnipodHistoryEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): OmnipodHistoryEventType {
        return try {
            OmnipodHistoryEventType.valueOf(value)
        } catch (_: Exception) {
            OmnipodHistoryEventType.STATUS_UPDATE
        }
    }
}