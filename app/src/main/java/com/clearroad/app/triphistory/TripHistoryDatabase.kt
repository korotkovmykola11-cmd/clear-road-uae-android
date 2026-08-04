package com.clearroad.app.triphistory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class TripHistoryDatabase : RoomDatabase() {
    abstract fun tripHistoryDao(): TripHistoryDao

    companion object {
        private const val DB_NAME = "marshio_trip_history.db"

        @Volatile
        private var instance: TripHistoryDatabase? = null

        /** Initial schema v1 — no migrations yet; avoid destructive fallback in production. */
        fun get(context: Context): TripHistoryDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            TripHistoryDatabase::class.java,
                            DB_NAME,
                        ).build().also { instance = it }
                }
    }
}
