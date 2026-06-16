package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Details confidence callout — level from [RouteConfidence]; route meaning from
 * [ModeExplanationPolicy] where the confidence block paraphrases Home copy.
 * Presentation only; does not affect route selection or scoring.
 */
internal object ConfidenceExplanationPolicy {

    data class ConfidenceCopy(
        val title: String,
        val body: String,
        val isHighConfidence: Boolean,
    )

    fun forMode(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ConfidenceCopy {
        if (routes.isEmpty()) {
            return emptyRoutesCopy(mode)
        }
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        return when (mode) {
            PreferenceMode.FASTEST -> fastestConfidence(routes, recIdx)
            PreferenceMode.NO_TOLLS -> saveAedConfidence(routes, recIdx)
            PreferenceMode.CALM -> smoothConfidence(routes, recIdx)
        }
    }

    private fun fastestConfidence(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ConfidenceCopy {
        val confidence = RouteConfidence.fromAdvantageOverNext(routes, recommendedIndex)
        return when (confidence.level) {
            RouteConfidence.Level.HIGH ->
                ConfidenceCopy(
                    title = "High confidence",
                    body = "This route is clearly faster than the available alternatives.",
                    isHighConfidence = true,
                )
            RouteConfidence.Level.MEDIUM ->
                ConfidenceCopy(
                    title = "Medium confidence",
                    body = "This route is somewhat faster, but the alternatives are still close.",
                    isHighConfidence = false,
                )
            RouteConfidence.Level.LOW ->
                ConfidenceCopy(
                    title = "Similar options",
                    body = "Times are very close among these routes.",
                    isHighConfidence = false,
                )
        }
    }

    private fun saveAedConfidence(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ConfidenceCopy {
        if (SalikPresentationPolicy.allRoutesTollFree(routes)) {
            val home =
                ModeExplanationPolicy.homeCardCopy(
                    mode = PreferenceMode.NO_TOLLS,
                    recommended = routes[recommendedIndex],
                    routes = routes,
                    recommendedIndex = recommendedIndex,
                )
            return ConfidenceCopy(
                title = "Similar options",
                body = bodyFromSaveAllTollFreeMeaning(home),
                isHighConfidence = false,
            )
        }
        return ConfidenceCopy(
            title = "Medium confidence",
            body = "This route keeps Salik cost down, but may require a few extra minutes.",
            isHighConfidence = false,
        )
    }

    private fun smoothConfidence(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ConfidenceCopy {
        if (SmoothDrivePresentationPolicy.matchesFastestRoute(routes, recommendedIndex)) {
            val home =
                ModeExplanationPolicy.homeCardCopy(
                    mode = PreferenceMode.CALM,
                    recommended = routes[recommendedIndex],
                    routes = routes,
                    recommendedIndex = recommendedIndex,
                )
            return ConfidenceCopy(
                title = "Similar options",
                body = bodyFromSmoothMatchesFastestMeaning(home),
                isHighConfidence = false,
            )
        }
        return ConfidenceCopy(
            title = "Medium confidence",
            body = "Less affected by traffic slowdowns, even if not the fastest route.",
            isHighConfidence = false,
        )
    }

    /** Re-phrases MEP all-toll-free meaning for the confidence callout. */
    internal fun bodyFromSaveAllTollFreeMeaning(
        home: ModeExplanationPolicy.HomeCardCopy,
    ): String {
        val timeDifference =
            home.narrative
                .removeSuffix(".")
                .replace(" here", "")
                .trim()
        return "All routes avoid Salik on this trip. $timeDifference."
    }

    /** Re-phrases MEP smooth=fastest meaning for the confidence callout. */
    internal fun bodyFromSmoothMatchesFastestMeaning(
        home: ModeExplanationPolicy.HomeCardCopy,
    ): String {
        val summaryPart =
            if (home.summary.contains("matches fastest")) {
                "Smooth option matches the fastest route today."
            } else {
                home.summary.trim().trimEnd('.') + "."
            }
        val trafficPart =
            home.narrative
                .removeSuffix(".")
                .replace(" across these routes", "")
                .trim()
        return "$summaryPart $trafficPart."
    }

    private fun emptyRoutesCopy(mode: PreferenceMode): ConfidenceCopy =
        when (mode) {
            PreferenceMode.FASTEST ->
                ConfidenceCopy(
                    title = "Similar options",
                    body = "Times are very close among these routes.",
                    isHighConfidence = false,
                )
            PreferenceMode.NO_TOLLS ->
                ConfidenceCopy(
                    title = "Similar options",
                    body = "All routes avoid Salik on this trip.",
                    isHighConfidence = false,
                )
            PreferenceMode.CALM ->
                ConfidenceCopy(
                    title = "Similar options",
                    body = "Smooth option matches the fastest route today.",
                    isHighConfidence = false,
                )
        }
}
