package com.clearroad.app.benchmark

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.domain.RouteIdentityExtractor
import com.clearroad.app.domain.UaeRoadCanon

/**
 * Stage 0A — text-based major corridor classification (no LatLng anchors).
 *
 * Primary corridor behavior mirrors production [RouteIdentityExtractor].
 * Connector roads (e.g. D59, Al Marsa) are benchmark-only evidence — not waypoint anchors.
 */
object CorridorClassifier {

    /**
     * Named connectors not represented as [UaeRoadCanon] stable keys.
     * Benchmark-only; does not modify production canon.
     */
    private val namedConnectorPatterns: List<Pair<String, String>> =
        listOf(
            "al marsa" to "Al Marsa",
            "al khail" to "Al Khail",
            "business bay" to "Business Bay",
        )

    data class Result(
        val primaryStableKey: String,
        val primaryName: String,
        val allMatchedStableKeys: List<String>,
        val matchedRoadEvidence: List<String>,
        val secondaryConnectors: List<String>,
        val evidenceSummary: String,
    ) {
        val corridorLabel: String
            get() = primaryName

        /** Sorted unique evidence used for per-route diversity comparison. */
        fun routeEvidenceSet(): List<String> =
            (listOf(primaryStableKey) + allMatchedStableKeys + matchedRoadEvidence)
                .distinct()
                .sorted()

        fun routeEvidenceSignature(): String = routeEvidenceSet().joinToString("+")
    }

    fun classify(
        routeSummary: String,
        corridorScanText: String,
        distanceMeters: Int = 0,
    ): Result {
        val route =
            RealRouteDebugData(
                distanceText = "",
                durationText = "",
                distanceMeters = distanceMeters,
                durationSeconds = 1,
                tollAED = 0,
                hasToll = false,
                routeSummary = routeSummary,
                corridorScanText = corridorScanText,
            )
        val spine = RouteIdentityExtractor.extractSpine(route)
        val combined = "${routeSummary.trim()} ${corridorScanText.trim()}".trim()
        val normalized = UaeRoadCanon.normalize(combined)

        val allMatchedStableKeys =
            UaeRoadCanon.entries
                .filter { entry -> entry.patterns.any { normalized.contains(it) } }
                .map { it.stableKey }
                .distinct()

        val matchedRoadEvidence = extractMatchedRoadEvidence(normalized, allMatchedStableKeys)
        val secondaryConnectors =
            buildSecondaryConnectors(
                primaryStableKey = spine.stableKey,
                allMatchedStableKeys = allMatchedStableKeys,
                matchedRoadEvidence = matchedRoadEvidence,
            )

        return Result(
            primaryStableKey = spine.stableKey,
            primaryName = spine.primaryName,
            allMatchedStableKeys = allMatchedStableKeys,
            matchedRoadEvidence = matchedRoadEvidence,
            secondaryConnectors = secondaryConnectors,
            evidenceSummary = spine.summary.ifBlank { routeSummary },
        )
    }

    private fun extractMatchedRoadEvidence(
        normalized: String,
        stableKeys: List<String>,
    ): List<String> {
        val evidence = linkedSetOf<String>()
        stableKeys.forEach { evidence.add(it) }
        namedConnectorPatterns.forEach { (pattern, label) ->
            if (normalized.contains(pattern)) {
                evidence.add(label)
            }
        }
        return evidence.toList()
    }

    private fun buildSecondaryConnectors(
        primaryStableKey: String,
        allMatchedStableKeys: List<String>,
        matchedRoadEvidence: List<String>,
    ): List<String> =
        (allMatchedStableKeys.filter { it != primaryStableKey } +
            matchedRoadEvidence.filter { item ->
                item != primaryStableKey && item !in allMatchedStableKeys
            })
            .distinct()
}
