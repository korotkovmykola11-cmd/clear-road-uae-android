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

enum class RouteIntelligenceCacheOutcome {
    HIT,
    MISS,
    EXPIRED,
}

data class RouteIntelligenceCacheLookup(
    val outcome: RouteIntelligenceCacheOutcome,
    val profile: RouteProfile? = null,
)

class RouteIntelligenceCache(
    private val ttlMillis: Long = DEFAULT_TTL_MS,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private data class CacheEntry(
        val profile: RouteProfile,
        val expiresAtMillis: Long,
    )

    private val lock = Any()
    private val entries = mutableMapOf<String, CacheEntry>()

    fun lookup(polylineHash: String): RouteIntelligenceCacheLookup =
        synchronized(lock) {
            val entry = entries[polylineHash]
                ?: return RouteIntelligenceCacheLookup(RouteIntelligenceCacheOutcome.MISS)
            if (entry.expiresAtMillis <= nowMillis()) {
                entries.remove(polylineHash)
                return RouteIntelligenceCacheLookup(RouteIntelligenceCacheOutcome.EXPIRED)
            }
            RouteIntelligenceCacheLookup(
                outcome = RouteIntelligenceCacheOutcome.HIT,
                profile = entry.profile,
            )
        }

    fun put(
        polylineHash: String,
        profile: RouteProfile,
    ) {
        synchronized(lock) {
            entries[polylineHash] =
                CacheEntry(
                    profile = profile,
                    expiresAtMillis = nowMillis() + ttlMillis,
                )
        }
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
        }
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
        val lookup = cache.lookup(cacheKey)
        when (lookup.outcome) {
            RouteIntelligenceCacheOutcome.HIT -> {
                RouteIntelligenceDiag.logCache(
                    routeIndex = route.routeIndex,
                    event = RouteIntelligenceDiag.CacheEvent.CACHE_HIT,
                )
                return lookup.profile!!
            }
            RouteIntelligenceCacheOutcome.EXPIRED -> {
                RouteIntelligenceDiag.logCache(
                    routeIndex = route.routeIndex,
                    event = RouteIntelligenceDiag.CacheEvent.CACHE_EXPIRED,
                )
            }
            RouteIntelligenceCacheOutcome.MISS -> {
                RouteIntelligenceDiag.logCache(
                    routeIndex = route.routeIndex,
                    event = RouteIntelligenceDiag.CacheEvent.CACHE_MISS,
                )
            }
        }
        RouteIntelligenceDiag.logCache(
            routeIndex = route.routeIndex,
            event = RouteIntelligenceDiag.CacheEvent.NETWORK_FETCH,
        )
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
        private val defaultInstance: RouteIntelligenceService by lazy {
            RouteIntelligenceService()
        }

        fun default(): RouteIntelligenceService = defaultInstance
    }

    fun reportFromLoadedProfile(
        route: RawRouteForIntelligence,
        profile: RouteProfile,
        isGoogleDefault: Boolean,
        isMarshioSelected: Boolean,
    ): RouteIntelligenceReport {
        val googleFeatures = GoogleRouteFeatureReader.read(route)
        val profileWithGoogle =
            profile.copy(googleSourceStatus = googleFeatures.googleSourceStatus)
        val osmSource = osmSourceFromLoadedProfile(profile)
        return RouteIntelligenceReportBuilder.build(
            route = route,
            profile = profileWithGoogle,
            osmSource = osmSource,
            googleFeatures = googleFeatures,
            isGoogleDefault = isGoogleDefault,
            isMarshioSelected = isMarshioSelected,
        )
    }

    internal fun osmSourceFromLoadedProfile(profile: RouteProfile): SourceData? {
        if (
            profile.osmSourceStatus == SourceStatus.UNAVAILABLE &&
            profile.evidence == null &&
            profile.metadata == null &&
            profile.trafficSignalsCount == null &&
            profile.roundaboutsCount == null &&
            profile.mainRoadRatio == null
        ) {
            return null
        }
        return SourceData(
            provider = OsmOverpassSource.PROVIDER,
            status = profile.osmSourceStatus,
            trafficSignalsCount = profile.trafficSignalsCount,
            roundaboutsCount = profile.roundaboutsCount,
            mainRoadRatio = profile.mainRoadRatio,
            evidence = profile.evidence,
            metadata = profile.metadata,
            roadTypeBreakdown = profile.evidence?.mainRoad?.roadTypeBreakdown,
        )
    }

    fun comparisonReportFromLoadedRoutes(
        marshioRoute: RawRouteForIntelligence,
        marshioProfile: RouteProfile,
        alternativeRoute: RawRouteForIntelligence?,
        alternativeProfile: RouteProfile?,
        googleDefaultRouteIndex: Int,
        marshioSelectedRouteIndex: Int,
    ): RouteIntelligenceComparisonReport {
        val routeProfiles =
            buildList {
                add(marshioRoute to marshioProfile)
                if (alternativeRoute != null && alternativeProfile != null) {
                    add(alternativeRoute to alternativeProfile)
                }
            }.sortedBy { (route, _) -> route.routeIndex }
        val reports =
            routeProfiles.map { (route, profile) ->
                reportFromLoadedProfile(
                    route = route,
                    profile = profile,
                    isGoogleDefault = route.routeIndex == googleDefaultRouteIndex,
                    isMarshioSelected = route.routeIndex == marshioSelectedRouteIndex,
                )
            }
        return RouteIntelligenceReportBuilder.buildComparison(
            reports = reports,
            marshioSelectedRouteIndex = marshioSelectedRouteIndex,
            googleDefaultRouteIndex = googleDefaultRouteIndex,
        )
    }
}
