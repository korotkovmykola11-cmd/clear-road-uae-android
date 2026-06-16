package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteTrafficDelayMetrics

/**
 * Single source for Home recommendation card copy (reason chip, summary, narrative).
 * Presentation only — does not affect route selection or scoring.
 */
internal object ModeExplanationPolicy {

    data class HomeCardCopy(
        val reasonChip: String,
        val summary: String,
        val narrative: String,
    )

    fun homeCardCopy(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): HomeCardCopy =
        when (mode) {
            PreferenceMode.FASTEST -> fastestCopy(recommended, routes, recommendedIndex)
            PreferenceMode.NO_TOLLS -> saveAedCopy(recommended, routes, recommendedIndex)
            PreferenceMode.CALM -> smoothCopy(recommended, routes, recommendedIndex)
        }

    fun reasonChip(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String = homeCardCopy(mode, recommended, routes, recommendedIndex).reasonChip

    fun summary(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String {
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex.coerceAtLeast(0))
        val recommended = routes[recIdx]
        return homeCardCopy(mode, recommended, routes, recIdx).summary
    }

    fun narrative(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String {
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex.coerceAtLeast(0))
        val recommended = routes[recIdx]
        return homeCardCopy(mode, recommended, routes, recIdx).narrative
    }

    private fun fastestCopy(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): HomeCardCopy {
        val advantageMinutes = minutesAdvantageOverNext(recommended, routes, recommendedIndex)
        val reasonChip =
            if (advantageMinutes > 0) {
                "+$advantageMinutes min advantage"
            } else {
                ""
            }
        val narrative =
            if (recommended.tollAED > 0) {
                "Salik ${recommended.tollAED} AED on this route."
            } else {
                "No Salik on this route."
            }
        return HomeCardCopy(
            reasonChip = reasonChip,
            summary = "Quickest on this list.",
            narrative = narrative,
        )
    }

    private fun saveAedCopy(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): HomeCardCopy {
        if (SalikPresentationPolicy.allRoutesTollFree(routes)) {
            return HomeCardCopy(
                reasonChip = "No Salik difference",
                summary = "All options avoid Salik.",
                narrative = "Time is the main difference here.",
            )
        }
        val gates = SalikFacts.presentationTollCount(recommended)
        val reasonChip =
            if (gates > 0) {
                "$gates Salik gates"
            } else {
                "0 Salik gates"
            }
        val narrative =
            when {
                recommended.tollAED > 0 ->
                    "About ${recommended.tollAED} AED in Salik."
                othersHaveHigherGoogleToll(recommended, routes, recommendedIndex) ->
                    "No Salik on this route; other options may cost more."
                else ->
                    "Lower Salik cost than other options on this trip."
            }
        return HomeCardCopy(
            reasonChip = reasonChip,
            summary = "Lower Salik cost.",
            narrative = narrative,
        )
    }

    private fun smoothCopy(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): HomeCardCopy {
        if (SmoothDrivePresentationPolicy.matchesFastestRoute(routes, recommendedIndex)) {
            return HomeCardCopy(
                reasonChip = smoothReasonChip(recommended, routes),
                summary = "Smooth option matches fastest today.",
                narrative = "Traffic conditions are similar across these routes.",
            )
        }
        return HomeCardCopy(
            reasonChip = smoothReasonChip(recommended, routes),
            summary = "Less affected by traffic slowdowns.",
            narrative = "Picked for a steadier ETA.",
        )
    }

    private fun smoothReasonChip(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
    ): String {
        if (RouteTrafficDelayMetrics.isLowestDelayRatioAmong(recommended, routes)) {
            return "Least traffic slowdown"
        }
        val delayMinutes = RouteTrafficDelayMetrics.delayMinutesRounded(recommended)
        if (delayMinutes > 0) {
            return "+$delayMinutes min traffic slowdown"
        }
        return "Similar ETA to alternatives"
    }

    private fun othersHaveHigherGoogleToll(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Boolean =
        routes.indices.any { index ->
            index != recommendedIndex && routes[index].tollAED > recommended.tollAED
        }

    private fun minutesAdvantageOverNext(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Int {
        val nextAlternativeSeconds =
            routes.indices
                .filter { it != recommendedIndex }
                .minOfOrNull { routes[it].durationSeconds }
                ?: return 0
        return minutesAdvantage(recommended.durationSeconds, nextAlternativeSeconds)
    }

    private fun minutesAdvantage(fasterSeconds: Int, slowerSeconds: Int): Int =
        ((slowerSeconds - fasterSeconds).coerceAtLeast(0) + 59) / 60
}
