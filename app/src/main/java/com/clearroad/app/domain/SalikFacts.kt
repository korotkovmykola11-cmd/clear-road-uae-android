package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import java.util.Locale

/**
 * Shared Salik facts for presentation and honesty gates (Google toll + scan gate counts).
 * Does not affect route selection or scoring.
 */
internal object SalikFacts {

    fun presentationTollCount(route: RealRouteDebugData): Int {
        val scan = route.corridorScanText.lowercase(Locale.US)
        val tollRoadCount = SalikDetection.countOccurrences(scan, "toll road")
        if (tollRoadCount > 0) return tollRoadCount
        if (route.tollAED > 0) {
            return (route.tollAED + SalikDetection.AED_PER_TOLL_ROAD - 1) /
                SalikDetection.AED_PER_TOLL_ROAD
        }
        return 0
    }

    fun salikDiffers(
        left: RealRouteDebugData,
        right: RealRouteDebugData,
    ): Boolean {
        if (left.tollAED != right.tollAED) return true
        return presentationTollCount(left) != presentationTollCount(right)
    }

    fun allTollZero(routes: List<RealRouteDebugData>): Boolean =
        routes.isNotEmpty() &&
            routes.all { route ->
                route.tollAED == 0 && presentationTollCount(route) == 0
            }
}
