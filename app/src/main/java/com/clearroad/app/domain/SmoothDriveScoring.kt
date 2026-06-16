package com.clearroad.app.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Stage 35.2 — SMOOTH DRIVE scoring in testable isolation.
 * Predictability-first; no toll, fuel, total cost, or AED/min inputs.
 * Wired to Home CALM selection via [com.clearroad.app.RouteRecommendationSelection].
 *
 * Stage 35.8 — when traffic delay signal is inactive on all routes, corridor weight is
 * guarded so SMOOTH does not pick purely on motorway bonus.
 */
object SmoothDriveScoring {

    const val DELAY_RATIO_WEIGHT = 120.0
    const val DELAY_MIN_WEIGHT = 1.5
    const val TIME_BUDGET_MIN_MINUTES = 5.0
    const val TIME_BUDGET_MAX_MINUTES = 12.0
    const val TIME_BUDGET_RATIO = 0.15
    const val TIME_OVERRUN_EXPONENT = 2.0
    const val TIME_OVERRUN_MULTIPLIER = 2.0
    const val DISTANCE_TIE_BREAK_WEIGHT = 0.02

    const val INACTIVE_GUARD_TIME_WEIGHT = 1.0
    const val INACTIVE_GUARD_DISTANCE_WEIGHT = 0.05
    const val INACTIVE_GUARD_MOTORWAY_PENALTY = -0.25
    const val INACTIVE_GUARD_MIXED_PENALTY = 0.0
    const val INACTIVE_GUARD_URBAN_PENALTY = 0.25

    enum class CorridorClass {
        MOTORWAY,
        MIXED,
        URBAN_WEAVE,
    }

    data class RouteInput(
        val baseDurationSeconds: Int,
        val durationInTrafficSeconds: Int,
        val distanceMeters: Int,
        val corridorText: String = "",
    )

    data class ScoreBreakdown(
        val delayRatio: Double,
        val delayMin: Double,
        val delayRatioComponent: Double,
        val delayMinComponent: Double,
        val timePenaltyComponent: Double,
        val corridorComponent: Double,
        val distanceComponent: Double,
        val total: Double,
        val delaySignalInactiveGuardApplied: Boolean = false,
    )

