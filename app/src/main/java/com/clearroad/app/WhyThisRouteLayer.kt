package com.clearroad.app

import com.clearroad.app.domain.DetailsExplanationPolicy
import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.2 — Route Details "Why this route" block.
 * Delegates to [DetailsExplanationPolicy] → [ModeExplanationPolicy].
 */
internal object WhyThisRouteLayer {

    data class RouteWhyCopy(
        val title: String,
        val why: String,
    )

    fun recommendedRouteCopy(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        directionsStatus: String?,
    ): RouteWhyCopy {
        if (directionsStatus != "OK" || routes.isEmpty()) {
            return loadingCopy(mode)
        }
        val copy =
            DetailsExplanationPolicy.recommendedRouteCopy(
                mode = mode,
                recommended = recommended,
                routes = routes,
                recommendedIndex = recommendedIndex,
            )
        return RouteWhyCopy(title = copy.title, why = copy.why)
    }

    private fun loadingCopy(mode: PreferenceMode): RouteWhyCopy =
        when (mode) {
            PreferenceMode.FASTEST ->
                RouteWhyCopy(
                    title = "Quickest option",
                    why = "MARSHIO will explain this choice once routes finish loading.",
                )
            PreferenceMode.NO_TOLLS ->
                RouteWhyCopy(
                    title = "Lower Salik route",
                    why = "MARSHIO will explain Salik cost once routes finish loading.",
                )
            PreferenceMode.CALM ->
                RouteWhyCopy(
                    title = "SMOOTH DRIVE pick",
                    why = "MARSHIO will explain this choice once routes finish loading.",
                )
        }
}
