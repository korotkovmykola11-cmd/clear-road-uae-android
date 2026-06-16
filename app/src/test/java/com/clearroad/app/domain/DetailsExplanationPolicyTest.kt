package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsExplanationPolicyTest {

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
    fun saveAed_allTollFree_alignsWithHomeMeaning() {
        val trip = routes(23 * 60, 26 * 60)
        val home = ModeExplanationPolicy.homeCardCopy(PreferenceMode.NO_TOLLS, trip[0], trip, 0)
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.NO_TOLLS,
                trip[0],
                trip,
                0,
            )
        assertEquals("All options avoid Salik", details.title)
        assertEquals(home.summary.trimEnd('.'), details.title)
        assertTrue(details.why.contains(home.narrative))
        assertTrue(details.why.contains(home.summary.trimEnd('.')))
    }

    @Test
    fun smooth_matchesFastest_alignsWithHomeNarrative() {
        val trip = routes(26 * 60, 28 * 60)
        val home = ModeExplanationPolicy.homeCardCopy(PreferenceMode.CALM, trip[0], trip, 0)
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.CALM,
                trip[0],
                trip,
                0,
            )
        assertEquals("Smooth option matches fastest", details.title)
        assertEquals(home.narrative, details.why)
    }
}
