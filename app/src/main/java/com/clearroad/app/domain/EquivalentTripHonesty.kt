package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Presentation-only honesty when all Google alternatives are effectively equivalent.
 * Does not affect route selection, scoring, or winner index.
 */
internal object EquivalentTripHonesty {

    const val MAX_DURATION_SPREAD_SECONDS =
        PresentationThresholds.EQUIVALENT_TRIP_MAX_DURATION_SPREAD_SECONDS
    const val CHIP_TEXT = "All options about the same"
    const val NARRATIVE_TEXT =
        "Google shows similar routes here. Open Maps — any option works."

    data class Result(
        val isEquivalentTrip: Boolean,
        val chipText: String,
        val narrativeText: String,
    )

    fun evaluate(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): Result {
        if (!isEquivalentTrip(routes, identities, recommendedIndex)) {
            return Result(
                isEquivalentTrip = false,
                chipText = "",
                narrativeText = "",
            )
        }
        return Result(
            isEquivalentTrip = true,
            chipText = CHIP_TEXT,
            narrativeText = NARRATIVE_TEXT,
        )
    }

    internal fun audit(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): HonestyAuditSnapshot {
        val rejectReasons = mutableListOf<String>()
        val routeCount = routes.size

        if (routeCount < 2) {
            rejectReasons.add("ROUTE_COUNT_$routeCount")
        }
        if (identities.isEmpty()) {
            rejectReasons.add("NO_IDENTITIES")
        }

        val allTollFree =
            routes.isNotEmpty() && SalikFacts.allTollZero(routes)
        if (routes.isNotEmpty() && !allTollFree) {
            rejectReasons.add("TOLL_DIFFERENCE")
        }

        val allTrafficDelayZero =
            routes.isNotEmpty() &&
                RouteIdentityPresentationPolicy.allTrafficDelayZero(routes)
        if (routes.isNotEmpty() && !allTrafficDelayZero) {
            rejectReasons.add("TRAFFIC_DELAY_PRESENT")
        }

        val durationSpreadSeconds =
            RouteIdentityPresentationPolicy.durationSpreadSeconds(routes)
        if (durationSpreadSeconds > MAX_DURATION_SPREAD_SECONDS) {
            rejectReasons.add("DURATION_SPREAD_$durationSpreadSeconds")
        }

        val recIdx =
            if (routes.isEmpty()) {
                0
            } else {
                recommendedIndex.coerceIn(0, routes.lastIndex)
            }
        val identityCollapsed = RouteIdentityPresentationPolicy.identitiesCollapsed(identities)
        val identitySuppressed =
            if (routes.isNotEmpty() && identities.isNotEmpty()) {
                !RouteIdentityPresentationPolicy.shouldShowOnHome(routes, identities, recIdx)
            } else {
                false
            }

        val identityGateMet = identitySuppressed || identityCollapsed
        if (routes.size >= 2 && identities.isNotEmpty() && !identityGateMet) {
            if (!identitySuppressed) {
                rejectReasons.add("IDENTITY_NOT_SUPPRESSED")
            }
            if (!identityCollapsed) {
                rejectReasons.add("IDENTITY_NOT_COLLAPSED")
            }
        }

        val equivalentTrip = rejectReasons.isEmpty()

        return HonestyAuditSnapshot(
            equivalentTrip = equivalentTrip,
            durationSpreadSeconds = durationSpreadSeconds,
            allTollFree = allTollFree,
            allTrafficDelayZero = allTrafficDelayZero,
            identityCollapsed = identityCollapsed,
            identitySuppressed = identitySuppressed,
            routeCount = routeCount,
            recommendedIndex = recIdx,
            rejectReasons = rejectReasons.toList(),
            identityPrimaryNames = identities.map { it.primaryName },
            identityStableKeys = identities.map { it.stableKey },
        )
    }

    internal data class HonestyAuditSnapshot(
        val equivalentTrip: Boolean,
        val durationSpreadSeconds: Int,
        val allTollFree: Boolean,
        val allTrafficDelayZero: Boolean,
        val identityCollapsed: Boolean,
        val identitySuppressed: Boolean,
        val routeCount: Int,
        val recommendedIndex: Int,
        val rejectReasons: List<String>,
        val identityPrimaryNames: List<String>,
        val identityStableKeys: List<String>,
    )

    internal fun isEquivalentTrip(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): Boolean {
        if (routes.size < 2 || identities.isEmpty()) return false

        if (!SalikFacts.allTollZero(routes)) return false
        if (!RouteIdentityPresentationPolicy.allTrafficDelayZero(routes)) return false
        if (
            RouteIdentityPresentationPolicy.durationSpreadSeconds(routes) >
            MAX_DURATION_SPREAD_SECONDS
        ) {
            return false
        }

        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val identitySuppressed =
            !RouteIdentityPresentationPolicy.shouldShowOnHome(routes, identities, recIdx)
        val identitiesCollapsed = RouteIdentityPresentationPolicy.identitiesCollapsed(identities)

        return identitySuppressed || identitiesCollapsed
    }
}
