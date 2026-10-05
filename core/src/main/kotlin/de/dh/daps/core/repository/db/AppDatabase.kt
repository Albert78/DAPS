package de.dh.daps.core.repository.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.dh.daps.core.repository.db.dao.AlarmProfileDao
import de.dh.daps.core.repository.db.dao.MealReminderDao
import de.dh.daps.core.repository.db.dao.MetabolicEventsDao
import de.dh.daps.core.repository.db.dao.ProviderDao
import de.dh.daps.core.repository.db.dao.SettingsDao
import de.dh.daps.core.repository.db.dao.SystemMetricsDao
import de.dh.daps.core.repository.db.dao.TherapyDao
import de.dh.daps.core.repository.db.entities.AlarmProfileEntity
import de.dh.daps.core.repository.db.entities.CoreInsightEntity
import de.dh.daps.core.repository.db.entities.CurrentSettingsEntity
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.entities.DataProviderEntity
import de.dh.daps.core.repository.db.entities.DeferredBolusEntity
import de.dh.daps.core.repository.db.entities.GlucoseReadingEntity
import de.dh.daps.core.repository.db.entities.InsulinEntity
import de.dh.daps.core.repository.db.entities.InsulinProfileEntity
import de.dh.daps.core.repository.db.entities.InsulinTypeEntity
import de.dh.daps.core.repository.db.entities.MealEntity
import de.dh.daps.core.repository.db.entities.MealReminderEntity
import de.dh.daps.core.repository.db.entities.MealTypeEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.SensorTypeEntity
import de.dh.daps.core.repository.db.entities.TherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.TickMetricEntity
import de.dh.daps.core.repository.db.entities.WakeupMetricEntity
import java.util.concurrent.Executors

@Database(entities = [
    // Providers
    SensorTypeEntity::class,
    DataProviderEntity::class,
    GlucoseReadingEntity::class,

    // Therapy
    InsulinProfileEntity::class,
    CurrentTherapySettingsEntity::class,
    ScheduledTherapyAdjustmentEntity::class,
    TherapyAdjustmentEntity::class,
    CurrentSettingsEntity::class,
    AlarmProfileEntity::class,

    // Metabolic events
    MealTypeEntity::class,
    MealEntity::class,
    MealReminderEntity::class,
    InsulinTypeEntity::class,
    InsulinEntity::class,
    DeferredBolusEntity::class,
    CoreInsightEntity::class,
    WakeupMetricEntity::class,
    TickMetricEntity::class
], version = 1)
@TypeConverters(
    DbTypeConverters::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun therapyDao(): TherapyDao
    abstract fun metabolicEventsDao(): MetabolicEventsDao
    abstract fun settingsDao(): SettingsDao
    abstract fun systemMetricsDao(): SystemMetricsDao
    abstract fun alarmProfileDao(): AlarmProfileDao
    abstract fun mealReminderDao(): MealReminderDao

    companion object {
        const val CURRENT_DATABASE_VERSION = "1.0"
        private const val DATABASE_NAME = "ApsDatabase.db"
        private const val LOG_DB_STATEMENTS = false

        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val sLock = Any()

        fun getInstance(context: Context): AppDatabase {
            synchronized(sLock) {
                if (INSTANCE == null) {
                    val dbBuilder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                    // .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_1_4)
                        .fallbackToDestructiveMigration(true)

                    if (LOG_DB_STATEMENTS) {
                        dbBuilder.setQueryCallback({ sqlQuery, bindArgs ->
                            println("SQL Query: $sqlQuery SQL Args: $bindArgs")
                        }, Executors.newSingleThreadExecutor())
                    }
                    INSTANCE = dbBuilder.build()
                }
                return INSTANCE!!
            }
        }
    }
}