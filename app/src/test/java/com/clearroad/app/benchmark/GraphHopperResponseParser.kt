package com.clearroad.app.benchmark

import com.clearroad.app.decodeRoutePathPoints
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

/**
 * Stage 0B — offline parser for GraphHopper Cloud Routing API responses.
 *
 * Geometry format: encoded polyline string in [paths.points] when [paths.points_encoded] is true.
 * GraphHopper uses the same polyline algorithm as Google; decoded via [decodeRoutePathPoints].
 *
 * Consumed JSON fields:
 * - paths.distance
 * - paths.time
 * - paths.points
 * - paths.points_encoded
 * - paths.instructions.text
 * - paths.instructions.street_name
 * - message (error responses)
 */
object GraphHopperResponseParser {

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
            val errorMessage = root.optString("message").trim()
            val paths = root.optJSONArray("paths")

            if (paths == null || paths.length() == 0) {
                val reason =
                    if (errorMessage.isNotEmpty()) {
                        "GraphHopper error: $errorMessage"
                    } else {
                        "No paths in GraphHopper response (caseId=$caseId)"
                    }
                return ParseResult.Failure(reason)
            }

            val candidates =
                buildList {
                    for (index in 0 until paths.length()) {
                        val path = paths.optJSONObject(index) ?: continue
                        parsePath(path, caseId, requestId, index)?.let { add(it) }
                    }
                }

            if (candidates.isEmpty()) {
                return ParseResult.Failure(
                    "No valid route candidates parsed from GraphHopper response (caseId=$caseId)",
                )
            }

            ParseResult.Success(candidates)
        } catch (error: Exception) {
            ParseResult.Failure(
                "Failed to parse GraphHopper response (caseId=$caseId): ${error.message}",
            )
        }
    }

    private fun parsePath(
        path: JSONObject,
        caseId: String,
        requestId: String,
        routeIndex: Int,
    ): BenchmarkRouteCandidate? {
        val distanceMeters = path.optDouble("distance", 0.0).toInt()
        val durationSeconds = path.optLong("time", 0L).let { millis ->
            if (millis <= 0L) 0 else (millis / 1_000L).toInt()
        }
        val routePathPoints = decodeGeometry(path)

        if (routePathPoints.size < 2 || distanceMeters <= 0 || durationSeconds <= 0) {
            return null
        }

        val corridorScanText = buildCorridorScanText(path)
        val routeSummary = buildRouteSummary(path, routeIndex)

        return BenchmarkRouteCandidate(
            candidateId = "$caseId-GRAPHHOPPER-$routeIndex",
            caseId = caseId,
            provider = PROVIDER_ID,
            routeIndex = routeIndex,
            routeSummary = routeSummary,
            corridorScanText = corridorScanText,
            routePathPoints = routePathPoints,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAed = 0,
            sourceFixture = requestId,
        )
    }

    private fun decodeGeometry(path: JSONObject): List<LatLng> {
        val pointsEncoded = path.optBoolean("points_encoded", true)
        if (!pointsEncoded) {
            return decodeCoordinatePairs(path.opt("points"))
        }
        val encoded = path.optString("points")
        return decodeRoutePathPoints(encoded)
    }

    private fun decodeCoordinatePairs(rawPoints: Any?): List<LatLng> {
        if (rawPoints !is org.json.JSONObject) {
            return emptyList()
        }
        val coordinates = rawPoints.optJSONArray("coordinates") ?: return emptyList()
        return buildList {
            for (index in 0 until coordinates.length()) {
                val pair = coordinates.optJSONArray(index) ?: continue
                if (pair.length() < 2) continue
                val lng = pair.optDouble(0)
                val lat = pair.optDouble(1)
                add(LatLng(lat, lng))
            }
        }
    }

    private fun buildRouteSummary(path: JSONObject, routeIndex: Int): String {
        val instructions = path.optJSONArray("instructions")
        if (instructions != null && instructions.length() > 0) {
            val first = instructions.optJSONObject(0)
            val text = first?.optString("text").orEmpty().trim()
            if (text.isNotEmpty()) {
                return text
            }
        }
        return "GraphHopper route $routeIndex"
    }

    private fun buildCorridorScanText(path: JSONObject): String {
        val parts = linkedSetOf<String>()
        val instructions = path.optJSONArray("instructions") ?: return ""
        for (index in 0 until instructions.length()) {
            val instruction = instructions.optJSONObject(index) ?: continue
            instruction.optString("street_name").trim().takeIf { it.isNotEmpty() }?.let { parts.add(it) }
            instruction.optString("text").trim().takeIf { it.isNotEmpty() }?.let { parts.add(it) }
        }
        return parts.joinToString(" ")
    }

    private const val PROVIDER_ID = "GRAPHHOPPER"
}
