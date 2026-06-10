package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.SalikDetection
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DIRECTIONS_AUDIT_TAG = "DirectionsAudit"

/** UI route numbers are 1-based (Route 1, Route 2, Route 3). */
internal data class RouteAuditMetrics(
    val routeIndex: Int,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val trafficDelaySeconds: Int,
    val tollCount: Int,
    val polylinePointCount: Int,
    val polylinePathMeters: Int,
)

internal data class RoutePairComparison(
    val left: RouteAuditMetrics,
    val right: RouteAuditMetrics,
    val durationDeltaSeconds: Int,
    val distanceDeltaMeters: Int,
    val trafficDelayDeltaSeconds: Int,
    val tollCountDelta: Int,
    val polylinePointCountDelta: Int,
    val polylinePathDeltaMeters: Int,
    val nearDuplicate: Boolean,
)

internal fun directionsAuditSalikCount(item: RealRouteDebugData): Int {
    val scan = item.corridorScanText.lowercase(Locale.US)
    val tollRoadCount = SalikDetection.countOccurrences(scan, "toll road")
    if (tollRoadCount > 0) return tollRoadCount
    if (item.tollAED > 0) {
        return (item.tollAED + SalikDetection.AED_PER_TOLL_ROAD - 1) /
            SalikDetection.AED_PER_TOLL_ROAD
    }
    return 0
}

internal fun directionsAuditTrafficDelaySeconds(route: RealRouteDebugData): Int {
    val trafficSeconds =
        route.durationInTrafficSeconds?.takeIf { it > 0 }
            ?: route.durationSeconds.takeIf { it > 0 }
            ?: 0
    val baseSeconds =
        route.baseDurationSeconds.takeIf { it > 0 } ?: trafficSeconds
    return (trafficSeconds - baseSeconds).coerceAtLeast(0)
}

internal fun directionsAuditPolylinePathMeters(points: List<LatLng>): Int {
    if (points.size < 2) return 0
    var total = 0.0
    for (index in 1 until points.size) {
        total += SphericalUtil.computeDistanceBetween(points[index - 1], points[index])
    }
    return total.roundToInt()
}

internal fun routeAuditMetrics(
    route: RealRouteDebugData,
    zeroBasedIndex: Int,
): RouteAuditMetrics =
    RouteAuditMetrics(
        routeIndex = zeroBasedIndex + 1,
        durationSeconds = route.durationSeconds,
        distanceMeters = route.distanceMeters,
        trafficDelaySeconds = directionsAuditTrafficDelaySeconds(route),
        tollCount = directionsAuditSalikCount(route),
        polylinePointCount = route.routePathPoints.size,
        polylinePathMeters = directionsAuditPolylinePathMeters(route.routePathPoints),
    )

internal fun compareRoutePair(
    left: RealRouteDebugData,
    leftZeroBasedIndex: Int,
    right: RealRouteDebugData,
    rightZeroBasedIndex: Int,
): RoutePairComparison {
    val leftMetrics = routeAuditMetrics(left, leftZeroBasedIndex)
    val rightMetrics = routeAuditMetrics(right, rightZeroBasedIndex)
    val durationDelta = abs(leftMetrics.durationSeconds - rightMetrics.durationSeconds)
    val distanceDelta = abs(leftMetrics.distanceMeters - rightMetrics.distanceMeters)
    val trafficDelayDelta =
        abs(leftMetrics.trafficDelaySeconds - rightMetrics.trafficDelaySeconds)
    val tollCountDelta = abs(leftMetrics.tollCount - rightMetrics.tollCount)
    val polylinePointDelta =
        abs(leftMetrics.polylinePointCount - rightMetrics.polylinePointCount)
    val polylinePathDelta =
        abs(leftMetrics.polylinePathMeters - rightMetrics.polylinePathMeters)
    val nearDuplicate =
        durationDelta <= 120 &&
            distanceDelta <= 1_000 &&
            tollCountDelta == 0 &&
            (
                polylinePathDelta <= 500 ||
                    (
                        leftMetrics.polylinePathMeters > 0 &&
                            polylinePathDelta.toDouble() /
                            leftMetrics.polylinePathMeters <= 0.05
                        )
                )
    return RoutePairComparison(
        left = leftMetrics,
        right = rightMetrics,
        durationDeltaSeconds = durationDelta,
        distanceDeltaMeters = distanceDelta,
        trafficDelayDeltaSeconds = trafficDelayDelta,
        tollCountDelta = tollCountDelta,
        polylinePointCountDelta = polylinePointDelta,
        polylinePathDeltaMeters = polylinePathDelta,
        nearDuplicate = nearDuplicate,
    )
}

private fun logRouteMetrics(
    route: RealRouteDebugData,
    metrics: RouteAuditMetrics,
) {
    Log.d(
        DIRECTIONS_AUDIT_TAG,
        "ROUTE_METRICS routeIndex=${metrics.routeIndex} " +
            "duration=${metrics.durationSeconds}s " +
            "baseDuration=${route.baseDurationSeconds}s " +
            "durationInTraffic=${route.durationInTrafficSeconds ?: "null"} " +
            "distanceMeters=${metrics.distanceMeters} " +
            "trafficDelay=${metrics.trafficDelaySeconds}s " +
            "tollCount=${metrics.tollCount} " +
            "polylinePoints=${metrics.polylinePointCount} " +
            "polylinePathMeters=${metrics.polylinePathMeters}",
    )
}

private fun logRoutePairComparison(comparison: RoutePairComparison) {
    val verdict = if (comparison.nearDuplicate) "NEAR_DUPLICATE" else "DISTINCT"
    Log.d(
        DIRECTIONS_AUDIT_TAG,
        "ROUTE_PAIR_COMPARE leftRoute=${comparison.left.routeIndex} " +
            "rightRoute=${comparison.right.routeIndex} " +
            "durationDeltaSec=${comparison.durationDeltaSeconds} " +
            "distanceDeltaMeters=${comparison.distanceDeltaMeters} " +
            "trafficDelayDeltaSec=${comparison.trafficDelayDeltaSeconds} " +
            "tollCountDelta=${comparison.tollCountDelta} " +
            "polylinePointsDelta=${comparison.polylinePointCountDelta} " +
            "polylinePathDeltaMeters=${comparison.polylinePathDeltaMeters} " +
            "verdict=$verdict",
    )
}

internal fun logDirectionsAuditRoutes(
    status: String?,
    routes: List<RealRouteDebugData>,
) {
    if (status != "OK") return
    routes.forEachIndexed { index, route ->
        logRouteMetrics(route, routeAuditMetrics(route, index))
        val trafficText =
            route.durationInTrafficText?.let { text ->
                val seconds = route.durationInTrafficSeconds
                if (seconds != null) {
                    "$text ($seconds sec)"
                } else {
                    text
                }
            } ?: "n/a"
        Log.d(
            DIRECTIONS_AUDIT_TAG,
            "Route $index | duration=${route.baseDurationText} " +
                "(${route.baseDurationSeconds} sec) | duration_in_traffic=$trafficText | " +
                "selected=${route.durationText} (${route.durationSeconds} sec) | " +
                "${route.distanceText} | Salik=${directionsAuditSalikCount(route)}",
        )
    }
    if (routes.size >= 3) {
        logRoutePairComparison(
            compareRoutePair(
                left = routes[1],
                leftZeroBasedIndex = 1,
                right = routes[2],
                rightZeroBasedIndex = 2,
            ),
        )
    }
}
