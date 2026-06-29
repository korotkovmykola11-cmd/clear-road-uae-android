package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDetailsComparisonPresentationTest {

    @Test
    fun build_agreesWhenMarshioMatchesGoogleDefault() {
        val routes =
            listOf(
                route(
                    durationSeconds = 840,
                    durationText = "14 min",
                    distanceMeters = 7300,
                    distanceText = "7.3 km",
                    durationInTrafficSeconds = 960,
                    baseDurationSeconds = 840,
                ),
            )

        val result = buildForRecommended(routes, recommendedIndex = 0)

        assertNotNull(result.googleMarshioDecision)
        assertEquals(GoogleMarshioDecisionState.AGREES, result.googleMarshioDecision?.state)
        assertEquals("MARSHIO agrees with Google", result.googleMarshioDecision?.headline)
        assertEquals("Use this route.", result.googleMarshioDecision?.verdictText)
        assertEquals("MARSHIO says", result.googleMarshioDecision?.singleRoute?.cardTitle)
    }

    @Test
    fun build_disagreesWhenMarshioDiffersFromGoogleDefault() {
        val routes =
            listOf(
                route(
                    durationSeconds = 960,
                    durationText = "16 min",
                    distanceMeters = 7500,
                    distanceText = "7.5 km",
                    durationInTrafficSeconds = 1080,
                    baseDurationSeconds = 960,
                    routeSummary = "Google Default Rd",
                ),
                route(
                    durationSeconds = 840,
                    durationText = "14 min",
                    distanceMeters = 7300,
                    distanceText = "7.3 km",
                    durationInTrafficSeconds = 900,
                    baseDurationSeconds = 840,
                    routeSummary = "Marshio Pick Rd",
                ),
            )
        val identities =
            listOf(
                identity("Google Default Rd"),
                identity("Marshio Pick Rd"),
            )

        val result =
            RouteDetailsComparisonPresentation.build(
                routes = routes,
                identities = identities,
                detailRouteIndex = 1,
                recommendedIndex = 1,
                mode = PreferenceMode.FASTEST,
                directionsStatus = "OK",
            )

        val decision = result.googleMarshioDecision
        assertNotNull(decision)
        assertEquals(GoogleMarshioDecisionState.DISAGREES, decision?.state)
        assertEquals("MARSHIO disagrees with Google", decision?.headline)
        assertEquals("Google says", decision?.googleRoute?.cardTitle)
        assertEquals("MARSHIO says", decision?.marshioRoute?.cardTitle)
        assertTrue(decision?.disagreementReasons?.contains("2 min faster than Google's default") == true)
        assertTrue(decision?.disagreementReasons?.any { it.contains("Saves") } == true)
        assertTrue(decision?.disagreementReasons?.contains("1 min less traffic delay") == true)
        assertEquals(
            "MARSHIO recommends this route because it is 2 min faster than Google's default.",
            decision?.whyOneLiner,
        )
        assertEquals(decision?.whyOneLiner, decision?.verdictText)
    }

    @Test
    fun build_disagreesExplainsSlowerMarshioWithDistanceTradeoff() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3100,
                    distanceText = "3.1 km",
                ),
                route(
                    durationSeconds = 540,
                    durationText = "9 min",
                    distanceMeters = 2000,
                    distanceText = "2.0 km",
                ),
            )

        val result =
            buildForRecommended(
                routes = routes,
                recommendedIndex = 1,
                mode = PreferenceMode.FASTEST,
            )

        val decision = result.googleMarshioDecision
        assertNotNull(decision)
        assertEquals(GoogleMarshioDecisionState.DISAGREES, decision?.state)
        assertEquals(
            "Google's route is 1 min faster. MARSHIO selected the shorter route (1.1 km less). Decide which trade-off you prefer.",
            decision?.whyOneLiner,
        )
        assertTrue(decision?.disagreementReasons?.contains("Saves 1.1 km") == true)
        assertTrue(decision?.disagreementReasons?.contains("No meaningful time difference") == true)
    }

    @Test
    fun build_disagreesHonestTradeoffForSmallDistanceSavings() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3100,
                    distanceText = "3.1 km",
                ),
                route(
                    durationSeconds = 540,
                    durationText = "9 min",
                    distanceMeters = 2839,
                    distanceText = "2.8 km",
                ),
            )

        val result =
            buildForRecommended(
                routes = routes,
                recommendedIndex = 1,
                mode = PreferenceMode.FASTEST,
            )

        val decision = result.googleMarshioDecision
        assertNotNull(decision)
        assertEquals(
            "Google's route is 1 min faster. MARSHIO selected the shorter route (261 m less). Decide which trade-off you prefer.",
            decision?.whyOneLiner,
        )
    }

    @Test
    fun build_disagreesNoTollsExplainsSalikTradeoff() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    hasToll = true,
                    tollAed = 4,
                ),
                route(
                    durationSeconds = 540,
                    durationText = "9 min",
                    distanceMeters = 3100,
                    distanceText = "3.1 km",
                    hasToll = false,
                    tollAed = 0,
                ),
            )

        val result =
            buildForRecommended(
                routes = routes,
                recommendedIndex = 1,
                mode = PreferenceMode.NO_TOLLS,
            )

        val decision = result.googleMarshioDecision
        assertNotNull(decision)
        assertEquals(
            "Google's route is 1 min faster. MARSHIO avoids 4 AED Salik. Matches your No tolls preference. Decide which trade-off you prefer.",
            decision?.whyOneLiner,
        )
        assertTrue(decision?.disagreementReasons?.contains("4 AED less Salik") == true)
        assertTrue(decision?.disagreementReasons?.contains("Matches your No tolls preference") == true)
    }

    @Test
    fun build_disagreesFastestHonestWhenGoogleClearlyFaster() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                ),
                route(
                    durationSeconds = 780,
                    durationText = "13 min",
                    distanceMeters = 3100,
                    distanceText = "3.1 km",
                ),
            )

        val result =
            buildForRecommended(
                routes = routes,
                recommendedIndex = 1,
                mode = PreferenceMode.FASTEST,
            )

        val decision = result.googleMarshioDecision
        assertNotNull(decision)
        assertEquals(
            "Google's default is 5 min faster — MARSHIO picked a different route; compare both before you drive.",
            decision?.whyOneLiner,
        )
    }

    @Test
    fun build_agreementState_rejectedAlternativesUseAlternativeLabelsAndIntro() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    routeSummary = "Sheikh Zayed Road (E11)",
                ),
                route(
                    durationSeconds = 600,
                    durationText = "10 min",
                    distanceMeters = 4100,
                    distanceText = "4.1 km",
                    routeSummary = "Al Khail Road",
                ),
                route(
                    durationSeconds = 540,
                    durationText = "9 min",
                    distanceMeters = 2700,
                    distanceText = "2.7 km",
                    routeSummary = "Sheikh Zayed Alt",
                ),
            )
        val identities =
            listOf(
                identity("Sheikh Zayed Road (E11)"),
                identity("Al Khail Road"),
                identity("Sheikh Zayed Alt"),
            )

        val result =
            RouteDetailsComparisonPresentation.build(
                routes = routes,
                identities = identities,
                detailRouteIndex = 0,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
                directionsStatus = "OK",
            )

        assertEquals(GoogleMarshioDecisionState.AGREES, result.googleMarshioDecision?.state)
        assertEquals(2, result.rejectedAlternatives.size)
        assertTrue(result.rejectedAlternativesIntro.contains("Google also showed 2 alternatives"))
        assertTrue(result.rejectedAlternatives.any { it.label.startsWith("Alternative 2 —") })
        assertTrue(result.rejectedAlternatives.any { it.label.startsWith("Alternative 3 —") })
        assertFalse(result.rejectedAlternatives.any { it.label.contains("Google option") })
    }

    @Test
    fun build_shorterButSlowerAlternative_doesNotSayNoAdvantages() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    routeSummary = "MARSHIO Route",
                ),
                route(
                    durationSeconds = 600,
                    durationText = "10 min",
                    distanceMeters = 4100,
                    distanceText = "4.1 km",
                    routeSummary = "Longer Alt",
                ),
                route(
                    durationSeconds = 540,
                    durationText = "9 min",
                    distanceMeters = 2700,
                    distanceText = "2.7 km",
                    routeSummary = "Sheikh Zayed Road (E11)",
                ),
            )

        val result = buildForRecommended(routes, recommendedIndex = 0)

        val shorterSlower =
            result.rejectedAlternatives.first { it.advantageLines.any { line -> line.contains("shorter") } }
        assertTrue(shorterSlower.drawbackLines.any { it.contains("slower") })
        assertTrue(shorterSlower.verdictText.contains("shorter distance, but slower ETA"))
        assertFalse(shorterSlower.verdictText.contains("No advantages", ignoreCase = true))
    }

    @Test
    fun build_fastestMode_rejectsSlowerEta() {
        val routes =
            listOf(
                route(durationSeconds = 480, durationText = "8 min", distanceMeters = 3000, distanceText = "3.0 km"),
                route(durationSeconds = 600, durationText = "10 min", distanceMeters = 3100, distanceText = "3.1 km"),
                route(durationSeconds = 720, durationText = "12 min", distanceMeters = 3200, distanceText = "3.2 km"),
            )

        val result = buildForRecommended(routes, recommendedIndex = 0, mode = PreferenceMode.FASTEST)

        assertTrue(result.rejectedAlternatives.all { it.verdictText.contains("Rejected") })
        assertTrue(result.rejectedAlternatives.all { it.drawbackLines.any { line -> line.contains("slower") } })
    }

    @Test
    fun build_saveAedMode_rejectsHigherSalik() {
        val routes =
            listOf(
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    hasToll = false,
                    tollAed = 0,
                ),
                route(
                    durationSeconds = 480,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    hasToll = true,
                    tollAed = 4,
                ),
                route(
                    durationSeconds = 500,
                    durationText = "8 min",
                    distanceMeters = 3000,
                    distanceText = "3.0 km",
                    hasToll = true,
                    tollAed = 8,
                ),
            )

        val result = buildForRecommended(routes, recommendedIndex = 0, mode = PreferenceMode.NO_TOLLS)

        assertTrue(result.rejectedAlternativesIntro.contains("Google also showed"))
        assertTrue(
            result.rejectedAlternatives.all {
                it.verdictText.contains("Salik", ignoreCase = true) ||
                    it.drawbackLines.any { line -> line.contains("Salik") }
            },
        )
    }

    @Test
    fun build_disagreementState_usesDisagreementIntro() {
        val routes =
            listOf(
                route(durationSeconds = 600, durationText = "10 min", distanceMeters = 4000, distanceText = "4.0 km"),
                route(durationSeconds = 480, durationText = "8 min", distanceMeters = 3000, distanceText = "3.0 km"),
                route(durationSeconds = 720, durationText = "12 min", distanceMeters = 3500, distanceText = "3.5 km"),
            )

        val result = buildForRecommended(routes, recommendedIndex = 1)

        assertEquals(GoogleMarshioDecisionState.DISAGREES, result.googleMarshioDecision?.state)
        assertEquals(
            "These are the other Google routes MARSHIO rejected.",
            result.rejectedAlternativesIntro,
        )
    }

    @Test
    fun trafficDelayText_hiddenWhenTrafficDataMissing() {
        val route = route(durationSeconds = 600, durationText = "10 min", distanceMeters = 1000, distanceText = "1 km")
        assertEquals(null, RouteDetailsComparisonPresentation.trafficDelayText(route))
    }

    @Test
    fun salikText_hiddenWhenUnknown() {
        val route =
            route(
                durationSeconds = 600,
                durationText = "10 min",
                distanceMeters = 1000,
                distanceText = "1 km",
                hasToll = true,
                tollAed = 0,
            )
        assertEquals(null, RouteDetailsComparisonPresentation.salikText(route))
    }

    private fun buildForRecommended(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        mode: PreferenceMode = PreferenceMode.FASTEST,
    ) =
        RouteDetailsComparisonPresentation.build(
            routes = routes,
            identities = emptyList(),
            detailRouteIndex = recommendedIndex,
            recommendedIndex = recommendedIndex,
            mode = mode,
            directionsStatus = "OK",
        )

    private fun identity(name: String): RouteIdentity =
        RouteIdentity(
            primaryName = name,
            fullName = name,
            stableKey = name,
            disambiguator = null,
            tollExposure = com.clearroad.app.domain.TollExposure.NO_SALIK,
        )

    private fun route(
        durationSeconds: Int,
        durationText: String,
        distanceMeters: Int,
        distanceText: String,
        durationInTrafficSeconds: Int? = null,
        baseDurationSeconds: Int = durationSeconds,
        routeSummary: String = "",
        hasToll: Boolean = false,
        tollAed: Int = 0,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = distanceText,
            durationText = durationText,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = hasToll,
            routeSummary = routeSummary,
            baseDurationSeconds = baseDurationSeconds,
            durationInTrafficSeconds = durationInTrafficSeconds,
        )
}
