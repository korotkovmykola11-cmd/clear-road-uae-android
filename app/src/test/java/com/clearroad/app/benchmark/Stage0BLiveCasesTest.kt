package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage0BLiveCasesTest {

    @Test
    fun catalog_hasExactlyTenCases() {
        assertEquals(10, Stage0BLiveCases.all.size)
    }

    @Test
    fun catalog_hasUniqueCaseIds() {
        val ids = Stage0BLiveCases.all.map { it.caseId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun catalog_hasValidCoordinateRanges() {
        Stage0BLiveCases.all.forEach { case ->
            assertTrue(case.originLat in UAE_LAT_RANGE)
            assertTrue(case.destinationLat in UAE_LAT_RANGE)
            assertTrue(case.originLng in UAE_LNG_RANGE)
            assertTrue(case.destinationLng in UAE_LNG_RANGE)
        }
    }

    @Test
    fun catalog_hasNoDuplicateOrderedPairs() {
        val pairs =
            Stage0BLiveCases.all.map {
                orderedPairKey(it.originLat, it.originLng, it.destinationLat, it.destinationLng)
            }
        assertEquals(pairs.size, pairs.distinct().size)
    }

    @Test
    fun catalog_hasNoZeroLengthPairs() {
        Stage0BLiveCases.all.forEach { case ->
            val samePoint =
                case.originLat == case.destinationLat && case.originLng == case.destinationLng
            assertFalse("Zero-length O-D for ${case.caseId}", samePoint)
        }
    }

    @Test
    fun catalog_matchesAgreedLivePilotOrdering() {
        val labels =
            Stage0BLiveCases.all.map { "${it.originLabel} → ${it.destinationLabel}" }
        assertEquals(
            listOf(
                "DIFC → Dubai Marina",
                "Dubai Marina → Airport T3",
                "Sharjah → Downtown Dubai",
                "Ajman → DIFC",
                "JVC → Abu Dhabi",
                "Downtown Dubai → Abu Dhabi",
                "Downtown Dubai → Ajman",
                "Business Bay → JLT",
                "DIFC → Palm Jumeirah",
                "Dubai Marina → Deira",
            ),
            labels,
        )
    }

    private fun orderedPairKey(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): String = "$originLat,$originLng->$destinationLat,$destinationLng"

    private companion object {
        private val UAE_LAT_RANGE = 22.0..27.0
        private val UAE_LNG_RANGE = 51.0..57.0
    }
}
