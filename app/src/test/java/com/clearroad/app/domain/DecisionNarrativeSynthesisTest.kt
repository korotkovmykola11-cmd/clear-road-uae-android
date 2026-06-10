package com.clearroad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionNarrativeSynthesisTest {

    @Test
    fun fastest_noToll_highConfidence_narrative() {
        assertEquals(
            "Best time-focused pick with stable timing and no Salik listed.",
            DecisionNarrativeSynthesis.narrative(
                mode = PreferenceMode.FASTEST,
                recommendedTollAed = 0,
                highConfidence = true,
            ),
        )
    }

    @Test
    fun confidenceDisplay_mapsFromHighConfidenceFlag() {
        assertEquals("Confidence 92%", DecisionNarrativeSynthesis.confidenceDisplayLabel(true))
        assertEquals("Confidence 78%", DecisionNarrativeSynthesis.confidenceDisplayLabel(false))
    }
}
