package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stage 50.2 — edge-case and partial-failure coverage for Route Intelligence. */
class RouteIntelligenceFailureTest {

    private val routePath =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
        )

    private val googleSteps =
        listOf(
            GoogleDirectionsStepInput(500, "merge"),
            GoogleDirectionsStepInput(500, "ramp-right"),
            GoogleDirectionsStepInput(500, "turn-left"),
        )

    @Test
    fun reportFor_osmUnavailable_googleFeaturesAvailable() =
        runBlocking {
            val route =
                sampleRoute(
                    routeIndex = 0,
                    googleSteps = googleSteps,
                )
            val service = serviceWithOsmSource(osmUnavailableSource())

            val report =
                service.reportFor(
                    route = route,
                    isGoogleDefault = true,
                    isMarshioSelected = false,
                )

            assertEquals(SourceStatus.UNAVAILABLE, report.profile.osmSourceStatus)
            assertEquals(SourceStatus.OK, report.profile.googleSourceStatus)
            assertNull(report.signals.trafficSignalsCount)
            assertNotNull(report.signals.criticalManeuversCount)
            assertTrue(report.signals.criticalManeuversCount!! > 0)
            assertNull(report.osmEvidence)
            assertNotNull(report.googleEvidence)
        }

    @Test
    fun reportFor_osmAvailable_googleFeaturesUnavailable() =
        runBlocking {
            val route =
                sampleRoute(
                    routeIndex = 0,
                    googleSteps = emptyList(),
                    criticalManeuversCount = null,
                )
            val service = serviceWithOsmSource(osmOkSource(trafficSignalsCount = 4))

            val report =
                service.reportFor(
                    route = route,
                    isGoogleDefault = false,
                    isMarshioSelected = true,
                )

            assertEquals(SourceStatus.OK, report.profile.osmSourceStatus)
            assertEquals(SourceStatus.UNAVAILABLE, report.profile.googleSourceStatus)
            assertEquals(4, report.signals.trafficSignalsCount)
            assertNull(report.signals.criticalManeuversCount)
            assertNotNull(report.osmEvidence)
            assertNull(report.googleEvidence)
        }

    @Test
    fun parseResponse_malformedOverpassJson_reportsParseErrorWithoutCrashing() {
        val result = OsmOverpassParser.parseResponse("""{ this is not valid json """)

        assertTrue(result is OsmOverpassParser.ParseResult.MalformedJson)
        val malformed = result as OsmOverpassParser.ParseResult.MalformedJson
        assertTrue(malformed.errorMessage.contains("Malformed Overpass JSON", ignoreCase = true))
    }

    @Test
    fun enrich_emptyOverpassResponse_returnsOsmOkWithZeroCounts() =
        runBlocking {
            val source =
                OsmOverpassSource(
                    httpClient = fixedJsonClient("""{"elements":[]}"""),
                )

            val result = source.enrich(sampleRoute())

            assertEquals(SourceStatus.OK, result.status)
            assertEquals(0, result.trafficSignalsCount)
            assertEquals(0, result.roundaboutsCount)
            assertNull(result.mainRoadRatio)
        }

    @Test
    fun enrich_malformedOverpassJson_reportsOsmUnavailableWithoutCrashing() =
        runBlocking {
            val source =
                OsmOverpassSource(
                    httpClient = fixedJsonClient("""{not-json"""),
                )

            val result = source.enrich(sampleRoute())

            assertEquals(SourceStatus.UNAVAILABLE, result.status)
            assertNull(result.trafficSignalsCount)
            assertNull(result.roundaboutsCount)
            assertNotNull(result.errorMessage)
            assertTrue(result.errorMessage!!.contains("Malformed Overpass JSON", ignoreCase = true))
        }

    @Test
    fun enrich_overpassHttpFailure_returnsOsmUnavailable() =
        runBlocking {
            val source =
                OsmOverpassSource(
                    httpClient =
                        object : RouteIntelligenceHttpClient {
                            override suspend fun get(
                                url: String,
                                attempt: RouteIntelligenceDiag.HttpAttemptContext?,
                            ): Result<String> =
                                Result.failure(IllegalStateException("network down"))
                        },
                )

            val result = source.enrich(sampleRoute())

            assertEquals(SourceStatus.UNAVAILABLE, result.status)
            assertNull(result.trafficSignalsCount)
            assertNotNull(result.errorMessage)
        }

    @Test
    fun comparisonReportFor_mixedSourceFailures_buildsWithoutCrashing() =
        runBlocking {
            val osmOk = serviceWithOsmSource(osmOkSource(trafficSignalsCount = 3))
            val osmDown = serviceWithOsmSource(osmUnavailableSource())

            val routeOsmOkGoogleOk =
                sampleRoute(routeIndex = 0, googleSteps = googleSteps)
            val routeOsmOkGoogleMissing =
                sampleRoute(routeIndex = 1, googleSteps = emptyList())
            val routeOsmDownGooglePartial =
                sampleRoute(
                    routeIndex = 2,
                    googleSteps = emptyList(),
                    criticalManeuversCount = 2,
                )

            val comparisonOk =
                osmOk.comparisonReportFor(
                    routes = listOf(routeOsmOkGoogleOk, routeOsmOkGoogleMissing),
                    marshioSelectedRouteIndex = 0,
                )
            val comparisonPartial =
                osmDown.comparisonReportFor(
                    routes = listOf(routeOsmDownGooglePartial),
                    marshioSelectedRouteIndex = 0,
                )

            assertEquals(2, comparisonOk.reports.size)
            assertEquals(SourceStatus.OK, comparisonOk.reports[0].profile.osmSourceStatus)
            assertEquals(SourceStatus.OK, comparisonOk.reports[0].profile.googleSourceStatus)
            assertEquals(SourceStatus.OK, comparisonOk.reports[1].profile.osmSourceStatus)
            assertEquals(SourceStatus.UNAVAILABLE, comparisonOk.reports[1].profile.googleSourceStatus)
            assertTrue(comparisonOk.summary.dataCompleteness > 0f)

            assertEquals(1, comparisonPartial.reports.size)
            assertEquals(SourceStatus.UNAVAILABLE, comparisonPartial.reports[0].profile.osmSourceStatus)
            assertEquals(SourceStatus.PARTIAL, comparisonPartial.reports[0].profile.googleSourceStatus)
            assertTrue(comparisonPartial.summary.dataCompleteness > 0f)
        }

    private fun sampleRoute(
        routeIndex: Int = 0,
        googleSteps: List<GoogleDirectionsStepInput> = emptyList(),
        criticalManeuversCount: Int? = null,
    ): RawRouteForIntelligence =
        RawRouteForIntelligence(
            routeIndex = routeIndex,
            routeName = "Route ${routeIndex + 1}",
            routePathPoints = routePath,
            durationSeconds = 600 + routeIndex * 60,
            distanceMeters = 5000,
            googleSteps = googleSteps,
            criticalManeuversCount = criticalManeuversCount,
        )

    private fun serviceWithOsmSource(source: RouteDataSource): RouteIntelligenceService =
        RouteIntelligenceService(
            osmAggregator = RouteIntelligenceDataAggregator(sources = listOf(source)),
            cache = RouteIntelligenceCache(),
        )

    private fun osmUnavailableSource(): RouteDataSource =
        FixedRouteDataSource(
            data =
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
        )

    private fun osmOkSource(trafficSignalsCount: Int): RouteDataSource =
        FixedRouteDataSource(
            data =
                SourceData(
                    provider = OsmOverpassSource.PROVIDER,
                    status = SourceStatus.OK,
                    trafficSignalsCount = trafficSignalsCount,
                    roundaboutsCount = 0,
                    mainRoadRatio = 0.7f,
                    evidence =
                        RouteEvidence(
                            trafficLights =
                                TrafficLightEvidence(
                                    count = trafficSignalsCount,
                                    osmNodeIds = emptyList(),
                                    nearRoutePositions = emptyList(),
                                ),
                            roundabouts = RoundaboutEvidence(0, emptyList(), emptyList()),
                            mainRoad =
                                MainRoadEvidence(
                                    mainRoadRatio = 0.7f,
                                    mainRoadMeters = 700.0,
                                    classifiedMeters = 1000.0,
                                ),
                        ),
                    metadata =
                        SourceMetadata(
                            provider = OsmOverpassSource.PROVIDER,
                            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
                            confidence = 0.55f,
                        ),
                ),
        )

    private fun fixedJsonClient(json: String): RouteIntelligenceHttpClient =
        object : RouteIntelligenceHttpClient {
            override suspend fun get(
                url: String,
                attempt: RouteIntelligenceDiag.HttpAttemptContext?,
            ): Result<String> = Result.success(json)
        }

    private class FixedRouteDataSource(
        private val data: SourceData,
    ) : RouteDataSource {
        override suspend fun enrich(route: RawRouteForIntelligence): SourceData = data
    }
}
