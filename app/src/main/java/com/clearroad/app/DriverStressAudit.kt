package com.clearroad.app

import com.clearroad.app.domain.CalmStressTieBreak
import com.clearroad.app.domain.SmoothDriveScoring
import kotlin.math.max
import kotlin.math.roundToInt

internal data class DriverStressRouteMetrics(
    val routeIndex: Int,
    val isCurrentWinner: Boolean,
    val durationMin: Int,
    val durationInTrafficMin: Int,
    val trafficDelayMin: Int,
    val distanceKm: Double,
    val totalSteps: Int,
    val stepsPerKm: Double,
    val criticalManeuversCount: Int,
    val criticalManeuversPerKm: Double,
    val maxDensity2KmSegment: Int,
    val keywordDistribution: Map<String, Int>,
    val stressRankByCriticalCount: Int = 0,
    val stressRankByStepsPerKm: Int = 0,
    val stressRankByDensity: Int = 0,
)

internal data class DriverStressSessionSummary(
    val currentWinnerIndex: Int,
    val lowestStepsPerKmIndex: Int,
    val lowestCriticalManeuverIndex: Int,
    val timeLostMinIfLowestStressChosen: Int,
    val maneuversSavedPctIfLowestStressChosen: Int,
)

internal data class CalmStressCorrelation(
    val calmWinnerIndex: Int,
    val bestCriticalIndex: Int,
    val bestStepsPerKmIndex: Int,
    val bestDensityIndex: Int,
    val winnerMatchesBestCritical: Boolean,
    val winnerMatchesBestSteps: Boolean,
    val winnerMatchesBestDensity: Boolean,
)

internal data class CalmScoreBreakdownLog(
    val routeIndex: Int,
    val delayScore: Double,
    val corridorScore: Double,
    val trafficScore: Double,
    val distanceScore: Double,
    val finalScore: Double,
    val delaySignalInactiveGuardApplied: Boolean,
)

internal data class CalmAuditSummary(
    val winner: Int,
    val bestStressRoute: Int,
    val stressGapPct: Int,
    val timePenaltyMin: Int,
    val calmPickedLowestStress: Boolean,
)

internal object DriverStressAudit {

    internal const val DENSITY_WINDOW_METERS = 2_000

    internal val CRITICAL_MANEUVER_KEYWORDS =
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

    fun criticalManeuversCountFromRouteJson(routeJson: String): Int =
        keywordDistribution(extractStepRecordsFromRouteJson(routeJson)).values.sum()

    fun buildCalmStressInputs(
        routes: List<RealRouteDebugData>,
    ): List<CalmStressTieBreak.RouteStressInput>? {
        if (routes.any { it.criticalManeuversCount == null }) return null
        return routes.map { route ->
            CalmStressTieBreak.RouteStressInput(
                durationInTrafficMin = minutesRounded(trafficDurationSeconds(route)),
                criticalManeuversCount = route.criticalManeuversCount!!,
            )
        }
    }

    fun metricsFromRouteJson(
        routeJson: String,
        route: RealRouteDebugData,
        routeIndex: Int,
        isCurrentWinner: Boolean,
    ): DriverStressRouteMetrics =
        metricsFromSteps(
            steps = extractStepRecordsFromRouteJson(routeJson),
            route = route,
            routeIndex = routeIndex,
            isCurrentWinner = isCurrentWinner,
        )

    fun metricsFromSteps(
        steps: List<DirectionsStepRecord>,
        route: RealRouteDebugData,
        routeIndex: Int,
        isCurrentWinner: Boolean,
    ): DriverStressRouteMetrics {
        val distanceKm = route.distanceMeters.coerceAtLeast(0) / 1000.0
        val totalSteps = steps.size
        val keywordDistribution = keywordDistribution(steps)
        val criticalManeuversCount = keywordDistribution.values.sum()
        val durationInTrafficMin = minutesRounded(trafficDurationSeconds(route))
        val trafficDelayMin = minutesRounded(directionsAuditTrafficDelaySeconds(route))
        return DriverStressRouteMetrics(
            routeIndex = routeIndex,
            isCurrentWinner = isCurrentWinner,
            durationMin = minutesRounded(route.durationSeconds),
            durationInTrafficMin = durationInTrafficMin,
            trafficDelayMin = trafficDelayMin,
            distanceKm = distanceKm,
            totalSteps = totalSteps,
            stepsPerKm = perKm(totalSteps.toDouble(), distanceKm),
            criticalManeuversCount = criticalManeuversCount,
            criticalManeuversPerKm = perKm(criticalManeuversCount.toDouble(), distanceKm),
            maxDensity2KmSegment = maxCriticalDensityWindow(steps, DENSITY_WINDOW_METERS),
            keywordDistribution = keywordDistribution,
        )
    }

