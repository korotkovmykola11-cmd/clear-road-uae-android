package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationHandoffTest {

    @Test
    fun googleMapsDirectionsUrl_originDestinationWithNavigateAction() {
        val origin = LatLng(25.4051615, 55.5135881)
        val destination = LatLng(25.2138142, 55.2820336)

        val url = googleMapsDirectionsUrl(origin, destination)

        assertEquals(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=25.4051615,55.5135881" +
                "&destination=25.2138142,55.2820336" +
                "&travelmode=driving" +
                "&dir_action=navigate",
            url,
        )
        assertFalse(url.contains("waypoints="))
        assertFalse(url.contains("via:"))
    }

    @Test
    fun wazeNavigateUrl_destinationOnlyWithNavigateYes() {
        val destination = LatLng(25.2138142, 55.2820336)

        val url = wazeNavigateUrl(destination)

        assertEquals(
            "https://waze.com/ul?ll=25.2138142,55.2820336&navigate=yes",
            url,
        )
        assertFalse(url.contains("waypoint"))
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
