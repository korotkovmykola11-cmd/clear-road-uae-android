package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SmoothDriveScoring

/**
 * Home route recommendation index selection.
 * FASTEST and SAVE AED use legacy cost/time scoring; CALM uses [SmoothDriveScoring].
 */
internal object RouteRecommendationSelection {

    private const val SMOOTH_LOG_TAG = "SmoothDriveScoring"

    fun toSmoothDriveRouteInput(item: RealRouteDebugData): SmoothDriveScoring.RouteInput? {
        val trafficSeconds =
            item.durationInTrafficSeconds?.takeIf { it > 0 }
                ?: item.durationSeconds.takeIf { it > 0 }
                ?: return null
        val baseSeconds =
            item.baseDurationSeconds.takeIf { it > 0 } ?: trafficSeconds
        val distanceMeters = item.distanceMeters.takeIf { it > 0 } ?: return null
        return SmoothDriveScoring.RouteInput(
            baseDurationSeconds = baseSeconds,
            durationInTrafficSeconds = trafficSeconds,
            distanceMeters = distanceMeters,
            corridorText = item.corridorScanText,
        )
    }

    fun pickRecommendedRouteIndex(
        routes: List<RealRouteDebugData>,
        mode: PreferenceMode,
    ): Int {
        if (routes.isEmpty()) return 0
        return when (mode) {
            PreferenceMode.CALM -> pickCalmRouteIndex(routes)
            PreferenceMode.FASTEST,
            PreferenceMode.NO_TOLLS,
            -> pickLegacyRouteIndex(routes, mode)
        }
    }

    fun logSmoothDriveSelection(
        routes: List<RealRouteDebugData>,
        winnerIndex: Int,
    ) {
        logSmoothAudit(routes)
    }

    fun logSmoothAudit(routes: List<RealRouteDebugData>) {
        if (routes.isEmpty()) return
        val inputs = routes.map { toSmoothDriveRouteInput(it) }
        if (inputs.any { it == null }) {
            Log.d(SMOOTH_LOG_TAG, "=== SMOOTH AUDIT ===")
            Log.d(SMOOTH_LOG_TAG, "skipped: incomplete traffic/base data for smooth scoring")
            return
        }
        val smoothInputs = inputs.filterNotNull()
        val scores = SmoothDriveScoring.scoreAll(smoothInputs)
        val fastestTrafficSeconds =
            smoothInputs.minOf { it.durationInTrafficSeconds }
        val timeBudgetMinutes =
            SmoothDriveScoring.timeBudgetMinutes(fastestTrafficSeconds)

        Log.d(SMOOTH_LOG_TAG, "=== SMOOTH AUDIT ===")
        Log.d(SMOOTH_LOG_TAG, "")
        routes.forEachIndexed { index, route ->
            val input = smoothInputs[index]
            val breakdown = scores[index]
            val baseMin = input.baseDurationSeconds / 60.0
            val trafficMin = input.durationInTrafficSeconds / 60.0
            val distanceKm = input.distanceMeters / 1000.0
            Log.d(SMOOTH_LOG_TAG, "Route $index")
            Log.d(SMOOTH_LOG_TAG, "Corridor: ${corridorAuditLabel(route.corridorScanText)}")
            Log.d(SMOOTH_LOG_TAG, "Base: ${formatMinutes(baseMin)}")
            Log.d(SMOOTH_LOG_TAG, "Traffic: ${formatMinutes(trafficMin)}")
            Log.d(SMOOTH_LOG_TAG, "Delay: ${formatMinutes(breakdown.delayMin)}")
            Log.d(SMOOTH_LOG_TAG, "DelayRatio: ${formatRatio(breakdown.delayRatio)}")
            Log.d(SMOOTH_LOG_TAG, "Distance km: ${"%.1f".format(distanceKm)}")
            Log.d(SMOOTH_LOG_TAG, "P_delayRatio: ${formatScore(breakdown.delayRatioComponent)}")
            Log.d(SMOOTH_LOG_TAG, "P_delayAbs: ${formatScore(breakdown.delayMinComponent)}")
            Log.d(SMOOTH_LOG_TAG, "P_time: ${formatScore(breakdown.timePenaltyComponent)}")
            Log.d(SMOOTH_LOG_TAG, "P_corridor: ${formatScore(breakdown.corridorComponent)}")
            Log.d(SMOOTH_LOG_TAG, "P_dist: ${formatScore(breakdown.distanceComponent)}")
            Log.d(SMOOTH_LOG_TAG, "SMOOTH_SCORE: ${formatScore(breakdown.total)}")
            Log.d(SMOOTH_LOG_TAG, "")
        }

        val fastestWinner = pickRecommendedRouteIndex(routes, PreferenceMode.FASTEST)
        val saveWinner = pickRecommendedRouteIndex(routes, PreferenceMode.NO_TOLLS)
        val smoothWinner = SmoothDriveScoring.pickWinnerIndex(smoothInputs)

        Log.d(SMOOTH_LOG_TAG, "FASTEST WINNER: $fastestWinner")
        Log.d(SMOOTH_LOG_TAG, "SAVE WINNER: $saveWinner")
        Log.d(SMOOTH_LOG_TAG, "SMOOTH WINNER: $smoothWinner")
        Log.d(SMOOTH_LOG_TAG, "")
        logSmoothExplanation(
            scores = scores,
            winnerIndex = smoothWinner,
            inputs = smoothInputs,
            timeBudgetMinutes = timeBudgetMinutes,
            fastestTrafficSeconds = fastestTrafficSeconds,
        )
    }

