package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficPolylineBuilderTest {

    private val path =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
            LatLng(25.02, 55.0),
            LatLng(25.03, 55.0),
            LatLng(25.04, 55.0),
        )

    @Test
    fun buildFromPointIntervals_splitsPathBySpeedCategory() {
        val intervals =
            listOf(
                RouteSpeedInterval(0, 1, SpeedCategory.FREE),
                RouteSpeedInterval(2, 3, SpeedCategory.JAM),
                RouteSpeedInterval(3, 4, SpeedCategory.MODERATE),
            )

        val segments = TrafficPolylineBuilder.buildFromPointIntervals(path, intervals)

        assertEquals(3, segments.size)
        assertEquals(SpeedCategory.FREE, segments[0].speedCategory)
        assertEquals(2, segments[0].points.size)
        assertEquals(SpeedCategory.JAM, segments[1].speedCategory)
        assertEquals(2, segments[1].points.size)
        assertEquals(SpeedCategory.MODERATE, segments[2].speedCategory)
        assertEquals(2, segments[2].points.size)
    }

    @Test
    fun mergeConsecutive_mergesAdjacentSameCategory() {
        val input =
            listOf(
                TrafficSegment(listOf(path[0], path[1]), SpeedCategory.FREE),
                TrafficSegment(listOf(path[1], path[2]), SpeedCategory.FREE),
                TrafficSegment(listOf(path[2], path[3]), SpeedCategory.SLOW),
            )

        val merged = TrafficPolylineBuilder.mergeConsecutive(input)

        assertEquals(2, merged.size)
        assertEquals(SpeedCategory.FREE, merged[0].speedCategory)
        assertEquals(3, merged[0].points.size)
        assertEquals(SpeedCategory.SLOW, merged[1].speedCategory)
    }

    @Test
    fun build_prefersSpeedReadingIntervalsOverStepRecords() {
        val route =
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "15 min",
                distanceMeters = 10_000,
                durationSeconds = 900,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
                trafficSpeedIntervals =
                    listOf(
                        RouteSpeedInterval(0, path.lastIndex, SpeedCategory.MODERATE),
                    ),
                stepTrafficRecords =
                    listOf(
                        DirectionsStepTrafficRecord(path, SpeedCategory.JAM),
                    ),
            )

        val segments = TrafficPolylineBuilder.build(route)

        assertEquals(1, segments.size)
        assertEquals(SpeedCategory.MODERATE, segments[0].speedCategory)
    }

    @Test
    fun build_usesStepRecordsWhenIntervalsMissing() {
        val stepA = listOf(path[0], path[1], path[2])
        val stepB = listOf(path[2], path[3], path[4])
        val route =
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "15 min",
                distanceMeters = 10_000,
                durationSeconds = 900,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
                stepTrafficRecords =
                    listOf(
                        DirectionsStepTrafficRecord(stepA, SpeedCategory.FREE),
                        DirectionsStepTrafficRecord(stepB, SpeedCategory.JAM),
                    ),
            )

        val segments = TrafficPolylineBuilder.build(route)

        assertEquals(2, segments.size)
        assertEquals(SpeedCategory.FREE, segments[0].speedCategory)
        assertEquals(SpeedCategory.JAM, segments[1].speedCategory)
    }

    @Test
    fun build_fallbackWhenNoTrafficData() {
        val route =
            RealRouteDebugData(
                distanceText = "10 km",
                durationText = "15 min",
                distanceMeters = 10_000,
                durationSeconds = 900,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
            )

        val segments = TrafficPolylineBuilder.build(route)

        assertEquals(1, segments.size)
        assertEquals(SpeedCategory.UNKNOWN, segments[0].speedCategory)
        assertEquals(path, segments[0].points)
    }

    @Test
    fun mapGoogleSpeed_mapsRoutesApiValues() {
        assertEquals(SpeedCategory.FREE, TrafficSpeedParsing.mapGoogleSpeed("NORMAL"))
        assertEquals(SpeedCategory.SLOW, TrafficSpeedParsing.mapGoogleSpeed("SLOW"))
        assertEquals(SpeedCategory.JAM, TrafficSpeedParsing.mapGoogleSpeed("TRAFFIC_JAM"))
        assertEquals(SpeedCategory.UNKNOWN, TrafficSpeedParsing.mapGoogleSpeed("TRAFFIC_UNSPECIFIED"))
        assertEquals(SpeedCategory.MODERATE, TrafficSpeedParsing.mapGoogleSpeed("SOMETHING_ELSE"))
    }

    @Test
    fun speedCategoryFromDurations_mapsDelayToCategory() {
        assertEquals(SpeedCategory.FREE, TrafficSpeedParsing.speedCategoryFromDurations(600, 620))
        assertEquals(SpeedCategory.MODERATE, TrafficSpeedParsing.speedCategoryFromDurations(600, 700))
        assertEquals(SpeedCategory.SLOW, TrafficSpeedParsing.speedCategoryFromDurations(600, 850))
        assertEquals(SpeedCategory.JAM, TrafficSpeedParsing.speedCategoryFromDurations(600, 1200))
    }

    @Test
    fun parseSpeedReadingIntervals_readsRoutesApiJson() {
        val json =
            """
            {
              "travelAdvisory": {
                "speedReadingIntervals": [
                  {
                    "startPolylinePointIndex": 0,
                    "endPolylinePointIndex": 2,
                    "speed": "NORMAL"
                  },
                  {
                    "startPolylinePointIndex": 3,
                    "endPolylinePointIndex": 4,
                    "speed": "TRAFFIC_JAM"
                  }
                ]
              }
            }
            """.trimIndent()

        val intervals = TrafficSpeedParsing.parseSpeedReadingIntervals(json, path.size)

        assertEquals(2, intervals.size)
        assertEquals(SpeedCategory.FREE, intervals[0].speedCategory)
        assertEquals(SpeedCategory.JAM, intervals[1].speedCategory)
    }

    @Test
    fun build_returnsEmptyForShortPath() {
        val route =
            RealRouteDebugData(
                distanceText = "1 km",
                durationText = "2 min",
                distanceMeters = 1_000,
                durationSeconds = 120,
                tollAED = 0,
                hasToll = false,
                routePathPoints = listOf(LatLng(25.0, 55.0)),
            )

        assertTrue(TrafficPolylineBuilder.build(route).isEmpty())
    }
}
