package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteIntelligenceReportBuilderTest {

    private val routePath =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
        )

    @Test
    fun buildComparisonReport_includesAllRoutes() {
        val reports =
            listOf(
                sampleReport(
                    routeIndex = 0,
                    durationSeconds = 600,
                    signalsCount = 9,
                    complexity = 0.7f,
                    mainRoadRatio = 0.62f,
                    isGoogleDefault = true,
                    isMarshioSelected = false,
                ),
                sampleReport(
                    routeIndex = 1,
                    durationSeconds = 660,
                    signalsCount = 3,
                    complexity = 0.3f,
                    mainRoadRatio = 0.81f,
                    isGoogleDefault = false,
                    isMarshioSelected = true,
                ),
                sampleReport(
                    routeIndex = 2,
                    durationSeconds = 720,
                    signalsCount = 5,
                    complexity = 0.5f,
                    mainRoadRatio = 0.55f,
                ),
            )

        val comparison =
            RouteIntelligenceReportBuilder.buildComparison(
                reports = reports,
                marshioSelectedRouteIndex = 1,
                googleDefaultRouteIndex = 0,
            )

        assertEquals(3, comparison.reports.size)
        assertEquals(0, comparison.googleDefaultRouteIndex)
        assertEquals(1, comparison.marshioSelectedRouteIndex)
        assertTrue(comparison.reports[1].isMarshioSelected)
        assertTrue(comparison.reports[0].isGoogleDefault)
    }

    @Test
    fun buildComparisonSummary_selectsLeastSignalsRouteIndex() {
        val reports =
            listOf(
                sampleReport(routeIndex = 0, durationSeconds = 600, signalsCount = 9, complexity = 0.4f),
                sampleReport(routeIndex = 1, durationSeconds = 620, signalsCount = 3, complexity = 0.5f),
                sampleReport(routeIndex = 2, durationSeconds = 640, signalsCount = 7, complexity = 0.6f),
            )

        val summary = RouteIntelligenceReportBuilder.buildSummary(reports)

        assertEquals(1, summary.leastSignalsRouteIndex)
    }

    @Test
    fun buildComparisonSummary_selectsLowestComplexityRouteIndex() {
        val reports =
            listOf(
                sampleReport(routeIndex = 0, durationSeconds = 600, signalsCount = 4, complexity = 0.8f),
                sampleReport(routeIndex = 1, durationSeconds = 610, signalsCount = 4, complexity = 0.2f),
                sampleReport(routeIndex = 2, durationSeconds = 620, signalsCount = 4, complexity = 0.5f),
            )

        val summary = RouteIntelligenceReportBuilder.buildSummary(reports)

        assertEquals(1, summary.lowestComplexityRouteIndex)
    }

    @Test
    fun buildReport_partialSourceData_doesNotCrash() {
        val route =
            RawRouteForIntelligence(
                routeIndex = 0,
                routeName = "Route 1",
                routePathPoints = routePath,
                durationSeconds = 600,
                distanceMeters = 5000,
                googleSteps = emptyList(),
                criticalManeuversCount = 2,
            )
        val profile =
            RouteProfile(
                trafficSignalsCount = null,
                roundaboutsCount = null,
                mainRoadRatio = null,
                complexityScore = null,
                osmSourceStatus = SourceStatus.UNAVAILABLE,
                evidence = null,
                metadata = null,
            )
        val googleFeatures = GoogleRouteFeatureReader.read(route)
        val profileWithGoogle =
            profile.copy(googleSourceStatus = googleFeatures.googleSourceStatus)

        val report =
            RouteIntelligenceReportBuilder.build(
                route = route,
                profile = profileWithGoogle,
                osmSource = null,
                googleFeatures = googleFeatures,
                isGoogleDefault = true,
                isMarshioSelected = false,
            )

        assertEquals(2, report.signals.criticalManeuversCount)
        assertEquals(SourceStatus.UNAVAILABLE, report.profile.osmSourceStatus)
        assertEquals(SourceStatus.PARTIAL, report.profile.googleSourceStatus)
        assertNull(report.signals.trafficSignalsCount)
    }

    @Test
    fun buildComparisonSummary_routeFactLines_areFactsOnly() {
        val reports =
            listOf(
                sampleReport(
                    routeIndex = 0,
                    durationSeconds = 600,
                    signalsCount = 9,
                    complexity = 0.7f,
                    mainRoadRatio = 0.62f,
                ),
                sampleReport(
                    routeIndex = 1,
                    durationSeconds = 660,
                    signalsCount = 3,
                    complexity = 0.3f,
                    mainRoadRatio = 0.81f,
                ),
            )

        val summary = RouteIntelligenceReportBuilder.buildSummary(reports)

        assertEquals(
            "Route 0: fastest, 9 signals, 1 roundabout, 62% main roads",
            summary.routeFactLines[0],
        )
        assertEquals(
            "Route 1: +1 min, 3 signals, 1 roundabout, 81% main roads",
            summary.routeFactLines[1],
        )
    }

    private fun sampleReport(
        routeIndex: Int,
        durationSeconds: Int,
        signalsCount: Int,
        complexity: Float,
        mainRoadRatio: Float = 0.5f,
        isGoogleDefault: Boolean = false,
        isMarshioSelected: Boolean = false,
    ): RouteIntelligenceReport {
        val profile =
            RouteProfile(
                trafficSignalsCount = signalsCount,
                roundaboutsCount = 1,
                mainRoadRatio = mainRoadRatio,
                complexityScore = complexity,
                osmSourceStatus = SourceStatus.OK,
                evidence = null,
                metadata = null,
            )
        return RouteIntelligenceReport(
            routeIndex = routeIndex,
            routeName = "Route ${routeIndex + 1}",
            isGoogleDefault = isGoogleDefault,
            isMarshioSelected = isMarshioSelected,
            durationSeconds = durationSeconds,
            distanceMeters = 5000,
            profile = profile,
            signals =
                RouteIntelligenceSignals(
                    trafficSignalsCount = signalsCount,
                    roundaboutsCount = 1,
                    mainRoadRatio = mainRoadRatio,
                    complexityScore = complexity,
                    criticalManeuversCount = 2,
                    turnsCount = 1,
                    roundaboutsFromManeuvers = 1,
                    rampOrExitCount = 0,
                    restrictedRoadHintsCount = null,
                    roadTypeBreakdown = null,
                    osmSourceStatus = SourceStatus.OK,
                    googleSourceStatus = SourceStatus.OK,
                ),
            confidence = 0.7f,
            osmEvidence = null,
            googleEvidence = null,
        )
    }
}

