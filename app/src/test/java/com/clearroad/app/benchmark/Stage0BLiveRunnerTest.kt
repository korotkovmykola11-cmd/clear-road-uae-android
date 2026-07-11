package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage0BLiveRunnerTest {

    @Test
    fun run_tenCasesTwoProviders_makesTwentyReservations() {
        val budget = BenchmarkLiveBudget.stage0B()
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        val result = runner().run(Stage0BLiveCases.all, google, graphHopper, budget)

        assertEquals(20, result.budgetSnapshot.totalReserved)
        assertEquals(10, result.budgetSnapshot.googleReserved)
        assertEquals(10, result.budgetSnapshot.graphHopperReserved)
        assertEquals(20, result.providerOutcomes.size)
    }

    @Test
    fun run_usesDeterministicProviderOrder() {
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        val result = runner().run(Stage0BLiveCases.all, google, graphHopper, BenchmarkLiveBudget.stage0B())

        val expected =
            Stage0BLiveCases.all.flatMap { case ->
                listOf(
                    case.caseId to BenchmarkProviderId.GOOGLE,
                    case.caseId to BenchmarkProviderId.GRAPHHOPPER,
                )
            }
        assertEquals(expected, result.providerOutcomes.map { it.caseId to it.providerId })
    }

    @Test
    fun run_neverExceedsTotalBudgetCap() {
        val budget = BenchmarkLiveBudget.stage0B()
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        val result = runner().run(Stage0BLiveCases.all, google, graphHopper, budget)

        assertTrue(result.budgetSnapshot.totalReserved <= BenchmarkLiveBudget.TOTAL_CAP)
    }

    @Test
    fun run_neverExceedsGoogleCap() {
        val result =
            runner().run(
                Stage0BLiveCases.all,
                RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        assertTrue(result.budgetSnapshot.googleReserved <= BenchmarkLiveBudget.GOOGLE_CAP)
    }

    @Test
    fun run_neverExceedsGraphHopperCap() {
        val result =
            runner().run(
                Stage0BLiveCases.all,
                RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        assertTrue(result.budgetSnapshot.graphHopperReserved <= BenchmarkLiveBudget.GRAPHHOPPER_CAP)
    }

    @Test
    fun run_doesNotRetryProviderCalls() {
        val google =
            RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ ->
                ProviderFetchResult.Failed(
                    providerId = BenchmarkProviderId.GOOGLE,
                    caseId = case.caseId,
                    requestId = "google-${case.caseId}-1",
                    errorCategory = FailureCategory.HTTP,
                    httpStatus = 500,
                    latencyMs = 10L,
                    message = "HTTP error 500",
                )
            }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        runner().run(listOf(Stage0BLiveCases.all.first()), google, graphHopper, BenchmarkLiveBudget.stage0B())

        assertEquals(1, google.fetchCount)
        assertEquals(1, graphHopper.fetchCount)
    }

    @Test
    fun run_mergesSuccessfulCandidatesFromBothProviders() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, _ -> success(case, "GOOGLE", routeIndex = 0) },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, _ -> success(case, "GRAPHHOPPER", routeIndex = 0) },
                BenchmarkLiveBudget.stage0B(),
            )

        val caseResult = result.caseResults.single()
        assertEquals(2, caseResult.mergedCandidateCount)
        assertNotNull(caseResult.analysisResult)
        assertEquals(2, caseResult.analysisResult!!.validCandidateCount)
    }

    @Test
    fun run_oneProviderFails_otherSucceeds_stillAnalyzesCase() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, permit ->
                    ProviderFetchResult.Failed(
                        providerId = BenchmarkProviderId.GOOGLE,
                        caseId = case.caseId,
                        requestId = permit.requestId,
                        errorCategory = FailureCategory.HTTP,
                        httpStatus = 500,
                        latencyMs = 12L,
                        message = "HTTP error 500",
                    )
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        val caseResult = result.caseResults.single()
        assertEquals(1, caseResult.mergedCandidateCount)
        assertNotNull(caseResult.analysisResult)
    }

    @Test
    fun run_bothProvidersFail_doesNotAnalyzeCase() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, permit ->
                    failed(case, permit.requestId, BenchmarkProviderId.GOOGLE)
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, permit ->
                    failed(case, permit.requestId, BenchmarkProviderId.GRAPHHOPPER)
                },
                BenchmarkLiveBudget.stage0B(),
            )

        val caseResult = result.caseResults.single()
        assertEquals(0, caseResult.mergedCandidateCount)
        assertNull(caseResult.analysisResult)
        assertNotNull(caseResult.analysisSkippedReason)
    }

    @Test
    fun run_skippedProviderOutcome_isPreserved() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, _ ->
                    ProviderFetchResult.Skipped(
                        providerId = BenchmarkProviderId.GOOGLE,
                        caseId = case.caseId,
                        requestId = null,
                        reason = SkipReason.MISSING_KEY,
                        message = "API key not configured",
                    )
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        assertTrue(result.caseResults.single().googleOutcome.result is ProviderFetchResult.Skipped)
        assertEquals(1, result.providerSkipCount)
    }

    @Test
    fun run_producesCrossProviderGeometryComparisons() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, permit ->
                    success(case, "GOOGLE", routeIndex = 0, pathOffset = 0.0, requestId = permit.requestId)
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, permit ->
                    success(case, "GRAPHHOPPER", routeIndex = 0, pathOffset = 0.02, requestId = permit.requestId)
                },
                BenchmarkLiveBudget.stage0B(),
            )

        val analysis = result.caseResults.single().analysisResult!!
        assertEquals(1, analysis.pairComparisons.size)
        assertTrue(analysis.pairComparisons.single().candidateAId.contains("GOOGLE"))
        assertTrue(analysis.pairComparisons.single().candidateBId.contains("GRAPHHOPPER"))
    }

    @Test
    fun run_reusesBenchmarkRunnerAnalyzeCase() {
        val case = Stage0BLiveCases.all.first()
        val candidates = listOf(sampleCandidate(case, "GOOGLE", 0))
        val cannedResult = BenchmarkRunner.analyzeCase(case, candidates, "live")
        var invocationCount = 0
        var lastCaseId: String? = null
        val spyRunner =
            Stage0BLiveRunner { benchmarkCase, mergedCandidates, sourceLabel ->
                invocationCount++
                lastCaseId = benchmarkCase.caseId
                BenchmarkRunner.analyzeCase(benchmarkCase, mergedCandidates, sourceLabel)
            }

        val result =
            spyRunner.run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, _ -> success(case, "GOOGLE") },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, _ ->
                    ProviderFetchResult.Skipped(
                        providerId = BenchmarkProviderId.GRAPHHOPPER,
                        caseId = case.caseId,
                        reason = SkipReason.MISSING_KEY,
                        message = "skipped",
                    )
                },
                BenchmarkLiveBudget.stage0B(),
            )

        assertEquals(1, invocationCount)
        assertEquals(case.caseId, lastCaseId)
        assertEquals(cannedResult.caseId, result.caseResults.single().analysisResult!!.caseId)
    }

    @Test
    fun run_emptyProviderSuccess_becomesInvalidResponseFailure() {
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, permit ->
                    ProviderFetchResult.Failed(
                        providerId = BenchmarkProviderId.GOOGLE,
                        caseId = case.caseId,
                        requestId = permit.requestId,
                        errorCategory = FailureCategory.INVALID_RESPONSE,
                        httpStatus = 200,
                        latencyMs = 5L,
                        message = "Provider returned no route candidates",
                    )
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, permit ->
                    ProviderFetchResult.Failed(
                        providerId = BenchmarkProviderId.GRAPHHOPPER,
                        caseId = case.caseId,
                        requestId = permit.requestId,
                        errorCategory = FailureCategory.INVALID_RESPONSE,
                        httpStatus = 200,
                        latencyMs = 5L,
                        message = "Provider returned no route candidates",
                    )
                },
                BenchmarkLiveBudget.stage0B(),
            )

        assertEquals(2, result.providerFailureCount)
        assertNull(result.caseResults.single().analysisResult)
    }

    @Test
    fun run_budgetExhaustion_preventsExtraProviderCalls() {
        val budget = BenchmarkLiveBudget.stage0B()
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        repeat(BenchmarkLiveBudget.GOOGLE_CAP) {
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "prefill-google-$it")
        }
        repeat(BenchmarkLiveBudget.GRAPHHOPPER_CAP) {
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, "prefill-gh-$it")
        }

        val result = runner().run(listOf(Stage0BLiveCases.all.first()), google, graphHopper, budget)

        assertEquals(0, google.fetchCount)
        assertEquals(0, graphHopper.fetchCount)
        assertEquals(2, result.providerSkipCount)
        assertTrue(result.caseResults.single().googleOutcome.result is ProviderFetchResult.Skipped)
    }

    @Test
    fun run_providerException_isHandledSafely() {
        val case = Stage0BLiveCases.all.first()
        val throwingGoogle =
            object : BenchmarkLiveProvider {
                override val providerId = BenchmarkProviderId.GOOGLE
                override val displayName = "Throwing Google"
                override fun fetch(case: UaeRouteBenchmarkCase, permit: BenchmarkLiveBudget.ReservationResult.Granted) =
                    throw IllegalStateException("provider boom api_key=secret")
            }

        val result =
            runner().run(
                listOf(case),
                throwingGoogle,
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        val googleOutcome = result.caseResults.single().googleOutcome.result as ProviderFetchResult.Failed
        assertEquals(FailureCategory.INTERNAL, googleOutcome.errorCategory)
        assertFalse(googleOutcome.message.contains("secret"))
        assertEquals(1, result.caseResults.single().mergedCandidateCount)
    }

    @Test
    fun run_resultContainsNoFakeSecretValues() {
        val secret = "super-secret-live-key"
        val result =
            runner().run(
                listOf(Stage0BLiveCases.all.first()),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, _ ->
                    ProviderFetchResult.Failed(
                        providerId = BenchmarkProviderId.GOOGLE,
                        caseId = "live-difc-marina",
                        requestId = "google-live-difc-marina-1",
                        errorCategory = FailureCategory.HTTP,
                        httpStatus = 401,
                        latencyMs = 1L,
                        message = "Unauthorized api_key=$secret",
                    )
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") },
                BenchmarkLiveBudget.stage0B(),
            )

        val text = result.formatReadable()
        assertFalse(text.contains(secret))
    }

    @Test
    fun run_performsNoHttp() {
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }

        runner().run(Stage0BLiveCases.all, google, graphHopper, BenchmarkLiveBudget.stage0B())

        assertEquals(0, google.httpCallCount)
        assertEquals(0, graphHopper.httpCallCount)
    }

    @Test
    fun run_repeatedWithSameFakes_isDeterministic() {
        val cases = Stage0BLiveCases.all.take(2)
        val first = runWithFakes(cases)
        val second = runWithFakes(cases)

        assertEquals(first.budgetSnapshot, second.budgetSnapshot)
        assertEquals(first.caseResults.map { it.caseId }, second.caseResults.map { it.caseId })
        assertEquals(first.providerOutcomes.size, second.providerOutcomes.size)
        assertEquals(first.analyzedCases, second.analyzedCases)
    }

    private fun runWithFakes(cases: List<UaeRouteBenchmarkCase>): Stage0BLiveRunResult {
        val google = RecordingProvider(BenchmarkProviderId.GOOGLE) { case, _ -> success(case, "GOOGLE") }
        val graphHopper = RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { case, _ -> success(case, "GRAPHHOPPER") }
        return runner().run(cases, google, graphHopper, BenchmarkLiveBudget.stage0B())
    }

    private fun runner(): Stage0BLiveRunner = Stage0BLiveRunner()

    private fun success(
        case: UaeRouteBenchmarkCase,
        provider: String,
        routeIndex: Int = 0,
        pathOffset: Double = 0.0,
        requestId: String = "$provider-${case.caseId}-1",
    ): ProviderFetchResult.Success =
        ProviderFetchResult.Success(
            providerId = if (provider == "GOOGLE") BenchmarkProviderId.GOOGLE else BenchmarkProviderId.GRAPHHOPPER,
            caseId = case.caseId,
            requestId = requestId,
            candidates = listOf(sampleCandidate(case, provider, routeIndex, pathOffset)),
            httpStatus = 200,
            latencyMs = 10L,
        )

    private fun failed(
        case: UaeRouteBenchmarkCase,
        requestId: String,
        providerId: BenchmarkProviderId,
    ): ProviderFetchResult.Failed =
        ProviderFetchResult.Failed(
            providerId = providerId,
            caseId = case.caseId,
            requestId = requestId,
            errorCategory = FailureCategory.NETWORK,
            message = "network down",
        )

    private fun sampleCandidate(
        case: UaeRouteBenchmarkCase,
        provider: String,
        routeIndex: Int,
        pathOffset: Double = 0.0,
    ): BenchmarkRouteCandidate =
        BenchmarkRouteCandidate(
            candidateId = "${case.caseId}-$provider-$routeIndex",
            caseId = case.caseId,
            provider = provider,
            routeIndex = routeIndex,
            routeSummary = "Sheikh Zayed Rd/E11",
            corridorScanText = "Merge onto Sheikh Zayed Rd / E11",
            routePathPoints =
                listOf(
                    LatLng(case.originLat + pathOffset, case.originLng + pathOffset),
                    LatLng(case.destinationLat + pathOffset, case.destinationLng + pathOffset),
                ),
            distanceMeters = 18_000,
            durationSeconds = 1_200,
            tollAed = if (provider == "GOOGLE") 4 else 0,
            sourceFixture = "live",
        )

    private class RecordingProvider(
        override val providerId: BenchmarkProviderId,
        private val handler: (UaeRouteBenchmarkCase, BenchmarkLiveBudget.ReservationResult.Granted) -> ProviderFetchResult,
    ) : BenchmarkLiveProvider {
        override val displayName: String = providerId.name
        var fetchCount: Int = 0
        val httpCallCount: Int = 0

        override fun fetch(
            case: UaeRouteBenchmarkCase,
            permit: BenchmarkLiveBudget.ReservationResult.Granted,
        ): ProviderFetchResult {
            fetchCount++
            return handler(case, permit)
        }
    }
}
