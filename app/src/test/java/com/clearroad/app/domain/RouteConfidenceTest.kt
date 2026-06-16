package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteConfidenceTest {

    private fun routes(vararg durations: Int) =
        durations.map { duration ->
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "${duration / 60} mins",
                distanceMeters = 10_000,
                durationSeconds = duration,
                tollAED = 0,
                hasToll = false,
            )
        }

    @Test
    fun spreadUnder90Seconds_isLow() {
        val trip = routes(8 * 60, 8 * 60 + 60)
        val result = RouteConfidence.fromAdvantageOverNext(trip, 0)
        assertEquals(RouteConfidence.Level.LOW, result.level)
        assertFalse(result.isHighConfidence)
    }

    @Test
    fun spreadOver180Seconds_isHigh() {
        val trip = routes(25 * 60, 34 * 60)
        val result = RouteConfidence.fromAdvantageOverNext(trip, 0)
        assertEquals(RouteConfidence.Level.HIGH, result.level)
        assertTrue(result.isHighConfidence)
    }
}
