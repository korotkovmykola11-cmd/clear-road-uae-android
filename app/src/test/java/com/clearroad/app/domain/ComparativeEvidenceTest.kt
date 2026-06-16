package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RecommendationSummaryLayer
import com.clearroad.app.buildRecommendationSurfaceUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparativeEvidenceTest {

    @Test
    fun saveAed_slowerButLowerSalik_evidenceOn() {
        val routes =
            listOf(
                route(durationSeconds = 27 * 60, tollAed = 16),
                route(durationSeconds = 35 * 60, tollAed = 4),
            )
        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = 1,
                mode = PreferenceMode.NO_TOLLS,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals("Compared to fastest:", result.lines[0])
        assertEquals("+8 min · lower Salik", result.lines[1])
    }

    @Test
    fun fastest_threeMinAdvantage_evidenceOn() {
        val routes =
            listOf(
                route(durationSeconds = 21 * 60),
                route(durationSeconds = 24 * 60),
                route(durationSeconds = 25 * 60),
            )
        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals("Compared to next option:", result.lines[0])
        assertEquals("3 min faster than next option", result.lines[1])
    }

    @Test
    fun equivalentTrip_evidenceOff() {
        val routes = equivalentLocalRoutes()
        val identities = RouteIdentityResolver.resolveAll(routes)

        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
                isEquivalentTrip =
                    EquivalentTripHonesty.isEquivalentTrip(routes, identities, 0),
            )

        assertFalse(result.isVisible)
    }

    @Test
    fun smooth_sameAsFastestRoute_evidenceOff() {
        val routes =
            listOf(
                route(
                    durationSeconds = 26 * 60,
                    baseDurationSeconds = 24 * 60,
                    durationInTrafficSeconds = 26 * 60,
                ),
                route(
                    durationSeconds = 30 * 60,
                    baseDurationSeconds = 24 * 60,
                    durationInTrafficSeconds = 30 * 60,
                ),
            )
        val fastestIndex = ComparativeEvidence.fastestRouteIndex(routes)!!
        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = fastestIndex,
                mode = PreferenceMode.CALM,
                isEquivalentTrip = false,
            )

        assertFalse(result.isVisible)
    }

    @Test
    fun fastest_noMeaningfulDelta_evidenceOff() {
        val routes =
            listOf(
                route(durationSeconds = 21 * 60),
                route(durationSeconds = 21 * 60 + 45),
            )
        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
                isEquivalentTrip = false,
            )

        assertFalse(result.isVisible)
    }

    @Test
    fun existingCopyUnchangedWhenEvidenceEmpty() {
        val routes =
            listOf(
                route(
                    durationSeconds = 21 * 60,
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11",
                ),
                route(
                    durationSeconds = 21 * 60 + 45,
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
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

        assertTrue(model.comparativeEvidenceLines.isEmpty())
        assertEquals("Quickest on this list.", model.decisionSummary)
        assertEquals(
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS, routes, 0),
            buildRecommendationSurfaceUiModel(
                ready = true,
                loading = false,
                loadingMessage = "",
                routes = routes,
                recommendedRouteIndex = 0,
                routeIdentity = identities[0].primaryName,
                routeIdentities = identities,
                mode = PreferenceMode.NO_TOLLS,
            ).decisionSummary,
        )
    }

    @Test
    fun smooth_lowerTrafficDelayThanFastest_evidenceOn() {
        val routes =
            listOf(
                route(
                    durationSeconds = 28 * 60,
                    baseDurationSeconds = 27 * 60,
                    durationInTrafficSeconds = 28 * 60,
                ),
                route(
                    durationSeconds = 26 * 60,
                    baseDurationSeconds = 24 * 60,
                    durationInTrafficSeconds = 26 * 60,
                ),
            )
        val result =
            ComparativeEvidence.build(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.CALM,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals("Compared to fastest:", result.lines[0])
        assertEquals(
            "+2 min · less affected by traffic slowdowns",
            result.lines[1],
        )
    }

    @Test
    fun rejectedAlternatives_saveWinner_listsFasterRouteWithMoreSalik() {
        val routes =
            listOf(
                route(
                    durationSeconds = 27 * 60,
                    tollAed = 16,
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Toll road",
                ),
                route(
                    durationSeconds = 35 * 60,
                    tollAed = 4,
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 1,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals(1, result.lines.size)
        assertEquals(
            "${identities[0].primaryName}: 8 min faster · +12 AED Salik",
            result.lines[0].formatted,
        )
    }

    @Test
    fun rejectedAlternatives_fastestWinner_listsSlowerRouteThatSavesSalik() {
        val routes =
            listOf(
                route(
                    durationSeconds = 27 * 60,
                    tollAed = 16,
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Toll road",
                ),
                route(
                    durationSeconds = 35 * 60,
                    tollAed = 4,
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals(1, result.lines.size)
        assertEquals(
            "${identities[1].primaryName}: +8 min · saves 12 AED Salik",
            result.lines[0].formatted,
        )
    }

    @Test
    fun rejectedAlternatives_equivalentTrip_hidden() {
        val routes = equivalentLocalRoutes()
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip =
                    EquivalentTripHonesty.isEquivalentTrip(routes, identities, 0),
            )

        assertFalse(result.isVisible)
    }

    @Test
    fun rejectedAlternatives_noMeaningfulTradeoff_hidden() {
        val routes =
            listOf(
                route(durationSeconds = 21 * 60, tollAed = 0),
                route(durationSeconds = 21 * 60 + 45, tollAed = 0),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip = false,
            )

        assertFalse(result.isVisible)
    }

    @Test
    fun rejectedAlternatives_differentIdentityOnly_shown() {
        val routes =
            listOf(
                route(
                    durationSeconds = 21 * 60,
                    tollAed = 0,
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11",
                ),
                route(
                    durationSeconds = 21 * 60 + 30,
                    tollAed = 0,
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertEquals(
            "${identities[1].fullName}: different route",
            result.lines[0].formatted,
        )
    }

    @Test
    fun rejectedAlternatives_oneMinuteSpread_shownWhenPhase1WouldShow() {
        val routes =
            listOf(
                route(durationSeconds = 21 * 60, tollAed = 0),
                route(durationSeconds = 21 * 60 + 75, tollAed = 0),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertTrue(result.lines[0].detail.contains("min"))
    }

    @Test
    fun rejectedAlternatives_differentFullName_disambiguatorShown() {
        val routes =
            listOf(
                route(
                    durationSeconds = 20 * 60,
                    distanceMeters = 16_000,
                    summary = "E11",
                    scan = "E11 marina district",
                ),
                route(
                    durationSeconds = 21 * 60,
                    distanceMeters = 18_000,
                    summary = "E11",
                    scan = "E11 downtown district",
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val result =
            ComparativeEvidence.buildRejectedAlternatives(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
                isEquivalentTrip = false,
            )

        assertTrue(result.isVisible)
        assertTrue(result.lines[0].label.isNotBlank())
    }

    private fun equivalentLocalRoutes(): List<RealRouteDebugData> =
        listOf(
            route(
                durationSeconds = 12 * 60,
                distanceMeters = 8_700,
                summary = "King Faisal St",
                scan = "Sharjah King Faisal St",
            ),
            route(
                durationSeconds = 12 * 60 + 20,
                distanceMeters = 8_800,
                summary = "King Faisal St",
                scan = "Sharjah King Faisal St alt",
            ),
        )

    private fun route(
        durationSeconds: Int,
        tollAed: Int = 0,
        baseDurationSeconds: Int = durationSeconds,
        durationInTrafficSeconds: Int = durationSeconds,
        distanceMeters: Int = 20_000,
        summary: String = "Sheikh Zayed Rd/E11",
        scan: String = "Sheikh Zayed Rd/E11",
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
            baseDurationSeconds = baseDurationSeconds,
            durationInTrafficSeconds = durationInTrafficSeconds,
        )
}
