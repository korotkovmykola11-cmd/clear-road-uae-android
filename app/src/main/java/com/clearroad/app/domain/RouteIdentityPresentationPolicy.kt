package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import java.util.Locale

/**
 * Presentation-only policy for when Route Identity appears on Home vs Details.
 * Does not affect route selection, scoring, or winner index.
 */
internal object RouteIdentityPresentationPolicy {

    private val majorHighwayKeys = setOf("E11", "E311", "E611", "E44")

    enum class RouteIdentityVisibility {
        FULL,
        SUPPRESSED,
    }

    data class RouteIdentityDisplay(
        val homeLine: String,
        val detailsTitle: String,
        val visibility: RouteIdentityVisibility,
    )

    fun displayForHome(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): RouteIdentityDisplay {
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex.coerceAtLeast(0))
        val identity = identities.getOrNull(recIdx)
        val showOnHome = shouldShowOnHome(routes, identities, recIdx)
        val summary = honestSummary(routes[recIdx])

        return if (showOnHome) {
            RouteIdentityDisplay(
                homeLine = identity?.primaryName.orEmpty(),
                detailsTitle = identity?.fullName.orEmpty().ifBlank { summary },
                visibility = RouteIdentityVisibility.FULL,
            )
        } else {
            RouteIdentityDisplay(
                homeLine = "",
                detailsTitle = summary.ifBlank { identity?.fullName.orEmpty() },
                visibility = RouteIdentityVisibility.SUPPRESSED,
            )
        }
    }

    fun displayForDetails(
        route: RealRouteDebugData,
        identity: RouteIdentity?,
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): RouteIdentityDisplay {
        val showOnHome = shouldShowOnHome(routes, identities, recommendedIndex)
        val summary = honestSummary(route)

        return if (showOnHome) {
            RouteIdentityDisplay(
                homeLine = identity?.primaryName.orEmpty(),
                detailsTitle = identity?.fullName.orEmpty().ifBlank { summary },
                visibility = RouteIdentityVisibility.FULL,
            )
        } else {
            RouteIdentityDisplay(
                homeLine = "",
                detailsTitle = summary.ifBlank { identity?.fullName.orEmpty() },
                visibility = RouteIdentityVisibility.SUPPRESSED,
            )
        }
    }

    internal fun shouldShowOnHome(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
    ): Boolean {
        if (routes.isEmpty() || identities.isEmpty()) return false
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)

        if (salikDiffers(routes)) return true

        val durationSpread = durationSpreadSeconds(routes)
        if (
            durationSpread > PresentationThresholds.IDENTITY_SHOW_DURATION_SPREAD_SECONDS &&
            distinctStableKeyCount(identities) >= 2
        ) {
            return true
        }

        val recommended = routes[recIdx]
        val corridor = SmoothDriveScoring.classifyCorridor(recommended.corridorScanText)
        if (
            recommended.distanceMeters >= PresentationThresholds.LONG_ROUTE_DISTANCE_METERS &&
            corridor != SmoothDriveScoring.CorridorClass.URBAN_WEAVE
        ) {
            return true
        }

        if (majorHighwayInSummary(recommended)) return true

        if (
            allTollZero(routes) &&
            allTrafficDelayZero(routes) &&
            durationSpread <= PresentationThresholds.IDENTITY_HIDE_DURATION_SPREAD_SECONDS &&
            identitiesCollapsed(identities)
        ) {
            return false
        }

        return true
    }

    internal fun honestSummary(route: RealRouteDebugData): String =
        route.routeSummary.trim().ifBlank {
            RouteIdentityExtractor.extractSummary(route).trim()
        }

    internal fun salikDiffers(routes: List<RealRouteDebugData>): Boolean {
        if (routes.size < 2) return false
        return routes.indices.any { left ->
            routes.indices.any { right ->
                left != right && SalikFacts.salikDiffers(routes[left], routes[right])
            }
        }
    }

    internal fun allTollZero(routes: List<RealRouteDebugData>): Boolean =
        SalikFacts.allTollZero(routes)

    internal fun allTrafficDelayZero(routes: List<RealRouteDebugData>): Boolean =
        routes.all { presentationTrafficDelaySeconds(it) == 0 }

    internal fun durationSpreadSeconds(routes: List<RealRouteDebugData>): Int {
        if (routes.isEmpty()) return 0
        return routes.maxOf { it.durationSeconds } - routes.minOf { it.durationSeconds }
    }

    internal fun identitiesCollapsed(identities: List<RouteIdentity>): Boolean {
        if (identities.isEmpty()) return true
        if (identities.map { it.primaryName }.distinct().size == 1) return true
        return identities.map { it.stableKey }.distinct().size == 1
    }

    internal fun distinctStableKeyCount(identities: List<RouteIdentity>): Int =
        identities.map { it.stableKey }.distinct().size

    internal fun majorHighwayInSummary(route: RealRouteDebugData): Boolean {
        val summary = honestSummary(route)
        if (summary.isBlank()) return false
        val entry = UaeRoadCanon.matchHighestPriorityCanon(summary) ?: return false
        return entry.stableKey in majorHighwayKeys
    }

    internal fun presentationTollCount(route: RealRouteDebugData): Int =
        SalikFacts.presentationTollCount(route)

    internal fun presentationTrafficDelaySeconds(route: RealRouteDebugData): Int {
        val trafficSeconds =
            route.durationInTrafficSeconds?.takeIf { it > 0 }
                ?: route.durationSeconds.takeIf { it > 0 }
                ?: 0
        val baseSeconds =
            route.baseDurationSeconds.takeIf { it > 0 } ?: trafficSeconds
        return (trafficSeconds - baseSeconds).coerceAtLeast(0)
    }
}
