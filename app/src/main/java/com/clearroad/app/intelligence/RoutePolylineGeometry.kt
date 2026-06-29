package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal data class IntelligenceBoundingBox(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    fun overpassSelector(): String = "$south,$west,$north,$east"
}

internal object RoutePolylineGeometry {

    const val NEAR_ROUTE_METERS = 60.0
    const val BBOX_EXPAND_METERS = 100.0

    private val MAIN_ROAD_HIGHWAYS =
        setOf(
            "primary",
            "primary_link",
            "trunk",
            "trunk_link",
            "motorway",
            "motorway_link",
        )

    fun stablePolylineHash(points: List<LatLng>): String {
        val normalized =
            points.joinToString("|") { point ->
                String.format(Locale.US, "%.5f,%.5f", point.latitude, point.longitude)
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun boundingBoxExpanded(
        points: List<LatLng>,
        expandMeters: Double = BBOX_EXPAND_METERS,
    ): IntelligenceBoundingBox? {
        if (points.isEmpty()) return null
        var south = points.first().latitude
        var north = points.first().latitude
        var west = points.first().longitude
        var east = points.first().longitude
        points.forEach { point ->
            south = min(south, point.latitude)
            north = max(north, point.latitude)
            west = min(west, point.longitude)
            east = max(east, point.longitude)
        }

        val southEdgeCenter = LatLng(south, (west + east) / 2.0)
        val northEdgeCenter = LatLng(north, (west + east) / 2.0)
        val westEdgeCenter = LatLng((south + north) / 2.0, west)
        val eastEdgeCenter = LatLng((south + north) / 2.0, east)

        south = SphericalUtil.computeOffset(southEdgeCenter, expandMeters, 180.0).latitude
        north = SphericalUtil.computeOffset(northEdgeCenter, expandMeters, 0.0).latitude
        west = SphericalUtil.computeOffset(westEdgeCenter, expandMeters, 270.0).longitude
        east = SphericalUtil.computeOffset(eastEdgeCenter, expandMeters, 90.0).longitude

        return IntelligenceBoundingBox(
            south = south.coerceIn(-90.0, 90.0),
            west = west.coerceIn(-180.0, 180.0),
            north = north.coerceIn(-90.0, 90.0),
            east = east.coerceIn(-180.0, 180.0),
        )
    }

    fun minDistanceToPolylineMeters(
        point: LatLng,
        path: List<LatLng>,
    ): Double {
        if (path.isEmpty()) return Double.MAX_VALUE
        if (path.size == 1) return SphericalUtil.computeDistanceBetween(point, path.first())
        var best = Double.MAX_VALUE
        for (index in 1 until path.size) {
            val distance = distanceToSegmentMeters(point, path[index - 1], path[index])
            if (distance < best) best = distance
        }
        return best
    }

    fun isNearRoute(
        point: LatLng,
        path: List<LatLng>,
        thresholdMeters: Double = NEAR_ROUTE_METERS,
    ): Boolean = minDistanceToPolylineMeters(point, path) <= thresholdMeters

    fun nearRouteLengthMeters(
        segmentPoints: List<LatLng>,
        path: List<LatLng>,
        thresholdMeters: Double = NEAR_ROUTE_METERS,
    ): Double {
        if (segmentPoints.size < 2) return 0.0
        var length = 0.0
        for (index in 1 until segmentPoints.size) {
            val start = segmentPoints[index - 1]
            val end = segmentPoints[index]
            val midpoint =
                LatLng(
                    (start.latitude + end.latitude) / 2.0,
                    (start.longitude + end.longitude) / 2.0,
                )
            if (isNearRoute(midpoint, path, thresholdMeters)) {
                length += SphericalUtil.computeDistanceBetween(start, end)
            }
        }
        return length
    }

    fun isMainRoadHighway(highway: String?): Boolean =
        highway != null && highway in MAIN_ROAD_HIGHWAYS

    internal fun distanceToSegmentMeters(
        point: LatLng,
        segmentStart: LatLng,
        segmentEnd: LatLng,
    ): Double {
        val startToEnd = SphericalUtil.computeDistanceBetween(segmentStart, segmentEnd)
        if (startToEnd <= 0.0) {
            return SphericalUtil.computeDistanceBetween(point, segmentStart)
        }
        val startToPoint = SphericalUtil.computeDistanceBetween(segmentStart, point)
        val endToPoint = SphericalUtil.computeDistanceBetween(segmentEnd, point)
        val headingStartToEnd = SphericalUtil.computeHeading(segmentStart, segmentEnd)
        val headingStartToPoint = SphericalUtil.computeHeading(segmentStart, point)
        val relativeAngle = Math.toRadians(normalizeAngle(headingStartToPoint - headingStartToEnd))
        val alongTrack = startToPoint * kotlin.math.cos(relativeAngle)
        when {
            alongTrack <= 0.0 -> return startToPoint
            alongTrack >= startToEnd -> return endToPoint
            else -> {
                val projected =
                    SphericalUtil.computeOffset(segmentStart, alongTrack, headingStartToEnd)
                return SphericalUtil.computeDistanceBetween(point, projected)
            }
        }
    }

    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle
        while (normalized > 180.0) normalized -= 360.0
        while (normalized < -180.0) normalized += 360.0
        return normalized
    }
}
