package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeExplanationPolicyTest {

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
    fun saveAed_allTollFree_copyIsConsistent() {
        val trip = routes(23 * 60, 26 * 60)
        val copy =
            ModeExplanationPolicy.homeCardCopy(
                mode = PreferenceMode.NO_TOLLS,
                recommended = trip[0],
                routes = trip,
                recommendedIndex = 0,
            )
        assertEquals("No Salik difference", copy.reasonChip)
        assertEquals("All options avoid Salik.", copy.summary)
        assertEquals("Time is the main difference here.", copy.narrative)
        assertTrue(copy.narrative.contains("Lower Salik").not())
        assertTrue(copy.narrative.contains("No Salik on this route").not())
    }

    @Test
    fun saveAed_salikDiffers_summaryAndNarrativeAlign() {
        val trip = routes(25 * 60, 28 * 60, tolls = intArrayOf(0, 16))
        val copy =
            ModeExplanationPolicy.homeCardCopy(
                mode = PreferenceMode.NO_TOLLS,
                recommended = trip[0],
                routes = trip,
                recommendedIndex = 0,
            )
        assertEquals("Lower Salik cost.", copy.summary)
        assertEquals("No Salik on this route; other options may cost more.", copy.narrative)
    }

    @Test
    fun fastest_copyIsConsistent() {
        val trip = routes(25 * 60, 30 * 60)
        val copy =
            ModeExplanationPolicy.homeCardCopy(
                mode = PreferenceMode.FASTEST,
                recommended = trip[0],
                routes = trip,
                recommendedIndex = 0,
            )
        assertEquals("+5 min advantage", copy.reasonChip)
        assertEquals("Quickest on this list.", copy.summary)
        assertEquals("No Salik on this route.", copy.narrative)
    }
}
