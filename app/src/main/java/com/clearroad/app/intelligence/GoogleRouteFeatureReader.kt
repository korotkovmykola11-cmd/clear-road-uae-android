package com.clearroad.app.intelligence

import java.time.Instant

/** Parses maneuver signals from Google Directions steps — not a [RouteDataSource]. */
internal object GoogleRouteSignalsExtractor {

    private val CRITICAL_MANEUVER_KEYWORDS =
        listOf(
            "merge",
            "fork",
            "exit",
            "ramp",
            "keep left",
            "keep right",
            "slight left",
            "slight right",
            "sharp left",
            "sharp right",
            "roundabout",
        )

    private val RESTRICTED_ROAD_KEYWORDS =
        listOf(
            "ferry",
            "unpaved",
        )

    fun extract(steps: List<GoogleDirectionsStepInput>): GoogleManeuverEvidence {
        val maneuvers = steps.mapNotNull { step -> step.maneuver?.lowercase()?.replace('_', '-') }
        if (maneuvers.isEmpty()) {
            return GoogleManeuverEvidence(
                criticalManeuversCount = null,
                turnsCount = null,
                roundaboutsFromManeuvers = null,
                rampOrExitCount = null,
                restrictedRoadHintsCount = null,
            )
        }

        val criticalCount = maneuvers.count { isCriticalManeuver(it) }
        return GoogleManeuverEvidence(
            criticalManeuversCount = criticalCount,
            turnsCount = maneuvers.count { isTurnManeuver(it) },
            roundaboutsFromManeuvers = maneuvers.count { it.contains("roundabout") },
            rampOrExitCount =
                maneuvers.count {
                    it.contains("ramp") || it.contains("exit") || it.startsWith("fork-")
                },
            restrictedRoadHintsCount =
                maneuvers.count { maneuver ->
                    RESTRICTED_ROAD_KEYWORDS.any { keyword -> maneuver.contains(keyword) }
                }.takeIf { count -> count > 0 },
        )
    }

    internal fun isCriticalManeuver(maneuver: String): Boolean =
        CRITICAL_MANEUVER_KEYWORDS.any { keyword -> maneuver.contains(keyword) }

    private fun isTurnManeuver(maneuver: String): Boolean =
        maneuver.startsWith("turn-") ||
            maneuver.contains("roundabout") ||
            maneuver.contains("uturn") ||
            maneuver.startsWith("fork-") ||
            maneuver.startsWith("ramp-")
}

/** Features derived from the Google Directions route itself (steps / maneuvers). */
data class GoogleRouteFeatures(
    val googleSourceStatus: SourceStatus,
    val maneuvers: GoogleManeuverEvidence?,
    val metadata: SourceMetadata?,
    val errorMessage: String? = null,
)

object GoogleRouteFeatureReader {

    fun read(route: RawRouteForIntelligence): GoogleRouteFeatures {
        val metadata =
            SourceMetadata(
                provider = PROVIDER,
                fetchedAt = Instant.now(),
                confidence = 0.95f,
            )

        if (route.googleSteps.isEmpty()) {
            val precomputed = route.criticalManeuversCount
            return GoogleRouteFeatures(
                googleSourceStatus =
                    if (precomputed != null) {
                        SourceStatus.PARTIAL
                    } else {
                        SourceStatus.UNAVAILABLE
                    },
                maneuvers =
                    precomputed?.let { count ->
                        GoogleManeuverEvidence(
                            criticalManeuversCount = count,
                            turnsCount = null,
                            roundaboutsFromManeuvers = null,
                            rampOrExitCount = null,
                            restrictedRoadHintsCount = null,
                        )
                    },
                metadata = metadata,
                errorMessage = if (precomputed == null) "Google steps unavailable" else null,
            )
        }

        val maneuverEvidence = GoogleRouteSignalsExtractor.extract(route.googleSteps)
        val criticalCount =
            maneuverEvidence.criticalManeuversCount
                ?: route.criticalManeuversCount

        return GoogleRouteFeatures(
            googleSourceStatus = SourceStatus.OK,
            maneuvers = maneuverEvidence.copy(criticalManeuversCount = criticalCount),
            metadata = metadata,
        )
    }

    const val PROVIDER = "Google Directions"
}
