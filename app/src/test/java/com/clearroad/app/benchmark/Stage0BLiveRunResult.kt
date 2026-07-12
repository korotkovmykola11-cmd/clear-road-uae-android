package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

    fun formatReadable(metadata: Stage0BLiveReportMetadata = Stage0BLiveReportMetadata.default()): String =
        buildString {
            appendLine("=== Stage 0B Live Benchmark Run ===")
            appendMetadata(metadata)
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
            appendLine()
            appendRequestConfigurationSummary()
            appendLine()
            appendDisclaimers()
            caseResults.forEach { case ->
                appendCaseReport(case, metadata)
            }
        }

    fun consistencyFailures(): List<String> =
        caseResults.flatMap { case ->
            val analysis = case.analysisResult ?: return@flatMap emptyList()
            val check =
                validateCaseConsistency(
                    case = case,
                    analysis = analysis,
                    googleCandidateCount = providerCandidateCount(case.googleOutcome),
                    graphHopperCandidateCount = providerCandidateCount(case.graphHopperOutcome),
                )
            if (check.passed) {
                emptyList()
            } else {
                check.issues.map { issue -> "${case.caseId}: $issue" }
            }
        }

    private fun StringBuilder.appendMetadata(metadata: Stage0BLiveReportMetadata) {
        appendLine("runTimestampUtc=${metadata.runTimestampUtc}")
        appendLine("gitCommitSha=${metadata.gitCommitSha}")
        appendLine("reportSchemaVersion=${metadata.reportSchemaVersion}")
        appendLine("caseCatalogVersion=${metadata.caseCatalogVersion}")
        appendLine("similarityAlgorithmVersion=${metadata.similarityAlgorithmVersion}")
        appendLine(
            "duplicateThresholdPercent=${formatThresholdPercent(metadata.duplicateThreshold)} " +
                "genuinelyDifferentThresholdPercent=${formatThresholdPercent(metadata.genuinelyDifferentThreshold)}",
        )
    }

    private fun StringBuilder.appendRequestConfigurationSummary() {
        appendLine("Request configuration (non-secret):")
        appendLine("  Google: api=Routes v2 computeRoutes travelMode=DRIVE routingPreference=TRAFFIC_AWARE")
        appendLine("    computeAlternativeRoutes=true tollComputation=TOLLS")
        appendLine("    fieldMaskVersion=${GoogleRoutesResponseParser.PARSED_FIELD_MASK.hashCode().toUInt()}")
        appendLine("  GraphHopper: api=Routing v1 profile=car algorithm=alternative_route")
        appendLine("    maxPaths=${GraphHopperLiveBenchmarkProvider.MAX_ALTERNATIVE_PATHS}")
        appendLine("    pointsEncoded=true instructions=true calcPoints=true")
    }

    private fun StringBuilder.appendDisclaimers() {
        appendLine("Disclaimers:")
        appendLine("  - Provider Success means HTTP response + parsing success only.")
        appendLine("  - Success does not mean better route, provider agreement, or superiority.")
        appendLine("  - durationSeconds values are provider-native and are not calibrated as directly comparable ETAs.")
        appendLine("  - tollAed=0 may mean unavailable depending on provider adapter.")
        appendLine("  - duplicateHypothesis and genuinelyDifferentHypothesis are threshold hypotheses, not ground truth.")
        appendLine("  - geometryFingerprintSha256 is an audit identifier, not proof of geometric equality.")
        appendLine("  - Matching fingerprints indicate identical normalized input under this implementation.")
        appendLine("  - Fingerprint collision probability is negligible but not zero.")
    }

    private fun StringBuilder.appendCaseReport(
        case: CaseRunResult,
        metadata: Stage0BLiveReportMetadata,
    ) {
        appendLine()
        appendLine("--- ${case.caseId} ---")
        appendLine("google=${outcomeLabel(case.googleOutcome)}")
        appendLine("graphHopper=${outcomeLabel(case.graphHopperOutcome)}")
        appendLine("mergedCandidates=${case.mergedCandidateCount}")

        val analysis = case.analysisResult
        if (analysis == null) {
            appendLine("analysis skipped: ${case.analysisSkippedReason}")
            return
        }

        appendLine(
            "analysis valid=${analysis.validCandidateCount} pairs=${analysis.pairComparisons.size}",
        )

        val rawCandidates = collectRawCandidates(case)
        val candidatesById = analysis.candidates.associateBy { it.candidateId }
        val googleCandidateCount = providerCandidateCount(case.googleOutcome)
        val graphHopperCandidateCount = providerCandidateCount(case.graphHopperOutcome)
        val consistency =
            validateCaseConsistency(
                case = case,
                analysis = analysis,
                googleCandidateCount = googleCandidateCount,
                graphHopperCandidateCount = graphHopperCandidateCount,
            )

        appendLine()
        appendLine("Provider summary:")
        appendProviderSummary(case.googleOutcome)
        appendProviderSummary(case.graphHopperOutcome)

        appendLine()
        appendLine("Candidates:")
        analysis.candidates.forEach { candidate ->
            appendCandidateDetail(candidate, rawCandidates[candidate.candidateId])
        }

        appendLine()
        appendLine("Pair comparisons:")
        analysis.pairComparisons.forEach { pair ->
            appendPairComparison(pair, candidatesById)
        }

        val crossProviderPairs = analysis.pairComparisons.filter { pair ->
            isCrossProviderPair(pair, candidatesById)
        }
        val lowestOverlapCrossProvider =
            selectLowestOverlapCrossProviderPair(analysis.pairComparisons, candidatesById)

        appendLine()
        appendLine("Case summary:")
        appendLine("googleCandidateCount=$googleCandidateCount")
        appendLine("graphHopperCandidateCount=$graphHopperCandidateCount")
        appendLine("mergedCandidateCount=${case.mergedCandidateCount}")
        appendLine("distinctPrimaryCorridorCount=${analysis.distinctPrimaryCorridorCount}")
        appendLine("distinctEvidenceSetCount=${analysis.distinctCorridorEvidenceSetCount}")
        appendLine("duplicateHypothesisPairCount=${analysis.duplicatePairCount}")
        appendLine("genuinelyDifferentHypothesisPairCount=${analysis.genuinelyDifferentPairCount}")
        appendLine("crossProviderPairCount=${crossProviderPairs.size}")
        appendLine(
            "duplicateThresholdPercent=${formatThresholdPercent(metadata.duplicateThreshold)} " +
                "genuinelyDifferentThresholdPercent=${formatThresholdPercent(metadata.genuinelyDifferentThreshold)} " +
                "similarityAlgorithmVersion=${metadata.similarityAlgorithmVersion}",
        )
        if (lowestOverlapCrossProvider != null) {
            val overlapPct = formatOverlapPercentage(lowestOverlapCrossProvider.overlapPercentage)
            appendLine(
                "lowestOverlapCrossProviderPair=" +
                    "${lowestOverlapCrossProvider.candidateAId} vs " +
                    "${lowestOverlapCrossProvider.candidateBId} overlap=$overlapPct",
            )
        } else {
            appendLine("lowestOverlapCrossProviderPair=none")
        }
        appendLine("consistencyStatus=${if (consistency.passed) "OK" else "FAILED"}")
        consistency.issues.forEach { issue ->
            appendLine("consistencyIssue=$issue")
        }

        appendLine()
        appendLine("Interpretation:")
        appendInterpretation(
            analysis = analysis,
            googleCandidateCount = googleCandidateCount,
            graphHopperCandidateCount = graphHopperCandidateCount,
            crossProviderPairs = crossProviderPairs,
            candidatesById = candidatesById,
        )
    }

    private fun StringBuilder.appendProviderSummary(outcome: ProviderOutcome) {
        val label = providerLabel(outcome.providerId)
        when (val result = outcome.result) {
            is ProviderFetchResult.Success ->
                appendLine(
                    "  $label: outcome=Success candidates=${result.candidates.size} " +
                        "httpStatus=${result.httpStatus} latencyMs=${result.latencyMs} " +
                        "requestId=${outcome.requestId.orEmpty()}",
                )
            is ProviderFetchResult.Failed ->
                appendLine(
                    "  $label: outcome=Failed(${result.errorCategory}) candidates=0 " +
                        "httpStatus=${result.httpStatus ?: "n/a"} latencyMs=${result.latencyMs ?: "n/a"} " +
                        "requestId=${outcome.requestId.orEmpty()}",
                )
            is ProviderFetchResult.Skipped ->
                appendLine(
                    "  $label: outcome=Skipped(${result.reason}) candidates=0 " +
                        "httpStatus=n/a latencyMs=n/a requestId=${outcome.requestId ?: "n/a"}",
                )
        }
    }

    private fun StringBuilder.appendCandidateDetail(
        candidate: BenchmarkReport.CandidateResult,
        rawCandidate: BenchmarkRouteCandidate?,
    ) {
        val geometryPointCount = rawCandidate?.routePathPoints?.size ?: "n/a"
        val geometryFingerprintSha256 =
            rawCandidate?.routePathPoints?.let(::geometryFingerprintSha256).orEmpty().ifBlank { "n/a" }
        val routeSummary = sanitizeRouteSummary(rawCandidate?.routeSummary.orEmpty())
        appendLine(
            "  id=${candidate.candidateId} provider=${candidate.provider} routeIndex=${candidate.routeIndex} " +
                "valid=${candidate.isValid}",
        )
        appendLine("    summary=$routeSummary")
        appendLine(
            "    distanceMeters=${candidate.distanceMeters} " +
                "durationSeconds=${candidate.durationSeconds} " +
                "(provider-native; not directly comparable ETA) " +
                "geometryPointCount=$geometryPointCount geometryFingerprintSha256=$geometryFingerprintSha256",
        )
        appendLine("    primaryCorridor=${candidate.primaryStableKey}")
        appendLine("    allMatchedCorridorKeys=${candidate.allDetectedCorridorKeys}")
        appendLine("    secondaryConnectors=${candidate.secondaryConnectors}")
        appendLine("    evidenceSignature=${candidate.routeEvidenceSignature}")
    }

    private fun StringBuilder.appendPairComparison(
        pair: BenchmarkReport.PairComparison,
        candidatesById: Map<String, BenchmarkReport.CandidateResult>,
    ) {
        val providerA = candidatesById[pair.candidateAId]?.provider ?: "unknown"
        val providerB = candidatesById[pair.candidateBId]?.provider ?: "unknown"
        val relation = if (providerA == providerB) "same-provider" else "cross-provider"
        val overlapPct = formatOverlapPercentage(pair.overlapPercentage)
        appendLine(
            "  ${pair.candidateAId} vs ${pair.candidateBId} " +
                "providers=$providerA/$providerB relation=$relation overlap=$overlapPct " +
                "samePrimaryCorridor=${pair.sameCorridor} duplicateHypothesis=${pair.isDuplicate} " +
                "genuinelyDifferentHypothesis=${pair.isGenuinelyDifferent}",
        )
    }

    private fun StringBuilder.appendInterpretation(
        analysis: BenchmarkReport.CaseResult,
        googleCandidateCount: Int,
        graphHopperCandidateCount: Int,
        crossProviderPairs: List<BenchmarkReport.PairComparison>,
        candidatesById: Map<String, BenchmarkReport.CandidateResult>,
    ) {
        appendLine("googleReturnedAtLeastOneCandidate=${yesNo(googleCandidateCount > 0)}")
        appendLine("graphHopperReturnedAtLeastOneCandidate=${yesNo(graphHopperCandidateCount > 0)}")
        appendLine(
            "anyCrossProviderPairBelowGenuinelyDifferentThreshold=" +
                yesNo(crossProviderPairs.any { it.isGenuinelyDifferent }),
        )
        appendLine(
            "graphHopperCandidateWithoutAnyGoogleDuplicateHypothesis=" +
                yesNo(
                    hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(
                        analysis.pairComparisons,
                        candidatesById,
                    ),
                ),
        )
        appendLine(
            "differentPrimaryCorridorsDetected=${yesNo(analysis.distinctPrimaryCorridorCount >= 2)}",
        )
        appendLine(
            "differentEvidenceSignaturesDetected=${yesNo(analysis.distinctCorridorEvidenceSetCount >= 2)}",
        )
    }

    private fun outcomeLabel(outcome: ProviderOutcome): String =
        when (outcome.result) {
            is ProviderFetchResult.Success -> "Success"
            is ProviderFetchResult.Failed -> "Failed(${outcome.result.errorCategory})"
            is ProviderFetchResult.Skipped -> "Skipped(${outcome.result.reason})"
        }
}

