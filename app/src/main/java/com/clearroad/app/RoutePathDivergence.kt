package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

/**
 * Finds where two route polylines split — presentation only for map evidence.
 */
internal object RoutePathDivergence {

    private const val TOGETHER_THRESHOLD_METERS = 45.0

    data class Result(
        val splitPoint: LatLng?,
        val sharedPath: List<LatLng>,
        val googleDivergentPath: List<LatLng>,
        val marshioDivergentPath: List<LatLng>,
        val hasDivergence: Boolean,
    )

    fun analyze(
        googlePath: List<LatLng>,
        marshioPath: List<LatLng>,
    ): Result {
        if (googlePath.size < 2 || marshioPath.size < 2) {
            return emptyResult()
        }

        val splitGoogleIndex = findSplitIndex(referencePath = googlePath, otherPath = marshioPath)
        val splitMarshioIndex = findSplitIndex(referencePath = marshioPath, otherPath = googlePath)
        val forkGoogleIndex = (splitGoogleIndex + 1).coerceAtMost(googlePath.lastIndex)
        val forkMarshioIndex = (splitMarshioIndex + 1).coerceAtMost(marshioPath.lastIndex)

        if (forkGoogleIndex >= googlePath.lastIndex && forkMarshioIndex >= marshioPath.lastIndex) {
            return emptyResult()
        }

        val rejoinGoogleIndex =
            findRejoinIndex(
                referencePath = googlePath,
                otherPath = marshioPath,
                forkStartIndex = forkGoogleIndex,
            )
        val rejoinMarshioIndex =
            findRejoinIndex(
                referencePath = marshioPath,
                otherPath = googlePath,
                forkStartIndex = forkMarshioIndex,
            )

        val sharedPath = googlePath.subList(0, (splitGoogleIndex + 1).coerceAtMost(googlePath.size))
        val googleDivergentPath =
            googlePath.subList(
                forkGoogleIndex,
                (rejoinGoogleIndex ?: googlePath.size).coerceAtMost(googlePath.size),
            )
        val marshioDivergentPath =
            marshioPath.subList(
                forkMarshioIndex,
                (rejoinMarshioIndex ?: marshioPath.size).coerceAtMost(marshioPath.size),
            )

        val splitPoint =
            if (sharedPath.isNotEmpty()) {
                sharedPath.last()
            } else {
                googlePath.getOrNull(splitGoogleIndex) ?: marshioPath.getOrNull(splitMarshioIndex)
            }

        val hasDivergence =
            googleDivergentPath.size >= 2 &&
                marshioDivergentPath.size >= 2 &&
                maxPathSeparationMeters(googleDivergentPath, marshioDivergentPath) >= TOGETHER_THRESHOLD_METERS

        return Result(
            splitPoint = splitPoint,
            sharedPath = sharedPath,
            googleDivergentPath = googleDivergentPath,
            marshioDivergentPath = marshioDivergentPath,
            hasDivergence = hasDivergence,
        )
    }

    private fun findSplitIndex(
        referencePath: List<LatLng>,
        otherPath: List<LatLng>,
    ): Int {
        for (index in 1 until referencePath.size) {
            if (closestDistanceMeters(referencePath[index], otherPath) > TOGETHER_THRESHOLD_METERS) {
                return (index - 1).coerceAtLeast(0)
            }
        }
        return 0
    }

    private fun findRejoinIndex(
        referencePath: List<LatLng>,
        otherPath: List<LatLng>,
        forkStartIndex: Int,
    ): Int? {
        for (index in (forkStartIndex + 1) until referencePath.size) {
            if (closestDistanceMeters(referencePath[index], otherPath) <= TOGETHER_THRESHOLD_METERS) {
                return index
            }
        }
        return null
    }

    private fun closestDistanceMeters(point: LatLng, path: List<LatLng>): Double =
        path.minOfOrNull { SphericalUtil.computeDistanceBetween(point, it) } ?: Double.MAX_VALUE

    private fun maxPathSeparationMeters(
        leftPath: List<LatLng>,
        rightPath: List<LatLng>,
    ): Double {
        val leftToRight = leftPath.maxOfOrNull { closestDistanceMeters(it, rightPath) } ?: 0.0
        val rightToLeft = rightPath.maxOfOrNull { closestDistanceMeters(it, leftPath) } ?: 0.0
        return maxOf(leftToRight, rightToLeft)
    }

    private fun emptyResult(): Result =
        Result(
            splitPoint = null,
            sharedPath = emptyList(),
            googleDivergentPath = emptyList(),
            marshioDivergentPath = emptyList(),
            hasDivergence = false,
        )
}
