package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalikScoringAlignmentTest {

    @Test
    fun heuristicSalikInactive_whenAllGoogleTollZero() {
        val routes =
            listOf(
                route(durationSec = 1500, scan = "Sheikh Zayed Rd/E11 Toll road Toll road"),
                route(durationSec = 1800, scan = "E311 local connector"),
            )
        assertTrue(heuristicSalikInactiveForTrip(routes))
        assertEquals(0, effectiveTollAedForScoring(routes[0], routes))
        assertEquals(0, effectiveTollAedForScoring(routes[1], routes))
    }

    @Test
    fun heuristicSalikActive_whenAnyGoogleTollPresent() {
        val routes =
            listOf(
                route(durationSec = 1500, scan = "Sheikh Zayed Rd/E11 Toll road", tollAed = 8),
                route(durationSec = 1800, scan = "E311 local connector", tollAed = 0),
            )
        assertTrue(heuristicSalikInactiveForTrip(routes).not())
        assertEquals(8, effectiveTollAedForScoring(routes[0], routes))
    }

    @Test
    fun saveAed_picksFastestWhenAllGoogleTollZero() {
        val routes =
            listOf(
                route(
                    durationSec = 1500,
                    distanceM = 24_600,
                    scan = "Sheikh Zayed Rd/E11 Sheikh Zayed Rd / E11 Toll road Toll road",
                ),
                route(
                    durationSec = 1920,
                    distanceM = 29_000,
                    scan = "Al Khail Rd/E44 Al Khail Rd / E44 then E11 Toll road",
                ),
            )
        assertEquals(
            0,
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.NO_TOLLS),
        )
    }

    private fun route(
        durationSec: Int,
        distanceM: Int = 10_000,
        scan: String = "",
        tollAed: Int = 0,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceM / 1000.0} km",
            durationText = "${durationSec / 60} min",
            distanceMeters = distanceM,
            durationSeconds = durationSec,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            corridorScanText = scan,
        )
}
