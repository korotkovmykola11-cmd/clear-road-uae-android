package com.clearroad.app.replay

/**
 * Stage 39A — frozen field route set for offline CALM policy replay.
 * Test-only; not wired to production selection.
 */
internal data class FieldRouteReplayRoute(
    val durationMin: Int,
    val durationInTrafficMin: Int,
    val distanceKm: Double,
    val criticalManeuversCount: Int,
) {
    fun toSmoothInput(): com.clearroad.app.domain.SmoothDriveScoring.RouteInput =
        com.clearroad.app.domain.SmoothDriveScoring.RouteInput(
            baseDurationSeconds = durationMin.coerceAtLeast(1) * 60,
            durationInTrafficSeconds = durationInTrafficMin.coerceAtLeast(1) * 60,
            distanceMeters = (distanceKm * 1000.0).toInt().coerceAtLeast(1),
            corridorText = "",
        )
}

internal enum class FieldBucket {
    PREV_PM,
    AM_RUSH,
    PM_RUSH,
}

internal data class FieldRouteSet(
    val id: String,
    val bucket: FieldBucket,
    val routes: List<FieldRouteReplayRoute>,
    /** Frozen `currentWinnerIndex` from field logcat — observed CALM selection. */
    val fieldCalmWinnerIndex: Int,
)
