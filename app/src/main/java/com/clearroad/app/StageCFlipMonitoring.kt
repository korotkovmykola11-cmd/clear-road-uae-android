package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.CalmStressTieBreak
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SmoothDriveScoring

/**
 * Post-flip monitoring tags for logcat analysis (Stage C Path B).
 * Filter: `STAGE_C_ROUTE_MONITOR` / `STAGE_C_CALM_MONITOR`.
 */
internal object StageCFlipMonitoring {

    const val ROUTE_MONITOR_TAG = "STAGE_C_ROUTE_MONITOR"
    const val CALM_MONITOR_TAG = "STAGE_C_CALM_MONITOR"

    fun logRouteFetch(
        provider: String,
        status: String?,
        routes: List<RealRouteDebugData>,
    ) {
        if (!ArchitectureValidation.USE_ROUTES_V2_FETCH) return
        val intervalSummary =
            routes.mapIndexed { index, route ->
                "r$index=intervals:${route.trafficSpeedIntervals.size}" +
                    " steps:${route.stepTrafficRecords.size}" +
                    " polylinePts:${route.routePathPoints.size}"
            }.joinToString(" ")
        Log.d(
            ROUTE_MONITOR_TAG,
            "provider=$provider status=$status routeCount=${routes.size} $intervalSummary",
        )
        routes.forEachIndexed { index, route ->
            val categories =
                route.trafficSpeedIntervals
                    .groupingBy { it.speedCategory.name }
                    .eachCount()
                    .entries
                    .joinToString(",") { "${it.key}=${it.value}" }
            Log.d(
                ROUTE_MONITOR_TAG,
                "route[$index] trafficSource=" +
                    if (route.trafficSpeedIntervals.isNotEmpty()) {
                        "speedReadingIntervals"
                    } else if (route.stepTrafficRecords.isNotEmpty()) {
                        "stepTrafficRecords"
                    } else {
                        "legApproximationOrUnknown"
                    } +
                    " corridor=${SmoothDriveScoring.classifyCorridor(route.corridorScanText)} " +
                    "speedCategories=[$categories]",
            )
        }
    }

    fun logCalmSelection(
        routes: List<RealRouteDebugData>,
        calmIndex: Int,
    ) {
        if (!ArchitectureValidation.USE_ROUTES_V2_FETCH) return
        if (routes.isEmpty()) return
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        val smoothWinner =
            if (inputs.size == routes.size) {
                SmoothDriveScoring.pickWinnerIndex(inputs)
            } else {
                calmIndex
            }
        val stressInputs = DriverStressAudit.buildCalmStressInputs(routes)
        val policy =
            if (stressInputs != null) {
                CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs)
            } else {
                null
            }
        val calmRoute = routes[calmIndex.coerceIn(0, routes.lastIndex)]
        Log.d(
            CALM_MONITOR_TAG,
            "calmIdx=$calmIndex smoothWin=$smoothWinner " +
                "override=${policy?.overrideApplied ?: false} " +
                "stressGap=${policy?.stressGapPct ?: 0}% " +
                "corridor=${StageCBaselineCaptureCompat.primaryCorridor(calmRoute)} " +
                "corridorClass=${SmoothDriveScoring.classifyCorridor(calmRoute.corridorScanText)} " +
                "crit=${calmRoute.criticalManeuversCount}",
        )
    }
}

/** Minimal corridor helper for prod monitoring without test-module dependency. */
private object StageCBaselineCaptureCompat {
    fun primaryCorridor(route: RealRouteDebugData): String {
        val scan = route.corridorScanText.lowercase()
        return when {
            scan.contains("e311") -> "E311"
            scan.contains("e11") || scan.contains("sheikh zayed") -> "E11"
            scan.contains("e611") || scan.contains("emirates road") -> "E611"
            else -> route.routeSummary.ifBlank { "MIXED" }.take(24)
        }
    }
}
