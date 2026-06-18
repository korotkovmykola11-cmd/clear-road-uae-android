package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.PreferenceMode
import java.util.Locale

private const val DRIVER_STRESS_AUDIT_TAG = "DRIVER_STRESS_AUDIT"

internal fun logDriverStressAudit(
    routes: List<RealRouteDebugData>,
    rawJson: String?,
    mode: PreferenceMode,
    currentWinnerIndex: Int,
) {
    if (routes.isEmpty()) {
        Log.d(DRIVER_STRESS_AUDIT_TAG, "routeCount=0")
        return
    }

    val routeJsonObjects = rawJson?.let(::extractAllRouteObjectJson).orEmpty()
    if (routeJsonObjects.isEmpty()) {
        Log.d(
            DRIVER_STRESS_AUDIT_TAG,
            "routeCount=${routes.size} mode=$mode rawJson=missing",
        )
        return
    }

    val winnerIdx = currentWinnerIndex.coerceIn(0, routes.lastIndex)
    val count = minOf(routes.size, routeJsonObjects.size)
    val metrics =
        (0 until count).map { index ->
            DriverStressAudit.metricsFromRouteJson(
                routeJson = routeJsonObjects[index],
                route = routes[index],
                routeIndex = index,
                isCurrentWinner = index == winnerIdx,
            )
        }
    val rankedMetrics = DriverStressAudit.attachStressRanks(metrics)

    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "routeCount=${routes.size} mode=$mode currentWinnerIndex=$winnerIdx",
    )
    rankedMetrics.forEach(::logDriverStressRouteMetrics)
    DriverStressAudit.buildSessionSummary(rankedMetrics, winnerIdx)
        ?.let(::logDriverStressSessionSummary)

    if (mode == PreferenceMode.CALM) {
        val calmWinnerIndex =
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                routes,
                PreferenceMode.CALM,
            )
        DriverStressAudit.buildCalmStressCorrelation(rankedMetrics, calmWinnerIndex)
            ?.let(::logCalmStressCorrelation)
        DriverStressAudit.buildCalmScoreBreakdowns(routes).forEach(::logCalmScoreBreakdown)
        DriverStressAudit.buildCalmAuditSummary(rankedMetrics, calmWinnerIndex)
            ?.let(::logCalmAuditSummary)
    }
}

private fun logDriverStressRouteMetrics(metrics: DriverStressRouteMetrics) {
    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "ROUTE route_index=${metrics.routeIndex} " +
            "is_current_winner=${metrics.isCurrentWinner} " +
            "duration_min=${metrics.durationMin} " +
            "duration_in_traffic_min=${metrics.durationInTrafficMin} " +
            "traffic_delay_min=${metrics.trafficDelayMin} " +
            "distance_km=${formatDouble(metrics.distanceKm)} " +
            "total_steps=${metrics.totalSteps} " +
            "steps_per_km=${formatDouble(metrics.stepsPerKm)} " +
            "critical_maneuvers_count=${metrics.criticalManeuversCount} " +
            "critical_maneuvers_per_km=${formatDouble(metrics.criticalManeuversPerKm)} " +
            "max_density_2km_segment=${metrics.maxDensity2KmSegment} " +
            "stressRankByCriticalCount=${metrics.stressRankByCriticalCount} " +
            "stressRankByStepsPerKm=${metrics.stressRankByStepsPerKm} " +
            "stressRankByDensity=${metrics.stressRankByDensity} " +
            "maneuver_keywords=${formatKeywordDistribution(metrics.keywordDistribution)}",
    )
}

private fun logDriverStressSessionSummary(summary: DriverStressSessionSummary) {
    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "SESSION currentWinnerIndex=${summary.currentWinnerIndex} " +
            "lowestStepsPerKmIndex=${summary.lowestStepsPerKmIndex} " +
            "lowestCriticalManeuverIndex=${summary.lowestCriticalManeuverIndex} " +
            "timeLostMinIfLowestStressChosen=${summary.timeLostMinIfLowestStressChosen} " +
            "maneuversSavedPctIfLowestStressChosen=${summary.maneuversSavedPctIfLowestStressChosen}",
    )
}

private fun logCalmStressCorrelation(correlation: CalmStressCorrelation) {
    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "CALM_CORRELATION calmWinnerIndex=${correlation.calmWinnerIndex} " +
            "bestCriticalIndex=${correlation.bestCriticalIndex} " +
            "bestStepsPerKmIndex=${correlation.bestStepsPerKmIndex} " +
            "bestDensityIndex=${correlation.bestDensityIndex} " +
            "winnerMatchesBestCritical=${correlation.winnerMatchesBestCritical} " +
            "winnerMatchesBestSteps=${correlation.winnerMatchesBestSteps} " +
            "winnerMatchesBestDensity=${correlation.winnerMatchesBestDensity}",
    )
}

private fun logCalmScoreBreakdown(breakdown: CalmScoreBreakdownLog) {
    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "CALM_SCORE_BREAKDOWN route=${breakdown.routeIndex} " +
            "delayScore=${formatDouble(breakdown.delayScore)} " +
            "corridorScore=${formatDouble(breakdown.corridorScore)} " +
            "trafficScore=${formatDouble(breakdown.trafficScore)} " +
            "distanceScore=${formatDouble(breakdown.distanceScore)} " +
            "finalScore=${formatDouble(breakdown.finalScore)} " +
            "delaySignalInactiveGuard=${breakdown.delaySignalInactiveGuardApplied}",
    )
}

private fun logCalmAuditSummary(summary: CalmAuditSummary) {
    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "CALM_AUDIT_SUMMARY winner=${summary.winner} " +
            "bestStressRoute=${summary.bestStressRoute} " +
            "stressGapPct=${summary.stressGapPct} " +
            "timePenaltyMin=${summary.timePenaltyMin} " +
            "calmPickedLowestStress=${summary.calmPickedLowestStress}",
    )
}

private fun formatDouble(value: Double): String =
    "%.2f".format(Locale.US, value)

private fun formatKeywordDistribution(distribution: Map<String, Int>): String =
    if (distribution.isEmpty()) {
        "none"
    } else {
        distribution.entries.joinToString(separator = ",") { (keyword, count) ->
            "$keyword:$count"
        }
    }
