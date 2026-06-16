package com.clearroad.app

import com.clearroad.app.domain.ModeExplanationPolicy
import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.7 — one-line factual reason chip for the Home recommendation card.
 * Delegates to [ModeExplanationPolicy].
 */
internal object RecommendationReasonLayer {

    fun reason(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String =
        ModeExplanationPolicy.reasonChip(
            mode = mode,
            recommended = recommended,
            routes = routes,
            recommendedIndex = recommendedIndex,
        )
}
