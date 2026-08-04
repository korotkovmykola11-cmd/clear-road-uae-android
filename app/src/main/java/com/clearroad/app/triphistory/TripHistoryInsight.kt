package com.clearroad.app.triphistory

import kotlin.math.abs
import kotlin.math.roundToInt

data class TripHistoryInsightResult(
    val durationLine: String?,
    val salikLine: String?,
) {
    val hasContent: Boolean =
        !durationLine.isNullOrBlank() || !salikLine.isNullOrBlank()
}

/**
 * Compares the current calculation against prior **same O-D pair + mode** handoff history.
 * Does not compare physical corridors (E11 vs E311) — only grid-matched origin/destination keys.
 */
object TripHistoryInsight {

    const val MIN_PRIOR_TRIPS = 3
    const val DURATION_DEVIATION_RATIO = 0.15

    fun compute(
        priorTrips: List<TripHistoryEntry>,
        today: TripHistoryEntry,
    ): TripHistoryInsightResult? {
        if (priorTrips.size < MIN_PRIOR_TRIPS) return null

        val durationLine = buildDurationLine(priorTrips, today.durationSeconds)
        val salikLine = buildSalikLine(priorTrips, today.hasSalik)
        val result = TripHistoryInsightResult(durationLine, salikLine)
        return result.takeIf { it.hasContent }
    }

    internal fun medianDurationSeconds(trips: List<TripHistoryEntry>): Int {
        val sorted = trips.map { it.durationSeconds }.sorted()
        if (sorted.isEmpty()) return 0
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt()
        } else {
            sorted[mid]
        }
    }

    internal fun buildDurationLine(
        priorTrips: List<TripHistoryEntry>,
        todayDurationSeconds: Int,
    ): String {
        val median = medianDurationSeconds(priorTrips)
        if (median <= 0) return "Typical time for this trip"
        val deltaRatio = abs(todayDurationSeconds - median).toDouble() / median.toDouble()
        if (deltaRatio <= DURATION_DEVIATION_RATIO) {
            return "Typical time for this trip"
        }
        val deltaMinutes = abs(todayDurationSeconds - median) / 60.0
        val minutesLabel = formatMinutes(deltaMinutes)
        return if (todayDurationSeconds > median) {
            "Today is $minutesLabel slower than usual for this trip"
        } else {
            "Today is $minutesLabel faster than usual for this trip"
        }
    }

    internal fun buildSalikLine(
        priorTrips: List<TripHistoryEntry>,
        todayHasSalik: Boolean,
    ): String? {
        val withSalik = priorTrips.count { it.hasSalik }
        val withoutSalik = priorTrips.size - withSalik
        val usualHasSalik = withSalik > withoutSalik
        val usualNoSalik = withoutSalik > withSalik
        return when {
            usualNoSalik && todayHasSalik ->
                "Usually no Salik on this trip, but today's option includes it"
            usualHasSalik && !todayHasSalik ->
                "Usually includes Salik on this trip, but today's option has none"
            else -> null
        }
    }

    private fun formatMinutes(minutes: Double): String {
        val rounded = minutes.roundToInt().coerceAtLeast(1)
        return if (rounded == 1) "1 min" else "$rounded min"
    }
}
