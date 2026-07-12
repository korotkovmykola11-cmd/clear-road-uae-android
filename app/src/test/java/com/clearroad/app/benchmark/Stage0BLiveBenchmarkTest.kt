package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 0B live entry benchmark — disabled by default.
 *
 * Enable only when BOTH are true:
 * 1. Live activation: -Dbenchmark.stage0b.live=true OR BENCHMARK_STAGE0B_LIVE=true
 * 2. API keys present: GOOGLE_MAPS_API_KEY and GRAPHHOPPER_API_KEY
 *
 * Pilot size defaults to 1 case (`live-difc-marina`).
 * Override with -Dbenchmark.stage0b.caseCount=1|2|3|10 or BENCHMARK_STAGE0B_CASE_COUNT.
 * Optional window start: -Dbenchmark.stage0b.caseOffset=N or BENCHMARK_STAGE0B_CASE_OFFSET (default 0).
 *
 * Verified manual run (Windows PowerShell — env activation reaches test JVM):
 * ```
 * $env:BENCHMARK_STAGE0B_LIVE="true"
 * $env:BENCHMARK_STAGE0B_CASE_COUNT="1"
 * $env:GOOGLE_MAPS_API_KEY="..."
 * $env:GRAPHHOPPER_API_KEY="..."
 * ./gradlew :app:testDebugUnitTest `
 *   --tests "com.clearroad.app.benchmark.Stage0BLiveBenchmarkTest"
 * ```
 *
 * Invalid live configuration (when live is enabled) fails the entry test instead of skipping.
 */
class Stage0BLiveBenchmarkTest {

    @Test
    fun runLiveBenchmark_whenExplicitlyEnabled() {
        val preflight = Stage0BLiveBenchmarkGate.preflight()
        enforceStage0BLivePreflight(preflight)

        val run = Stage0BLiveBenchmarkHarness().run()
        enforceStage0BLivePreflight(run.preflight)

        val result = requireNotNull(run.result)
        val metadata = Stage0BLiveReportMetadata.forLiveRun()
        val report = result.formatReadable(metadata)
        println(report)

        assertLiveRunInvariants(result, run.preflight)
        assertNoSecretsInReport(report)
        assertReportConsistency(result)

        if (result.providerFailureCount > 0) {
            org.junit.Assert.fail(
                "Stage 0B live run recorded ${result.providerFailureCount} provider failure(s). " +
                    "See printed report above.",
            )
        }
    }

    private fun assertLiveRunInvariants(
        result: Stage0BLiveRunResult,
        preflight: Stage0BLivePreflight,
    ) {
        val selectedCases = preflight.selectedCases
        val expectedOutcomes = selectedCases.size * 2

        assertEquals(preflight.caseCount, result.requestedCases)
        assertEquals(selectedCases.size, result.completedCases)
        assertEquals(expectedOutcomes, result.providerOutcomes.size)

        assertTrue(result.budgetSnapshot.totalReserved <= BenchmarkLiveBudget.TOTAL_CAP)
        assertTrue(result.budgetSnapshot.googleReserved <= BenchmarkLiveBudget.GOOGLE_CAP)
        assertTrue(result.budgetSnapshot.graphHopperReserved <= BenchmarkLiveBudget.GRAPHHOPPER_CAP)
        assertEquals(result.budgetSnapshot.totalReserved, expectedOutcomes)
        assertEquals(result.budgetSnapshot.googleReserved, selectedCases.size)
        assertEquals(result.budgetSnapshot.graphHopperReserved, selectedCases.size)

        val perProviderCase =
            result.providerOutcomes.groupBy { outcome -> outcome.caseId to outcome.providerId }
        perProviderCase.values.forEach { outcomes ->
            assertEquals("Each provider/case pair must have exactly one outcome (no retries)", 1, outcomes.size)
        }
        assertEquals(result.budgetSnapshot.totalReserved, result.providerOutcomes.size)

        result.providerOutcomes.forEach { outcome ->
            when (outcome.result) {
                is ProviderFetchResult.Success,
                is ProviderFetchResult.Failed,
                -> assertNotNull(outcome.requestId)
                is ProviderFetchResult.Skipped -> Unit
            }
        }

        val expectedOrder =
            selectedCases.flatMap { case ->
                listOf(
                    case.caseId to BenchmarkProviderId.GOOGLE,
                    case.caseId to BenchmarkProviderId.GRAPHHOPPER,
                )
            }
        assertEquals(expectedOrder, result.providerOutcomes.map { it.caseId to it.providerId })
        assertEquals(selectedCases.map { it.caseId }, result.caseResults.map { it.caseId })
    }

    private fun assertReportConsistency(result: Stage0BLiveRunResult) {
        val failures = result.consistencyFailures()
        if (failures.isNotEmpty()) {
            org.junit.Assert.fail(
                "Stage 0B live report consistency check failed: ${failures.joinToString("; ")}",
            )
        }
    }

    private fun assertNoSecretsInReport(report: String) {
        val googleKey = System.getenv(BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY).orEmpty()
        val graphHopperKey = System.getenv(BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY).orEmpty()

        if (googleKey.isNotBlank()) {
            assertFalse("Report leaked Google API key", report.contains(googleKey))
        }
        if (graphHopperKey.isNotBlank()) {
            assertFalse("Report leaked GraphHopper API key", report.contains(graphHopperKey))
        }

        assertFalse(report.contains("api_key="))
        assertFalse(report.contains("X-Goog-Api-Key"))
        assertFalse(report.contains("encodedPolyline"))
        assertFalse(report.contains("LatLng"))
    }
}
