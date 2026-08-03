package com.clearroad.app

import com.clearroad.app.benchmark.RoutesV2ResponseAdapterTestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesV2ManeuverPolicyTest {

    @Test
    fun difcMarinaFixture_smoothWinnerNotFalseOverriddenByPolicyB() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-difc-marina-traffic.json"),
            )
        val calmIdx =
            com.clearroad.app.RouteRecommendationSelection.pickRecommendedRouteIndex(
                routes,
                com.clearroad.app.domain.PreferenceMode.CALM,
            )
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        val smoothWin = com.clearroad.app.domain.SmoothDriveScoring.pickWinnerIndex(inputs)
        if (smoothWin != 0) {
            assertTrue(
                "Expected CALM to respect SmoothDrive non-default winner (difc-marina false override class)",
                calmIdx == smoothWin,
            )
        }
    }

    @Test
    fun uturnShortLoopOnHighway_notCriticalWithoutRoundaboutText() {
        val step =
            DirectionsStepRecord(
                distanceMeters = 100,
                maneuver = "UTURN_LEFT",
                htmlInstructions = "Continue on Sheikh Zayed Rd/E11",
                startLocation = null,
            )
        assertFalse(RoutesV2ManeuverPolicy.isCritical(step))
    }

    @Test
    fun uturnShortLoopOffHighway_criticalOnlyInFullAuditNotPolicyB() {
        val step =
            DirectionsStepRecord(
                distanceMeters = 80,
                maneuver = "UTURN_LEFT",
                htmlInstructions = "At the local junction",
                startLocation = null,
            )
        assertTrue(RoutesV2ManeuverPolicy.isCritical(step))
        assertFalse(RoutesV2ManeuverPolicy.isPolicyBStress(step))
    }
}
