package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Presentation-only SMOOTH copy gates. Does not affect route selection or scoring.
 */
internal object SmoothDrivePresentationPolicy {

    fun matchesFastestRoute(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Boolean {
        if (routes.isEmpty()) return false
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val fastestIndex =
            routes.indices.minWith(
                compareBy<Int> { routes[it].durationSeconds }.thenBy { it },
            )
        return recIdx == fastestIndex
    }

    fun summaryLine(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String =
        if (matchesFastestRoute(routes, recommendedIndex)) {
            "Smooth option matches fastest today."
        } else {
            "Less affected by traffic slowdowns."
        }

    fun narrativeLine(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String =
        if (matchesFastestRoute(routes, recommendedIndex)) {
            "Traffic conditions are similar across these routes."
        } else {
            "Picked for a steadier ETA."
        }
}
