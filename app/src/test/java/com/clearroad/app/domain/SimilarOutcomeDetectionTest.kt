package com.clearroad.app.legacy

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SimilarOutcomeDetectionTest {

    private fun route(minutes: Int, tollAed: Int) =
        SimilarOutcomeDetection.SimilarOutcomeRouteInput(
            durationSeconds = minutes * 60,
            tollAed = tollAed,
        )

    @Test
    fun exampleA_fastestRecommendedArrivesSlightlyEarlier() {
        val routes = listOf(
            route(31, 0),
            route(32, 0),
            route(40, 0),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
            )

        assertNotNull(guidance)
        assertEquals("Similar outcome", guidance!!.choice)
        assertEquals(
            "Fastest arrives slightly earlier, but the difference is unlikely to matter.",
            guidance.why,
        )
    }

    @Test
    fun fastestEqualTimesUseComfortMessage() {
        val routes = listOf(
            route(31, 0),
            route(31, 0),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
            )

        assertNotNull(guidance)
        assertEquals(
            "Travel times are nearly identical. Choose whichever route feels more comfortable.",
            guidance!!.why,
        )
    }

    @Test
    fun exampleB_saveAedExplainsSalikTradeoff() {
        val routes = listOf(
            route(28, 8),
            route(29, 0),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.NO_TOLLS,
            )

        assertNotNull(guidance)
        assertEquals("Save AED avoids Salik with only 1 extra minute.", guidance!!.why)
    }

    @Test
    fun saveAedSameTollUsesSalikParityMessage() {
        val routes = listOf(
            route(30, 8),
            route(31, 8),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.NO_TOLLS,
            )

        assertNotNull(guidance)
        assertEquals("Salik cost is nearly the same across these routes.", guidance!!.why)
    }

    @Test
    fun exampleC_calmExplainsCalmerTradeoff() {
        val routes = listOf(
            route(25, 0),
            route(27, 0),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.CALM,
            )

        assertNotNull(guidance)
        assertEquals(
            "Smooth Drive offers a calmer route with almost identical arrival time.",
            guidance!!.why,
        )
    }

    @Test
    fun exampleD_doesNotTriggerWhenSalikGapIsLarge() {
        val routes = listOf(
            route(28, 0),
            route(29, 16),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
            )

        assertNull(guidance)
    }

    @Test
    fun doesNotTriggerWhenTimeGapExceedsTwoMinutes() {
        val routes = listOf(
            route(25, 0),
            route(28, 0),
        )

        val guidance =
            SimilarOutcomeDetection.detect(
                routes = routes,
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
            )

        assertNull(guidance)
    }

    @Test
    fun doesNotTriggerWithSingleRoute() {
        val guidance =
            SimilarOutcomeDetection.detect(
                routes = listOf(route(30, 0)),
                recommendedIndex = 0,
                mode = PreferenceMode.FASTEST,
            )

        assertNull(guidance)
    }
}
