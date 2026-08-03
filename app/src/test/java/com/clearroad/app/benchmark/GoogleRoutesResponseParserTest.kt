package com.clearroad.app.benchmark

import com.clearroad.app.RoutesV2ResponseAdapter
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleRoutesResponseParserTest {

    private val requestId = "google-case-1-1"
    private val caseId = "difc-marina"
    private val encodedPolyline = PolyUtil.encode(
        listOf(
            LatLng(25.2100, 55.2750),
            LatLng(25.1500, 55.2200),
            LatLng(25.0800, 55.1400),
        ),
    )

    @Test
    fun oneRoute_parses() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = singleRouteJson(),
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GoogleRoutesResponseParser.ParseResult.Success)
        val success = result as GoogleRoutesResponseParser.ParseResult.Success
        assertEquals(1, success.candidates.size)
        assertTrue(success.candidates.first().isValid)
    }

    @Test
    fun multipleRoutes_parse() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson =
                    """
                    {
                      "routes": [
                        ${routeObject(index = 0, description = "Route A", durationSeconds = 1200)},
                        ${routeObject(index = 1, description = "Route B", durationSeconds = 1300)}
                      ]
                    }
                    """.trimIndent(),
                caseId = caseId,
                requestId = requestId,
            )

        val success = result as GoogleRoutesResponseParser.ParseResult.Success
        assertEquals(2, success.candidates.size)
        assertEquals(0, success.candidates[0].routeIndex)
        assertEquals(1, success.candidates[1].routeIndex)
    }

    @Test
    fun encodedGeometry_decodes() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = singleRouteJson(),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        val points = result.candidates.first().routePathPoints
        assertEquals(3, points.size)
        assertEquals(25.2100, points.first().latitude, 0.0001)
        assertEquals(55.1400, points.last().longitude, 0.0001)
    }

    @Test
    fun distanceAndDuration_parse() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = singleRouteJson(distanceMeters = 18_500, durationSeconds = 1_420),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        val candidate = result.candidates.first()
        assertEquals(18_500, candidate.distanceMeters)
        assertEquals(1_420, candidate.durationSeconds)
    }

    @Test
    fun optionalToll_parses() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson =
                    singleRouteJson(
                        tollAed = 4,
                        includeTollInfo = true,
                    ),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        assertEquals(4, result.candidates.first().tollAed)
    }

    @Test
    fun missingToll_isAccepted() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = singleRouteJson(includeTollInfo = false),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        assertEquals(0, result.candidates.first().tollAed)
    }

    @Test
    fun missingSteps_isAccepted() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson =
                    """
                    {
                      "routes": [
                        {
                          "distanceMeters": 18000,
                          "duration": "1200s",
                          "description": "Sheikh Zayed Rd/E11",
                          "polyline": { "encodedPolyline": "$encodedPolyline" }
                        }
                      ]
                    }
                    """.trimIndent(),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        assertEquals("Sheikh Zayed Rd/E11", result.candidates.first().corridorScanText)
    }

    @Test
    fun malformedJson_returnsFailure() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = """{ "routes": [ """,
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GoogleRoutesResponseParser.ParseResult.Failure)
    }

    @Test
    fun emptyRoutes_returnsFailure() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = """{ "routes": [] }""",
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GoogleRoutesResponseParser.ParseResult.Failure)
        assertTrue((result as GoogleRoutesResponseParser.ParseResult.Failure).message.contains("No routes"))
    }

    @Test
    fun missingGeometry_yieldsFailure() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson =
                    """
                    {
                      "routes": [
                        {
                          "distanceMeters": 18000,
                          "duration": "1200s",
                          "description": "No geometry route"
                        }
                      ]
                    }
                    """.trimIndent(),
                caseId = caseId,
                requestId = requestId,
            )

        assertTrue(result is GoogleRoutesResponseParser.ParseResult.Failure)
    }

    @Test
    fun candidateIds_areStable() {
        val json = singleRouteJson()
        val first =
            (
                GoogleRoutesResponseParser.parse(json, caseId, requestId)
                    as GoogleRoutesResponseParser.ParseResult.Success
            ).candidates.first().candidateId
        val second =
            (
                GoogleRoutesResponseParser.parse(json, caseId, requestId)
                    as GoogleRoutesResponseParser.ParseResult.Success
            ).candidates.first().candidateId

        assertEquals("$caseId-GOOGLE-0", first)
        assertEquals(first, second)
    }

    @Test
    fun caseIdAndRequestId_arePreserved() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson = singleRouteJson(),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        val candidate = result.candidates.first()
        assertEquals(caseId, candidate.caseId)
        assertEquals(requestId, candidate.sourceFixture)
    }

    @Test
    fun fieldMask_excludesUnsupportedStepName() {
        assertFalse(GoogleRoutesResponseParser.PARSED_FIELD_MASK.contains("steps.name"))
        assertEquals(
            RoutesV2ResponseAdapter.ADAPTER_FIELD_MASK,
            GoogleRoutesResponseParser.PARSED_FIELD_MASK,
        )
    }

    @Test
    fun navigationInstruction_buildsCorridorScanText_withoutStepName() {
        val result =
            GoogleRoutesResponseParser.parse(
                responseJson =
                    """
                    {
                      "routes": [
                        {
                          "distanceMeters": 18000,
                          "duration": "1200s",
                          "description": "Sheikh Zayed Rd/E11",
                          "polyline": { "encodedPolyline": "$encodedPolyline" },
                          "legs": [
                            {
                              "steps": [
                                {
                                  "navigationInstruction": {
                                    "instructions": "Merge onto Sheikh Zayed Rd / E11"
                                  }
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Success

        val corridorScanText = result.candidates.first().corridorScanText
        assertTrue(corridorScanText.contains("Sheikh Zayed Rd/E11"))
        assertTrue(corridorScanText.contains("Merge onto Sheikh Zayed Rd / E11"))
    }

    @Test
    fun parseResultToString_doesNotLeakRawJson() {
        val secretPayload = """{"api_key":"super-secret","routes":[]}"""
        val failure =
            GoogleRoutesResponseParser.parse(
                responseJson = secretPayload,
                caseId = caseId,
                requestId = requestId,
            ) as GoogleRoutesResponseParser.ParseResult.Failure

        val text = failure.toString()
        assertFalse(text.contains(secretPayload))
        assertFalse(text.contains("super-secret"))
    }

    private fun singleRouteJson(
        distanceMeters: Int = 18_000,
        durationSeconds: Int = 1_200,
        tollAed: Int = 0,
        includeTollInfo: Boolean = tollAed > 0,
    ): String =
        """
        {
          "routes": [
            ${routeObject(
                index = 0,
                description = "Sheikh Zayed Rd/E11",
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                tollAed = tollAed,
                includeTollInfo = includeTollInfo,
                includeSteps = true,
            )}
          ]
        }
        """.trimIndent()

    private fun routeObject(
        index: Int,
        description: String,
        distanceMeters: Int = 18_000,
        durationSeconds: Int = 1_200,
        tollAed: Int = 0,
        includeTollInfo: Boolean = false,
        includeSteps: Boolean = false,
    ): String {
        val tollBlock =
            if (includeTollInfo) {
                """
                ,
                "travelAdvisory": {
                  "tollInfo": {
                    "estimatedPrice": [
                      { "currencyCode": "AED", "units": "$tollAed", "nanos": 0 }
                    ]
                  }
                }
                """.trimIndent()
            } else {
                ""
            }
        val stepsBlock =
            if (includeSteps) {
                """
                ,
                "legs": [
                  {
                    "steps": [
                      {
                        "navigationInstruction": {
                          "instructions": "Merge onto Sheikh Zayed Rd / E11"
                        }
                      }
                    ]
                  }
                ]
                """.trimIndent()
            } else {
                ""
            }
        return """
            {
              "distanceMeters": $distanceMeters,
              "duration": "${durationSeconds}s",
              "description": "$description",
              "routeLabels": ["DEFAULT_ROUTE"],
              "polyline": { "encodedPolyline": "$encodedPolyline" }
              $tollBlock
              $stepsBlock
            }
            """.trimIndent()
    }
}
