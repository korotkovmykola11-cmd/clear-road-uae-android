package com.clearroad.app.triphistory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clearroad.app.domain.PreferenceMode

/**
 * Decision snapshot at the moment of handoff intent.
 *
 * Does not confirm which route Google Maps/Waze actually used, nor that the trip was completed.
 * [handoffCandidateRouteIdentityKey] marks the route shown on Route Details when the user tapped
 * handoff — not a confirmed Google navigation choice or driven path.
 */
@Entity(
    tableName = "trip_decision_snapshots",
    indices = [
        Index(value = ["originKey", "destinationKey", "mode"]),
        Index(value = ["timestamp"]),
        Index(value = ["handoffCandidateRouteIdentityKey"]),
    ],
)
internal data class TripDecisionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val originKey: String,
    val destinationKey: String,
    val mode: String,
    val handoffCandidateRouteIndex: Int,
    val handoffCandidateRouteIdentityKey: String,
    val handoffCandidateDurationSeconds: Int,
    val handoffCandidateHasSalik: Boolean,
    val handoffCandidateTollAed: Double?,
    val marshioRecommendedRouteIndex: Int,
    val googleDefaultRouteIndex: Int,
    val baselineRouteIdentityKey: String,
    val baselineDurationSeconds: Int,
    val baselineHasSalik: Boolean,
    val baselineTollAed: Double?,
    /** Predicted delta at decision time, not observed/actual time saved — Google Maps/Waze may route differently after handoff. */
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
        handoffCandidateRouteIndex = handoffCandidateRouteIndex,
        handoffCandidateRouteIdentityKey = handoffCandidateRouteIdentityKey,
        handoffCandidateDurationSeconds = handoffCandidateDurationSeconds,
        handoffCandidateHasSalik = handoffCandidateHasSalik,
        handoffCandidateTollAed = handoffCandidateTollAed,
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
        handoffCandidateRouteIndex = handoffCandidateRouteIndex,
        marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
        googleDefaultRouteIndex = googleDefaultRouteIndex,
        handoffCandidateRouteIdentityKey = handoffCandidateRouteIdentityKey,
        handoffCandidateDurationSeconds = handoffCandidateDurationSeconds,
        handoffCandidateHasSalik = handoffCandidateHasSalik,
        handoffCandidateTollAed = handoffCandidateTollAed,
        baselineRouteIdentityKey = baselineRouteIdentityKey,
        baselineDurationSeconds = baselineDurationSeconds,
        baselineHasSalik = baselineHasSalik,
        baselineTollAed = baselineTollAed,
        timeDeltaVsBaselineSeconds = timeDeltaVsBaselineSeconds,
        bestNoSalikDurationSeconds = bestNoSalikDurationSeconds,
        salikTimeDeltaSeconds = salikTimeDeltaSeconds,
        alternatives = alternatives,
    )
