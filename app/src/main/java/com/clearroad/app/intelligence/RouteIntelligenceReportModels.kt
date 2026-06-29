package com.clearroad.app.intelligence

data class GoogleDirectionsStepInput(
    val distanceMeters: Int,
    val maneuver: String?,
)

data class RoadTypeBreakdown(
    val motorwayMeters: Double = 0.0,
    val trunkMeters: Double = 0.0,
    val primaryMeters: Double = 0.0,
    val secondaryMeters: Double = 0.0,
    val tertiaryMeters: Double = 0.0,
    val residentialMeters: Double = 0.0,
    val serviceMeters: Double = 0.0,
    val unclassifiedMeters: Double = 0.0,
    val unknownMeters: Double = 0.0,
) {
    val totalClassifiedMeters: Double =
        motorwayMeters +
            trunkMeters +
            primaryMeters +
            secondaryMeters +
            tertiaryMeters +
            residentialMeters +
            serviceMeters +
            unclassifiedMeters +
            unknownMeters
}

data class GoogleManeuverEvidence(
    val criticalManeuversCount: Int?,
    val turnsCount: Int?,
    val roundaboutsFromManeuvers: Int?,
    val rampOrExitCount: Int?,
    val restrictedRoadHintsCount: Int?,
)

data class RouteIntelligenceSignals(
    val trafficSignalsCount: Int?,
    val roundaboutsCount: Int?,
    val mainRoadRatio: Float?,
    val complexityScore: Float?,
    val criticalManeuversCount: Int?,
    val turnsCount: Int?,
    val roundaboutsFromManeuvers: Int?,
    val rampOrExitCount: Int?,
    val restrictedRoadHintsCount: Int?,
    val roadTypeBreakdown: RoadTypeBreakdown?,
    val osmSourceStatus: SourceStatus,
    val googleSourceStatus: SourceStatus,
)

data class RouteIntelligenceReport(
    val routeIndex: Int,
    val routeName: String,
    val isGoogleDefault: Boolean,
    val isMarshioSelected: Boolean,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val profile: RouteProfile,
    val signals: RouteIntelligenceSignals,
    val confidence: Float,
    val osmEvidence: RouteEvidence?,
    val googleEvidence: GoogleManeuverEvidence?,
)

data class RouteIntelligenceComparisonSummary(
    val fastestRouteIndex: Int?,
    val lowestComplexityRouteIndex: Int?,
    val mostMainRoadRouteIndex: Int?,
    val leastSignalsRouteIndex: Int?,
    val dataCompleteness: Float,
    val routeFactLines: List<String>,
)

data class RouteIntelligenceComparisonReport(
    val googleDefaultRouteIndex: Int,
    val marshioSelectedRouteIndex: Int,
    val reports: List<RouteIntelligenceReport>,
    val summary: RouteIntelligenceComparisonSummary,
)
