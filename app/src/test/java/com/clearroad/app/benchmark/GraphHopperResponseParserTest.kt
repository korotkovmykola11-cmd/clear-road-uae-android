package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphHopperResponseParserTest {

    private val requestId = "graphhopper-case-1-1"
    private val caseId = "difc-marina"
    private val encodedPolyline =
        PolyUtil.encode(
            listOf(
                LatLng(25.2100, 55.2750),
                LatLng(25.1500, 55.2200),
                LatLng(25.0800, 55.1400),
            ),
        )

    @Test
    fun onePath_parses() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = responseJson(paths = listOf(pathObject(index = 0))),
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Success)
        assertEquals(1, (result as GraphHopperResponseParser.ParseResult.Success).candidates.size)
    }

    @Test
    fun multiplePaths_parse() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths =
                            listOf(
                                pathObject(index = 0, distance = 18_000.0, timeMs = 1_200_000),
                                pathObject(index = 1, distance = 19_000.0, timeMs = 1_300_000),
                            ),
                    ),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        assertEquals(2, result.candidates.size)
        assertEquals(0, result.candidates[0].routeIndex)
        assertEquals(1, result.candidates[1].routeIndex)
    }

    @Test
    fun encodedGeometry_decodes() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = responseJson(paths = listOf(pathObject(index = 0))),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        val points = result.candidates.first().routePathPoints
        assertEquals(3, points.size)
        assertEquals(25.2100, points.first().latitude, 0.0001)
        assertEquals(55.1400, points.last().longitude, 0.0001)
    }

    @Test
    fun distanceAndTime_parse() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths = listOf(pathObject(index = 0, distance = 18_500.0, timeMs = 1_420_000)),
                    ),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        val candidate = result.candidates.first()
        assertEquals(18_500, candidate.distanceMeters)
        assertEquals(1_420, candidate.durationSeconds)
    }

    @Test
    fun instructions_buildCorridorScanText() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths =
                            listOf(
                                pathObject(
                                    index = 0,
                                    includeInstructions = true,
                                ),
                            ),
                    ),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        val corridor = result.candidates.first().corridorScanText
        assertTrue(corridor.contains("Sheikh Zayed Rd"))
        assertTrue(corridor.contains("Merge onto E11"))
    }

    @Test
    fun missingInstructions_isAccepted() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths = listOf(pathObject(index = 0, includeInstructions = false)),
                    ),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        assertEquals("GraphHopper route 0", result.candidates.first().routeSummary)
        assertEquals("", result.candidates.first().corridorScanText)
    }

    @Test
    fun malformedJson_returnsFailure() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = """{ "paths": [ """,
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Failure)
    }

    @Test
    fun emptyPaths_returnsFailure() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = """{ "paths": [] }""",
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Failure)
        assertTrue((result as GraphHopperResponseParser.ParseResult.Failure).message.contains("No paths"))
    }

    @Test
    fun missingGeometry_yieldsFailure() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths =
                            listOf(
                                """
                                {
                                  "distance": 18000,
                                  "time": 1200000,
                                  "points_encoded": true
                                }
                                """.trimIndent(),
                            ),
                    ),
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Failure)
    }

    @Test
    fun invalidDistanceOrTime_yieldsFailure() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson =
                    responseJson(
                        paths =
                            listOf(
                                pathObject(index = 0, distance = 0.0, timeMs = 0),
                            ),
                    ),
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Failure)
    }

    @Test
    fun candidateIds_areStable() {
        val json = responseJson(paths = listOf(pathObject(index = 0)))
        val first =
            (
                GraphHopperResponseParser.parse(json, caseId, requestId)
                    as GraphHopperResponseParser.ParseResult.Success
            ).candidates.first().candidateId
        val second =
            (
                GraphHopperResponseParser.parse(json, caseId, requestId)
                    as GraphHopperResponseParser.ParseResult.Success
            ).candidates.first().candidateId

        assertEquals("$caseId-GRAPHHOPPER-0", first)
        assertEquals(first, second)
    }

    @Test
    fun caseIdAndRequestId_arePreserved() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = responseJson(paths = listOf(pathObject(index = 0))),
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Success

        val candidate = result.candidates.first()
        assertEquals(caseId, candidate.caseId)
        assertEquals(requestId, candidate.sourceFixture)
        assertEquals(0, candidate.tollAed)
    }

    @Test
    fun graphHopperErrorJson_isHandled() {
        val result =
            GraphHopperResponseParser.parse(
                responseJson = """{ "message": "Point not found", "hints": [] }""",
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GraphHopperResponseParser.ParseResult.Failure)
        assertTrue((result as GraphHopperResponseParser.ParseResult.Failure).message.contains("Point not found"))
    }

    @Test
    fun parseResultToString_doesNotLeakRawJson() {
        val secretPayload = """{"key":"super-secret","paths":[]}"""
        val failure =
            GraphHopperResponseParser.parse(
                responseJson = secretPayload,
                caseId = caseId,
                requestId = requestId,
            ) as GraphHopperResponseParser.ParseResult.Failure

        val text = failure.toString()
        assertFalse(text.contains(secretPayload))
        assertFalse(text.contains("super-secret"))
    }

    private fun responseJson(paths: List<String>): String =
        """
        {
          "paths": [
            ${paths.joinToString(",\n")}
          ]
        }
        """.trimIndent()

    private fun pathObject(
        index: Int,
        distance: Double = 18_000.0,
        timeMs: Long = 1_200_000,
        includeInstructions: Boolean = false,
    ): String {
        val instructionsBlock =
            if (includeInstructions) {
                """
                ,
                "instructions": [
                  {
                    "text": "Merge onto E11",
                    "street_name": "Sheikh Zayed Rd",
                    "distance": 1000,
                    "time": 60000,
                    "sign": 0
                  }
                ]
                """.trimIndent()
            } else {
                ""
            }
        return """
            {
              "distance": $distance,
              "time": $timeMs,
              "points": "$encodedPolyline",
              "points_encoded": true
              $instructionsBlock
            }
            """.trimIndent()
    }
}
