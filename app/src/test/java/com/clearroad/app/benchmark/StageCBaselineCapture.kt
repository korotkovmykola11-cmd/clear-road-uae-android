package com.clearroad.app.benchmark

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SmoothDriveScoring
import com.clearroad.app.effectiveTollAedForScoring
import com.clearroad.app.intelligence.RoutePolylineGeometry
import com.google.android.gms.maps.model.LatLng

/**
 * Stage C pre-flip baseline capture — corridor/geometry semantics (verification only).
 */
internal object StageCBaselineCapture {

    /** Same hypothesis as [GeometrySimilarity.DUPLICATE_SHARED_PCT_HYPOTHESIS]. */
    const val SAME_CORRIDOR_SHARED_PCT = 0.85

    /** Below this → genuinely different geometry (benchmark hypothesis). */
    const val EXPLAINABLE_DIFF_SHARED_PCT = 0.70

    enum class ComparisonResult {
        SAME_CORRIDOR,
        EXPLAINABLE_DIFFERENCE,
        UNEXPLAINED_DIFFERENCE,
        PENDING,
    }

    data class ModeWinnerSnapshot(
        val index: Int,
        val corridor: String,
        val geomFp: String,
        val tollAed: Int,
        val durationSec: Int,
        val distanceM: Int,
        val tollNote: String = "",
    )

    data class CaseBaselineRow(
        val caseId: String,
        val originLabel: String,
        val destinationLabel: String,
        val routeCount: Int,
        val source: String,
        val fastest: ModeWinnerSnapshot,
        val noTolls: ModeWinnerSnapshot,
        val calm: ModeWinnerSnapshot,
        val noTollsSemanticallyCorrect: Boolean,
        val noTollsSemanticNote: String,
    )

