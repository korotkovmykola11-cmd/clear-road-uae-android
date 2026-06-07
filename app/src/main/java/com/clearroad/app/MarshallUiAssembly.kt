package com.clearroad.app

import com.clearroad.app.domain.DecisionNarrativeSynthesis
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.model.MarshallRecommendationBannerUiModel
import com.clearroad.app.ui.model.RecommendationSurfaceUiModel
import com.clearroad.app.ui.model.WhyTagUiModel

internal fun marshallSalikMetric(tollAed: Int): String =
    if (tollAed == 0) "0 Salik" else "Salik $tollAed AED"

internal fun buildRecommendationSurfaceUiModel(
    ready: Boolean,
    loading: Boolean,
    loadingMessage: String,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
    routeIdentity: String,
    mode: PreferenceMode,
    highConfidence: Boolean,
): RecommendationSurfaceUiModel {
    if (loading) {
        return RecommendationSurfaceUiModel(
            ready = false,
            loading = true,
            loadingMessage = loadingMessage,
            mode = mode,
        )
    }
    if (!ready || routes.isEmpty()) {
        return RecommendationSurfaceUiModel(ready = false, mode = mode)
    }
    val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
    val recommended = routes[recIdx]
    return RecommendationSurfaceUiModel(
        ready = true,
        routeName = routeIdentity,
        travelTime = recommended.durationText,
        mode = mode,
        decisionSummary = RecommendationSummaryLayer.forMode(mode),
        narrative = DecisionNarrativeSynthesis.narrative(
            mode = mode,
            recommendedTollAed = recommended.tollAED,
            highConfidence = highConfidence,
        ),
    )
}

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

internal fun decisionFirstReasonLine(
    whyTags: List<WhyTagUiModel>,
    recommendedNuance: String?,
    isRecommended: Boolean,
    personality: String,
): String {
    whyTags.firstOrNull()?.label?.takeIf { it.isNotBlank() }?.let { return it }
    if (isRecommended) {
        recommendedNuance?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return personality
}

internal fun decisionFirstSupportingWhyTags(
    whyTags: List<WhyTagUiModel>,
    reasonLine: String,
): List<WhyTagUiModel> =
    whyTags.filter { it.label != reasonLine }

internal fun tollOnlyFromSalikLine(salikLine: String): String =
    salikLine.substringBefore(" · ").ifBlank { salikLine }
