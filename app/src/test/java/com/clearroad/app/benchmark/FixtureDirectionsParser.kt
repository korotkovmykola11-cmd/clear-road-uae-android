package com.clearroad.app.benchmark

import com.clearroad.app.extractRouteLegsDebugData

/**
 * Stage 0A — parse Google Classic Directions fixture JSON into benchmark candidates.
 *
 * Reuses production [extractRouteLegsDebugData] via adapter; does not modify DirectionsParsing.kt.
 */
object FixtureDirectionsParser {

    sealed class ParseResult {
        data class Success(val candidates: List<BenchmarkRouteCandidate>) : ParseResult()
        data class Failure(val message: String) : ParseResult()
    }

    fun parse(
        fixtureJson: String,
        caseId: String,
        sourceFixture: String,
        provider: String = "GOOGLE_FIXTURE",
    ): ParseResult {
        if (fixtureJson.isBlank()) {
            return ParseResult.Failure("Fixture JSON is blank for caseId=$caseId")
        }
        return try {
            val routes = extractRouteLegsDebugData(fixtureJson)
            if (routes.isEmpty()) {
                return ParseResult.Failure(
                    "No routes parsed from fixture $sourceFixture (caseId=$caseId)",
                )
            }
            val candidates =
                routes.mapIndexed { index, route ->
                    BenchmarkRouteCandidate(
                        candidateId = "$caseId-$provider-$index",
                        caseId = caseId,
                        provider = provider,
                        routeIndex = index,
                        routeSummary = route.routeSummary,
                        corridorScanText = route.corridorScanText,
                        routePathPoints = route.routePathPoints,
                        distanceMeters = route.distanceMeters,
                        durationSeconds =
                            route.durationInTrafficSeconds?.takeIf { it > 0 }
                                ?: route.durationSeconds,
                        tollAed = route.tollAED,
                        sourceFixture = sourceFixture,
                    )
                }
            ParseResult.Success(candidates)
        } catch (error: Exception) {
            ParseResult.Failure(
                "Failed to parse fixture $sourceFixture (caseId=$caseId): ${error.message}",
            )
        }
    }
}
