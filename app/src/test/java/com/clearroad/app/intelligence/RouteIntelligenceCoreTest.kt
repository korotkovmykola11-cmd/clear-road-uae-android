package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProfileBuilderTest {

    @Test
    fun build_okSource_producesRouteProfileWithComplexity() {
        val enriched =
            EnrichedRoute(
                raw =
                    RawRouteForIntelligence(
                        routeIndex = 0,
                        routeName = "Route 1",
                        routePathPoints = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                        durationSeconds = 600,
                        distanceMeters = 5000,
                    ),
                sources =
                    listOf(
                        SourceData(
                            provider = OsmOverpassSource.PROVIDER,
                            status = SourceStatus.OK,
                            trafficSignalsCount = 3,
                            roundaboutsCount = 1,
                            mainRoadRatio = 0.8f,
                            evidence =
                                RouteEvidence(
                                    trafficLights = TrafficLightEvidence(3, listOf(1, 2, 3), emptyList()),
                                    roundabouts = RoundaboutEvidence(1, listOf(9), emptyList()),
                                    mainRoad = MainRoadEvidence(0.8f, 800.0, 1000.0),
                                ),
                            metadata =
                                SourceMetadata(
                                    provider = OsmOverpassSource.PROVIDER,
                                    fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
                                    confidence = 0.55f,
                                ),
                        ),
                    ),
            )

        val profile = RouteProfileBuilder.build(enriched)

        assertEquals(SourceStatus.OK, profile.osmSourceStatus)
        assertEquals(3, profile.trafficSignalsCount)
        assertEquals(1, profile.roundaboutsCount)
        assertEquals(0.8f, profile.mainRoadRatio)
        assertNotNull(profile.complexityScore)
        assertNotNull(profile.evidence?.trafficLights)
    }

    @Test
    fun build_unavailableSource_returnsUnavailableProfile() {
        val enriched =
            EnrichedRoute(
                raw =
                    RawRouteForIntelligence(
                        routeIndex = 0,
                        routeName = "Route 1",
                        routePathPoints = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                        durationSeconds = 600,
                        distanceMeters = 5000,
                    ),
                sources =
                    listOf(
                        SourceData(
                            provider = OsmOverpassSource.PROVIDER,
                            status = SourceStatus.UNAVAILABLE,
                            trafficSignalsCount = null,
                            roundaboutsCount = null,
                            mainRoadRatio = null,
                            evidence = null,
                            metadata = null,
                            errorMessage = "Overpass request failed",
                        ),
                    ),
            )

        val profile = RouteProfileBuilder.build(enriched)

        assertEquals(SourceStatus.UNAVAILABLE, profile.osmSourceStatus)
        assertNull(profile.trafficSignalsCount)
        assertNull(profile.complexityScore)
    }
}

class RouteIntelligenceCacheTest {

    @Test
    fun cache_returnsProfileWithinTtl() {
        var now = 0L
        val cache = RouteIntelligenceCache(ttlMillis = 30 * 60 * 1000, nowMillis = { now })
        val profile =
            RouteProfile(
                trafficSignalsCount = 2,
                roundaboutsCount = 0,
                mainRoadRatio = 0.5f,
                complexityScore = 0.2f,
                osmSourceStatus = SourceStatus.OK,
                evidence = null,
                metadata = null,
            )

        cache.put("abc", profile)
        assertEquals(profile, cache.get("abc"))

        now += 29 * 60 * 1000
        assertEquals(profile, cache.get("abc"))

        now += 2 * 60 * 1000
        assertNull(cache.get("abc"))
    }
}

class OsmOverpassSourceTest {

    @Test
    fun fetchOverpassJson_failsOverToSecondaryEndpoint() =
        runBlocking {
            var callCount = 0
            val httpClient =
                object : RouteIntelligenceHttpClient {
                    override suspend fun get(
                        url: String,
                        attempt: RouteIntelligenceDiag.HttpAttemptContext?,
                    ): Result<String> {
                        callCount++
                        return if (callCount == 1) {
                            Result.failure(IllegalStateException("primary down"))
                        } else {
                            Result.success("""{"elements":[]}""")
                        }
                    }
                }
            val source = OsmOverpassSource(httpClient = httpClient)

            val json = source.fetchOverpassJson("[out:json];node(1);out;", routeIndex = 0)

            assertEquals("""{"elements":[]}""", json)
            assertEquals(2, callCount)
        }
}

class RouteIntelligenceDiagTest {

    private val attempt =
        RouteIntelligenceDiag.HttpAttemptContext(
            routeIndex = 0,
            endpoint = RouteIntelligenceDiag.EndpointRole.PRIMARY,
        )

    @Test
    fun formatHttpOutcome_successIncludesStatusCode() {
        val line =
            RouteIntelligenceDiag.formatHttpOutcome(
                attempt = attempt,
                outcome = RouteIntelligenceDiag.HttpOutcome.SUCCESS,
                durationMs = 842,
                statusCode = 200,
            )

        assertEquals(
            "RI_HTTP route=0 endpoint=PRIMARY outcome=SUCCESS status=200 durationMs=842",
            line,
        )
    }