class GoogleRouteFeatureReaderTest {

    @Test
    fun read_missingSteps_returnsPartialWhenCriticalCountKnown() {
        val result =
            GoogleRouteFeatureReader.read(
                RawRouteForIntelligence(
                    routeIndex = 0,
                    routeName = "Route 1",
                    routePathPoints = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                    durationSeconds = 600,
                    distanceMeters = 5000,
                    googleSteps = emptyList(),
                    criticalManeuversCount = 4,
                ),
            )

        assertEquals(SourceStatus.PARTIAL, result.googleSourceStatus)
        assertEquals(4, result.maneuvers?.criticalManeuversCount)
    }

    @Test
    fun read_missingStepsAndCount_returnsUnavailable() {
        val result =
            GoogleRouteFeatureReader.read(
                RawRouteForIntelligence(
                    routeIndex = 0,
                    routeName = "Route 1",
                    routePathPoints = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                    durationSeconds = 600,
                    distanceMeters = 5000,
                ),
            )

        assertEquals(SourceStatus.UNAVAILABLE, result.googleSourceStatus)
        assertNull(result.maneuvers)
    }

    @Test
    fun read_withSteps_extractsManeuverCounts() {
        val result =
            GoogleRouteFeatureReader.read(
                RawRouteForIntelligence(
                    routeIndex = 0,
                    routeName = "Route 1",
                    routePathPoints = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                    durationSeconds = 600,
                    distanceMeters = 5000,
                    googleSteps =
                        listOf(
                            GoogleDirectionsStepInput(500, "merge"),
                            GoogleDirectionsStepInput(500, "roundabout-left"),
                            GoogleDirectionsStepInput(500, "ramp-right"),
                            GoogleDirectionsStepInput(500, "turn-left"),
                        ),
                ),
            )

        assertEquals(SourceStatus.OK, result.googleSourceStatus)
        assertNotNull(result.maneuvers)
        assertEquals(3, result.maneuvers?.criticalManeuversCount)
        assertEquals(3, result.maneuvers?.turnsCount)
        assertEquals(1, result.maneuvers?.roundaboutsFromManeuvers)
        assertEquals(1, result.maneuvers?.rampOrExitCount)
    }
}