data class Stage0BLiveReportMetadata(
    val runTimestampUtc: String,
    val gitCommitSha: String,
    val reportSchemaVersion: String,
    val caseCatalogVersion: String,
    val similarityAlgorithmVersion: String,
    val duplicateThreshold: Double,
    val genuinelyDifferentThreshold: Double,
) {
    companion object {
        const val REPORT_SCHEMA_VERSION = "stage0b-live-report-v2"
        const val SIMILARITY_ALGORITHM_VERSION = "geometry-similarity-v1"

        fun caseCatalogVersion(): String = "stage0b-v1-${Stage0BLiveCases.all.size}cases"

        fun default(
            runTimestampUtc: String = currentTimestampUtc(),
            gitCommitSha: String = resolveGitCommitSha(),
        ): Stage0BLiveReportMetadata =
            Stage0BLiveReportMetadata(
                runTimestampUtc = runTimestampUtc,
                gitCommitSha = gitCommitSha,
                reportSchemaVersion = REPORT_SCHEMA_VERSION,
                caseCatalogVersion = caseCatalogVersion(),
                similarityAlgorithmVersion = SIMILARITY_ALGORITHM_VERSION,
                duplicateThreshold = GeometrySimilarity.DUPLICATE_SHARED_PCT_HYPOTHESIS,
                genuinelyDifferentThreshold = GeometrySimilarity.GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS,
            )

        fun forLiveRun(
            runTimestampUtc: String = currentTimestampUtc(),
            gitCommitSha: String = resolveGitCommitSha(),
        ): Stage0BLiveReportMetadata = default(runTimestampUtc, gitCommitSha)

        private fun currentTimestampUtc(): String =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC))

        private fun resolveGitCommitSha(): String {
            val fromProperty = System.getProperty("benchmark.stage0b.gitCommitSha")?.trim().orEmpty()
            if (fromProperty.isNotEmpty()) {
                return fromProperty
            }
            val fromEnv = System.getenv("BENCHMARK_STAGE0B_GIT_COMMIT_SHA")?.trim().orEmpty()
            if (fromEnv.isNotEmpty()) {
                return fromEnv
            }
            return "UNKNOWN"
        }
    }
}

