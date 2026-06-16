package com.clearroad.app

import com.clearroad.app.domain.ModeExplanationPolicy
import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.4 — one-line human summary for the Home recommendation card.
 * Delegates to [ModeExplanationPolicy].
 */
internal object RecommendationSummaryLayer {

    fun forMode(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String = ModeExplanationPolicy.summary(mode, routes, recommendedIndex)
}
