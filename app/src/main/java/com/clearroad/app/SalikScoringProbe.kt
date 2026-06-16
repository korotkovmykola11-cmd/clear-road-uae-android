package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SalikDetection

private const val SALIK_SCORING_PROBE_TAG = "SalikScoringProbe"

internal fun effectiveTollAedForScoring(
    item: RealRouteDebugData,
    allRoutes: List<RealRouteDebugData> = emptyList(),
): Int {
    if (item.tollAED > 0) return item.tollAED
    if (!ArchitectureValidation.USE_HEURISTIC_SALIK_FOR_SCORING) return 0
    if (allRoutes.isNotEmpty() && allRoutes.all { it.tollAED == 0 }) return 0
    return SalikDetection.estimate(item.corridorScanText).estimatedSalikPenaltyAed
}

/** True when Google shows no toll on any route — heuristic Salik is not used for ranking. */
internal fun heuristicSalikInactiveForTrip(routes: List<RealRouteDebugData>): Boolean =
    routes.isNotEmpty() && routes.all { it.tollAED == 0 }

internal fun calculateLegacyRouteScore(
    mode: PreferenceMode,
    durationMinutes: Int,
    distanceKm: Double,
    tollAed: Double,
    totalCostAed: Double,
): Double {
    val aedPerMinute =
        calculateAedPerMinute(totalCostAed, durationMinutes)
    return when (mode) {
        PreferenceMode.FASTEST ->
            durationMinutes + totalCostAed * 0.15 + distanceKm * 0.05 +
                aedPerMinute * 0.2
        PreferenceMode.NO_TOLLS ->
            totalCostAed * 4.0 + tollAed * 6.0 + durationMinutes * 0.35 +
                aedPerMinute * 0.4
        PreferenceMode.CALM ->
            durationMinutes * 0.6 + totalCostAed * 1.2 + distanceKm * 0.15 +
                aedPerMinute * 0.3
    }
}

internal fun logSalikScoringProbe(
    routes: List<RealRouteDebugData>,
    mode: PreferenceMode,
    recommendedRouteIndex: Int,
) {
    if (!ArchitectureValidation.USE_HEURISTIC_SALIK_FOR_SCORING) return
    if (mode == PreferenceMode.CALM) {
        Log.d(
            SALIK_SCORING_PROBE_TAG,
            "CALM/SMOOTH: route selection uses SmoothDriveScoring tag; " +
                "legacy cost-weighted probe skipped",
        )
        return
    }
    routes.forEachIndexed { index, item ->
        val salikEstimate = SalikDetection.estimate(item.corridorScanText)
        val effectiveToll = effectiveTollAedForScoring(item, routes)
        val distanceKm = item.distanceMeters / 1000.0
        val fuelAed = estimateFuelCostAed(distanceKm)
        val totalCostAed =
            estimateTotalRouteCostAed(effectiveToll, fuelAed).toDouble()
        val legacyProbeScore =
            calculateLegacyRouteScore(
                mode,
                item.durationSeconds / 60,
                distanceKm,
                effectiveToll.toDouble(),
                totalCostAed,
            )
        Log.d(
            SALIK_SCORING_PROBE_TAG,
            "routeIndex=$index " +
                "duration=${item.durationText} " +
                "distance=${item.distanceText} " +
                "googleTollAED=${item.tollAED} " +
                "heuristicInactive=${heuristicSalikInactiveForTrip(routes)} " +
                "estimatedSalikPenaltyAed=${salikEstimate.estimatedSalikPenaltyAed} " +
                "exposure=${salikEstimate.exposure} " +
                "reason=${salikEstimate.reason} " +
                "selectedMode=$mode " +
                "legacyProbeScore=$legacyProbeScore " +
                "recommendedRouteIndex=$recommendedRouteIndex",
        )
    }
}
