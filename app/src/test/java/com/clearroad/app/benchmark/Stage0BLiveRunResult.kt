package com.clearroad.app.benchmark

/**
 * Stage 0B — in-memory live benchmark run result.
 */
data class Stage0BLiveRunResult(
    val requestedCases: Int,
    val completedCases: Int,
    val analyzedCases: Int,
    val budgetSnapshot: BenchmarkLiveBudget.BudgetSnapshot,
    val providerSuccessCount: Int,
    val providerFailureCount: Int,
    val providerSkipCount: Int,
    val totalCandidateCount: Int,
    val caseResults: List<CaseRunResult>,
    val providerOutcomes: List<ProviderOutcome>,
) {
    data class CaseRunResult(
        val caseId: String,
        val googleOutcome: ProviderOutcome,
        val graphHopperOutcome: ProviderOutcome,
        val mergedCandidateCount: Int,
        val analysisResult: BenchmarkReport.CaseResult?,
        val analysisSkippedReason: String?,
    )

    data class ProviderOutcome(
        val providerId: BenchmarkProviderId,
        val caseId: String,
        val requestId: String?,
        val result: ProviderFetchResult,
    )

    fun formatReadable(): String =
        buildString {
            appendLine("=== Stage 0B Live Benchmark Run ===")
            appendLine(
                "cases requested=$requestedCases completed=$completedCases analyzed=$analyzedCases " +
                    "candidates=$totalCandidateCount",
            )
            appendLine(
                "providers success=$providerSuccessCount failure=$providerFailureCount skip=$providerSkipCount",
            )
            appendLine(
                "budget totalReserved=${budgetSnapshot.totalReserved} " +
                    "google=${budgetSnapshot.googleReserved} graphHopper=${budgetSnapshot.graphHopperReserved}",
            )
            caseResults.forEach { case ->
                appendLine()
                appendLine("--- ${case.caseId} ---")
                appendLine("google=${outcomeLabel(case.googleOutcome)}")
                appendLine("graphHopper=${outcomeLabel(case.graphHopperOutcome)}")
                appendLine("mergedCandidates=${case.mergedCandidateCount}")
                if (case.analysisResult != null) {
                    appendLine(
                        "analysis valid=${case.analysisResult.validCandidateCount} " +
                            "pairs=${case.analysisResult.pairComparisons.size}",
                    )
                } else {
                    appendLine("analysis skipped: ${case.analysisSkippedReason}")
                }
            }
        }

    private fun outcomeLabel(outcome: ProviderOutcome): String =
        when (outcome.result) {
            is ProviderFetchResult.Success -> "Success"
            is ProviderFetchResult.Failed -> "Failed(${outcome.result.errorCategory})"
            is ProviderFetchResult.Skipped -> "Skipped(${outcome.result.reason})"
        }
}
