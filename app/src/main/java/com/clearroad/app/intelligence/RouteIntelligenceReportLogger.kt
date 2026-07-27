package com.clearroad.app.intelligence

import android.util.Log
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object RouteIntelligenceReportLogger {

    const val TAG = "MARSHIO_INTELLIGENCE_REPORT"

    fun log(report: RouteIntelligenceComparisonReport) {
        val lines = buildList {
            report.reports.forEach { routeReport ->
                add(formatRouteLine(routeReport))
            }
            add("summary fastest=${report.summary.fastestRouteIndex}")
            add("summary leastSignals=${report.summary.leastSignalsRouteIndex}")
            add("summary lowestComplexity=${report.summary.lowestComplexityRouteIndex}")
            add("summary mostMainRoad=${report.summary.mostMainRoadRouteIndex}")
            add("summary dataCompleteness=${formatFloat(report.summary.dataCompleteness)}")
            report.summary.routeFactLines.forEach { fact -> add("fact $fact") }
        }
        Log.d(TAG, lines.joinToString("\n"))
    }

    internal fun formatRouteLine(report: RouteIntelligenceReport): String {
        val etaMin = (report.durationSeconds + 59) / 60
        return buildString {
            append("route=${report.routeIndex}")
            append(" googleDefault=${report.isGoogleDefault}")
            append(" marshio=${report.isMarshioSelected}")
            append(" eta=${etaMin}min")
            append(" signals=${report.signals.trafficSignalsCount ?: "?"}")
            append(" roundabouts=${report.signals.roundaboutsCount ?: "?"}")
            append(" mainRoadRatio=${formatFloat(report.signals.mainRoadRatio)}")
            append(" complexity=${formatFloat(report.signals.complexityScore)}")
            append(" maneuvers=${report.signals.criticalManeuversCount ?: "?"}")
        }
    }

    private fun formatFloat(value: Float?): String =
        when (value) {
            null -> "?"
            else -> String.format(Locale.US, "%.2f", value)
        }
}

object RouteIntelligenceReportJson {

    fun toJson(report: RouteIntelligenceComparisonReport): String =
        JSONObject()
            .put("googleDefaultRouteIndex", report.googleDefaultRouteIndex)
            .put("marshioSelectedRouteIndex", report.marshioSelectedRouteIndex)
            .put("reports", JSONArray(report.reports.map(::reportJson)))
            .put("summary", summaryJson(report.summary))
            .toString(2)

    private fun reportJson(report: RouteIntelligenceReport): JSONObject =
        JSONObject()
            .put("routeIndex", report.routeIndex)
            .put("routeName", report.routeName)
            .put("isGoogleDefault", report.isGoogleDefault)
            .put("isMarshioSelected", report.isMarshioSelected)
            .put("durationSeconds", report.durationSeconds)
            .put("distanceMeters", report.distanceMeters)
            .put("confidence", report.confidence.toDouble())
            .put(
                "profile",
                JSONObject()
                    .put("trafficSignalsCount", report.profile.trafficSignalsCount)
                    .put("roundaboutsCount", report.profile.roundaboutsCount)
                    .put("mainRoadRatio", report.profile.mainRoadRatio?.toDouble())
                    .put("complexityScore", report.profile.complexityScore?.toDouble())
                    .put("osmSourceStatus", report.profile.osmSourceStatus.name)
                    .put("googleSourceStatus", report.profile.googleSourceStatus.name),
            )
            .put("signals", signalsJson(report.signals))

    private fun signalsJson(signals: RouteIntelligenceSignals): JSONObject =
        JSONObject()
            .put("trafficSignalsCount", signals.trafficSignalsCount)
            .put("roundaboutsCount", signals.roundaboutsCount)
            .put("mainRoadRatio", signals.mainRoadRatio?.toDouble())
            .put("complexityScore", signals.complexityScore?.toDouble())
            .put("criticalManeuversCount", signals.criticalManeuversCount)
            .put("turnsCount", signals.turnsCount)
            .put("roundaboutsFromManeuvers", signals.roundaboutsFromManeuvers)
            .put("rampOrExitCount", signals.rampOrExitCount)
            .put("restrictedRoadHintsCount", signals.restrictedRoadHintsCount)
            .put("roadTypeBreakdown", signals.roadTypeBreakdown?.let(::roadTypeBreakdownJson))
            .put("osmSourceStatus", signals.osmSourceStatus.name)
            .put("googleSourceStatus", signals.googleSourceStatus.name)

    private fun roadTypeBreakdownJson(breakdown: RoadTypeBreakdown): JSONObject =
        JSONObject()
            .put("motorwayMeters", breakdown.motorwayMeters)
            .put("trunkMeters", breakdown.trunkMeters)
            .put("primaryMeters", breakdown.primaryMeters)
            .put("secondaryMeters", breakdown.secondaryMeters)
            .put("tertiaryMeters", breakdown.tertiaryMeters)
            .put("residentialMeters", breakdown.residentialMeters)
            .put("serviceMeters", breakdown.serviceMeters)
            .put("unclassifiedMeters", breakdown.unclassifiedMeters)
            .put("unknownMeters", breakdown.unknownMeters)

    private fun summaryJson(summary: RouteIntelligenceComparisonSummary): JSONObject =
        JSONObject()
            .put("fastestRouteIndex", summary.fastestRouteIndex)
            .put("lowestComplexityRouteIndex", summary.lowestComplexityRouteIndex)
            .put("mostMainRoadRouteIndex", summary.mostMainRoadRouteIndex)
            .put("leastSignalsRouteIndex", summary.leastSignalsRouteIndex)
            .put("dataCompleteness", summary.dataCompleteness.toDouble())
            .put("routeFactLines", JSONArray(summary.routeFactLines))
}
