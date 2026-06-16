package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationReasonLayerTest {

    private fun route(
        durationSeconds: Int,
        tollAed: Int = 0,
        corridorScanText: String = "",
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "10 km",
            durationText = "${durationSeconds / 60} mins",
            distanceMeters = 10_000,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            corridorScanText = corridorScanText,
        )

    @Test
    fun fastestReasonShowsMinuteAdvantage() {
        val routes =
            listOf(
                route(durationSeconds = 25 * 60),
                route(durationSeconds = 30 * 60),
            )
        assertEquals(
            "+5 min advantage",
            RecommendationReasonLayer.reason(
                mode = PreferenceMode.FASTEST,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
            ),
        )
    }

    @Test
    fun fastestReasonHidesZeroMinuteAdvantage() {
        val durationSeconds = 28 * 60
        val routes =
            listOf(
                route(durationSeconds = durationSeconds),
                route(durationSeconds = durationSeconds),
            )
        assertEquals(
            "",
            RecommendationReasonLayer.reason(
                mode = PreferenceMode.FASTEST,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
            ),
        )
    }

    @Test
    fun saveAedReasonShowsZeroSalikGates() {
        assertEquals(
            "0 Salik gates",
            RecommendationReasonLayer.reason(
                mode = PreferenceMode.NO_TOLLS,
                recommended = route(durationSeconds = 30 * 60, tollAed = 0),
                routes = emptyList(),
                recommendedIndex = 0,
            ),
        )
    }

    @Test
    fun smoothDriveReasonUsesLowestTrafficDelayWhenOnlyRoute() {
        assertEquals(
            "Least traffic slowdown",
            RecommendationReasonLayer.reason(
                mode = PreferenceMode.CALM,
                recommended = route(durationSeconds = 30 * 60),
                routes = emptyList(),
                recommendedIndex = 0,
            ),
        )
    }
}
