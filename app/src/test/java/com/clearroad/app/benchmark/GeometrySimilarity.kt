package com.clearroad.app.benchmark

import com.clearroad.app.RoutePathDivergence
import com.clearroad.app.intelligence.RoutePolylineGeometry
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.min

/**
 * Stage 0A — geometry overlap between two route polylines.
 *
 * Thresholds are benchmark hypotheses, not production policy.
 */
object GeometrySimilarity {

    /** Benchmark hypothesis: routes with shared length ≥ this fraction are near-duplicates. */
    const val DUPLICATE_SHARED_PCT_HYPOTHESIS = 0.85

    /** Benchmark hypothesis: routes below this shared fraction are genuinely different by geometry. */
    const val GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS = 0.70

    data class Result(
        val pathALengthMeters: Double,
        val pathBLengthMeters: Double,
        val sharedLengthMeters: Double,
        val sharedPercentageOfShorter: Double,
        val isDuplicateHypothesis: Boolean,
        val isGenuinelyDifferentHypothesis: Boolean,
        val hasPathDivergence: Boolean,
        val isValid: Boolean,
    )

    fun compare(pathA: List<LatLng>, pathB: List<LatLng>): Result {
        val lengthA = pathLengthMeters(pathA)
        val lengthB = pathLengthMeters(pathB)

        if (pathA.size < 2 || pathB.size < 2 || lengthA <= 0.0 || lengthB <= 0.0) {
            return Result(
                pathALengthMeters = lengthA,
                pathBLengthMeters = lengthB,
                sharedLengthMeters = 0.0,
                sharedPercentageOfShorter = 0.0,
                isDuplicateHypothesis = false,
                isGenuinelyDifferentHypothesis = false,
                hasPathDivergence = false,
                isValid = false,
            )
        }

        val sharedAOnB = RoutePolylineGeometry.nearRouteLengthMeters(pathA, pathB)
        val sharedBOnA = RoutePolylineGeometry.nearRouteLengthMeters(pathB, pathA)
        val sharedLength = min(sharedAOnB, sharedBOnA)
        val shorterLength = min(lengthA, lengthB)
        val sharedPct =
            if (shorterLength > 0.0) {
                (sharedLength / shorterLength).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

        val divergence = RoutePathDivergence.analyze(pathA, pathB)

        return Result(
            pathALengthMeters = lengthA,
            pathBLengthMeters = lengthB,
            sharedLengthMeters = sharedLength,
            sharedPercentageOfShorter = sharedPct,
            isDuplicateHypothesis = sharedPct >= DUPLICATE_SHARED_PCT_HYPOTHESIS,
            isGenuinelyDifferentHypothesis = sharedPct < GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS,
            hasPathDivergence = divergence.hasDivergence,
            isValid = true,
        )
    }

    fun pathLengthMeters(path: List<LatLng>): Double {
        if (path.size < 2) return 0.0
        var total = 0.0
        for (index in 1 until path.size) {
            total += SphericalUtil.computeDistanceBetween(path[index - 1], path[index])
        }
        return total
    }
}
