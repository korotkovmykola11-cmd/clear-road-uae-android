package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.SalikDetection
import java.util.Locale

private const val DIRECTIONS_AUDIT_TAG = "DirectionsAudit"

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

internal fun logDirectionsAuditRoutes(
    status: String?,
    routes: List<RealRouteDebugData>,
) {
    if (status != "OK") return
    routes.forEachIndexed { index, route ->
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
}
