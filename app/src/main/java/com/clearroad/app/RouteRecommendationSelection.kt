package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.CalmStressTieBreak
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
        val minDistanceMeters = smoothInputs.minOf { it.distanceMeters }
        val allDelaySignalsZero =
            scores.all { it.delayRatio <= 0.0 && it.delayMin <= 0.0 }
        val allTrafficFieldsMissing =
            routes.all { it.durationInTrafficSeconds == null }

        Log.d(SMOOTH_LOG_TAG, "=== SMOOTH AUDIT ===")
        Log.d(
            SMOOTH_LOG_TAG,
            "DATA_SOURCE leg.duration -> RealRouteDebugData.baseDurationSeconds; " +
                "leg.duration_in_traffic -> RealRouteDebugData.durationInTrafficSeconds " +
                "(null => traffic falls back to durationSeconds, usually equal to base)",
        )
        Log.d(
            SMOOTH_LOG_TAG,
            "fastestTrafficSeconds=$fastestTrafficSeconds " +
                "timeBudgetMinutes=${formatMinutes(timeBudgetMinutes)} " +
                "minDistanceMeters=$minDistanceMeters",
        )
        if (allTrafficFieldsMissing) {
            Log.w(
                SMOOTH_LOG_TAG,
                "DELAY_SIGNAL_INACTIVE reason=missing_duration_in_traffic " +
                    "all routes use fallback traffic=base => Delay and DelayRatio stay 0",
            )
        } else if (allDelaySignalsZero) {
            Log.w(
                SMOOTH_LOG_TAG,
                "DELAY_SIGNAL_INACTIVE reason=traffic_leq_base " +
                    "Google duration_in_traffic <= duration on every route at fetch time",
            )
        }
        if (scores.any { it.delaySignalInactiveGuardApplied }) {
            Log.w(
                SMOOTH_LOG_TAG,
                "DELAY_SIGNAL_INACTIVE_GUARD applied=true " +
                    "reason=all_delay_zero corridorWeight=guarded",
            )
        }
        Log.d(SMOOTH_LOG_TAG, "")
        routes.forEachIndexed { index, route ->
            val input = smoothInputs[index]
            val breakdown = scores[index]
            val delaySeconds =
                SmoothDriveScoring.trafficDelaySeconds(
                    input.baseDurationSeconds,
                    input.durationInTrafficSeconds,
                )
            Log.d(SMOOTH_LOG_TAG, "Route $index")
            Log.d(
                SMOOTH_LOG_TAG,
                "DATA baseDurationSeconds=${route.baseDurationSeconds} " +
                    "durationInTrafficSeconds=${route.durationInTrafficSeconds ?: "null"} " +
                    "durationSeconds=${route.durationSeconds} " +
                    "trafficSource=${smoothTrafficSourceLabel(route)}",
            )
            Log.d(
                SMOOTH_LOG_TAG,
                "Corridor: ${corridorAuditLabel(route.corridorScanText)} " +
                    "class=${SmoothDriveScoring.classifyCorridor(input.corridorText)}",
            )
            Log.d(
                SMOOTH_LOG_TAG,
                "FORMULA delaySeconds=max(0, trafficSec-baseSec)=max(0, " +
                    "${input.durationInTrafficSeconds}-${input.baseDurationSeconds})=$delaySeconds",
            )
            Log.d(
                SMOOTH_LOG_TAG,
                "FORMULA delayMin=delaySeconds/60=${formatMinutes(breakdown.delayMin)} " +
                    "delayRatio=delaySeconds/baseSec=$delaySeconds/${input.baseDurationSeconds}=" +
                    formatRatio(breakdown.delayRatio),
            )
            val deltaMinutes =
                (input.durationInTrafficSeconds - fastestTrafficSeconds)
                    .coerceAtLeast(0) / 60.0
            val timeOverrunMinutes =
                kotlin.math.max(0.0, deltaMinutes - timeBudgetMinutes)
            Log.d(
                SMOOTH_LOG_TAG,
                "FORMULA deltaMin=(trafficSec-fastestTrafficSec)/60=" +
                    formatMinutes(deltaMinutes) +
                    " timeOverrun=max(0, deltaMin-timeBudget)=" +
                    formatMinutes(timeOverrunMinutes),
            )
            Log.d(
                SMOOTH_LOG_TAG,
                "FORMULA P_delayRatio=delayRatio*${SmoothDriveScoring.DELAY_RATIO_WEIGHT}=" +
                    formatScore(breakdown.delayRatioComponent) +
                    " P_delayAbs=delayMin*${SmoothDriveScoring.DELAY_MIN_WEIGHT}=" +
                    formatScore(breakdown.delayMinComponent) +
                    " P_time=${SmoothDriveScoring.TIME_OVERRUN_MULTIPLIER}*" +
                    "timeOverrun^${SmoothDriveScoring.TIME_OVERRUN_EXPONENT}=" +
                    formatScore(breakdown.timePenaltyComponent) +
                    " P_corridor=${formatScore(breakdown.corridorComponent)}" +
                    " P_dist=${formatScore(breakdown.distanceComponent)}",
            )
            Log.d(
                SMOOTH_LOG_TAG,
                "SCORE total=${formatScore(breakdown.total)} " +
                    "= P_delayRatio + P_delayAbs + P_time + P_corridor + P_dist",
            )
            Log.d(SMOOTH_LOG_TAG, "")
        }

        val fastestWinner = pickRecommendedRouteIndex(routes, PreferenceMode.FASTEST)
        val saveWinner = pickRecommendedRouteIndex(routes, PreferenceMode.NO_TOLLS)
        val smoothWinner = SmoothDriveScoring.pickWinnerIndex(smoothInputs)

        Log.d(SMOOTH_LOG_TAG, "FASTEST WINNER: $fastestWinner")
        Log.d(SMOOTH_LOG_TAG, "SAVE WINNER: $saveWinner")
        if (allDelaySignalsZero && scores.any { it.corridorComponent != 0.0 || it.distanceComponent != 0.0 }) {
            Log.w(
                SMOOTH_LOG_TAG,
                "WINNER_DRIVER likely corridor/distance tie-break " +
                    "because P_delayRatio and P_delayAbs are 0 on all routes",
            )
        }
        Log.d(
            SMOOTH_LOG_TAG,
            "WINNER route=$smoothWinner score=${formatScore(scores[smoothWinner].total)}",
        )
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

    private fun smoothTrafficSourceLabel(route: RealRouteDebugData): String =
        when {
            route.durationInTrafficSeconds != null &&
                route.durationInTrafficSeconds > 0 ->
                "GOOGLE_duration_in_traffic"
            else ->
                "FALLBACK_durationSeconds"
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
            val smoothInputs = inputs.filterNotNull()
            val smoothWinner = SmoothDriveScoring.pickWinnerIndex(smoothInputs)
            val stressInputs = DriverStressAudit.buildCalmStressInputs(routes) ?: return smoothWinner
            val decision = CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs)
            return decision.selectedIndex
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
                    legacyRouteScore(routes[idx], mode, routes)
                },
                { it },
            ),
        )

    private fun legacyRouteScore(
        item: RealRouteDebugData,
        mode: PreferenceMode,
        routes: List<RealRouteDebugData>,
    ): Double {
        val distanceKm = item.distanceMeters / 1000.0
        val effectiveToll = effectiveTollAedForScoring(item, routes)
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
