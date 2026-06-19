package com.clearroad.app.replay

/**
 * Stage 39A — aggregate replay metrics across frozen field baseline.
 */
internal data class Stage39AReplayRow(
    val policy: CalmReplayPolicy,
    val pmBlindSpotsFixed: Int,
    val amRegressions: Int,
    val meaningfulOpportunitiesCaptured: Int,
    val meaningfulOpportunityDenominator: Int,
    val falsePositives: Int,
    val dualBlindSpotsUnderPolicyA: Int,
    val dualBlindSpotsFixed: Int,
    val calmMatchPct: Int,
)

internal object Stage39AReplayEngine {

    fun replayAll(
        sets: List<FieldRouteSet>,
        policies: List<CalmReplayPolicy> = CalmReplayPolicy.entries,
    ): List<Stage39AReplayRow> =
        policies.map { policy -> replayPolicy(sets, policy) }

    fun replayPolicy(
        sets: List<FieldRouteSet>,
        policy: CalmReplayPolicy,
    ): Stage39AReplayRow {
        var pmBlindSpotsFixed = 0
        var amRegressions = 0
        var meaningfulCaptured = 0
        var meaningfulDenominator = 0
        var falsePositives = 0
        var dualUnderA = 0
        var dualFixed = 0
        var calmMatches = 0

        sets.forEach { set ->
            val routes = set.routes
            val policyWinner = CalmReplayPolicyEngine.pickWinnerIndex(set, policy)
            val policyAudit = ReplayStressAudit.forWinner(routes, policyWinner)
            if (policyAudit.calmPickedLowestStress) calmMatches++

            val baselineWinner =
                CalmReplayPolicyEngine.pickWinnerIndex(
                    set,
                    CalmReplayPolicy.CURRENT_CALM,
                )
            val baselineAudit = ReplayStressAudit.forWinner(routes, baselineWinner)

            if (baselineAudit.winnerSlowerAndMoreStressful) dualUnderA++
            if (
                policy != CalmReplayPolicy.CURRENT_CALM &&
                baselineAudit.winnerSlowerAndMoreStressful &&
                policyAudit.calmPickedLowestStress
            ) {
                dualFixed++
            }

            val isMeaningfulOpportunity =
                baselineAudit.stressGapPct >= 20 && baselineAudit.timePenaltyMin <= 5
            if (isMeaningfulOpportunity) {
                meaningfulDenominator++
                if (policyAudit.calmPickedLowestStress) {
                    meaningfulCaptured++
                }
            }

            if (policy != CalmReplayPolicy.CURRENT_CALM && policyWinner != baselineWinner) {
                val validOverride =
                    baselineAudit.stressGapPct >= thresholdFor(policy) &&
                        baselineAudit.timePenaltyMin <= 5 &&
                        extraGuardFor(policy, routes, baselineWinner, baselineAudit) &&
                        policyAudit.calmPickedLowestStress
                if (!validOverride) {
                    falsePositives++
                }
            }

            if (set.bucket == FieldBucket.PM_RUSH && policy != CalmReplayPolicy.CURRENT_CALM) {
                val wasBlindSpot =
                    !baselineAudit.calmPickedLowestStress &&
                        baselineAudit.timePenaltyMin <= 5 &&
                        baselineAudit.stressGapPct >= 20
                if (wasBlindSpot && policyAudit.calmPickedLowestStress) {
                    pmBlindSpotsFixed++
                }
            }

            if (set.bucket == FieldBucket.AM_RUSH && policy != CalmReplayPolicy.CURRENT_CALM) {
                if (baselineAudit.calmPickedLowestStress && !policyAudit.calmPickedLowestStress) {
                    amRegressions++
                }
            }
        }

        val calmMatchPct =
            if (sets.isEmpty()) 0 else (calmMatches * 100) / sets.size

        return Stage39AReplayRow(
            policy = policy,
            pmBlindSpotsFixed = pmBlindSpotsFixed,
            amRegressions = amRegressions,
            meaningfulOpportunitiesCaptured = meaningfulCaptured,
            meaningfulOpportunityDenominator = meaningfulDenominator,
            falsePositives = falsePositives,
            dualBlindSpotsUnderPolicyA = dualUnderA,
            dualBlindSpotsFixed = dualFixed,
            calmMatchPct = calmMatchPct,
        )
    }

