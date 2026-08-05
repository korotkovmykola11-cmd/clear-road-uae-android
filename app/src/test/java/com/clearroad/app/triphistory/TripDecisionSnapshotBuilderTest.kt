package com.clearroad.app.triphistory

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteDetailsAssembly
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.domain.TollExposure
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripDecisionSnapshotBuilderTest {

    private val origin = LatLng(25.2048, 55.2708)
    private val destination = LatLng(25.0800, 55.1400)

    @Test
    fun build_returnsNullWhenDirectionsNotOk() {
        val snapshot =
            TripDecisionSnapshotBuilder.build(
                input(
                    detailRouteIndex = 0,
                    directionsStatus = "ZERO_RESULTS",
                    routes = listOf(route(600)),
                    identities = listOf(identity("a")),
                ),
            )

        assertNull(snapshot)
    }

    @Test
    fun build_baselineIsGoogleDefaultIndexZero() {
        val routes =
            listOf(
                route(durationSeconds = 900, tollAed = 0),
                route(durationSeconds = 720, tollAed = 8, hasToll = true),
            )
        val identities =
            listOf(
                identity("google-default"),
                identity("salik-faster"),
            )

        val snapshot =
            TripDecisionSnapshotBuilder.build(
                input(
                    detailRouteIndex = 1,
                    recommendedRouteIndex = 1,
                    routes = routes,
                    identities = identities,
                ),
            )!!

        assertEquals(0, snapshot.googleDefaultRouteIndex)
        assertEquals("google-default", snapshot.baselineRouteIdentityKey)
        assertEquals(900, snapshot.baselineDurationSeconds)
        assertEquals(180, snapshot.timeDeltaVsBaselineSeconds)
        assertEquals("salik-faster", snapshot.handoffCandidateRouteIdentityKey)
    }

    @Test
    fun build_salikTimeDeltaUsesBestNoSalikRoute() {
        val routes =
            listOf(
                route(durationSeconds = 800),
                route(durationSeconds = 700, tollAed = 8, hasToll = true),
                route(durationSeconds = 750, tollAed = 8, hasToll = true),
            )
        val identities =
            listOf(
                identity("default"),
                identity("salik-chosen"),
                identity("salik-alt"),
            )

        val snapshot =
            TripDecisionSnapshotBuilder.build(
                input(
                    detailRouteIndex = 1,
                    recommendedRouteIndex = 1,
                    routes = routes,
                    identities = identities,
                ),
            )!!

        assertEquals(800, snapshot.bestNoSalikDurationSeconds)
        assertEquals(100, snapshot.salikTimeDeltaSeconds)
    }

    @Test
    fun build_alternativeRoleFlagsMarkHandoffCandidateBaselineAndRecommended() {
        val routes =
            listOf(
                route(durationSeconds = 900),
                route(durationSeconds = 720),
            )
        val identities =
            listOf(
                identity("baseline"),
                identity("recommended"),
            )

        val snapshot =
            TripDecisionSnapshotBuilder.build(
                input(
                    detailRouteIndex = 1,
                    recommendedRouteIndex = 1,
                    routes = routes,
                    identities = identities,
                ),
            )!!

        val baselineAlt = snapshot.alternatives[0]
        val handoffCandidateAlt = snapshot.alternatives[1]

        assertEquals(
            TripDecisionAlternativeRole.GOOGLE_DEFAULT,
            baselineAlt.roleFlags,
        )
        assertEquals(
            TripDecisionAlternativeRole.HANDOFF_CANDIDATE or TripDecisionAlternativeRole.MARSHIO_RECOMMENDED,
            handoffCandidateAlt.roleFlags,
        )
    }

    @Test
    fun build_handoffCandidateRouteIndexFollowsDetailRouteIndex() {
        val routes =
            listOf(
                route(durationSeconds = 900),
                route(durationSeconds = 720),
            )
        val identities =
            listOf(
                identity("baseline"),
                identity("viewed"),
            )

        val snapshot =
            TripDecisionSnapshotBuilder.build(
                input(
                    detailRouteIndex = 0,
                    recommendedRouteIndex = 1,
                    routes = routes,
                    identities = identities,
                ),
            )!!

        assertEquals(0, snapshot.handoffCandidateRouteIndex)
        assertEquals(1, snapshot.marshioRecommendedRouteIndex)
        assertEquals("baseline", snapshot.handoffCandidateRouteIdentityKey)
    }

    private fun input(
        detailRouteIndex: Int = 1,
        recommendedRouteIndex: Int = 1,
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        directionsStatus: String = "OK",
    ): RouteDetailsAssembly.Input =
        RouteDetailsAssembly.Input(
            detailRouteIndex = detailRouteIndex,
            detailRoute = routes[detailRouteIndex],
            routes = routes,
            identities = identities,
            mode = PreferenceMode.FASTEST,
            recommendedRouteIndex = recommendedRouteIndex,
            directionsStatus = directionsStatus,
            fromLatLng = origin,
            toLatLng = destination,
            useLegacyHomeFallback = false,
        )

    private fun route(
        durationSeconds: Int,
        tollAed: Int = 0,
        hasToll: Boolean = false,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "10 km",
            durationText = "${durationSeconds / 60} min",
            distanceMeters = 10_000,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = hasToll,
        )

    private fun identity(stableKey: String): RouteIdentity =
        RouteIdentity(
            primaryName = stableKey,
            fullName = stableKey,
            stableKey = stableKey,
            disambiguator = null,
            tollExposure = TollExposure.NO_SALIK,
        )
}
