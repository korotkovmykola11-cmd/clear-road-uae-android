package com.clearroad.app.intelligence

import com.clearroad.app.ui.model.RouteIntelligenceRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRouteRowUiModel
import com.clearroad.app.ui.model.RouteIntelligenceUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object RouteIntelligencePresentation {

    const val UNAVAILABLE_MESSAGE = "Route Intelligence unavailable."

    suspend fun load(
        request: RouteIntelligenceRequestUiModel,
        service: RouteIntelligenceService = RouteIntelligenceService.default(),
    ): RouteIntelligenceUiModel =
        coroutineScope {
            val marshioDeferred =
                async {
                    service.profileFor(RouteIntelligenceAssembly.toRawRoute(request.marshioRoute))
                }
            val alternativeDeferred =
                async {
                    request.alternativeRoute?.let { alternative ->
                        service.profileFor(RouteIntelligenceAssembly.toRawRoute(alternative))
                    }
                }
            val marshioProfile = marshioDeferred.await()
            val alternativeProfile = alternativeDeferred.await()

            if (
                marshioProfile.osmSourceStatus == SourceStatus.UNAVAILABLE &&
                    (alternativeProfile == null || alternativeProfile.osmSourceStatus == SourceStatus.UNAVAILABLE)
            ) {
                return@coroutineScope RouteIntelligenceUiModel(
                    loading = false,
                    available = false,
                    unavailableMessage = UNAVAILABLE_MESSAGE,
                )
            }

            RouteIntelligenceUiModel(
                loading = false,
                available = true,
                marshioRoute = rowUiModel("MARSHIO route", request.marshioRoute.routeName, marshioProfile),
                alternativeRoute =
                    request.alternativeRoute?.let { alternative ->
                        alternativeProfile?.let { profile ->
                            rowUiModel("Alternative", alternative.routeName, profile)
                        }
                    },
            )
        }

    fun rowUiModel(
        label: String,
        routeName: String,
        profile: RouteProfile,
    ): RouteIntelligenceRouteRowUiModel? {
        if (profile.osmSourceStatus != SourceStatus.OK) return null
        return RouteIntelligenceRouteRowUiModel(
            label = label,
            routeName = routeName,
            trafficSignalsLine = trafficSignalsLine(profile.trafficSignalsCount),
            roundaboutsLine = roundaboutsLine(profile.roundaboutsCount),
            mainRoadLine = mainRoadLine(profile.mainRoadRatio),
            complexityLine = ComplexityScoreCalculator.complexityLabel(profile.complexityScore),
        )
    }

    fun trafficSignalsLine(count: Int?): String =
        when (count) {
            null -> "Traffic signals: unavailable"
            0 -> "No traffic signals detected"
            1 -> "1 traffic signal"
            else -> "$count traffic signals"
        }

    fun roundaboutsLine(count: Int?): String =
        when (count) {
            null -> "Roundabouts: unavailable"
            0 -> "No roundabouts detected"
            1 -> "1 roundabout"
            else -> "$count roundabouts"
        }

    fun mainRoadLine(ratio: Float?): String =
        when {
            ratio == null -> "Road class: unavailable"
            ratio >= 0.65f -> "Mostly main roads"
            ratio >= 0.35f -> "Mix of main and local streets"
            else -> "More local streets"
        }
}
