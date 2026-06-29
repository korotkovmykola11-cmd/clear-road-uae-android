package com.clearroad.app.guidance

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Polyline progress for MARSHIO Guidance — presentation only. */
internal object MarshioGuidanceSimulation {

    private const val SIMULATION_DURATION_MS = 45_000L
    private const val OFF_ROUTE_THRESHOLD_METERS = 50.0

    data class State(
        val position: LatLng,
        val traveledMeters: Double,
        val remainingDistanceMeters: Double,
        val remainingDurationSeconds: Int,
        val offRoute: Boolean,
        val statusLabel: String = "Following MARSHIO route",
        val bearingDegrees: Float? = null,
    )

    data class PolylineProjection(
        val closestPoint: LatLng,
        val traveledMeters: Double,
        val distanceToRouteMeters: Double,
        val segmentBearingDegrees: Float?,
    )

    fun stateAtPosition(
        position: LatLng,
        path: List<LatLng>,
        totalDurationSeconds: Int,
        gpsBearingDegrees: Float? = null,
    ): State {
        if (path.isEmpty()) {
            return State(
                position = position,
                traveledMeters = 0.0,
                remainingDistanceMeters = 0.0,
                remainingDurationSeconds = 0,
                offRoute = false,
                bearingDegrees = gpsBearingDegrees,
            )
        }

        val totalMeters = polylineLengthMeters(path).coerceAtLeast(1.0)
        val projection = projectOntoPolyline(position, path)
        val offRoute = projection.distanceToRouteMeters > OFF_ROUTE_THRESHOLD_METERS
        val remainingMeters =
            (totalMeters - projection.traveledMeters).coerceAtLeast(0.0)
        val remainingSeconds =
            estimateRemainingSeconds(
                totalDurationSeconds = totalDurationSeconds,
                totalMeters = totalMeters,
                remainingMeters = remainingMeters,
            )

        return State(
            position = if (offRoute) position else projection.closestPoint,
            traveledMeters = projection.traveledMeters,
            remainingDistanceMeters = remainingMeters,
            remainingDurationSeconds = remainingSeconds,
            offRoute = offRoute,
            statusLabel = if (offRoute) "Off-route" else "Following MARSHIO route",
            bearingDegrees = gpsBearingDegrees ?: projection.segmentBearingDegrees,
        )
    }

    internal fun projectOntoPolyline(
        position: LatLng,
        path: List<LatLng>,
    ): PolylineProjection {
        if (path.isEmpty()) {
            return PolylineProjection(
                closestPoint = position,
                traveledMeters = 0.0,
                distanceToRouteMeters = 0.0,
                segmentBearingDegrees = null,
            )
        }
        if (path.size == 1) {
            val distance = haversineDistanceMeters(position, path.first())
            return PolylineProjection(
                closestPoint = path.first(),
                traveledMeters = 0.0,
                distanceToRouteMeters = distance,
                segmentBearingDegrees = null,
            )
        }

        var bestDistance = Double.MAX_VALUE
        var bestPoint = path.first()
        var bestTraveled = 0.0
        var bestBearing: Float? = null
        var traveledBeforeSegment = 0.0

        for (index in 1 until path.size) {
            val start = path[index - 1]
            val end = path[index]
            val segmentMeters = haversineDistanceMeters(start, end)
            val projection =
                projectPointOntoSegment(
                    point = position,
                    segmentStart = start,
                    segmentEnd = end,
                )
            if (projection.distanceMeters < bestDistance) {
                bestDistance = projection.distanceMeters
                bestPoint = projection.point
                bestTraveled = traveledBeforeSegment + (segmentMeters * projection.fractionAlongSegment)
                bestBearing = bearingDegrees(start, end)
            }
            traveledBeforeSegment += segmentMeters
        }

        return PolylineProjection(
            closestPoint = bestPoint,
            traveledMeters = bestTraveled.coerceAtLeast(0.0),
            distanceToRouteMeters = bestDistance,
            segmentBearingDegrees = bestBearing,
        )
    }

    internal data class SegmentProjection(
        val point: LatLng,
        val distanceMeters: Double,
        val fractionAlongSegment: Double,
    )

