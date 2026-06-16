package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TripAtAGlancePolicyTest {

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
    fun fastestUsesMepSummaryAndSalikNarrative() {
        val trip = routes(25 * 60, 30 * 60, tolls = intArrayOf(8, 0))
        val (primary, secondary) = TripAtAGlancePolicy.lines(PreferenceMode.FASTEST, trip, 0)
        assertEquals("Quickest on this list.", primary)
        assertEquals("Salik 8 AED on this route.", secondary)
    }

    @Test
    fun saveAllTollFreeDoesNotUseSalikKeyFactorWording() {
        val trip = routes(23 * 60, 26 * 60)
        val (primary, secondary) = TripAtAGlancePolicy.lines(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("All options avoid Salik.", primary)
        assertEquals("Time is the main difference.", secondary)
        assertFalse(primary.contains("key factor", ignoreCase = true))
        assertFalse(secondary.orEmpty().contains("key factor", ignoreCase = true))
    }

    @Test
    fun saveSalikDiffersUsesMepMeaning() {
        val trip = routes(35 * 60, 27 * 60, tolls = intArrayOf(4, 16))
        val (primary, secondary) = TripAtAGlancePolicy.lines(PreferenceMode.NO_TOLLS, trip, 0)
        assertEquals("Lower Salik cost.", primary)
        assertEquals("About 4 AED in Salik.", secondary)
    }

    @Test
    fun smoothMatchesFastestUsesMepMeaning() {
        val trip = routes(26 * 60, 28 * 60)
        val (primary, secondary) = TripAtAGlancePolicy.lines(PreferenceMode.CALM, trip, 0)
        assertEquals("Smooth option matches fastest today.", primary)
        assertEquals("Traffic conditions are similar across these routes.", secondary)
    }

    @Test
    fun smoothNotFastestUsesMepMeaning() {
        val trip = routes(28 * 60, 26 * 60)
        val (primary, secondary) = TripAtAGlancePolicy.lines(PreferenceMode.CALM, trip, 0)
        assertEquals("Less affected by traffic slowdowns.", primary)
        assertEquals("Picked for a steadier ETA.", secondary)
    }

    @Test
    fun glanceSecondaryTrimsHereSuffix() {
        assertEquals(
            "Time is the main difference.",
            TripAtAGlancePolicy.glanceSecondary("Time is the main difference here."),
        )
    }
}
