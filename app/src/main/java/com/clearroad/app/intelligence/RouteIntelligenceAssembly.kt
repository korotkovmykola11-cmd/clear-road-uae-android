package com.clearroad.app.intelligence

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.ui.model.GoogleDirectionsStepInputUiModel
import com.clearroad.app.ui.model.RouteIntelligenceComparisonRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRouteInputUiModel

/** Builds read-only Route Intelligence inputs — does not affect route selection. */
internal object RouteIntelligenceAssembly {

    fun buildRequest(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        isRecommendedRouteDetails: Boolean,
    ): RouteIntelligenceRequestUiModel? {
        if (!isRecommendedRouteDetails || routes.isEmpty()) return null
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val marshioRoute = routes[recIdx]
        if (marshioRoute.routePathPoints.size < 2) return null

        val alternativeIndex = pickAlternativeIndex(routes, recIdx) ?: return null
        val alternativeRoute = routes[alternativeIndex]
        if (alternativeRoute.routePathPoints.size < 2) return null

        return RouteIntelligenceRequestUiModel(
            marshioRoute =
                toInput(
                    route = marshioRoute,
                    routeIndex = recIdx,
                    identities = identities,
                    routes = routes,
                    recommendedIndex = recIdx,
                ),
            alternativeRoute =
                toInput(
                    route = alternativeRoute,
                    routeIndex = alternativeIndex,
                    identities = identities,
                    routes = routes,
                    recommendedIndex = recIdx,
                ),
        )
    }

    fun buildComparisonRequest(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        isRecommendedRouteDetails: Boolean,
    ): RouteIntelligenceComparisonRequestUiModel? {
        if (!isRecommendedRouteDetails || routes.isEmpty()) return null
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val inputs =
            routes.mapIndexedNotNull { index, route ->
                if (route.routePathPoints.size < 2) return@mapIndexedNotNull null
                toInput(
                    route = route,
                    routeIndex = index,
                    identities = identities,
                    routes = routes,
                    recommendedIndex = recIdx,
                )
            }
        if (inputs.isEmpty()) return null
        return RouteIntelligenceComparisonRequestUiModel(
            routes = inputs,
            marshioSelectedRouteIndex = recIdx,
            googleDefaultRouteIndex = 0,
        )
    }

    private fun pickAlternativeIndex(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Int? {
        if (recommendedIndex != 0) return 0
        return routes.indices.firstOrNull { it != recommendedIndex }
    }

    private fun toInput(
        route: RealRouteDebugData,
        routeIndex: Int,
        identities: List<RouteIdentity>,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): RouteIntelligenceRouteInputUiModel {
        val title =
            RouteIdentityPresentationPolicy.displayForDetails(
                route = route,
                identity = identities.getOrNull(routeIndex),
                routes = routes,
                identities = identities,
                recommendedIndex = recommendedIndex,
            ).detailsTitle.ifBlank { "Route ${routeIndex + 1}" }
        return RouteIntelligenceRouteInputUiModel(
            routeIndex = routeIndex,
            routeName = title,
            routePathPoints = route.routePathPoints,
            durationSeconds = route.durationSeconds,
            distanceMeters = route.distanceMeters,
            googleSteps =
                route.googleSteps.map { step ->
                    GoogleDirectionsStepInputUiModel(
                        distanceMeters = step.distanceMeters,
                        maneuver = step.maneuver,
                    )
                },
            criticalManeuversCount = route.criticalManeuversCount,
        )
    }

    fun toRawRoute(input: RouteIntelligenceRouteInputUiModel): RawRouteForIntelligence =
        RawRouteForIntelligence(
            routeIndex = input.routeIndex,
            routeName = input.routeName,
            routePathPoints = input.routePathPoints,
            durationSeconds = input.durationSeconds,
            distanceMeters = input.distanceMeters,
            googleSteps =
                input.googleSteps.map { step ->
                    GoogleDirectionsStepInput(
                        distanceMeters = step.distanceMeters,
                        maneuver = step.maneuver,
                    )
                },
            criticalManeuversCount = input.criticalManeuversCount,
        )

    fun toRawRoutes(request: RouteIntelligenceComparisonRequestUiModel): List<RawRouteForIntelligence> =
        request.routes.map(::toRawRoute)
}
