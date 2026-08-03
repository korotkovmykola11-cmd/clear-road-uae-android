package com.clearroad.app

import com.clearroad.app.benchmark.RoutesV2ResponseAdapterTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesV2FetchTest {

    @Test
    fun buildDirectionsFetchResultFromV2_mapsRoutesWithSpeedReadingIntervals() {
        val raw = RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json")
        val result = buildDirectionsFetchResultFromV2(raw)

        assertEquals("OK", result.status)
        assertTrue(result.routes.isNotEmpty())
        assertTrue(result.routes.first().trafficSpeedIntervals.isNotEmpty())
        assertTrue(result.routes.first().criticalManeuversCount != null)
    }
}
