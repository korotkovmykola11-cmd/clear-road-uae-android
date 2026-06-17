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

    Log.d(
        DRIVER_STRESS_AUDIT_TAG,
        "routeCount=${routes.size} mode=$mode currentWinnerIndex=$winnerIdx",
    )
    metrics.forEach(::logDriverStressRouteMetrics)
    DriverStressAudit.buildSessionSummary(metrics, winnerIdx)?.let(::logDriverStressSessionSummary)
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