data class CaseConsistencyResult(
    val passed: Boolean,
    val issues: List<String>,
)

internal fun collectRawCandidates(case: Stage0BLiveRunResult.CaseRunResult): Map<String, BenchmarkRouteCandidate> {
    val candidates = linkedMapOf<String, BenchmarkRouteCandidate>()
    listOf(case.googleOutcome, case.graphHopperOutcome).forEach { outcome ->
        val success = outcome.result as? ProviderFetchResult.Success ?: return@forEach
        success.candidates.forEach { candidate ->
            candidates[candidate.candidateId] = candidate
        }
    }
    return candidates
}

internal fun providerCandidateCount(outcome: Stage0BLiveRunResult.ProviderOutcome): Int =
    when (val result = outcome.result) {
        is ProviderFetchResult.Success -> result.candidates.size
        else -> 0
    }

internal fun validCandidateCountByProvider(
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
    provider: String,
): Int = candidatesById.values.count { it.provider == provider && it.isValid }

internal fun isCrossProviderPair(
    pair: BenchmarkReport.PairComparison,
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
): Boolean {
    val providerA = candidatesById[pair.candidateAId]?.provider
    val providerB = candidatesById[pair.candidateBId]?.provider
    return providerA != null && providerB != null && providerA != providerB
}

internal fun selectLowestOverlapCrossProviderPair(
    pairs: List<BenchmarkReport.PairComparison>,
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
): BenchmarkReport.PairComparison? =
    pairs
        .filter { pair -> isCrossProviderPair(pair, candidatesById) }
        .minWithOrNull(
            compareBy<BenchmarkReport.PairComparison> { it.overlapPercentage }
                .thenBy { it.candidateAId }
                .thenBy { it.candidateBId },
        )

