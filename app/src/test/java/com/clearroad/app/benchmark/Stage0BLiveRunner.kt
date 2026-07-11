package com.clearroad.app.benchmark

/**
 * Stage 0B — offline-capable live benchmark orchestrator.
 *
 * Reserves budget, invokes providers sequentially, merges candidates, and analyzes via [BenchmarkRunner].
 */
class Stage0BLiveRunner(
    private val analyzeCaseFn: (
        UaeRouteBenchmarkCase,
        List<BenchmarkRouteCandidate>,
        String,
    ) -> BenchmarkReport.CaseResult = BenchmarkRunner::analyzeCase,
) {

    fun run(
        cases: List<UaeRouteBenchmarkCase>,
        googleProvider: BenchmarkLiveProvider,
        graphHopperProvider: BenchmarkLiveProvider,
        budget: BenchmarkLiveBudget,
    ): Stage0BLiveRunResult {
        val caseResults = mutableListOf<Stage0BLiveRunResult.CaseRunResult>()
        val providerOutcomes = mutableListOf<Stage0BLiveRunResult.ProviderOutcome>()
        var totalCandidateCount = 0
        var providerSuccessCount = 0
        var providerFailureCount = 0
        var providerSkipCount = 0
        var analyzedCases = 0

        cases.forEach { benchmarkCase ->
            val googleOutcome =
                fetchWithBudget(
                    provider = googleProvider,
                    liveProvider = BenchmarkLiveBudget.LiveProvider.GOOGLE,
                    benchmarkCase = benchmarkCase,
                    budget = budget,
                )
            providerOutcomes += googleOutcome
            when (googleOutcome.result) {
                is ProviderFetchResult.Success -> providerSuccessCount++
                is ProviderFetchResult.Failed -> providerFailureCount++
                is ProviderFetchResult.Skipped -> providerSkipCount++
            }

            val graphHopperOutcome =
                fetchWithBudget(
                    provider = graphHopperProvider,
                    liveProvider = BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER,
                    benchmarkCase = benchmarkCase,
                    budget = budget,
                )
            providerOutcomes += graphHopperOutcome
            when (graphHopperOutcome.result) {
                is ProviderFetchResult.Success -> providerSuccessCount++
                is ProviderFetchResult.Failed -> providerFailureCount++
                is ProviderFetchResult.Skipped -> providerSkipCount++
            }

            val mergedCandidates = mergeSuccessfulCandidates(googleOutcome.result, graphHopperOutcome.result)
            totalCandidateCount += mergedCandidates.size

            val analysis =
                if (mergedCandidates.isEmpty()) {
                    Stage0BLiveRunResult.CaseRunResult(
                        caseId = benchmarkCase.caseId,
                        googleOutcome = googleOutcome,
                        graphHopperOutcome = graphHopperOutcome,
                        mergedCandidateCount = 0,
                        analysisResult = null,
                        analysisSkippedReason = analysisSkippedReason(googleOutcome.result, graphHopperOutcome.result),
                    )
                } else {
                    analyzedCases++
                    Stage0BLiveRunResult.CaseRunResult(
                        caseId = benchmarkCase.caseId,
                        googleOutcome = googleOutcome,
                        graphHopperOutcome = graphHopperOutcome,
                        mergedCandidateCount = mergedCandidates.size,
                        analysisResult =
                            analyzeCaseFn(
                                benchmarkCase,
                                mergedCandidates,
                                "live",
                            ),
                        analysisSkippedReason = null,
                    )
                }
            caseResults += analysis
        }

        return Stage0BLiveRunResult(
            requestedCases = cases.size,
            completedCases = caseResults.size,
            analyzedCases = analyzedCases,
            budgetSnapshot = budget.snapshot(),
            providerSuccessCount = providerSuccessCount,
            providerFailureCount = providerFailureCount,
            providerSkipCount = providerSkipCount,
            totalCandidateCount = totalCandidateCount,
            caseResults = caseResults,
            providerOutcomes = providerOutcomes,
        )
    }

    private fun fetchWithBudget(
        provider: BenchmarkLiveProvider,
        liveProvider: BenchmarkLiveBudget.LiveProvider,
        benchmarkCase: UaeRouteBenchmarkCase,
        budget: BenchmarkLiveBudget,
    ): Stage0BLiveRunResult.ProviderOutcome {
        return when (val reservation = budget.reserve(liveProvider, benchmarkCase.caseId)) {
            is BenchmarkLiveBudget.ReservationResult.Granted -> {
                val result =
                    try {
                        provider.fetch(benchmarkCase, reservation)
                    } catch (error: Exception) {
                        ProviderFetchResult.Failed(
                            providerId = provider.providerId,
                            caseId = benchmarkCase.caseId,
                            requestId = reservation.requestId,
                            errorCategory = FailureCategory.INTERNAL,
                            message = sanitizeMessage(error.message ?: error::class.simpleName.orEmpty()),
                        )
                    }
                Stage0BLiveRunResult.ProviderOutcome(
                    providerId = provider.providerId,
                    caseId = benchmarkCase.caseId,
                    requestId = reservation.requestId,
                    result = result,
                )
            }
            is BenchmarkLiveBudget.ReservationResult.Rejected ->
                Stage0BLiveRunResult.ProviderOutcome(
                    providerId = provider.providerId,
                    caseId = benchmarkCase.caseId,
                    requestId = null,
                    result =
                        ProviderFetchResult.Skipped(
                            providerId = provider.providerId,
                            caseId = benchmarkCase.caseId,
                            requestId = null,
                            reason = SkipReason.BUDGET_EXHAUSTED,
                            message = "Budget reservation rejected: ${reservation.reason}",
                        ),
                )
        }
    }

    private fun mergeSuccessfulCandidates(
        googleResult: ProviderFetchResult,
        graphHopperResult: ProviderFetchResult,
    ): List<BenchmarkRouteCandidate> =
        buildList {
            if (googleResult is ProviderFetchResult.Success) {
                addAll(googleResult.candidates)
            }
            if (graphHopperResult is ProviderFetchResult.Success) {
                addAll(graphHopperResult.candidates)
            }
        }

    private fun analysisSkippedReason(
        googleResult: ProviderFetchResult,
        graphHopperResult: ProviderFetchResult,
    ): String {
        val googleLabel = outcomeSummary(googleResult)
        val graphHopperLabel = outcomeSummary(graphHopperResult)
        return "No valid candidates from any provider (google=$googleLabel, graphHopper=$graphHopperLabel)"
    }

    private fun outcomeSummary(result: ProviderFetchResult): String =
        when (result) {
            is ProviderFetchResult.Success -> "success(${result.candidates.size})"
            is ProviderFetchResult.Failed -> "failed(${result.errorCategory})"
            is ProviderFetchResult.Skipped -> "skipped(${result.reason})"
        }
}
