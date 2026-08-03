package com.clearroad.app

import android.util.Log

private const val TAG = "TrafficSegmentDiag"

internal object TrafficSegmentDebugLog {
    fun logBuiltSegments(
        route: RealRouteDebugData,
        segments: List<TrafficSegment>,
    ) {
        if (!BuildConfig.DEBUG) return

        val source =
            when {
                route.trafficSpeedIntervals.isNotEmpty() ->
                    "speedReadingIntervals(count=${route.trafficSpeedIntervals.size})"
                route.stepTrafficRecords.isNotEmpty() ->
                    "stepDurationInTraffic(count=${route.stepTrafficRecords.size})"
                else -> "fallback_UNKNOWN"
            }
        val counts = categoryCounts(segments)
        Log.d(
            TAG,
            "trafficSegments source=$source " +
                "pathPoints=${route.routePathPoints.size} " +
                "segmentCount=${segments.size} " +
                formatCounts(counts),
        )
    }

    internal fun categoryCounts(segments: List<TrafficSegment>): Map<SpeedCategory, Int> =
        segments.groupingBy { it.speedCategory }.eachCount()

    internal fun formatCounts(counts: Map<SpeedCategory, Int>): String =
        buildString {
            append("FREE=").append(counts[SpeedCategory.FREE] ?: 0)
            append(" MODERATE=").append(counts[SpeedCategory.MODERATE] ?: 0)
            append(" SLOW=").append(counts[SpeedCategory.SLOW] ?: 0)
            append(" JAM=").append(counts[SpeedCategory.JAM] ?: 0)
            append(" UNKNOWN=").append(counts[SpeedCategory.UNKNOWN] ?: 0)
        }
}