    @Test
    fun formatHttpOutcome_httpErrorIncludesStatusCode() {
        val line =
            RouteIntelligenceDiag.formatHttpOutcome(
                attempt = attempt,
                outcome = RouteIntelligenceDiag.HttpOutcome.HTTP_ERROR,
                durationMs = 1243,
                statusCode = 429,
            )

        assertEquals(
            "RI_HTTP route=0 endpoint=PRIMARY outcome=HTTP_ERROR status=429 durationMs=1243",
            line,
        )
    }

    @Test
    fun formatHttpOutcome_exceptionUsesClassOnlyWithoutMessage() {
        val line =
            RouteIntelligenceDiag.formatHttpOutcome(
                attempt = attempt,
                outcome = RouteIntelligenceDiag.HttpOutcome.EXCEPTION,
                durationMs = 30012,
                exceptionClass = "SocketTimeoutException",
                phase = RouteIntelligenceDiag.HttpPhase.UNKNOWN,
            )

        assertEquals(
            "RI_HTTP route=0 endpoint=PRIMARY outcome=EXCEPTION exception=SocketTimeoutException phase=UNKNOWN durationMs=30012",
            line,
        )
    }

    @Test
    fun formatHttpOutcome_containsNoSensitiveFields() {
        val line =
            RouteIntelligenceDiag.formatHttpOutcome(
                attempt = attempt,
                outcome = RouteIntelligenceDiag.HttpOutcome.HTTP_ERROR,
                durationMs = 500,
                statusCode = 503,
            )

        val forbidden =
            listOf(
                "http://",
                "https://",
                "overpass",
                "data=",
                "25.",
                "55.",
                "bbox",
                "Ajman",
                "Sharjah",
                "network down",
            )
        forbidden.forEach { token ->
            assertFalse("Line must not contain '$token': $line", line.contains(token, ignoreCase = true))
        }
    }

    @Test
    fun classifyPresentationResult_mapsCardStates() {
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_UNAVAILABLE,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.UNAVAILABLE,
                alternativeStatus = SourceStatus.UNAVAILABLE,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.UNAVAILABLE,
                alternativeStatus = SourceStatus.OK,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_AVAILABLE,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.OK,
                alternativeStatus = SourceStatus.OK,
            ),
        )
    }

    @Test
    fun fetchOverpassJson_primaryFailureAndFallbackSuccess_attemptsBothEndpoints() =
        runBlocking {
            val outcomes = mutableListOf<RouteIntelligenceDiag.EndpointRole>()
            val httpClient =
                object : RouteIntelligenceHttpClient {
                    override suspend fun get(
                        url: String,
                        attempt: RouteIntelligenceDiag.HttpAttemptContext?,
                    ): Result<String> {
                        attempt?.let { outcomes += it.endpoint }
                        return when (attempt?.endpoint) {
                            RouteIntelligenceDiag.EndpointRole.PRIMARY ->
                                Result.failure(RouteIntelligenceHttpStatusException(429))
                            RouteIntelligenceDiag.EndpointRole.FALLBACK ->
                                Result.success("""{"elements":[]}""")
                            else -> Result.failure(IllegalStateException("unexpected"))
                        }
                    }
                }
            val source = OsmOverpassSource(httpClient = httpClient)

            val json = source.fetchOverpassJson("[out:json];node(1);out;", routeIndex = 1)

            assertEquals("""{"elements":[]}""", json)
            assertEquals(
                listOf(
                    RouteIntelligenceDiag.EndpointRole.PRIMARY,
                    RouteIntelligenceDiag.EndpointRole.FALLBACK,
                ),
                outcomes,
            )
        }
}

class ComplexityScoreCalculatorTest {

    @Test
    fun calculate_higherSignalsAndLocalRoads_increaseScore() {
        val low =
            ComplexityScoreCalculator.calculate(
                trafficSignalsCount = 1,
                roundaboutsCount = 0,
                mainRoadRatio = 0.9f,
            )
        val high =
            ComplexityScoreCalculator.calculate(
                trafficSignalsCount = 12,
                roundaboutsCount = 3,
                mainRoadRatio = 0.1f,
            )

        assertNotNull(low)
        assertNotNull(high)
        assertTrue(high!! > low!!)
        assertTrue(high <= 1.0f)
        assertTrue(low >= 0.0f)
    }

    @Test
    fun complexityLabel_mapsScoreBands() {
        assertEquals("Low complexity", ComplexityScoreCalculator.complexityLabel(0.1f))
        assertEquals("Moderate complexity", ComplexityScoreCalculator.complexityLabel(0.4f))
        assertEquals("Higher complexity", ComplexityScoreCalculator.complexityLabel(0.8f))
    }
}
