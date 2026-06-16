package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RecommendationReasonLayer
import com.clearroad.app.RecommendationSummaryLayer
import com.clearroad.app.buildRecommendationSurfaceUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquivalentTripHonestyTest {

    @Test
    fun shortEquivalentRoute_5to6km_honestyOn() {
        val routes = equivalentLocalRoutes(
            distanceMeters = listOf(5_700, 6_100, 5_900),
            durationSeconds = listOf(10 * 60, 11 * 60, 10 * 60 + 30),
            summary = "Al Nuaimiya St",
        )
        val identities = RouteIdentityResolver.resolveAll(routes)

        assertTrue(
            EquivalentTripHonesty.isEquivalentTrip(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            ),
        )
        val result = EquivalentTripHonesty.evaluate(routes, identities, 0)
        assertEquals(EquivalentTripHonesty.CHIP_TEXT, result.chipText)
        assertEquals(EquivalentTripHonesty.NARRATIVE_TEXT, result.narrativeText)
    }

    @Test
    fun shortEquivalentRoute_8to9km_honestyOn() {
        val routes = equivalentLocalRoutes(
            distanceMeters = listOf(8_700, 8_800, 8_600),
            durationSeconds = listOf(12 * 60, 12 * 60 + 20, 12 * 60 + 30),
            summary = "King Faisal St",
        )
        val identities = RouteIdentityResolver.resolveAll(routes)

        assertTrue(
            EquivalentTripHonesty.isEquivalentTrip(
                routes = routes,
                identities = identities,
                recommendedIndex = 1,
            ),
        )
    }

    @Test
    fun longRouteWithSalikDifference_honestyOff() {
        val routes =
            listOf(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Toll road",
                    distanceMeters = 29_700,
                    durationSeconds = 27 * 60,
                    tollAed = 16,
                ),
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Toll road",
                    distanceMeters = 31_700,
                    durationSeconds = 30 * 60,
                    tollAed = 16,
                ),
                route(
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                    distanceMeters = 27_400,
                    durationSeconds = 35 * 60,
                    tollAed = 4,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)

        assertFalse(
            EquivalentTripHonesty.isEquivalentTrip(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            ),
        )
    }

    @Test
    fun longRouteWithDurationSpreadOver120Sec_honestyOff() {
        val routes =
            listOf(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11",
                    distanceMeters = 25_000,
                    durationSeconds = 20 * 60,
                ),
                route(
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                    distanceMeters = 27_000,
                    durationSeconds = 24 * 60,
                ),
                route(
                    summary = "Emirates Rd/E611",
                    scan = "Emirates Rd/E611",
                    distanceMeters = 26_000,
                    durationSeconds = 21 * 60,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)

        assertFalse(
            EquivalentTripHonesty.isEquivalentTrip(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            ),
        )
    }

    @Test
    fun existingModeCopyUnchangedWhenHonestyOff() {
        val routes =
            listOf(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Toll road",
                    distanceMeters = 29_700,
                    durationSeconds = 27 * 60,
                    tollAed = 16,
                ),
                route(
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                    distanceMeters = 27_400,
                    durationSeconds = 35 * 60,
                    tollAed = 4,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val model =
            buildRecommendationSurfaceUiModel(
                ready = true,
                loading = false,
                loadingMessage = "",
                routes = routes,
                recommendedRouteIndex = 0,
                routeIdentity = identities[0].primaryName,
                routeIdentities = identities,
                mode = PreferenceMode.FASTEST,
            )

        assertFalse(model.decisionSummary.isBlank())
        assertEquals("Quickest on this list.", model.decisionSummary)
        assertEquals(
            RecommendationReasonLayer.reason(
                mode = PreferenceMode.FASTEST,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
            ),
            model.recommendationReason,
        )
        assertEquals(
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS, routes, 1),
            buildRecommendationSurfaceUiModel(
                ready = true,
                loading = false,
                loadingMessage = "",
                routes = routes,
                recommendedRouteIndex = 1,
                routeIdentity = identities[1].primaryName,
                routeIdentities = identities,
                mode = PreferenceMode.NO_TOLLS,
            ).decisionSummary,
        )
    }

    @Test
    fun equivalentTrip_sameHonestyCopyForAllModes() {
        val routes = equivalentLocalRoutes(
            distanceMeters = listOf(8_200, 8_400),
            durationSeconds = listOf(12 * 60, 12 * 60 + 15),
            summary = "University City Rd/S120",
        )
        val identities = RouteIdentityResolver.resolveAll(routes)

        for (mode in PreferenceMode.entries) {
            val model =
                buildRecommendationSurfaceUiModel(
                    ready = true,
                    loading = false,
                    loadingMessage = "",
                    routes = routes,
                    recommendedRouteIndex = 0,
                    routeIdentity = "",
                    routeIdentities = identities,
                    mode = mode,
                )
            assertEquals(EquivalentTripHonesty.CHIP_TEXT, model.recommendationReason)
            assertEquals(EquivalentTripHonesty.NARRATIVE_TEXT, model.narrative)
            assertEquals("", model.decisionSummary)
        }
    }

    @Test
    fun audit_listsDurationSpreadRejectWhenSpreadExceedsThreshold() {
        val routes =
            listOf(
                route(
                    summary = "Street A",
                    scan = "Sharjah Street A",
                    distanceMeters = 9_000,
                    durationSeconds = 18 * 60,
                ),
                route(
                    summary = "Street B",
                    scan = "Sharjah Street B",
                    distanceMeters = 9_400,
                    durationSeconds = 21 * 60,
                ),
                route(
                    summary = "Street C",
                    scan = "Sharjah Street C",
                    distanceMeters = 9_200,
                    durationSeconds = 24 * 60,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val snapshot = EquivalentTripHonesty.audit(routes, identities, recommendedIndex = 1)

        assertFalse(snapshot.equivalentTrip)
        assertTrue(
            snapshot.rejectReasons.any { it.startsWith("DURATION_SPREAD_") },
        )
    }

    @Test
    fun audit_listsIdentityRejectsWhenNotCollapsedAndNotSuppressed() {
        val routes =
            listOf(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Ajman Sheikh Zayed Rd/E11",
                    distanceMeters = 9_400,
                    durationSeconds = 21 * 60,
                ),
                route(
                    summary = "King Faisal St",
                    scan = "Ajman King Faisal St",
                    distanceMeters = 9_200,
                    durationSeconds = 21 * 60 + 30,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val snapshot = EquivalentTripHonesty.audit(routes, identities, recommendedIndex = 0)

        assertFalse(snapshot.equivalentTrip)
        assertTrue(snapshot.rejectReasons.contains("IDENTITY_NOT_COLLAPSED"))
    }

    private fun equivalentLocalRoutes(
        distanceMeters: List<Int>,
        durationSeconds: List<Int>,
        summary: String,
    ): List<RealRouteDebugData> =
        distanceMeters.indices.map { index ->
            route(
                summary = summary,
                scan = "Sharjah $summary local street ${index + 1}",
                distanceMeters = distanceMeters[index],
                durationSeconds = durationSeconds[index],
            )
        }

    private fun route(
        summary: String,
        scan: String,
        distanceMeters: Int,
        durationSeconds: Int,
        tollAed: Int = 0,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceMeters / 1000.0} km",
            durationText = "${durationSeconds / 60} mins",
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            routeSummary = summary,
            corridorScanText = scan,
            baseDurationSeconds = durationSeconds,
            durationInTrafficSeconds = durationSeconds,
        )
}
