package com.clearroad.app.intelligence

import com.clearroad.app.ui.model.RouteIntelligenceComparisonRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRouteRowUiModel
import com.clearroad.app.ui.model.RouteIntelligenceUiModel

/** Loaded Route Intelligence session — profiles fetched once for UI and diagnostic report. */
data class RouteIntelligenceLoadResult(
    val uiModel: RouteIntelligenceUiModel,
    val marshioRoute: RawRouteForIntelligence,
    val marshioProfile: RouteProfile,
    val alternativeRoute: RawRouteForIntelligence?,
    val alternativeProfile: RouteProfile?,
    val googleDefaultRouteIndex: Int,
    val marshioSelectedRouteIndex: Int,
)

object RouteIntelligencePresentation {

    const val UNAVAILABLE_MESSAGE = "Route Intelligence unavailable."

    const val SELECTED_ROUTE_UNAVAILABLE_HEADLINE = "Selected route details unavailable."

    const val SELECTED_ROUTE_UNAVAILABLE_EXPLANATION = "Alternative route details are shown below."

    internal data class DriverRouteCopy(
        val headline: String,
        val explanation: String,
    )

    suspend fun load(
        request: RouteIntelligenceRequestUiModel,
        comparisonRequest: RouteIntelligenceComparisonRequestUiModel? = null,
        service: RouteIntelligenceService = RouteIntelligenceService.default(),
    ): RouteIntelligenceLoadResult {
        val marshioRaw = RouteIntelligenceAssembly.toRawRoute(request.marshioRoute)
        val marshioProfile = service.profileFor(marshioRaw)

        val alternativeRaw =
            request.alternativeRoute?.let { RouteIntelligenceAssembly.toRawRoute(it) }
        val alternativeProfile =
            alternativeRaw?.let { service.profileFor(it) }

        val presentationResult =
            RouteIntelligenceDiag.classifyPresentationResult(
                marshioStatus = marshioProfile.osmSourceStatus,
                alternativeStatus = alternativeProfile?.osmSourceStatus,
            )
        RouteIntelligenceDiag.logPresentation(
            marshioRouteIndex = request.marshioRoute.routeIndex,
            marshioStatus = marshioProfile.osmSourceStatus,
            alternativeRouteIndex = request.alternativeRoute?.routeIndex,
            alternativeStatus = alternativeProfile?.osmSourceStatus,
            result = presentationResult,
        )

        val googleDefaultRouteIndex = comparisonRequest?.googleDefaultRouteIndex ?: 0
        val marshioSelectedRouteIndex =
            comparisonRequest?.marshioSelectedRouteIndex ?: request.marshioRoute.routeIndex

        val uiModel =
            buildUiModel(
                request = request,
                marshioProfile = marshioProfile,
                alternativeProfile = alternativeProfile,
            )

        return RouteIntelligenceLoadResult(
            uiModel = uiModel,
            marshioRoute = marshioRaw,
            marshioProfile = marshioProfile,
            alternativeRoute = alternativeRaw,
            alternativeProfile = alternativeProfile,
            googleDefaultRouteIndex = googleDefaultRouteIndex,
            marshioSelectedRouteIndex = marshioSelectedRouteIndex,
        )
    }

