package de.dh.pump.omnipod.dash.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DashHistoryEntity::class], version = 1, exportSchema = false)
abstract class DashHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): DashHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: DashHistoryDatabase? = null

        fun getInstance(context: Context): DashHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DashHistoryDatabase::class.java,
                    "omnipod_dash_history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}