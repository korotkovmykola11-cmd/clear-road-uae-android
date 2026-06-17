package com.clearroad.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DriverStressAuditTest {

    private fun route(
        durationSeconds: Int,
        distanceMeters: Int = 10_000,
        durationInTrafficSeconds: Int? = durationSeconds,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceMeters / 1000} km",
            durationText = "${durationSeconds / 60} mins",
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = 0,
            hasToll = false,
            durationInTrafficSeconds = durationInTrafficSeconds,
        )

    @Test
    fun isCriticalManeuver_matchesGoogleManeuverTokens() {
        assertTrue(DriverStressAudit.isCriticalManeuver("merge"))
        assertTrue(DriverStressAudit.isCriticalManeuver("fork-left"))
        assertTrue(DriverStressAudit.isCriticalManeuver("ramp-right"))
        assertTrue(DriverStressAudit.isCriticalManeuver("keep-left"))
        assertTrue(DriverStressAudit.isCriticalManeuver("turn-slight-left"))
        assertTrue(DriverStressAudit.isCriticalManeuver("turn-sharp-right"))
        assertTrue(DriverStressAudit.isCriticalManeuver("roundabout-left"))
        assertTrue(DriverStressAudit.isCriticalManeuver("straight").not())
    }

    @Test
    fun metricsFromSteps_calculatesDensityAndPerKm() {
        val steps =
            listOf(
                DirectionsStepRecord(distanceMeters = 500, maneuver = "merge"),
                DirectionsStepRecord(distanceMeters = 500, maneuver = "straight"),
                DirectionsStepRecord(distanceMeters = 500, maneuver = "ramp-right"),
                DirectionsStepRecord(distanceMeters = 500, maneuver = "fork-left"),
            )
        val metrics =
            DriverStressAudit.metricsFromSteps(
                steps = steps,
                route = route(durationSeconds = 20 * 60, distanceMeters = 2_000),
                routeIndex = 0,
                isCurrentWinner = true,
            )

        assertEquals(4, metrics.totalSteps)
        assertEquals(2.0, metrics.stepsPerKm, 0.01)
        assertEquals(3, metrics.criticalManeuversCount)
        assertEquals(1.5, metrics.criticalManeuversPerKm, 0.01)
        assertEquals(3, metrics.maxDensity2KmSegment)
        assertEquals(1, metrics.keywordDistribution["merge"])
        assertEquals(1, metrics.keywordDistribution["ramp"])
        assertEquals(1, metrics.keywordDistribution["fork"])
    }

    @Test
    fun maxCriticalDensityWindow_countsWithinTwoKmWindow() {
        val steps =
            listOf(
                DirectionsStepRecord(distanceMeters = 800, maneuver = "merge"),
                DirectionsStepRecord(distanceMeters = 800, maneuver = "merge"),
                DirectionsStepRecord(distanceMeters = 800, maneuver = "straight"),
                DirectionsStepRecord(distanceMeters = 800, maneuver = "ramp-right"),
            )

        assertEquals(
            2,
            DriverStressAudit.maxCriticalDensityWindow(
                steps,
                DriverStressAudit.DENSITY_WINDOW_METERS,
            ),
        )
    }

    @Test
    fun buildSessionSummary_reportsWinnerVersusLowestStress() {
        val metrics =
            listOf(
                DriverStressAudit.metricsFromSteps(
                    steps =
                        listOf(
                            DirectionsStepRecord(1000, "merge"),
                            DirectionsStepRecord(1000, "fork-left"),
                            DirectionsStepRecord(1000, "ramp-right"),
                        ),
                    route = route(durationSeconds = 20 * 60, distanceMeters = 3_000),
                    routeIndex = 0,
                    isCurrentWinner = true,
                ),
                DriverStressAudit.metricsFromSteps(
                    steps =
                        listOf(
                            DirectionsStepRecord(1000, "straight"),
                            DirectionsStepRecord(1000, "straight"),
                        ),
                    route = route(durationSeconds = 23 * 60, distanceMeters = 3_000),
                    routeIndex = 1,
                    isCurrentWinner = false,
                ),
            )

        val summary = DriverStressAudit.buildSessionSummary(metrics, currentWinnerIndex = 0)!!

        assertEquals(0, summary.currentWinnerIndex)
        assertEquals(1, summary.lowestStepsPerKmIndex)
        assertEquals(1, summary.lowestCriticalManeuverIndex)
        assertEquals(3, summary.timeLostMinIfLowestStressChosen)
        assertEquals(100, summary.maneuversSavedPctIfLowestStressChosen)
    }

    @Test
    fun extractStepRecordsFromRouteJson_readsFixtureRouteSteps() {
        val fixture = File("../directions_debug.json")
        if (!fixture.exists()) return

        val raw = fixture.readText()
        val routeJsonObjects = extractAllRouteObjectJson(raw)
        assertTrue(routeJsonObjects.isNotEmpty())

        val steps = extractStepRecordsFromRouteJson(routeJsonObjects.first())
        assertTrue(steps.isNotEmpty())
        assertTrue(steps.any { it.maneuver != null })
    }
}
