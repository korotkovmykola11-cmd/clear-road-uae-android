package com.clearroad.app.triphistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface TripHistoryDao {

    @Query(
        """
        SELECT * FROM trip_history
        WHERE originKey = :originKey
          AND destinationKey = :destinationKey
          AND mode = :mode
          AND timestamp >= :minTimestamp
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
        minTimestamp: Long,
        limit: Int,
    ): List<TripHistoryEntity>

    @Query(
        """
        SELECT * FROM trip_history
        WHERE originKey = :originKey
          AND destinationKey = :destinationKey
          AND mode = :mode
        ORDER BY timestamp DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
    ): TripHistoryEntity?

    @Insert
    suspend fun insert(entity: TripHistoryEntity): Long

    @Query("DELETE FROM trip_history WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query(
        """
        SELECT COUNT(*) FROM trip_history
        WHERE originKey = :originKey
          AND destinationKey = :destinationKey
          AND mode = :mode
        """,
    )
    suspend fun countForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
    ): Int

    @Query(
        """
        DELETE FROM trip_history
        WHERE id IN (
            SELECT id FROM trip_history
            WHERE originKey = :originKey
              AND destinationKey = :destinationKey
              AND mode = :mode
            ORDER BY timestamp ASC
            LIMIT :deleteCount
        )
        """,
    )
    suspend fun deleteOldestForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
        deleteCount: Int,
    )
}
