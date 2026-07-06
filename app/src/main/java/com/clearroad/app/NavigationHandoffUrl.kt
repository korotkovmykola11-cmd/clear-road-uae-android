package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Google Maps mobile handoff supports at most 3 intermediate waypoints reliably. */
private const val MAX_GOOGLE_MAPS_WAYPOINTS = 3
private const val WAYPOINT_ORIGIN_BUFFER_METERS = 80.0
private const val WAYPOINT_MIN_SPACING_METERS = 120.0

internal fun latLngParam(latLng: LatLng): String =
    "${latLng.latitude},${latLng.longitude}"

private fun encodeLatLngForUrl(latLng: LatLng): String =
    "${latLng.latitude}%2C${latLng.longitude}"

/**
 * Builds a Google Maps directions URL that routes through [waypoints].
 * Uses `/maps/dir/` path segments (lat,lng) — the `via:` query prefix is not supported
 * on Android Google Maps and is treated as a literal address search.
 */
internal fun googleMapsDirectionsUrl(
    origin: LatLng,
    destination: LatLng,
    waypoints: List<LatLng> = emptyList(),
): String {
    if (waypoints.isEmpty()) {
        return "https://www.google.com/maps/dir/?api=1" +
            "&origin=${encodeLatLngForUrl(origin)}" +
            "&destination=${encodeLatLngForUrl(destination)}" +
            "&travelmode=driving"
    }

    val path =
        (listOf(origin) + waypoints + destination)
            .joinToString("/") { encodeLatLngForUrl(it) }
    return "https://www.google.com/maps/dir/$path?api=1&travelmode=driving"
}

internal fun sampleNavigationWaypoints(
    path: List<LatLng>,
    origin: LatLng,
    destination: LatLng,
    maxWaypoints: Int = MAX_GOOGLE_MAPS_WAYPOINTS,
): List<LatLng> {
    if (path.size < 3 || maxWaypoints <= 0) return emptyList()

    val cumulativeDistances = ArrayList<Double>(path.size).apply { add(0.0) }
    for (index in 1 until path.size) {
        val segmentMeters = haversineDistanceMeters(path[index - 1], path[index])
        cumulativeDistances += cumulativeDistances.last() + segmentMeters
    }
    val totalMeters = cumulativeDistances.last()
    if (totalMeters < WAYPOINT_MIN_SPACING_METERS * 2) return emptyList()

    val eligibleIndices =
        path.indices.filter { index ->
            val point = path[index]
            haversineDistanceMeters(point, origin) > WAYPOINT_ORIGIN_BUFFER_METERS &&
                haversineDistanceMeters(point, destination) > WAYPOINT_ORIGIN_BUFFER_METERS
        }
    if (eligibleIndices.isEmpty()) return emptyList()

    val targetCount = maxWaypoints.coerceAtMost(eligibleIndices.size)
    val sampled = LinkedHashSet<LatLng>()
    for (slot in 1..targetCount) {
        val targetMeters = totalMeters * slot / (targetCount + 1).toDouble()
        val nearestIndex =
            cumulativeDistances.indices.minByOrNull { index ->
                kotlin.math.abs(cumulativeDistances[index] - targetMeters)
            } ?: continue
        if (nearestIndex !in eligibleIndices) continue
        val candidate = path[nearestIndex]
        val tooClose =
            sampled.any {
                haversineDistanceMeters(it, candidate) < WAYPOINT_MIN_SPACING_METERS
            }
        if (!tooClose) {
            sampled += candidate
        }
    }
    return sampled.toList()
}

private fun haversineDistanceMeters(
    left: LatLng,
    right: LatLng,
): Double {
    val earthRadiusMeters = 6_371_000.0
    val lat1 = Math.toRadians(left.latitude)
    val lat2 = Math.toRadians(right.latitude)
    val deltaLat = Math.toRadians(right.latitude - left.latitude)
    val deltaLng = Math.toRadians(right.longitude - left.longitude)
    val a =
        sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}
