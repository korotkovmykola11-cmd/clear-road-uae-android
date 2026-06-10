package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationSummaryLayerTest {

    @Test
    fun fastestSummary() {
        assertEquals(
            "Best time-focused pick.",
            RecommendationSummaryLayer.forMode(PreferenceMode.FASTEST),
        )
    }

    @Test
    fun saveAedSummary() {
        assertEquals(
            "Reduces Salik exposure.",
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS),
        )
    }

    @Test
    fun smoothDriveSummary() {
        assertEquals(
            "Less delay added by traffic.",
            RecommendationSummaryLayer.forMode(PreferenceMode.CALM),
        )
    }
}
