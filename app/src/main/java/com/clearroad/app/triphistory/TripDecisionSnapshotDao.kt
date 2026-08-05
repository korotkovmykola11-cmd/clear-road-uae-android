package com.clearroad.app.triphistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface TripDecisionSnapshotDao {

    @Insert
    suspend fun insert(entity: TripDecisionSnapshotEntity): Long

    @Insert
    suspend fun insertAlternatives(entities: List<TripDecisionAlternativeEntity>)

    @Query(
        """
        SELECT * FROM trip_decision_snapshots
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
    ): TripDecisionSnapshotEntity?

    @Query("DELETE FROM trip_decision_snapshots WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query(
        """
        SELECT COUNT(*) FROM trip_decision_snapshots
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
        DELETE FROM trip_decision_snapshots
        WHERE id IN (
            SELECT id FROM trip_decision_snapshots
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

    @Query("SELECT * FROM trip_decision_alternatives WHERE snapshotId = :snapshotId ORDER BY routeIndex ASC")
    suspend fun getAlternativesForSnapshot(snapshotId: Long): List<TripDecisionAlternativeEntity>

    @Query("SELECT COUNT(*) FROM trip_decision_alternatives WHERE snapshotId = :snapshotId")
    suspend fun countAlternativesForSnapshot(snapshotId: Long): Int

    @Query("DELETE FROM trip_decision_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshotById(snapshotId: Long)
}
