package com.clearroad.app

/**
 * Stage 35.9 — travel time display for the Home recommendation card only.
 * Formats from route duration seconds; does not affect Route Details or scoring.
 */
internal object RecommendationTravelTimeFormatter {

    fun format(durationSeconds: Int): String {
        if (durationSeconds <= 0) return ""
        val totalMinutes = (durationSeconds + 59) / 60
        if (totalMinutes < 60) {
            return "$totalMinutes mins"
        }
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h %02dm".format(minutes)
    }
}
