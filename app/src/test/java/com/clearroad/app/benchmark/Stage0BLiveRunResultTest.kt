package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import kotlin.math.nextUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage0BLiveRunResultTest {

    @Test
    fun formatReadable_includesCandidateDetails() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("Provider summary:"))
        assertTrue(report.contains("Candidates:"))
        assertTrue(report.contains("id=live-difc-marina-GOOGLE-0"))
        assertTrue(report.contains("id=live-difc-marina-GRAPHHOPPER-1"))
        assertTrue(report.contains("primaryCorridor="))
        assertTrue(report.contains("evidenceSignature="))
        assertTrue(report.contains("geometryPointCount=3"))
        assertTrue(report.contains("geometryFingerprintSha256="))
    }

    @Test
    fun formatReadable_includesProviderCandidateCounts() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("GOOGLE: outcome=Success candidates=2"))
        assertTrue(report.contains("GRAPHHOPPER: outcome=Success candidates=2"))
        assertTrue(report.contains("googleCandidateCount=2"))
        assertTrue(report.contains("graphHopperCandidateCount=2"))
        assertTrue(report.contains("mergedCandidateCount=4"))
    }

    @Test
    fun formatReadable_includesAllPairRows() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("Pair comparisons:"))
        assertEquals(6, report.lines().count { it.trimStart().startsWith("live-difc-marina-") && it.contains(" vs ") })
    }

    @Test
    fun formatReadable_marksCrossProviderPairsCorrectly() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(
            report.contains(
                "live-difc-marina-GOOGLE-0 vs live-difc-marina-GRAPHHOPPER-0 " +
                    "providers=GOOGLE/GRAPHHOPPER relation=cross-provider",
            ),
        )
        assertTrue(
            report.contains(
                "live-difc-marina-GOOGLE-0 vs live-difc-marina-GOOGLE-1 " +
                    "providers=GOOGLE/GOOGLE relation=same-provider",
            ),
        )
        assertTrue(report.contains("crossProviderPairCount=4"))
    }

    @Test
    fun formatReadable_selectsLowestOverlapCrossProviderPairDeterministically() {
        val result = detailedFakeRun()
        val report = result.formatReadable(testMetadata())
        val analysis = requireNotNull(result.caseResults.single().analysisResult)
        val candidatesById = analysis.candidates.associateBy { it.candidateId }
        val selected = requireNotNull(selectLowestOverlapCrossProviderPair(analysis.pairComparisons, candidatesById))

        assertTrue(report.contains("lowestOverlapCrossProviderPair=${selected.candidateAId} vs ${selected.candidateBId}"))
    }

    @Test
    fun selectLowestOverlapCrossProviderPair_tieBreaksByCandidateId() {
        val candidatesById =
            mapOf(
                "A-GOOGLE-0" to candidateResult("A-GOOGLE-0", "GOOGLE"),
                "B-GRAPHHOPPER-0" to candidateResult("B-GRAPHHOPPER-0", "GRAPHHOPPER"),
                "C-GOOGLE-1" to candidateResult("C-GOOGLE-1", "GOOGLE"),
                "D-GRAPHHOPPER-1" to candidateResult("D-GRAPHHOPPER-1", "GRAPHHOPPER"),
            )
        val pairs =
            listOf(
                pair("B-GRAPHHOPPER-0", "C-GOOGLE-1", overlap = 0.40),
                pair("A-GOOGLE-0", "D-GRAPHHOPPER-1", overlap = 0.40),
            )

        val selected = selectLowestOverlapCrossProviderPair(pairs, candidatesById)

        assertNotNull(selected)
        assertEquals("A-GOOGLE-0", selected!!.candidateAId)
        assertEquals("D-GRAPHHOPPER-1", selected.candidateBId)
    }

    @Test
    fun formatReadable_usesHypothesisMetricLabels() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("duplicateHypothesisPairCount="))
        assertTrue(report.contains("genuinelyDifferentHypothesisPairCount="))
        assertTrue(report.contains("duplicateThresholdPercent=85"))
        assertTrue(report.contains("genuinelyDifferentThresholdPercent=70"))
        assertTrue(report.contains("similarityAlgorithmVersion=geometry-similarity-v1"))
        assertFalse(report.contains("duplicatePairCount="))
        assertFalse(report.contains("genuinelyDifferentPairCount="))
        assertFalse(report.contains("mostDifferentCrossProviderPair="))
    }

    @Test
    fun formatReadable_includesMetadataAndRequestConfiguration() {
        val metadata =
            Stage0BLiveReportMetadata(
                runTimestampUtc = "2026-07-12T12:00:00Z",
                gitCommitSha = "abc123def456",
                reportSchemaVersion = Stage0BLiveReportMetadata.REPORT_SCHEMA_VERSION,
                caseCatalogVersion = Stage0BLiveReportMetadata.caseCatalogVersion(),
                similarityAlgorithmVersion = Stage0BLiveReportMetadata.SIMILARITY_ALGORITHM_VERSION,
                duplicateThreshold = GeometrySimilarity.DUPLICATE_SHARED_PCT_HYPOTHESIS,
                genuinelyDifferentThreshold = GeometrySimilarity.GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS,
            )
        val report = detailedFakeRun().formatReadable(metadata)

        assertTrue(report.contains("runTimestampUtc=2026-07-12T12:00:00Z"))
        assertTrue(report.contains("gitCommitSha=abc123def456"))
        assertTrue(report.contains("reportSchemaVersion=stage0b-live-report-v2"))
        assertTrue(report.contains("caseCatalogVersion=stage0b-v1-10cases"))
        assertTrue(report.contains("Request configuration (non-secret):"))
        assertTrue(report.contains("api=Routes v2 computeRoutes"))
        assertTrue(report.contains("api=Routing v1 profile=car"))
        assertFalse(report.contains("graphhopper.com/api"))
        assertFalse(report.contains("routes.googleapis.com"))
    }

    @Test
    fun formatReadable_includesDisclaimers() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("Disclaimers:"))
        assertTrue(report.contains("Provider Success means HTTP response + parsing success only."))
        assertTrue(report.contains("Success does not mean better route, provider agreement, or superiority."))
        assertTrue(report.contains("durationSeconds values are provider-native and are not calibrated as directly comparable ETAs."))
        assertTrue(report.contains("tollAed=0 may mean unavailable depending on provider adapter."))
        assertTrue(report.contains("duplicateHypothesis and genuinelyDifferentHypothesis are threshold hypotheses, not ground truth."))
        assertTrue(report.contains("geometryFingerprintSha256 is an audit identifier, not proof of geometric equality."))
        assertTrue(report.contains("Fingerprint collision probability is negligible but not zero."))
    }

    @Test
    fun formatReadable_reportsConsistencyOkForValidFakeRun() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("consistencyStatus=OK"))
        assertFalse(report.contains("consistencyIssue="))
        assertTrue(detailedFakeRun().consistencyFailures().isEmpty())
    }

    @Test
    fun validateCaseConsistency_detectsInconsistentPairCount() {
        val case = syntheticCaseResult(mergedCandidateCount = 2)
        val analysis =
            BenchmarkReport.CaseResult(
                caseId = "case-1",
                originLabel = "A",
                destinationLabel = "B",
                fixtureFile = "live",
                rawCandidateCount = 2,
                validCandidateCount = 2,
                distinctPrimaryCorridorCount = 1,
                distinctCorridorEvidenceSetCount = 1,
                geometryDiverseCandidateCount = 0,
                duplicatePairCount = 0,
                genuinelyDifferentPairCount = 0,
                candidates =
                    listOf(
                        candidateResult("case-1-GOOGLE-0", "GOOGLE"),
                        candidateResult("case-1-GRAPHHOPPER-0", "GRAPHHOPPER"),
                    ),
                pairComparisons = emptyList(),
            )

        val result =
            validateCaseConsistency(
                case = case,
                analysis = analysis,
                googleCandidateCount = 1,
                graphHopperCandidateCount = 1,
            )

        assertFalse(result.passed)
        assertTrue(result.issues.any { it.contains("pairComparisons(0)") })
    }

    @Test
    fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis_falseWhenGhDuplicatesOneGoogle() {
        val candidatesById =
            mapOf(
                "GH-1" to candidateResult("GH-1", "GRAPHHOPPER"),
                "G-1" to candidateResult("G-1", "GOOGLE"),
                "G-2" to candidateResult("G-2", "GOOGLE"),
            )
        val pairs =
            listOf(
                pair("GH-1", "G-1", overlap = 0.90, duplicate = true),
                pair("GH-1", "G-2", overlap = 0.50, duplicate = false),
            )

        assertFalse(hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById))
    }

    @Test
    fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis_trueWhenGhDuplicatesNoGoogle() {
        val candidatesById =
            mapOf(
                "GH-1" to candidateResult("GH-1", "GRAPHHOPPER"),
                "G-1" to candidateResult("G-1", "GOOGLE"),
                "G-2" to candidateResult("G-2", "GOOGLE"),
            )
        val pairs =
            listOf(
                pair("GH-1", "G-1", overlap = 0.50, duplicate = false),
                pair("GH-1", "G-2", overlap = 0.40, duplicate = false),
            )

        assertTrue(hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById))
    }

    @Test
    fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis_trueWhenOneGhQualifiesAmongTwo() {
        val candidatesById =
            mapOf(
                "GH-1" to candidateResult("GH-1", "GRAPHHOPPER"),
                "GH-2" to candidateResult("GH-2", "GRAPHHOPPER"),
                "G-1" to candidateResult("G-1", "GOOGLE"),
            )
        val pairs =
            listOf(
                pair("GH-1", "G-1", overlap = 0.90, duplicate = true),
                pair("GH-2", "G-1", overlap = 0.40, duplicate = false),
            )

        assertTrue(hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById))
    }

    @Test
    fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis_falseWhenNoGoogleCandidates() {
        val candidatesById = mapOf("GH-1" to candidateResult("GH-1", "GRAPHHOPPER"))
        val pairs = emptyList<BenchmarkReport.PairComparison>()

        assertFalse(hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById))
    }

    @Test
    fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis_falseWhenNoGraphHopperCandidates() {
        val candidatesById = mapOf("G-1" to candidateResult("G-1", "GOOGLE"))
        val pairs = emptyList<BenchmarkReport.PairComparison>()

        assertFalse(hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById))
    }

    @Test
    fun formatReadable_usesCorrectDuplicateHypothesisInterpretationLabel() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertTrue(report.contains("graphHopperCandidateWithoutAnyGoogleDuplicateHypothesis=yes"))
        assertFalse(report.contains("graphHopperCandidateWithoutAnyGoogleDuplicate=yes"))
        assertFalse(report.contains("anyGraphHopperCandidateNotDuplicatedByEveryGoogleCandidate"))
    }

    @Test
    fun sanitizeRouteSummary_normalizesCrLfTab() {
        val sanitized = sanitizeRouteSummary("Line one\r\nLine two\textra")

        assertFalse(sanitized.contains("\n"))
        assertFalse(sanitized.contains("\r"))
        assertEquals("Line one Line two extra", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_removesOtherControlCharacters() {
        val sanitized = sanitizeRouteSummary("Start\u0000middle\u0007end\u001Btail")

        assertFalse(sanitized.contains("\u0000"))
        assertFalse(sanitized.contains("\u0007"))
        assertFalse(sanitized.contains("\u001B"))
        assertEquals("Start middle end tail", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_redactsCommaSeparatedCoordinates() {
        val sanitized = sanitizeRouteSummary("Via 25.21381, 55.28203 then E11")

        assertFalse(sanitized.contains("25.21381"))
        assertFalse(sanitized.contains("55.28203"))
        assertTrue(sanitized.contains("E11"))
    }

    @Test
    fun sanitizeRouteSummary_redactsSpaceSeparatedCoordinates() {
        val sanitized = sanitizeRouteSummary("Via 25.21381 55.28203 then E11")

        assertFalse(sanitized.contains("25.21381"))
        assertFalse(sanitized.contains("55.28203"))
        assertTrue(sanitized.contains("E11"))
    }

    @Test
    fun sanitizeRouteSummary_redactsParenthesizedCoordinates() {
        val sanitized = sanitizeRouteSummary("Waypoint (25.21381,55.28203) on E11")

        assertFalse(sanitized.contains("25.21381"))
        assertFalse(sanitized.contains("55.28203"))
        assertTrue(sanitized.contains("E11"))
    }

    @Test
    fun sanitizeRouteSummary_redactsLowerPrecisionCoordinatePairs() {
        val sanitized = sanitizeRouteSummary("Near 25.12, 55.13 toward E11")

        assertFalse(sanitized.contains("25.12"))
        assertFalse(sanitized.contains("55.13"))
        assertTrue(sanitized.contains("E11"))
    }

    @Test
    fun sanitizeRouteSummary_redactsUrlWithKeyQuery() {
        val sanitized = sanitizeRouteSummary("Head to https://example.com/route?api_key=secret via E11")

        assertFalse(sanitized.contains("https://"))
        assertFalse(sanitized.contains("api_key="))
        assertTrue(sanitized.contains("E11"))
    }

    @Test
    fun sanitizeRouteSummary_preservesOrdinaryRoadName() {
        val sanitized = sanitizeRouteSummary("Merge onto Sheikh Zayed Rd / E11")

        assertEquals("Merge onto Sheikh Zayed Rd / E11", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_redactsLowercaseHttpUrl() {
        val sanitized = sanitizeRouteSummary("Head to http://example.com/path via E11")

        assertEquals("Head to [redacted-url] via E11", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_redactsLowercaseHttpsUrl() {
        val sanitized = sanitizeRouteSummary("Head to https://example.com/path via E11")

        assertEquals("Head to [redacted-url] via E11", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_redactsUppercaseHttpUrl() {
        val sanitized = sanitizeRouteSummary("Head to HTTP://example.com/path via E11")

        assertEquals("Head to [redacted-url] via E11", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_redactsUppercaseHttpsUrlWithApiKeyQuery() {
        val sanitized = sanitizeRouteSummary("Head to HTTPS://example.com/route?api_key=secret via E11")

        assertEquals("Head to [redacted-url] via E11", sanitized)
        assertFalse(sanitized.contains("api_key="))
    }

    @Test
    fun sanitizeRouteSummary_redactsMixedCaseSchemeUrl() {
        val sanitized = sanitizeRouteSummary("Head to HtTpS://example.com/path via E11")

        assertEquals("Head to [redacted-url] via E11", sanitized)
    }

    @Test
    fun sanitizeRouteSummary_preservesRoadTextContainingHttpLettersWithoutUrl() {
        val sanitized = sanitizeRouteSummary("Continue via http underpass to Sheikh Zayed Rd / E11")

        assertEquals("Continue via http underpass to Sheikh Zayed Rd / E11", sanitized)
    }

    @Test
    fun geometryFingerprintSha256_isDeterministicForIdenticalGeometry() {
        val points =
            listOf(
                LatLng(25.1, 55.2),
                LatLng(25.2, 55.3),
            )
        val first = geometryFingerprintSha256(points)
        val second = geometryFingerprintSha256(points)

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
        assertFalse(first.contains("25.1"))
        assertFalse(first.contains("55.2"))
    }

    @Test
    fun geometryFingerprintSha256_changesWhenOneCoordinateChangesSlightly() {
        val base =
            listOf(
                LatLng(25.1, 55.2),
                LatLng(25.2, 55.3),
            )
        val shifted =
            listOf(
                LatLng(25.1.nextUp(), 55.2),
                LatLng(25.2, 55.3),
            )

        assertNotEquals(geometryFingerprintSha256(base), geometryFingerprintSha256(shifted))
    }

    @Test
    fun geometryFingerprintSha256_changesWhenPointOrderChanges() {
        val ordered =
            listOf(
                LatLng(25.1, 55.2),
                LatLng(25.2, 55.3),
            )
        val reversed =
            listOf(
                LatLng(25.2, 55.3),
                LatLng(25.1, 55.2),
            )

        assertNotEquals(geometryFingerprintSha256(ordered), geometryFingerprintSha256(reversed))
    }

    @Test
    fun geometryFingerprintSha256_changesWhenPointCountChanges() {
        val twoPoints =
            listOf(
                LatLng(25.1, 55.2),
                LatLng(25.2, 55.3),
            )
        val threePoints =
            listOf(
                LatLng(25.1, 55.2),
                LatLng(25.15, 55.25),
                LatLng(25.2, 55.3),
            )

        assertNotEquals(geometryFingerprintSha256(twoPoints), geometryFingerprintSha256(threePoints))
    }

    @Test
    fun formatReadable_containsNoFakeSecret() {
        val secret = "stage0b-report-fake-secret-key"
        val case = Stage0BLiveCases.all.first()
        val result =
            runner().run(
                listOf(case),
                RecordingProvider(BenchmarkProviderId.GOOGLE) { _, _ ->
                    ProviderFetchResult.Failed(
                        providerId = BenchmarkProviderId.GOOGLE,
                        caseId = case.caseId,
                        requestId = "google-${case.caseId}-1",
                        errorCategory = FailureCategory.HTTP,
                        httpStatus = 401,
                        latencyMs = 5L,
                        message = "Unauthorized api_key=$secret",
                    )
                },
                RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, permit ->
                    success(
                        case = case,
                        provider = "GRAPHHOPPER",
                        routeIndex = 0,
                        pathOffset = 0.0,
                        requestId = permit.requestId,
                    )
                },
                BenchmarkLiveBudget.stage0B(),
            )

        val report = result.formatReadable(testMetadata())

        assertFalse(report.contains(secret))
        assertFalse(report.contains("api_key="))
        assertFalse(report.contains("X-Goog-Api-Key"))
    }

    @Test
    fun formatReadable_containsNoRawGeometryCoordinates() {
        val report = detailedFakeRun().formatReadable(testMetadata())

        assertFalse(report.contains("LatLng"))
        assertFalse(report.contains("encodedPolyline"))
        assertFalse(report.contains("25.2138142"))
        assertFalse(report.contains("55.2820336"))
        assertTrue(report.contains("geometryPointCount="))
    }

    @Test
    fun formatReadable_skipsDetailedSectionsWhenAnalysisUnavailable() {
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

        val report = result.formatReadable(testMetadata())

        assertTrue(report.contains("analysis skipped:"))
        assertFalse(report.contains("Pair comparisons:"))
        assertFalse(report.contains("Interpretation:"))
    }

    @Test
    fun stage0AFixtureReportFormat_unchanged() {
        val manifest =
            BenchmarkRunner.loadManifests(
                javaClass.classLoader
                    .getResourceAsStream("benchmark/cases.json")!!
                    .bufferedReader()
                    .readText(),
            ).first()
        val fixtureJson =
            javaClass.classLoader
                .getResourceAsStream("benchmark/${manifest.fixtureFile}")!!
                .bufferedReader()
                .readText()

        val caseResult = BenchmarkRunner.runCase(manifest, fixtureJson)
        val report =
            BenchmarkReport(
                cases = listOf(caseResult),
                aggregate =
                    BenchmarkReport.AggregateResult(
                        casesProcessed = 1,
                        casesWithAtLeastTwoPrimaryCorridors =
                            if (caseResult.distinctPrimaryCorridorCount >= 2) 1 else 0,
                        casesWithAtLeastTwoEvidenceSets =
                            if (caseResult.distinctCorridorEvidenceSetCount >= 2) 1 else 0,
                        casesWithAtLeastThreeGenuinelyDifferentCandidates =
                            if (BenchmarkRunner.genuinelyDifferentCandidateCount(caseResult) >= 3) 1 else 0,
                    ),
            ).formatReadable()

        assertTrue(report.startsWith("=== Stage 0A Fixture Benchmark Report ==="))
        assertTrue(report.contains("overlap "))
        assertFalse(report.contains("Interpretation:"))
        assertFalse(report.contains("Provider summary:"))
    }

    private fun testMetadata(): Stage0BLiveReportMetadata =
        Stage0BLiveReportMetadata(
            runTimestampUtc = "2026-07-12T12:00:00Z",
            gitCommitSha = "test-commit-sha",
            reportSchemaVersion = Stage0BLiveReportMetadata.REPORT_SCHEMA_VERSION,
            caseCatalogVersion = Stage0BLiveReportMetadata.caseCatalogVersion(),
            similarityAlgorithmVersion = Stage0BLiveReportMetadata.SIMILARITY_ALGORITHM_VERSION,
            duplicateThreshold = GeometrySimilarity.DUPLICATE_SHARED_PCT_HYPOTHESIS,
            genuinelyDifferentThreshold = GeometrySimilarity.GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS,
        )

    private fun detailedFakeRun(): Stage0BLiveRunResult {
        val case = Stage0BLiveCases.all.first()
        return runner().run(
            listOf(case),
            RecordingProvider(BenchmarkProviderId.GOOGLE) { _, permit ->
                success(
                    case = case,
                    provider = "GOOGLE",
                    routeIndex = 0,
                    pathOffset = 0.0,
                    requestId = permit.requestId,
                    candidates =
                        listOf(
                            sampleCandidate(case, "GOOGLE", 0, 0.0),
                            sampleCandidate(case, "GOOGLE", 1, 0.02),
                        ),
                )
            },
            RecordingProvider(BenchmarkProviderId.GRAPHHOPPER) { _, permit ->
                success(
                    case = case,
                    provider = "GRAPHHOPPER",
                    routeIndex = 0,
                    pathOffset = 0.0,
                    requestId = permit.requestId,
                    candidates =
                        listOf(
                            sampleCandidate(case, "GRAPHHOPPER", 0, 0.0, "Al Khail Rd connector"),
                            sampleCandidate(
                                case,
                                "GRAPHHOPPER",
                                1,
                                0.35,
                                "Business Bay bypass",
                                corridorScanText = "Continue on Business Bay route",
                            ),
                        ),
                )
            },
            BenchmarkLiveBudget.stage0B(),
        )
    }

    private fun syntheticCaseResult(mergedCandidateCount: Int): Stage0BLiveRunResult.CaseRunResult =
        Stage0BLiveRunResult.CaseRunResult(
            caseId = "case-1",
            googleOutcome =
                Stage0BLiveRunResult.ProviderOutcome(
                    providerId = BenchmarkProviderId.GOOGLE,
                    caseId = "case-1",
                    requestId = "google-case-1-1",
                    result =
                        ProviderFetchResult.Success(
                            providerId = BenchmarkProviderId.GOOGLE,
                            caseId = "case-1",
                            requestId = "google-case-1-1",
                            candidates = listOf(sampleCandidate(Stage0BLiveCases.all.first(), "GOOGLE", 0, 0.0)),
                            httpStatus = 200,
                            latencyMs = 1L,
                        ),
                ),
            graphHopperOutcome =
                Stage0BLiveRunResult.ProviderOutcome(
                    providerId = BenchmarkProviderId.GRAPHHOPPER,
                    caseId = "case-1",
                    requestId = "graphhopper-case-1-2",
                    result =
                        ProviderFetchResult.Success(
                            providerId = BenchmarkProviderId.GRAPHHOPPER,
                            caseId = "case-1",
                            requestId = "graphhopper-case-1-2",
                            candidates = listOf(sampleCandidate(Stage0BLiveCases.all.first(), "GRAPHHOPPER", 0, 0.0)),
                            httpStatus = 200,
                            latencyMs = 1L,
                        ),
                ),
            mergedCandidateCount = mergedCandidateCount,
            analysisResult = null,
            analysisSkippedReason = null,
        )

    private fun runner(): Stage0BLiveRunner = Stage0BLiveRunner()

    private fun success(
        case: UaeRouteBenchmarkCase,
        provider: String,
        routeIndex: Int,
        pathOffset: Double,
        requestId: String,
        candidates: List<BenchmarkRouteCandidate> = listOf(sampleCandidate(case, provider, routeIndex, pathOffset)),
    ): ProviderFetchResult.Success =
        ProviderFetchResult.Success(
            providerId = if (provider == "GOOGLE") BenchmarkProviderId.GOOGLE else BenchmarkProviderId.GRAPHHOPPER,
            caseId = case.caseId,
            requestId = requestId,
            candidates = candidates,
            httpStatus = 200,
            latencyMs = 42L,
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
        pathOffset: Double,
        routeSummary: String = "Sheikh Zayed Rd/E11",
        corridorScanText: String = "Merge onto Sheikh Zayed Rd / E11",
    ): BenchmarkRouteCandidate =
        BenchmarkRouteCandidate(
            candidateId = "${case.caseId}-$provider-$routeIndex",
            caseId = case.caseId,
            provider = provider,
            routeIndex = routeIndex,
            routeSummary = routeSummary,
            corridorScanText = corridorScanText,
            routePathPoints =
                listOf(
                    LatLng(case.originLat + pathOffset, case.originLng + pathOffset),
                    LatLng(
                        (case.originLat + case.destinationLat) / 2.0 + pathOffset,
                        (case.originLng + case.destinationLng) / 2.0 + pathOffset,
                    ),
                    LatLng(case.destinationLat + pathOffset, case.destinationLng + pathOffset),
                ),
            distanceMeters = 18_000 + (pathOffset * 10_000).toInt(),
            durationSeconds = 1_200,
            tollAed = if (provider == "GOOGLE") 4 else 0,
            sourceFixture = "live",
        )

    private fun candidateResult(candidateId: String, provider: String): BenchmarkReport.CandidateResult =
        BenchmarkReport.CandidateResult(
            candidateId = candidateId,
            provider = provider,
            routeIndex = 0,
            corridorLabel = "E11",
            primaryStableKey = "E11",
            allDetectedCorridorKeys = listOf("E11"),
            matchedRoadEvidence = emptyList(),
            secondaryConnectors = emptyList(),
            routeEvidenceSignature = "E11",
            distanceMeters = 1_000,
            durationSeconds = 100,
            tollAed = 0,
            isValid = true,
        )

    private fun pair(
        candidateAId: String,
        candidateBId: String,
        overlap: Double,
        duplicate: Boolean = overlap >= GeometrySimilarity.DUPLICATE_SHARED_PCT_HYPOTHESIS,
    ): BenchmarkReport.PairComparison =
        BenchmarkReport.PairComparison(
            candidateAId = candidateAId,
            candidateBId = candidateBId,
            overlapPercentage = overlap,
            sameCorridor = false,
            isDuplicate = duplicate,
            isGenuinelyDifferent = overlap < GeometrySimilarity.GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS,
        )

    private class RecordingProvider(
        override val providerId: BenchmarkProviderId,
        private val handler: (UaeRouteBenchmarkCase, BenchmarkLiveBudget.ReservationResult.Granted) -> ProviderFetchResult,
    ) : BenchmarkLiveProvider {
        override val displayName: String = providerId.name

        override fun fetch(
            case: UaeRouteBenchmarkCase,
            permit: BenchmarkLiveBudget.ReservationResult.Granted,
        ): ProviderFetchResult = handler(case, permit)
    }
}
