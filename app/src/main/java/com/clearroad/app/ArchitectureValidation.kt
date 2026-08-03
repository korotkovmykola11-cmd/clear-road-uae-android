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
     * Stage C flip — prod uses Routes API v2 `computeRoutes` + [RoutesV2ResponseAdapter]
     * when true. Bundled with Policy B v2 ([RoutesV2ManeuverPolicy]) and Path B SmoothDrive weights.
     * Override per device: SharedPreferences `debug_fetch_provider=legacy` (no rebuild).
     */
    const val USE_ROUTES_V2_FETCH = true

    /**
     * Stage 31.5 — when true, [SalikDetection] augments scoring only if Google tollAED is 0.
     * UI still shows Google tollAED. Flip to false to disable heuristic ranking.
     */
    const val USE_HEURISTIC_SALIK_FOR_SCORING = true
}
