package com.clearroad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothDriveScoringTest {

    @Test
    fun peakScenario_difcToMarina_picksIdx2() {
        val routes = Stage352PeakFixtures.difcToMarina()
        assertEquals(2, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun peakScenario_ajmanToDifc_picksIdx1() {
        val routes = Stage352PeakFixtures.ajmanToDifc()
        assertEquals(1, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun peakScenario_sharjahToDowntown_picksIdx2() {
        val routes = Stage352PeakFixtures.sharjahToDowntown()
        assertEquals(2, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun peakScenario_jvcToAbuDhabi_picksIdx2() {
        val routes = Stage352PeakFixtures.jvcToAbuDhabi()
        assertEquals(2, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun tollDoesNotAffectSmoothScore() {
        val route =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 2612,
                durationInTrafficSeconds = 2845,
                distanceMeters = 46_300,
                corridorText = "Sheikh Mohammed Bin Zayed Rd/E311",
            )
        val scoreWithoutTollMention =
            SmoothDriveScoring.scoreRoute(
                route,
                fastestTrafficSeconds = 2845,
                minDistanceMeters = 46_300,
            )
        val scoreWithTollMentionInCorridorText =
            SmoothDriveScoring.scoreRoute(
                route.copy(
                    corridorText =
                        "Sheikh Mohammed Bin Zayed Rd/E311 Toll road Toll road Toll road",
                ),
                fastestTrafficSeconds = 2845,
                minDistanceMeters = 46_300,
            )
        assertEquals(scoreWithoutTollMention.total, scoreWithTollMentionInCorridorText.total, 0.001)
        assertEquals(
            scoreWithoutTollMention.delayRatioComponent,
            scoreWithTollMentionInCorridorText.delayRatioComponent,
            0.001,
        )
    }

    @Test
    fun lowerDelayRatio_beatsCheaperCorridorChoice() {
        val fastButVolatile =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 2474,
                durationInTrafficSeconds = 2908,
                distanceMeters = 37_600,
                corridorText = "E11 Sheikh Zayed Toll road",
            )
        val slightlySlowerButSteadier =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 2612,
                durationInTrafficSeconds = 2845,
                distanceMeters = 46_300,
                corridorText = "Sheikh Mohammed Bin Zayed Rd/E311",
            )
        val routes = listOf(fastButVolatile, slightlySlowerButSteadier)
        val scores = SmoothDriveScoring.scoreAll(routes)
        assertTrue(scores[1].delayRatio < scores[0].delayRatio)
        assertEquals(1, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun timeBudget_preventsExtremelyLongDetour() {
        val steadyShort =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 3600,
                durationInTrafficSeconds = 4200,
                distanceMeters = 100_000,
                corridorText = "E11",
            )
        val lowRatioButVeryLong =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 7200,
                durationInTrafficSeconds = 7400,
                distanceMeters = 100_000,
                corridorText = "E11",
            )
        val routes = listOf(steadyShort, lowRatioButVeryLong)
        val scores = SmoothDriveScoring.scoreAll(routes)
        assertTrue(scores[1].delayRatio < scores[0].delayRatio)
        assertTrue(scores[1].timePenaltyComponent > scores[0].timePenaltyComponent)
        assertEquals(0, SmoothDriveScoring.pickWinnerIndex(routes))
    }

    @Test
    fun equalDelayRatios_corridorAndDistanceActAsTieBreakersOnly() {
        val motorway =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 2000,
                durationInTrafficSeconds = 2200,
                distanceMeters = 30_000,
                corridorText = "Sheikh Zayed Rd/E11",
            )
        val mixedFarther =
            SmoothDriveScoring.RouteInput(
                baseDurationSeconds = 2000,
                durationInTrafficSeconds = 2200,
                distanceMeters = 32_000,
                corridorText = "local street Marina",
            )
        val scores = SmoothDriveScoring.scoreAll(listOf(motorway, mixedFarther))
        assertEquals(scores[0].delayRatioComponent, scores[1].delayRatioComponent, 0.001)
        assertEquals(scores[0].delayMinComponent, scores[1].delayMinComponent, 0.001)
        assertEquals(scores[0].timePenaltyComponent, scores[1].timePenaltyComponent, 0.001)
        assertNotEquals(scores[0].corridorComponent, scores[1].corridorComponent)
        assertTrue(scores[0].total < scores[1].total)
        assertEquals(0, SmoothDriveScoring.pickWinnerIndex(listOf(motorway, mixedFarther)))
    }
    @Test
    fun inactiveGuard_testA_fastestBeatsMotorwayWhenAllDelayZero() {
        val fastest =
            route(
                baseSec = 30 * 60,
                trafficSec = 30 * 60,
                distanceM = 30_000,
                corridor = "local mixed route",
            )
        val motorwayThreeMinutesSlower =
            route(
                baseSec = 33 * 60,
                trafficSec = 33 * 60,
                distanceM = 30_000,
                corridor = "Sheikh Zayed Rd/E11",
            )
        val routes = listOf(fastest, motorwayThreeMinutesSlower)
        val scores = SmoothDriveScoring.scoreAll(routes)

        assertTrue(SmoothDriveScoring.isDelaySignalInactive(routes))
        assertTrue(scores.all { it.delaySignalInactiveGuardApplied })
        assertEquals(0, SmoothDriveScoring.pickWinnerIndex(routes))
        assertTrue(scores[0].total < scores[1].total)
    }

    @Test
    fun inactiveGuard_testB_shorterDistanceWinsWhenSameTrafficTime() {
        val shorter =
            route(
                baseSec = 30 * 60,
                trafficSec = 30 * 60,
                distanceM = 28_000,
                corridor = "local mixed route",
            )
        val longer =
            route(
                baseSec = 30 * 60,
                trafficSec = 30 * 60,
                distanceM = 32_000,
                corridor = "local mixed route",
            )
        val routes = listOf(shorter, longer)
        val scores = SmoothDriveScoring.scoreAll(routes)

        assertTrue(scores.all { it.delaySignalInactiveGuardApplied })
        assertEquals(0, SmoothDriveScoring.pickWinnerIndex(routes))
        assertTrue(scores[0].distanceComponent < scores[1].distanceComponent)
    }

    @Test
    fun inactiveGuard_testC_peakFixturesKeepStage351Behavior() {
        val scenarios =
            listOf(
                Stage352PeakFixtures.difcToMarina() to 2,
                Stage352PeakFixtures.ajmanToDifc() to 1,
                Stage352PeakFixtures.sharjahToDowntown() to 2,
                Stage352PeakFixtures.jvcToAbuDhabi() to 2,
            )
        scenarios.forEach { (routes, expectedWinner) ->
            assertTrue(!SmoothDriveScoring.isDelaySignalInactive(routes))
            val scores = SmoothDriveScoring.scoreAll(routes)
            assertTrue(scores.none { it.delaySignalInactiveGuardApplied })
            assertEquals(expectedWinner, SmoothDriveScoring.pickWinnerIndex(routes))
        }
    }

    @Test
    fun inactiveGuard_testD_ajmanLikeZeroDelayDoesNotPickSlowestMotorwayCorridor() {
        val r0 =
            route(
                baseSec = 36 * 60,
                trafficSec = 36 * 60,
                distanceM = 45_600,
                corridor = "E11",
            )
        val r1 =
            route(
                baseSec = 38 * 60,
                trafficSec = 38 * 60,
                distanceM = 43_700,
                corridor = "local street Marina",
            )
        val r2 =
            route(
                baseSec = 39 * 60,
                trafficSec = 39 * 60,
                distanceM = 40_900,
                corridor = "Sheikh Mohammed Bin Zayed Rd/E311",
            )
        val routes = listOf(r0, r1, r2)

        assertTrue(SmoothDriveScoring.isDelaySignalInactive(routes))
        assertEquals(0, SmoothDriveScoring.pickWinnerIndex(routes))

        val scores = SmoothDriveScoring.scoreAll(routes)
        val winnerScore = scores[0].total
        assertTrue(scores[1].total > winnerScore)
        assertTrue(scores[2].total > winnerScore)
    }

    private fun route(
        baseSec: Int,
        trafficSec: Int,
        distanceM: Int,
        corridor: String,
    ): SmoothDriveScoring.RouteInput =
        SmoothDriveScoring.RouteInput(
            baseDurationSeconds = baseSec,
            durationInTrafficSeconds = trafficSec,
            distanceMeters = distanceM,
            corridorText = corridor,
        )
}

/** Peak-traffic metrics from Stage 35.0 / 35.1 audit (`docs/stage-35-0-audit/`). */
private object Stage352PeakFixtures {

    fun difcToMarina(): List<SmoothDriveScoring.RouteInput> =
        listOf(
            route(
                baseSec = 1458,
                trafficSec = 1663,
                distanceM = 24_300,
                corridor = "Sheikh Zayed Rd/E11",
            ),
            route(
                baseSec = 1509,
                trafficSec = 1664,
                distanceM = 26_800,
                corridor = "Sheikh Zayed Rd/E11 and King Salman Bin Abdulaziz Al Saud St/D94",
            ),
            route(
                baseSec = 1710,
                trafficSec = 1771,
                distanceM = 24_700,
                corridor = "Sheikh Zayed Rd/E11, Al Naseem St/D61 and King Salman Bin Abdulaziz Al Saud St/D94",
            ),
        )

    fun ajmanToDifc(): List<SmoothDriveScoring.RouteInput> =
        listOf(
            route(
                baseSec = 2474,
                trafficSec = 2908,
                distanceM = 37_600,
                corridor = "E11",
            ),
            route(
                baseSec = 2612,
                trafficSec = 2845,
                distanceM = 46_300,
                corridor = "Sheikh Mohammed Bin Zayed Rd/E311",
            ),
            route(
                baseSec = 2717,
                trafficSec = 2963,
                distanceM = 48_200,
                corridor = "Sheikh Mohammed Bin Zayed Rd/E311 and Ras Al Khor Rd/E44",
            ),
        )

    fun sharjahToDowntown(): List<SmoothDriveScoring.RouteInput> =
        listOf(
            route(
                baseSec = 1974,
                trafficSec = 2044,
                distanceM = 29_800,
                corridor = "E11",
            ),
            route(
                baseSec = 1956,
                trafficSec = 2155,
                distanceM = 27_700,
                corridor = "S120 and E11",
            ),
            route(
                baseSec = 2047,
                trafficSec = 2104,
                distanceM = 33_100,
                corridor = "Sheikh Mohammed Bin Zayed Rd/E311",
            ),
        )

    fun jvcToAbuDhabi(): List<SmoothDriveScoring.RouteInput> =
        listOf(
            route(
                baseSec = 4228,
                trafficSec = 4534,
                distanceM = 102_700,
                corridor = "E11",
            ),
            route(
                baseSec = 4353,
                trafficSec = 4594,
                distanceM = 106_900,
                corridor = "E11 and Al Shahama - Abu Dhabi Rd/Sheikh Zayed Bin Sultan St/E10",
            ),
            route(
                baseSec = 4812,
                trafficSec = 4981,
                distanceM = 116_800,
                corridor = "Expo Rd/E77 and E11",
            ),
        )

    private fun route(
        baseSec: Int,
        trafficSec: Int,
        distanceM: Int,
        corridor: String,
    ): SmoothDriveScoring.RouteInput =
        SmoothDriveScoring.RouteInput(
            baseDurationSeconds = baseSec,
            durationInTrafficSeconds = trafficSec,
            distanceMeters = distanceM,
            corridorText = corridor,
        )
}