@Deprecated(
    message = "Use selectLowestOverlapCrossProviderPair",
    replaceWith = ReplaceWith("selectLowestOverlapCrossProviderPair(pairs, candidatesById)"),
)
internal fun selectMostDifferentCrossProviderPair(
    pairs: List<BenchmarkReport.PairComparison>,
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
): BenchmarkReport.PairComparison? = selectLowestOverlapCrossProviderPair(pairs, candidatesById)

/**
 * GraphHopper duplicate-hypothesis qualifier: a candidate qualifies only when it has at least
 * one Google comparison and no GH-vs-Google pair has duplicateHypothesis=true.
 */
internal fun hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(
    pairs: List<BenchmarkReport.PairComparison>,
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
): Boolean {
    val googleIds =
        candidatesById.values
            .filter { it.provider == "GOOGLE" && it.isValid }
            .map { it.candidateId }
            .toSet()
    val graphHopperIds =
        candidatesById.values
            .filter { it.provider == "GRAPHHOPPER" && it.isValid }
            .map { it.candidateId }
            .toSet()

    if (googleIds.isEmpty() || graphHopperIds.isEmpty()) {
        return false
    }

    return graphHopperIds.any { graphHopperId ->
        val comparisonsWithGoogle =
            pairs.filter { pair ->
                (pair.candidateAId == graphHopperId && pair.candidateBId in googleIds) ||
                    (pair.candidateBId == graphHopperId && pair.candidateAId in googleIds)
            }
        comparisonsWithGoogle.isNotEmpty() &&
            comparisonsWithGoogle.all { comparison -> !comparison.isDuplicate }
    }
}

