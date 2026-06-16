package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Stage 31.5 decision narrative — delegates to [ModeExplanationPolicy].
 */
internal object DecisionNarrativeSynthesis {

    fun narrative(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        @Suppress("UNUSED_PARAMETER") recommendedTollAed: Int,
        @Suppress("UNUSED_PARAMETER") highConfidence: Boolean,
    ): String = ModeExplanationPolicy.narrative(mode, routes, recommendedIndex)
}
