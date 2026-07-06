package com.clearroad.app

import com.google.maps.android.PolyUtil

internal object TrafficSpeedParsing {
    fun parseRouteTraffic(
        routeJson: String,
        pathPointCount: Int,
    ): Pair<List<RouteSpeedInterval>, List<DirectionsStepTrafficRecord>> {
        val intervals = parseSpeedReadingIntervals(routeJson, pathPointCount)
        if (intervals.isNotEmpty()) {
            return intervals to emptyList()
        }
        return emptyList<RouteSpeedInterval>() to extractStepTrafficRecordsFromRouteJson(routeJson)
    }

    /** Google Routes API travelAdvisory.speedReadingIntervals when present in JSON. */
    internal fun parseSpeedReadingIntervals(
        routeJson: String,
        pathPointCount: Int,
    ): List<RouteSpeedInterval> {
        if (pathPointCount < 2) return emptyList()
        val key = "\"speedReadingIntervals\""
        val idx = routeJson.indexOf(key)
        if (idx == -1) return emptyList()
        val bracket = routeJson.indexOf('[', idx + key.length)
        if (bracket == -1) return emptyList()
        val close = findMatchingBracket(routeJson, bracket) ?: return emptyList()

        val results = mutableListOf<RouteSpeedInterval>()
        var i = bracket + 1
        while (i < close) {
            while (i < close && (routeJson[i].isWhitespace() || routeJson[i] == ',')) i++
            if (i >= close || routeJson[i] != '{') break
            val objStart = i
            val objEnd = findMatchingClosingBrace(routeJson, objStart) ?: break
            val obj = routeJson.substring(objStart, objEnd + 1)
            val start = readIntField(obj, "startPolylinePointIndex")
            val end = readIntField(obj, "endPolylinePointIndex")
            val speed = readStringField(obj, "speed")
            if (start != null && end != null && speed != null) {
                results +=
                    RouteSpeedInterval(
                        startPointIndex = start,
                        endPointIndex = end,
                        speedCategory = mapGoogleSpeed(speed),
                    )
            }
            i = objEnd + 1
        }
        return results
    }

    internal fun extractStepTrafficRecordsFromRouteJson(
        routeJson: String,
    ): List<DirectionsStepTrafficRecord> {
        val legsIdx = routeJson.indexOf("\"legs\"")
        if (legsIdx == -1) return emptyList()
        val legsBracket = routeJson.indexOf('[', legsIdx)
        if (legsBracket == -1) return emptyList()
        val firstLegBrace = routeJson.indexOf('{', legsBracket)
        if (firstLegBrace == -1) return emptyList()
        val stepsIdx = routeJson.indexOf("\"steps\"", firstLegBrace)
        if (stepsIdx == -1) return emptyList()
        val stepsBracket = routeJson.indexOf('[', stepsIdx)
        if (stepsBracket == -1) return emptyList()

        val results = mutableListOf<DirectionsStepTrafficRecord>()
        var i = stepsBracket + 1
        while (i < routeJson.length) {
            while (i < routeJson.length && (routeJson[i].isWhitespace() || routeJson[i] == ',')) i++
            if (i >= routeJson.length) break
            if (routeJson[i] == ']') break
            if (routeJson[i] != '{') {
                i++
                continue
            }
            val stepStart = i
            val stepEnd = findMatchingClosingBrace(routeJson, stepStart) ?: break
            val stepJson = routeJson.substring(stepStart, stepEnd + 1)
            val encoded =
                extractNestedPolylinePoints(stepJson) ?: run {
                    i = stepEnd + 1
                    continue
                }
            val points = PolyUtil.decode(encoded)
            if (points.size < 2) {
                i = stepEnd + 1
                continue
            }
            val baseDuration = readDurationSeconds(stepJson, "duration") ?: 0
            val trafficDuration = readDurationSeconds(stepJson, "duration_in_traffic") ?: baseDuration
            results +=
                DirectionsStepTrafficRecord(
                    points = points,
                    speedCategory = speedCategoryFromDurations(baseDuration, trafficDuration),
                )
            i = stepEnd + 1
        }
        return results
    }

    internal fun speedCategoryFromDurations(
        baseSeconds: Int,
        trafficSeconds: Int,
    ): SpeedCategory {
        if (baseSeconds <= 0 || trafficSeconds <= 0) return SpeedCategory.FREE
        val delay = (trafficSeconds - baseSeconds).coerceAtLeast(0)
        val ratio = trafficSeconds.toDouble() / baseSeconds.toDouble()
        return when {
            delay <= 30 && ratio <= 1.08 -> SpeedCategory.FREE
            delay <= 120 && ratio <= 1.25 -> SpeedCategory.MODERATE
            delay <= 300 && ratio <= 1.55 -> SpeedCategory.SLOW
            else -> SpeedCategory.JAM
        }
    }

    internal fun mapGoogleSpeed(raw: String): SpeedCategory =
        when (raw.uppercase()) {
            "NORMAL" -> SpeedCategory.FREE
            "SLOW" -> SpeedCategory.SLOW
            "TRAFFIC_JAM" -> SpeedCategory.JAM
            "TRAFFIC_UNSPECIFIED" -> SpeedCategory.UNKNOWN
            else -> SpeedCategory.MODERATE
        }

    private fun extractNestedPolylinePoints(stepJson: String): String? {
        val polylineKey = "\"polyline\""
        val polyIdx = stepJson.indexOf(polylineKey)
        if (polyIdx == -1) return null
        val pointsKey = "\"points\""
        val pointsIdx = stepJson.indexOf(pointsKey, polyIdx)
        if (pointsIdx == -1) return null
        return extractJsonQuotedStringFollowingKey(stepJson, pointsIdx + pointsKey.length)
    }

    private fun readDurationSeconds(
        json: String,
        field: String,
    ): Int? {
        val key = "\"$field\""
        val idx = json.indexOf(key)
        if (idx == -1) return null
        return intValueFromDistanceOrDurationKey(json, idx)
    }

    private fun readIntField(
        json: String,
        field: String,
    ): Int? {
        val key = "\"$field\""
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val colon = json.indexOf(':', idx + key.length)
        if (colon == -1) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) i++
        val start = i
        while (i < json.length && (json[i].isDigit() || json[i] == '-')) i++
        if (i == start) return null
        return json.substring(start, i).toIntOrNull()
    }

    private fun readStringField(
        json: String,
        field: String,
    ): String? {
        val key = "\"$field\""
        val idx = json.indexOf(key)
        if (idx == -1) return null
        return extractJsonQuotedStringFollowingKey(json, idx + key.length)
    }

    private fun findMatchingBracket(
        json: String,
        openIndex: Int,
    ): Int? {
        if (openIndex >= json.length || json[openIndex] != '[') return null
        var depth = 0
        for (i in openIndex until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }
}
