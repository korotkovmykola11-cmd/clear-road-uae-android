package com.clearroad.app.triphistory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clearroad.app.domain.PreferenceMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TripHistoryDatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration1to2_preservesTripHistoryAndCreatesV2Tables() = runBlocking {
        createV1DatabaseWithSeedRow()

        val migrated =
            Room.databaseBuilder(context, TripHistoryDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()

        val preserved =
            migrated.tripHistoryDao().getRecentForOdMode(
                originKey = "origin_v1",
                destinationKey = "dest_v1",
                mode = PreferenceMode.FASTEST.name,
                minTimestamp = 0,
                limit = 10,
            )
        assertEquals(1, preserved.size)
        assertEquals(600, preserved.single().durationSeconds)

        val snapshotId =
            migrated.tripDecisionSnapshotDao().insert(
                TripDecisionSnapshotEntity(
                    timestamp = 1_700_000_000_000L,
                    originKey = "origin_v1",
                    destinationKey = "dest_v1",
                    mode = PreferenceMode.FASTEST.name,
                    viewedRouteIndex = 0,
                    chosenRouteIdentityKey = "chosen",
                    chosenDurationSeconds = 620,
                    chosenHasSalik = false,
                    chosenTollAed = null,
                    marshioRecommendedRouteIndex = 0,
                    googleDefaultRouteIndex = 0,
                    baselineRouteIdentityKey = "baseline",
                    baselineDurationSeconds = 700,
                    baselineHasSalik = false,
                    baselineTollAed = null,
                    timeDeltaVsBaselineSeconds = 80,
                    bestNoSalikDurationSeconds = 620,
                    salikTimeDeltaSeconds = null,
                ),
            )
        migrated.tripDecisionSnapshotDao().insertAlternatives(
            listOf(
                TripDecisionAlternativeEntity(
                    snapshotId = snapshotId,
                    routeIndex = 0,
                    routeIdentityKey = "chosen",
                    durationSeconds = 620,
                    hasSalik = false,
                    tollAed = null,
                    roleFlags = TripDecisionAlternativeRole.CHOSEN,
                ),
            ),
        )
        assertEquals(1, migrated.tripDecisionSnapshotDao().countAlternativesForSnapshot(snapshotId))

        migrated.close()

        assertTrue(indexExists("index_trip_decision_snapshots_originKey_destinationKey_mode"))
        assertTrue(indexExists("index_trip_decision_snapshots_timestamp"))
        assertTrue(indexExists("index_trip_decision_snapshots_chosenRouteIdentityKey"))
        assertTrue(indexExists("index_trip_decision_alternatives_snapshotId"))
    }

    @Test
    fun migration1to2_foreignKeyCascadeDeletesAlternatives() = runBlocking {
        createV1DatabaseWithSeedRow()

        val migrated =
            Room.databaseBuilder(context, TripHistoryDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()

        val snapshotId =
            migrated.tripDecisionSnapshotDao().insert(
                TripDecisionSnapshotEntity(
                    timestamp = 1_700_000_000_000L,
                    originKey = "origin_v1",
                    destinationKey = "dest_v1",
                    mode = PreferenceMode.FASTEST.name,
                    viewedRouteIndex = 0,
                    chosenRouteIdentityKey = "chosen",
                    chosenDurationSeconds = 620,
                    chosenHasSalik = false,
                    chosenTollAed = null,
                    marshioRecommendedRouteIndex = 0,
                    googleDefaultRouteIndex = 0,
                    baselineRouteIdentityKey = "baseline",
                    baselineDurationSeconds = 700,
                    baselineHasSalik = false,
                    baselineTollAed = null,
                    timeDeltaVsBaselineSeconds = 80,
                    bestNoSalikDurationSeconds = 620,
                    salikTimeDeltaSeconds = null,
                ),
            )
        migrated.tripDecisionSnapshotDao().insertAlternatives(
            listOf(
                TripDecisionAlternativeEntity(
                    snapshotId = snapshotId,
                    routeIndex = 0,
                    routeIdentityKey = "chosen",
                    durationSeconds = 620,
                    hasSalik = false,
                    tollAed = null,
                    roleFlags = TripDecisionAlternativeRole.CHOSEN,
                ),
            ),
        )
        assertEquals(1, migrated.tripDecisionSnapshotDao().countAlternativesForSnapshot(snapshotId))

        migrated.tripDecisionSnapshotDao().deleteSnapshotById(snapshotId)

        assertEquals(0, migrated.tripDecisionSnapshotDao().countAlternativesForSnapshot(snapshotId))
        migrated.close()
    }

    private fun createV1DatabaseWithSeedRow() {
        context.openOrCreateDatabase(dbName, 0, null).use { db ->
            db.version = 1
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trip_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    originKey TEXT NOT NULL,
                    destinationKey TEXT NOT NULL,
                    durationSeconds INTEGER NOT NULL,
                    mode TEXT NOT NULL,
                    hasSalik INTEGER NOT NULL,
                    tollAed REAL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_history_originKey_destinationKey_mode
                ON trip_history (originKey, destinationKey, mode)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_history_timestamp
                ON trip_history (timestamp)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO trip_history (
                    timestamp, originKey, destinationKey, durationSeconds, mode, hasSalik, tollAed
                ) VALUES (
                    1699000000000, 'origin_v1', 'dest_v1', 600, 'FASTEST', 0, NULL
                )
                """.trimIndent(),
            )
        }
    }

    private fun indexExists(indexName: String): Boolean {
        context.openOrCreateDatabase(dbName, 0, null).use { db ->
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                arrayOf(indexName),
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }
    }
}
