package com.clearroad.app

import com.clearroad.app.domain.ComparativeEvidence
import com.clearroad.app.domain.EquivalentTripHonesty
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.domain.TripAtAGlancePolicy
import com.clearroad.app.legacy.LegacyHomePresentation
import com.clearroad.app.intelligence.RouteIntelligenceAssembly
import com.clearroad.app.ui.model.MarshioGuidanceGoogleStepUiModel
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.google.android.gms.maps.model.LatLng

/**
 * Route Details sheet — assembles [RouteDetailsUiModel] from domain policies.
 * Presentation only; does not affect route selection or scoring.
 */
internal object RouteDetailsAssembly {

    data class Input(
        val detailRouteIndex: Int,
        val detailRoute: RealRouteDebugData,
        val routes: List<RealRouteDebugData>,
        val identities: List<RouteIdentity>,
        val mode: PreferenceMode,
        val recommendedRouteIndex: Int,
        val directionsStatus: String?,
        val fromLatLng: LatLng?,
        val toLatLng: LatLng?,
        val useLegacyHomeFallback: Boolean,
    )

    fun buildUiModel(input: Input): RouteDetailsUiModel {
        val detailIdx = input.detailRouteIndex
        val detailItem = input.detailRoute
        val routes = input.routes
        val recIdxForConfidence =
            input.recommendedRouteIndex.coerceIn(0, routes.lastIndex)
        val isRecommendedRouteDetails = detailIdx == recIdxForConfidence
        val detailPersonality =
            tollPhraseForCard(
                detailItem,
                detailIdx,
                input.mode,
                input.recommendedRouteIndex,
                routes,
            )
        val detailAligned =
            if (input.useLegacyHomeFallback && !isRecommendedRouteDetails) {
                LegacyHomePresentation.recommendationAlignedExplanation(
                    detailPersonality,
                    input.mode,
                )
            } else {
                null
            }
        val recommendedWhyCopy =
            if (isRecommendedRouteDetails) {
                WhyThisRouteLayer.recommendedRouteCopy(
                    mode = input.mode,
                    recommended = detailItem,
                    routes = routes,
                    recommendedIndex = recIdxForConfidence,
                    directionsStatus = input.directionsStatus,
                )
            } else {
                null
            }
        val routeReasonTitle =
            recommendedWhyCopy?.title
                ?: detailAligned?.first
                ?: detailPersonality
        val routeReasonWhy =
            recommendedWhyCopy?.why
                ?: detailAligned?.second.orEmpty()
        val (costSummaryPrimary, costSummarySecondary) =
            TripAtAGlancePolicy.lines(input.mode, routes, detailIdx)
        val recommendationConfidenceCopy =
            if (isRecommendedRouteDetails) {
                RecommendationConfidenceLayer.forMode(
                    input.mode,
                    routes,
                    recIdxForConfidence,
                )
            } else {
                null
            }
        val detailRouteIdentityTitle =
            RouteIdentityPresentationPolicy.displayForDetails(
                route = detailItem,
                identity = input.identities.getOrNull(detailIdx),
                routes = routes,
                identities = input.identities,
                recommendedIndex = recIdxForConfidence,
            ).detailsTitle
        val rejectedAlternativeLines =
            rejectedAlternativeLines(
                routes = routes,
                identities = input.identities,
                recommendedIndex = recIdxForConfidence,
                detailRouteIndex = detailIdx,
                isRecommendedRouteDetails = isRecommendedRouteDetails,
            )
        val comparisonPresentation =
            RouteDetailsComparisonPresentation.build(
                routes = routes,
                identities = input.identities,
                detailRouteIndex = detailIdx,
                recommendedIndex = recIdxForConfidence,
                mode = input.mode,
                directionsStatus = input.directionsStatus,
            )
        val mapEvidence =
            if (isRecommendedRouteDetails) {
                RouteMapEvidencePresentation.build(
                    routes = routes,
                    recommendedIndex = recIdxForConfidence,
                    decisionState = comparisonPresentation.googleMarshioDecision?.state,
                )
            } else {
                null
            }
        val handoffRoutePathPoints =
            if (isRecommendedRouteDetails) {
                routes.getOrNull(recIdxForConfidence)?.routePathPoints.orEmpty()
            } else {
                detailItem.routePathPoints
            }
        val guidanceRoute =
            if (isRecommendedRouteDetails) {
                routes.getOrNull(recIdxForConfidence) ?: detailItem
            } else {
                detailItem
            }
        val driveWeather =
            DriveWeatherAssembly.build(
                DriveWeatherAssembly.Input(
                    detailRoute = detailItem,
                    detailRouteIndex = detailIdx,
                    routes = routes,
                    identities = input.identities,
                    mode = input.mode,
                    recommendedIndex = recIdxForConfidence,
                    isRecommendedRouteDetails = isRecommendedRouteDetails,
                    googleMarshioDecision = comparisonPresentation.googleMarshioDecision,
                    routeIdentityTitle = detailRouteIdentityTitle,
                    routeReasonTitle = routeReasonTitle,
                    routeReasonWhy = routeReasonWhy,
                    directionsStatus = input.directionsStatus,
                ),
            )
        val showMarshioGuidanceEntry =
            isRecommendedRouteDetails && handoffRoutePathPoints.size >= 2
        return buildRouteDetailsUiModel(
            routeIndex = detailIdx,
            routeNumber = detailIdx + 1,
            routeIdentityTitle = detailRouteIdentityTitle,
            routeReasonTitle = routeReasonTitle,
            routeReasonWhy = routeReasonWhy,
            item = detailItem,
            selectedMode = input.mode,
            routes = routes,
            confidenceLabel = confidenceLabel(input.directionsStatus, detailItem),
            costSummaryPrimary = costSummaryPrimary,
            costSummarySecondary = costSummarySecondary,
            recommendationConfidenceTitle =
                recommendationConfidenceCopy?.title.orEmpty(),
            recommendationConfidenceText =
                recommendationConfidenceCopy?.body.orEmpty(),
            recommendationTradeoffText = null,
            isHighConfidence =
                recommendationConfidenceCopy?.isHighConfidence ?: false,
            rejectedAlternativeLines = rejectedAlternativeLines,
            rejectedAlternativesIntro = comparisonPresentation.rejectedAlternativesIntro,
            rejectedAlternatives = comparisonPresentation.rejectedAlternatives,
            googleMarshioDecision = comparisonPresentation.googleMarshioDecision,
            showLegacyWhyCopy =
                comparisonPresentation.googleMarshioDecision == null && !isRecommendedRouteDetails,
            otherRoutePathPoints = comparisonPresentation.otherRoutePathPoints,
            routeOptionsCount = comparisonPresentation.routeOptionsCount,
            mapEvidence = mapEvidence,
            handoffRoutePathPoints = handoffRoutePathPoints,
            googleDefaultRoutePathPoints = routes.firstOrNull()?.routePathPoints.orEmpty(),
            durationSeconds = guidanceRoute.durationSeconds,
            selectedRouteIndex = recIdxForConfidence,
            showMarshioGuidanceEntry = showMarshioGuidanceEntry,
            marshioGuidanceGoogleSteps =
                if (showMarshioGuidanceEntry) {
                    guidanceRoute.googleSteps.map { step ->
                        MarshioGuidanceGoogleStepUiModel(
                            distanceMeters = step.distanceMeters,
                            maneuver = step.maneuver,
                            htmlInstructions = step.htmlInstructions,
                            startLocation = step.startLocation,
                        )
                    }
                } else {
                    emptyList()
                },
            routeIntelligenceRequest =
                RouteIntelligenceAssembly.buildRequest(
                    routes = routes,
                    identities = input.identities,
                    recommendedIndex = recIdxForConfidence,
                    isRecommendedRouteDetails = isRecommendedRouteDetails,
                ),
            routeIntelligenceComparisonRequest =
                RouteIntelligenceAssembly.buildComparisonRequest(
                    routes = routes,
                    identities = input.identities,
                    recommendedIndex = recIdxForConfidence,
                    isRecommendedRouteDetails = isRecommendedRouteDetails,
                ),
            showRouteIntelligenceSection = isRecommendedRouteDetails,
            routeIntelligenceUnavailableReason =
                RouteIntelligenceAssembly.unavailableReason(
                    routes = routes,
                    recommendedIndex = recIdxForConfidence,
                    isRecommendedRouteDetails = isRecommendedRouteDetails,
                ),
            fromLatLng = input.fromLatLng,
            toLatLng = input.toLatLng,
            driveWeather = driveWeather,
        )
    }

