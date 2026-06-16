package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.domain.SmoothDriveScoring

private const val COMPLEXITY_AUDIT_TAG = "ComplexityAudit"

internal data class RouteComplexityMetrics(
    val routeIndex: Int,
    val turnCount: Int,
    val leftTurnCount: Int,
    val rightTurnCount: Int,
    val roundaboutCount: Int,
    val uTurnCount: Int,
    val mergeCount: Int,
    val rampCount: Int,
    val keepCount: Int,
    val maneuverTagCount: Int,
    val stepCountEstimate: Int,
)

internal data class RouteComplexityAuditSnapshot(
    val routeIndex: Int,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val turnCount: Int,
    val leftTurnCount: Int,
    val roundaboutCount: Int,
    val uTurnCount: Int,
    val rampCount: Int,
    val mergeCount: Int,
    val polylinePathMeters: Int,
    val corridorClassification: SmoothDriveScoring.CorridorClass,
)

internal data class RouteComplexityCompareSnapshot(
    val durationSpreadSeconds: Int,
    val turnCountSpread: Int,
    val leftTurnSpread: Int,
    val complexityDiffersMoreThanTime: Boolean,
)

internal object RouteComplexityAudit {

    fun metricsFromRouteJson(
        routeJson: String,
        routeIndex: Int,
    ): RouteComplexityMetrics {
        val maneuvers = extractManeuverValuesFromRouteJson(routeJson)
        return metricsFromManeuvers(maneuvers, routeIndex)
    }

    internal fun metricsFromManeuvers(
        maneuvers: List<String>,
        routeIndex: Int,
    ): RouteComplexityMetrics {
        val normalized = maneuvers.map { it.lowercase() }
        return RouteComplexityMetrics(
            routeIndex = routeIndex,
            turnCount = normalized.count { isTurnManeuver(it) },
            leftTurnCount = normalized.count { isLeftTurnManeuver(it) },
            rightTurnCount = normalized.count { isRightTurnManeuver(it) },
            roundaboutCount = normalized.count { it.contains("roundabout") },
            uTurnCount = normalized.count { it.contains("uturn") },
            mergeCount = normalized.count { it.contains("merge") },
            rampCount = normalized.count { it.contains("ramp") },
            keepCount = normalized.count { it.startsWith("keep-") },
            maneuverTagCount = maneuvers.size,
            stepCountEstimate = maneuvers.size,
        )
    }

    internal fun buildSnapshots(
        routes: List<RealRouteDebugData>,
        routeJsonObjects: List<String>,
    ): List<RouteComplexityAuditSnapshot> {
        val count = minOf(routes.size, routeJsonObjects.size)
        return (0 until count).map { index ->
            val route = routes[index]
            val complexity =
                metricsFromRouteJson(routeJsonObjects[index], routeIndex = index)
            RouteComplexityAuditSnapshot(
                routeIndex = index,
                durationSeconds = route.durationSeconds,
                distanceMeters = route.distanceMeters,
                turnCount = complexity.turnCount,
                leftTurnCount = complexity.leftTurnCount,
                roundaboutCount = complexity.roundaboutCount,
                uTurnCount = complexity.uTurnCount,
                rampCount = complexity.rampCount,
                mergeCount = complexity.mergeCount,
                polylinePathMeters = directionsAuditPolylinePathMeters(route.routePathPoints),
                corridorClassification =
                    SmoothDriveScoring.classifyCorridor(route.corridorScanText),
            )
        }
    }

    internal fun buildCompareSnapshot(
        snapshots: List<RouteComplexityAuditSnapshot>,
        routes: List<RealRouteDebugData>,
    ): RouteComplexityCompareSnapshot? {
        if (snapshots.size < 2) return null
        val durationSpread = RouteIdentityPresentationPolicy.durationSpreadSeconds(routes)
        val turnSpread =
            (snapshots.maxOf { it.turnCount } - snapshots.minOf { it.turnCount }).coerceAtLeast(0)
        val leftTurnSpread =
            (snapshots.maxOf { it.leftTurnCount } - snapshots.minOf { it.leftTurnCount })
                .coerceAtLeast(0)
        return RouteComplexityCompareSnapshot(
            durationSpreadSeconds = durationSpread,
            turnCountSpread = turnSpread,
            leftTurnSpread = leftTurnSpread,
            complexityDiffersMoreThanTime = turnSpread > 0 && durationSpread <= 90,
        )
    }

    private fun isTurnManeuver(maneuver: String): Boolean =
        maneuver.startsWith("turn-") ||
            maneuver.contains("roundabout") ||
            maneuver.contains("uturn") ||
            maneuver.startsWith("fork-") ||
            maneuver.startsWith("ramp-")

    private fun isLeftTurnManeuver(maneuver: String): Boolean =
        maneuver.contains("left")

    private fun isRightTurnManeuver(maneuver: String): Boolean =
        maneuver.contains("right")
}

internal fun logRouteComplexityAudit(
    routes: List<RealRouteDebugData>,
    rawJson: String?,
) {
    if (routes.isEmpty()) return
    val routeJsonObjects = rawJson?.let(::extractAllRouteObjectJson).orEmpty()
    if (routeJsonObjects.isEmpty()) {
        Log.d(
            COMPLEXITY_AUDIT_TAG,
            "COMPLEXITY_AUDIT routeCount=${routes.size} rawJson=missing",
        )
        return
    }

    Log.d(
        COMPLEXITY_AUDIT_TAG,
        "COMPLEXITY_AUDIT routeCount=${routes.size}",
    )

    val snapshots = RouteComplexityAudit.buildSnapshots(routes, routeJsonObjects)
    snapshots.forEach(::logRouteComplexitySnapshot)

    RouteComplexityAudit.buildCompareSnapshot(snapshots, routes)?.let(::logRouteComplexityCompare)
}

private fun logRouteComplexitySnapshot(snapshot: RouteComplexityAuditSnapshot) {
    Log.d(COMPLEXITY_AUDIT_TAG, "ROUTE[${snapshot.routeIndex}]")
    Log.d(COMPLEXITY_AUDIT_TAG, "durationSeconds=${snapshot.durationSeconds}")
    Log.d(COMPLEXITY_AUDIT_TAG, "distanceMeters=${snapshot.distanceMeters}")
    Log.d(COMPLEXITY_AUDIT_TAG, "turn_count=${snapshot.turnCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "left_turn_count=${snapshot.leftTurnCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "roundabout_count=${snapshot.roundaboutCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "u_turn_count=${snapshot.uTurnCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "ramp_count=${snapshot.rampCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "merge_count=${snapshot.mergeCount}")
    Log.d(COMPLEXITY_AUDIT_TAG, "polylinePathMeters=${snapshot.polylinePathMeters}")
    Log.d(
        COMPLEXITY_AUDIT_TAG,
        "corridorClassification=${snapshot.corridorClassification}",
    )
}

private fun logRouteComplexityCompare(compare: RouteComplexityCompareSnapshot) {
    Log.d(COMPLEXITY_AUDIT_TAG, "COMPLEXITY_COMPARE")
    Log.d(COMPLEXITY_AUDIT_TAG, "durationSpreadSeconds=${compare.durationSpreadSeconds}")
    Log.d(COMPLEXITY_AUDIT_TAG, "turnCountSpread=${compare.turnCountSpread}")
    Log.d(COMPLEXITY_AUDIT_TAG, "leftTurnSpread=${compare.leftTurnSpread}")
    Log.d(
        COMPLEXITY_AUDIT_TAG,
        "complexityDiffersMoreThanTime=${compare.complexityDiffersMoreThanTime}",
    )
}
