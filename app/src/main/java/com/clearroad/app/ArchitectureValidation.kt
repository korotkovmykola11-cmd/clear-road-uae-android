package com.clearroad.app

/**
 * Temporary flags for Stage 31.5 recommendation-first Home architecture.
 *
 * When [RECOMMENDATION_ONLY_HOME] is true:
 * - Origin and destination inputs are always visible (no search capsule).
 * - Route cards on Home are hidden (code retained for rollback).
 * - [RecommendationSurface] replaces the legacy banner; View Details opens from it only.
 *
 * Flip to false to restore the legacy Home layout for comparison.
 */
internal object ArchitectureValidation {
    const val RECOMMENDATION_ONLY_HOME = true
}
