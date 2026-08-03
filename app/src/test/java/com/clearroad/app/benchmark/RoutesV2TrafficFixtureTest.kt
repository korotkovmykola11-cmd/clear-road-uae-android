package com.clearroad.app.benchmark

import com.clearroad.app.SpeedCategory
import com.clearroad.app.TrafficPolylineBuilder
import com.clearroad.app.TrafficSpeedParsing
import com.clearroad.app.decodeRoutePathPoints
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 * Stage A — offline validation of live-captured Routes API v2 traffic fixtures.
 *
 * Fixtures live under [FIXTURE_ROOT] with metadata in [MANIFEST_FILE].
 */
class RoutesV2TrafficFixtureTest {

    @Test
    fun manifest_listsThreeLiveFixtures() {
        val manifest = readResource("$FIXTURE_ROOT/$MANIFEST_FILE")
        val root = JSONObject(manifest)
        val cases = root.getJSONArray("cases")
        assertEquals(3, cases.length())
        assertTrue(root.getString("fieldMask").contains("speedReadingIntervals"))
    }

    @Test
    fun sharjahDowntown_fixtureContainsTrafficJamIntervals() {
        val routeJson = firstRouteObjectJson(readFixture("live-sharjah-downtown"))
        val intervals = TrafficSpeedParsing.parseSpeedReadingIntervals(routeJson, pathPointCount = 800)
        val jamCount = intervals.count { it.speedCategory == SpeedCategory.JAM }
        assertTrue("Expected TRAFFIC_JAM intervals in peak fixture", jamCount >= 1)
    }

    @Test
    fun allFixtures_speedReadingIntervalsAlignWithHighQualityPolyline() {
        listOf("live-difc-marina", "live-sharjah-downtown", "live-business-bay-jlt").forEach { caseId ->
            val routeJson = firstRouteObjectJson(readFixture(caseId))
            val encoded = encodedPolylineFromRouteJson(routeJson)
            assertFalse("$caseId: missing encoded polyline", encoded.isNullOrBlank())
            val path = decodeRoutePathPoints(encoded)
            assertTrue("$caseId: polyline too short", path.size >= 2)

            val intervals = TrafficSpeedParsing.parseSpeedReadingIntervals(routeJson, path.size)
            assertTrue("$caseId: expected speedReadingIntervals", intervals.isNotEmpty())

            intervals.forEach { interval ->
                assertTrue(
                    "$caseId: start index ${interval.startPointIndex} out of range (path=${path.size})",
                    interval.startPointIndex in 0..path.lastIndex,
                )
                assertTrue(
                    "$caseId: end index ${interval.endPointIndex} out of range (path=${path.size})",
                    interval.endPointIndex in 0..path.lastIndex,
                )
                assertTrue(
                    "$caseId: inverted interval ${interval.startPointIndex}..${interval.endPointIndex}",
                    interval.startPointIndex <= interval.endPointIndex,
                )
            }

            val lastEnd = intervals.maxOf { it.endPointIndex }
            assertTrue(
                "$caseId: last interval end $lastEnd should reach near polyline end ${path.lastIndex}",
                lastEnd >= path.lastIndex - 2,
            )
        }
    }

    @Test
    fun sharjahDowntown_fixtureBuildsMultiColorTrafficSegments() {
        val routeJson = firstRouteObjectJson(readFixture("live-sharjah-downtown"))
        val encoded = encodedPolylineFromRouteJson(routeJson)!!
        val path = decodeRoutePathPoints(encoded)
        val intervals = TrafficSpeedParsing.parseSpeedReadingIntervals(routeJson, path.size)
        val segments =
            TrafficPolylineBuilder.buildFromPointIntervals(path, intervals)
        val categories = segments.map { it.speedCategory }.toSet()
        assertTrue(categories.contains(SpeedCategory.JAM))
        assertTrue(categories.size >= 2)
    }

    @Test
    fun difcMarina_fixtureParsesThroughGoogleRoutesResponseParser() {
        val responseJson = readFixture("live-difc-marina")
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = responseJson,
                caseId = "live-difc-marina",
                requestId = "fixture-stage-a",
            )
        assertTrue(result is GoogleRoutesResponseParser.ParseResult.Success)
        val success = result as GoogleRoutesResponseParser.ParseResult.Success
        assertTrue(success.candidates.size >= 2)
    }

    private fun readFixture(caseId: String): String =
        readResource("$FIXTURE_ROOT/$caseId-traffic.json")

    private fun readResource(path: String): String {
        val stream =
            checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "Missing test resource: $path"
            }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    private fun firstRouteObjectJson(responseJson: String): String {
        val routes = JSONObject(responseJson).getJSONArray("routes")
        return routes.getJSONObject(0).toString()
    }

    private fun encodedPolylineFromRouteJson(routeJson: String): String? =
        JSONObject(routeJson)
            .optJSONObject("polyline")
            ?.optString("encodedPolyline")
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val FIXTURE_ROOT = "benchmark/routes-v2-traffic"
        private const val MANIFEST_FILE = "manifest.json"
    }
}
