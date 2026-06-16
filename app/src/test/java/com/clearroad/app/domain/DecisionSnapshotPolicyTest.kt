package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionSnapshotPolicyTest {

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
    fun recommendedSummary_matchesModeExplanationPolicy() {
        val trip = routes(23 * 60, 26 * 60)
        val homeSummary =
            ModeExplanationPolicy.summary(PreferenceMode.NO_TOLLS, trip, 0)
        val snapshot =
            DecisionSnapshotPolicy.lines(
                mode = PreferenceMode.NO_TOLLS,
                recommendedRouteIndex = 0,
                routes = trip,
            )
        assertEquals(homeSummary, snapshot.recommendedSummary)
        assertEquals("Time is the main difference here.", snapshot.othersSummary)
    }

    @Test
    fun smooth_matchesFastest_othersSummaryUsesHomeNarrative() {
        val trip = routes(26 * 60, 28 * 60)
        val home =
            ModeExplanationPolicy.homeCardCopy(
                PreferenceMode.CALM,
                trip[0],
                trip,
                0,
            )
        val snapshot =
            DecisionSnapshotPolicy.lines(
                mode = PreferenceMode.CALM,
                recommendedRouteIndex = 0,
                routes = trip,
            )
        assertEquals(home.summary, snapshot.recommendedSummary)
        assertEquals(home.narrative, snapshot.othersSummary)
    }
}
