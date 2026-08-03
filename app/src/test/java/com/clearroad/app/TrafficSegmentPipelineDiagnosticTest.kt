package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 1 diagnostic — confirms traffic color pipeline after map cascade removal.
 * Not a product feature; documents live behavior on fixture + synthetic rush-hour data.
 */
class TrafficSegmentPipelineDiagnosticTest {

    @Test
    fun legacyBenchmarkFixture_legLevelApproximationWhenDelayPresent() {
        val json = readResource("benchmark/route1-DIFC-Marina.json")
        val routes = extractRouteLegsDebugData(json)
        assertTrue("Expected benchmark routes", routes.isNotEmpty())

        routes.forEachIndexed { index, route ->
            val segments = TrafficPolylineBuilder.build(route)
            val counts = TrafficSegmentDebugLog.categoryCounts(segments)
            val ratio = LegLevelTrafficApproximation.legDelayRatio(route)
            val legCategory =
                ratio?.let { LegLevelTrafficApproximation.speedCategoryFromDelayRatio(it) }
            val line =
                "Route[$index] baseDuration=${route.baseDurationSeconds} " +
                    "legDurationInTraffic=${route.durationInTrafficSeconds} " +
                    "delayRatio=$ratio legCategory=$legCategory " +
                    "segments=${segments.size} " +
                    "segmentCounts{${TrafficSegmentDebugLog.formatCounts(counts)}}"
            println(line)

            assertTrue(route.trafficSpeedIntervals.isEmpty())
            assertTrue(route.stepTrafficRecords.isNotEmpty())
            assertTrue(segments.isNotEmpty())
        }
    }

    @Test
    fun legDelayWithoutStepTraffic_overridesUniformFreeWithRouteLevelCategory() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
                LatLng(25.02, 55.0),
            )
        val route =
            RealRouteDebugData(
                distanceText = "5 km",
                durationText = "20 min",
                distanceMeters = 5_000,
                durationSeconds = 1_400,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
                baseDurationSeconds = 1_000,
                durationInTrafficSeconds = 1_400,
                stepTrafficRecords =
                    listOf(
                        DirectionsStepTrafficRecord(
                            points = listOf(path[0], path[1]),
                            speedCategory = SpeedCategory.FREE,
                        ),
                        DirectionsStepTrafficRecord(
                            points = listOf(path[1], path[2]),
                            speedCategory = SpeedCategory.FREE,
                        ),
                    ),
            )

        val segments = TrafficPolylineBuilder.build(route)
        val counts = TrafficSegmentDebugLog.categoryCounts(segments)

        assertEquals(1, segments.size)
        assertEquals(SpeedCategory.SLOW, segments.single().speedCategory)
        assertEquals(1, counts[SpeedCategory.SLOW])
        println(
            "Leg-only delay override: ratio=1.4 -> ${TrafficSegmentDebugLog.formatCounts(counts)}",
        )
    }

    @Test
    fun realPerStepTrafficCategories_areNotOverriddenByLegApproximation() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
                LatLng(25.02, 55.0),
                LatLng(25.03, 55.0),
            )
        val route =
            RealRouteDebugData(
                distanceText = "5 km",
                durationText = "20 min",
                distanceMeters = 5_000,
                durationSeconds = 1_200,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
                baseDurationSeconds = 1_000,
                durationInTrafficSeconds = 1_700,
                stepTrafficRecords =
                    listOf(
                        DirectionsStepTrafficRecord(
                            points = listOf(path[0], path[1]),
                            speedCategory =
                                TrafficSpeedParsing.speedCategoryFromDurations(
                                    baseSeconds = 600,
                                    trafficSeconds = 620,
                                ),
                        ),
                        DirectionsStepTrafficRecord(
                            points = listOf(path[1], path[2], path[3]),
                            speedCategory =
                                TrafficSpeedParsing.speedCategoryFromDurations(
                                    baseSeconds = 600,
                                    trafficSeconds = 1_200,
                                ),
                        ),
                    ),
            )

        val segments = TrafficPolylineBuilder.build(route)

        assertEquals(2, segments.size)
        assertEquals(SpeedCategory.FREE, segments.first().speedCategory)
        assertEquals(SpeedCategory.JAM, segments.last().speedCategory)
    }

    @Test
    fun syntheticStepDelay_producesJamSegment_andRedStaticMapPath() {
        val path =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.0),
                LatLng(25.02, 55.0),
                LatLng(25.03, 55.0),
            )
        val route =
            RealRouteDebugData(
                distanceText = "5 km",
                durationText = "20 min",
                distanceMeters = 5_000,
                durationSeconds = 1_200,
                tollAED = 0,
                hasToll = false,
                routePathPoints = path,
                stepTrafficRecords =
                    listOf(
                        DirectionsStepTrafficRecord(
                            points = listOf(path[0], path[1]),
                            speedCategory =
                                TrafficSpeedParsing.speedCategoryFromDurations(
                                    baseSeconds = 600,
                                    trafficSeconds = 620,
                                ),
                        ),
                        DirectionsStepTrafficRecord(
                            points = listOf(path[1], path[2], path[3]),
                            speedCategory =
                                TrafficSpeedParsing.speedCategoryFromDurations(
                                    baseSeconds = 600,
                                    trafficSeconds = 1_200,
                                ),
                        ),
                    ),
            )

        val segments = TrafficPolylineBuilder.build(route)
        val counts = TrafficSegmentDebugLog.categoryCounts(segments)
        println(
            "Synthetic rush-hour segments: ${TrafficSegmentDebugLog.formatCounts(counts)}",
        )

        assertEquals(SpeedCategory.FREE, segments.first().speedCategory)
        assertEquals(SpeedCategory.JAM, segments.last().speedCategory)

        val url =
            buildStaticMapUrl(
                trafficSegments = segments,
                from = path.first(),
                to = path.last(),
                apiKey = "test-key",
            )
        assertTrue(url.contains("color%3A0x2ECC71FF"))
        assertTrue(url.contains("color%3A0xE74C3CFF"))
        println("Static map URL with JAM segment:\n$url")
    }

    @Test
    fun parseRouteTraffic_legacyStepJson_withoutStepDurationInTraffic_yieldsFreeSteps() {
        val stepJson =
            """
            {
              "duration": { "value": 120 },
              "polyline": { "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@" }
            }
            """.trimIndent()
        val routeJson =
            """
            {
              "legs": [
                {
                  "steps": [ $stepJson ]
                }
              ]
            }
            """.trimIndent()

        val (intervals, steps) = TrafficSpeedParsing.parseRouteTraffic(routeJson, pathPointCount = 3)

        assertTrue(intervals.isEmpty())
        assertEquals(1, steps.size)
        assertEquals(SpeedCategory.FREE, steps.first().speedCategory)
        println("Legacy step without duration_in_traffic -> ${steps.first().speedCategory}")
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }.bufferedReader().use { it.readText() }
}