    internal fun buildUiModel(
        request: RouteIntelligenceRequestUiModel,
        marshioProfile: RouteProfile,
        alternativeProfile: RouteProfile?,
    ): RouteIntelligenceUiModel {
        val selectedUsable = marshioProfile.osmSourceStatus == SourceStatus.OK
        val alternativeUsable = alternativeProfile?.osmSourceStatus == SourceStatus.OK

        return when {
            !selectedUsable && !alternativeUsable ->
                RouteIntelligenceUiModel(
                    loading = false,
                    available = false,
                    unavailableMessage = UNAVAILABLE_MESSAGE,
                )
            !selectedUsable && alternativeUsable ->
                RouteIntelligenceUiModel(
                    loading = false,
                    available = true,
                    summaryLine = SELECTED_ROUTE_UNAVAILABLE_HEADLINE,
                    explanationLine = SELECTED_ROUTE_UNAVAILABLE_EXPLANATION,
                    marshioRoute = null,
                    alternativeRoute =
                        request.alternativeRoute?.let { alternative ->
                            alternativeProfile?.let { profile ->
                                rowUiModel(
                                    label = comparisonRouteLabel(alternative.routeIndex),
                                    routeName = alternative.routeName,
                                    profile = profile,
                                )
                            }
                        },
                )
            else -> {
                val comparisonAlternative = alternativeProfile?.takeIf { alternativeUsable }
                val driverCopy =
                    driverRouteCopy(
                        marshio = marshioProfile,
                        alternative = comparisonAlternative,
                        alternativeRouteIndex = request.alternativeRoute?.routeIndex,
                    )
                RouteIntelligenceUiModel(
                    loading = false,
                    available = true,
                    summaryLine = driverCopy.headline,
                    explanationLine = driverCopy.explanation,
                    marshioRoute = rowUiModel("MARSHIO route", request.marshioRoute.routeName, marshioProfile),
                    alternativeRoute =
                        request.alternativeRoute?.let { alternative ->
                            alternativeProfile?.let { profile ->
                                rowUiModel(
                                    label = comparisonRouteLabel(alternative.routeIndex),
                                    routeName = alternative.routeName,
                                    profile = profile,
                                )
                            }
                        },
                )
            }
        }
    }

    fun comparisonReportFromLoad(
        loadResult: RouteIntelligenceLoadResult,
    ): RouteIntelligenceComparisonReport =
        RouteIntelligenceService.default().comparisonReportFromLoadedRoutes(
            marshioRoute = loadResult.marshioRoute,
            marshioProfile = loadResult.marshioProfile,
            alternativeRoute = loadResult.alternativeRoute,
            alternativeProfile = loadResult.alternativeProfile,
            googleDefaultRouteIndex = loadResult.googleDefaultRouteIndex,
            marshioSelectedRouteIndex = loadResult.marshioSelectedRouteIndex,
        )

    internal fun comparisonRouteLabel(routeIndex: Int): String =
        if (routeIndex == 0) "Google default" else "Alternative"

    internal fun comparisonTargetPhrase(routeIndex: Int): String =
        if (routeIndex == 0) {
            "Google's default route"
        } else {
            "the alternative route"
        }

    internal fun driverRouteCopy(
        marshio: RouteProfile,
        alternative: RouteProfile?,
        alternativeRouteIndex: Int? = null,
    ): DriverRouteCopy {
        val headline = driverHeadline(marshio)
        val explanation =
            driverExplanation(
                marshio = marshio,
                alternative = alternative,
                alternativeRouteIndex = alternativeRouteIndex,
            )
        return DriverRouteCopy(headline = headline, explanation = explanation)
    }

    internal fun driverHeadline(marshio: RouteProfile): String {
        val signals = marshio.trafficSignalsCount ?: 0
        val roundabouts = marshio.roundaboutsCount ?: 0
        val complexity = marshio.complexityScore
        val mainRoadRatio = marshio.mainRoadRatio

        if (mainRoadRatio != null && mainRoadRatio >= 0.65f && (complexity ?: 1f) < 0.4f) {
            return "Smooth highway drive."
        }
        if (
            (complexity != null && complexity >= 0.55f) ||
                signals >= 6 ||
                roundabouts >= 5
        ) {
            return "Busy city drive."
        }
        if (
            (complexity != null && complexity < 0.3f) &&
                signals <= 2 &&
                roundabouts <= 2
        ) {
            return "Easy city drive."
        }
        if (mainRoadRatio != null && mainRoadRatio >= 0.5f && (complexity ?: 1f) < 0.45f) {
            return "Calm route."
        }
        return "Steady city drive."
    }

