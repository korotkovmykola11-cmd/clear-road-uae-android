package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePathDivergenceTest {

    @Test
    fun analyze_findsSplitWhenPathsFork() {
        val shared =
            listOf(
                LatLng(25.0000, 55.0000),
                LatLng(25.0010, 55.0010),
                LatLng(25.0020, 55.0020),
            )
        val googleTail =
            listOf(
                LatLng(25.0030, 55.0040),
                LatLng(25.0040, 55.0050),
            )
        val marshioTail =
            listOf(
                LatLng(25.0030, 55.0000),
                LatLng(25.0040, 54.9990),
            )
        val googlePath = shared + googleTail
        val marshioPath = shared + marshioTail

        val result = RoutePathDivergence.analyze(googlePath, marshioPath)

        assertTrue(result.hasDivergence)
        assertNotNull(result.splitPoint)
        assertEquals(shared, result.sharedPath)
        assertTrue(result.googleDivergentPath.size >= 2)
        assertTrue(result.marshioDivergentPath.size >= 2)
    }

    @Test
    fun analyze_noSplitForIdenticalPaths() {
        val path =
            listOf(
                LatLng(25.0000, 55.0000),
                LatLng(25.0010, 55.0010),
                LatLng(25.0020, 55.0020),
            )

        val result = RoutePathDivergence.analyze(path, path)

        assertEquals(false, result.hasDivergence)
    }
}
