package com.clearroad.app.intelligence

import kotlin.math.roundToInt

object RouteIntelligenceReportBuilder {

    fun build(
        route: RawRouteForIntelligence,
        profile: RouteProfile,
        osmSource: SourceData?,
        googleFeatures: GoogleRouteFeatures,
        isGoogleDefault: Boolean,
        isMarshioSelected: Boolean,
    ): RouteIntelligenceReport {
        val signals = buildSignals(profile, osmSource, googleFeatures)
        val confidence = computeConfidence(profile, osmSource, googleFeatures)

        return RouteIntelligenceReport(
            routeIndex = route.routeIndex,
            routeName = route.routeName,
            isGoogleDefault = isGoogleDefault,
            isMarshioSelected = isMarshioSelected,
            durationSeconds = route.durationSeconds,
            distanceMeters = route.distanceMeters,
            profile = profile,
            signals = signals,
            confidence = confidence,
            osmEvidence = profile.evidence,
            googleEvidence = googleFeatures.maneuvers,
        )
    }

    fun buildComparison(
        reports: List<RouteIntelligenceReport>,
        marshioSelectedRouteIndex: Int,
        googleDefaultRouteIndex: Int = 0,
    ): RouteIntelligenceComparisonReport {
        val summary = buildSummary(reports)
        return RouteIntelligenceComparisonReport(
            googleDefaultRouteIndex = googleDefaultRouteIndex,
            marshioSelectedRouteIndex = marshioSelectedRouteIndex,
            reports = reports,
            summary = summary,
        )
    }

    internal fun buildSignals(
        profile: RouteProfile,
        osmSource: SourceData?,
        googleFeatures: GoogleRouteFeatures,
    ): RouteIntelligenceSignals {
        val googleManeuvers = googleFeatures.maneuvers
        return RouteIntelligenceSignals(
            trafficSignalsCount = profile.trafficSignalsCount,
            roundaboutsCount = profile.roundaboutsCount,
            mainRoadRatio = profile.mainRoadRatio,
            complexityScore = profile.complexityScore,
            criticalManeuversCount = googleManeuvers?.criticalManeuversCount,
            turnsCount = googleManeuvers?.turnsCount,
            roundaboutsFromManeuvers = googleManeuvers?.roundaboutsFromManeuvers,
            rampOrExitCount = googleManeuvers?.rampOrExitCount,
            restrictedRoadHintsCount = googleManeuvers?.restrictedRoadHintsCount,
            roadTypeBreakdown = osmSource?.roadTypeBreakdown,
            osmSourceStatus = profile.osmSourceStatus,
            googleSourceStatus = profile.googleSourceStatus,
        )
    }

    internal fun buildSummary(reports: List<RouteIntelligenceReport>): RouteIntelligenceComparisonSummary {
        if (reports.isEmpty()) {
            return RouteIntelligenceComparisonSummary(
                fastestRouteIndex = null,
                lowestComplexityRouteIndex = null,
                mostMainRoadRouteIndex = null,
                leastSignalsRouteIndex = null,
                dataCompleteness = 0f,
                routeFactLines = emptyList(),
            )
        }

        val fastestRouteIndex =
            reports.minWith(
                compareBy<RouteIntelligenceReport> { it.durationSeconds }.thenBy { it.routeIndex },
            ).routeIndex

        val lowestComplexityRouteIndex =
            reports
                .filter { it.signals.complexityScore != null }
                .minWithOrNull(
                    compareBy<RouteIntelligenceReport> { it.signals.complexityScore!! }
                        .thenBy { it.routeIndex },
                )
                ?.routeIndex

        val mostMainRoadRouteIndex =
            reports
                .filter { it.signals.mainRoadRatio != null }
                .maxWithOrNull(
                    compareBy<RouteIntelligenceReport> { it.signals.mainRoadRatio!! }
                        .thenBy { it.routeIndex },
                )
                ?.routeIndex

        val leastSignalsRouteIndex =
            reports
                .filter { it.signals.trafficSignalsCount != null }
                .minWithOrNull(
                    compareBy<RouteIntelligenceReport> { it.signals.trafficSignalsCount!! }
                        .thenBy { it.routeIndex },
                )
                ?.routeIndex

        val usableCount =
            reports.count { report ->
                isSourceUsable(report.profile.osmSourceStatus) ||
                    isSourceUsable(report.profile.googleSourceStatus)
            }
        val dataCompleteness = usableCount.toFloat() / reports.size.toFloat()

        val fastestDuration =
            reports.firstOrNull { it.routeIndex == fastestRouteIndex }?.durationSeconds ?: 0
        val routeFactLines =
            reports.map { report ->
                formatRouteFactLine(
                    report = report,
                    fastestDurationSeconds = fastestDuration,
                    isFastest = report.routeIndex == fastestRouteIndex,
                )
            }

        return RouteIntelligenceComparisonSummary(
            fastestRouteIndex = fastestRouteIndex,
            lowestComplexityRouteIndex = lowestComplexityRouteIndex,
            mostMainRoadRouteIndex = mostMainRoadRouteIndex,
            leastSignalsRouteIndex = leastSignalsRouteIndex,
            dataCompleteness = dataCompleteness,
            routeFactLines = routeFactLines,
        )
    }

    internal fun isSourceUsable(status: SourceStatus): Boolean =
        status == SourceStatus.OK || status == SourceStatus.PARTIAL

    internal fun formatRouteFactLine(
        report: RouteIntelligenceReport,
        fastestDurationSeconds: Int,
        isFastest: Boolean,
    ): String {
        val etaPart =
            if (isFastest) {
                "fastest"
            } else {
                val deltaMin =
                    ((report.durationSeconds - fastestDurationSeconds).coerceAtLeast(0) + 59) / 60
                "+$deltaMin min"
            }
        val signals = report.signals.trafficSignalsCount?.toString() ?: "?"
        val roundabouts =
            when (val count = report.signals.roundaboutsCount) {
                null -> "?"
                1 -> "1 roundabout"
                else -> "$count roundabouts"
            }
        val mainRoadPct =
            report.signals.mainRoadRatio?.let { ratio ->
                "${(ratio * 100f).roundToInt()}% main roads"
            } ?: "main roads ?"
        return "Route ${report.routeIndex}: $etaPart, $signals signals, $roundabouts, $mainRoadPct"
    }

    private fun computeConfidence(
        profile: RouteProfile,
        osmSource: SourceData?,
        googleFeatures: GoogleRouteFeatures,
    ): Float {
        val confidences =
            listOfNotNull(
                osmSource?.metadata?.confidence?.takeIf { profile.osmSourceStatus == SourceStatus.OK },
                googleFeatures.metadata?.confidence?.takeIf {
                    profile.googleSourceStatus == SourceStatus.OK ||
                        profile.googleSourceStatus == SourceStatus.PARTIAL
                },
            )
        if (confidences.isEmpty()) return 0f
        return confidences.average().toFloat().coerceIn(0f, 1f)
    }
}
