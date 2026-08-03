package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticMapPreviewUrlBuilderTest {

    @Test
    fun buildStaticMapUrl_includesPathsMarkersSizeAndKey() {
        val segments =
            listOf(
                TrafficSegment(
                    points =
                        listOf(
                            LatLng(25.0, 55.0),
                            LatLng(25.01, 55.01),
                            LatLng(25.02, 55.02),
                        ),
                    speedCategory = SpeedCategory.FREE,
                ),
                TrafficSegment(
                    points =
                        listOf(
                            LatLng(25.02, 55.02),
                            LatLng(25.03, 55.03),
                        ),
                    speedCategory = SpeedCategory.JAM,
                ),
            )
        val from = LatLng(25.0, 55.0)
        val to = LatLng(25.03, 55.03)

        val url = buildStaticMapUrl(segments, from, to, apiKey = "test-static-maps-key")

        assertTrue(url.startsWith("https://maps.googleapis.com/maps/api/staticmap?"))
        assertTrue(url.contains("path="))
        assertTrue(url.contains("color%3A0x2ECC71FF%7Cweight%3A5%7Cenc%3A"))
        assertTrue(url.contains("color%3A0xE74C3CFF%7Cweight%3A5%7Cenc%3A"))
        assertTrue(url.contains("markers=color%3Ablue%7Clabel%3AA%7C25.0%2C55.0"))
        assertTrue(url.contains("markers=color%3Ared%7Clabel%3AB%7C25.03%2C55.03"))
        assertTrue(url.contains("size=640x400"))
        assertTrue(url.contains("scale=2"))
        assertTrue(url.contains("maptype=roadmap"))
        assertTrue(url.contains("key=test-static-maps-key"))
        assertFalse(url.contains("center="))
        assertFalse(url.contains("zoom="))
    }

    @Test
    fun staticMapTrafficColors_matchRemovedTrafficPalette() {
        assertEquals("2ECC71", StaticMapTrafficColors.hexRgb(SpeedCategory.FREE))
        assertEquals("F1C40F", StaticMapTrafficColors.hexRgb(SpeedCategory.MODERATE))
        assertEquals("E67E22", StaticMapTrafficColors.hexRgb(SpeedCategory.SLOW))
        assertEquals("E74C3C", StaticMapTrafficColors.hexRgb(SpeedCategory.JAM))
        assertEquals("1D9E75", StaticMapTrafficColors.hexRgb(SpeedCategory.UNKNOWN))
    }

    @Test
    fun buildStaticMapUrl_skipsSegmentsWithFewerThanTwoPoints() {
        val url =
            buildStaticMapUrl(
                trafficSegments =
                    listOf(
                        TrafficSegment(
                            points = listOf(LatLng(25.0, 55.0)),
                            speedCategory = SpeedCategory.FREE,
                        ),
                    ),
                from = LatLng(25.0, 55.0),
                to = LatLng(25.1, 55.1),
                apiKey = "test-key",
            )

        assertFalse(url.contains("path="))
        assertTrue(url.contains("markers="))
    }

    @Test
    fun exampleUrl_forDocumentation() {
        val segments =
            listOf(
                TrafficSegment(
                    points =
                        listOf(
                            LatLng(25.2048, 55.2708),
                            LatLng(25.2100, 55.2800),
                            LatLng(25.2200, 55.2900),
                        ),
                    speedCategory = SpeedCategory.MODERATE,
                ),
                TrafficSegment(
                    points =
                        listOf(
                            LatLng(25.2200, 55.2900),
                            LatLng(25.2300, 55.3000),
                        ),
                    speedCategory = SpeedCategory.SLOW,
                ),
            )
        val url =
            buildStaticMapUrl(
                trafficSegments = segments,
                from = LatLng(25.2048, 55.2708),
                to = LatLng(25.2300, 55.3000),
                apiKey = "YOUR_API_KEY",
            )
        println("Static map example URL:\n$url")
        assertTrue(url.length < STATIC_MAP_URL_MAX_LENGTH)
    }
}
