package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.DataProvider
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.SensorType
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.db.entities.DataProviderEntity
import de.dh.daps.core.repository.db.entities.GlucoseReadingEntity
import de.dh.daps.core.repository.db.entities.SensorTypeEntity

// BgReading Converters
fun BgReading.toEntity(dataProviderId: Long, sourceSensorId: Long) = GlucoseReadingEntity(
    id = this.id,
    value_mgdl = this.value.mgdlInt.toShort(),
    sample_kind = this.sampleKind,
    timestamp = this.timestamp,
    fk_data_provider = dataProviderId,
    fk_source_sensor = sourceSensorId
)

fun GlucoseReadingEntity.toModel() = BgReading(
    id = this.id,
    value = BgValue.fromMgDl(this.value_mgdl),
    sampleKind = this.sample_kind,
    timestamp = this.timestamp
)

// SensorType Converters
fun SensorType.toEntity() = SensorTypeEntity(
    id = this.id,
    name = this.name
)

fun SensorTypeEntity.toModel() = SensorType(
    id = this.id,
    name = this.name
)

// DataProvider Converters
fun DataProvider.toEntity() = DataProviderEntity(
    id = this.id,
    name = this.name,
    type = this.type
)

fun DataProviderEntity.toModel() = DataProvider(
    id = this.id,
    name = this.name,
    type = this.type
)