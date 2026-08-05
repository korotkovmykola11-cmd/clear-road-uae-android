package com.clearroad.app.triphistory

import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHistoryRepositoryTest {

    private val origin = LatLng(25.2048, 55.2708)
    private val destination = LatLng(25.0800, 55.1400)
    private val originKey: String
        get() = TripHistoryKey.fromLatLng(origin)
    private val destinationKey: String
        get() = TripHistoryKey.fromLatLng(destination)
    private val mode = PreferenceMode.FASTEST

    private val fixedNow = 1_700_000_000_000L
    private val dayMs = TimeUnit.DAYS.toMillis(1)
    private val retentionMs = TripHistoryRepository.RETENTION_MILLIS

    @Test
    fun priorTrips_keepsEntryFrom29DaysAgo() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        seed(
            dao,
            entry(timestamp = fixedNow - 29 * dayMs, durationSeconds = 600),
        )

        val prior = repository.priorTripsForOdMode(origin, destination, mode)

        assertEquals(1, prior.size)
        assertEquals(600, prior.single().durationSeconds)
    }

    @Test
    fun priorTrips_excludesEntryOlderThan30Days() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        seed(
            dao,
            entry(timestamp = fixedNow - retentionMs - 1, durationSeconds = 600),
        )

        assertTrue(repository.priorTripsForOdMode(origin, destination, mode).isEmpty())
    }

    @Test
    fun retentionBoundary_entryExactlyAt30Days_isKept() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        val cutoff = repository.retentionCutoffTimestamp()
        seed(dao, entry(timestamp = cutoff, durationSeconds = 700))

        val prior = repository.priorTripsForOdMode(origin, destination, mode)

        assertEquals(1, prior.size)
        assertEquals(700, prior.single().durationSeconds)
    }

    @Test
    fun retentionBoundary_entryOneMillisecondBeforeCutoff_isExcludedFromRead() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        val cutoff = repository.retentionCutoffTimestamp()
        seed(dao, entry(timestamp = cutoff - 1, durationSeconds = 700))

        assertTrue(repository.priorTripsForOdMode(origin, destination, mode).isEmpty())
    }

    @Test
    fun persistEntry_deletesRowsOlderThan30Days_evenWhenCountBelow50() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        repeat(10) { index ->
            seed(
                dao,
                entry(
                    timestamp = fixedNow - (400L + index) * dayMs,
                    durationSeconds = 500 + index,
                ),
            )
        }
        val recent = entry(timestamp = fixedNow - 2 * dayMs, durationSeconds = 900)

        repository.persistEntry(recent)

        val remaining = dao.allEntries()
        assertEquals(1, remaining.size)
        assertEquals(900, remaining.single().durationSeconds)
    }

    @Test
    fun persistEntry_keepsOnly50NewestForSameOdMode() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        repeat(51) { index ->
            seed(
                dao,
                entry(
                    timestamp = fixedNow - (50 - index) * 60_000L,
                    durationSeconds = 600 + index,
                ),
            )
        }

        repository.persistEntry(
            entry(timestamp = fixedNow, durationSeconds = 999),
        )

        val forOdMode =
            dao.allEntries().filter {
                it.originKey == originKey &&
                    it.destinationKey == destinationKey &&
                    it.mode == mode.name
            }
        assertEquals(50, forOdMode.size)
        assertTrue(forOdMode.none { it.durationSeconds == 600 })
        assertTrue(forOdMode.any { it.durationSeconds == 999 })
    }

    @Test
    fun persistEntry_countCapDoesNotTrimOtherOdOrModes() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        repeat(55) { index ->
            seed(
                dao,
                entry(
                    timestamp = fixedNow - index * 60_000L,
                    durationSeconds = 600 + index,
                    originKey = originKey,
                    destinationKey = destinationKey,
                    mode = mode,
                ),
            )
        }
        val otherModeEntry =
            entry(
                timestamp = fixedNow - 10 * dayMs,
                durationSeconds = 777,
                mode = PreferenceMode.CALM,
            )
        val otherOrigin = LatLng(25.3000, 55.3000)
        val otherDestination = LatLng(25.1000, 55.1000)
        val (otherOriginKey, otherDestinationKey) =
            TripHistoryKey.pairKeys(otherOrigin, otherDestination)
        val otherOdEntry =
            entry(
                timestamp = fixedNow - 10 * dayMs,
                durationSeconds = 888,
                originKey = otherOriginKey,
                destinationKey = otherDestinationKey,
            )
        seed(dao, otherModeEntry, otherOdEntry)

        repository.persistEntry(entry(timestamp = fixedNow, durationSeconds = 999))

        val fastestOdCount =
            dao.allEntries().count {
                it.originKey == originKey &&
                    it.destinationKey == destinationKey &&
                    it.mode == mode.name
            }
        assertEquals(50, fastestOdCount)
        assertEquals(1, dao.allEntries().count { it.durationSeconds == 777 })
        assertEquals(1, dao.allEntries().count { it.durationSeconds == 888 })
    }

    @Test
    fun priorTrips_oldEntriesDoNotAffectInsightMedianOrSalik() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        repeat(3) {
            seed(
                dao,
                entry(
                    timestamp = fixedNow - retentionMs - 1_000L - it,
                    durationSeconds = 3_600,
                    hasSalik = true,
                ),
            )
        }
        repeat(3) {
            seed(
                dao,
                entry(
                    timestamp = fixedNow - (it + 1) * dayMs,
                    durationSeconds = 600,
                    hasSalik = false,
                ),
            )
        }

        val prior = repository.priorTripsForOdMode(origin, destination, mode)
        val today = entry(timestamp = fixedNow, durationSeconds = 620, hasSalik = false)
        val insight = DecisionHistoryInsight.compute(prior, today)

        assertEquals(3, prior.size)
        assertEquals("Typical handoff pattern for this trip", insight?.durationLine)
        assertNull(insight?.salikLine)
    }

    @Test
    fun runRetentionMaintenance_deletesOlderThanCutoffBeforeCountCap() = runBlocking {
        val dao = FakeTripHistoryDao()
        val repository = TripHistoryRepository.createForTest(dao) { fixedNow }
        seed(
            dao,
            entry(timestamp = fixedNow - retentionMs - 1, durationSeconds = 100),
        )
        repeat(52) { index ->
            seed(
                dao,
                entry(
                    timestamp = fixedNow - index * 60_000L,
                    durationSeconds = 200 + index,
                ),
            )
        }

        repository.runRetentionMaintenance(
            entry(timestamp = fixedNow, durationSeconds = 999),
        )

        assertTrue(dao.allEntries().none { it.durationSeconds == 100 })
        assertEquals(50, dao.allEntries().size)
    }

    private suspend fun seed(dao: FakeTripHistoryDao, vararg entries: TripHistoryEntry) {
        entries.forEach { entry ->
            dao.insert(entry.toEntity())
        }
    }

    private fun entry(
        timestamp: Long,
        durationSeconds: Int,
        hasSalik: Boolean = false,
        originKey: String = this.originKey,
        destinationKey: String = this.destinationKey,
        mode: PreferenceMode = this.mode,
    ): TripHistoryEntry =
        TripHistoryEntry(
            timestamp = timestamp,
            originKey = originKey,
            destinationKey = destinationKey,
            durationSeconds = durationSeconds,
            mode = mode,
            hasSalik = hasSalik,
            tollAed = if (hasSalik) 8.0 else null,
        )
}
