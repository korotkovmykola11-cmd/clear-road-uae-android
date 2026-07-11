package com.clearroad.app.benchmark

import org.json.JSONArray
import org.json.JSONObject

/**
 * Stage 0A — runs fixture-only benchmark pipeline end-to-end.
 */
object BenchmarkRunner {

    data class BenchmarkCaseManifest(
        val case: UaeRouteBenchmarkCase,
        val fixtureFile: String,
    )

    fun loadManifests(casesJson: String): List<BenchmarkCaseManifest> {
        val root = JSONObject(casesJson)
        val cases = root.getJSONArray("cases")
        return buildList {
            for (index in 0 until cases.length()) {
                val entry = cases.getJSONObject(index)
                add(
                    BenchmarkCaseManifest(
                        case =
                            UaeRouteBenchmarkCase(
                                caseId = entry.getString("caseId"),
                                originLabel = entry.getString("originLabel"),
                                destinationLabel = entry.getString("destinationLabel"),
                                originLat = entry.getDouble("originLat"),
                                originLng = entry.getDouble("originLng"),
                                destinationLat = entry.getDouble("destinationLat"),
                                destinationLng = entry.getDouble("destinationLng"),
                                expectedMajorCorridors =
                                    entry.getJSONArray("expectedMajorCorridors").toStringList(),
                                tags = entry.getJSONArray("tags").toStringList(),
                                notes = entry.optString("notes", ""),
                            ),
                        fixtureFile = entry.getString("fixtureFile"),
                    ),
                )
            }
        }
    }

    fun runCase(
        manifest: BenchmarkCaseManifest,
        fixtureJson: String,
    ): BenchmarkReport.CaseResult {
        val parseResult =
            FixtureDirectionsParser.parse(
                fixtureJson = fixtureJson,
                caseId = manifest.case.caseId,
                sourceFixture = manifest.fixtureFile,
            )
        val candidates =
            when (parseResult) {
                is FixtureDirectionsParser.ParseResult.Success -> parseResult.candidates
                is FixtureDirectionsParser.ParseResult.Failure ->
                    error(parseResult.message)
            }

        return analyzeCase(
            benchmarkCase = manifest.case,
            candidates = candidates,
            sourceLabel = manifest.fixtureFile,
        )
    }

    fun analyzeCase(
        benchmarkCase: UaeRouteBenchmarkCase,
        candidates: List<BenchmarkRouteCandidate>,
        sourceLabel: String = "",
    ): BenchmarkReport.CaseResult {
        val classified =
            candidates.map { candidate ->
                val classification =
                    CorridorClassifier.classify(
                        routeSummary = candidate.routeSummary,
                        corridorScanText = candidate.corridorScanText,
                        distanceMeters = candidate.distanceMeters,
                    )
                Triple(candidate, classification, candidate.isValid)
            }

        val candidateResults =
            classified.map { (candidate, classification, valid) ->
                BenchmarkReport.CandidateResult(
                    candidateId = candidate.candidateId,
                    provider = candidate.provider,
                    routeIndex = candidate.routeIndex,
                    corridorLabel = classification.corridorLabel,
                    primaryStableKey = classification.primaryStableKey,
                    allDetectedCorridorKeys = classification.allMatchedStableKeys,
                    matchedRoadEvidence = classification.matchedRoadEvidence,
                    secondaryConnectors = classification.secondaryConnectors,
                    routeEvidenceSignature = classification.routeEvidenceSignature(),
                    distanceMeters = candidate.distanceMeters,
                    durationSeconds = candidate.durationSeconds,
                    tollAed = candidate.tollAed,
                    isValid = valid,
                )
            }

        val validCandidates = classified.filter { (_, _, valid) -> valid }.map { it.first }
        val pairComparisons = buildPairComparisons(validCandidates, classified)

        val validResults = candidateResults.filter { it.isValid }
        val distinctPrimaryCorridors = validResults.map { it.primaryStableKey }.distinct()
        val distinctEvidenceSets = validResults.map { it.routeEvidenceSignature }.distinct()
        val geometryDiverseCandidateCount =
            genuinelyDifferentCandidateIds(pairComparisons).size

        return BenchmarkReport.CaseResult(
            caseId = benchmarkCase.caseId,
            originLabel = benchmarkCase.originLabel,
            destinationLabel = benchmarkCase.destinationLabel,
            fixtureFile = sourceLabel,
            rawCandidateCount = candidates.size,
            validCandidateCount = validCandidates.size,
            distinctPrimaryCorridorCount = distinctPrimaryCorridors.size,
            distinctCorridorEvidenceSetCount = distinctEvidenceSets.size,
            geometryDiverseCandidateCount = geometryDiverseCandidateCount,
            duplicatePairCount = pairComparisons.count { it.isDuplicate },
            genuinelyDifferentPairCount = pairComparisons.count { it.isGenuinelyDifferent },
            candidates = candidateResults,
            pairComparisons = pairComparisons,
        )
    }

