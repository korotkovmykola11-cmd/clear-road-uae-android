package com.clearroad.app.guidance

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarshioGuidanceSimulationTest {

    @Test
    fun stateAtProgress_reachesDestinationAtEnd() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.01),
                LatLng(25.02, 55.02),
            )

        val state =
            MarshioGuidanceSimulation.stateAtProgress(
                path = path,
                totalDurationSeconds = 600,
                progressFraction = 1f,
            )

        assertEquals(path.last(), state.position)
        assertEquals(0, state.remainingDurationSeconds)
        assertTrue(state.remainingDistanceMeters < 1.0)
        assertEquals(false, state.offRoute)
    }

    @Test
    fun estimateRemainingSeconds_scalesWithDistance() {
        val remaining =
            MarshioGuidanceSimulation.estimateRemainingSeconds(
                totalDurationSeconds = 600,
                totalMeters = 10_000.0,
                remainingMeters = 5_000.0,
            )

        assertEquals(300, remaining)
    }

    @Test
    fun stateAtPosition_onRoute_updatesRemainingDistance() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
                LatLng(25.02, 55.0),
            )
        val position = LatLng(25.005, 55.0)

        val state =
            MarshioGuidanceSimulation.stateAtPosition(
                position = position,
                path = path,
                totalDurationSeconds = 600,
            )

        assertEquals(false, state.offRoute)
        assertTrue(state.remainingDistanceMeters > 0.0)
        assertTrue(state.traveledMeters > 0.0)
        assertTrue(state.remainingDurationSeconds in 1..600)
    }

    @Test
    fun stateAtPosition_farFromRoute_marksOffRoute() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
            )
        val offPosition = LatLng(25.005, 55.01)

        val state =
            MarshioGuidanceSimulation.stateAtPosition(
                position = offPosition,
                path = path,
                totalDurationSeconds = 600,
            )

        assertEquals(true, state.offRoute)
        assertEquals("Off-route", state.statusLabel)
        assertEquals(offPosition, state.position)
    }

    @Test
    fun isOffRoute_usesPolylineDistanceNotJustVertices() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
            )
        val nearMidpoint = LatLng(25.005, 55.0)

        assertEquals(false, MarshioGuidanceSimulation.isOffRoute(nearMidpoint, path))
    }

    @Test
    fun projectOntoPolyline_midSegment_hasHalfTraveledDistance() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.02, 55.0),
            )
        val midpoint = LatLng(25.01, 55.0)
        val total = MarshioGuidanceSimulation.polylineLengthMeters(path)
        val projection = MarshioGuidanceSimulation.projectOntoPolyline(midpoint, path)

        assertTrue(projection.distanceToRouteMeters < 5.0)
        assertEquals(total / 2.0, projection.traveledMeters, total * 0.15)
    }
}