    fun buildSessionSummary(
        routeMetrics: List<DriverStressRouteMetrics>,
        currentWinnerIndex: Int,
    ): DriverStressSessionSummary? {
        if (routeMetrics.isEmpty()) return null
        val winnerIdx = currentWinnerIndex.coerceIn(0, routeMetrics.lastIndex)
        val lowestStepsPerKmIndex =
            routeMetrics.indices.minWith(
                compareBy<Int> { routeMetrics[it].stepsPerKm }.thenBy { it },
            )
        val lowestCriticalManeuverIndex =
            routeMetrics.indices.minWith(
                compareBy<Int> { routeMetrics[it].criticalManeuversCount }.thenBy { it },
            )
        val winner = routeMetrics[winnerIdx]
        val lowestStress = routeMetrics[lowestCriticalManeuverIndex]
        val timeLostMin =
            (lowestStress.durationInTrafficMin - winner.durationInTrafficMin).coerceAtLeast(0)
        val maneuversSavedPct =
            if (winner.criticalManeuversCount <= 0) {
                0
            } else {
                (
                    (winner.criticalManeuversCount - lowestStress.criticalManeuversCount)
                        .coerceAtLeast(0) *
                        100.0 / winner.criticalManeuversCount
                    ).roundToInt()
            }
        return DriverStressSessionSummary(
            currentWinnerIndex = winnerIdx,
            lowestStepsPerKmIndex = lowestStepsPerKmIndex,
            lowestCriticalManeuverIndex = lowestCriticalManeuverIndex,
            timeLostMinIfLowestStressChosen = timeLostMin,
            maneuversSavedPctIfLowestStressChosen = maneuversSavedPct,
        )
    }

    fun attachStressRanks(
        metrics: List<DriverStressRouteMetrics>,
    ): List<DriverStressRouteMetrics> {
        if (metrics.isEmpty()) return metrics
        val criticalRanks = rankAscending(metrics) { it.criticalManeuversCount.toDouble() }
        val stepsPerKmRanks = rankAscending(metrics) { it.stepsPerKm }
        val densityRanks = rankAscending(metrics) { it.maxDensity2KmSegment.toDouble() }
        return metrics.map { item ->
            item.copy(
                stressRankByCriticalCount = criticalRanks.getValue(item.routeIndex),
                stressRankByStepsPerKm = stepsPerKmRanks.getValue(item.routeIndex),
                stressRankByDensity = densityRanks.getValue(item.routeIndex),
            )
        }
    }

    fun buildCalmStressCorrelation(
        metrics: List<DriverStressRouteMetrics>,
        calmWinnerIndex: Int,
    ): CalmStressCorrelation? {
        if (metrics.isEmpty()) return null
        val winnerIdx = calmWinnerIndex.coerceIn(0, metrics.lastIndex)
        val bestCriticalIndex =
            metrics.indices.minWith(
                compareBy<Int> { metrics[it].criticalManeuversCount }.thenBy { it },
            )
        val bestStepsPerKmIndex =
            metrics.indices.minWith(
                compareBy<Int> { metrics[it].stepsPerKm }.thenBy { it },
            )
        val bestDensityIndex =
            metrics.indices.minWith(
                compareBy<Int> { metrics[it].maxDensity2KmSegment }.thenBy { it },
            )
        return CalmStressCorrelation(
            calmWinnerIndex = winnerIdx,
            bestCriticalIndex = bestCriticalIndex,
            bestStepsPerKmIndex = bestStepsPerKmIndex,
            bestDensityIndex = bestDensityIndex,
            winnerMatchesBestCritical = winnerIdx == bestCriticalIndex,
            winnerMatchesBestSteps = winnerIdx == bestStepsPerKmIndex,
            winnerMatchesBestDensity = winnerIdx == bestDensityIndex,
        )
    }

    fun buildCalmScoreBreakdowns(
        routes: List<RealRouteDebugData>,
    ): List<CalmScoreBreakdownLog> {
        val inputs =
            routes.map { route ->
                RouteRecommendationSelection.toSmoothDriveRouteInput(route)
            }
        if (inputs.any { it == null }) return emptyList()
        val smoothInputs = inputs.filterNotNull()
        val scores = SmoothDriveScoring.scoreAll(smoothInputs)
        return scores.mapIndexed { index, score ->
            CalmScoreBreakdownLog(
                routeIndex = index,
                delayScore = score.delayRatioComponent + score.delayMinComponent,
                corridorScore = score.corridorComponent,
                trafficScore = score.timePenaltyComponent,
                distanceScore = score.distanceComponent,
                finalScore = score.total,
                delaySignalInactiveGuardApplied = score.delaySignalInactiveGuardApplied,
            )
        }
    }

