package com.clearroad.app.intelligence

object ComplexityScoreWeights {
    const val PER_TRAFFIC_SIGNAL = 0.035f
    const val PER_ROUNDABOUT = 0.08f
    const val LOCAL_ROAD_MULTIPLIER = 0.45f
    const val MAX_TRAFFIC_SIGNALS = 20
    const val MAX_ROUNDABOUTS = 8
}

object ComplexityScoreCalculator {

    fun calculate(
        trafficSignalsCount: Int?,
        roundaboutsCount: Int?,
        mainRoadRatio: Float?,
    ): Float? {
        if (trafficSignalsCount == null && roundaboutsCount == null && mainRoadRatio == null) {
            return null
        }
        val signalScore =
            ((trafficSignalsCount ?: 0).coerceAtMost(ComplexityScoreWeights.MAX_TRAFFIC_SIGNALS) *
                ComplexityScoreWeights.PER_TRAFFIC_SIGNAL)
        val roundaboutScore =
            ((roundaboutsCount ?: 0).coerceAtMost(ComplexityScoreWeights.MAX_ROUNDABOUTS) *
                ComplexityScoreWeights.PER_ROUNDABOUT)
        val localRoadRatio = 1f - (mainRoadRatio ?: 0.5f)
        val localRoadScore = localRoadRatio.coerceIn(0f, 1f) * ComplexityScoreWeights.LOCAL_ROAD_MULTIPLIER
        return (signalScore + roundaboutScore + localRoadScore).coerceIn(0f, 1f)
    }

    fun complexityLabel(score: Float?): String =
        when {
            score == null -> "Unknown complexity"
            score < 0.25f -> "Low complexity"
            score < 0.55f -> "Moderate complexity"
            else -> "Higher complexity"
        }
}
