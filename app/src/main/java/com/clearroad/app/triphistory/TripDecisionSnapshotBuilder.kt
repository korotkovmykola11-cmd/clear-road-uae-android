package com.clearroad.app.triphistory

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteDetailsAssembly
import com.google.android.gms.maps.model.LatLng

/** Builds compact decision snapshots at handoff from Route Details assembly input. */
internal object TripDecisionSnapshotBuilder {

    const val GOOGLE_DEFAULT_ROUTE_INDEX = 0

    fun build(input: RouteDetailsAssembly.Input): TripHandoffSnapshotUiModel? {
        if (input.directionsStatus != "OK") return null
        if (input.routes.isEmpty()) return null
        val from = input.fromLatLng ?: return null
        val to = input.toLatLng ?: return null
        if (input.identities.size != input.routes.size) return null

        val viewedRouteIndex = input.detailRouteIndex.coerceIn(0, input.routes.lastIndex)
        val marshioRecommendedRouteIndex =
            input.recommendedRouteIndex.coerceIn(0, input.routes.lastIndex)
        val googleDefaultRouteIndex =
            GOOGLE_DEFAULT_ROUTE_INDEX.coerceIn(0, input.routes.lastIndex)

        val chosenRoute = input.routes[viewedRouteIndex]
        val chosenIdentity = input.identities[viewedRouteIndex]
        val baselineRoute = input.routes[googleDefaultRouteIndex]
        val baselineIdentity = input.identities[googleDefaultRouteIndex]

        val chosenHasSalik = hasSalik(chosenRoute)
        val baselineHasSalik = hasSalik(baselineRoute)
        val bestNoSalikDurationSeconds =
            input.routes
                .filterNot { hasSalik(it) }
                .minOfOrNull { it.durationSeconds.coerceAtLeast(0) }

        val salikTimeDeltaSeconds =
            if (chosenHasSalik && bestNoSalikDurationSeconds != null) {
                bestNoSalikDurationSeconds - chosenRoute.durationSeconds.coerceAtLeast(0)
            } else {
                null
            }

        val (originKey, destinationKey) = TripHistoryKey.pairKeys(from, to)
        val timestamp = System.currentTimeMillis()

        val alternatives =
            input.routes.indices.map { routeIndex ->
                val route = input.routes[routeIndex]
                TripHandoffAlternativeUiModel(
                    routeIndex = routeIndex,
                    routeIdentityKey = input.identities[routeIndex].stableKey,
                    durationSeconds = route.durationSeconds.coerceAtLeast(0),
                    hasSalik = hasSalik(route),
                    tollAed = tollAed(route),
                    roleFlags =
                        buildTripDecisionAlternativeRoleFlags(
                            routeIndex = routeIndex,
                            viewedRouteIndex = viewedRouteIndex,
                            googleDefaultRouteIndex = googleDefaultRouteIndex,
                            marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
                        ),
                )
            }

        return TripHandoffSnapshotUiModel(
            timestamp = timestamp,
            originKey = originKey,
            destinationKey = destinationKey,
            mode = input.mode,
            viewedRouteIndex = viewedRouteIndex,
            marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
            googleDefaultRouteIndex = googleDefaultRouteIndex,
            chosenRouteIdentityKey = chosenIdentity.stableKey,
            chosenDurationSeconds = chosenRoute.durationSeconds.coerceAtLeast(0),
            chosenHasSalik = chosenHasSalik,
            chosenTollAed = tollAed(chosenRoute),
            baselineRouteIdentityKey = baselineIdentity.stableKey,
            baselineDurationSeconds = baselineRoute.durationSeconds.coerceAtLeast(0),
            baselineHasSalik = baselineHasSalik,
            baselineTollAed = tollAed(baselineRoute),
            timeDeltaVsBaselineSeconds =
                baselineRoute.durationSeconds.coerceAtLeast(0) -
                    chosenRoute.durationSeconds.coerceAtLeast(0),
            bestNoSalikDurationSeconds = bestNoSalikDurationSeconds,
            salikTimeDeltaSeconds = salikTimeDeltaSeconds,
            alternatives = alternatives,
        )
    }

    internal fun hasSalik(route: RealRouteDebugData): Boolean =
        route.tollAED > 0 || route.hasToll

    internal fun tollAed(route: RealRouteDebugData): Double? =
        route.tollAED.takeIf { it > 0 }?.toDouble()
}
