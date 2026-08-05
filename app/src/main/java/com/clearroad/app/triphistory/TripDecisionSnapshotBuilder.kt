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

        val handoffCandidateRouteIndex = input.detailRouteIndex.coerceIn(0, input.routes.lastIndex)
        val marshioRecommendedRouteIndex =
            input.recommendedRouteIndex.coerceIn(0, input.routes.lastIndex)
        val googleDefaultRouteIndex =
            GOOGLE_DEFAULT_ROUTE_INDEX.coerceIn(0, input.routes.lastIndex)

        val handoffCandidateRoute = input.routes[handoffCandidateRouteIndex]
        val handoffCandidateIdentity = input.identities[handoffCandidateRouteIndex]
        val baselineRoute = input.routes[googleDefaultRouteIndex]
        val baselineIdentity = input.identities[googleDefaultRouteIndex]

        val handoffCandidateHasSalik = hasSalik(handoffCandidateRoute)
        val baselineHasSalik = hasSalik(baselineRoute)
        val bestNoSalikDurationSeconds =
            input.routes
                .filterNot { hasSalik(it) }
                .minOfOrNull { it.durationSeconds.coerceAtLeast(0) }

        val salikTimeDeltaSeconds =
            if (handoffCandidateHasSalik && bestNoSalikDurationSeconds != null) {
                bestNoSalikDurationSeconds - handoffCandidateRoute.durationSeconds.coerceAtLeast(0)
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
                            handoffCandidateRouteIndex = handoffCandidateRouteIndex,
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
            handoffCandidateRouteIndex = handoffCandidateRouteIndex,
            marshioRecommendedRouteIndex = marshioRecommendedRouteIndex,
            googleDefaultRouteIndex = googleDefaultRouteIndex,
            handoffCandidateRouteIdentityKey = handoffCandidateIdentity.stableKey,
            handoffCandidateDurationSeconds = handoffCandidateRoute.durationSeconds.coerceAtLeast(0),
            handoffCandidateHasSalik = handoffCandidateHasSalik,
            handoffCandidateTollAed = tollAed(handoffCandidateRoute),
            baselineRouteIdentityKey = baselineIdentity.stableKey,
            baselineDurationSeconds = baselineRoute.durationSeconds.coerceAtLeast(0),
            baselineHasSalik = baselineHasSalik,
            baselineTollAed = tollAed(baselineRoute),
            timeDeltaVsBaselineSeconds =
                baselineRoute.durationSeconds.coerceAtLeast(0) -
                    handoffCandidateRoute.durationSeconds.coerceAtLeast(0),
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
