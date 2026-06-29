package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import java.time.Instant

data class RawRouteForIntelligence(
    val routeIndex: Int,
    val routeName: String,
    val routePathPoints: List<LatLng>,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val googleSteps: List<GoogleDirectionsStepInput> = emptyList(),
    val criticalManeuversCount: Int? = null,
)

enum class SourceStatus {
    OK,
    PARTIAL,
    UNAVAILABLE,
    NOT_APPLICABLE,
}

data class SourceMetadata(
    val provider: String,
    val fetchedAt: Instant,
    val confidence: Float,
)

data class TrafficLightEvidence(
    val count: Int,
    val osmNodeIds: List<Long>,
    val nearRoutePositions: List<LatLng>,
)

data class RoundaboutEvidence(
    val count: Int,
    val osmWayIds: List<Long>,
    val nearRoutePositions: List<LatLng>,
)

data class MainRoadEvidence(
    val mainRoadRatio: Float,
    val mainRoadMeters: Double,
    val classifiedMeters: Double,
    val roadTypeBreakdown: RoadTypeBreakdown? = null,
)

data class RouteEvidence(
    val trafficLights: TrafficLightEvidence?,
    val roundabouts: RoundaboutEvidence?,
    val mainRoad: MainRoadEvidence?,
)

data class RouteProfile(
    val trafficSignalsCount: Int?,
    val roundaboutsCount: Int?,
    val mainRoadRatio: Float?,
    val complexityScore: Float?,
    val osmSourceStatus: SourceStatus,
    val googleSourceStatus: SourceStatus = SourceStatus.UNAVAILABLE,
    val evidence: RouteEvidence?,
    val metadata: SourceMetadata?,
)

data class SourceData(
    val provider: String,
    val status: SourceStatus,
    val trafficSignalsCount: Int?,
    val roundaboutsCount: Int?,
    val mainRoadRatio: Float?,
    val evidence: RouteEvidence?,
    val metadata: SourceMetadata?,
    val roadTypeBreakdown: RoadTypeBreakdown? = null,
    val errorMessage: String? = null,
)

data class EnrichedRoute(
    val raw: RawRouteForIntelligence,
    val sources: List<SourceData>,
)

interface RouteDataSource {
    suspend fun enrich(route: RawRouteForIntelligence): SourceData
}
