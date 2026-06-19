package com.clearroad.app.replay

/**
 * Stage 39A offline policies — not production selection.
 */
internal enum class CalmReplayPolicy {
    /** Observed field CALM winner (`currentWinnerIndex` from frozen logcat). */
    CURRENT_CALM,

    /**
     * Stress tie-break (spec guards):
     * timePenalty ≤ 5, stressGap ≥ 20%, and blind-spot class
     * (timePenalty == 0 OR winner not strictly faster in traffic).
     */
    STRESS_TIE_BREAK,

    /**
     * Strict stress tie-break:
     * timePenalty == 0 and stressGap ≥ 35% only.
     */
    STRICT_STRESS_TIE_BREAK,
}

internal object CalmReplayPolicyEngine {

    fun pickWinnerIndex(
        set: FieldRouteSet,
        policy: CalmReplayPolicy,
    ): Int {
        val routes = set.routes
        require(routes.isNotEmpty())
        val calmWinner = set.fieldCalmWinnerIndex.coerceIn(0, routes.lastIndex)
        if (policy == CalmReplayPolicy.CURRENT_CALM) return calmWinner

        val bestStressIndex = lowestCriticalIndex(routes)
        val audit = ReplayStressAudit.forWinner(routes, calmWinner)

        val override =
            when (policy) {
                CalmReplayPolicy.STRESS_TIE_BREAK ->
                    audit.timePenaltyMin <= 5 &&
                        audit.stressGapPct >= 20 &&
                        (
                            audit.timePenaltyMin == 0 ||
                                !isStrictlyFasterInTraffic(routes, calmWinner, bestStressIndex)
                            )
                CalmReplayPolicy.STRICT_STRESS_TIE_BREAK ->
                    audit.timePenaltyMin == 0 && audit.stressGapPct >= 35
                CalmReplayPolicy.CURRENT_CALM -> false
            }

        return if (override) bestStressIndex else calmWinner
    }

    private fun lowestCriticalIndex(routes: List<FieldRouteReplayRoute>): Int =
        routes.indices.minWith(
            compareBy<Int> { routes[it].criticalManeuversCount }.thenBy { it },
        )

    private fun isStrictlyFasterInTraffic(
        routes: List<FieldRouteReplayRoute>,
        winnerIndex: Int,
        otherIndex: Int,
    ): Boolean =
        routes[winnerIndex].durationInTrafficMin < routes[otherIndex].durationInTrafficMin
}

internal data class ReplayStressAudit(
    val calmWinnerIndex: Int,
    val bestStressRouteIndex: Int,
    val stressGapPct: Int,
    val timePenaltyMin: Int,
    val calmPickedLowestStress: Boolean,
    val winnerSlowerAndMoreStressful: Boolean,
) {
    companion object {
        fun forWinner(
            routes: List<FieldRouteReplayRoute>,
            calmWinnerIndex: Int,
        ): ReplayStressAudit {
            val winnerIdx = calmWinnerIndex.coerceIn(0, routes.lastIndex)
            val bestStressIdx =
                routes.indices.minWith(
                    compareBy<Int> { routes[it].criticalManeuversCount }.thenBy { it },
                )
            val winner = routes[winnerIdx]
            val bestStress = routes[bestStressIdx]
            val stressGapPct =
                if (winner.criticalManeuversCount <= 0) {
                    0
                } else {
                    (
                        (winner.criticalManeuversCount - bestStress.criticalManeuversCount)
                            .coerceAtLeast(0) *
                            100.0 / winner.criticalManeuversCount
                        ).toInt()
                }
            val timePenaltyMin =
                (bestStress.durationInTrafficMin - winner.durationInTrafficMin)
                    .coerceAtLeast(0)
            val winnerSlowerAndMoreStressful =
                winner.durationInTrafficMin > bestStress.durationInTrafficMin &&
                    winner.criticalManeuversCount > bestStress.criticalManeuversCount
            return ReplayStressAudit(
                calmWinnerIndex = winnerIdx,
                bestStressRouteIndex = bestStressIdx,
                stressGapPct = stressGapPct,
                timePenaltyMin = timePenaltyMin,
                calmPickedLowestStress = winnerIdx == bestStressIdx,
                winnerSlowerAndMoreStressful = winnerSlowerAndMoreStressful,
            )
        }
    }
}
