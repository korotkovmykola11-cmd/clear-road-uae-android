package com.clearroad.app.triphistory

import com.clearroad.app.domain.PreferenceMode

/** Compact handoff payload — built in RouteDetailsAssembly, persisted on navigation handoff. */
data class TripHandoffSnapshotUiModel(
    val timestamp: Long,
    val originKey: String,
    val destinationKey: String,
    val mode: PreferenceMode,
    val viewedRouteIndex: Int,
    val marshioRecommendedRouteIndex: Int,
    val googleDefaultRouteIndex: Int,
    val chosenRouteIdentityKey: String,
    val chosenDurationSeconds: Int,
    val chosenHasSalik: Boolean,
    val chosenTollAed: Double?,
    val baselineRouteIdentityKey: String,
    val baselineDurationSeconds: Int,
    val baselineHasSalik: Boolean,
    val baselineTollAed: Double?,
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
