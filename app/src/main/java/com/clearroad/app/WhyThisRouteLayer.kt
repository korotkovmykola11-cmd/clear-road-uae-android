package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.2 — mode-aware copy for the Route Details "Why this route" block.
 * Wording only; uses existing route metrics. No scoring or Salik heuristic.
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
        val fastestSeconds = routes.minOf { it.durationSeconds }
        val nextAlternativeSeconds =
            routes.indices
                .filter { it != recommendedIndex }
                .minOfOrNull { routes[it].durationSeconds }
        return when (mode) {
            PreferenceMode.FASTEST ->
                fastestCopy(
                    recommendedSeconds = recommended.durationSeconds,
                    nextAlternativeSeconds = nextAlternativeSeconds,
                )
            PreferenceMode.NO_TOLLS ->
                saveAedCopy(
                    recommended = recommended,
                    recommendedIndex = recommendedIndex,
                    routes = routes,
                    fastestSeconds = fastestSeconds,
                )
            PreferenceMode.CALM ->
                smoothDriveCopy(
                    recommended = recommended,
                    routes = routes,
                    recommendedSeconds = recommended.durationSeconds,
                    fastestSeconds = fastestSeconds,
                )
        }
    }

    private fun loadingCopy(mode: PreferenceMode): RouteWhyCopy =
        when (mode) {
            PreferenceMode.FASTEST ->
                RouteWhyCopy(
                    title = "Time-focused route",
                    why = "MARSHIO will explain this choice once routes finish loading.",
                )
            PreferenceMode.NO_TOLLS ->
                RouteWhyCopy(
                    title = "Lower Salik route",
                    why = "MARSHIO will explain Salik exposure once routes finish loading.",
                )
            PreferenceMode.CALM ->
                RouteWhyCopy(
                    title = "Lower traffic delay load",
                    why = "MARSHIO will explain this traffic-timing choice once routes finish loading.",
                )
        }

    private fun fastestCopy(
        recommendedSeconds: Int,
        nextAlternativeSeconds: Int?,
    ): RouteWhyCopy {
        val title = "Best time-focused pick"
        val whyBody = buildString {
            append("MARSHIO chose the quickest practical option on this list.")
            val savedMinutes =
                nextAlternativeSeconds?.let {
                    minutesSaved(recommendedSeconds, it)
                }
            when {
                savedMinutes != null && savedMinutes >= 2 ->
                    append(" It saves about $savedMinutes minutes compared with the next option.")
                savedMinutes == 1 ->
                    append(" It saves about a minute compared with the next option.")
                else ->
                    append(" It is the best time-focused pick among these routes.")
            }
        }
        return RouteWhyCopy(title = title, why = whyBody)
    }

    private fun saveAedCopy(
        recommended: RealRouteDebugData,
        recommendedIndex: Int,
        routes: List<RealRouteDebugData>,
        fastestSeconds: Int,
    ): RouteWhyCopy {
        val title = "Lower Salik impact"
        val whyBody = buildString {
            append("MARSHIO picked a route with lower Salik impact on this trip.")
            if (recommended.tollAED > 0) {
                append(" Google estimates Salik at ${recommended.tollAED} AED on this route.")
            } else {
                val othersHaveHigherToll =
                    routes.indices.any { index ->
                        index != recommendedIndex && routes[index].tollAED > recommended.tollAED
                    }
                if (othersHaveHigherToll) {
                    append(" This route reduces Salik exposure versus faster alternatives here.")
                } else {
                    append(" This route keeps Salik exposure as low as possible among these options.")
                }
            }
            append(extraMinutesVersusFastest(recommended.durationSeconds, fastestSeconds))
        }
        return RouteWhyCopy(title = title, why = whyBody)
    }

    private fun smoothDriveCopy(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedSeconds: Int,
        fastestSeconds: Int,
    ): RouteWhyCopy {
        val title = "Lower traffic delay load"
        val delayMinutes = RouteTrafficDelayMetrics.delayMinutesRounded(recommended)
        val delayPercent = RouteTrafficDelayMetrics.delayRatioPercentRounded(recommended)
        val whyBody = buildString {
            append(
                "MARSHIO chose this route for a lower traffic delay load among these options.",
            )
            if (RouteTrafficDelayMetrics.isLowestDelayRatioAmong(recommended, routes)) {
                append(" It adds the least extra time from traffic on this list.")
            } else if (delayMinutes > 0) {
                append(" Traffic adds about $delayMinutes min ($delayPercent%) to the base ETA.")
            } else {
                append(" Traffic adds little extra time to the base ETA right now.")
            }
            append(extraMinutesVersusFastest(recommendedSeconds, fastestSeconds))
            if (recommendedSeconds > fastestSeconds) {
                append(" It may not be the quickest option on the list.")
            }
            append(" This choice is based on traffic delay, not Salik, fuel, or total trip cost.")
        }
        return RouteWhyCopy(title = title, why = whyBody)
    }

    private fun extraMinutesVersusFastest(
        recommendedSeconds: Int,
        fastestSeconds: Int,
    ): String {
        if (fastestSeconds <= 0 || recommendedSeconds <= fastestSeconds) return ""
        val extraMinutes = minutesSaved(fastestSeconds, recommendedSeconds)
        return when {
            extraMinutes >= 2 ->
                " It may take about $extraMinutes extra minutes versus the fastest route."
            extraMinutes == 1 ->
                " It may take about a minute extra versus the fastest route."
            else -> ""
        }
    }

    private fun minutesSaved(fasterSeconds: Int, slowerSeconds: Int): Int =
        ((slowerSeconds - fasterSeconds).coerceAtLeast(0) + 59) / 60
}
