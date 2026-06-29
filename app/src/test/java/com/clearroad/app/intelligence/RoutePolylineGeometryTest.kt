package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePolylineGeometryTest {

    @Test
    fun boundingBoxExpanded_expandsBeyondRoutePoints() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.01),
            )
        val bbox = RoutePolylineGeometry.boundingBoxExpanded(path, expandMeters = 100.0)

        assertNotNull(bbox)
        assertTrue(bbox!!.south < path.minOf { it.latitude })
        assertTrue(bbox.north > path.maxOf { it.latitude })
        assertTrue(bbox.west < path.minOf { it.longitude })
        assertTrue(bbox.east > path.maxOf { it.longitude })
    }

    @Test
    fun minDistanceToPolyline_midSegment_isNearZero() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.02, 55.0),
            )
        val midpoint = LatLng(25.01, 55.0)

        val distance = RoutePolylineGeometry.minDistanceToPolylineMeters(midpoint, path)

        assertTrue(distance < 5.0)
    }

    @Test
    fun minDistanceToPolyline_parallelRoad_isFartherThanThreshold() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.02, 55.0),
            )
        val parallel = LatLng(25.01, 55.01)

        val distance = RoutePolylineGeometry.minDistanceToPolylineMeters(parallel, path)

        assertTrue(distance > RoutePolylineGeometry.NEAR_ROUTE_METERS)
    }

    @Test
    fun stablePolylineHash_isStableForSameRoute() {
        val path =
            listOf(
                LatLng(25.123456, 55.654321),
                LatLng(25.223456, 55.754321),
            )

        val first = RoutePolylineGeometry.stablePolylineHash(path)
        val second = RoutePolylineGeometry.stablePolylineHash(path)

        assertEquals(first, second)
        assertFalse(first.isBlank())
    }
}
