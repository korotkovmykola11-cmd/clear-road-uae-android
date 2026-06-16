package com.clearroad.app.legacy

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.marshallSalikMetric
import com.clearroad.app.ui.model.MarshallRecommendationBannerUiModel

internal fun buildMarshallRecommendationBannerUiModel(
    ready: Boolean,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
    routeIdentity: String,
    whyText: String,
): MarshallRecommendationBannerUiModel {
    if (!ready || routes.isEmpty()) {
        return MarshallRecommendationBannerUiModel(
            ready = false,
            metricsLine = "",
            routeIdentity = "",
            whyLine = "",
        )
    }
    val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
    val recommended = routes[recIdx]
    return MarshallRecommendationBannerUiModel(
        ready = true,
        metricsLine = "${recommended.durationText} • ${marshallSalikMetric(recommended.tollAED)}",
        routeIdentity = routeIdentity,
        whyLine = whyText,
    )
}
