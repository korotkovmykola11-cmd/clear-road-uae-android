package com.clearroad.app.legacy

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.tollPhraseForCard
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteConfidence
import com.clearroad.app.domain.RouteOption
import com.clearroad.app.domain.SalikPresentationPolicy
import com.clearroad.app.domain.SmoothDrivePresentationPolicy

/**
 * Rollback-only Home copy helpers — used when [ArchitectureValidation.RECOMMENDATION_ONLY_HOME]
 * is false. Prod path uses [ModeExplanationPolicy] tree instead.
 */
internal object LegacyHomePresentation {

    fun routesForDecision(
        routeList: List<RealRouteDebugData>,
        singleRoute: RealRouteDebugData?,
    ): List<RouteOption> =
        when {
            routeList.isNotEmpty() ->
                routeList.mapIndexed { index, item ->
                    RouteOption(
                        id = "real_route_$index",
                        name = "Real route ${index + 1}",
                        durationMin = item.durationSeconds / 60,
                        distanceKm = item.distanceMeters / 1000.0,
                        tollAed = item.tollAED.toDouble(),
                        salikGates = if (item.hasToll) 1 else 0,
                        passesAbuDhabi = false,
                        parkingMayBePaid = false,
                    )
                }
            singleRoute != null ->
                listOf(
                    RouteOption(
                        id = "real_route",
                        name = "Real route",
                        durationMin = singleRoute.durationSeconds / 60,
                        distanceKm = singleRoute.distanceMeters / 1000.0,
                        tollAed = singleRoute.tollAED.toDouble(),
                        salikGates = if (singleRoute.hasToll) 1 else 0,
                        passesAbuDhabi = false,
                        parkingMayBePaid = false,
                    ),
                )
            else -> RouteDecisionEngine.sampleRoutes
        }

    fun selectedDecisionWhy(
        showRouteCardOverrides: Boolean,
        routes: List<RealRouteDebugData>,
        recommendedRouteIndex: Int,
        mode: PreferenceMode,
        decisionWhy: String?,
    ): String =
        when {
            showRouteCardOverrides -> {
                val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
                val recommendationTollAed = routes[recIdx].tollAED
                val personalityLine =
                    tollPhraseForCard(
                        routes[recIdx],
                        recIdx,
                        mode,
                        recommendedRouteIndex,
                        routes,
                    )
                val alignedCopy = recommendationAlignedExplanation(personalityLine, mode)
                val similarOutcomeGuidance =
                    if (routes.size >= 2) {
                        SimilarOutcomeDetection.detect(
                            routes =
                                routes.map { item ->
                                    SimilarOutcomeDetection.SimilarOutcomeRouteInput(
                                        durationSeconds = item.durationSeconds,
                                        tollAed = item.tollAED,
                                    )
                                },
                            recommendedIndex = recIdx,
                            mode = mode,
                        )
                    } else {
                        null
                    }
                similarOutcomeGuidance?.why
                    ?: alignedCopy?.second
                    ?: manualRouteWhy(mode, recIdx, recommendationTollAed)
            }
            else ->
                decisionWhy
                    ?: "Add starting point and destination to get a recommendation."
        }

    fun recommendationAlignedExplanation(
        personality: String,
        mode: PreferenceMode,
    ): Triple<String, String, String>? =
        RouteReasoning.alignedExplanation(personality, mode)?.let {
            Triple(it.choice, it.why, it.tip)
        }

    fun confidenceHintBelowRecommendation(
        mode: PreferenceMode,
        directionsStatus: String?,
        recommendedRouteIndex: Int,
        routes: List<RealRouteDebugData>,
    ): String =
        recommendationExplanationText(
            mode = mode,
            directionsStatus = directionsStatus,
            routes = routes,
            recommendedRouteIndex = recommendedRouteIndex,
            compact = true,
        )

    private fun manualRouteWhy(
        mode: PreferenceMode,
        routeIndex: Int,
        tollAED: Int?,
    ): String =
        RouteReasoning.manualWhy(
            RouteReasoningContext(
                mode = mode,
                routeIndex = routeIndex,
                tollAed = tollAED,
            ),
        )

    private fun recommendationExplanationText(
        mode: PreferenceMode,
        directionsStatus: String?,
        routes: List<RealRouteDebugData>,
        recommendedRouteIndex: Int,
        compact: Boolean,
    ): String {
        if (directionsStatus != "OK" || routes.isEmpty()) {
            return when (mode) {
                PreferenceMode.FASTEST -> "Timing unclear until routes load."
                PreferenceMode.NO_TOLLS -> "Salik cost unclear until routes load."
                PreferenceMode.CALM -> "Pace unclear until routes load."
            }
        }
        val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
        val recommended = routes[recIdx]
        val highConfidence = isHighConfidenceRecommendation(mode, routes, recIdx)
        val nextAlternativeDurationSeconds =
            routes.indices
                .filter { it != recIdx }
                .minOfOrNull { routes[it].durationSeconds }
        val fastestDurationSeconds = routes.minOf { it.durationSeconds }
        return RouteReasoning.humanRecommendationExplanation(
            mode = mode,
            recommendedDurationSeconds = recommended.durationSeconds,
            recommendedTollAed = recommended.tollAED,
            nextAlternativeDurationSeconds = nextAlternativeDurationSeconds,
            fastestDurationSeconds = fastestDurationSeconds,
            highConfidence = highConfidence,
            compact = compact,
            allRoutesTollFree = SalikPresentationPolicy.allRoutesTollFree(routes),
            smoothMatchesFastest =
                SmoothDrivePresentationPolicy.matchesFastestRoute(routes, recIdx),
        )
    }

    private fun isHighConfidenceRecommendation(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedRouteIndex: Int,
    ): Boolean {
        if (routes.isEmpty()) return false
        val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
        return when (mode) {
            PreferenceMode.FASTEST ->
                RouteConfidence.fromAdvantageOverNext(routes, recIdx).isHighConfidence
            PreferenceMode.NO_TOLLS -> {
                val recommendedToll = routes[recIdx].tollAED
                routes.indices
                    .filter { it != recIdx }
                    .all { routes[it].tollAED > recommendedToll }
            }
            PreferenceMode.CALM -> false
        }
    }
}
