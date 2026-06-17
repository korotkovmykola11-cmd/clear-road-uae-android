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
    fun fastest_omitsSalikNarrativeFromWhyBody() {
        val trip = routes(25 * 60, 30 * 60)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                PreferenceMode.FASTEST,
                trip[0],
                trip,
                0,
            )
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.FASTEST,
                trip[0],
                trip,
                0,
            )
        assertEquals("Quickest option", details.title)
        assertTrue(details.why.contains("quickest practical option"))
        assertTrue(details.why.contains("5 minutes"))
        assertTrue(details.why.contains(home.narrative).not())
    }

    @Test
    fun saveAed_allTollFree_keepsMeaningInTitleOnly() {
        val trip = routes(23 * 60, 26 * 60)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                PreferenceMode.NO_TOLLS,
                trip[0],
                trip,
                0,
            )
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.NO_TOLLS,
                trip[0],
                trip,
                0,
            )
        assertEquals("All options avoid Salik", details.title)
        assertEquals(home.summary.trimEnd('.'), details.title)
        assertTrue(details.why.contains(home.summary.trimEnd('.')).not())
        assertTrue(details.why.contains(home.narrative).not())
        assertEquals("", details.why)
    }

    @Test
    fun saveAed_allTollFree_keepsVersusFastestDeltaInWhy() {
        val trip = routes(26 * 60, 23 * 60)
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.NO_TOLLS,
                trip[0],
                trip,
                0,
            )
        assertEquals("All options avoid Salik", details.title)
        assertTrue(details.why.contains("3 extra minutes versus the fastest route"))
    }

    @Test
    fun smooth_matchesFastest_keepsMeaningInTitleOnly() {
        val trip = routes(26 * 60, 28 * 60)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                PreferenceMode.CALM,
                trip[0],
                trip,
                0,
            )
        val details =
            DetailsExplanationPolicy.recommendedRouteCopy(
                PreferenceMode.CALM,
                trip[0],
                trip,
                0,
            )
        assertEquals("Smooth option matches fastest", details.title)
        assertTrue(details.why.contains(home.narrative).not())
        assertEquals("", details.why)
    }
}
