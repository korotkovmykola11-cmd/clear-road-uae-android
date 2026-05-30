package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.model.ChoiceWhyTipUiModel
import com.clearroad.app.ui.model.RouteCardUiModel
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.google.android.gms.maps.model.LatLng

internal fun salikMetaText(tollAed: Int, personality: String): String =
    if (tollAed > 0) "Salik $tollAed AED · $personality"
    else "No Salik · $personality"

internal fun buildRouteCardUiModel(
    routeIndex: Int,
    item: RealRouteDebugData,
    selectedMode: PreferenceMode,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
    routeCardSelectionIndex: Int,
    userExplicitRouteSelection: Boolean,
    salikLine: String,
    confidence: String,
    recommendedNuance: String?,
): RouteCardUiModel {
    val isUserSelected = routeIndex == routeCardSelectionIndex
    val showUserSelectedChrome = isUserSelected && userExplicitRouteSelection
    val isRecommended = routeIndex == recommendedRouteIndex
    return RouteCardUiModel(
        routeIndex = routeIndex,
        routeTitle = "Route ${routeIndex + 1}",
        durationText = item.durationText,
        distanceText = item.distanceText,
        salikLine = salikLine,
        confidence = confidence,
        isRecommended = isRecommended,
        isUserSelected = isUserSelected,
        showUserSelectedChrome = showUserSelectedChrome,
        recommendedNuance = if (isRecommended) recommendedNuance else null,
        durationOnSurfaceAlpha = when {
            isRecommended -> 0.97f
            isUserSelected -> 0.90f
            else -> 0.94f
        },
        whyTags = whyTagsForRoute(item, routeIndex, selectedMode, routes),
        mode = selectedMode,
    )
}

internal fun buildRouteDetailsUiModel(
    routeIndex: Int,
    routeNumber: Int,
    routeReasonTitle: String,
    routeReasonWhy: String,
    item: RealRouteDebugData,
    selectedMode: PreferenceMode,
    routes: List<RealRouteDebugData>,
    confidenceLabel: String,
    costSummaryPrimary: String,
    costSummarySecondary: String?,
    decisionSnapshotRecommendedHeading: String,
    decisionSnapshotRecommendedSummary: String,
    decisionSnapshotOthersHeading: String,
    decisionSnapshotOthersSummary: String,
    recommendationConfidenceText: String,
    isHighConfidence: Boolean,
    fromLatLng: LatLng?,
    toLatLng: LatLng?,
): RouteDetailsUiModel =
    RouteDetailsUiModel(
        routeNumber = routeNumber,
        routeReasonTitle = routeReasonTitle,
        routeReasonWhy = routeReasonWhy,
        durationText = item.durationText,
        distanceText = item.distanceText,
        tollAed = item.tollAED,
        confidenceLabel = confidenceLabel,
        costSummaryPrimary = costSummaryPrimary,
        costSummarySecondary = costSummarySecondary,
        decisionSnapshotRecommendedHeading = decisionSnapshotRecommendedHeading,
        decisionSnapshotRecommendedSummary = decisionSnapshotRecommendedSummary,
        decisionSnapshotOthersHeading = decisionSnapshotOthersHeading,
        decisionSnapshotOthersSummary = decisionSnapshotOthersSummary,
        recommendationConfidenceText = recommendationConfidenceText,
        isHighConfidence = isHighConfidence,
        whyTags = whyTagsForRoute(item, routeIndex, selectedMode, routes),
        mode = selectedMode,
        fromLatLng = fromLatLng,
        toLatLng = toLatLng,
    )

internal fun choiceWhyTipUiModel(
    choice: String,
    why: String,
    tip: String,
    compact: Boolean,
): ChoiceWhyTipUiModel =
    ChoiceWhyTipUiModel(
        choice = choice,
        why = why,
        tip = tip,
        compact = compact,
    )