class RouteIntelligenceAssemblyReportTest {

    @Test
    fun buildComparisonRequest_includesAllRoutesWithGeometry() {
        val routes =
            listOf(
                routeWithPath(index = 0),
                routeWithPath(index = 1),
                routeWithPath(index = 2),
            )

        val request =
            RouteIntelligenceAssembly.buildComparisonRequest(
                routes = routes,
                identities = emptyList(),
                recommendedIndex = 1,
                isRecommendedRouteDetails = true,
            )

        assertNotNull(request)
        assertEquals(3, request!!.routes.size)
        assertEquals(1, request.marshioSelectedRouteIndex)
        assertEquals(0, request.googleDefaultRouteIndex)
    }

    @Test
    fun unavailableReason_shapeMissingWhenMarshioPolylineTooShort() {
        val routes =
            listOf(
                routeWithPath(index = 0),
                routeWithPath(index = 1).copy(routePathPoints = emptyList()),
            )

        val reason =
            RouteIntelligenceAssembly.unavailableReason(
                routes = routes,
                recommendedIndex = 1,
                isRecommendedRouteDetails = true,
            )

        assertEquals("Route shape unavailable — intelligence skipped", reason)
    }

    @Test
    fun unavailableReason_nullWhenGeometryAndAlternativesExist() {
        val routes =
            listOf(
                routeWithPath(index = 0),
                routeWithPath(index = 1),
            )

        val reason =
            RouteIntelligenceAssembly.unavailableReason(
                routes = routes,
                recommendedIndex = 1,
                isRecommendedRouteDetails = true,
            )

        assertNull(reason)
    }

    @Test
    fun unavailableReason_notEnoughAlternativesForSingleRoute() {
        val routes = listOf(routeWithPath(index = 0))

        val reason =
            RouteIntelligenceAssembly.unavailableReason(
                routes = routes,
                recommendedIndex = 0,
                isRecommendedRouteDetails = true,
            )

        assertEquals("Not enough route alternatives", reason)
    }