    internal fun projectPointOntoSegment(
        point: LatLng,
        segmentStart: LatLng,
        segmentEnd: LatLng,
    ): SegmentProjection {
        val startX = segmentStart.longitude
        val startY = segmentStart.latitude
        val endX = segmentEnd.longitude
        val endY = segmentEnd.latitude
        val pointX = point.longitude
        val pointY = point.latitude

        val dx = endX - startX
        val dy = endY - startY
        if (dx == 0.0 && dy == 0.0) {
            val distance = haversineDistanceMeters(point, segmentStart)
            return SegmentProjection(
                point = segmentStart,
                distanceMeters = distance,
                fractionAlongSegment = 0.0,
            )
        }

        val fraction =
            (((pointX - startX) * dx + (pointY - startY) * dy) / (dx * dx + dy * dy))
                .coerceIn(0.0, 1.0)
        val projected =
            LatLng(
                startY + dy * fraction,
                startX + dx * fraction,
            )
        return SegmentProjection(
            point = projected,
            distanceMeters = haversineDistanceMeters(point, projected),
            fractionAlongSegment = fraction,
        )
    }

    internal fun bearingDegrees(
        start: LatLng,
        end: LatLng,
    ): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)
        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    fun polylineLengthMeters(path: List<LatLng>): Double {
        if (path.size < 2) return 0.0
        var total = 0.0
        for (index in 1 until path.size) {
            total += haversineDistanceMeters(path[index - 1], path[index])
        }
        return total
    }

    fun stateAtProgress(
        path: List<LatLng>,
        totalDurationSeconds: Int,
        progressFraction: Float,
        simulatedOffRoute: Boolean = false,
    ): State {
        if (path.isEmpty()) {
            return State(
                position = LatLng(0.0, 0.0),
                traveledMeters = 0.0,
                remainingDistanceMeters = 0.0,
                remainingDurationSeconds = 0,
                offRoute = simulatedOffRoute,
            )
        }

        val totalMeters = polylineLengthMeters(path).coerceAtLeast(1.0)
        val clampedProgress = progressFraction.coerceIn(0f, 1f)
        val traveledMeters = totalMeters * clampedProgress
        val remainingMeters = (totalMeters - traveledMeters).coerceAtLeast(0.0)
        val remainingSeconds =
            estimateRemainingSeconds(
                totalDurationSeconds = totalDurationSeconds,
                totalMeters = totalMeters,
                remainingMeters = remainingMeters,
            )

        return State(
            position = positionAtDistance(path, traveledMeters),
            traveledMeters = traveledMeters,
            remainingDistanceMeters = remainingMeters,
            remainingDurationSeconds = remainingSeconds,
            offRoute = simulatedOffRoute,
        )
    }

    fun simulationProgressFraction(elapsedMs: Long): Float {
        val looped = elapsedMs % SIMULATION_DURATION_MS
        return looped.toFloat() / SIMULATION_DURATION_MS.toFloat()
    }

    fun isOffRoute(
        position: LatLng,
        path: List<LatLng>,
    ): Boolean {
        if (path.isEmpty()) return false
        return projectOntoPolyline(position, path).distanceToRouteMeters > OFF_ROUTE_THRESHOLD_METERS
    }

    internal fun positionAtDistance(
        path: List<LatLng>,
        distanceMeters: Double,
    ): LatLng {
        if (path.isEmpty()) return LatLng(0.0, 0.0)
        if (path.size == 1) return path.first()

        var remaining = distanceMeters.coerceAtLeast(0.0)
        for (index in 1 until path.size) {
            val start = path[index - 1]
            val end = path[index]
            val segmentMeters = haversineDistanceMeters(start, end)
            if (segmentMeters <= 0.0) continue
            if (remaining <= segmentMeters) {
                val fraction = (remaining / segmentMeters).coerceIn(0.0, 1.0)
                return interpolate(start, end, fraction)
            }
            remaining -= segmentMeters
        }
        return path.last()
    }

    internal fun estimateRemainingSeconds(
        totalDurationSeconds: Int,
        totalMeters: Double,
        remainingMeters: Double,
    ): Int {
        if (totalMeters <= 0.0 || totalDurationSeconds <= 0) return 0
        val ratio = (remainingMeters / totalMeters).coerceIn(0.0, 1.0)
        return (totalDurationSeconds * ratio).toInt().coerceAtLeast(0)
    }

    private fun interpolate(
        start: LatLng,
        end: LatLng,
        fraction: Double,
    ): LatLng =
        LatLng(
            start.latitude + (end.latitude - start.latitude) * fraction,
            start.longitude + (end.longitude - start.longitude) * fraction,
        )

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
}
