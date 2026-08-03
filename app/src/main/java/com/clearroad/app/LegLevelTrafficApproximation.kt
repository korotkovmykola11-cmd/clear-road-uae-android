package com.clearroad.app

import com.google.android.gms.maps.model.LatLng

/**
 * When Legacy Directions returns leg-level [RealRouteDebugData.durationInTrafficSeconds]
 * but steps lack per-step `duration_in_traffic`, step parsing yields all [SpeedCategory.FREE].
 *
 * Legacy Directions API doesn't expose per-step traffic; this is a route-level delay
 * approximation, not per-segment accuracy.
 */
internal object LegLevelTrafficApproximation {

    internal fun speedCategoryFromDelayRatio(ratio: Double): SpeedCategory =
        when {
            ratio < 1.1 -> SpeedCategory.FREE
            ratio < 1.3 -> SpeedCategory.MODERATE
            ratio < 1.6 -> SpeedCategory.SLOW
            else -> SpeedCategory.JAM
        }

    internal fun legDelayRatio(route: RealRouteDebugData): Double? {
        val baseSeconds = route.baseDurationSeconds.takeIf { it > 0 } ?: return null
        val inTrafficSeconds = route.durationInTrafficSeconds ?: return null
        if (inTrafficSeconds <= baseSeconds) return null
        return inTrafficSeconds.toDouble() / baseSeconds.toDouble()
    }

    /**
     * Replace uniformly-FREE step segments when leg delay implies heavier traffic.
     * Skipped when step parsing already produced non-FREE categories (real per-step signal).
     */
    fun shouldOverrideUniformFreeSteps(
        route: RealRouteDebugData,
        stepDerivedSegments: List<TrafficSegment>,
    ): Boolean {
        val ratio = legDelayRatio(route) ?: return false
        if (speedCategoryFromDelayRatio(ratio) == SpeedCategory.FREE) return false
        if (stepDerivedSegments.isEmpty()) return true
        return stepDerivedSegments.all { it.speedCategory == SpeedCategory.FREE }
    }

    fun buildUniformRouteSegments(route: RealRouteDebugData): List<TrafficSegment>? {
        val path = route.routePathPoints
        if (path.size < 2) return null
        val ratio = legDelayRatio(route) ?: return null
        val category = speedCategoryFromDelayRatio(ratio)
        return listOf(
            TrafficSegment(
                points = path,
                speedCategory = category,
            ),
        )
    }
}