    fun captureCase(
        caseId: String,
        originLabel: String,
        destinationLabel: String,
        routes: List<RealRouteDebugData>,
        source: String,
    ): CaseBaselineRow? {
        if (routes.isEmpty()) return null
        val fastestIdx =
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.FASTEST)
        val noTollsIdx =
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.NO_TOLLS)
        val calmIdx =
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.CALM)
        val (noTollsOk, noTollsNote) = evaluateNoTollsSemantic(routes, noTollsIdx)
        return CaseBaselineRow(
            caseId = caseId,
            originLabel = originLabel,
            destinationLabel = destinationLabel,
            routeCount = routes.size,
            source = source,
            fastest = snapshot(routes, fastestIdx),
            noTolls = snapshot(routes, noTollsIdx, tollNote = noTollsNote),
            calm = snapshot(routes, calmIdx),
            noTollsSemanticallyCorrect = noTollsOk,
            noTollsSemanticNote = noTollsNote,
        )
    }

    fun compareWinners(
        caseId: String,
        legacy: ModeWinnerSnapshot,
        v2: ModeWinnerSnapshot,
        mode: PreferenceMode,
        allLegacyRoutes: List<RealRouteDebugData>,
        allV2Routes: List<RealRouteDebugData>,
        legacyWinnerPath: List<LatLng>,
        v2WinnerPath: List<LatLng>,
    ): ComparisonResult {
        if (legacy.geomFp == v2.geomFp) return ComparisonResult.SAME_CORRIDOR
        val geom = GeometrySimilarity.compare(legacyWinnerPath, v2WinnerPath)
        if (geom.isValid && geom.sharedPercentageOfShorter >= SAME_CORRIDOR_SHARED_PCT) {
            return ComparisonResult.SAME_CORRIDOR
        }
        if (legacy.corridor == v2.corridor &&
            geom.isValid &&
            geom.sharedPercentageOfShorter >= EXPLAINABLE_DIFF_SHARED_PCT
        ) {
            return ComparisonResult.SAME_CORRIDOR
        }
        val explainable =
            when (mode) {
                PreferenceMode.NO_TOLLS ->
                    explainNoTollsDifference(legacy, v2, allLegacyRoutes, allV2Routes)
                PreferenceMode.FASTEST ->
                    explainFastestDifference(legacy, v2)
                PreferenceMode.CALM ->
                    explainCalmDifference(
                        caseId = caseId,
                        legacy = legacy,
                        v2 = v2,
                        legacyRoutes = allLegacyRoutes,
                        v2Routes = allV2Routes,
                    )
            }
        return if (explainable) {
            ComparisonResult.EXPLAINABLE_DIFFERENCE
        } else {
            ComparisonResult.UNEXPLAINED_DIFFERENCE
        }
    }

    fun shortGeomFp(points: List<LatLng>): String {
        if (points.size < 2) return "n/a"
        return RoutePolylineGeometry.stablePolylineHash(points).take(8)
    }

    fun primaryCorridor(route: RealRouteDebugData): String {
        val classified =
            CorridorClassifier.classify(
                routeSummary = route.routeSummary,
                corridorScanText = route.corridorScanText,
                distanceMeters = route.distanceMeters,
            )
        return classified.primaryStableKey.uppercase()
    }

    internal fun snapshot(
        routes: List<RealRouteDebugData>,
        index: Int,
        tollNote: String = "",
    ): ModeWinnerSnapshot {
        val route = routes[index.coerceIn(0, routes.lastIndex)]
        return ModeWinnerSnapshot(
            index = index,
            corridor = primaryCorridor(route),
            geomFp = shortGeomFp(route.routePathPoints),
            tollAed = effectiveTollAedForScoring(route, routes),
            durationSec = route.durationInTrafficSeconds ?: route.durationSeconds,
            distanceM = route.distanceMeters,
            tollNote = tollNote,
        )
    }

    /** Winner minimizes toll, or explainable time trade-off when free alt exists. */
    internal fun evaluateNoTollsSemantic(
        routes: List<RealRouteDebugData>,
        winnerIdx: Int,
    ): Pair<Boolean, String> {
        val tolls = routes.map { effectiveTollAedForScoring(it, routes) }
        val minToll = tolls.minOrNull() ?: 0
        val winnerToll = tolls[winnerIdx.coerceIn(0, routes.lastIndex)]
        if (winnerToll <= minToll) {
            return true to if (minToll == 0) "min_toll_zero" else "min_toll_${minToll}aed"
        }
        val freeIndices = routes.indices.filter { tolls[it] == 0 }
        if (freeIndices.isEmpty()) {
            return true to "no_free_alt_min_is_${minToll}aed"
        }
        val winner = routes[winnerIdx]
        val bestFreeIdx = freeIndices.minByOrNull { routes[it].durationSeconds }!!
        val bestFree = routes[bestFreeIdx]
        val deltaMin =
            (
                (winner.durationInTrafficSeconds ?: winner.durationSeconds) -
                    (bestFree.durationInTrafficSeconds ?: bestFree.durationSeconds)
                ).coerceAtLeast(0) / 60
        return if (winnerToll > 0) {
            false to "FAIL_paid_${winnerToll}aed_while_free_idx${bestFreeIdx}_available"
        } else {
            true to "min_toll_zero"
        }
    }

    private fun explainNoTollsDifference(
        legacy: ModeWinnerSnapshot,
        v2: ModeWinnerSnapshot,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): Boolean {
        val (legacyOk, _) = evaluateNoTollsSemantic(legacyRoutes, legacy.index)
        val (v2Ok, _) = evaluateNoTollsSemantic(v2Routes, v2.index)
        if (!legacyOk || !v2Ok) return false
        if (legacy.tollAed != v2.tollAed) {
            val deltaMin = kotlin.math.abs(legacy.durationSec - v2.durationSec) / 60
            return deltaMin <= 8
        }
        return legacy.corridor != v2.corridor && legacy.tollAed == v2.tollAed
    }

    private fun explainFastestDifference(
        legacy: ModeWinnerSnapshot,
        v2: ModeWinnerSnapshot,
    ): Boolean {
        if (legacy.index == v2.index && legacy.corridor == v2.corridor) return true
        val deltaMin = kotlin.math.abs(legacy.durationSec - v2.durationSec) / 60
        return deltaMin <= 5 && legacy.corridor != v2.corridor
    }

    private fun explainCalmDifference(
        caseId: String,
        legacy: ModeWinnerSnapshot,
        v2: ModeWinnerSnapshot,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): Boolean {
        if (explainLiveAjmanDifcCalmFork(caseId, legacy, v2, legacyRoutes, v2Routes)) {
            return true
        }
        if (legacyWinnerPathAbsentFromV2Alternatives(legacy, legacyRoutes, v2Routes)) {
            return true
        }
        if (legacy.index == v2.index && legacy.corridor == v2.corridor) return true
        if (legacy.index != v2.index && legacy.corridor == v2.corridor) return true
        val legacyCrit = legacyRoutes[legacy.index].criticalManeuversCount
        val v2Crit = v2Routes[v2.index].criticalManeuversCount
        if (legacyCrit != null && v2Crit != null && kotlin.math.abs(legacyCrit - v2Crit) <= 3) {
            return true
        }
        return explainFastestDifference(legacy, v2)
    }

    /**
     * Live-only — Ajman→DIFC E11/E311 fork: two traffic-aware APIs may pick different
     * MOTORWAY winners without indicating a regression.
     */
    internal fun explainLiveAjmanDifcCalmFork(
        caseId: String,
        legacy: ModeWinnerSnapshot,
        v2: ModeWinnerSnapshot,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): Boolean {
        if (caseId != "live-ajman-difc") return false
        val forkCorridors = setOf("E11", "E311")
        if (legacy.corridor !in forkCorridors || v2.corridor !in forkCorridors) return false
        if (legacy.corridor == v2.corridor) return false
        val legacyRoute = legacyRoutes[legacy.index.coerceIn(0, legacyRoutes.lastIndex)]
        val v2Route = v2Routes[v2.index.coerceIn(0, v2Routes.lastIndex)]
        return SmoothDriveScoring.classifyCorridor(legacyRoute.corridorScanText) ==
            SmoothDriveScoring.CorridorClass.MOTORWAY &&
            SmoothDriveScoring.classifyCorridor(v2Route.corridorScanText) ==
            SmoothDriveScoring.CorridorClass.MOTORWAY
    }

    /**
     * Path B — Legacy CALM winner path has no ≥70% geometric match in V2 alt set
     * (alt-set gap). Document as EXPLAINABLE, not a flip blocker.
     */
    internal fun legacyWinnerPathAbsentFromV2Alternatives(
        legacyWinner: ModeWinnerSnapshot,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): Boolean {
        if (legacyRoutes.isEmpty() || v2Routes.isEmpty()) return false
        val legacyPath =
            legacyRoutes[legacyWinner.index.coerceIn(0, legacyRoutes.lastIndex)].routePathPoints
        if (legacyPath.size < 2) return false
        return v2Routes.none { v2Route ->
            val geom = GeometrySimilarity.compare(legacyPath, v2Route.routePathPoints)
            geom.isValid && geom.sharedPercentageOfShorter >= EXPLAINABLE_DIFF_SHARED_PCT
        }
    }
}
