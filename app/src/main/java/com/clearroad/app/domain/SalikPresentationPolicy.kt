package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Presentation-only Salik copy gates. Does not affect route selection or scoring.
 */
internal object SalikPresentationPolicy {

    fun allRoutesTollFree(routes: List<RealRouteDebugData>): Boolean =
        SalikFacts.allTollZero(routes)

    fun salikDiffersAcrossRoutes(routes: List<RealRouteDebugData>): Boolean {
        if (routes.size < 2) return false
        return routes.indices.any { left ->
            routes.indices.any { right ->
                left != right && SalikFacts.salikDiffers(routes[left], routes[right])
            }
        }
    }

    fun saveAedSummaryLine(routes: List<RealRouteDebugData>): String =
        if (allRoutesTollFree(routes)) {
            "All options avoid Salik."
        } else {
            "Lower Salik cost."
        }

    fun saveAedReasonChip(routes: List<RealRouteDebugData>): String =
        if (allRoutesTollFree(routes)) {
            "No Salik difference"
        } else {
            "Lower Salik cost"
        }
}
