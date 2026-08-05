package com.clearroad.app.triphistory

import com.clearroad.app.domain.PreferenceMode

/**
 * Compact handoff payload — built in RouteDetailsAssembly, persisted on navigation handoff.
 *
 * This is a decision snapshot at the moment of handoff intent — it does not confirm which route
 * Google/Waze actually used, nor that the trip was completed.
 */
data class TripHandoffSnapshotUiModel(
    val timestamp: Long,
    val originKey: String,
    val destinationKey: String,
    val mode: PreferenceMode,
    val handoffCandidateRouteIndex: Int,
    val marshioRecommendedRouteIndex: Int,
    val googleDefaultRouteIndex: Int,
    val handoffCandidateRouteIdentityKey: String,
    val handoffCandidateDurationSeconds: Int,
    val handoffCandidateHasSalik: Boolean,
    val handoffCandidateTollAed: Double?,
    val baselineRouteIdentityKey: String,
    val baselineDurationSeconds: Int,
    val baselineHasSalik: Boolean,
    val baselineTollAed: Double?,
    /** Predicted delta at decision time, not observed/actual time saved — Google Maps/Waze may route differently after handoff. */
    val timeDeltaVsBaselineSeconds: Int,
    val bestNoSalikDurationSeconds: Int?,
    val salikTimeDeltaSeconds: Int?,
    val alternatives: List<TripHandoffAlternativeUiModel>,
)

data class TripHandoffAlternativeUiModel(
    val routeIndex: Int,
    val routeIdentityKey: String,
    val durationSeconds: Int,
    val hasSalik: Boolean,
    val tollAed: Double?,
    val roleFlags: Int,
)

/** Result of a unified v1+v2 handoff persist attempt. */
data class TripHandoffPersistResult(
    val recorded: Boolean,
    val deduplicated: Boolean,
)
