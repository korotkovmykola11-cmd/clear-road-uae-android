package com.clearroad.app

/**
 * Temporary flags for Stage 31.5 recommendation-first Home architecture.
 *
 * When [RECOMMENDATION_ONLY_HOME] is true (prod):
 * - Origin and destination inputs are always visible (no search capsule).
 * - Route cards on Home are hidden.
 * - [RecommendationSurface] replaces the legacy banner; View Details opens from it only.
 * - Presentation copy uses [com.clearroad.app.domain.ModeExplanationPolicy] tree.
 *
 * When false (rollback): legacy Home uses [com.clearroad.app.legacy] package
 * (route cards, [com.clearroad.app.legacy.MarshallRecommendationBanner],
 * [com.clearroad.app.legacy.RouteReasoning]).
 *
 * Flip to false to restore the legacy Home layout for comparison.
 */
internal object ArchitectureValidation {
    const val RECOMMENDATION_ONLY_HOME = true

    /**
     * Stage 31.5 — when true, [SalikDetection] augments scoring only if Google tollAED is 0.
     * UI still shows Google tollAED. Flip to false to disable heuristic ranking.
     */
    const val USE_HEURISTIC_SALIK_FOR_SCORING = true
}
