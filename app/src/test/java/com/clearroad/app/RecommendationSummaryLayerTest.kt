package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationSummaryLayerTest {

    @Test
    fun fastestSummary() {
        assertEquals(
            "Gets you there sooner.",
            RecommendationSummaryLayer.forMode(PreferenceMode.FASTEST),
        )
    }

    @Test
    fun saveAedSummary() {
        assertEquals(
            "Avoids unnecessary Salik costs.",
            RecommendationSummaryLayer.forMode(PreferenceMode.NO_TOLLS),
        )
    }

    @Test
    fun smoothDriveSummary() {
        assertEquals(
            "Less stop-and-go driving.",
            RecommendationSummaryLayer.forMode(PreferenceMode.CALM),
        )
    }
}
