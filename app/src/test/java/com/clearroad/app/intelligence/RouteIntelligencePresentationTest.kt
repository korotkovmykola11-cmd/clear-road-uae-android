package com.clearroad.app.intelligence

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.ui.model.RouteIntelligenceComparisonRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRequestUiModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RouteIntelligencePresentationTest {

    @Test
    fun driverRouteCopy_easyCityDrive() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 0,
                        roundaboutsCount = 0,
                        complexityScore = 0.12f,
                    ),
                alternative = null,
            )

        assertEquals("Easy city drive.", copy.headline)
        assertEquals("Simple route with few decision points.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_busyCityWithManyRoundabouts() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 2,
                        roundaboutsCount = 6,
                        complexityScore = 0.48f,
                        mainRoadRatio = 0.4f,
                    ),
                alternative = null,
            )

        assertEquals("Busy city drive.", copy.headline)
        assertEquals("Mostly city streets with many roundabouts.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_moreIntersectionsThanAlternativeRoute() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.4f),
                alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            )

        assertEquals("Busy city drive.", copy.headline)
        assertEquals("More intersections than the alternative route.", copy.explanation)
    }

    @Test
    fun driverRouteCopy_comparativeWordingUsesGoogleDefaultRouteWhenComparisonIndexZero() {
        val marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.55f)
        val alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f)

        assertEquals(
            "More intersections than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = marshio,
                alternative = alternative,
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "Fewer intersections than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "Expect a busier drive than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 0,
            ),
        )
        assertEquals(
            "A calmer drive than Google's default route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternativeRouteIndex = 0,
            ),
        )
    }

    @Test
    fun driverRouteCopy_comparativeWordingUsesAlternativeRouteWhenComparisonIndexGreaterThanZero() {
        val marshio = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.55f)
        val alternative = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f)

        assertEquals(
            "More intersections than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = marshio,
                alternative = alternative,
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "Fewer intersections than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 3, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 7, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "Expect a busier drive than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternativeRouteIndex = 1,
            ),
        )
        assertEquals(
            "A calmer drive than the alternative route.",
            RouteIntelligencePresentation.driverExplanation(
                marshio = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.35f),
                alternative = profile(trafficSignalsCount = 4, roundaboutsCount = 1, complexityScore = 0.55f),
                alternativeRouteIndex = 1,
            ),
        )
    }

    @Test
    fun driverRouteCopy_smoothHighwayDrive() {
        val copy =
            RouteIntelligencePresentation.driverRouteCopy(
                marshio =
                    profile(
                        trafficSignalsCount = 1,
                        roundaboutsCount = 0,
                        complexityScore = 0.18f,
                        mainRoadRatio = 0.72f,
                    ),
                alternative = null,
            )

        assertEquals("Smooth highway drive.", copy.headline)
        assertEquals("Mostly highway driving with fewer decision points.", copy.explanation)
    }

    @Test
    fun driverDetailLines_avoidRawCounts() {
        val row =
            RouteIntelligencePresentation.rowUiModel(
                label = "MARSHIO route",
                routeName = "E44",
                profile =
                    profile(
                        trafficSignalsCount = 1,
                        roundaboutsCount = 13,
                        complexityScore = 0.62f,
                        mainRoadRatio = 0.42f,
                    ),
            )!!

        assertEquals("Few traffic lights", row.trafficSignalsLine)
        assertEquals("Many roundabouts", row.roundaboutsLine)
        assertEquals("Mix of main and local streets", row.mainRoadLine)
        assertEquals("Higher driving workload", row.complexityLine)
    }

    @Test
    fun comparisonRouteLabel_googleDefaultWhenIndexZero() {
        assertEquals("Google default", RouteIntelligencePresentation.comparisonRouteLabel(0))
        assertEquals("Alternative", RouteIntelligencePresentation.comparisonRouteLabel(1))
    }

    private fun profile(
        trafficSignalsCount: Int?,
        roundaboutsCount: Int?,
        complexityScore: Float?,
        mainRoadRatio: Float = 0.5f,
    ): RouteProfile =
        RouteProfile(
            trafficSignalsCount = trafficSignalsCount,
            roundaboutsCount = roundaboutsCount,
            mainRoadRatio = mainRoadRatio,
            complexityScore = complexityScore,
            osmSourceStatus = SourceStatus.OK,
            evidence = null,
            metadata = null,
        )
}

