package com.clearroad.app.triphistory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripHistoryEntity::class,
        TripDecisionSnapshotEntity::class,
        TripDecisionAlternativeEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
internal abstract class TripHistoryDatabase : RoomDatabase() {
    abstract fun tripHistoryDao(): TripHistoryDao

    abstract fun tripDecisionSnapshotDao(): TripDecisionSnapshotDao

    companion object {
        private const val DB_NAME = "marshio_trip_history.db"

        @Volatile
        private var instance: TripHistoryDatabase? = null

        fun get(context: Context): TripHistoryDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            TripHistoryDatabase::class.java,
                            DB_NAME,
                        )
                            .addMigrations(MIGRATION_1_2)
                            .build()
                            .also { instance = it }
                }

        /** Test-only in-memory database (v2 schema). */
        internal fun createInMemoryForTest(context: Context): TripHistoryDatabase =
            Room.inMemoryDatabaseBuilder(context, TripHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
