package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.db.entities.DataProviderEntity
import de.dh.daps.core.repository.db.entities.GlucoseReadingEntity
import de.dh.daps.core.repository.db.entities.SensorTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM sensor_type ORDER BY name ASC")
    suspend fun getAllSensorTypes(): List<SensorTypeEntity>

    @Query("SELECT * FROM sensor_type where name = :name")
    suspend fun getSensorTypeByName(name: String): SensorTypeEntity?

    @Insert
    suspend fun insertSensorType(value: SensorTypeEntity): Long

    @Query("SELECT * FROM data_provider ORDER BY name ASC")
    suspend fun getAllDataProviders(): List<DataProviderEntity>

    @Query("SELECT * FROM data_provider where name = :name")
    suspend fun getDataProviderByName(name: String): DataProviderEntity?

    @Insert
    suspend fun insertDataProvider(value: DataProviderEntity): Long

    @Query("SELECT * FROM glucose_reading where timestamp > :timestamp ORDER BY timestamp ASC")
    suspend fun getReadingsFromTime(timestamp: Timestamp): List<GlucoseReadingEntity>

    @Query("SELECT * FROM glucose_reading ORDER BY timestamp ASC")
    suspend fun getAllGlucoseReadings(): List<GlucoseReadingEntity>

    @Query("SELECT * FROM glucose_reading ORDER BY timestamp ASC")
    fun observeAllReadings(): Flow<List<GlucoseReadingEntity>>

    @Insert
    suspend fun insertGlucoseReading(reading: GlucoseReadingEntity): Long

    @Query("DELETE FROM glucose_reading")
    suspend fun deleteAllGlucoseReadings()

    @Query("DELETE FROM sensor_type")
    suspend fun deleteAllSensorTypes()

    @Query("DELETE FROM data_provider")
    suspend fun deleteAllDataProviders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorTypes(items: List<SensorTypeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataProviders(items: List<DataProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlucoseReadings(items: List<GlucoseReadingEntity>)
}