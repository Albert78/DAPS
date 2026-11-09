package de.dh.daps.core.repository.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.dh.daps.core.repository.db.dao.AlertProfileDao
import de.dh.daps.core.repository.db.dao.MetabolicEventsDao
import de.dh.daps.core.repository.db.dao.ProviderDao
import de.dh.daps.core.repository.db.dao.ReminderDao
import de.dh.daps.core.repository.db.dao.RetentionSettingsDao
import de.dh.daps.core.repository.db.dao.SettingsDao
import de.dh.daps.core.repository.db.dao.SystemMetricsDao
import de.dh.daps.core.repository.db.dao.TherapyDao
import de.dh.daps.core.repository.db.entities.AlertProfileEntity
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
import de.dh.daps.core.repository.db.entities.MealTypeEntity
import de.dh.daps.core.repository.db.entities.ReminderEntity
import de.dh.daps.core.repository.db.entities.RetentionSettingsEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.SensorTypeEntity
import de.dh.daps.core.repository.db.entities.TherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.TickMetricEntity
import de.dh.daps.core.repository.db.entities.WakeupMetricEntity
import java.util.concurrent.Executors

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `core_insights` RENAME COLUMN `predictedBgAtPeak` TO `predictedBgAtCorrection`")
    }
}

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
    AlertProfileEntity::class,

    // Metabolic events
    MealTypeEntity::class,
    MealEntity::class,
    ReminderEntity::class,
    InsulinTypeEntity::class,
    InsulinEntity::class,
    DeferredBolusEntity::class,
    CoreInsightEntity::class,
    WakeupMetricEntity::class,
    TickMetricEntity::class,
    RetentionSettingsEntity::class
], version = 2)
@TypeConverters(
    DbTypeConverters::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun therapyDao(): TherapyDao
    abstract fun metabolicEventsDao(): MetabolicEventsDao
    abstract fun settingsDao(): SettingsDao
    abstract fun systemMetricsDao(): SystemMetricsDao
    abstract fun alertProfileDao(): AlertProfileDao
    abstract fun reminderDao(): ReminderDao
    abstract fun retentionSettingsDao(): RetentionSettingsDao

    companion object {
        const val CURRENT_DATABASE_VERSION = "2.0"
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
                        .addMigrations(MIGRATION_1_2)
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