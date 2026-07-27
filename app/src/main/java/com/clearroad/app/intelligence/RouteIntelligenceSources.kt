package com.clearroad.app.intelligence

import java.time.Instant

internal class OsmOverpassSource(
    private val httpClient: RouteIntelligenceHttpClient = RouteIntelligenceHttp,
    private val primaryEndpoint: String = PRIMARY_ENDPOINT,
    private val fallbackEndpoint: String = FALLBACK_ENDPOINT,
) : RouteDataSource {

    override suspend fun enrich(route: RawRouteForIntelligence): SourceData {
        if (route.routePathPoints.size < 2) {
            return finalize(route.routeIndex, unavailable("Route geometry unavailable"))
        }

        val bbox =
            RoutePolylineGeometry.boundingBoxExpanded(route.routePathPoints)
                ?: return finalize(route.routeIndex, unavailable("Could not compute bounding box"))

        val query = OsmOverpassQueryBuilder.build(bbox)
        val json =
            fetchOverpassJson(query, route.routeIndex)
                ?: return finalize(route.routeIndex, unavailable("Overpass request failed"))

        when (val parseResult = OsmOverpassParser.parseResponse(json)) {
            is OsmOverpassParser.ParseResult.MalformedJson -> {
                RouteIntelligenceDiag.logParse(
                    routeIndex = route.routeIndex,
                    error = RouteIntelligenceDiag.ParseErrorKind.MalformedJson,
                )
                return finalize(route.routeIndex, unavailable(parseResult.errorMessage))
            }
            is OsmOverpassParser.ParseResult.Ok -> {
                val features =
                    OsmRouteFeatureExtractor.extract(route.routePathPoints, parseResult.elements)
                val fetchedAt = Instant.now()

                return finalize(
                    route.routeIndex,
                    SourceData(
                        provider = PROVIDER,
                        status = SourceStatus.OK,
                        trafficSignalsCount = features.trafficSignalsCount,
                        roundaboutsCount = features.roundaboutsCount,
                        mainRoadRatio = features.mainRoadRatio,
                        roadTypeBreakdown = features.roadTypeBreakdown,
                        evidence =
                            RouteEvidence(
                                trafficLights = features.trafficLightEvidence,
                                roundabouts = features.roundaboutEvidence,
                                mainRoad = features.mainRoadEvidence,
                            ),
                        metadata =
                            SourceMetadata(
                                provider = PROVIDER,
                                fetchedAt = fetchedAt,
                                confidence = MEDIUM_CONFIDENCE,
                            ),
                    ),
                )
            }
        }
    }

    internal suspend fun fetchOverpassJson(
        query: String,
        routeIndex: Int,
    ): String? {
        val primaryUrl = RouteIntelligenceHttp.buildOverpassUrl(primaryEndpoint, query)
        val primaryAttempt =
            RouteIntelligenceDiag.HttpAttemptContext(
                routeIndex = routeIndex,
                endpoint = RouteIntelligenceDiag.EndpointRole.PRIMARY,
            )
        val primaryResult = httpClient.get(primaryUrl, primaryAttempt)
        if (primaryResult.isSuccess) {
            return primaryResult.getOrNull()
        }
        val fallbackUrl = RouteIntelligenceHttp.buildOverpassUrl(fallbackEndpoint, query)
        val fallbackAttempt =
            RouteIntelligenceDiag.HttpAttemptContext(
                routeIndex = routeIndex,
                endpoint = RouteIntelligenceDiag.EndpointRole.FALLBACK,
            )
        val fallbackResult = httpClient.get(fallbackUrl, fallbackAttempt)
        return fallbackResult.getOrNull()
    }

    private fun finalize(
        routeIndex: Int,
        data: SourceData,
    ): SourceData {
        RouteIntelligenceDiag.logOsmResult(routeIndex, data.status)
        return data
    }

    private fun unavailable(reason: String): SourceData =
        SourceData(
            provider = PROVIDER,
            status = SourceStatus.UNAVAILABLE,
            trafficSignalsCount = null,
            roundaboutsCount = null,
            mainRoadRatio = null,
            evidence = null,
            metadata = null,
            errorMessage = reason,
        )

    companion object {
        const val PROVIDER = "OSM Overpass"
        const val PRIMARY_ENDPOINT = "https://overpass-api.de/api/interpreter"
        const val FALLBACK_ENDPOINT = "https://overpass.kumi.systems/api/interpreter"
        const val MEDIUM_CONFIDENCE = 0.55f
    }
}
