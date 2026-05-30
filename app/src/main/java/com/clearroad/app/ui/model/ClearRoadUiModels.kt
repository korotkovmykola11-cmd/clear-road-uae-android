package com.clearroad.app.ui.model

import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng

/** Short Why tag — Stage 24 will populate; empty in Stage 22 prep. */
data class WhyTagUiModel(
    val label: String,
)

/** Reserved for optional YUNO guide slot — not rendered in Stage 22. */
data class GuideSlotUiModel(
    val visible: Boolean = false,
    val message: String? = null,
)

data class ChoiceWhyTipUiModel(
    val choice: String,
    val why: String,
    val tip: String,
    val compact: Boolean,
    val guideSlot: GuideSlotUiModel = GuideSlotUiModel(),
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
    val routeReasonTitle: String,
    val routeReasonWhy: String,
    val durationText: String,
    val distanceText: String,
    val tollAed: Int,
    val confidenceLabel: String,
    val costSummaryPrimary: String,
    val costSummarySecondary: String?,
    val decisionSnapshotRecommendedHeading: String,
    val decisionSnapshotRecommendedSummary: String,
    val decisionSnapshotOthersHeading: String,
    val decisionSnapshotOthersSummary: String,
    val recommendationConfidenceText: String,
    val isHighConfidence: Boolean = false,
    val whyTags: List<WhyTagUiModel> = emptyList(),
    val mode: PreferenceMode = PreferenceMode.FASTEST,
    val fromLatLng: LatLng? = null,
    val toLatLng: LatLng? = null,
    val guideSlot: GuideSlotUiModel = GuideSlotUiModel(),
)
