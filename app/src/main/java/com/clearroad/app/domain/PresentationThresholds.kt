package com.clearroad.app.domain

/**
 * Shared presentation thresholds — one source of truth for copy and visibility gates.
 * Does not affect route selection or scoring formulas.
 */
internal object PresentationThresholds {

    const val CONFIDENCE_LOW_MAX_SPREAD_SECONDS = 90
    const val CONFIDENCE_MEDIUM_MAX_SPREAD_SECONDS = 180

    const val EQUIVALENT_TRIP_MAX_DURATION_SPREAD_SECONDS = 90

    const val IDENTITY_HIDE_DURATION_SPREAD_SECONDS = 90
    const val IDENTITY_SHOW_DURATION_SPREAD_SECONDS = 120
    const val LONG_ROUTE_DISTANCE_METERS = 18_000
    const val SHORT_LOCAL_DISTANCE_METERS = 12_000

    const val COMPARATIVE_FASTEST_MIN_ADVANTAGE_SECONDS = 60
    const val COMPARATIVE_SAVE_AED_MIN_TIME_DELTA_SECONDS = 120
    const val COMPARATIVE_SAVE_AED_SAME_TIME_TOLERANCE_SECONDS = 60
    const val COMPARATIVE_SMOOTH_MEANINGFUL_DELAY_DELTA_SECONDS = 60
    const val COMPARATIVE_DETAILS_MIN_TIME_DELTA_SECONDS = COMPARATIVE_FASTEST_MIN_ADVANTAGE_SECONDS
    const val ROUTES_VISIBLY_DIFFER_DISTANCE_METERS = 300
}
