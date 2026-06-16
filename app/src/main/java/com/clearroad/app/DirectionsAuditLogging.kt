package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.RouteIdentityResolver
import com.clearroad.app.domain.SalikDetection
import com.clearroad.app.domain.SmoothDriveScoring
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.security.MessageDigest
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

/** Short stable id for comparing whether Google returned distinct geometries. */
internal fun directionsAuditPolylineShortHash(
    encodedPolyline: String?,
    decodedPoints: List<LatLng>,
): String {
    val source =
        when {
            !encodedPolyline.isNullOrBlank() -> "enc:$encodedPolyline"
            decodedPoints.isNotEmpty() ->
                "pts:" +
                    decodedPoints.joinToString("|") { point ->
                        "%.5f,%.5f".format(Locale.US, point.latitude, point.longitude)
                    }
            else -> "empty"
        }
    val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
    return digest.take(4).joinToString("") { byte -> "%02x".format(Locale.US, byte) }
}

internal fun uniquePolylineHashCount(routes: List<RealRouteDebugData>): Int =
    routes
        .map { route ->
            directionsAuditPolylineShortHash(
                encodedPolyline = null,
                decodedPoints = route.routePathPoints,
            )
        }
        .toSet()
        .size

internal fun uniquePolylineHashCountFromRaw(
    routes: List<RealRouteDebugData>,
    routeJsonObjects: List<String>,
): Int {
    val hashes =
        routes.indices.map { index ->
            val encoded =
                routeJsonObjects.getOrNull(index)?.let(::extractOverviewPolylinePoints)
            directionsAuditPolylineShortHash(
                encodedPolyline = encoded,
                decodedPoints = routes[index].routePathPoints,
            )
        }
    return hashes.toSet().size
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

private fun formatRouteLabels(labels: List<String>): String =
    if (labels.isEmpty()) {
        "n/a"
    } else {
        labels.joinToString(",")
    }

internal fun logDirectionsAuditRoutes(
    status: String?,
    routes: List<RealRouteDebugData>,
    rawJson: String? = null,
) {
    if (status != "OK") {
        Log.d(
            DIRECTIONS_AUDIT_TAG,
            "ROUTES_RECEIVED count=0 status=${status ?: "null"} " +
                "api=DirectionsJSON alternatives=true",
        )
        return
    }

    val routeJsonObjects = rawJson?.let(::extractAllRouteObjectJson).orEmpty()
    val identities = RouteIdentityResolver.resolveAll(routes)

    Log.d(
        DIRECTIONS_AUDIT_TAG,
        "ROUTES_RECEIVED count=${routes.size} api=DirectionsJSON alternatives=true " +
            "(classic Directions API — routeLabels absent unless migrated to Routes API v2)",
    )

    val polylineHashes = mutableListOf<String>()
    routes.forEachIndexed { index, route ->
        val routeJson = routeJsonObjects.getOrNull(index).orEmpty()
        val encodedPolyline =
            routeJson.takeIf { it.isNotBlank() }?.let(::extractOverviewPolylinePoints)
        val polylineHash =
            directionsAuditPolylineShortHash(
                encodedPolyline = encodedPolyline,
                decodedPoints = route.routePathPoints,
            )
        polylineHashes.add(polylineHash)

        val metrics = routeAuditMetrics(route, index)
        val identity = identities[index]
        val corridorClass =
            SmoothDriveScoring.classifyCorridor(route.corridorScanText)
        val routeLabels =
            if (routeJson.isNotBlank()) {
                extractRouteLabelsFromRouteJson(routeJson)
            } else {
                emptyList()
            }

        Log.d(
            DIRECTIONS_AUDIT_TAG,
            "ROUTE[$index] routeIndex=$index " +
                "duration=${route.durationSeconds}s " +
                "staticDuration=${route.baseDurationSeconds}s " +
                "trafficDelaySeconds=${metrics.trafficDelaySeconds} " +
                "distanceMeters=${route.distanceMeters} " +
                "tollCount=${metrics.tollCount} " +
                "routeLabels=${formatRouteLabels(routeLabels)} " +
                "routeName=${identity.fullName} " +
                "stableKey=${identity.stableKey} " +
                "summary=${route.routeSummary.ifBlank { "n/a" }} " +
                "polylineHash=$polylineHash " +
                "corridorClassification=$corridorClass " +
                "polylinePoints=${route.routePathPoints.size} " +
                "polylinePathMeters=${metrics.polylinePathMeters}",
        )

        logRouteMetrics(route, metrics)
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

    val uniqueByPolyline =
        if (routeJsonObjects.size == routes.size) {
            uniquePolylineHashCountFromRaw(routes, routeJsonObjects)
        } else {
            uniquePolylineHashCount(routes)
        }
    Log.d(
        DIRECTIONS_AUDIT_TAG,
        "UNIQUE_ROUTE_COUNT byPolyline=$uniqueByPolyline total=${routes.size}",
    )

    if (routes.size >= 2) {
        val nearDuplicatePairs =
            buildList {
                for (left in routes.indices) {
                    for (right in left + 1 until routes.size) {
                        val comparison =
                            compareRoutePair(
                                left = routes[left],
                                leftZeroBasedIndex = left,
                                right = routes[right],
                                rightZeroBasedIndex = right,
                            )
                        if (comparison.nearDuplicate) {
                            add("${left}↔${right}")
                        }
                    }
                }
            }
        Log.d(
            DIRECTIONS_AUDIT_TAG,
            "NEAR_DUPLICATE_PAIRS count=${nearDuplicatePairs.size} " +
                "pairs=${nearDuplicatePairs.joinToString(", ").ifBlank { "none" }}",
        )
    }

    identities
        .groupBy { it.stableKey }
        .filter { (_, group) -> group.size > 1 }
        .forEach { (stableKey, group) ->
            val distinctNames = group.map { it.fullName }.distinct()
            Log.d(
                DIRECTIONS_AUDIT_TAG,
                "IDENTITY_COLLAPSE stableKey=$stableKey count=${group.size} " +
                    "distinctNames=${distinctNames.size} names=$distinctNames",
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