    fun buildCalmAuditSummary(
        metrics: List<DriverStressRouteMetrics>,
        calmWinnerIndex: Int,
    ): CalmAuditSummary? {
        if (metrics.isEmpty()) return null
        val winnerIdx = calmWinnerIndex.coerceIn(0, metrics.lastIndex)
        val bestStressRoute =
            metrics.indices.minWith(
                compareBy<Int> { metrics[it].criticalManeuversCount }.thenBy { it },
            )
        val winner = metrics[winnerIdx]
        val bestStress = metrics[bestStressRoute]
        val stressGapPct =
            if (winner.criticalManeuversCount <= 0) {
                0
            } else {
                (
                    (winner.criticalManeuversCount - bestStress.criticalManeuversCount)
                        .coerceAtLeast(0) *
                        100.0 / winner.criticalManeuversCount
                    ).roundToInt()
            }
        val timePenaltyMin =
            (bestStress.durationInTrafficMin - winner.durationInTrafficMin).coerceAtLeast(0)
        return CalmAuditSummary(
            winner = winnerIdx,
            bestStressRoute = bestStressRoute,
            stressGapPct = stressGapPct,
            timePenaltyMin = timePenaltyMin,
            calmPickedLowestStress = winnerIdx == bestStressRoute,
        )
    }

    internal fun rankAscending(
        metrics: List<DriverStressRouteMetrics>,
        value: (DriverStressRouteMetrics) -> Double,
    ): Map<Int, Int> {
        val ordered =
            metrics
                .map { it.routeIndex to value(it) }
                .sortedWith(compareBy<Pair<Int, Double>> { it.second }.thenBy { it.first })
        return ordered.mapIndexed { rankIndex, (routeIndex, _) ->
            routeIndex to (rankIndex + 1)
        }.toMap()
    }

    internal fun isCriticalManeuver(maneuver: String?): Boolean =
        matchedKeyword(maneuver) != null

    internal fun matchedKeyword(maneuver: String?): String? {
        if (maneuver.isNullOrBlank()) return null
        val normalized = maneuver.lowercase().replace('_', '-')
        return CRITICAL_MANEUVER_KEYWORDS.firstOrNull { keyword ->
            val token = keyword.replace(' ', '-')
            normalized.contains(token)
        }
    }

    internal fun keywordDistribution(
        steps: List<DirectionsStepRecord>,
    ): Map<String, Int> {
        val counts = linkedMapOf<String, Int>()
        steps.forEach { step ->
            matchedKeyword(step.maneuver)?.let { keyword ->
                counts[keyword] = (counts[keyword] ?: 0) + 1
            }
        }
        return counts
    }

    internal fun maxCriticalDensityWindow(
        steps: List<DirectionsStepRecord>,
        windowMeters: Int,
    ): Int {
        if (steps.isEmpty() || windowMeters <= 0) return 0
        val positions =
            steps.map { step ->
                StepWindowPosition(
                    distanceMeters = step.distanceMeters.coerceAtLeast(0),
                    isCritical = isCriticalManeuver(step.maneuver),
                )
            }
        var cumulative = 0
        val anchored =
            positions.map { position ->
                val start = cumulative
                cumulative += position.distanceMeters
                AnchoredStep(startMeters = start, isCritical = position.isCritical)
            }
        var maxCount = 0
        for (index in anchored.indices) {
            val windowStart = anchored[index].startMeters
            val windowEnd = windowStart + windowMeters
            val count =
                anchored.count { step ->
                    step.isCritical && step.startMeters in windowStart until windowEnd
                }
            maxCount = max(maxCount, count)
        }
        return maxCount
    }

    private data class StepWindowPosition(
        val distanceMeters: Int,
        val isCritical: Boolean,
    )

    private data class AnchoredStep(
        val startMeters: Int,
        val isCritical: Boolean,
    )

    private fun trafficDurationSeconds(route: RealRouteDebugData): Int =
        route.durationInTrafficSeconds?.takeIf { it > 0 } ?: route.durationSeconds

    private fun perKm(value: Double, distanceKm: Double): Double =
        if (distanceKm <= 0.0) 0.0 else value / distanceKm

    private fun minutesRounded(seconds: Int): Int = (seconds.coerceAtLeast(0) + 59) / 60
}
