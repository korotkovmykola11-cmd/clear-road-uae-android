package com.clearroad.app.guidance

/** Pure step index from distance traveled along MARSHIO guidance steps. */
internal object MarshioGuidanceStepTracker {

    fun currentStepIndex(
        traveledMeters: Double,
        steps: List<GuidanceStepUi>,
    ): Int {
        if (steps.isEmpty()) return -1

        val traveled = traveledMeters.coerceAtLeast(0.0)
        if (traveled <= 0.0) return 0

        var cumulativeMeters = 0.0
        for (index in steps.indices) {
            cumulativeMeters += steps[index].distanceMeters.coerceAtLeast(0)
            if (traveled < cumulativeMeters) {
                return index
            }
        }
        return steps.lastIndex
    }
}
