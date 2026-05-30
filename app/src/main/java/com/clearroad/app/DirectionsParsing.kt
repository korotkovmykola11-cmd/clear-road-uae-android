package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class RealRouteDebugData(
    val distanceText: String,
    val durationText: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val tollAED: Int,
    val hasToll: Boolean,
    val corridorScanText: String = "",
    val routePathPoints: List<LatLng> = emptyList(),
)

internal fun buildDirectionsUrl(origin: LatLng, destination: LatLng): String {
    val o = "${origin.latitude},${origin.longitude}"
    val d = "${destination.latitude},${destination.longitude}"
    val key = BuildConfig.PLACES_API_KEY
    return "https://maps.googleapis.com/maps/api/directions/json?origin=$o&destination=$d&mode=driving&alternatives=true&key=$key"
}

internal suspend fun fetchDirectionsRaw(url: String): String? =
    withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 20_000
            }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

internal fun extractDirectionsStatus(json: String): String? {
    val marker = "\"status\""
    val keyIdx = json.indexOf(marker)
    if (keyIdx == -1) return null
    val colon = json.indexOf(':', keyIdx + marker.length)
    if (colon == -1) return null
    var i = colon + 1
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != '"') return null
    val start = i + 1
    val end = json.indexOf('"', start)
    if (end == -1) return null
    return json.substring(start, end)
}

internal fun extractFirstLegDistanceDuration(json: String): Pair<String, String>? {
    fun textFromDistanceOrDurationKey(keyIndex: Int): String? {
        val colon = json.indexOf(':', keyIndex)
        if (colon == -1) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '{') return null
        val innerStart = i + 1
        val innerClose = json.indexOf('}', innerStart)
        if (innerClose == -1) return null
        val textMarker = "\"text\""
        val textIdx = json.indexOf(textMarker, innerStart)
        if (textIdx == -1 || textIdx >= innerClose) return null
        val textColon = json.indexOf(':', textIdx + textMarker.length)
        if (textColon == -1 || textColon >= innerClose) return null
        var j = textColon + 1
        while (j < innerClose && json[j].isWhitespace()) j++
        if (j >= innerClose || json[j] != '"') return null
        val strStart = j + 1
        val strEnd = json.indexOf('"', strStart)
        if (strEnd == -1 || strEnd > innerClose) return null
        return json.substring(strStart, strEnd)
    }

    val routesIdx = json.indexOf("\"routes\"")
    if (routesIdx == -1) return null
    val routesBracket = json.indexOf('[', routesIdx)
    if (routesBracket == -1) return null
    val firstRouteBrace = json.indexOf('{', routesBracket)
    if (firstRouteBrace == -1) return null
    val legsIdx = json.indexOf("\"legs\"", firstRouteBrace)
    if (legsIdx == -1) return null
    val legsBracket = json.indexOf('[', legsIdx)
    if (legsBracket == -1) return null
    val firstLegBrace = json.indexOf('{', legsBracket)
    if (firstLegBrace == -1) return null

    val stepsIdx = json.indexOf("\"steps\"", firstLegBrace)
    val legScanEnd = if (stepsIdx == -1) json.length else stepsIdx

    val distanceIdx = json.indexOf("\"distance\"", firstLegBrace)
    if (distanceIdx == -1 || distanceIdx >= legScanEnd) return null
    val durationIdx = json.indexOf("\"duration\"", firstLegBrace)
    if (durationIdx == -1 || durationIdx >= legScanEnd) return null

    val distanceText = textFromDistanceOrDurationKey(distanceIdx) ?: return null
    val durationText = textFromDistanceOrDurationKey(durationIdx) ?: return null
    return Pair(distanceText, durationText)
}

