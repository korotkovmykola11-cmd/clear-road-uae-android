package com.clearroad.app.triphistory

import android.content.Context
import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit

/**
 * On-device trip history (SQLite via Room). Data never leaves the device.
 *
 * Trips are recorded on navigation handoff (Google Maps / Waze), not on every route fetch.
 * At most one row per **originKey + destinationKey + mode** within [HANDOFF_DEDUP_WINDOW_MS].
 *
 * Median / insight queries are scoped to originKey + destinationKey + [PreferenceMode].
 *
 * TODO: Settings — add "Clear trip history" action for user privacy.
 */
class TripHistoryRepository internal constructor(
    private val dao: TripHistoryDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun priorTripsForOdMode(
        origin: LatLng,
        destination: LatLng,
        mode: PreferenceMode,
    ): List<TripHistoryEntry> {
        val (originKey, destinationKey) = TripHistoryKey.pairKeys(origin, destination)
        return dao
            .getRecentForOdMode(
                originKey = originKey,
                destinationKey = destinationKey,
                mode = mode.name,
                minTimestamp = retentionCutoffTimestamp(),
                limit = MAX_ENTRIES_PER_OD_MODE,
            ).map { it.toEntry() }
    }

    /**
     * Persists a trip after explicit user handoff. Returns false when dedup window suppresses
     * a duplicate entry for the same O-D pair + mode.
     */
    suspend fun recordHandoffTrip(entry: TripHistoryEntry): Boolean {
        val latest =
            dao.getLatestForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
            )
        if (
            latest != null &&
            nowMillis() - latest.timestamp < HANDOFF_DEDUP_WINDOW_MS
        ) {
            return false
        }
        persistEntry(entry)
        return true
    }

    internal suspend fun persistEntry(entry: TripHistoryEntry) {
        dao.insert(entry.toEntity())
        runRetentionMaintenance(entry)
    }

    /**
     * Retention maintenance order (on handoff persist only):
     * 1. Delete all rows with `timestamp < retentionCutoffTimestamp()` globally.
     * 2. For this originKey + destinationKey + mode, keep at most [MAX_ENTRIES_PER_OD_MODE]
     *    newest rows (time cutoff already applied in step 1).
     *
     * Reads do not purge; [priorTripsForOdMode] excludes rows below the cutoff via DAO filter so
     * stale rows never feed median / Salik majority until physically removed on next handoff.
     */
    internal suspend fun runRetentionMaintenance(entry: TripHistoryEntry) {
        dao.deleteOlderThan(retentionCutoffTimestamp())
        val excess =
            dao.countForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
            ) - MAX_ENTRIES_PER_OD_MODE
        if (excess > 0) {
            dao.deleteOldestForOdMode(
                originKey = entry.originKey,
                destinationKey = entry.destinationKey,
                mode = entry.mode.name,
                deleteCount = excess,
            )
        }
    }

    /**
     * Cutoff for the rolling retention window: `nowMillis() - RETENTION_MILLIS`.
     *
     * Boundary (inclusive lower bound for reads, exclusive for deletes):
     * - `timestamp == cutoff` → **kept** (`timestamp >= cutoff` in history queries).
     * - `timestamp < cutoff` → **deleted** / excluded from insight.
     */
    internal fun retentionCutoffTimestamp(now: Long = nowMillis()): Long = now - RETENTION_MILLIS

    companion object {
        /** Rolling retention window aligned to Service Specific Terms 30-day lat/lng cache period. */
        const val RETENTION_DAYS = 30

        /** Max stored handoffs per same O-D pair + mode (additional cap within retention window). */
        const val MAX_ENTRIES_PER_OD_MODE = 50
        val RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        /** At most one handoff record per O-D + mode within this window (policy: 4–6 h). */
        val HANDOFF_DEDUP_WINDOW_MS: Long = TimeUnit.HOURS.toMillis(5)

        @Volatile
        private var defaultInstance: TripHistoryRepository? = null

        fun get(context: Context): TripHistoryRepository =
            defaultInstance
                ?: synchronized(this) {
                    defaultInstance
                        ?: TripHistoryRepository(
                            TripHistoryDatabase.get(context).tripHistoryDao(),
                        ).also { defaultInstance = it }
                }

        /** Test-only factory with injectable clock. */
        internal fun createForTest(
            dao: TripHistoryDao,
            nowMillis: () -> Long = { System.currentTimeMillis() },
        ): TripHistoryRepository = TripHistoryRepository(dao, nowMillis)
    }
}

internal fun buildTripHistoryEntry(
    origin: LatLng,
    destination: LatLng,
    route: RealRouteDebugData,
    mode: PreferenceMode,
    timestamp: Long = System.currentTimeMillis(),
): TripHistoryEntry {
    val hasSalik = route.tollAED > 0 || route.hasToll
    return buildTripHistoryEntry(
        origin = origin,
        destination = destination,
        durationSeconds = route.durationSeconds.coerceAtLeast(0),
        mode = mode,
        hasSalik = hasSalik,
        tollAed = route.tollAED.takeIf { it > 0 }?.toDouble(),
        timestamp = timestamp,
    )
}

internal fun buildTripHistoryEntry(
    origin: LatLng,
    destination: LatLng,
    durationSeconds: Int,
    mode: PreferenceMode,
    hasSalik: Boolean,
    tollAed: Double?,
    timestamp: Long = System.currentTimeMillis(),
): TripHistoryEntry {
    val (originKey, destinationKey) = TripHistoryKey.pairKeys(origin, destination)
    return TripHistoryEntry(
        timestamp = timestamp,
        originKey = originKey,
        destinationKey = destinationKey,
        durationSeconds = durationSeconds.coerceAtLeast(0),
        mode = mode,
        hasSalik = hasSalik,
        tollAed = tollAed,
    )
}
