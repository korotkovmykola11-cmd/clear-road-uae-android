package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationConfidenceLayerTest {

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
    fun fastestReturnsHighConfidenceWhenSpreadLarge() {
        val trip = routes(25 * 60, 34 * 60)
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.FASTEST, trip, 0)
        assertEquals("High confidence", copy.title)
        assertTrue(copy.isHighConfidence)
    }

    @Test
    fun fastestReturnsLowConfidenceWhenSpreadSmall() {
        val trip = routes(8 * 60, 8 * 60 + 60)
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.FASTEST, trip, 0)
        assertEquals("Similar options", copy.title)
        assertFalse(copy.isHighConfidence)
    }

    @Test
    fun saveAedReturnsSimilarWhenAllTollFree() {
        val trip = routes(23 * 60, 26 * 60)
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("Similar options", copy.title)
        assertFalse(copy.isHighConfidence)
    }

    @Test
    fun saveAedReturnsMediumWhenSalikDiffers() {
        val trip = routes(35 * 60, 27 * 60, tolls = intArrayOf(4, 16))
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("Medium confidence", copy.title)
        assertFalse(copy.isHighConfidence)
    }

    @Test
    fun smoothDriveReturnsSimilarWhenMatchesFastest() {
        val trip = routes(26 * 60, 28 * 60)
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.CALM, trip, 0)
        assertEquals("Similar options", copy.title)
        assertFalse(copy.isHighConfidence)
    }

    @Test
    fun smoothDriveReturnsMediumWhenNotFastest() {
        val trip = routes(28 * 60, 26 * 60)
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.CALM, trip, 0)
        assertEquals("Medium confidence", copy.title)
        assertFalse(copy.isHighConfidence)
    }
}
