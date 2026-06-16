package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionNarrativeSynthesisTest {

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
    fun fastest_noToll_narrative() {
        val trip = routes(25 * 60, 28 * 60)
        assertEquals(
            "No Salik on this route.",
            DecisionNarrativeSynthesis.narrative(
                mode = PreferenceMode.FASTEST,
                routes = trip,
                recommendedIndex = 0,
                recommendedTollAed = 0,
                highConfidence = true,
            ),
        )
    }

    @Test
    fun saveAed_allTollFree_narrative() {
        val trip = routes(23 * 60, 26 * 60)
        assertEquals(
            "Time is the main difference here.",
            DecisionNarrativeSynthesis.narrative(
                mode = PreferenceMode.NO_TOLLS,
                routes = trip,
                recommendedIndex = 0,
                recommendedTollAed = 0,
                highConfidence = false,
            ),
        )
    }

    @Test
    fun saveAed_withToll_narrative() {
        val trip = routes(35 * 60, 27 * 60, tolls = intArrayOf(4, 16))
        assertEquals(
            "About 4 AED in Salik.",
            DecisionNarrativeSynthesis.narrative(
                mode = PreferenceMode.NO_TOLLS,
                routes = trip,
                recommendedIndex = 0,
                recommendedTollAed = 4,
                highConfidence = false,
            ),
        )
    }

    @Test
    fun smoothDrive_matchesFastest_narrative() {
        val trip = routes(26 * 60, 28 * 60)
        assertEquals(
            "Traffic conditions are similar across these routes.",
            DecisionNarrativeSynthesis.narrative(
                mode = PreferenceMode.CALM,
                routes = trip,
                recommendedIndex = 0,
                recommendedTollAed = 0,
                highConfidence = false,
            ),
        )
    }

}
