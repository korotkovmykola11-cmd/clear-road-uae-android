package com.clearroad.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsAuditLoggingTest {

    @Test
    fun routeAuditMetricsUsesTrafficDelayFromGoogleFields() {
        val route =
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "35 mins",
                distanceMeters = 10_000,
                durationSeconds = 35 * 60,
                tollAED = 8,
                hasToll = true,
                baseDurationSeconds = 30 * 60,
                durationInTrafficSeconds = 35 * 60,
            )
        val metrics = routeAuditMetrics(route, zeroBasedIndex = 1)
        assertEquals(2, metrics.routeIndex)
        assertEquals(35 * 60, metrics.durationSeconds)
        assertEquals(10_000, metrics.distanceMeters)
        assertEquals(5 * 60, metrics.trafficDelaySeconds)
        assertEquals(2, metrics.tollCount)
    }

    @Test
    fun directionsDebugFixtureRoute2AndRoute3Comparison() {
        val fixture = File("../directions_debug.json")
        if (!fixture.exists()) return

        val routes = extractRouteLegsDebugData(fixture.readText())
        assertTrue("Expected at least 3 routes in fixture", routes.size >= 3)

        val route2 = routeAuditMetrics(routes[1], zeroBasedIndex = 1)
        val route3 = routeAuditMetrics(routes[2], zeroBasedIndex = 2)
        val comparison =
            compareRoutePair(
                left = routes[1],
                leftZeroBasedIndex = 1,
                right = routes[2],
                rightZeroBasedIndex = 2,
            )

        val summary =
            buildString {
                appendLine("Route2=$route2")
                appendLine("Route3=$route3")
                append("Compare=$comparison")
            }
        System.err.println(summary)
        assertEquals(2, route2.routeIndex)
        assertEquals(3, route3.routeIndex)
    }
}
