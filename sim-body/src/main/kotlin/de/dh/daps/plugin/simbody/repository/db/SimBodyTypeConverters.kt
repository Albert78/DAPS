package de.dh.daps.plugin.simbody.repository.db

import androidx.room.TypeConverter
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.data.Timestamp

class SimBodyTypeConverters {
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? = timestamp?.ms

    @TypeConverter
    fun toTimestamp(value: Long?): Timestamp? = value?.let { Timestamp(it) }

    @TypeConverter
    fun fromInsulinAmount(amount: InsulinAmount?): Double? = amount?.iu

    @TypeConverter
    fun toInsulinAmount(value: Double?): InsulinAmount? = value?.let { InsulinAmount(it) }

    @TypeConverter
    fun fromInsulinOrigin(origin: InsulinOrigin?): String? = origin?.name

    @TypeConverter
    fun toInsulinOrigin(value: String?): InsulinOrigin? = value?.let {
        try {
            InsulinOrigin.valueOf(it)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromPumpDeliveryType(type: PumpDeliveryType?): String? = type?.name

    @TypeConverter
    fun toPumpDeliveryType(value: String?): PumpDeliveryType? = value?.let {
        try {
            PumpDeliveryType.valueOf(it)
        } catch (_: Exception) {
            null
        }
    }
}