    fun confidenceLabel(
        directionsStatus: String?,
        item: RealRouteDebugData,
    ): String {
        if (item.durationSeconds <= 0 || item.distanceMeters <= 0) return "Light read"
        if (directionsStatus != "OK") return "Light read"
        val tollKnownFromFare = item.hasToll || item.tollAED > 0
        return if (tollKnownFromFare) "Steady" else "Typical"
    }

    private fun rejectedAlternativeLines(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        detailRouteIndex: Int,
        isRecommendedRouteDetails: Boolean,
    ): List<String> {
        if (!isRecommendedRouteDetails) {
            logComparativeEvidenceDetailsAudit(
                routes = routes,
                identities = identities,
                recommendedIndex = recommendedIndex,
                isEquivalentTrip = false,
                isRecommendedRouteDetails = false,
                detailRouteIndex = detailRouteIndex,
            )
            return emptyList()
        }
        val equivalentTrip =
            EquivalentTripHonesty.evaluate(
                routes = routes,
                identities = identities,
                recommendedIndex = recommendedIndex,
            ).isEquivalentTrip
        logComparativeEvidenceDetailsAudit(
            routes = routes,
            identities = identities,
            recommendedIndex = recommendedIndex,
            isEquivalentTrip = equivalentTrip,
            isRecommendedRouteDetails = true,
            detailRouteIndex = detailRouteIndex,
        )
        return ComparativeEvidence.buildRejectedAlternatives(
            routes = routes,
            identities = identities,
            recommendedIndex = recommendedIndex,
            isEquivalentTrip = equivalentTrip,
        ).formattedLines
    }
}
