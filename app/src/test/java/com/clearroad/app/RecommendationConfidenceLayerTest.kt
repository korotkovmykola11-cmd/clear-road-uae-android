package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationConfidenceLayerTest {

    @Test
    fun fastestReturnsHighConfidenceCopy() {
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.FASTEST)
        assertEquals("High confidence", copy.title)
        assertEquals(
            "This route is clearly faster than the available alternatives.",
            copy.body,
        )
        assertTrue(copy.isHighConfidence)
    }

    @Test
    fun saveAedReturnsMediumConfidenceCopy() {
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.NO_TOLLS)
        assertEquals("Medium confidence", copy.title)
        assertEquals(
            "This route reduces Salik exposure, but may require a few extra minutes.",
            copy.body,
        )
        assertFalse(copy.isHighConfidence)
    }

    @Test
    fun smoothDriveReturnsMediumConfidenceCopy() {
        val copy = RecommendationConfidenceLayer.forMode(PreferenceMode.CALM)
        assertEquals("Medium confidence", copy.title)
        assertEquals(
            "This route favors steadier pacing over maximum speed.",
            copy.body,
        )
        assertFalse(copy.isHighConfidence)
    }
}
