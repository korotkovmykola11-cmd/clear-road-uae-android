package com.clearroad.app.triphistory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clearroad.app.domain.PreferenceMode

@Entity(
    tableName = "trip_decision_snapshots",
    indices = [
        Index(value = ["originKey", "destinationKey", "mode"]),
        Index(value = ["timestamp"]),
        Index(value = ["chosenRouteIdentityKey"]),
    ],
)
internal data class TripDecisionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val originKey: String,
    val destinationKey: String,
    val mode: String,
    val viewedRouteIndex: Int,
    val chosenRouteIdentityKey: String,
    val chosenDurationSeconds: Int,
    val chosenHasSalik: Boolean,
    val chosenTollAed: Double?,
    val marshioRecommendedRouteIndex: Int,
    val googleDefaultRouteIndex: Int,
    val baselineRouteIdentityKey: String,
    val baselineDurationSeconds: Int,
    val baselineHasSalik: Boolean,
    val baselineTollAed: Double?,
    val timeDeltaVsBaselineSeconds: Int,
    val bestNoSalikDurationSeconds: Int?,
    val salikTimeDeltaSeconds: Int?,
)

internal fun TripHandoffSnapshotUiModel.toEntity(): TripDecisionSnapshotEntity =
    TripDecisionSnapshotEntity(
        timestamp = timestamp,
        originKey = originKey,
        destinationKey = destinationKey,
        mode = mode.name,
        viewedRouteIndex = viewedRouteIndex,
        chosenRouteIdentityKey = chosenRouteIdentityKey,
        chosenDurationSeconds = chosenDurationSeconds,
        chosenHasSalik = chosenHasSalik,
        chosenTollAed = chosenTollAed,
        marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
        googleDefaultRouteIndex = googleDefaultRouteIndex,
        baselineRouteIdentityKey = baselineRouteIdentityKey,
        baselineDurationSeconds = baselineDurationSeconds,
        baselineHasSalik = baselineHasSalik,
        baselineTollAed = baselineTollAed,
        timeDeltaVsBaselineSeconds = timeDeltaVsBaselineSeconds,
        bestNoSalikDurationSeconds = bestNoSalikDurationSeconds,
        salikTimeDeltaSeconds = salikTimeDeltaSeconds,
    )

internal fun TripDecisionSnapshotEntity.toUiModel(
    alternatives: List<TripHandoffAlternativeUiModel>,
): TripHandoffSnapshotUiModel =
    TripHandoffSnapshotUiModel(
        timestamp = timestamp,
        originKey = originKey,
        destinationKey = destinationKey,
        mode = PreferenceMode.valueOf(mode),
        viewedRouteIndex = viewedRouteIndex,
        marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
        googleDefaultRouteIndex = googleDefaultRouteIndex,
        chosenRouteIdentityKey = chosenRouteIdentityKey,
        chosenDurationSeconds = chosenDurationSeconds,
        chosenHasSalik = chosenHasSalik,
        chosenTollAed = chosenTollAed,
        baselineRouteIdentityKey = baselineRouteIdentityKey,
        baselineDurationSeconds = baselineDurationSeconds,
        baselineHasSalik = baselineHasSalik,
        baselineTollAed = baselineTollAed,
        timeDeltaVsBaselineSeconds = timeDeltaVsBaselineSeconds,
        bestNoSalikDurationSeconds = bestNoSalikDurationSeconds,
        salikTimeDeltaSeconds = salikTimeDeltaSeconds,
        alternatives = alternatives,
    )
