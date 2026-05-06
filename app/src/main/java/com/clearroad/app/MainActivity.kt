package com.clearroad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteDecisionEngine
import com.clearroad.app.domain.RouteOption
import com.clearroad.app.ui.theme.ClearRoad2Theme
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.text.Charsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Places.isInitialized() && BuildConfig.PLACES_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        }
        val placesClient =
            if (Places.isInitialized()) Places.createClient(this) else null
        setContent {
            ClearRoad2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClearRoadScreen(
                        modifier = Modifier.padding(innerPadding),
                        placesClient = placesClient,
                    )
                }
            }
        }
    }
}

private fun preferenceModeLabel(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> "Fastest"
        PreferenceMode.NO_TOLLS -> "No tolls"
        PreferenceMode.CALM -> "Calm"
    }

private fun manualRouteChoice(mode: PreferenceMode, routeIndex: Int): String =
    when (mode) {
        PreferenceMode.FASTEST -> when (routeIndex) {
            0 -> "Best route"
            1 -> "Alternative fast route"
            else -> "Longer option"
        }
        PreferenceMode.NO_TOLLS -> when (routeIndex) {
            0 -> "Easiest on tolls"
            1 -> "Moderate toll route"
            else -> "Higher toll option"
        }
        PreferenceMode.CALM -> "Balanced route"
    }

private fun manualRouteWhy(mode: PreferenceMode, routeIndex: Int): String =
    when (mode) {
        PreferenceMode.FASTEST -> when (routeIndex) {
            0 -> "Uses the shortest-time option among these directions."
            1 -> "Comparable corridor with a bit more time on the road."
            else -> "Takes longer — compare toll and comfort before you go."
        }
        PreferenceMode.NO_TOLLS -> when (routeIndex) {
            0 -> "Keeps toll spend lowest among these paths."
            1 -> "Balances toll cost with time somewhat evenly."
            else -> "Expect higher toll lines along this path."
        }
        PreferenceMode.CALM -> "Balances driving time and road cost."
    }

private fun manualRouteTip(mode: PreferenceMode, routeIndex: Int): String =
    when (mode) {
        PreferenceMode.FASTEST -> when (routeIndex) {
            0 -> "Stick with this if minutes matter most."
            1 -> "Good middle ground when traffic shifts."
            else -> "Review toll totals before committing."
        }
        PreferenceMode.NO_TOLLS -> when (routeIndex) {
            0 -> "Kindest on Salik spend among these."
            1 -> "Carry tag balance for occasional gates."
            else -> "Keep Salik topped up if you pick this one."
        }
        PreferenceMode.CALM -> "Good when you want a smoother overall drive."
    }

private fun fetchLatLng(
    client: PlacesClient?,
    placeId: String?,
    onResult: (LatLng?) -> Unit,
) {
    if (placeId == null || client == null) {
        onResult(null)
        return
    }

    client.fetchPlace(
        FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.LOCATION),
        ).build(),
    )
        .addOnSuccessListener { response ->
            onResult(response.place.location)
        }
        .addOnFailureListener {
            onResult(null)
        }
}

private fun latLngToCommaString(latLng: LatLng): String =
    "${latLng.latitude},${latLng.longitude}"

private fun directionsRequestPreview(
    selectedFromLatLng: LatLng?,
    selectedToLatLng: LatLng?,
): String? {
    val from = selectedFromLatLng ?: return null
    val to = selectedToLatLng ?: return null
    return buildString {
        appendLine("Directions request ready:")
        appendLine("origin=${latLngToCommaString(from)}")
        appendLine("destination=${latLngToCommaString(to)}")
    }.trimEnd()
}

private fun buildDirectionsUrl(origin: LatLng, destination: LatLng): String {
    val o = latLngToCommaString(origin)
    val d = latLngToCommaString(destination)
    val key = BuildConfig.PLACES_API_KEY
    return "https://maps.googleapis.com/maps/api/directions/json?origin=$o&destination=$d&mode=driving&alternatives=true&key=$key"
}

private suspend fun fetchDirectionsRaw(url: String): String? =
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

