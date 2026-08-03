package com.clearroad.app

import com.clearroad.app.benchmark.RoutesV2ResponseAdapterTestFixtures
import com.clearroad.app.domain.SmoothDriveScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesV2ResponseAdapterTest {

    @Test
    fun sharjahFixture_mapsTrafficIntervalsAndJamSegments() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json"),
            )

        assertTrue(routes.isNotEmpty())
        val route = routes.first()
        assertTrue(route.trafficSpeedIntervals.isNotEmpty())
        assertTrue(
            route.trafficSpeedIntervals.any { it.speedCategory == SpeedCategory.JAM },
        )
        val segments = TrafficPolylineBuilder.build(route)
        assertTrue(segments.any { it.speedCategory == SpeedCategory.JAM })
    }

    @Test
    fun sharjahFixture_decodesHighQualityPolyline() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json"),
            )
        val route = routes.first()
        assertTrue(route.routePathPoints.size >= 100)
        assertTrue(route.trafficSpeedIntervals.isNotEmpty())
        val lastEnd = route.trafficSpeedIntervals.maxOf { it.endPointIndex }
        assertTrue(lastEnd >= route.routePathPoints.lastIndex - 2)
    }

    @Test
    fun difcMarinaFixture_mapsStaticAndTrafficDuration() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-difc-marina-traffic.json"),
            )
        val route = routes.first()
        assertTrue(route.baseDurationSeconds > 0)
        assertTrue(route.durationInTrafficSeconds!! >= route.baseDurationSeconds)
    }

    @Test
    fun difcMarinaFixture_parsesMultipleAlternatives() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-difc-marina-traffic.json"),
            )
        assertTrue(routes.size >= 2)
        routes.forEach { route ->
            assertTrue(route.distanceMeters > 0)
            assertTrue(route.routePathPoints.size >= 2)
        }
    }

    @Test
    fun adapter_producesSmoothDriveInputs() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json"),
            )
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        assertEquals(routes.size, inputs.size)
        assertTrue(SmoothDriveScoring.pickWinnerIndex(inputs) in routes.indices)
    }

    @Test
    fun sharjahFixture_extractsCriticalManeuversFromV2ManeuverField() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json"),
            )
        val route = routes.first()
        assertTrue((route.criticalManeuversCount ?: 0) > 0)
        assertTrue(route.googleSteps.any { it.maneuver == "MERGE" })
    }

    @Test
    fun routesV2ManeuverEnums_mapToCriticalKeywords() {
        assertTrue(DriverStressAudit.isCriticalManeuver("MERGE"))
        assertTrue(DriverStressAudit.isCriticalManeuver("RAMP_LEFT"))
        assertTrue(DriverStressAudit.isCriticalManeuver("ROUNDABOUT_LEFT"))
        assertFalse(DriverStressAudit.isCriticalManeuver("STRAIGHT"))
    }
}
