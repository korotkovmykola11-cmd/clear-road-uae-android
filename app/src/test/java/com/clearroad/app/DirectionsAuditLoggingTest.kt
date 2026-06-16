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

        val raw = fixture.readText()
        val routes = extractRouteLegsDebugData(raw)
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
        val routeJsonObjects = extractAllRouteObjectJson(raw)
        val uniqueCount = uniquePolylineHashCountFromRaw(routes, routeJsonObjects)

        val summary =
            buildString {
                appendLine("Route2=$route2")
                appendLine("Route3=$route3")
                appendLine("UniqueByPolyline=$uniqueCount/${routes.size}")
                append("Compare=$comparison")
            }
        System.err.println(summary)
        assertEquals(2, route2.routeIndex)
        assertEquals(3, route3.routeIndex)
        assertTrue(
            "Fixture should contain distinct polylines",
            uniqueCount >= 2,
        )
    }

    @Test
    fun uniquePolylineHashCount_treatsIdenticalPathsAsOne() {
        val points =
            listOf(
                com.google.android.gms.maps.model.LatLng(25.0, 55.0),
                com.google.android.gms.maps.model.LatLng(25.1, 55.1),
            )
        val route =
            RealRouteDebugData(
                distanceText = "5 km",
                durationText = "10 mins",
                distanceMeters = 5_000,
                durationSeconds = 600,
                tollAED = 0,
                hasToll = false,
                routePathPoints = points,
            )
        val duplicate =
            route.copy(durationSeconds = 620, durationText = "11 mins")
        assertEquals(1, uniquePolylineHashCount(listOf(route, duplicate)))
    }

    @Test
    fun directionsAuditPolylineShortHash_differsForDifferentEncodedPolylines() {
        val hashA =
            directionsAuditPolylineShortHash(
                encodedPolyline = "abc123",
                decodedPoints = emptyList(),
            )
        val hashB =
            directionsAuditPolylineShortHash(
                encodedPolyline = "xyz789",
                decodedPoints = emptyList(),
            )
        assertTrue(hashA.isNotBlank())
        assertTrue(hashB.isNotBlank())
        assertTrue(hashA != hashB)
    }
}