class RouteIntelligenceLoadBehaviorTest {

    private val routePath =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
        )

    @Test
    fun load_fetchesSelectedRouteBeforeComparisonRoute() =
        runBlocking {
            val marshioFinished = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            val service = serviceWithSource { route ->
                events.add("start-${route.routeIndex}")
                if (route.routeIndex == 0) {
                    delay(30)
                    marshioFinished.complete(Unit)
                } else {
                    marshioFinished.await()
                }
                events.add("end-${route.routeIndex}")
                okSourceData(signals = 3)
            }

            RouteIntelligencePresentation.load(
                request(marshioIndex = 0, alternativeIndex = 1),
                service = service,
            )

            assertEquals(listOf("start-0", "end-0", "start-1", "end-1"), events)
        }

    @Test
    fun load_requestsEachComparedRouteProfileOnce() =
        runBlocking {
            val enrichCounts = mutableMapOf<Int, Int>()
            val service =
                serviceWithSource { route ->
                    enrichCounts[route.routeIndex] = (enrichCounts[route.routeIndex] ?: 0) + 1
                    okSourceData(signals = route.routeIndex + 1)
                }

            RouteIntelligencePresentation.load(
                request(marshioIndex = 0, alternativeIndex = 1),
                service = service,
            )

            assertEquals(1, enrichCounts[0])
            assertEquals(1, enrichCounts[1])
            assertNull(enrichCounts[2])
        }

    @Test
    fun comparisonReportFromLoad_performsZeroAdditionalSourceFetches() =
        runBlocking {
            var enrichCalls = 0
            val service =
                serviceWithSource { _ ->
                    enrichCalls++
                    okSourceData(signals = 4)
                }

            val loadResult =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )
            val callsAfterLoad = enrichCalls

            val report = RouteIntelligencePresentation.comparisonReportFromLoad(loadResult)

            assertEquals(callsAfterLoad, enrichCalls)
            assertEquals(2, report.reports.size)
        }

    @Test
    fun load_secondSessionWithCachedProfilesSkipsSourceFetch() =
        runBlocking {
            var enrichCalls = 0
            val service =
                serviceWithSource { route ->
                    enrichCalls++
                    okSourceData(signals = route.routeIndex + 2)
                }
            val request = request(marshioIndex = 0, alternativeIndex = 1)

            RouteIntelligencePresentation.load(request, service = service)
            val callsAfterFirstLoad = enrichCalls

            val secondResult = RouteIntelligencePresentation.load(request, service = service)

            assertEquals(callsAfterFirstLoad, enrichCalls)
            assertEquals(2, callsAfterFirstLoad)
            assertTrue(secondResult.uiModel.available)
            assertNotNull(secondResult.uiModel.marshioRoute)
            assertNotNull(secondResult.uiModel.alternativeRoute)
        }

    @Test
    fun load_cachedProfilesStillProduceReportWithoutRefetch() =
        runBlocking {
            var enrichCalls = 0
            val service =
                serviceWithSource { _ ->
                    enrichCalls++
                    okSourceData(signals = 4)
                }
            val request = request(marshioIndex = 0, alternativeIndex = 1)

            val firstResult = RouteIntelligencePresentation.load(request, service = service)
            RouteIntelligencePresentation.load(request, service = service)
            val callsAfterLoads = enrichCalls

            val report = RouteIntelligencePresentation.comparisonReportFromLoad(firstResult)

            assertEquals(callsAfterLoads, enrichCalls)
            assertEquals(2, report.reports.size)
        }

    @Test
    fun load_doesNotFetchUnrelatedThirdRouteForReporting() =
        runBlocking {
            val enrichedIndices = mutableSetOf<Int>()
            val service =
                serviceWithSource { route ->
                    enrichedIndices.add(route.routeIndex)
                    okSourceData(signals = 2)
                }

            RouteIntelligencePresentation.load(
                request(marshioIndex = 1, alternativeIndex = 0),
                service = service,
            )

            assertEquals(setOf(0, 1), enrichedIndices)
        }

    @Test
    fun load_marshioUnavailableComparisonOk_producesPartialCard() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    if (route.routeIndex == 1) {
                        unavailableSourceData()
                    } else {
                        okSourceData(signals = 5)
                    }
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 1, alternativeIndex = 0),
                    service = service,
                )

            assertTrue(result.uiModel.available)
            assertFalse(result.uiModel.loading)
            assertNull(result.uiModel.marshioRoute)
            assertNotNull(result.uiModel.alternativeRoute)
            assertEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
                result.uiModel.summaryLine,
            )
            assertEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_EXPLANATION,
                result.uiModel.explanationLine,
            )
            assertFalse(result.uiModel.summaryLine.contains("Steady city drive"))
            assertEquals(
                RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
                RouteIntelligenceDiag.classifyPresentationResult(
                    marshioStatus = SourceStatus.UNAVAILABLE,
                    alternativeStatus = SourceStatus.OK,
                ),
            )
        }

    @Test
    fun load_marshioOkComparisonUnavailable_producesPartialCard() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    if (route.routeIndex == 0) {
                        okSourceData(signals = 5)
                    } else {
                        unavailableSourceData()
                    }
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )

            assertTrue(result.uiModel.available)
            assertNotNull(result.uiModel.marshioRoute)
            assertNull(result.uiModel.alternativeRoute)
            assertNotEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
                result.uiModel.summaryLine,
            )
            val expectedSelectedCopy =
                RouteIntelligencePresentation.driverRouteCopy(
                    marshio = result.marshioProfile,
                    alternative = null,
                )
            assertEquals(expectedSelectedCopy.headline, result.uiModel.summaryLine)
            assertEquals(expectedSelectedCopy.explanation, result.uiModel.explanationLine)
            assertEquals(
                RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
                RouteIntelligenceDiag.classifyPresentationResult(
                    marshioStatus = SourceStatus.OK,
                    alternativeStatus = SourceStatus.UNAVAILABLE,
                ),
            )
        }

    @Test
    fun load_bothUnavailable_producesUnavailableCard() =
        runBlocking {
            val service =
                serviceWithSource { _ ->
                    unavailableSourceData()
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )

            assertFalse(result.uiModel.available)
            assertEquals(
                RouteIntelligencePresentation.UNAVAILABLE_MESSAGE,
                result.uiModel.unavailableMessage,
            )
        }

    @Test
    fun load_bothOk_producesAvailableCard() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    okSourceData(signals = route.routeIndex + 2)
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )

            assertTrue(result.uiModel.available)
            assertNotNull(result.uiModel.marshioRoute)
            assertNotNull(result.uiModel.alternativeRoute)
            assertNotEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
                result.uiModel.summaryLine,
            )
            val expectedCopy =
                RouteIntelligencePresentation.driverRouteCopy(
                    marshio = result.marshioProfile,
                    alternative = result.alternativeProfile,
                    alternativeRouteIndex = result.alternativeRoute?.routeIndex,
                )
            assertEquals(expectedCopy.headline, result.uiModel.summaryLine)
            assertEquals(expectedCopy.explanation, result.uiModel.explanationLine)
            assertEquals(
                RouteIntelligenceDiag.PresentationCardResult.CARD_AVAILABLE,
                RouteIntelligenceDiag.classifyPresentationResult(
                    marshioStatus = SourceStatus.OK,
                    alternativeStatus = SourceStatus.OK,
                ),
            )
        }

    @Test
    fun load_recommendedIndexZero_comparesAgainstIndexOne() =
        runBlocking {
            val routes =
                listOf(
                    routeWithPath(index = 0),
                    routeWithPath(index = 1),
                    routeWithPath(index = 2),
                )

            val request =
                RouteIntelligenceAssembly.buildRequest(
                    routes = routes,
                    identities = emptyList(),
                    recommendedIndex = 0,
                    isRecommendedRouteDetails = true,
                )!!

            assertEquals(0, request.marshioRoute.routeIndex)
            assertEquals(1, request.alternativeRoute?.routeIndex)
        }

    @Test
    fun load_recommendedNonZero_comparesAgainstIndexZero() =
        runBlocking {
            val routes =
                listOf(
                    routeWithPath(index = 0),
                    routeWithPath(index = 1),
                )

            val request =
                RouteIntelligenceAssembly.buildRequest(
                    routes = routes,
                    identities = emptyList(),
                    recommendedIndex = 1,
                    isRecommendedRouteDetails = true,
                )!!

            assertEquals(1, request.marshioRoute.routeIndex)
            assertEquals(0, request.alternativeRoute?.routeIndex)
        }

    @Test
    fun load_usesComparisonRequestRouteIdentity() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    okSourceData(signals = route.routeIndex + 3)
                }

            val loadResult =
                RouteIntelligencePresentation.load(
                    request = request(marshioIndex = 1, alternativeIndex = 0),
                    comparisonRequest =
                        RouteIntelligenceComparisonRequestUiModel(
                            routes =
                                listOf(
                                    routeInput(0),
                                    routeInput(1),
                                    routeInput(2),
                                ),
                            marshioSelectedRouteIndex = 1,
                            googleDefaultRouteIndex = 0,
                        ),
                    service = service,
                )

            assertEquals(0, loadResult.googleDefaultRouteIndex)
            assertEquals(1, loadResult.marshioSelectedRouteIndex)

            val report = RouteIntelligencePresentation.comparisonReportFromLoad(loadResult)
            assertEquals(2, report.reports.size)
            assertTrue(report.reports.none { it.routeIndex == 2 })
            assertTrue(report.reports.first { it.routeIndex == 0 }.isGoogleDefault)
            assertTrue(report.reports.first { it.routeIndex == 1 }.isMarshioSelected)
        }

    @Test
    fun comparisonReportFromLoad_marksGoogleDefaultAndMarshioSelected() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    okSourceData(signals = route.routeIndex + 3)
                }

            val loadResult =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 1, alternativeIndex = 0),
                    service = service,
                )

            val report = RouteIntelligencePresentation.comparisonReportFromLoad(loadResult)

            assertEquals(2, report.reports.size)
            assertEquals(0, report.googleDefaultRouteIndex)
            assertEquals(1, report.marshioSelectedRouteIndex)
            val googleReport = report.reports.first { it.routeIndex == 0 }
            val marshioReport = report.reports.first { it.routeIndex == 1 }
            assertTrue(googleReport.isGoogleDefault)
            assertFalse(googleReport.isMarshioSelected)
            assertFalse(marshioReport.isGoogleDefault)
            assertTrue(marshioReport.isMarshioSelected)
        }

    @Test
    fun comparisonReportFromLoad_containsOnlyUiComparisonPair() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    okSourceData(signals = route.routeIndex + 1)
                }

            val loadResult =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )

            val report = RouteIntelligencePresentation.comparisonReportFromLoad(loadResult)

            assertEquals(2, report.reports.size)
            assertEquals(setOf(0, 1), report.reports.map { it.routeIndex }.toSet())
        }

    private fun request(
        marshioIndex: Int,
        alternativeIndex: Int,
    ) =
        RouteIntelligenceRequestUiModel(
            marshioRoute = routeInput(marshioIndex),
            alternativeRoute = routeInput(alternativeIndex),
        )

    private fun routeInput(index: Int) =
        com.clearroad.app.ui.model.RouteIntelligenceRouteInputUiModel(
            routeIndex = index,
            routeName = "Route ${index + 1}",
            routePathPoints =
                listOf(
                    LatLng(25.0 + index * 0.01, 55.0),
                    LatLng(25.01 + index * 0.01, 55.0),
                ),
            durationSeconds = 600 + index * 60,
            distanceMeters = 5000,
            criticalManeuversCount = index,
        )

    private fun serviceWithSource(
        enrich: suspend (RawRouteForIntelligence) -> SourceData,
    ): RouteIntelligenceService =
        RouteIntelligenceService(
            osmAggregator = RouteIntelligenceDataAggregator(listOf(enrichingSource(enrich))),
            cache = RouteIntelligenceCache(),
        )

    private fun enrichingSource(
        enrich: suspend (RawRouteForIntelligence) -> SourceData,
    ): RouteDataSource =
        object : RouteDataSource {
            override suspend fun enrich(route: RawRouteForIntelligence): SourceData = enrich(route)
        }

    private fun okSourceData(signals: Int): SourceData =
        SourceData(
            provider = OsmOverpassSource.PROVIDER,
            status = SourceStatus.OK,
            trafficSignalsCount = signals,
            roundaboutsCount = 1,
            mainRoadRatio = 0.6f,
            evidence =
                RouteEvidence(
                    trafficLights =
                        TrafficLightEvidence(
                            count = signals,
                            osmNodeIds = emptyList(),
                            nearRoutePositions = emptyList(),
                        ),
                    roundabouts =
                        RoundaboutEvidence(
                            count = 1,
                            osmWayIds = emptyList(),
                            nearRoutePositions = emptyList(),
                        ),
                    mainRoad =
                        MainRoadEvidence(
                            mainRoadRatio = 0.6f,
                            mainRoadMeters = 600.0,
                            classifiedMeters = 1000.0,
                        ),
                ),
            metadata =
                SourceMetadata(
                    provider = OsmOverpassSource.PROVIDER,
                    fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
                    confidence = 0.8f,
                ),
        )

    private fun unavailableSourceData(): SourceData =
        SourceData(
            provider = OsmOverpassSource.PROVIDER,
            status = SourceStatus.UNAVAILABLE,
            trafficSignalsCount = null,
            roundaboutsCount = null,
            mainRoadRatio = null,
            evidence = null,
            metadata = null,
        )

    private fun notApplicableSourceData(): SourceData =
        SourceData(
            provider = OsmOverpassSource.PROVIDER,
            status = SourceStatus.NOT_APPLICABLE,
            trafficSignalsCount = null,
            roundaboutsCount = null,
            mainRoadRatio = null,
            evidence = null,
            metadata =
                SourceMetadata(
                    provider = OsmOverpassSource.PROVIDER,
                    fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
                    confidence = 0.0f,
                ),
        )

    @Test
    fun load_marshioNotApplicableAlternativeOk_producesHonestPartialCopy() =
        runBlocking {
            val service =
                serviceWithSource { route ->
                    if (route.routeIndex == 1) {
                        notApplicableSourceData()
                    } else {
                        okSourceData(signals = 5)
                    }
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 1, alternativeIndex = 0),
                    service = service,
                )

            assertTrue(result.uiModel.available)
            assertNull(result.uiModel.marshioRoute)
            assertNotNull(result.uiModel.alternativeRoute)
            assertEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
                result.uiModel.summaryLine,
            )
            assertEquals(
                RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_EXPLANATION,
                result.uiModel.explanationLine,
            )
            assertEquals(
                RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
                RouteIntelligenceDiag.classifyPresentationResult(
                    marshioStatus = SourceStatus.NOT_APPLICABLE,
                    alternativeStatus = SourceStatus.OK,
                ),
            )
        }

    @Test
    fun load_bothNotApplicable_producesUnavailableCard() =
        runBlocking {
            val service =
                serviceWithSource { _ ->
                    notApplicableSourceData()
                }

            val result =
                RouteIntelligencePresentation.load(
                    request(marshioIndex = 0, alternativeIndex = 1),
                    service = service,
                )

            assertFalse(result.uiModel.available)
            assertEquals(
                RouteIntelligencePresentation.UNAVAILABLE_MESSAGE,
                result.uiModel.unavailableMessage,
            )
            assertEquals(
                RouteIntelligenceDiag.PresentationCardResult.CARD_UNAVAILABLE,
                RouteIntelligenceDiag.classifyPresentationResult(
                    marshioStatus = SourceStatus.NOT_APPLICABLE,
                    alternativeStatus = SourceStatus.NOT_APPLICABLE,
                ),
            )
        }

    @Test
    fun classifyPresentationResult_treatsOnlyOkAsUsable() {
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_AVAILABLE,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.OK,
                alternativeStatus = SourceStatus.OK,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.PARTIAL,
                alternativeStatus = SourceStatus.OK,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_PARTIAL,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.OK,
                alternativeStatus = SourceStatus.PARTIAL,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_UNAVAILABLE,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.PARTIAL,
                alternativeStatus = SourceStatus.UNAVAILABLE,
            ),
        )
        assertEquals(
            RouteIntelligenceDiag.PresentationCardResult.CARD_UNAVAILABLE,
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = SourceStatus.NOT_APPLICABLE,
                alternativeStatus = SourceStatus.NOT_APPLICABLE,
            ),
        )
    }

    @Test
    fun buildUiModel_selectedPartialAlternativeOk_usesHonestPartialCopy() {
        val uiModel =
            RouteIntelligencePresentation.buildUiModel(
                request = request(marshioIndex = 1, alternativeIndex = 0),
                marshioProfile = osmProfile(status = SourceStatus.PARTIAL),
                alternativeProfile = osmProfile(status = SourceStatus.OK, signals = 4),
            )

        assertTrue(uiModel.available)
        assertNull(uiModel.marshioRoute)
        assertNotNull(uiModel.alternativeRoute)
        assertEquals(
            RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
            uiModel.summaryLine,
        )
        assertEquals(
            RouteIntelligencePresentation.SELECTED_ROUTE_UNAVAILABLE_EXPLANATION,
            uiModel.explanationLine,
        )
        assertFalse(uiModel.summaryLine.contains("Steady city drive"))
    }

    @Test
    fun buildUiModel_bothNonOkProfiles_producesUnavailableCard() {
        val uiModel =
            RouteIntelligencePresentation.buildUiModel(
                request = request(marshioIndex = 0, alternativeIndex = 1),
                marshioProfile = osmProfile(status = SourceStatus.PARTIAL),
                alternativeProfile = osmProfile(status = SourceStatus.NOT_APPLICABLE),
            )

        assertFalse(uiModel.available)
        assertEquals(
            RouteIntelligencePresentation.UNAVAILABLE_MESSAGE,
            uiModel.unavailableMessage,
        )
    }

    private fun osmProfile(
        status: SourceStatus,
        signals: Int? = null,
    ): RouteProfile =
        RouteProfile(
            trafficSignalsCount = signals,
            roundaboutsCount = if (status == SourceStatus.OK) 1 else null,
            mainRoadRatio = if (status == SourceStatus.OK) 0.5f else null,
            complexityScore = if (status == SourceStatus.OK) 0.4f else null,
            osmSourceStatus = status,
            evidence = null,
            metadata = null,
        )

    private fun routeWithPath(index: Int) =
        RealRouteDebugData(
            distanceText = "5 km",
            durationText = "10 min",
            distanceMeters = 5000,
            durationSeconds = 600 + index * 60,
            tollAED = 0,
            hasToll = false,
            routePathPoints =
                listOf(
                    LatLng(25.0 + index * 0.001, 55.0),
                    LatLng(25.01 + index * 0.001, 55.0),
                ),
            criticalManeuversCount = index,
            googleSteps =
                listOf(
                    com.clearroad.app.DirectionsStepRecord(500, "merge"),
                ),
        )
}
