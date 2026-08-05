package com.clearroad.app.triphistory

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trip_decision_snapshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    originKey TEXT NOT NULL,
                    destinationKey TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    viewedRouteIndex INTEGER NOT NULL,
                    chosenRouteIdentityKey TEXT NOT NULL,
                    chosenDurationSeconds INTEGER NOT NULL,
                    chosenHasSalik INTEGER NOT NULL,
                    chosenTollAed REAL,
                    marshioRecommendedRouteIndex INTEGER NOT NULL,
                    googleDefaultRouteIndex INTEGER NOT NULL,
                    baselineRouteIdentityKey TEXT NOT NULL,
                    baselineDurationSeconds INTEGER NOT NULL,
                    baselineHasSalik INTEGER NOT NULL,
                    baselineTollAed REAL,
                    timeDeltaVsBaselineSeconds INTEGER NOT NULL,
                    bestNoSalikDurationSeconds INTEGER,
                    salikTimeDeltaSeconds INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_decision_snapshots_originKey_destinationKey_mode
                ON trip_decision_snapshots (originKey, destinationKey, mode)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_decision_snapshots_timestamp
                ON trip_decision_snapshots (timestamp)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_decision_snapshots_chosenRouteIdentityKey
                ON trip_decision_snapshots (chosenRouteIdentityKey)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trip_decision_alternatives (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    snapshotId INTEGER NOT NULL,
                    routeIndex INTEGER NOT NULL,
                    routeIdentityKey TEXT NOT NULL,
                    durationSeconds INTEGER NOT NULL,
                    hasSalik INTEGER NOT NULL,
                    tollAed REAL,
                    roleFlags INTEGER NOT NULL,
                    FOREIGN KEY(snapshotId) REFERENCES trip_decision_snapshots(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_trip_decision_alternatives_snapshotId
                ON trip_decision_alternatives (snapshotId)
                """.trimIndent(),
            )
        }
    }
