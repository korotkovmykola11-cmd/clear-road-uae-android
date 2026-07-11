package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometrySimilarityTest {

    private val routeA =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
            LatLng(25.02, 55.0),
        )

    @Test
    fun identicalRoutes_areDuplicateHypothesis() {
        val result = GeometrySimilarity.compare(routeA, routeA)

        assertTrue(result.isValid)
        assertEquals(1.0, result.sharedPercentageOfShorter, 0.01)
        assertTrue(result.isDuplicateHypothesis)
        assertFalse(result.isGenuinelyDifferentHypothesis)
    }

    @Test
    fun partiallyOverlappingRoutes_haveIntermediateSharedPercentage() {
        val routeB =
            listOf(
                LatLng(25.01, 55.0),
                LatLng(25.02, 55.0),
                LatLng(25.03, 55.0),
            )

        val result = GeometrySimilarity.compare(routeA, routeB)

        assertTrue(result.isValid)
        assertTrue(result.sharedPercentageOfShorter > 0.2)
        assertTrue(result.sharedPercentageOfShorter < 0.95)
    }

    @Test
    fun clearlyDifferentRoutes_areGenuinelyDifferentHypothesis() {
        val routeB =
            listOf(
                LatLng(25.0, 55.02),
                LatLng(25.01, 55.02),
                LatLng(25.02, 55.02),
            )

        val result = GeometrySimilarity.compare(routeA, routeB)

        assertTrue(result.isValid)
        assertTrue(result.sharedPercentageOfShorter < GeometrySimilarity.GENUINELY_DIFFERENT_SHARED_PCT_HYPOTHESIS)
        assertTrue(result.isGenuinelyDifferentHypothesis)
        assertFalse(result.isDuplicateHypothesis)
    }

    @Test
    fun emptyOrInvalidGeometry_isNotValid() {
        val empty = emptyList<LatLng>()
        val single = listOf(LatLng(25.0, 55.0))

        val emptyResult = GeometrySimilarity.compare(empty, routeA)
        val singleResult = GeometrySimilarity.compare(single, routeA)

        assertFalse(emptyResult.isValid)
        assertEquals(0.0, emptyResult.sharedPercentageOfShorter, 0.0)
        assertFalse(singleResult.isValid)
    }
}
