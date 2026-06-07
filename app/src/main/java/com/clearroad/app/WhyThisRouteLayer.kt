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
                    recommendedSeconds = recommended.durationSeconds,
                    fastestSeconds = fastestSeconds,
                )
        }
    }

    private fun loadingCopy(mode: PreferenceMode): RouteWhyCopy =
        when (mode) {
            PreferenceMode.FASTEST ->
                RouteWhyCopy(
                    title = "Fastest route",
                    why = "MARSHIO will explain this choice once routes finish loading.",
                )
            PreferenceMode.NO_TOLLS ->
                RouteWhyCopy(
                    title = "Lower Salik route",
                    why = "MARSHIO will explain Salik exposure once routes finish loading.",
                )
            PreferenceMode.CALM ->
                RouteWhyCopy(
                    title = "Smoother drive",
                    why = "MARSHIO will explain this pacing choice once routes finish loading.",
                )
        }

    private fun fastestCopy(
        recommendedSeconds: Int,
        nextAlternativeSeconds: Int?,
    ): RouteWhyCopy {
        val title = "Fastest available route"
        val whyBody = buildString {
            append("MARSHIO chose this route to get you there sooner.")
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
                    append(" It is the quickest option available for this trip.")
            }
            append(" Traffic on this corridor looks predictable.")
        }
        return RouteWhyCopy(title = title, why = whyBody)
    }

    private fun saveAedCopy(
        recommended: RealRouteDebugData,
        recommendedIndex: Int,
        routes: List<RealRouteDebugData>,
        fastestSeconds: Int,
    ): RouteWhyCopy {
        val title = "Lower Salik exposure"
        val whyBody = buildString {
            append("MARSHIO prefers less Salik risk on this trip.")
            if (recommended.tollAED > 0) {
                append(" Google estimates Salik at ${recommended.tollAED} AED on this route.")
            } else {
                val othersHaveHigherToll =
                    routes.indices.any { index ->
                        index != recommendedIndex && routes[index].tollAED > recommended.tollAED
                    }
                if (othersHaveHigherToll) {
                    append(" This route keeps Salik lower than faster alternatives on this list.")
                } else {
                    append(" This route keeps Salik exposure as low as possible among these options.")
                }
            }
            append(extraMinutesVersusFastest(recommended.durationSeconds, fastestSeconds))
        }
        return RouteWhyCopy(title = title, why = whyBody)
    }

    private fun smoothDriveCopy(
        recommendedSeconds: Int,
        fastestSeconds: Int,
    ): RouteWhyCopy {
        val title = "Smoother drive"
        val whyBody = buildString {
            append("MARSHIO chose a calmer route with less stop-start stress.")
            append(extraMinutesVersusFastest(recommendedSeconds, fastestSeconds))
            append(" Pacing on this corridor should feel steadier.")
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
