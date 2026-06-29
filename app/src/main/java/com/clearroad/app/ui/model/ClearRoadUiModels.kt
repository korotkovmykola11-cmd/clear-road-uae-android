package com.clearroad.app.ui.model

import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng

data class RouteIntelligenceRouteInputUiModel(
    val routeIndex: Int,
    val routeName: String,
    val routePathPoints: List<LatLng>,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val googleSteps: List<GoogleDirectionsStepInputUiModel> = emptyList(),
    val criticalManeuversCount: Int? = null,
)

data class GoogleDirectionsStepInputUiModel(
    val distanceMeters: Int,
    val maneuver: String?,
)

data class RouteIntelligenceRequestUiModel(
    val marshioRoute: RouteIntelligenceRouteInputUiModel,
    val alternativeRoute: RouteIntelligenceRouteInputUiModel?,
)

data class RouteIntelligenceComparisonRequestUiModel(
    val routes: List<RouteIntelligenceRouteInputUiModel>,
    val marshioSelectedRouteIndex: Int,
    val googleDefaultRouteIndex: Int = 0,
)

data class RouteIntelligenceRouteRowUiModel(
    val label: String,
    val routeName: String,
    val trafficSignalsLine: String,
    val roundaboutsLine: String,
    val mainRoadLine: String,
    val complexityLine: String,
)

data class RouteIntelligenceUiModel(
    val loading: Boolean = false,
    val available: Boolean = false,
    val unavailableMessage: String = "",
    val marshioRoute: RouteIntelligenceRouteRowUiModel? = null,
    val alternativeRoute: RouteIntelligenceRouteRowUiModel? = null,
)

/** Short Why tag — Stage 24 will populate; empty in Stage 22 prep. */
data class WhyTagUiModel(
    val label: String,
)

data class RouteDecisionRouteCardUiModel(
    val cardTitle: String,
    val roadName: String,
    val etaText: String,
    val distanceText: String,
    val trafficDelayText: String? = null,
    val salikText: String? = null,
)

enum class GoogleMarshioDecisionState {
    AGREES,
    DISAGREES,
    NO_MEANINGFUL_DIFFERENCE,
}

data class RouteGoogleMarshioDecisionUiModel(
    val state: GoogleMarshioDecisionState,
    val headline: String,
    val subtext: String,
    val singleRoute: RouteDecisionRouteCardUiModel? = null,
    val googleRoute: RouteDecisionRouteCardUiModel? = null,
    val marshioRoute: RouteDecisionRouteCardUiModel? = null,
    val disagreementReasons: List<String> = emptyList(),
    val whyOneLiner: String = "",
    val verdictText: String,
)

data class RejectedAlternativeUiModel(
    val label: String,
    val advantageLines: List<String> = emptyList(),
    val drawbackLines: List<String> = emptyList(),
    val verdictText: String,
)

/** Map evidence when MARSHIO disagrees with Google — presentation only. */
data class RouteMapEvidenceUiModel(
    val enabled: Boolean,
    val strategicHeadline: String = "Route comparison",
    val strategicCaption: String = "Full trip — gray is Google, green is MARSHIO.",
    val localHeadline: String = "Where routes split",
    val localCaption: String = "",
    val splitPoint: LatLng? = null,
    val splitLabel: String = "Routes split here",
    val sharedPath: List<LatLng> = emptyList(),
    val googleDivergentPath: List<LatLng> = emptyList(),
    val marshioDivergentPath: List<LatLng> = emptyList(),
    val googleComparisonPath: List<LatLng> = emptyList(),
    val marshioComparisonPath: List<LatLng> = emptyList(),
)

/** Stage 31.5 recommendation surface — decision-first home model. */
data class RecommendationSurfaceUiModel(
    val ready: Boolean,
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val routeName: String = "",
    val travelTime: String = "",
    val decisionSummary: String = "Best Decision Right Now",
    val decisionLabel: String = "",
    val recommendationBadge: String = "",
    val recommendationReason: String = "",
    val comparativeEvidenceLines: List<String> = emptyList(),
    val narrative: String = "",
    val mode: PreferenceMode = PreferenceMode.FASTEST,
    val emptyTitle: String = "YUNO is ready to help choose the best route.",
    val emptySubtitle: String =
        "Enter origin and destination to receive a recommendation.",
)

/** MARSHIO Home v2.2 recommendation banner — visual model only. */
data class MarshallRecommendationBannerUiModel(
    val ready: Boolean,
    val label: String = "RECOMMENDED ROUTE",
    val metricsLine: String,
    val routeIdentity: String,
    val whyLine: String,
)

data class ChoiceWhyTipUiModel(
    val choice: String,
    val why: String,
    val tip: String,
    val compact: Boolean,
)

data class RouteCardUiModel(
    val routeIndex: Int,
    val routeTitle: String,
    val durationText: String,
    val distanceText: String,
    val salikLine: String,
    val confidence: String,
    val isRecommended: Boolean,
    val isUserSelected: Boolean,
    val showUserSelectedChrome: Boolean,
    val recommendedNuance: String?,
    val durationOnSurfaceAlpha: Float,
    val whyTags: List<WhyTagUiModel> = emptyList(),
    val mode: PreferenceMode,
)

data class RouteDetailsUiModel(
    val routeNumber: Int,
    val routeIdentityTitle: String,
    val routeReasonTitle: String,
    val routeReasonWhy: String,
    val durationText: String,
    val distanceText: String,
    val tollAed: Int,
    val confidenceLabel: String,
    val costSummaryPrimary: String,
    val costSummarySecondary: String?,
    val recommendationConfidenceTitle: String = "",
    val recommendationConfidenceText: String,
    val recommendationTradeoffText: String? = null,
    val isHighConfidence: Boolean = false,
    val rejectedAlternativeLines: List<String> = emptyList(),
    val rejectedAlternativesIntro: String = "",
    val rejectedAlternatives: List<RejectedAlternativeUiModel> = emptyList(),
    val googleMarshioDecision: RouteGoogleMarshioDecisionUiModel? = null,
    val showLegacyWhyCopy: Boolean = false,
    val whyTags: List<WhyTagUiModel> = emptyList(),
    val mode: PreferenceMode = PreferenceMode.FASTEST,
    val fromLatLng: LatLng? = null,
    val toLatLng: LatLng? = null,
    val routePathPoints: List<LatLng> = emptyList(),
    val otherRoutePathPoints: List<List<LatLng>> = emptyList(),
    val routeOptionsCount: Int = 1,
    val mapEvidence: RouteMapEvidenceUiModel? = null,
    val handoffRoutePathPoints: List<LatLng> = emptyList(),
    val googleDefaultRoutePathPoints: List<LatLng> = emptyList(),
    val durationSeconds: Int = 0,
    val selectedRouteIndex: Int = 0,
    val showMarshioGuidanceEntry: Boolean = false,
    val routeIntelligenceRequest: RouteIntelligenceRequestUiModel? = null,
    val routeIntelligenceComparisonRequest: RouteIntelligenceComparisonRequestUiModel? = null,
)
