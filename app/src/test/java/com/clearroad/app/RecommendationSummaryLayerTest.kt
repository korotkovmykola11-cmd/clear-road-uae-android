package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationSummaryLayerTest {

    private fun routes(vararg durations: Int, tolls: IntArray = IntArray(durations.size) { 0 }) =
        durations.mapIndexed { index, duration ->
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "${duration / 60} mins",
                distanceMeters = 10_000,
                durationSeconds = duration,
                tollAED = tolls.getOrElse(index) { 0 },
                hasToll = tolls.getOrElse(index) { 0 } > 0,
            )
        }

    @Test
    fun fastestSummary() {
        val trip = routes(25 * 60, 28 * 60)
        assertEquals(
            "Quickest on this list.",
            RecommendationSummaryLayer.forMode(PreferenceMode.FASTEST, trip, 0),
        )
    }

    @Test
    fun saveAedSummary_whenSalikDiffers() {
        val trip = routes(25 * 60, 28 * 60, tolls = intArrayOf(4, 16))
        assertEquals(
            "Lower Salik cost.",
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS, trip, 0),
        )
    }

    @Test
    fun saveAedSummary_whenAllTollFree() {
        val trip = routes(8 * 60, 8 * 60 + 30)
        assertEquals(
            "All options avoid Salik.",
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS, trip, 0),
        )
    }

    @Test
    fun smoothDriveSummary_whenMatchesFastest() {
        val trip = routes(26 * 60, 28 * 60)
        assertEquals(
            "Smooth option matches fastest today.",
            RecommendationSummaryLayer.forMode(PreferenceMode.CALM, trip, 0),
        )
    }

    @Test
    fun smoothDriveSummary_whenNotFastest() {
        val trip = routes(28 * 60, 26 * 60)
        assertEquals(
            "Less affected by traffic slowdowns.",
            RecommendationSummaryLayer.forMode(PreferenceMode.CALM, trip, 0),
        )
    }
}
