package com.clearroad.app

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
)

internal data class DriverStressSessionSummary(
    val currentWinnerIndex: Int,
    val lowestStepsPerKmIndex: Int,
    val lowestCriticalManeuverIndex: Int,
    val timeLostMinIfLowestStressChosen: Int,
    val maneuversSavedPctIfLowestStressChosen: Int,
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
