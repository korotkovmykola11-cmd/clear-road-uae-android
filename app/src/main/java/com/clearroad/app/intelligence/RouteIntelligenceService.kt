package com.clearroad.app.intelligence

import java.time.Instant

class RouteIntelligenceDataAggregator(
    private val sources: List<RouteDataSource>,
) {
    suspend fun enrich(route: RawRouteForIntelligence): EnrichedRoute {
        val sourceData = sources.map { source -> source.enrich(route) }
        return EnrichedRoute(raw = route, sources = sourceData)
    }
}

object RouteProfileBuilder {

    fun build(enriched: EnrichedRoute): RouteProfile {
        val osm = enriched.sources.firstOrNull { it.provider == OsmOverpassSource.PROVIDER }

        if (osm == null || osm.status == SourceStatus.UNAVAILABLE) {
            return RouteProfile(
                trafficSignalsCount = null,
                roundaboutsCount = null,
                mainRoadRatio = null,
                complexityScore = null,
                osmSourceStatus = SourceStatus.UNAVAILABLE,
                evidence = null,
                metadata = osm?.metadata,
            )
        }

        if (osm.status == SourceStatus.NOT_APPLICABLE) {
            return RouteProfile(
                trafficSignalsCount = null,
                roundaboutsCount = null,
                mainRoadRatio = null,
                complexityScore = null,
                osmSourceStatus = SourceStatus.NOT_APPLICABLE,
                evidence = null,
                metadata = osm.metadata,
            )
        }

        val complexity =
            ComplexityScoreCalculator.calculate(
                trafficSignalsCount = osm.trafficSignalsCount,
                roundaboutsCount = osm.roundaboutsCount,
                mainRoadRatio = osm.mainRoadRatio,
            )

        return RouteProfile(
            trafficSignalsCount = osm.trafficSignalsCount,
            roundaboutsCount = osm.roundaboutsCount,
            mainRoadRatio = osm.mainRoadRatio,
            complexityScore = complexity,
            osmSourceStatus = SourceStatus.OK,
            evidence = osm.evidence,
            metadata = osm.metadata,
        )
    }
}

class RouteIntelligenceCache(
    private val ttlMillis: Long = DEFAULT_TTL_MS,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private data class CacheEntry(
        val profile: RouteProfile,
        val expiresAtMillis: Long,
    )

    private val entries = mutableMapOf<String, CacheEntry>()

    fun get(polylineHash: String): RouteProfile? {
        val entry = entries[polylineHash] ?: return null
        if (entry.expiresAtMillis <= nowMillis()) {
            entries.remove(polylineHash)
            return null
        }
        return entry.profile
    }

    fun put(
        polylineHash: String,
        profile: RouteProfile,
    ) {
        entries[polylineHash] =
            CacheEntry(
                profile = profile,
                expiresAtMillis = nowMillis() + ttlMillis,
            )
    }

    fun clear() {
        entries.clear()
    }

    companion object {
        const val DEFAULT_TTL_MS = 30L * 60L * 1000L
    }
}

class RouteIntelligenceService(
    private val osmAggregator: RouteIntelligenceDataAggregator =
        RouteIntelligenceDataAggregator(
            sources = listOf(OsmOverpassSource()),
        ),
    private val cache: RouteIntelligenceCache = RouteIntelligenceCache(),
) {
    suspend fun profileFor(route: RawRouteForIntelligence): RouteProfile {
        val cacheKey = RoutePolylineGeometry.stablePolylineHash(route.routePathPoints)
        cache.get(cacheKey)?.let { return it }
        val profile = RouteProfileBuilder.build(osmAggregator.enrich(route))
        if (profile.osmSourceStatus == SourceStatus.OK) {
            cache.put(cacheKey, profile)
        }
        return profile
    }

    suspend fun reportFor(
        route: RawRouteForIntelligence,
        isGoogleDefault: Boolean,
        isMarshioSelected: Boolean,
    ): RouteIntelligenceReport {
        val osmEnriched = osmAggregator.enrich(route)
        val osmSource = osmEnriched.sources.firstOrNull { it.provider == OsmOverpassSource.PROVIDER }
        val googleFeatures = GoogleRouteFeatureReader.read(route)
        val profile =
            RouteProfileBuilder.build(osmEnriched).copy(
                googleSourceStatus = googleFeatures.googleSourceStatus,
            )
        return RouteIntelligenceReportBuilder.build(
            route = route,
            profile = profile,
            osmSource = osmSource,
            googleFeatures = googleFeatures,
            isGoogleDefault = isGoogleDefault,
            isMarshioSelected = isMarshioSelected,
        )
    }

    suspend fun comparisonReportFor(
        routes: List<RawRouteForIntelligence>,
        marshioSelectedRouteIndex: Int,
        googleDefaultRouteIndex: Int = 0,
    ): RouteIntelligenceComparisonReport {
        val marshioIdx = marshioSelectedRouteIndex.coerceIn(0, (routes.size - 1).coerceAtLeast(0))
        val reports =
            routes.map { route ->
                reportFor(
                    route = route,
                    isGoogleDefault = route.routeIndex == googleDefaultRouteIndex,
                    isMarshioSelected = route.routeIndex == marshioIdx,
                )
            }
        return RouteIntelligenceReportBuilder.buildComparison(
            reports = reports,
            marshioSelectedRouteIndex = marshioIdx,
            googleDefaultRouteIndex = googleDefaultRouteIndex,
        )
    }

    companion object {
        fun default(): RouteIntelligenceService = RouteIntelligenceService()
    }
}
