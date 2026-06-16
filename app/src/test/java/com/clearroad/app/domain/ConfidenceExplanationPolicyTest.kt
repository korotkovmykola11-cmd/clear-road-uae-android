package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfidenceExplanationPolicyTest {

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
    fun saveAllTollFreeBodyComposesFromMepMeaning() {
        val trip = routes(23 * 60, 26 * 60)
        val copy = ConfidenceExplanationPolicy.forMode(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("Similar options", copy.title)
        assertEquals(
            "All routes avoid Salik on this trip. Time is the main difference.",
            copy.body,
        )
    }

    @Test
    fun smoothMatchesFastestBodyComposesFromMepMeaning() {
        val trip = routes(26 * 60, 28 * 60)
        val copy = ConfidenceExplanationPolicy.forMode(PreferenceMode.CALM, trip, 0)
        assertEquals("Similar options", copy.title)
        assertEquals(
            "Smooth option matches the fastest route today. Traffic conditions are similar.",
            copy.body,
        )
    }

    @Test
    fun fastestHighConfidenceUsesRouteConfidenceOnly() {
        val trip = routes(25 * 60, 34 * 60)
        val copy = ConfidenceExplanationPolicy.forMode(PreferenceMode.FASTEST, trip, 0)
        assertEquals("High confidence", copy.title)
        assertEquals(
            "This route is clearly faster than the available alternatives.",
            copy.body,
        )
        assertEquals(true, copy.isHighConfidence)
    }

    @Test
    fun saveSalikDiffersStaysConfidenceSpecific() {
        val trip = routes(35 * 60, 27 * 60, tolls = intArrayOf(4, 16))
        val copy = ConfidenceExplanationPolicy.forMode(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("Medium confidence", copy.title)
        assertEquals(
            "This route keeps Salik cost down, but may require a few extra minutes.",
            copy.body,
        )
    }

    @Test
    fun smoothNotFastestStaysConfidenceSpecific() {
        val trip = routes(28 * 60, 26 * 60)
        val copy = ConfidenceExplanationPolicy.forMode(PreferenceMode.CALM, trip, 0)
        assertEquals("Medium confidence", copy.title)
        assertEquals(
            "Less affected by traffic slowdowns, even if not the fastest route.",
            copy.body,
        )
    }
}
