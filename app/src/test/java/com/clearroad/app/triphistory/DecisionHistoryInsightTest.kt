package com.clearroad.app.triphistory

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionHistoryInsightTest {

    @Test
    fun compute_returnsNull_whenFewerThanThreePriorTrips() {
        val prior = listOf(trip(durationSeconds = 600), trip(durationSeconds = 620))
        val today = trip(durationSeconds = 900)

        assertNull(DecisionHistoryInsight.compute(prior, today))
    }

    @Test
    fun compute_typicalTime_whenWithinFifteenPercentOfMedian() {
        val prior =
            listOf(
                trip(durationSeconds = 600),
                trip(durationSeconds = 620),
                trip(durationSeconds = 640),
            )
        val today = trip(durationSeconds = 650)

        val result = DecisionHistoryInsight.compute(prior, today)!!

        assertEquals("Typical handoff pattern for this trip", result.durationLine)
        assertNull(result.salikLine)
    }

    @Test
    fun compute_slowerThanUsual_whenAboveFifteenPercentThreshold() {
        val prior =
            listOf(
                trip(durationSeconds = 600),
                trip(durationSeconds = 600),
                trip(durationSeconds = 600),
            )
        val today = trip(durationSeconds = 780)

        val result = DecisionHistoryInsight.compute(prior, today)!!

        assertEquals(
            "This option looks ~3 min slower than your usual handoff pattern",
            result.durationLine,
        )
    }

    @Test
    fun compute_fasterThanUsual_whenBelowFifteenPercentThreshold() {
        val prior =
            listOf(
                trip(durationSeconds = 1200),
                trip(durationSeconds = 1200),
                trip(durationSeconds = 1200),
            )
        val today = trip(durationSeconds = 900)

        val result = DecisionHistoryInsight.compute(prior, today)!!

        assertEquals(
            "This option looks ~5 min faster than your usual handoff pattern",
            result.durationLine,
        )
    }

    @Test
    fun compute_salikInsight_whenTodayIncludesSalikButUsuallyNot() {
        val prior =
            listOf(
                trip(durationSeconds = 600, hasSalik = false),
                trip(durationSeconds = 610, hasSalik = false),
                trip(durationSeconds = 620, hasSalik = false),
            )
        val today = trip(durationSeconds = 615, hasSalik = true)

        val result = DecisionHistoryInsight.compute(prior, today)!!

        assertEquals(
            "Your usual handoff pattern skips Salik; this option would include it",
            result.salikLine,
        )
    }

    @Test
    fun compute_salikInsight_whenTodayHasNoSalikButUsuallyDoes() {
        val prior =
            listOf(
                trip(durationSeconds = 600, hasSalik = true),
                trip(durationSeconds = 610, hasSalik = true),
                trip(durationSeconds = 620, hasSalik = false),
            )
        val today = trip(durationSeconds = 615, hasSalik = false)

        val result = DecisionHistoryInsight.compute(prior, today)!!

        assertEquals(
            "Your usual handoff pattern includes Salik; this option would skip it",
            result.salikLine,
        )
    }

    @Test
    fun medianDurationSeconds_usesMiddleValueForOddCount() {
        val trips =
            listOf(
                trip(durationSeconds = 100),
                trip(durationSeconds = 300),
                trip(durationSeconds = 500),
            )

        assertEquals(300, DecisionHistoryInsight.medianDurationSeconds(trips))
    }

    @Test
    fun medianDurationSeconds_averagesMiddlePairForEvenCount() {
        val trips =
            listOf(
                trip(durationSeconds = 600),
                trip(durationSeconds = 620),
                trip(durationSeconds = 640),
                trip(durationSeconds = 660),
            )

        assertEquals(630, DecisionHistoryInsight.medianDurationSeconds(trips))
    }

    @Test
    fun buildDurationLine_exactlyAtThreshold_isStillTypical() {
        val prior =
            listOf(
                trip(durationSeconds = 1000),
                trip(durationSeconds = 1000),
                trip(durationSeconds = 1000),
            )
        val atThreshold = (1000 * (1 + DecisionHistoryInsight.DURATION_DEVIATION_RATIO)).toInt()

        val line = DecisionHistoryInsight.buildDurationLine(prior, atThreshold)

        assertEquals("Typical handoff pattern for this trip", line)
    }

    @Test
    fun buildDurationLine_justAboveThreshold_isSlower() {
        val prior =
            listOf(
                trip(durationSeconds = 1000),
                trip(durationSeconds = 1000),
                trip(durationSeconds = 1000),
            )
        val aboveThreshold = (1000 * (1 + DecisionHistoryInsight.DURATION_DEVIATION_RATIO)).toInt() + 1

        val line = DecisionHistoryInsight.buildDurationLine(prior, aboveThreshold)

        assertTrue(line.contains("slower than your usual handoff pattern"))
    }

    @Test
    fun buildSalikLine_returnsNull_onStrictMajorityTie() {
        val prior =
            listOf(
                trip(durationSeconds = 600, hasSalik = true),
                trip(durationSeconds = 610, hasSalik = false),
                trip(durationSeconds = 620, hasSalik = true),
                trip(durationSeconds = 630, hasSalik = false),
            )

        assertNull(DecisionHistoryInsight.buildSalikLine(prior, todayHasSalik = true))
        assertNull(DecisionHistoryInsight.buildSalikLine(prior, todayHasSalik = false))
    }

    private fun trip(
        durationSeconds: Int,
        hasSalik: Boolean = false,
    ): TripHistoryEntry =
        TripHistoryEntry(
            timestamp = 1L,
            originKey = "25.20000,55.27000",
            destinationKey = "25.08000,55.14000",
            durationSeconds = durationSeconds,
            mode = PreferenceMode.FASTEST,
            hasSalik = hasSalik,
            tollAed = if (hasSalik) 8.0 else null,
        )
}
