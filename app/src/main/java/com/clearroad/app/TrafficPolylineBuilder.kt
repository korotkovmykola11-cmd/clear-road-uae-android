package com.clearroad.app

import com.google.android.gms.maps.model.LatLng

internal object TrafficPolylineBuilder {
    /**
     * Builds colored traffic segments for the map preview.
     * Falls back to a single MARSHIO-green segment when speed data is unavailable.
     */
    fun build(route: RealRouteDebugData): List<TrafficSegment> {
        val path = route.routePathPoints
        if (path.size < 2) return emptyList()

        if (route.trafficSpeedIntervals.isNotEmpty()) {
            return mergeConsecutive(
                buildFromPointIntervals(path, route.trafficSpeedIntervals),
            )
        }

        if (route.stepTrafficRecords.isNotEmpty()) {
            return mergeConsecutive(
                route.stepTrafficRecords.mapNotNull { step ->
                    if (step.points.size < 2) return@mapNotNull null
                    TrafficSegment(points = step.points, speedCategory = step.speedCategory)
                },
            )
        }

        // TODO: populate from Google Routes API speedReadingIntervals on RawRoute when available.
        return listOf(
            TrafficSegment(
                points = path,
                speedCategory = SpeedCategory.UNKNOWN,
            ),
        )
    }

    fun buildFromPointIntervals(
        path: List<LatLng>,
        intervals: List<RouteSpeedInterval>,
    ): List<TrafficSegment> {
        if (path.size < 2 || intervals.isEmpty()) return emptyList()
        val lastIndex = path.lastIndex
        return intervals.mapNotNull { interval ->
            val start = interval.startPointIndex.coerceIn(0, lastIndex)
            val end = interval.endPointIndex.coerceIn(start, lastIndex)
            val segmentPoints = path.subList(start, end + 1)
            if (segmentPoints.size < 2) return@mapNotNull null
            TrafficSegment(
                points = segmentPoints,
                speedCategory = interval.speedCategory,
            )
        }
    }

    internal fun mergeConsecutive(segments: List<TrafficSegment>): List<TrafficSegment> {
        if (segments.isEmpty()) return emptyList()
        val merged = mutableListOf<TrafficSegment>()
        var currentCategory = segments.first().speedCategory
        var currentPoints = segments.first().points.toMutableList()

        fun flush() {
            if (currentPoints.size >= 2) {
                merged += TrafficSegment(currentPoints.toList(), currentCategory)
            }
        }

        for (i in 1 until segments.size) {
            val segment = segments[i]
            if (segment.speedCategory == currentCategory) {
                appendPoints(currentPoints, segment.points)
            } else {
                flush()
                currentCategory = segment.speedCategory
                currentPoints = segment.points.toMutableList()
            }
        }
        flush()
        return merged
    }

    private fun appendPoints(
        target: MutableList<LatLng>,
        incoming: List<LatLng>,
    ) {
        if (incoming.isEmpty()) return
        val first = incoming.first()
        if (target.isNotEmpty() && samePoint(target.last(), first)) {
            target.addAll(incoming.drop(1))
        } else {
            target.addAll(incoming)
        }
    }

    private fun samePoint(
        a: LatLng,
        b: LatLng,
    ): Boolean = a.latitude == b.latitude && a.longitude == b.longitude
}
