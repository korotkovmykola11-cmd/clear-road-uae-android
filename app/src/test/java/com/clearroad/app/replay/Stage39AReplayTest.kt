package com.clearroad.app.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 39A — offline replay of 47 frozen field CALM route sets.
 * Test-only; no production scoring changes.
 */
class Stage39AReplayTest {

    private val baseline = Stage39AFieldBaseline.all

    @Test
    fun frozenBaseline_has47DedupSets() {
        assertEquals(47, baseline.size)
        assertEquals(14, baseline.count { it.bucket == FieldBucket.PREV_PM })
        assertEquals(17, baseline.count { it.bucket == FieldBucket.AM_RUSH })
        assertEquals(16, baseline.count { it.bucket == FieldBucket.PM_RUSH })
    }

    @Test
    fun frozenBaseline_matchesStage383AggregateMetrics() {
        val rows = Stage39AReplayEngine.replayAll(baseline)
        val current = rows.first { it.policy == CalmReplayPolicy.CURRENT_CALM }
        assertEquals(40, current.calmMatchPct)
        assertEquals(14, current.meaningfulOpportunityDenominator)
        assertEquals(0, current.meaningfulOpportunitiesCaptured)
        assertTrue(current.dualBlindSpotsUnderPolicyA >= 2)
    }

    @Test
    fun anchorCase_18_15_09_isDualBlindSpotUnderPolicyA() {
        val set = baseline.first { it.id == "2026-06-19T18:15:09" }
        assertEquals(2, set.fieldCalmWinnerIndex)
        val audit =
            ReplayStressAudit.forWinner(set.routes, set.fieldCalmWinnerIndex)
        assertTrue(audit.winnerSlowerAndMoreStressful)
        assertEquals(40, audit.stressGapPct)
        assertEquals(0, audit.timePenaltyMin)
    }

    @Test
    fun anchorCase_18_17_35_isDualBlindSpotUnderPolicyA() {
        val set = baseline.first { it.id == "2026-06-19T18:17:35" }
        assertEquals(1, set.fieldCalmWinnerIndex)
        val audit =
            ReplayStressAudit.forWinner(set.routes, set.fieldCalmWinnerIndex)
        assertTrue(audit.winnerSlowerAndMoreStressful)
        assertEquals(44, audit.stressGapPct)
    }

    @Test
    fun replayAll_policiesProduceComparisonTable() {
        val rows = Stage39AReplayEngine.replayAll(baseline)
        assertEquals(3, rows.size)

        val table = Stage39AReplayEngine.formatMarkdownTable(rows)
        assertTrue(table.contains("PM blind spots fixed"))
        assertTrue(table.contains("Dual blind spots (slower + stressier) under A"))

        println("\n=== Stage 39A Replay Results ===\n$table")
    }
}
