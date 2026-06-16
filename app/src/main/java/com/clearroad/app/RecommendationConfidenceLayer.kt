package com.clearroad.app

import com.clearroad.app.domain.ConfidenceExplanationPolicy
import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.3 — confidence copy for recommended Route Details.
 * Delegates to [ConfidenceExplanationPolicy].
 */
internal object RecommendationConfidenceLayer {

    data class ConfidenceCopy(
        val title: String,
        val body: String,
        val isHighConfidence: Boolean,
    )

    fun forMode(
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ConfidenceCopy {
        val copy =
            ConfidenceExplanationPolicy.forMode(
                mode = mode,
                routes = routes,
                recommendedIndex = recommendedIndex,
            )
        return ConfidenceCopy(
            title = copy.title,
            body = copy.body,
            isHighConfidence = copy.isHighConfidence,
        )
    }
}
