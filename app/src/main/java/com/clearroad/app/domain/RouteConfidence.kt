package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Presentation-only confidence from duration advantage vs next alternative.
 * Does not affect route selection or scoring.
 */
internal object RouteConfidence {

    enum class Level {
        LOW,
        MEDIUM,
        HIGH,
    }

    data class Result(
        val level: Level,
        val isHighConfidence: Boolean,
        val advantageSeconds: Int,
    )

    fun fromAdvantageOverNext(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Result {
        if (routes.size < 2) {
            return Result(level = Level.LOW, isHighConfidence = false, advantageSeconds = 0)
        }
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val recommendedDuration = routes[recIdx].durationSeconds
        val nextDuration =
            routes.indices
                .filter { it != recIdx }
                .minOfOrNull { routes[it].durationSeconds }
                ?: recommendedDuration
        val advantageSeconds =
            (nextDuration - recommendedDuration).coerceAtLeast(0)
        return fromSpreadSeconds(
            spreadSeconds = advantageSeconds,
            tripDurationSeconds = recommendedDuration.coerceAtLeast(1),
        )
    }

    internal fun fromSpreadSeconds(
        spreadSeconds: Int,
        @Suppress("UNUSED_PARAMETER") tripDurationSeconds: Int,
    ): Result {
        val level =
            when {
                spreadSeconds < PresentationThresholds.CONFIDENCE_LOW_MAX_SPREAD_SECONDS -> Level.LOW
                spreadSeconds <= PresentationThresholds.CONFIDENCE_MEDIUM_MAX_SPREAD_SECONDS ->
                    Level.MEDIUM
                else -> Level.HIGH
            }
        return Result(
            level = level,
            isHighConfidence = level == Level.HIGH,
            advantageSeconds = spreadSeconds,
        )
    }
}
