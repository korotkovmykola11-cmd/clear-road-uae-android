package com.clearroad.app

import com.clearroad.app.domain.SmoothDriveScoring
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteComplexityAuditTest {

    @Test
    fun directionsDebugFixture_threeRoutes_haveManeuverCounts() {
        val fixture = File("../directions_debug.json")
        if (!fixture.exists()) return

        val raw = fixture.readText()
        val routes = extractRouteLegsDebugData(raw)
        val routeJsonObjects = extractAllRouteObjectJson(raw)
        assertTrue("Expected at least 3 routes", routes.size >= 3)
        assertTrue("Expected route JSON objects", routeJsonObjects.size >= 3)

        val snapshots = RouteComplexityAudit.buildSnapshots(routes, routeJsonObjects)
        val compare = RouteComplexityAudit.buildCompareSnapshot(snapshots, routes)

        snapshots.forEach { snapshot ->
            assertTrue(snapshot.durationSeconds > 0)
            assertTrue(snapshot.distanceMeters > 0)
            assertTrue(snapshot.polylinePathMeters > 0)
            assertTrue(snapshot.turnCount > 0)
            System.err.println(
                "ROUTE[${snapshot.routeIndex}] duration=${snapshot.durationSeconds} " +
                    "turns=${snapshot.turnCount} left=${snapshot.leftTurnCount} " +
                    "polylinePathMeters=${snapshot.polylinePathMeters} " +
                    "corridor=${snapshot.corridorClassification}",
            )
        }

        assertTrue(compare != null)
        System.err.println(
            "COMPLEXITY_COMPARE durationSpread=${compare!!.durationSpreadSeconds} " +
                "turnSpread=${compare.turnCountSpread} " +
                "complexityDiffersMoreThanTime=${compare.complexityDiffersMoreThanTime}",
        )
    }

    @Test
    fun metricsFromManeuvers_countsLeftRoundaboutAndUturn() {
        val metrics =
            RouteComplexityAudit.metricsFromManeuvers(
                maneuvers =
                    listOf(
                        "turn-left",
                        "turn-right",
                        "roundabout-right",
                        "uturn-left",
                        "merge",
                        "straight",
                        "ramp-right",
                    ),
                routeIndex = 0,
            )

        assertTrue(metrics.turnCount >= 4)
        assertTrue(metrics.leftTurnCount >= 2)
        assertTrue(metrics.roundaboutCount == 1)
        assertTrue(metrics.uTurnCount == 1)
        assertTrue(metrics.mergeCount == 1)
        assertTrue(metrics.rampCount == 1)
    }
}