@Deprecated(
    message = "Use hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis",
    replaceWith = ReplaceWith("hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById)"),
)
internal fun hasGraphHopperCandidateWithoutAnyGoogleDuplicate(
    pairs: List<BenchmarkReport.PairComparison>,
    candidatesById: Map<String, BenchmarkReport.CandidateResult>,
): Boolean = hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis(pairs, candidatesById)

internal fun validateCaseConsistency(
    case: Stage0BLiveRunResult.CaseRunResult,
    analysis: BenchmarkReport.CaseResult,
    googleCandidateCount: Int,
    graphHopperCandidateCount: Int,
): CaseConsistencyResult {
    val issues = mutableListOf<String>()
    val validCandidates = analysis.candidates.filter { it.isValid }
    val validGoogleCount = validCandidateCountByProvider(analysis.candidates.associateBy { it.candidateId }, "GOOGLE")
    val validGraphHopperCount =
        validCandidateCountByProvider(analysis.candidates.associateBy { it.candidateId }, "GRAPHHOPPER")
    val candidatesById = analysis.candidates.associateBy { it.candidateId }

    if (googleCandidateCount + graphHopperCandidateCount != case.mergedCandidateCount) {
        issues +=
            "googleCandidateCount($googleCandidateCount) + graphHopperCandidateCount($graphHopperCandidateCount) " +
                "!= mergedCandidateCount(${case.mergedCandidateCount})"
    }

    val expectedPairCount = validCandidates.size * (validCandidates.size - 1) / 2
    if (analysis.pairComparisons.size != expectedPairCount) {
        issues +=
            "pairComparisons(${analysis.pairComparisons.size}) != expectedPairCount($expectedPairCount)"
    }

    val expectedCrossProviderCount = validGoogleCount * validGraphHopperCount
    val actualCrossProviderCount =
        analysis.pairComparisons.count { pair -> isCrossProviderPair(pair, candidatesById) }
    if (actualCrossProviderCount != expectedCrossProviderCount) {
        issues +=
            "crossProviderPairCount($actualCrossProviderCount) != expectedCrossProviderCount($expectedCrossProviderCount)"
    }

    return CaseConsistencyResult(passed = issues.isEmpty(), issues = issues)
}

