package com.clearroad.app.triphistory

internal object TripDecisionAlternativeRole {
    const val HANDOFF_CANDIDATE = 1 shl 0
    const val GOOGLE_DEFAULT = 1 shl 1
    const val MARSHIO_RECOMMENDED = 1 shl 2
}

internal fun buildTripDecisionAlternativeRoleFlags(
    routeIndex: Int,
    handoffCandidateRouteIndex: Int,
    googleDefaultRouteIndex: Int,
    marshioRecommendedRouteIndex: Int,
): Int {
    var flags = 0
    if (routeIndex == handoffCandidateRouteIndex) {
        flags = flags or TripDecisionAlternativeRole.HANDOFF_CANDIDATE
    }
    if (routeIndex == googleDefaultRouteIndex) flags = flags or TripDecisionAlternativeRole.GOOGLE_DEFAULT
    if (routeIndex == marshioRecommendedRouteIndex) {
        flags = flags or TripDecisionAlternativeRole.MARSHIO_RECOMMENDED
    }
    return flags
}
