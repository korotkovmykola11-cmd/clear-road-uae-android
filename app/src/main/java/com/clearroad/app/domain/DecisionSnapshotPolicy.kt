package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Details decision snapshot copy — recommended line from [ModeExplanationPolicy];
 * others line from shared trip facts (narrative, [RouteConfidence], Salik/Smooth gates).
 * Policy + tests only until a product brief adds a Details UI block.
 * Presentation only; does not affect route selection or scoring.
 */
internal object DecisionSnapshotPolicy {

    data class Lines(
        val recommendedHeading: String,
        val recommendedSummary: String,
        val othersHeading: String,
        val othersSummary: String,
    )

    fun lines(
        mode: PreferenceMode,
        recommendedRouteIndex: Int,
        routes: List<RealRouteDebugData>,
    ): Lines {
        if (routes.isEmpty()) {
            return emptyLines(mode, recommendedRouteIndex)
        }
        val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                mode = mode,
                recommended = routes[recIdx],
                routes = routes,
                recommendedIndex = recIdx,
            )
        return Lines(
            recommendedHeading = recommendedHeading(mode, recIdx),
            recommendedSummary = home.summary,
            othersHeading = OTHERS_HEADING,
            othersSummary = othersSummary(mode, home, routes, recIdx),
        )
    }

    private const val OTHERS_HEADING = "Other routes"

    private fun recommendedHeading(
        mode: PreferenceMode,
        recommendedRouteIndex: Int,
    ): String =
        when (mode) {
            PreferenceMode.FASTEST -> "Route ${recommendedRouteIndex + 1} (recommended)"
            PreferenceMode.NO_TOLLS,
            PreferenceMode.CALM,
            -> "Recommended route"
        }

    private fun othersSummary(
        mode: PreferenceMode,
        home: ModeExplanationPolicy.HomeCardCopy,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String =
        when (mode) {
            PreferenceMode.FASTEST ->
                when (RouteConfidence.fromAdvantageOverNext(routes, recommendedIndex).level) {
                    RouteConfidence.Level.LOW ->
                        "Times are very close among these routes."
                    else ->
                        "Slightly slower without a clear gain."
                }
            PreferenceMode.NO_TOLLS ->
                if (SalikPresentationPolicy.allRoutesTollFree(routes)) {
                    home.narrative
                } else {
                    "Higher toll spending than recommended."
                }
            PreferenceMode.CALM ->
                if (SmoothDrivePresentationPolicy.matchesFastestRoute(routes, recommendedIndex)) {
                    home.narrative
                } else {
                    "More affected by traffic slowdowns than the recommended route."
                }
        }

    private fun emptyLines(
        mode: PreferenceMode,
        recommendedRouteIndex: Int,
    ): Lines =
        Lines(
            recommendedHeading = recommendedHeading(mode, recommendedRouteIndex),
            recommendedSummary = "",
            othersHeading = OTHERS_HEADING,
            othersSummary = "",
        )
}
