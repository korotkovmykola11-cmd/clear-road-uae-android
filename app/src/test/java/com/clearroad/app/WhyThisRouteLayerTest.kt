package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhyThisRouteLayerTest {

    private fun route(
        durationSeconds: Int,
        tollAed: Int = 0,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "10 km",
            durationText = "${durationSeconds / 60} mins",
            distanceMeters = 10_000,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = tollAed > 0,
        )

    @Test
    fun fastestExplainsTimeAdvantage() {
        val routes =
            listOf(
                route(durationSeconds = 25 * 60),
                route(durationSeconds = 32 * 60),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.FASTEST,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertEquals("Quickest option", copy.title)
        assertTrue(copy.why.contains("quickest practical option"))
        assertTrue(copy.why.contains("7 minutes"))
    }

    @Test
    fun saveAed_allTollFree_usesHonestCopy() {
        val routes =
            listOf(
                route(durationSeconds = 23 * 60, tollAed = 0),
                route(durationSeconds = 26 * 60, tollAed = 0),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.NO_TOLLS,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertEquals("All options avoid Salik", copy.title)
        assertEquals("", copy.why)
    }

    @Test
    fun saveAedDoesNotClaimExactSalikWhenGoogleTollIsZero() {
        val routes =
            listOf(
                route(durationSeconds = 47 * 60, tollAed = 0),
                route(durationSeconds = 50 * 60, tollAed = 8),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.NO_TOLLS,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertEquals("Lower Salik cost", copy.title)
        assertTrue(copy.why.contains("No Salik on this route; other options may cost more."))
        assertTrue(copy.why.contains("Google estimates").not())
    }

    @Test
    fun saveAedUsesGoogleFareWhenPresent() {
        val routes =
            listOf(
                route(durationSeconds = 40 * 60, tollAed = 8),
                route(durationSeconds = 38 * 60, tollAed = 16),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.NO_TOLLS,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertTrue(copy.why.contains("About 8 AED in Salik"))
    }

    @Test
    fun smoothDriveMatchesFastest_usesHonestCopy() {
        val routes =
            listOf(
                route(durationSeconds = 26 * 60),
                route(durationSeconds = 28 * 60),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.CALM,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertEquals("Smooth option matches fastest", copy.title)
        assertEquals("", copy.why)
    }

    @Test
    fun smoothDriveMentionsExtraMinutesWhenSlowerThanFastest() {
        val routes =
            listOf(
                route(durationSeconds = 50 * 60),
                route(durationSeconds = 47 * 60),
            )
        val copy =
            WhyThisRouteLayer.recommendedRouteCopy(
                mode = PreferenceMode.CALM,
                recommended = routes[0],
                routes = routes,
                recommendedIndex = 0,
                directionsStatus = "OK",
            )
        assertEquals("SMOOTH DRIVE pick", copy.title)
        assertTrue(copy.why.contains("Picked for a steadier ETA"))
        assertTrue(copy.why.contains("3 extra minutes"))
    }
}
