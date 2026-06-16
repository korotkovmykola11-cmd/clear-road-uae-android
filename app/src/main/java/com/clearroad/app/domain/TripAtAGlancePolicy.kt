package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Details "Trip at a glance" summary lines — meaning from [ModeExplanationPolicy].
 * Presentation only; does not affect route selection or scoring.
 */
internal object TripAtAGlancePolicy {

    fun lines(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        routeIndex: Int,
    ): Pair<String, String?> {
        if (routes.isEmpty()) {
            return emptyLines(mode)
        }
        val idx = routeIndex.coerceIn(0, routes.lastIndex)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                mode = mode,
                recommended = routes[idx],
                routes = routes,
                recommendedIndex = idx,
            )
        return Pair(home.summary, glanceSecondary(home.narrative))
    }

    internal fun glanceSecondary(narrative: String): String? {
        val trimmed = narrative.trim()
        if (trimmed.isEmpty()) return null
        return trimmed
            .removeSuffix(".")
            .replace(" here", "")
            .trim()
            .let { if (it.isEmpty()) null else "$it." }
    }

    private fun emptyLines(mode: PreferenceMode): Pair<String, String?> =
        when (mode) {
            PreferenceMode.FASTEST ->
                Pair("Quickest on this list.", "No Salik on this route.")
            PreferenceMode.NO_TOLLS ->
                Pair("All options avoid Salik.", "Time is the main difference.")
            PreferenceMode.CALM ->
                Pair(
                    "Smooth option matches fastest today.",
                    "Traffic conditions are similar across these routes.",
                )
        }
}
