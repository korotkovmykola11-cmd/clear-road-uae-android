package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng

/**
 * Stage 0A — provider-neutral route candidate parsed from a frozen fixture.
 */
data class BenchmarkRouteCandidate(
    val candidateId: String,
    val caseId: String,
    val provider: String,
    val routeIndex: Int,
    val routeSummary: String,
    val corridorScanText: String,
    val routePathPoints: List<LatLng>,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val tollAed: Int,
    val sourceFixture: String,
) {
    val isValid: Boolean
        get() =
            routePathPoints.size >= 2 &&
                distanceMeters > 0 &&
                durationSeconds > 0
}
