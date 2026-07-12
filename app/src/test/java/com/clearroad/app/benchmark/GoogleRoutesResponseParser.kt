package com.clearroad.app.benchmark

import com.clearroad.app.decodeRoutePathPoints
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

/**
 * Stage 0B — offline parser for Google Routes API v2 computeRoutes responses.
 *
 * Consumed JSON fields (field mask derived from this list):
 * - routes.duration
 * - routes.distanceMeters
 * - routes.polyline.encodedPolyline
 * - routes.description
 * - routes.routeLabels
 * - routes.travelAdvisory.tollInfo
 * - routes.legs.steps.navigationInstruction.instructions
 */
object GoogleRoutesResponseParser {

    const val PARSED_FIELD_MASK =
        "routes.duration," +
            "routes.distanceMeters," +
            "routes.polyline.encodedPolyline," +
            "routes.description," +
            "routes.routeLabels," +
            "routes.travelAdvisory.tollInfo," +
            "routes.legs.steps.navigationInstruction.instructions"

    sealed class ParseResult {
        data class Success(val candidates: List<BenchmarkRouteCandidate>) : ParseResult() {
            override fun toString(): String =
                "ParseResult.Success(candidateCount=${candidates.size})"
        }

        data class Failure(val message: String) : ParseResult() {
            override fun toString(): String =
                "ParseResult.Failure(message=${sanitizeMessage(message)})"
        }
    }

    fun parse(
        responseJson: String,
        caseId: String,
        requestId: String,
    ): ParseResult {
        if (responseJson.isBlank()) {
            return ParseResult.Failure("Response JSON is blank (caseId=$caseId)")
        }

        return try {
            val root = JSONObject(responseJson)
            val routes = root.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return ParseResult.Failure("No routes in Google Routes response (caseId=$caseId)")
            }

            val candidates =
                buildList {
                    for (index in 0 until routes.length()) {
                        val route = routes.optJSONObject(index) ?: continue
                        parseRoute(route, caseId, requestId, index)?.let { add(it) }
                    }
                }

            if (candidates.isEmpty()) {
                return ParseResult.Failure(
                    "No valid route candidates parsed from Google Routes response (caseId=$caseId)",
                )
            }

            ParseResult.Success(candidates)
        } catch (error: Exception) {
            ParseResult.Failure(
                "Failed to parse Google Routes response (caseId=$caseId): ${error.message}",
            )
        }
    }

    private fun parseRoute(
        route: JSONObject,
        caseId: String,
        requestId: String,
        routeIndex: Int,
    ): BenchmarkRouteCandidate? {
        val encodedPolyline =
            route.optJSONObject("polyline")?.optString("encodedPolyline").orEmpty()
        val routePathPoints = decodeRoutePathPoints(encodedPolyline)
        val distanceMeters = route.optInt("distanceMeters", 0)
        val durationSeconds = parseDurationSeconds(route.optString("duration"))

        if (routePathPoints.size < 2 || distanceMeters <= 0 || durationSeconds <= 0) {
            return null
        }

        val routeSummary = buildRouteSummary(route)
        val corridorScanText = buildCorridorScanText(route)
        val tollAed = parseTollAed(route.optJSONObject("travelAdvisory")?.optJSONObject("tollInfo"))

        return BenchmarkRouteCandidate(
            candidateId = "$caseId-GOOGLE-$routeIndex",
            caseId = caseId,
            provider = PROVIDER_ID,
            routeIndex = routeIndex,
            routeSummary = routeSummary,
            corridorScanText = corridorScanText,
            routePathPoints = routePathPoints,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAed = tollAed,
            sourceFixture = requestId,
        )
    }

    private fun buildRouteSummary(route: JSONObject): String {
        val description = route.optString("description").trim()
        if (description.isNotEmpty()) {
            return description
        }
        val labels = route.optJSONArray("routeLabels")
        if (labels != null && labels.length() > 0) {
            return buildString {
                for (index in 0 until labels.length()) {
                    val label = labels.optString(index).trim()
                    if (label.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(label)
                    }
                }
            }
        }
        return "Google route"
    }

    private fun buildCorridorScanText(route: JSONObject): String {
        val parts = linkedSetOf<String>()
        route.optString("description").trim().takeIf { it.isNotEmpty() }?.let { parts.add(it) }

        val legs = route.optJSONArray("legs") ?: return parts.joinToString(" ")
        for (legIndex in 0 until legs.length()) {
            val leg = legs.optJSONObject(legIndex) ?: continue
            val steps = leg.optJSONArray("steps") ?: continue
            for (stepIndex in 0 until steps.length()) {
                val step = steps.optJSONObject(stepIndex) ?: continue
                step.optJSONObject("navigationInstruction")
                    ?.optString("instructions")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { parts.add(it) }
            }
        }
        return parts.joinToString(" ")
    }

    private fun parseTollAed(tollInfo: JSONObject?): Int {
        if (tollInfo == null) {
            return 0
        }
        val prices = tollInfo.optJSONArray("estimatedPrice") ?: return 0
        var totalAed = 0.0
        for (index in 0 until prices.length()) {
            val money = prices.optJSONObject(index) ?: continue
            val currency = money.optString("currencyCode")
            if (currency != "AED") {
                continue
            }
            val units = money.optString("units").toLongOrNull() ?: 0L
            val nanos = money.optInt("nanos", 0)
            totalAed += units + nanos / 1_000_000_000.0
        }
        return totalAed.toInt().coerceAtLeast(0)
    }

    private fun parseDurationSeconds(raw: String?): Int {
        if (raw.isNullOrBlank()) {
            return 0
        }
        return raw.removeSuffix("s").toDoubleOrNull()?.toInt() ?: 0
    }

    private const val PROVIDER_ID = "GOOGLE"
}