    fun formatMarkdownTable(rows: List<Stage39AReplayRow>): String {
        val header =
            "| Metric | Current (A) | Policy B | Policy C |\n" +
                "|--------|-------------|----------|----------|"
        fun col(policy: CalmReplayPolicy, selector: (Stage39AReplayRow) -> Int): String =
            rows.first { it.policy == policy }.let(selector).toString()

        fun meaningfulCell(policy: CalmReplayPolicy): String {
            val row = rows.first { it.policy == policy }
            return "${row.meaningfulOpportunitiesCaptured} / ${row.meaningfulOpportunityDenominator}"
        }

        return buildString {
            appendLine(header)
            appendLine(
                "| PM blind spots fixed | — | ${col(CalmReplayPolicy.STRESS_TIE_BREAK) { it.pmBlindSpotsFixed }} | ${col(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK) { it.pmBlindSpotsFixed }} |",
            )
            appendLine(
                "| AM regressions | — | ${col(CalmReplayPolicy.STRESS_TIE_BREAK) { it.amRegressions }} | ${col(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK) { it.amRegressions }} |",
            )
            appendLine(
                "| Meaningful opportunities captured | ${meaningfulCell(CalmReplayPolicy.CURRENT_CALM)} | ${meaningfulCell(CalmReplayPolicy.STRESS_TIE_BREAK)} | ${meaningfulCell(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK)} |",
            )
            appendLine(
                "| False positives | — | ${col(CalmReplayPolicy.STRESS_TIE_BREAK) { it.falsePositives }} | ${col(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK) { it.falsePositives }} |",
            )
            appendLine(
                "| Dual blind spots (slower + stressier) under A | ${col(CalmReplayPolicy.CURRENT_CALM) { it.dualBlindSpotsUnderPolicyA }} | — | — |",
            )
            appendLine(
                "| Dual blind spots fixed | — | ${col(CalmReplayPolicy.STRESS_TIE_BREAK) { it.dualBlindSpotsFixed }} | ${col(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK) { it.dualBlindSpotsFixed }} |",
            )
            appendLine(
                "| CALM match (lowest stress) % | ${col(CalmReplayPolicy.CURRENT_CALM) { it.calmMatchPct }}% | ${col(CalmReplayPolicy.STRESS_TIE_BREAK) { it.calmMatchPct }}% | ${col(CalmReplayPolicy.STRICT_STRESS_TIE_BREAK) { it.calmMatchPct }}% |",
            )
        }
    }

    private fun thresholdFor(policy: CalmReplayPolicy): Int =
        when (policy) {
            CalmReplayPolicy.STRESS_TIE_BREAK -> 20
            CalmReplayPolicy.STRICT_STRESS_TIE_BREAK -> 35
            CalmReplayPolicy.CURRENT_CALM -> Int.MAX_VALUE
        }

    private fun extraGuardFor(
        policy: CalmReplayPolicy,
        routes: List<FieldRouteReplayRoute>,
        baselineWinner: Int,
        audit: ReplayStressAudit,
    ): Boolean =
        when (policy) {
            CalmReplayPolicy.STRESS_TIE_BREAK ->
                audit.timePenaltyMin == 0 ||
                    routes[baselineWinner].durationInTrafficMin >
                    routes[audit.bestStressRouteIndex].durationInTrafficMin
            CalmReplayPolicy.STRICT_STRESS_TIE_BREAK -> audit.timePenaltyMin == 0
            CalmReplayPolicy.CURRENT_CALM -> true
        }
}