    private fun routeWithPath(index: Int) =
        com.clearroad.app.RealRouteDebugData(
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

class RoadTypeBreakdownTest {

    private val routePath =
        listOf(
            LatLng(25.0, 55.0),
            LatLng(25.01, 55.0),
        )

    @Test
    fun extract_aggregatesMetersByRoadClass() {
        val elements =
            listOf(
                wayElement(
                    osmId = 1,
                    highway = "motorway",
                    lat = 25.005,
                ),
                wayElement(
                    osmId = 2,
                    highway = "primary",
                    lat = 25.005,
                ),
                wayElement(
                    osmId = 3,
                    highway = "residential",
                    lat = 25.005,
                ),
                wayElement(
                    osmId = 4,
                    highway = "cycleway",
                    lat = 25.005,
                ),
            )

        val features = OsmRouteFeatureExtractor.extract(routePath, elements)
        val breakdown = features.roadTypeBreakdown

        assertNotNull(breakdown)
        assertTrue(breakdown!!.motorwayMeters > 0.0)
        assertTrue(breakdown.primaryMeters > 0.0)
        assertTrue(breakdown.residentialMeters > 0.0)
        assertTrue(breakdown.unknownMeters > 0.0)
        assertEquals(
            breakdown.totalClassifiedMeters,
            breakdown.motorwayMeters +
                breakdown.primaryMeters +
                breakdown.residentialMeters +
                breakdown.unknownMeters,
            0.01,
        )
    }

    @Test
    fun roadTypeBucket_mapsHighwayTags() {
        assertEquals(
            OsmRouteFeatureExtractor.RoadTypeBucket.MOTORWAY,
            OsmRouteFeatureExtractor.roadTypeBucket("motorway_link"),
        )
        assertEquals(
            OsmRouteFeatureExtractor.RoadTypeBucket.SECONDARY,
            OsmRouteFeatureExtractor.roadTypeBucket("secondary"),
        )
        assertEquals(
            OsmRouteFeatureExtractor.RoadTypeBucket.UNKNOWN,
            OsmRouteFeatureExtractor.roadTypeBucket("cycleway"),
        )
    }

    private fun wayElement(
        osmId: Long,
        highway: String,
        lat: Double,
    ): OsmOverpassElement =
        OsmOverpassElement(
            osmId = osmId,
            type = "way",
            tags = mapOf("highway" to highway),
            points =
                listOf(
                    LatLng(lat, 55.0),
                    LatLng(lat + 0.001, 55.0),
                ),
        )
}

class RouteIntelligenceReportJsonTest {

    @Test
    fun toJson_emitsComparisonReportShape() {
        val report =
            RouteIntelligenceComparisonReport(
                googleDefaultRouteIndex = 0,
                marshioSelectedRouteIndex = 1,
                reports =
                    listOf(
                        RouteIntelligenceReport(
                            routeIndex = 0,
                            routeName = "Route 1",
                            isGoogleDefault = true,
                            isMarshioSelected = false,
                            durationSeconds = 600,
                            distanceMeters = 5000,
                            profile =
                                RouteProfile(
                                    trafficSignalsCount = 9,
                                    roundaboutsCount = 1,
                                    mainRoadRatio = 0.62f,
                                    complexityScore = 0.7f,
                                    osmSourceStatus = SourceStatus.OK,
                                    evidence = null,
                                    metadata = null,
                                ),
                            signals =
                                RouteIntelligenceSignals(
                                    trafficSignalsCount = 9,
                                    roundaboutsCount = 1,
                                    mainRoadRatio = 0.62f,
                                    complexityScore = 0.7f,
                                    criticalManeuversCount = 3,
                                    turnsCount = 2,
                                    roundaboutsFromManeuvers = 1,
                                    rampOrExitCount = 1,
                                    restrictedRoadHintsCount = null,
                                    roadTypeBreakdown =
                                        RoadTypeBreakdown(
                                            motorwayMeters = 1000.0,
                                            primaryMeters = 500.0,
                                        ),
                                    osmSourceStatus = SourceStatus.OK,
                                    googleSourceStatus = SourceStatus.OK,
                                ),
                            confidence = 0.75f,
                            osmEvidence = null,
                            googleEvidence = null,
                        ),
                    ),
                summary =
                    RouteIntelligenceComparisonSummary(
                        fastestRouteIndex = 0,
                        lowestComplexityRouteIndex = 0,
                        mostMainRoadRouteIndex = 0,
                        leastSignalsRouteIndex = 0,
                        dataCompleteness = 1f,
                        routeFactLines = listOf("Route 0: fastest, 9 signals, 1 roundabout, 62% main roads"),
                    ),
            )

        val json = RouteIntelligenceReportJson.toJson(report)

        assertTrue(json.contains("\"googleDefaultRouteIndex\": 0"))
        assertTrue(json.contains("\"marshioSelectedRouteIndex\": 1"))
        assertTrue(json.contains("\"motorwayMeters\": 1000"))
        assertTrue(json.contains("\"leastSignalsRouteIndex\": 0"))
    }
}

class RouteIntelligenceReportLoggerTest {

    @Test
    fun formatRouteLine_matchesCompactSpec_underGermanLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val line =
                RouteIntelligenceReportLogger.formatRouteLine(
                    RouteIntelligenceReport(
                        routeIndex = 0,
                        routeName = "Route 1",
                        isGoogleDefault = true,
                        isMarshioSelected = false,
                        durationSeconds = 540,
                        distanceMeters = 5000,
                        profile =
                            RouteProfile(
                                trafficSignalsCount = 9,
                                roundaboutsCount = 1,
                                mainRoadRatio = 0.62f,
                                complexityScore = 0.7f,
                                osmSourceStatus = SourceStatus.OK,
                                evidence = null,
                                metadata = null,
                            ),
                        signals =
                            RouteIntelligenceSignals(
                                trafficSignalsCount = 9,
                                roundaboutsCount = 1,
                                mainRoadRatio = 0.62f,
                                complexityScore = 0.7f,
                                criticalManeuversCount = 3,
                                turnsCount = 2,
                                roundaboutsFromManeuvers = 1,
                                rampOrExitCount = 1,
                                restrictedRoadHintsCount = null,
                                roadTypeBreakdown = null,
                                osmSourceStatus = SourceStatus.OK,
                                googleSourceStatus = SourceStatus.OK,
                            ),
                        confidence = 0.75f,
                        osmEvidence = null,
                        googleEvidence = null,
                    ),
                )

            assertEquals(
                "route=0 googleDefault=true marshio=false eta=9min signals=9 roundabouts=1 mainRoadRatio=0.62 complexity=0.70 maneuvers=3",
                line,
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
