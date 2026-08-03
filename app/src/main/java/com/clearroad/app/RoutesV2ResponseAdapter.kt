package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps Google Routes API v2 `computeRoutes` JSON into [RealRouteDebugData].
 *
 * Stage B adapter — does not replace Legacy [extractRouteLegsDebugData]; prod fetch switch is Stage C.
 */
object RoutesV2ResponseAdapter {

    /** Field mask for Stage B adapter + traffic/scoring parity (includes maneuver + static duration). */
    const val ADAPTER_FIELD_MASK =
        "routes.duration," +
            "routes.staticDuration," +
            "routes.distanceMeters," +
            "routes.polyline.encodedPolyline," +
            "routes.description," +
            "routes.routeLabels," +
            "routes.travelAdvisory.tollInfo," +
            "routes.travelAdvisory.speedReadingIntervals," +
            "routes.legs.steps.distanceMeters," +
            "routes.legs.steps.navigationInstruction.maneuver," +
            "routes.legs.steps.navigationInstruction.instructions"

    internal fun extractRouteLegsDebugData(responseJson: String): List<RealRouteDebugData> {
        if (responseJson.isBlank()) return emptyList()
        return try {
            val routes = JSONObject(responseJson).optJSONArray("routes") ?: return emptyList()
            buildList {
                for (index in 0 until routes.length()) {
                    routes.optJSONObject(index)?.let { route ->
                        routeObjectToDebugData(route)?.let(::add)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun routeObjectToDebugData(route: JSONObject): RealRouteDebugData? {
        val encodedPolyline =
            route.optJSONObject("polyline")?.optString("encodedPolyline").orEmpty()
        val routePathPoints = decodeRoutePathPoints(encodedPolyline)
        if (routePathPoints.size < 2) return null

        val distanceMeters = route.optInt("distanceMeters", 0)
        if (distanceMeters <= 0) return null

        val trafficDurationSeconds = parseDurationSeconds(route.optString("duration"))
        if (trafficDurationSeconds <= 0) return null

        val staticDurationSeconds =
            parseDurationSeconds(route.optString("staticDuration")).takeIf { it > 0 }
                ?: trafficDurationSeconds

        val googleSteps = extractStepRecordsFromRoutesV2Route(route)
        val routeSummary = route.optString("description").trim()
        val corridorScanText = buildCorridorScanText(route, routeSummary)
        val tollAed = parseTollAed(route.optJSONObject("travelAdvisory")?.optJSONObject("tollInfo"))
        val routeJson = route.toString()
        val (trafficSpeedIntervals, stepTrafficRecords) =
            TrafficSpeedParsing.parseRouteTraffic(routeJson, routePathPoints.size)
        val criticalManeuversCount = RoutesV2ManeuverPolicy.countPolicyBStress(googleSteps)

        return RealRouteDebugData(
            distanceText = formatDistanceText(distanceMeters),
            durationText = formatDurationText(trafficDurationSeconds),
            distanceMeters = distanceMeters,
            durationSeconds = trafficDurationSeconds,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            routeSummary = routeSummary,
            corridorScanText = corridorScanText,
            routePathPoints = routePathPoints,
            baseDurationText = formatDurationText(staticDurationSeconds),
            baseDurationSeconds = staticDurationSeconds,
            durationInTrafficText = formatDurationText(trafficDurationSeconds),
            durationInTrafficSeconds = trafficDurationSeconds,
            criticalManeuversCount = criticalManeuversCount,
            googleSteps = googleSteps,
            trafficSpeedIntervals = trafficSpeedIntervals,
            stepTrafficRecords = stepTrafficRecords,
        )
    }

    /**
     * Routes v2 steps expose maneuver under `navigationInstruction.maneuver`
     * (enum strings like MERGE, RAMP_LEFT). [DriverStressAudit.matchedKeyword] normalizes `_` → `-`.
     */
    internal fun extractStepRecordsFromRoutesV2Route(route: JSONObject): List<DirectionsStepRecord> {
        val results = mutableListOf<DirectionsStepRecord>()
        val legs = route.optJSONArray("legs") ?: return emptyList()
        for (legIndex in 0 until legs.length()) {
            val leg = legs.optJSONObject(legIndex) ?: continue
            val steps = leg.optJSONArray("steps") ?: continue
            for (stepIndex in 0 until steps.length()) {
                val step = steps.optJSONObject(stepIndex) ?: continue
                val navigation = step.optJSONObject("navigationInstruction")
                results +=
                    DirectionsStepRecord(
                        distanceMeters = step.optInt("distanceMeters", 0),
                        maneuver = navigation?.optString("maneuver")?.takeIf { it.isNotBlank() },
                        htmlInstructions =
                            navigation?.optString("instructions")?.takeIf { it.isNotBlank() },
                        startLocation = null,
                    )
            }
        }
        return results
    }

    internal fun buildCorridorScanText(
        route: JSONObject,
        routeSummary: String,
    ): String {
        val stepInstructions = buildString {
            val legs = route.optJSONArray("legs") ?: return@buildString
            for (legIndex in 0 until legs.length()) {
                val leg = legs.optJSONObject(legIndex) ?: continue
                val steps = leg.optJSONArray("steps") ?: continue
                for (stepIndex in 0 until steps.length()) {
                    val step = steps.optJSONObject(stepIndex) ?: continue
                    step.optJSONObject("navigationInstruction")
                        ?.optString("instructions")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { append(it).append(' ') }
                }
            }
        }.trim()
        return RoutesV2CorridorScanSupport.enrichCorridorScanText(
            routeSummary = routeSummary,
            stepInstructions = stepInstructions,
        )
    }

    internal fun parseTollAed(tollInfo: JSONObject?): Int {
        if (tollInfo == null) return 0
        val prices = tollInfo.optJSONArray("estimatedPrice") ?: return 0
        var totalAed = 0.0
        for (index in 0 until prices.length()) {
            val money = prices.optJSONObject(index) ?: continue
            if (money.optString("currencyCode") != "AED") continue
            val units = money.optString("units").toLongOrNull() ?: 0L
            val nanos = money.optInt("nanos", 0)
            totalAed += units + nanos / 1_000_000_000.0
        }
        return totalAed.toInt().coerceAtLeast(0)
    }

    internal fun parseDurationSeconds(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        return raw.removeSuffix("s").toDoubleOrNull()?.toInt() ?: 0
    }

    internal fun formatDurationText(seconds: Int): String {
        val minutes = (seconds.coerceAtLeast(0) + 59) / 60
        return if (minutes == 1) "1 min" else "$minutes mins"
    }

    internal fun formatDistanceText(distanceMeters: Int): String {
        val km = distanceMeters.coerceAtLeast(0) / 1000.0
        return if (km >= 10) {
            "${km.toInt()} km"
        } else {
            String.format("%.1f km", km)
        }
    }

    internal fun buildComputeRoutesRequestBody(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
        computeAlternativeRoutes: Boolean = true,
    ): String =
        JSONObject()
            .apply {
                put("origin", latLngWaypoint(originLat, originLng))
                put("destination", latLngWaypoint(destinationLat, destinationLng))
                put("travelMode", "DRIVE")
                put("routingPreference", "TRAFFIC_AWARE")
                put("polylineQuality", "HIGH_QUALITY")
                put("computeAlternativeRoutes", computeAlternativeRoutes)
                put(
                    "extraComputations",
                    JSONArray(listOf("TOLLS", "TRAFFIC_ON_POLYLINE")),
                )
            }.toString()

    internal fun buildComputeRoutesHeaders(apiKey: String): Map<String, String> =
        mapOf(
            "Content-Type" to "application/json",
            "X-Goog-Api-Key" to apiKey,
            "X-Goog-FieldMask" to ADAPTER_FIELD_MASK,
        )

    private fun latLngWaypoint(
        latitude: Double,
        longitude: Double,
    ): JSONObject =
        JSONObject().apply {
            put(
                "location",
                JSONObject().put(
                    "latLng",
                    JSONObject()
                        .put("latitude", latitude)
                        .put("longitude", longitude),
                ),
            )
        }
}