    internal fun driverExplanation(
        marshio: RouteProfile,
        alternative: RouteProfile?,
        alternativeRouteIndex: Int? = null,
    ): String {
        alternative?.let { alt ->
            val comparisonTarget =
                alternativeRouteIndex?.let(::comparisonTargetPhrase) ?: return@let
            val marshioSignals = marshio.trafficSignalsCount
            val alternativeSignals = alt.trafficSignalsCount
            if (marshioSignals != null && alternativeSignals != null) {
                when {
                    marshioSignals >= alternativeSignals + 2 ->
                        return "More intersections than $comparisonTarget."
                    marshioSignals + 2 <= alternativeSignals ->
                        return "Fewer intersections than $comparisonTarget."
                }
            }
            val marshioComplexity = marshio.complexityScore
            val alternativeComplexity = alt.complexityScore
            if (
                marshioComplexity != null &&
                    alternativeComplexity != null &&
                    marshioComplexity >= alternativeComplexity + 0.15f
            ) {
                return "Expect a busier drive than $comparisonTarget."
            }
            if (
                marshioComplexity != null &&
                    alternativeComplexity != null &&
                    marshioComplexity + 0.15f <= alternativeComplexity
            ) {
                return "A calmer drive than $comparisonTarget."
            }
        }

        val roundabouts = marshio.roundaboutsCount ?: 0
        val signals = marshio.trafficSignalsCount ?: 0
        val mainRoadRatio = marshio.mainRoadRatio
        val complexity = marshio.complexityScore

        if (roundabouts >= 5) {
            return "Mostly city streets with many roundabouts."
        }
        if (mainRoadRatio != null && mainRoadRatio >= 0.65f) {
            return "Mostly highway driving with fewer decision points."
        }
        if (signals >= 6) {
            return "More intersections than usual along the way."
        }
        if (roundabouts >= 3) {
            return "Many roundabouts ahead on city streets."
        }
        if (mainRoadRatio != null && mainRoadRatio < 0.35f) {
            return "Mostly local roads through the city."
        }
        if (complexity != null && complexity < 0.3f && signals <= 2 && roundabouts <= 2) {
            return "Simple route with few decision points."
        }
        if (mainRoadRatio != null && mainRoadRatio >= 0.35f && mainRoadRatio < 0.65f) {
            return "Mix of main roads and city streets."
        }
        return "Expect a typical urban mix of streets and junctions."
    }

    fun rowUiModel(
        label: String,
        routeName: String,
        profile: RouteProfile,
    ): RouteIntelligenceRouteRowUiModel? {
        if (profile.osmSourceStatus != SourceStatus.OK) return null
        return RouteIntelligenceRouteRowUiModel(
            label = label,
            routeName = routeName,
            trafficSignalsLine = trafficSignalsLine(profile.trafficSignalsCount),
            roundaboutsLine = roundaboutsLine(profile.roundaboutsCount),
            mainRoadLine = mainRoadLine(profile.mainRoadRatio),
            complexityLine = drivingWorkloadLine(profile.complexityScore),
        )
    }

    fun trafficSignalsLine(count: Int?): String =
        when {
            count == null -> "Traffic lights along the route unavailable"
            count == 0 -> "No traffic lights along the route"
            count <= 2 -> "Few traffic lights"
            count <= 5 -> "Several traffic lights"
            else -> "Many traffic lights"
        }

    fun roundaboutsLine(count: Int?): String =
        when {
            count == null -> "Roundabouts along the route unavailable"
            count == 0 -> "No roundabouts"
            count <= 2 -> "Few roundabouts"
            count <= 5 -> "Several roundabouts"
            else -> "Many roundabouts"
        }

    fun mainRoadLine(ratio: Float?): String =
        when {
            ratio == null -> "Road mix unavailable"
            ratio >= 0.65f -> "Mostly highway driving"
            ratio >= 0.35f -> "Mix of main and local streets"
            else -> "Mostly local roads"
        }

    fun drivingWorkloadLine(score: Float?): String =
        when {
            score == null -> "Driving workload unavailable"
            score < 0.25f -> "Light driving workload"
            score < 0.55f -> "Moderate driving workload"
            else -> "Higher driving workload"
        }
}
