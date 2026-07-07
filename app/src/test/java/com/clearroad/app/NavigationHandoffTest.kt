package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationHandoffTest {

    @Test
    fun googleMapsDirectionsUrl_usesQueryParamsWithPipeSeparatedWaypoints() {
        val origin = LatLng(25.0, 55.0)
        val destination = LatLng(25.1, 55.1)
        val waypoints =
            listOf(
                LatLng(25.02, 55.02),
                LatLng(25.04, 55.04),
            )

        val url = googleMapsDirectionsUrl(origin, destination, waypoints)

        assertTrue(url.startsWith("https://www.google.com/maps/dir/?api=1"))
        assertFalse(url.contains("via:"))
        assertFalse(url.contains("%2C"))
        assertTrue(url.contains("origin=25.0,55.0"))
        assertTrue(url.contains("destination=25.1,55.1"))
        assertTrue(url.contains("waypoints=25.02,55.02%7C25.04,55.04"))
        assertTrue(url.contains("travelmode=driving"))
        assertFalse(url.contains("/maps/dir/25."))
    }

    @Test
    fun googleMapsDirectionsUrl_withoutWaypointsUsesQueryParams() {
        val origin = LatLng(25.0, 55.0)
        val destination = LatLng(25.1, 55.1)

        val url = googleMapsDirectionsUrl(origin, destination)

        assertTrue(url.startsWith("https://www.google.com/maps/dir/?api=1"))
        assertTrue(url.contains("origin=25.0,55.0"))
        assertTrue(url.contains("destination=25.1,55.1"))
        assertFalse(url.contains("waypoints="))
        assertFalse(url.contains("via:"))
        assertFalse(url.contains("%2C"))
    }

    @Test
    fun sampleNavigationWaypoints_picksPointsAlongPath() {
        val origin = LatLng(25.0000, 55.0000)
        val destination = LatLng(25.0100, 55.0100)
        val path =
            (0..20).map { step ->
                LatLng(25.0000 + step * 0.0004, 55.0000 + step * 0.0004)
            }

        val waypoints = sampleNavigationWaypoints(path, origin, destination, maxWaypoints = 4)

        assertTrue(waypoints.isNotEmpty())
        assertTrue(waypoints.size <= 4)
        waypoints.forEach { waypoint ->
            assertTrue(path.contains(waypoint))
        }
    }

    @Test
    fun sampleNavigationWaypoints_emptyForShortPath() {
        val origin = LatLng(25.0, 55.0)
        val destination = LatLng(25.0001, 55.0001)
        val path = listOf(origin, destination)

        assertEquals(emptyList<LatLng>(), sampleNavigationWaypoints(path, origin, destination))
    }
}
