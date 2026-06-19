package com.clearroad.app.domain

import kotlin.math.roundToInt

/**
 * Stage 39 — Policy B (Stress Tie-Break) for CALM selection.
 * Applied after [SmoothDriveScoring.pickWinnerIndex]; does not alter SmoothDrive weights.
 */
object CalmStressTieBreak {

    const val LOG_TAG = "CalmStressTieBreak"

    data class RouteStressInput(
        val durationInTrafficMin: Int,
        val criticalManeuversCount: Int,
    )

    data class Decision(
        val selectedIndex: Int,
        val smoothDriveWinnerIndex: Int,
        val overrideApplied: Boolean,
        val bestStressRouteIndex: Int,
        val stressGapPct: Int,
        val timePenaltyMin: Int,
    )

    fun applyPolicyB(
        smoothDriveWinnerIndex: Int,
        routes: List<RouteStressInput>,
    ): Decision {
        require(routes.isNotEmpty())
        val calmWinner = smoothDriveWinnerIndex.coerceIn(0, routes.lastIndex)
        val bestStressIndex = lowestCriticalIndex(routes)
        val audit = auditForWinner(routes, calmWinner, bestStressIndex)
        if (bestStressIndex == calmWinner) {
            return Decision(
                selectedIndex = calmWinner,
                smoothDriveWinnerIndex = calmWinner,
                overrideApplied = false,
                bestStressRouteIndex = bestStressIndex,
                stressGapPct = audit.stressGapPct,
                timePenaltyMin = audit.timePenaltyMin,
            )
        }
        val override =
            shouldOverridePolicyB(
                routes = routes,
                calmWinnerIndex = calmWinner,
                bestStressIndex = bestStressIndex,
                audit = audit,
            )
        return if (override) {
            Decision(
                selectedIndex = bestStressIndex,
                smoothDriveWinnerIndex = calmWinner,
                overrideApplied = true,
                bestStressRouteIndex = bestStressIndex,
                stressGapPct = audit.stressGapPct,
                timePenaltyMin = audit.timePenaltyMin,
            )
        } else {
            Decision(
                selectedIndex = calmWinner,
                smoothDriveWinnerIndex = calmWinner,
                overrideApplied = false,
                bestStressRouteIndex = bestStressIndex,
                stressGapPct = audit.stressGapPct,
                timePenaltyMin = audit.timePenaltyMin,
            )
        }
    }

    internal data class Audit(
        val stressGapPct: Int,
        val timePenaltyMin: Int,
    )

    internal fun auditForWinner(
        routes: List<RouteStressInput>,
        calmWinnerIndex: Int,
        bestStressIndex: Int,
    ): Audit {
        val winner = routes[calmWinnerIndex.coerceIn(0, routes.lastIndex)]
        val bestStress = routes[bestStressIndex.coerceIn(0, routes.lastIndex)]
        val stressGapPct =
            if (winner.criticalManeuversCount <= 0) {
                0
            } else {
                (
                    (winner.criticalManeuversCount - bestStress.criticalManeuversCount)
                        .coerceAtLeast(0) *
                        100.0 / winner.criticalManeuversCount
                    ).roundToInt()
            }
        val timePenaltyMin =
            (bestStress.durationInTrafficMin - winner.durationInTrafficMin).coerceAtLeast(0)
        return Audit(stressGapPct = stressGapPct, timePenaltyMin = timePenaltyMin)
    }

    internal fun shouldOverridePolicyB(
        routes: List<RouteStressInput>,
        calmWinnerIndex: Int,
        bestStressIndex: Int,
        audit: Audit,
    ): Boolean =
        audit.timePenaltyMin <= 5 &&
            audit.stressGapPct >= 20 &&
            (
                audit.timePenaltyMin == 0 ||
                    !isStrictlyFasterInTraffic(routes, calmWinnerIndex, bestStressIndex)
                )

    private fun lowestCriticalIndex(routes: List<RouteStressInput>): Int =
        routes.indices.minWith(
            compareBy<Int> { routes[it].criticalManeuversCount }.thenBy { it },
        )

    private fun isStrictlyFasterInTraffic(
        routes: List<RouteStressInput>,
        winnerIndex: Int,
        otherIndex: Int,
    ): Boolean =
        routes[winnerIndex.coerceIn(0, routes.lastIndex)].durationInTrafficMin <
            routes[otherIndex.coerceIn(0, routes.lastIndex)].durationInTrafficMin
}
