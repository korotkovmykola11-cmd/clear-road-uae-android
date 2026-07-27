package com.clearroad.app

import com.clearroad.app.domain.ComparativeEvidence
import com.clearroad.app.domain.EquivalentTripHonesty
import com.clearroad.app.domain.ModeExplanationPolicy
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentity
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
    routeIdentities: List<RouteIdentity>,
    mode: PreferenceMode,
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
    val honesty =
        EquivalentTripHonesty.evaluate(
            routes = routes,
            identities = routeIdentities,
            recommendedIndex = recIdx,
        )
    val comparativeEvidence =
        ComparativeEvidence.build(
            routes = routes,
            recommendedIndex = recIdx,
            mode = mode,
            isEquivalentTrip = honesty.isEquivalentTrip,
        )
    val homeCopy =
        if (honesty.isEquivalentTrip) {
            null
        } else {
            ModeExplanationPolicy.homeCardCopy(
                mode = mode,
                recommended = recommended,
                routes = routes,
                recommendedIndex = recIdx,
            )
        }
    return RecommendationSurfaceUiModel(
        ready = true,
        routeName = routeIdentity,
        travelTime =
            recommended.durationText.takeIf { it.isNotBlank() }
                ?: RecommendationTravelTimeFormatter.format(recommended.durationSeconds),
        mode = mode,
        decisionLabel = DecisionLabelLayer.label(),
        recommendationBadge = RecommendationBadgeLayer.forMode(mode),
        recommendationReason =
            if (honesty.isEquivalentTrip) {
                honesty.chipText
            } else {
                homeCopy!!.reasonChip
            },
        comparativeEvidenceLines = comparativeEvidence.lines,
        decisionSummary = homeCopy?.summary.orEmpty(),
        narrative =
            if (honesty.isEquivalentTrip) {
                honesty.narrativeText
            } else {
                homeCopy!!.narrative
            },
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
