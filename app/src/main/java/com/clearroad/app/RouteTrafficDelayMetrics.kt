package com.clearroad.app

import com.clearroad.app.domain.SmoothDriveScoring
import kotlin.math.roundToInt

/** Read-only traffic delay metrics for explanation copy only. */
internal object RouteTrafficDelayMetrics {

    fun trafficSeconds(item: RealRouteDebugData): Int =
        item.durationInTrafficSeconds?.takeIf { it > 0 }
            ?: item.durationSeconds.takeIf { it > 0 }
            ?: 0

    fun baseSeconds(item: RealRouteDebugData): Int =
        item.baseDurationSeconds.takeIf { it > 0 } ?: trafficSeconds(item)

    fun delayRatio(item: RealRouteDebugData): Double =
        SmoothDriveScoring.trafficDelayRatio(
            baseSeconds(item),
            trafficSeconds(item),
        )

    fun delayMinutesRounded(item: RealRouteDebugData): Int {
        val delaySeconds =
            SmoothDriveScoring.trafficDelaySeconds(
                baseSeconds(item),
                trafficSeconds(item),
            )
        return (delaySeconds + 59) / 60
    }

    fun delayRatioPercentRounded(item: RealRouteDebugData): Int =
        (delayRatio(item) * 100.0).roundToInt()

    fun isLowestDelayRatioAmong(
        item: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
    ): Boolean {
        if (routes.isEmpty()) return true
        val itemRatio = delayRatio(item)
        val minRatio = routes.minOf { delayRatio(it) }
        return itemRatio <= minRatio + 0.000_001
    }
}