private fun extractDirectionsStatus(json: String): String? {
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

private fun extractFirstLegDistanceDuration(json: String): Pair<String, String>? {
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

private fun extractFirstLegDistanceDurationValues(json: String): Pair<Int, Int>? {
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

private fun isDirectionsDataValid(
    status: String?,
    distanceDuration: Pair<String, String>?,
    distanceDurationValues: Pair<Int, Int>?,
): Boolean {
    if (status != "OK") return false
    if (distanceDuration == null) return false
    if (distanceDurationValues == null) return false
    val (distanceMeters, durationSeconds) = distanceDurationValues
    return distanceMeters > 0 && durationSeconds > 0
}

private data class RealRouteDebugData(
    val distanceText: String,
    val durationText: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val tollAED: Int,
    val hasToll: Boolean,
)

private fun buildRealRouteDebugData(
    distanceDuration: Pair<String, String>?,
    distanceDurationValues: Pair<Int, Int>?,
    routeJsonForToll: String? = null,
): RealRouteDebugData? {
    if (distanceDuration == null || distanceDurationValues == null) return null
    val (distanceText, durationText) = distanceDuration
    val (distanceMeters, durationSeconds) = distanceDurationValues
    val tollPair =
        routeJsonForToll?.let { deriveTollAedFromRouteJson(it) } ?: Pair(0, false)
    return RealRouteDebugData(
        distanceText = distanceText,
        durationText = durationText,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        tollAED = tollPair.first,
        hasToll = tollPair.second,
    )
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
    val start = j + 1
    val end = json.indexOf('"', start)
    if (end == -1) return null
    return json.substring(start, end)
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

private fun deriveTollAedFromRouteJson(routeJson: String): Pair<Int, Boolean> {
    val fare = tryExtractFareAed(routeJson)
    return when {
        fare == null -> Pair(0, false)
        fare > 0 -> Pair(fare, true)
        else -> Pair(0, false)
    }
}

private fun firstRouteObjectJson(directionsJson: String): String? {
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

private fun extractRouteLegsDebugData(json: String): List<RealRouteDebugData> {
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

        return RealRouteDebugData(
            distanceText = distanceText,
            durationText = durationText,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollPair.first,
            hasToll = tollPair.second,
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

@Composable
fun ClearRoadScreen(
    modifier: Modifier = Modifier,
    placesClient: PlacesClient? = null,
) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PreferenceMode.FASTEST) }
    var fromPredictions by remember {
        mutableStateOf<List<AutocompletePrediction>>(emptyList())
    }
    var toPredictions by remember {
        mutableStateOf<List<AutocompletePrediction>>(emptyList())
    }
    var selectedFromPlaceId by remember { mutableStateOf<String?>(null) }
    var selectedToPlaceId by remember { mutableStateOf<String?>(null) }
    var selectedFromLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }
    var selectedToLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }
    var directionsResponse by remember { mutableStateOf<String?>(null) }
    var directionsStatus by remember { mutableStateOf<String?>(null) }
    var directionsDistanceDuration by remember {
        mutableStateOf<Pair<String, String>?>(null)
    }
    var directionsDistanceDurationValues by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    var realRouteDebugData by remember {
        mutableStateOf<RealRouteDebugData?>(null)
    }
    var realRouteDebugDataList by remember {
        mutableStateOf<List<RealRouteDebugData>>(emptyList())
    }
    var selectedRouteIndex by remember { mutableStateOf(0) }
    var directionsLoading by remember { mutableStateOf(false) }
    val isRouteReady =
        selectedFromLatLng != null && selectedToLatLng != null
    val routeList = realRouteDebugDataList
    val data = realRouteDebugData
    val routesForDecision = when {
        routeList.isNotEmpty() -> routeList.mapIndexed { index, item ->
            RouteOption(
                id = "real_route_$index",
                name = "Real route ${index + 1}",
                durationMin = item.durationSeconds / 60,
                distanceKm = item.distanceMeters / 1000.0,
                tollAed = item.tollAED.toDouble(),
                salikGates = if (item.hasToll) 1 else 0,
                passesAbuDhabi = false,
                parkingMayBePaid = false,
            )
        }
        data != null -> listOf(
            RouteOption(
                id = "real_route",
                name = "Real route",
                durationMin = data.durationSeconds / 60,
                distanceKm = data.distanceMeters / 1000.0,
                tollAed = data.tollAED.toDouble(),
                salikGates = if (data.hasToll) 1 else 0,
                passesAbuDhabi = false,
                parkingMayBePaid = false,
            ),
        )
        else -> RouteDecisionEngine.sampleRoutes
    }
    val decision = if (isRouteReady) {
        RouteDecisionEngine.choose(
            routesForDecision,
            selectedMode,
        )
    } else {
        null
    }
    val showRouteCardOverrides = realRouteDebugDataList.isNotEmpty()
    val calmRouteIndex = when {
        realRouteDebugDataList.size >= 3 -> 1
        realRouteDebugDataList.size == 2 -> 1
        else -> 0
    }
    val recommendedRouteIndex =
        if (!showRouteCardOverrides) {
            0
        } else if (selectedMode == PreferenceMode.CALM) {
            calmRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
        } else {
            val id = decision?.bestRoute?.id
            if (id != null && id.startsWith("real_route_")) {
                val ri = id.removePrefix("real_route_").toIntOrNull()
                if (ri != null && ri in realRouteDebugDataList.indices) ri else 0
            } else {
                0
            }
        }
    val routeCardSelectionIndex =
        if (showRouteCardOverrides) {
            selectedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
        } else {
            0
        }
    LaunchedEffect(selectedMode, realRouteDebugDataList.size, decision?.bestRoute?.id) {
        if (
            realRouteDebugDataList.isNotEmpty() &&
            selectedMode == PreferenceMode.CALM
        ) {
            selectedRouteIndex =
                calmRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
            return@LaunchedEffect
        }
        val id = decision?.bestRoute?.id ?: return@LaunchedEffect
        if (!id.startsWith("real_route_")) return@LaunchedEffect
        val ri = id.removePrefix("real_route_").toIntOrNull() ?: return@LaunchedEffect
        if (ri !in realRouteDebugDataList.indices) return@LaunchedEffect
        selectedRouteIndex = ri
    }
    LaunchedEffect(selectedFromLatLng, selectedToLatLng) {
        selectedRouteIndex = 0
        realRouteDebugData = null
        realRouteDebugDataList = emptyList()
        directionsResponse = null
        directionsStatus = null
        directionsDistanceDuration = null
        directionsDistanceDurationValues = null
        directionsLoading = false
        val from = selectedFromLatLng
        val to = selectedToLatLng
        if (from == null || to == null) return@LaunchedEffect
        directionsLoading = true
        val raw = fetchDirectionsRaw(buildDirectionsUrl(from, to))
        directionsResponse = raw
        directionsStatus = raw?.let { extractDirectionsStatus(it) }
        directionsDistanceDuration =
            raw?.let { extractFirstLegDistanceDuration(it) }
        directionsDistanceDurationValues =
            raw?.let { extractFirstLegDistanceDurationValues(it) }
        realRouteDebugData = buildRealRouteDebugData(
            directionsDistanceDuration,
            directionsDistanceDurationValues,
            raw?.let(::firstRouteObjectJson),
        )
        realRouteDebugDataList =
            raw?.let { extractRouteLegsDebugData(it) } ?: emptyList()
        directionsLoading = false
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                top = 16.dp,
                end = 24.dp,
                bottom = 32.dp,
            )
            .navigationBarsPadding(),
    ) {
        Text(
            text = "Clear Road",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Route decision assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = originText,
            onValueChange = stopFrom@{ newText ->
                if (newText.isBlank() || newText.length < 2) {
                    fromPredictions = emptyList()
                    selectedFromPlaceId = null
                    selectedFromLatLng = null
                    originText = newText
                    return@stopFrom
                }
                selectedFromPlaceId = null
                selectedFromLatLng = null
                originText = newText
                if (placesClient == null) {
                    fromPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            if (originText == newText &&
                                originText.length >= 2 &&
                                originText.isNotBlank()
                            ) {
                                fromPredictions = response.autocompletePredictions
                            } else {
                                fromPredictions = emptyList()
                            }
                        }
                        .addOnFailureListener {
                            fromPredictions = emptyList()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("From") },
            placeholder = { Text("Enter starting point") },
            singleLine = true,
            maxLines = 1,
        )
        if (fromPredictions.isNotEmpty() &&
            originText.length >= 2 &&
            originText.isNotBlank()
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                fromPredictions.take(5).forEach { prediction ->
                    val label = prediction.getFullText(null).toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                originText = label
                                fromPredictions = emptyList()
                                selectedFromPlaceId = prediction.placeId
                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                    selectedFromLatLng = result
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = destinationText,
            onValueChange = stopTo@{ newText ->
                if (newText.isBlank() || newText.length < 2) {
                    toPredictions = emptyList()
                    selectedToPlaceId = null
                    selectedToLatLng = null
                    destinationText = newText
                    return@stopTo
                }
                selectedToPlaceId = null
                selectedToLatLng = null
                destinationText = newText
                if (placesClient == null) {
                    toPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            if (destinationText == newText &&
                                destinationText.length >= 2 &&
                                destinationText.isNotBlank()
                            ) {
                                toPredictions = response.autocompletePredictions
                            } else {
                                toPredictions = emptyList()
                            }
                        }
                        .addOnFailureListener {
                            toPredictions = emptyList()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To") },
            placeholder = { Text("Enter destination") },
            singleLine = true,
            maxLines = 1,
        )
        if (toPredictions.isNotEmpty() &&
            destinationText.length >= 2 &&
            destinationText.isNotBlank()
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                toPredictions.take(5).forEach { prediction ->
                    val label = prediction.getFullText(null).toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                destinationText = label
                                toPredictions = emptyList()
                                selectedToPlaceId = prediction.placeId
                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                    selectedToLatLng = result
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PreferenceMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Text(
                    text = preferenceModeLabel(mode),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = mode }
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(vertical = 11.dp, horizontal = 6.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        textDecoration = TextDecoration.None,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choice",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                showRouteCardOverrides ->
                    manualRouteChoice(selectedMode, routeCardSelectionIndex)
                else -> decision?.choice ?: "Enter a route"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Why",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                showRouteCardOverrides ->
                    manualRouteWhy(selectedMode, routeCardSelectionIndex)
                else ->
                    decision?.why
                        ?: "Add starting point and destination to get a recommendation."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tip",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                showRouteCardOverrides ->
                    manualRouteTip(selectedMode, routeCardSelectionIndex)
                else -> decision?.tip ?: "Start with a common UAE route."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val fromCoords = selectedFromLatLng
        val toCoords = selectedToLatLng
        if (fromCoords != null && toCoords != null) {
            if (realRouteDebugDataList.isNotEmpty()) {
                val debugRoutes = realRouteDebugDataList
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Available routes: ${debugRoutes.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (index in debugRoutes.indices) {
                    val item = debugRoutes[index]
                    val isUserSelected = index == routeCardSelectionIndex
                    val isRecommended = index == recommendedRouteIndex
                    val outline = MaterialTheme.colorScheme.outline
                    val containerAlpha = when {
                        isUserSelected && isRecommended -> 0.88f
                        isUserSelected -> 0.86f
                        isRecommended -> 0.56f
                        else -> 0.48f
                    }
                    val cardBorder = when {
                        isUserSelected && isRecommended ->
                            BorderStroke(
                                width = 2.5.dp,
                                color = outline.copy(alpha = 0.92f),
                            )
                        isUserSelected ->
                            BorderStroke(
                                width = 2.dp,
                                color = outline.copy(alpha = 0.82f),
                            )
                        else -> null
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRouteIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
                        ),
                        border = cardBorder,
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 4.dp,
                            ),
                        ) {
                            if (isRecommended) {
                                Text(
                                    text = "Recommended",
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.06f,
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.72f,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = "Route ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(0.5.dp),
                            ) {
                                Text(
                                    text = item.durationText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = item.distanceText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Light,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Toll: ${item.tollAED} AED",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClearRoadPreview() {
    ClearRoad2Theme {
        ClearRoadScreen()
    }
}