internal fun extractFirstLegDistanceDurationValues(json: String): Pair<Int, Int>? {
    fun intValueFromDistanceOrDurationKey(keyIndex: Int): Int? {
        val colon = json.indexOf(':', keyIndex)
        if (colon == -1) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '{') return null
        val innerStart = i + 1
        val innerClose = json.indexOf('}', innerStart)
        if (innerClose == -1) return null
        val valueMarker = "\"value\""
        val valueIdx = json.indexOf(valueMarker, innerStart)
        if (valueIdx == -1 || valueIdx >= innerClose) return null
        val valueColon = json.indexOf(':', valueIdx + valueMarker.length)
        if (valueColon == -1 || valueColon >= innerClose) return null
        var j = valueColon + 1
        while (j < innerClose && json[j].isWhitespace()) j++
        val numStart = j
        while (j < innerClose && json[j].isDigit()) j++
        if (j == numStart) return null
        return json.substring(numStart, j).toIntOrNull()
    }

    val routesIdx = json.indexOf("\"routes\"")
    if (routesIdx == -1) return null
    val routesBracket = json.indexOf('[', routesIdx)
    if (routesBracket == -1) return null
    val firstRouteBrace = json.indexOf('{', routesBracket)
    if (firstRouteBrace == -1) return null
    val legsIdx = json.indexOf("\"legs\"", firstRouteBrace)
    if (legsIdx == -1) return null
    val legsBracket = json.indexOf('[', legsIdx)
    if (legsBracket == -1) return null
    val firstLegBrace = json.indexOf('{', legsBracket)
    if (firstLegBrace == -1) return null

    val stepsIdx = json.indexOf("\"steps\"", firstLegBrace)
    val legScanEnd = if (stepsIdx == -1) json.length else stepsIdx

    val distanceIdx = json.indexOf("\"distance\"", firstLegBrace)
    if (distanceIdx == -1 || distanceIdx >= legScanEnd) return null
    val durationIdx = json.indexOf("\"duration\"", firstLegBrace)
    if (durationIdx == -1 || durationIdx >= legScanEnd) return null

    val distanceValue = intValueFromDistanceOrDurationKey(distanceIdx) ?: return null
    val durationValue = intValueFromDistanceOrDurationKey(durationIdx) ?: return null
    return Pair(distanceValue, durationValue)
}

private fun skipJsonStringContent(json: String, openQuoteIndex: Int): Int {
    var i = openQuoteIndex + 1
    while (i < json.length) {
        when (json[i]) {
            '\\' -> i += 2
            '"' -> return i + 1
            else -> i++
        }
    }
    return json.length
}

private fun extractJsonQuotedStringFollowingKey(json: String, searchFrom: Int): String? {
    val colon = json.indexOf(':', searchFrom)
    if (colon == -1) return null
    var j = colon + 1
    while (j < json.length && json[j].isWhitespace()) j++
    if (j >= json.length || json[j] != '"') return null
    return decodeJsonStringLiteral(json, j)
}

/** Unescape a JSON string literal; [openQuoteIndex] must point at the opening `"`. */
private fun decodeJsonStringLiteral(json: String, openQuoteIndex: Int): String? {
    if (openQuoteIndex >= json.length || json[openQuoteIndex] != '"') return null
    val sb = StringBuilder()
    var i = openQuoteIndex + 1
    while (i < json.length) {
        when (json[i]) {
            '\\' -> {
                if (i + 1 >= json.length) return null
                when (json[i + 1]) {
                    '"', '\\', '/' -> {
                        sb.append(json[i + 1])
                        i += 2
                    }
                    'b' -> {
                        sb.append('\b')
                        i += 2
                    }
                    'f' -> {
                        sb.append('\u000C')
                        i += 2
                    }
                    'n' -> {
                        sb.append('\n')
                        i += 2
                    }
                    'r' -> {
                        sb.append('\r')
                        i += 2
                    }
                    't' -> {
                        sb.append('\t')
                        i += 2
                    }
                    'u' -> {
                        if (i + 5 >= json.length) return null
                        sb.append(json.substring(i + 2, i + 6).toInt(16).toChar())
                        i += 6
                    }
                    else -> return null
                }
            }
            '"' -> return sb.toString()
            else -> {
                sb.append(json[i])
                i++
            }
        }
    }
    return null
}

private fun extractJsonNumberFollowingKey(json: String, searchFrom: Int): Double? {
    val colon = json.indexOf(':', searchFrom)
    if (colon == -1) return null
    var j = colon + 1
    while (j < json.length && json[j].isWhitespace()) j++
    val start = j
    while (j < json.length && (json[j].isDigit() || json[j] == '.' || json[j] == '-')) j++
    if (j == start) return null
    return json.substring(start, j).toDoubleOrNull()
}

private val stripHtmlTagRegex = Regex("<[^>]+>")

private fun stripHtmlTags(raw: String): String =
    stripHtmlTagRegex.replace(raw, " ")

private fun extractRouteSummaryPlain(routeJson: String): String? {
    val key = "\"summary\""
    val idx = routeJson.indexOf(key)
    if (idx == -1) return null
    return extractJsonQuotedStringFollowingKey(routeJson, idx + key.length)?.let(::stripHtmlTags)
}

private fun appendHtmlInstructionPlainTexts(routeJson: String, budget: Int): String {
    val key = "\"html_instructions\""
    val sb = StringBuilder()
    var from = 0
    while (sb.length < budget && from < routeJson.length) {
        val idx = routeJson.indexOf(key, from)
        if (idx == -1) break
        val chunk =
            extractJsonQuotedStringFollowingKey(routeJson, idx + key.length)?.let(::stripHtmlTags)
        if (chunk != null) sb.append(chunk).append(' ')
        from = idx + key.length
    }
    return sb.toString().take(budget)
}

