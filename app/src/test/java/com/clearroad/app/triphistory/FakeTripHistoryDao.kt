package com.clearroad.app.triphistory

/**
 * In-memory [TripHistoryDao] for unit tests (controlled timestamps, no Room).
 */
internal class FakeTripHistoryDao : TripHistoryDao {
    private val entries = mutableListOf<TripHistoryEntity>()
    private var nextId = 1L

    fun allEntries(): List<TripHistoryEntity> = entries.toList()

    override suspend fun getRecentForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
        minTimestamp: Long,
        limit: Int,
    ): List<TripHistoryEntity> =
        entries
            .filter {
                it.originKey == originKey &&
                    it.destinationKey == destinationKey &&
                    it.mode == mode &&
                    it.timestamp >= minTimestamp
            }.sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getLatestForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
    ): TripHistoryEntity? =
        entries
            .filter {
                it.originKey == originKey &&
                    it.destinationKey == destinationKey &&
                    it.mode == mode
            }.maxByOrNull { it.timestamp }

    override suspend fun insert(entity: TripHistoryEntity): Long {
        val stored = entity.copy(id = nextId++)
        entries.add(stored)
        return stored.id
    }

    override suspend fun deleteOlderThan(cutoffTimestamp: Long) {
        entries.removeAll { it.timestamp < cutoffTimestamp }
    }

    override suspend fun countForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
    ): Int =
        entries.count {
            it.originKey == originKey &&
                it.destinationKey == destinationKey &&
                it.mode == mode
        }

    override suspend fun deleteOldestForOdMode(
        originKey: String,
        destinationKey: String,
        mode: String,
        deleteCount: Int,
    ) {
        val oldest =
            entries
                .filter {
                    it.originKey == originKey &&
                        it.destinationKey == destinationKey &&
                        it.mode == mode
                }.sortedBy { it.timestamp }
                .take(deleteCount)
        entries.removeAll(oldest.toSet())
    }
}
