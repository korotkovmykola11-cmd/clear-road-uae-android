package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SalikDetection
import java.util.Locale

/**
 * Stage 32.7 — one-line factual reason chip for the Home recommendation card.
 * Copy only; does not affect route selection or scoring.
 */
internal object RecommendationReasonLayer {

    fun reason(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String =
        when (mode) {
            PreferenceMode.FASTEST -> fastestReason(recommended, routes, recommendedIndex)
            PreferenceMode.NO_TOLLS -> saveAedReason(recommended)
            PreferenceMode.CALM -> "Lower traffic stress"
        }

    private fun fastestReason(
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): String {
        val nextAlternativeSeconds =
            routes.indices
                .filter { it != recommendedIndex }
                .minOfOrNull { routes[it].durationSeconds }
        val advantageMinutes =
            nextAlternativeSeconds?.let {
                minutesAdvantage(recommended.durationSeconds, it)
            } ?: 0
        return "+$advantageMinutes min advantage"
    }

    private fun saveAedReason(recommended: RealRouteDebugData): String {
        val gates = salikGateCount(recommended)
        return "$gates Salik gates"
    }

    private fun salikGateCount(recommended: RealRouteDebugData): Int {
        val scan = recommended.corridorScanText.lowercase(Locale.US)
        val tollRoadCount = SalikDetection.countOccurrences(scan, "toll road")
        if (tollRoadCount > 0) return tollRoadCount
        if (recommended.tollAED > 0) {
            return (recommended.tollAED + SalikDetection.AED_PER_TOLL_ROAD - 1) /
                SalikDetection.AED_PER_TOLL_ROAD
        }
        return 0
    }

    private fun minutesAdvantage(fasterSeconds: Int, slowerSeconds: Int): Int =
        ((slowerSeconds - fasterSeconds).coerceAtLeast(0) + 59) / 60
}