internal fun buildCorridorScanText(routeJson: String): String =
    buildString {
        extractRouteSummaryPlain(routeJson)?.let { append(it).append(' ') }
        append(appendHtmlInstructionPlainTexts(routeJson, 6000))
    }.trim()

private fun tryExtractFareAed(routeJson: String): Int? {
    val fareKey = "\"fare\""
    val fi = routeJson.indexOf(fareKey)
    if (fi == -1) return null
    val open = routeJson.indexOf('{', fi + fareKey.length)
    if (open == -1) return null
    val close = findMatchingClosingBrace(routeJson, open) ?: return null
    val fareObj = routeJson.substring(open, close + 1)
    val currencyLabel = "\"currency\""
    val ci = fareObj.indexOf(currencyLabel)
    if (ci == -1) return null
    val currency =
        extractJsonQuotedStringFollowingKey(fareObj, ci + currencyLabel.length)
            ?: return null
    if (!currency.trim().equals("AED", ignoreCase = true)) return null
    val valueLabel = "\"value\""
    val vi = fareObj.indexOf(valueLabel)
    if (vi == -1) return null
    val num = extractJsonNumberFollowingKey(fareObj, vi + valueLabel.length) ?: return null
    return num.roundToInt().coerceAtLeast(0)
}

private fun extractOverviewPolylinePoints(routeJson: String): String? {
    val overviewKey = "\"overview_polyline\""
    // Route-level overview_polyline follows legs; first indexOf can match html inside steps.
    val overviewIdx = routeJson.lastIndexOf(overviewKey)
    if (overviewIdx == -1) return null
    val open = routeJson.indexOf('{', overviewIdx + overviewKey.length)
    if (open == -1) return null
    val close = findMatchingClosingBrace(routeJson, open) ?: return null
    val overviewObj = routeJson.substring(open, close + 1)
    val pointsKey = "\"points\""
    val pointsIdx = overviewObj.lastIndexOf(pointsKey)
    if (pointsIdx == -1) return null
    return extractJsonQuotedStringFollowingKey(overviewObj, pointsIdx + pointsKey.length)
}

