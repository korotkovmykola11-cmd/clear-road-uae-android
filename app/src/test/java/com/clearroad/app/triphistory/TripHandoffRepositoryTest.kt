package com.clearroad.app.triphistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TripHandoffRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: TripHistoryDatabase

    private val origin = LatLng(25.2048, 55.2708)
    private val destination = LatLng(25.0800, 55.1400)
    private val originKey: String
        get() = TripHistoryKey.fromLatLng(origin)
    private val destinationKey: String
        get() = TripHistoryKey.fromLatLng(destination)
    private val mode = PreferenceMode.FASTEST

    private val fixedNow = 1_700_000_000_000L
    private val dayMs = TimeUnit.DAYS.toMillis(1)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = TripHistoryDatabase.createInMemoryForTest(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordHandoff_writesV1AndV2Together() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })
        val v1Entry = entry(timestamp = fixedNow, durationSeconds = 620)
        val snapshot = sampleSnapshot(timestamp = fixedNow)

        val result = repository.recordHandoff(v1Entry, snapshot)

        assertTrue(result.recorded)
        assertFalse(result.deduplicated)
        assertEquals(1, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(1, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
        val snapshotId =
            database.tripDecisionSnapshotDao()
                .getLatestForOdMode(originKey, destinationKey, mode.name)!!
                .id
        assertEquals(2, database.tripDecisionSnapshotDao().countAlternativesForSnapshot(snapshotId))
    }

    @Test
    fun recordHandoff_sharedDedupGateSuppressesV1AndV2() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })
        val recent =
            entry(
                timestamp = fixedNow - TimeUnit.HOURS.toMillis(2),
                durationSeconds = 600,
            )
        database.tripHistoryDao().insert(recent.toEntity())

        val result =
            repository.recordHandoff(
                v1Entry = entry(timestamp = fixedNow, durationSeconds = 700),
                snapshot = sampleSnapshot(timestamp = fixedNow),
            )

        assertFalse(result.recorded)
        assertTrue(result.deduplicated)
        assertEquals(1, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(0, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    @Test
    fun recordHandoff_dedupAllowsWriteAfterWindowExpires() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })
        database.tripHistoryDao().insert(
            entry(
                timestamp = fixedNow - TripHistoryRepository.HANDOFF_DEDUP_WINDOW_MS - 1,
                durationSeconds = 600,
            ).toEntity(),
        )

        val result =
            repository.recordHandoff(
                v1Entry = entry(timestamp = fixedNow, durationSeconds = 700),
                snapshot = sampleSnapshot(timestamp = fixedNow),
            )

        assertTrue(result.recorded)
        assertEquals(2, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(1, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    @Test
    fun recordHandoff_v2RetentionParityWithV1() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })
        val dedupSafeOffset = TripHistoryRepository.HANDOFF_DEDUP_WINDOW_MS + 1
        repeat(50) { index ->
            val timestamp = fixedNow - dedupSafeOffset - index * 60_000L
            database.tripHistoryDao().insert(entry(timestamp = timestamp, durationSeconds = 600 + index).toEntity())
            database.tripDecisionSnapshotDao().insert(sampleSnapshot(timestamp = timestamp).toEntity())
        }

        repository.recordHandoff(
            v1Entry = entry(timestamp = fixedNow, durationSeconds = 999),
            snapshot = sampleSnapshot(timestamp = fixedNow),
        )

        assertEquals(50, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(50, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    @Test
    fun recordHandoff_retentionDeletesSnapshotsOlderThan30Days() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })
        repeat(10) { index ->
            val timestamp = fixedNow - (400L + index) * dayMs
            database.tripDecisionSnapshotDao().insert(sampleSnapshot(timestamp = timestamp).toEntity())
        }

        repository.recordHandoff(
            v1Entry = entry(timestamp = fixedNow - 2 * dayMs, durationSeconds = 900),
            snapshot = sampleSnapshot(timestamp = fixedNow - 2 * dayMs),
        )

        assertEquals(1, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    @Test
    fun recordHandoff_rollsBackV1WhenSnapshotInsertFails() = runBlocking {
        val repository =
            TripHandoffRepository.createForTest(
                database = database,
                nowMillis = { fixedNow },
                failSnapshotInsertForTest = true,
            )

        try {
            repository.recordHandoff(
                v1Entry = entry(timestamp = fixedNow, durationSeconds = 620),
                snapshot = sampleSnapshot(timestamp = fixedNow),
            )
            throw AssertionError("expected transaction failure")
        } catch (_: IllegalStateException) {
        }

        assertEquals(0, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(0, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    @Test
    fun recordHandoff_v1OnlyWhenSnapshotNull() = runBlocking {
        val repository = TripHandoffRepository.createForTest(database, nowMillis = { fixedNow })

        val result =
            repository.recordHandoff(
                v1Entry = entry(timestamp = fixedNow, durationSeconds = 620),
                snapshot = null,
            )

        assertTrue(result.recorded)
        assertEquals(1, database.tripHistoryDao().countForOdMode(originKey, destinationKey, mode.name))
        assertEquals(0, database.tripDecisionSnapshotDao().countForOdMode(originKey, destinationKey, mode.name))
    }

    private fun entry(
        timestamp: Long,
        durationSeconds: Int,
    ): TripHistoryEntry =
        TripHistoryEntry(
            timestamp = timestamp,
            originKey = originKey,
            destinationKey = destinationKey,
            durationSeconds = durationSeconds,
            mode = mode,
            hasSalik = false,
            tollAed = null,
        )

    private fun sampleSnapshot(timestamp: Long): TripHandoffSnapshotUiModel =
        TripHandoffSnapshotUiModel(
            timestamp = timestamp,
            originKey = originKey,
            destinationKey = destinationKey,
            mode = mode,
            handoffCandidateRouteIndex = 1,
            marshioRecommendedRouteIndex = 1,
            googleDefaultRouteIndex = 0,
            handoffCandidateRouteIdentityKey = "handoff-candidate",
            handoffCandidateDurationSeconds = 720,
            handoffCandidateHasSalik = true,
            handoffCandidateTollAed = 8.0,
            baselineRouteIdentityKey = "baseline",
            baselineDurationSeconds = 900,
            baselineHasSalik = false,
            baselineTollAed = null,
            timeDeltaVsBaselineSeconds = 180,
            bestNoSalikDurationSeconds = 900,
            salikTimeDeltaSeconds = 180,
            alternatives =
                listOf(
                    TripHandoffAlternativeUiModel(
                        routeIndex = 0,
                        routeIdentityKey = "baseline",
                        durationSeconds = 900,
                        hasSalik = false,
                        tollAed = null,
                        roleFlags = TripDecisionAlternativeRole.GOOGLE_DEFAULT,
                    ),
                    TripHandoffAlternativeUiModel(
                        routeIndex = 1,
                        routeIdentityKey = "handoff-candidate",
                        durationSeconds = 720,
                        hasSalik = true,
                        tollAed = 8.0,
                        roleFlags =
                            TripDecisionAlternativeRole.HANDOFF_CANDIDATE or
                                TripDecisionAlternativeRole.MARSHIO_RECOMMENDED,
                    ),
                ),
        )
}
