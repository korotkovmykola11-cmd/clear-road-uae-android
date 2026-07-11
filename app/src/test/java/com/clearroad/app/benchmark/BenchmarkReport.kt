package com.clearroad.app.benchmark

/**
 * Stage 0A — structured benchmark output (no file I/O).
 */
data class BenchmarkReport(
    val cases: List<CaseResult>,
    val aggregate: AggregateResult,
) {
    data class CandidateResult(
        val candidateId: String,
        val provider: String,
        val routeIndex: Int,
        val corridorLabel: String,
        val primaryStableKey: String,
        val allDetectedCorridorKeys: List<String>,
        val matchedRoadEvidence: List<String>,
        val secondaryConnectors: List<String>,
        val routeEvidenceSignature: String,
        val distanceMeters: Int,
        val durationSeconds: Int,
        val tollAed: Int,
        val isValid: Boolean,
    )

    data class PairComparison(
        val candidateAId: String,
        val candidateBId: String,
        val overlapPercentage: Double,
        val sameCorridor: Boolean,
        val isDuplicate: Boolean,
        val isGenuinelyDifferent: Boolean,
    )

    data class CaseResult(
        val caseId: String,
        val originLabel: String,
        val destinationLabel: String,
        val fixtureFile: String,
        val rawCandidateCount: Int,
        val validCandidateCount: Int,
        val distinctPrimaryCorridorCount: Int,
        val distinctCorridorEvidenceSetCount: Int,
        val geometryDiverseCandidateCount: Int,
        val duplicatePairCount: Int,
        val genuinelyDifferentPairCount: Int,
        val candidates: List<CandidateResult>,
        val pairComparisons: List<PairComparison>,
    ) {
        /** @deprecated use [distinctPrimaryCorridorCount] */
        val uniqueCorridorCount: Int
            get() = distinctPrimaryCorridorCount
    }

    data class AggregateResult(
        val casesProcessed: Int,
        val casesWithAtLeastTwoPrimaryCorridors: Int,
        val casesWithAtLeastTwoEvidenceSets: Int,
        val casesWithAtLeastThreeGenuinelyDifferentCandidates: Int,
    ) {
        /** @deprecated use [casesWithAtLeastTwoPrimaryCorridors] */
        val casesWithAtLeastTwoCorridors: Int
            get() = casesWithAtLeastTwoPrimaryCorridors
    }

    fun formatReadable(): String =
        buildString {
            appendLine("=== Stage 0A Fixture Benchmark Report ===")
            appendLine(
                "Aggregate: cases=${aggregate.casesProcessed}, " +
                    ">=2 primary corridors=${aggregate.casesWithAtLeastTwoPrimaryCorridors}, " +
                    ">=2 evidence sets=${aggregate.casesWithAtLeastTwoEvidenceSets}, " +
                    ">=3 geometry-diverse candidates=${aggregate.casesWithAtLeastThreeGenuinelyDifferentCandidates}",
            )
            cases.forEach { case ->
                appendLine()
                appendLine("--- ${case.caseId}: ${case.originLabel} → ${case.destinationLabel} ---")
                appendLine("fixture=${case.fixtureFile} raw=${case.rawCandidateCount} valid=${case.validCandidateCount}")
                appendLine(
                    "primaryCorridors=${case.distinctPrimaryCorridorCount} " +
                        "evidenceSets=${case.distinctCorridorEvidenceSetCount} " +
                        "geometryDiverseCandidates=${case.geometryDiverseCandidateCount} " +
                        "duplicatePairs=${case.duplicatePairCount} " +
                        "genuinelyDifferentPairs=${case.genuinelyDifferentPairCount}",
                )
                case.candidates.forEach { candidate ->
                    appendLine(
                        "  [${candidate.routeIndex}] primary=${candidate.primaryStableKey} " +
                            "keys=${candidate.allDetectedCorridorKeys} " +
                            "connectors=${candidate.secondaryConnectors} " +
                            "evidence=${candidate.matchedRoadEvidence}",
                    )
                    appendLine(
                        "       dist=${candidate.distanceMeters}m dur=${candidate.durationSeconds}s " +
                            "toll=${candidate.tollAed}AED signature=${candidate.routeEvidenceSignature}",
                    )
                }
                case.pairComparisons.forEach { pair ->
                    appendLine(
                        "  overlap ${pair.candidateAId} vs ${pair.candidateBId}: " +
                            "${"%.1f".format(pair.overlapPercentage * 100)}% " +
                            "samePrimary=${pair.sameCorridor} dup=${pair.isDuplicate} " +
                            "genuinelyDifferent=${pair.isGenuinelyDifferent}",
                    )
                }
            }
        }
}