internal fun decodeRoutePathPoints(encodedPolyline: String?): List<LatLng> {
    if (encodedPolyline.isNullOrBlank()) return emptyList()
    return try {
        PolyUtil.decode(encodedPolyline)
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun deriveTollAedFromRouteJson(routeJson: String): Pair<Int, Boolean> {
    val fare = tryExtractFareAed(routeJson)
    return when {
        fare == null -> Pair(0, false)
        fare > 0 -> Pair(fare, true)
        else -> Pair(0, false)
    }
}

internal fun firstRouteObjectJson(directionsJson: String): String? {
    val routesIdx = directionsJson.indexOf("\"routes\"")
    if (routesIdx == -1) return null
    val routesBracket = directionsJson.indexOf('[', routesIdx)
    if (routesBracket == -1) return null
    var i = routesBracket + 1
    while (i < directionsJson.length) {
        while (i < directionsJson.length &&
            (directionsJson[i].isWhitespace() || directionsJson[i] == ',')) i++
        if (i >= directionsJson.length) break
        if (directionsJson[i] == ']') break
        if (directionsJson[i] != '{') {
            i++
            continue
        }
        val routeStart = i
        val routeEnd = findMatchingClosingBrace(directionsJson, routeStart) ?: return null
        return directionsJson.substring(routeStart, routeEnd + 1)
    }
    return null
}

private fun findMatchingClosingBrace(json: String, openBraceIndex: Int): Int? {
    if (openBraceIndex >= json.length || json[openBraceIndex] != '{') return null
    var depth = 0
    var i = openBraceIndex
    while (i < json.length) {
        when (json[i]) {
            '"' -> i = skipJsonStringContent(json, i)
            '{' -> {
                depth++
                i++
            }
            '}' -> {
                depth--
                if (depth == 0) return i
                i++
            }
            else -> i++
        }
    }
    return null
}

internal fun extractRouteLegsDebugData(json: String): List<RealRouteDebugData> {
    fun textFromDistanceOrDurationKey(routeJson: String, keyIndex: Int): String? {
        val colon = routeJson.indexOf(':', keyIndex)
        if (colon == -1) return null
        var i = colon + 1
        while (i < routeJson.length && routeJson[i].isWhitespace()) i++
        if (i >= routeJson.length || routeJson[i] != '{') return null
        val innerStart = i + 1
        val innerClose = routeJson.indexOf('}', innerStart)
        if (innerClose == -1) return null
        val textMarker = "\"text\""
        val textIdx = routeJson.indexOf(textMarker, innerStart)
        if (textIdx == -1 || textIdx >= innerClose) return null
        val textColon = routeJson.indexOf(':', textIdx + textMarker.length)
        if (textColon == -1 || textColon >= innerClose) return null
        var j = textColon + 1
        while (j < innerClose && routeJson[j].isWhitespace()) j++
        if (j >= innerClose || routeJson[j] != '"') return null
        val strStart = j + 1
        val strEnd = routeJson.indexOf('"', strStart)
        if (strEnd == -1 || strEnd > innerClose) return null
        return routeJson.substring(strStart, strEnd)
    }

    fun intValueFromDistanceOrDurationKey(routeJson: String, keyIndex: Int): Int? {
        val colon = routeJson.indexOf(':', keyIndex)
        if (colon == -1) return null
        var i = colon + 1
        while (i < routeJson.length && routeJson[i].isWhitespace()) i++
        if (i >= routeJson.length || routeJson[i] != '{') return null
        val innerStart = i + 1
        val innerClose = routeJson.indexOf('}', innerStart)
        if (innerClose == -1) return null
        val valueMarker = "\"value\""
        val valueIdx = routeJson.indexOf(valueMarker, innerStart)
        if (valueIdx == -1 || valueIdx >= innerClose) return null
        val valueColon = routeJson.indexOf(':', valueIdx + valueMarker.length)
        if (valueColon == -1 || valueColon >= innerClose) return null
        var j = valueColon + 1
        while (j < innerClose && routeJson[j].isWhitespace()) j++
        val numStart = j
        while (j < innerClose && routeJson[j].isDigit()) j++
        if (j == numStart) return null
        return routeJson.substring(numStart, j).toIntOrNull()
    }

    fun firstLegDebugFromRouteObject(routeJson: String): RealRouteDebugData? {
        val legsIdx = routeJson.indexOf("\"legs\"")
        if (legsIdx == -1) return null
        val legsBracket = routeJson.indexOf('[', legsIdx)
        if (legsBracket == -1) return null
        val firstLegBrace = routeJson.indexOf('{', legsBracket)
        if (firstLegBrace == -1) return null

        val stepsIdx = routeJson.indexOf("\"steps\"", firstLegBrace)
        val legScanEnd = if (stepsIdx == -1) routeJson.length else stepsIdx

        val distanceIdx = routeJson.indexOf("\"distance\"", firstLegBrace)
        if (distanceIdx == -1 || distanceIdx >= legScanEnd) return null
        val durationIdx = routeJson.indexOf("\"duration\"", firstLegBrace)
        if (durationIdx == -1 || durationIdx >= legScanEnd) return null

        val distanceText =
            textFromDistanceOrDurationKey(routeJson, distanceIdx) ?: return null
        val durationText =
            textFromDistanceOrDurationKey(routeJson, durationIdx) ?: return null
        val distanceMeters =
            intValueFromDistanceOrDurationKey(routeJson, distanceIdx) ?: return null
        val durationSeconds =
            intValueFromDistanceOrDurationKey(routeJson, durationIdx) ?: return null

        val tollPair = deriveTollAedFromRouteJson(routeJson)
        val corridorScanText = buildCorridorScanText(routeJson)
        val routePathPoints =
            decodeRoutePathPoints(extractOverviewPolylinePoints(routeJson))

        return RealRouteDebugData(
            distanceText = distanceText,
            durationText = durationText,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollPair.first,
            hasToll = tollPair.second,
            corridorScanText = corridorScanText,
            routePathPoints = routePathPoints,
        )
    }

    val routesIdx = json.indexOf("\"routes\"")
    if (routesIdx == -1) return emptyList()
    val routesBracket = json.indexOf('[', routesIdx)
    if (routesBracket == -1) return emptyList()

    val results = mutableListOf<RealRouteDebugData>()
    var i = routesBracket + 1
    while (i < json.length) {
        while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
        if (i >= json.length) break
        if (json[i] == ']') break
        if (json[i] != '{') {
            i++
            continue
        }
        val routeStart = i
        val routeEnd = findMatchingClosingBrace(json, routeStart) ?: break
        val routeJson = json.substring(routeStart, routeEnd + 1)
        firstLegDebugFromRouteObject(routeJson)?.let { results.add(it) }
        i = routeEnd + 1
    }
    return results
}
