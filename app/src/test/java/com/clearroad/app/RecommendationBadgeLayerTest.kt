package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationBadgeLayerTest {

    @Test
    fun fastestBadge() {
        assertEquals(
            "⚡ Recommended",
            RecommendationBadgeLayer.forMode(PreferenceMode.FASTEST),
        )
    }

    @Test
    fun saveAedBadge() {
        assertEquals(
            "💰 Best Value",
            RecommendationBadgeLayer.forMode(PreferenceMode.NO_TOLLS),
        )
    }

    @Test
    fun smoothDriveBadge() {
        assertEquals(
            "🌊 Most Relaxed",
            RecommendationBadgeLayer.forMode(PreferenceMode.CALM),
        )
    }
}
