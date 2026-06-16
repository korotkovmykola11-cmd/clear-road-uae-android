package com.clearroad.app.ui.model

import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng

/** Short Why tag — Stage 24 will populate; empty in Stage 22 prep. */
data class WhyTagUiModel(
    val label: String,
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
    val whyTags: List<WhyTagUiModel> = emptyList(),
    val mode: PreferenceMode = PreferenceMode.FASTEST,
    val fromLatLng: LatLng? = null,
    val toLatLng: LatLng? = null,
    val routePathPoints: List<LatLng> = emptyList(),
)