    fun pickWinnerIndex(routes: List<RouteInput>): Int {
        require(routes.isNotEmpty()) { "routes must not be empty" }
        val scores = scoreAll(routes)
        var bestIndex = 0
        var bestScore = scores[0].total
        for (index in 1 until routes.size) {
            val score = scores[index].total
            if (score < bestScore || (score == bestScore && index < bestIndex)) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex
    }

    fun isDelaySignalInactive(routes: List<RouteInput>): Boolean {
        if (routes.isEmpty()) return false
        return routes.all { route ->
            trafficDelaySeconds(
                route.baseDurationSeconds,
                route.durationInTrafficSeconds,
            ) == 0
        }
    }

    fun scoreAll(routes: List<RouteInput>): List<ScoreBreakdown> {
        require(routes.isNotEmpty()) { "routes must not be empty" }
        val delaySignalInactive = isDelaySignalInactive(routes)
        val fastestTrafficSeconds =
            routes.minOf { it.durationInTrafficSeconds }
        val minDistanceMeters =
            routes.minOf { it.distanceMeters }
        val timeBudgetMinutes = timeBudgetMinutes(fastestTrafficSeconds)
        return routes.map { route ->
            scoreRoute(
                route = route,
                fastestTrafficSeconds = fastestTrafficSeconds,
                minDistanceMeters = minDistanceMeters,
                timeBudgetMinutes = timeBudgetMinutes,
                delaySignalInactive = delaySignalInactive,
            )
        }
    }

    fun scoreRoute(
        route: RouteInput,
        fastestTrafficSeconds: Int,
        minDistanceMeters: Int,
        timeBudgetMinutes: Double = timeBudgetMinutes(fastestTrafficSeconds),
        delaySignalInactive: Boolean = false,
    ): ScoreBreakdown {
        val baseSeconds = route.baseDurationSeconds.coerceAtLeast(1)
        val trafficSeconds = route.durationInTrafficSeconds
        val delaySeconds = trafficDelaySeconds(baseSeconds, trafficSeconds)
        val delayMin = delaySeconds / 60.0
        val delayRatio = delaySeconds.toDouble() / baseSeconds.toDouble()

        if (delaySignalInactive) {
            return scoreRouteInactiveGuard(
                route = route,
                fastestTrafficSeconds = fastestTrafficSeconds,
                minDistanceMeters = minDistanceMeters,
                delayMin = delayMin,
                delayRatio = delayRatio,
            )
        }

        val deltaMinutes =
            (trafficSeconds - fastestTrafficSeconds).coerceAtLeast(0) / 60.0
        val timeOverrunMinutes = max(0.0, deltaMinutes - timeBudgetMinutes)
        val timePenalty =
            TIME_OVERRUN_MULTIPLIER *
                timeOverrunMinutes.pow(TIME_OVERRUN_EXPONENT)

        val corridorComponent = corridorPenalty(classifyCorridor(route.corridorText))
        val distanceKm = route.distanceMeters / 1000.0
        val minDistanceKm = minDistanceMeters / 1000.0
        val distanceComponent =
            (distanceKm - minDistanceKm) * DISTANCE_TIE_BREAK_WEIGHT

        val delayRatioComponent = delayRatio * DELAY_RATIO_WEIGHT
        val delayMinComponent = delayMin * DELAY_MIN_WEIGHT
        val total =
            delayRatioComponent +
                delayMinComponent +
                timePenalty +
                corridorComponent +
                distanceComponent

        return ScoreBreakdown(
            delayRatio = delayRatio,
            delayMin = delayMin,
            delayRatioComponent = delayRatioComponent,
            delayMinComponent = delayMinComponent,
            timePenaltyComponent = timePenalty,
            corridorComponent = corridorComponent,
            distanceComponent = distanceComponent,
            total = total,
            delaySignalInactiveGuardApplied = false,
        )
    }

    private fun scoreRouteInactiveGuard(
        route: RouteInput,
        fastestTrafficSeconds: Int,
        minDistanceMeters: Int,
        delayMin: Double,
        delayRatio: Double,
    ): ScoreBreakdown {
        val trafficSeconds = route.durationInTrafficSeconds
        val deltaMinutes =
            (trafficSeconds - fastestTrafficSeconds).coerceAtLeast(0) / 60.0
        val timeComponent = deltaMinutes * INACTIVE_GUARD_TIME_WEIGHT
        val distanceKm = route.distanceMeters / 1000.0
        val minDistanceKm = minDistanceMeters / 1000.0
        val distanceComponent =
            max(0.0, distanceKm - minDistanceKm) * INACTIVE_GUARD_DISTANCE_WEIGHT
        val corridorComponent =
            guardedCorridorPenalty(classifyCorridor(route.corridorText))
        val total = timeComponent + distanceComponent + corridorComponent

        return ScoreBreakdown(
            delayRatio = delayRatio,
            delayMin = delayMin,
            delayRatioComponent = 0.0,
            delayMinComponent = 0.0,
            timePenaltyComponent = timeComponent,
            corridorComponent = corridorComponent,
            distanceComponent = distanceComponent,
            total = total,
            delaySignalInactiveGuardApplied = true,
        )
    }

    fun classifyCorridor(corridorText: String): CorridorClass {
        val lc = corridorText.lowercase(Locale.US)
        val urbanHits =
            URBAN_CORRIDOR_KEYWORDS.count { keyword -> lc.contains(keyword) }
        val motorwayHits =
            MOTORWAY_CORRIDOR_KEYWORDS.count { keyword -> lc.contains(keyword) }
        return when {
            urbanHits >= 2 && motorwayHits == 0 -> CorridorClass.URBAN_WEAVE
            motorwayHits > 0 && urbanHits == 0 -> CorridorClass.MOTORWAY
            motorwayHits > urbanHits -> CorridorClass.MOTORWAY
            urbanHits > motorwayHits && urbanHits >= 2 -> CorridorClass.URBAN_WEAVE
            else -> CorridorClass.MIXED
        }
    }

    fun timeBudgetMinutes(fastestTrafficSeconds: Int): Double {
        val fastestMinutes = fastestTrafficSeconds / 60.0
        return (TIME_BUDGET_RATIO * fastestMinutes)
            .coerceIn(TIME_BUDGET_MIN_MINUTES, TIME_BUDGET_MAX_MINUTES)
    }

    /** Read-only metric for explanation copy; does not affect scoring. */
    fun trafficDelaySeconds(
        baseDurationSeconds: Int,
        durationInTrafficSeconds: Int,
    ): Int {
        val baseSeconds = baseDurationSeconds.coerceAtLeast(1)
        return (durationInTrafficSeconds - baseSeconds).coerceAtLeast(0)
    }

    /** Read-only metric for explanation copy; does not affect scoring. */
    fun trafficDelayRatio(
        baseDurationSeconds: Int,
        durationInTrafficSeconds: Int,
    ): Double {
        val base = baseDurationSeconds.coerceAtLeast(1)
        return trafficDelaySeconds(baseDurationSeconds, durationInTrafficSeconds)
            .toDouble() / base.toDouble()
    }

    private fun corridorPenalty(corridorClass: CorridorClass): Double =
        when (corridorClass) {
            CorridorClass.MOTORWAY -> -4.0
            CorridorClass.MIXED -> 0.0
            CorridorClass.URBAN_WEAVE -> 5.0
        }

    private fun guardedCorridorPenalty(corridorClass: CorridorClass): Double =
        when (corridorClass) {
            CorridorClass.MOTORWAY -> INACTIVE_GUARD_MOTORWAY_PENALTY
            CorridorClass.MIXED -> INACTIVE_GUARD_MIXED_PENALTY
            CorridorClass.URBAN_WEAVE -> INACTIVE_GUARD_URBAN_PENALTY
        }

    private val MOTORWAY_CORRIDOR_KEYWORDS =
        listOf(
            "e11",
            "sheikh zayed",
            "szr",
            "e311",
            "e611",
            "emirates road",
            "emirates rd",
            "mohammed bin zayed",
            "sheikh mohammed bin zayed",
        )

    private val URBAN_CORRIDOR_KEYWORDS =
        listOf(
            "marina",
            "jumeirah",
            "local",
            "street",
            "satwa",
            "karama",
            "al wasl",
        )
}