/**
 * SHA-256 fingerprint over exact Double bit representation of each point (lat then lng),
 * preserving order and including point count. Audit identifier only — not proof of equality.
 */
internal fun geometryFingerprintSha256(points: List<LatLng>): String {
    if (points.isEmpty()) {
        return ""
    }
    val payload =
        buildString {
            append("pointCount=${points.size};")
            points.forEachIndexed { index, point ->
                if (index > 0) {
                    append(';')
                }
                append(java.lang.Double.doubleToRawLongBits(point.latitude))
                append(':')
                append(java.lang.Double.doubleToRawLongBits(point.longitude))
            }
        }
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

@Deprecated(
    message = "Use geometryFingerprintSha256",
    replaceWith = ReplaceWith("geometryFingerprintSha256(points)"),
)
internal fun geometryFingerprint(points: List<LatLng>): String = geometryFingerprintSha256(points)

internal fun formatOverlapPercentage(overlap: Double): String =
    "${"%.1f".format(overlap * 100)}%"

internal fun formatThresholdPercent(threshold: Double): String =
    "${"%.0f".format(threshold * 100)}"

internal fun sanitizeRouteSummary(summary: String): String {
    if (summary.isBlank()) {
        return "(none)"
    }
    var sanitized = summary.replace(Regex("""\p{C}+"""), " ")
    sanitized = sanitized.replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), "[redacted-url]")
    sanitized = sanitized.replace(Regex("""(?i)api_key\s*=\s*\S+"""), "[redacted-key]")
    sanitized = sanitized.replace(Regex("""(?i)apiKey\s*=\s*\S+"""), "[redacted-key]")
    sanitized = sanitized.replace(Regex("""(?i)\bkey\s*=\s*\S+"""), "[redacted-key]")
    sanitized = sanitized.replace(COORD_PAIR_PAREN_REGEX, "[redacted-coords]")
    sanitized = sanitized.replace(COORD_PAIR_COMMA_REGEX, "[redacted-coords]")
    sanitized = sanitized.replace(COORD_PAIR_SPACE_REGEX, "[redacted-coords]")
    sanitized = sanitized.replace(Regex("""\s+"""), " ").trim()
    if (sanitized.isEmpty()) {
        return "(none)"
    }
    return sanitized.take(MAX_ROUTE_SUMMARY_LENGTH)
}

private val COORD_PAIR_COMMA_REGEX =
    Regex("""-?\d{1,3}\.\d+\s*,\s*-?\d{1,3}\.\d+""")
private val COORD_PAIR_SPACE_REGEX =
    Regex("""-?\d{1,3}\.\d+\s+-?\d{1,3}\.\d+""")
private val COORD_PAIR_PAREN_REGEX =
    Regex("""\(\s*-?\d{1,3}\.\d+\s*,\s*-?\d{1,3}\.\d+\s*\)""")

internal fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun providerLabel(providerId: BenchmarkProviderId): String =
    when (providerId) {
        BenchmarkProviderId.GOOGLE -> "GOOGLE"
        BenchmarkProviderId.GRAPHHOPPER -> "GRAPHHOPPER"
    }

private const val MAX_ROUTE_SUMMARY_LENGTH = 120
