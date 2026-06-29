package com.clearroad.app.guidance

import java.util.Locale

internal object MarshioGuidanceFormatting {

    fun formatDistance(meters: Double): String =
        if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        }

    fun formatDurationMinutes(seconds: Int): String {
        val minutes = (seconds / 60.0).coerceAtLeast(0.0)
        return if (minutes < 1.0) "< 1 min" else "${minutes.toInt()} min"
    }
}