    fun runAll(
        manifests: List<BenchmarkCaseManifest>,
        fixtureLoader: (String) -> String,
    ): BenchmarkReport {
        val caseResults = manifests.map { manifest -> runCase(manifest, fixtureLoader(manifest.fixtureFile)) }
        return BenchmarkReport(
            cases = caseResults,
            aggregate =
                BenchmarkReport.AggregateResult(
                    casesProcessed = caseResults.size,
                    casesWithAtLeastTwoPrimaryCorridors =
                        caseResults.count { it.distinctPrimaryCorridorCount >= 2 },
                    casesWithAtLeastTwoEvidenceSets =
                        caseResults.count { it.distinctCorridorEvidenceSetCount >= 2 },
                    casesWithAtLeastThreeGenuinelyDifferentCandidates =
                        caseResults.count { genuinelyDifferentCandidateCount(it) >= 3 },
                ),
        )
    }

    fun genuinelyDifferentCandidateCount(case: BenchmarkReport.CaseResult): Int =
        genuinelyDifferentCandidateIds(case.pairComparisons).size

    private fun genuinelyDifferentCandidateIds(
        pairComparisons: List<BenchmarkReport.PairComparison>,
    ): Set<String> {
        val ids = mutableSetOf<String>()
        pairComparisons
            .filter { it.isGenuinelyDifferent }
            .forEach { pair ->
                ids.add(pair.candidateAId)
                ids.add(pair.candidateBId)
            }
        return ids
    }

    private fun buildPairComparisons(
        validCandidates: List<BenchmarkRouteCandidate>,
        classified: List<Triple<BenchmarkRouteCandidate, CorridorClassifier.Result, Boolean>>,
    ): List<BenchmarkReport.PairComparison> {
        val classificationById = classified.associate { (candidate, result, _) -> candidate.candidateId to result }
        val comparisons = mutableListOf<BenchmarkReport.PairComparison>()
        for (i in validCandidates.indices) {
            for (j in i + 1 until validCandidates.size) {
                val left = validCandidates[i]
                val right = validCandidates[j]
                val geometry =
                    GeometrySimilarity.compare(left.routePathPoints, right.routePathPoints)
                val leftClass = classificationById[left.candidateId]
                val rightClass = classificationById[right.candidateId]
                comparisons.add(
                    BenchmarkReport.PairComparison(
                        candidateAId = left.candidateId,
                        candidateBId = right.candidateId,
                        overlapPercentage = geometry.sharedPercentageOfShorter,
                        sameCorridor =
                            leftClass != null &&
                                rightClass != null &&
                                leftClass.primaryStableKey == rightClass.primaryStableKey,
                        isDuplicate = geometry.isDuplicateHypothesis,
                        isGenuinelyDifferent = geometry.isGenuinelyDifferentHypothesis,
                    ),
                )
            }
        }
        return comparisons
    }

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }
}
