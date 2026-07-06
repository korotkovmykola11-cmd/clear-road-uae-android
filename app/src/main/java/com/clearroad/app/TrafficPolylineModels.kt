package com.clearroad.app

import com.google.android.gms.maps.model.LatLng

enum class SpeedCategory {
    FREE,
    MODERATE,
    SLOW,
    JAM,
    UNKNOWN,
}

data class TrafficSegment(
    val points: List<LatLng>,
    val speedCategory: SpeedCategory,
)

/** Point-index intervals on the route overview polyline (Routes API speedReadingIntervals). */
internal data class RouteSpeedInterval(
    val startPointIndex: Int,
    val endPointIndex: Int,
    val speedCategory: SpeedCategory,
)

/** Step-level traffic when per-segment speed readings are unavailable. */
internal data class DirectionsStepTrafficRecord(
    val points: List<LatLng>,
    val speedCategory: SpeedCategory,
)
