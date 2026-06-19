package com.clearroad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmStressTieBreakTest {

    private fun r(durationInTrafficMin: Int, critical: Int): CalmStressTieBreak.RouteStressInput =
        CalmStressTieBreak.RouteStressInput(
            durationInTrafficMin = durationInTrafficMin,
            criticalManeuversCount = critical,
        )

    @Test
    fun anchor_18_15_09_overridesToBestStress() {
        val routes =
            listOf(
                r(71, 15),
                r(80, 12),
                r(82, 20),
            )
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 2, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(1, decision.selectedIndex)
        assertEquals(40, decision.stressGapPct)
        assertEquals(0, decision.timePenaltyMin)
    }

    @Test
    fun anchor_18_17_35_overridesToBestStress() {
        val routes =
            listOf(
                r(16, 5),
                r(18, 9),
            )
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 1, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(0, decision.selectedIndex)
        assertEquals(44, decision.stressGapPct)
        assertEquals(0, decision.timePenaltyMin)
    }

    @Test
    fun alreadyLowestStress_noOverride() {
        val routes = listOf(r(20, 5), r(22, 8))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertFalse(decision.overrideApplied)
        assertEquals(0, decision.selectedIndex)
    }

    @Test
    fun guard_stressGap19_noOverride() {
        val routes = listOf(r(20, 21), r(22, 17))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertFalse(decision.overrideApplied)
        assertEquals(19, decision.stressGapPct)
    }

    @Test
    fun guard_stressGap20_overridesWhenOtherGuardsPass() {
        val routes = listOf(r(82, 20), r(80, 16))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(20, decision.stressGapPct)
        assertEquals(0, decision.timePenaltyMin)
        assertEquals(1, decision.selectedIndex)
    }

    @Test
    fun guard_timePenaltyPositive_noOverrideWhenWinnerStrictlyFaster() {
        val routes = listOf(r(20, 20), r(25, 10))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertFalse(decision.overrideApplied)
        assertEquals(5, decision.timePenaltyMin)
    }

    @Test
    fun guard_timePenalty6_noOverride() {
        val routes = listOf(r(20, 20), r(26, 10))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertFalse(decision.overrideApplied)
        assertEquals(6, decision.timePenaltyMin)
    }

    @Test
    fun guardG4_winnerStrictlyFasterInTraffic_noOverrideWhenTimePenaltyPositive() {
        val routes = listOf(r(18, 20), r(22, 10))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertFalse(decision.overrideApplied)
        assertEquals(4, decision.timePenaltyMin)
    }

    @Test
    fun guardG4_timePenaltyZero_allowsOverrideEvenWhenStressRouteFaster() {
        val routes = listOf(r(82, 20), r(80, 12))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(0, decision.timePenaltyMin)
        assertEquals(1, decision.selectedIndex)
    }

    @Test
    fun guardG4_winnerNotStrictlyFaster_allowsOverrideWithPositiveTimePenalty() {
        val routes = listOf(r(22, 20), r(22, 10))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 0, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(0, decision.timePenaltyMin)
    }

    @Test
    fun tieOnCriticalCount_picksLowerIndex() {
        val routes = listOf(r(20, 10), r(21, 10), r(22, 20))
        val decision = CalmStressTieBreak.applyPolicyB(smoothDriveWinnerIndex = 2, routes)

        assertTrue(decision.overrideApplied)
        assertEquals(0, decision.selectedIndex)
    }
}