    private fun logSmoothExplanation(
        scores: List<SmoothDriveScoring.ScoreBreakdown>,
        winnerIndex: Int,
        inputs: List<SmoothDriveScoring.RouteInput>,
        timeBudgetMinutes: Double,
        fastestTrafficSeconds: Int,
    ) {
        val winner = scores[winnerIndex]
        val minDelayRatio = scores.minOf { it.delayRatio }
        val lowestDelayRatio = winner.delayRatio <= minDelayRatio + 0.000_001
        val lowestTotal = winner.total <= scores.minOf { it.total } + 0.001
        val winnerTraffic = inputs[winnerIndex].durationInTrafficSeconds
        val deltaMin =
            (winnerTraffic - fastestTrafficSeconds).coerceAtLeast(0) / 60.0
        val insideBudget = deltaMin <= timeBudgetMinutes + 0.001
        val primaryScores =
            scores.map { breakdown ->
                breakdown.delayRatioComponent +
                    breakdown.delayMinComponent +
                    breakdown.timePenaltyComponent
            }
        val winnerPrimary = primaryScores[winnerIndex]
        val tiedAtTotal =
            scores.count { kotlin.math.abs(it.total - winner.total) < 0.001 } > 1
        val corridorTieBreak =
            tiedAtTotal ||
                scores.withIndex().any { (index, breakdown) ->
                    index != winnerIndex &&
                        kotlin.math.abs(primaryScores[index] - winnerPrimary) < 0.01 &&
                        (
                            breakdown.corridorComponent != winner.corridorComponent ||
                                breakdown.distanceComponent != winner.distanceComponent
                            )
                }

        Log.d(SMOOTH_LOG_TAG, "SMOOTH EXPLANATION:")
        Log.d(
            SMOOTH_LOG_TAG,
            "- lowest delay ratio? ${if (lowestDelayRatio) "yes" else "no"}",
        )
        Log.d(
            SMOOTH_LOG_TAG,
            "- lowest total score? ${if (lowestTotal) "yes" else "no"}",
        )
        Log.d(
            SMOOTH_LOG_TAG,
            "- inside time budget? ${if (insideBudget) "yes" else "no"}",
        )
        Log.d(
            SMOOTH_LOG_TAG,
            "- corridor tie-break used? ${if (corridorTieBreak) "yes" else "no"}",
        )
    }

    private fun corridorAuditLabel(scan: String): String {
        val trimmed = scan.trim()
        if (trimmed.isEmpty()) return "(unknown)"
        val summaryEnd = trimmed.indexOf(" Head ")
        if (summaryEnd > 0) return trimmed.substring(0, summaryEnd).trim()
        if (trimmed.length <= 100) return trimmed
        return trimmed.take(100).trim() + "…"
    }

    private fun formatMinutes(minutes: Double): String =
        "%.1f min".format(minutes)

    private fun formatRatio(ratio: Double): String =
        "%.4f".format(ratio)

    private fun formatScore(score: Double): String =
        "%.2f".format(score)

    private fun pickCalmRouteIndex(routes: List<RealRouteDebugData>): Int {
        val inputs = routes.map { toSmoothDriveRouteInput(it) }
        if (inputs.all { it != null }) {
            return SmoothDriveScoring.pickWinnerIndex(inputs.filterNotNull())
        }
        return pickLegacyRouteIndex(routes, PreferenceMode.CALM)
    }

    private fun pickLegacyRouteIndex(
        routes: List<RealRouteDebugData>,
        mode: PreferenceMode,
    ): Int =
        routes.indices.minWith(
            compareBy(
                { idx ->
                    legacyRouteScore(routes[idx], mode)
                },
                { it },
            ),
        )

    private fun legacyRouteScore(
        item: RealRouteDebugData,
        mode: PreferenceMode,
    ): Double {
        val distanceKm = item.distanceMeters / 1000.0
        val effectiveToll = effectiveTollAedForScoring(item)
        val fuelAed = estimateFuelCostAed(distanceKm)
        val totalCostAed =
            estimateTotalRouteCostAed(effectiveToll, fuelAed).toDouble()
        return calculateLegacyRouteScore(
            mode = mode,
            durationMinutes = item.durationSeconds / 60,
            distanceKm = distanceKm,
            tollAed = effectiveToll.toDouble(),
            totalCostAed = totalCostAed,
        )
    }
}
