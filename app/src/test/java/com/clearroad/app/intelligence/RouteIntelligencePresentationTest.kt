package com.clearroad.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteIntelligencePresentationTest {

    @Test
    fun driverRouteCopy_easyCityDrive() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 0,
                        roundaboutsCount = 0,
                        complexityScore = 0.12f,
                    ),
                alternative = null,
            )

        assertEquals("Easy city drive.", copy.headline)
        assertEquals("Simple route with few decision points.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_busyCityWithManyRoundabouts() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 2,
                        roundaboutsCount = 6,
                        complexityScore = 0.48f,
                        mainRoadRatio = 0.4f,
                    ),
                alternative = null,
            )

        assertEquals("Busy city drive.", copy.headline)
        assertEquals("Mostly city streets with many roundabouts.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_moreIntersectionsThanAlternativeRoute() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.4f),
                alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            )

        assertEquals("Busy city drive.", copy.headline)
        assertEquals("More intersections than the alternative route.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_comparativeWordingUsesGoogleDefaultRouteWhenComparisonIndexZero() {
        val marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.55f)
        val alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f)

        assertEquals(
            "More intersections than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = marshio,
                alternative = alternative,
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "Fewer intersections than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "Expect a busier drive than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "A calmer drive than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternativeRouteIndex = 0,
            ),
        )
    }

    @Test
    fun driverRouteCopy_comparativeWordingUsesAlternativeRouteWhenComparisonIndexGreaterThanZero() {
        val marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.55f)
        val alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f)

        assertEquals(
            "More intersections than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = marshio,
                alternative = alternative,
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "Fewer intersections than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "Expect a busier drive than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "A calmer drive than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternativeRouteIndex = 1,
            ),
        )
    }

    @Test
    fun driverRouteCopy_smoothHighwayDrive() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 1,
                        roundaboutsCount = 0,
                        complexityScore = 0.18f,
                        mainRoadRatio = 0.72f,
                    ),
                alternative = null,
            )

        assertEquals("Smooth highway drive.", copy.headline)
        assertEquals("Mostly highway driving with fewer decision points.", copy.explanation)
    }

    @Test
    fun driverDetailLines_avoidRawCounts() {
        val row =
            RouteIntelligencePresentation.rowUiModel(
                label = "MARSHIO route",
                routeName = "E44",
                profile =
                    profile(
                        trafficSignalsCount = 1,
                        roundaboutsCount = 13,
                        complexityScore = 0.62f,
                        mainRoadRatio = 0.42f,
                    ),
            )!!

        assertEquals("Few traffic lights", row.trafficSignalsLine)
        assertEquals("Many roundabouts", row.roundaboutsLine)
        assertEquals("Mix of main and local streets", row.mainRoadLine)
        assertEquals("Higher driving workload", row.complexityLine)
    }

    @Test
    fun comparisonRouteLabel_googleDefaultWhenIndexZero() {
        assertEquals("Google default", RouteIntelligencePresentation.comparisonRouteLabel(0))
        assertEquals("Alternative", RouteIntelligencePresentation.comparisonRouteLabel(1))
    }

    private fun profile(
        trafficSignalsCount: Int?,
        roundaboutsCount: Int?,
        complexityScore: Float?,
        mainRoadRatio: Float = 0.5f,
    ): RouteProfile =
        RouteProfile(
            trafficSignalsCount = trafficSignalsCount,
            roundaboutsCount = roundaboutsCount,
            mainRoadRatio = mainRoadRatio,
            complexityScore = complexityScore,
            osmSourceStatus = SourceStatus.OK,
            evidence = null,
            metadata = null,
        )
}
