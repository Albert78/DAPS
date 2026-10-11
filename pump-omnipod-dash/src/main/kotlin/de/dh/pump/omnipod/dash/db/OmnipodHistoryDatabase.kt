package de.dh.pump.omnipod.dash.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [OmnipodHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(OmnipodHistoryConverters::class)
abstract class OmnipodHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): OmnipodHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: OmnipodHistoryDatabase? = null

        fun getInstance(context: Context): OmnipodHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmnipodHistoryDatabase::class.java,
                    "omnipod_history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}