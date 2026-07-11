package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets

class Stage0AFixtureBenchmarkTest {

    @Test
    fun fixtureBenchmark_endToEnd() {
        val casesJson = readResource("benchmark/cases.json")
        val manifests = BenchmarkRunner.loadManifests(casesJson)
        assertEquals(5, manifests.size)

        val report =
            BenchmarkRunner.runAll(manifests) { fixtureFile ->
                readResource("benchmark/$fixtureFile")
            }

        println(report.formatReadable())

        assertEquals(5, report.aggregate.casesProcessed)
        report.cases.forEach { caseResult ->
            assertTrue(
                "Expected at least one candidate for ${caseResult.caseId}",
                caseResult.validCandidateCount >= 1,
            )
            caseResult.pairComparisons.forEach { pair ->
                assertTrue(
                    "Overlap out of range for ${pair.candidateAId} vs ${pair.candidateBId}",
                    pair.overlapPercentage in 0.0..1.0,
                )
            }
            caseResult.candidates.forEach { candidate ->
                assertTrue(candidate.corridorLabel.isNotBlank())
                assertTrue(candidate.routeEvidenceSignature.isNotBlank())
            }
        }
    }

    @Test
    fun difcMarina_showsCorridorDiversityViaPrimaryOrEvidenceAndGeometry() {
        val casesJson = readResource("benchmark/cases.json")
        val manifests = BenchmarkRunner.loadManifests(casesJson)
        val report =
            BenchmarkRunner.runAll(manifests) { fixtureFile ->
                readResource("benchmark/$fixtureFile")
            }

        val difcMarina =
            checkNotNull(
                report.cases.firstOrNull { it.fixtureFile == "route1-DIFC-Marina.json" },
            ) { "Missing DIFC → Marina case in report" }

        val distinctPrimaryCorridors =
            difcMarina.candidates
                .filter { it.isValid }
                .map { it.primaryStableKey }
                .distinct()
        val distinctEvidenceSets =
            difcMarina.candidates
                .filter { it.isValid }
                .map { it.routeEvidenceSignature }
                .distinct()
        val hasGenuinelyDifferentGeometryPair =
            difcMarina.pairComparisons.any { it.isGenuinelyDifferent }

        val criterionA = distinctPrimaryCorridors.size >= 2
        val criterionB =
            distinctEvidenceSets.size >= 2 && hasGenuinelyDifferentGeometryPair

        val evidence =
            buildString {
                appendLine("DIFC → Marina corridor diversity evaluation")
                appendLine("Criterion A (>=2 distinct primary corridors): $criterionA")
                appendLine("  primary keys ($distinctPrimaryCorridors): $distinctPrimaryCorridors")
                appendLine("Criterion B (>=2 evidence sets + genuinely different geometry): $criterionB")
                appendLine("  evidence sets (${distinctEvidenceSets.size}): $distinctEvidenceSets")
                appendLine("  genuinely different geometry pair present: $hasGenuinelyDifferentGeometryPair")
                appendLine()
                appendLine("Per-route classification (primary + connectors + geometry):")
                difcMarina.candidates.forEach { candidate ->
                    appendLine(
                        "  routeIndex=${candidate.routeIndex} primary=${candidate.primaryStableKey} " +
                            "keys=${candidate.allDetectedCorridorKeys} " +
                            "connectors=${candidate.secondaryConnectors} " +
                            "evidence=${candidate.matchedRoadEvidence} " +
                            "signature=${candidate.routeEvidenceSignature}",
                    )
                }
                appendLine("Pair overlaps:")
                difcMarina.pairComparisons.forEach { pair ->
                    appendLine(
                        "  ${pair.candidateAId} vs ${pair.candidateBId}: " +
                            "${"%.1f".format(pair.overlapPercentage * 100)}% " +
                            "genuinelyDifferent=${pair.isGenuinelyDifferent}",
                    )
                }
                if (!criterionA && criterionB) {
                    appendLine()
                    appendLine(
                        "Finding: primary-only classification is insufficient for connector diversity. " +
                            "Route 2 differs via D59 / Al Marsa while primary remains E11.",
                    )
                }
            }
        println(evidence)

        if (!criterionA && !criterionB) {
            fail(evidence)
        }

        assertTrue(criterionA || criterionB)
    }

    @Test
    fun allFiveFixtures_parseSuccessfully() {
        val casesJson = readResource("benchmark/cases.json")
        val manifests = BenchmarkRunner.loadManifests(casesJson)
        manifests.forEach { manifest ->
            val fixtureJson = readResource("benchmark/${manifest.fixtureFile}")
            val result =
                FixtureDirectionsParser.parse(
                    fixtureJson = fixtureJson,
                    caseId = manifest.case.caseId,
                    sourceFixture = manifest.fixtureFile,
                )
            assertTrue(
                "Parse failed for ${manifest.fixtureFile}: " +
                    (result as? FixtureDirectionsParser.ParseResult.Failure)?.message,
                result is FixtureDirectionsParser.ParseResult.Success,
            )
            val candidates = (result as FixtureDirectionsParser.ParseResult.Success).candidates
            assertEquals(3, candidates.size)
            assertTrue(candidates.all { it.isValid })
        }
    }

    private fun readResource(path: String): String {
        val stream =
            checkNotNull(javaClass.classLoader).getResourceAsStream(path)
                ?: error("Missing test resource: $path")
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
