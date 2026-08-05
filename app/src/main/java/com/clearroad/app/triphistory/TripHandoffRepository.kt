package com.clearroad.app.triphistory

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.clearroad.app.domain.PreferenceMode

/**
 * Unified handoff persistence: v1 [trip_history] + v2 decision snapshots in one logical operation.
 *
 * Shared dedup gate ([TripHistoryRepository.HANDOFF_DEDUP_WINDOW_MS]) applies before any write.
 * v1 and v2 inserts run inside a single Room transaction when a snapshot is present.
 */
class TripHandoffRepository internal constructor(
    private val database: TripHistoryDatabase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    /**
     * Test-only: when true, throws inside [recordHandoff]'s Room transaction after v1 insert
     * to verify atomic rollback. Never set from production — only via [createForTest].
     */
    @param:VisibleForTesting(otherwise = VisibleForTesting.NONE)
    private val failSnapshotInsertForTest: Boolean = false,
) {
    private val tripHistoryDao get() = database.tripHistoryDao()
    private val snapshotDao get() = database.tripDecisionSnapshotDao()

    /**
     * Records v1 legacy entry and optional v2 decision snapshot atomically.
     *
     * Returns [TripHandoffPersistResult.deduplicated] when the shared dedup gate suppresses the
     * handoff (neither v1 nor v2 is written).
     */
    suspend fun recordHandoff(
        v1Entry: TripHistoryEntry,
        snapshot: TripHandoffSnapshotUiModel?,
    ): TripHandoffPersistResult {
        if (isWithinDedupWindow(v1Entry)) {
            return TripHandoffPersistResult(recorded = false, deduplicated = true)
        }

        database.withTransaction {
            tripHistoryDao.insert(v1Entry.toEntity())
            if (snapshot != null) {
                if (failSnapshotInsertForTest) {
                    throw IllegalStateException("test: snapshot insert failure")
                }
                val snapshotId = snapshotDao.insert(snapshot.toEntity())
                val alternatives =
                    snapshot.alternatives.map { alternative ->
                        alternative.toEntity(snapshotId)
                    }
                if (alternatives.isNotEmpty()) {
                    snapshotDao.insertAlternatives(alternatives)
                }
            }
            runRetentionMaintenance(v1Entry)
        }
        return TripHandoffPersistResult(recorded = true, deduplicated = false)
    }

    /**
     * Shared dedup gate for v1+v2: checks the latest v1 row for the same O-D + mode.
     * v1 and v2 are always written together, so v1 timestamp is the handoff clock.
     */
    internal suspend fun isWithinDedupWindow(entry: TripHistoryEntry): Boolean {
        val latest =
            tripHistoryDao.getLatestForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
            )
        return latest != null &&
            nowMillis() - latest.timestamp < TripHistoryRepository.HANDOFF_DEDUP_WINDOW_MS
    }

    internal suspend fun runRetentionMaintenance(entry: TripHistoryEntry) {
        val cutoff = retentionCutoffTimestamp()
        tripHistoryDao.deleteOlderThan(cutoff)
        snapshotDao.deleteOlderThan(cutoff)

        trimCountCapForOdMode(entry)
    }

    private suspend fun trimCountCapForOdMode(entry: TripHistoryEntry) {
        val max = TripHistoryRepository.MAX_ENTRIES_PER_OD_MODE
        val v1Excess =
            tripHistoryDao.countForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
            ) - max
        if (v1Excess > 0) {
            tripHistoryDao.deleteOldestForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
                deleteCount = v1Excess,
            )
        }

        val snapshotExcess =
            snapshotDao.countForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
            ) - max
        if (snapshotExcess > 0) {
            snapshotDao.deleteOldestForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
                deleteCount = snapshotExcess,
            )
        }
    }

    internal fun retentionCutoffTimestamp(now: Long = nowMillis()): Long =
        now - TripHistoryRepository.RETENTION_MILLIS

    companion object {
        @Volatile
        private var defaultInstance: TripHandoffRepository? = null

        fun get(context: Context): TripHandoffRepository =
            defaultInstance
                ?: synchronized(this) {
                    defaultInstance
                        ?: TripHandoffRepository(
                            TripHistoryDatabase.get(context),
                        ).also { defaultInstance = it }
                }

        /**
         * Test-only factory. [failSnapshotInsertForTest] is an in-memory flag for the returned
         * instance only — not persisted and not reachable from [get].
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        internal fun createForTest(
            database: TripHistoryDatabase,
            nowMillis: () -> Long = { System.currentTimeMillis() },
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            failSnapshotInsertForTest: Boolean = false,
        ): TripHandoffRepository =
            TripHandoffRepository(database, nowMillis, failSnapshotInsertForTest)
    }
}
