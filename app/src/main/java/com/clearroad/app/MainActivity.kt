package com.clearroad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteDecisionEngine
import com.clearroad.app.domain.RouteOption
import com.clearroad.app.ui.theme.ClearRoad2Theme
import com.google.android.gms.maps.model.LatLng
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
)

private fun buildRealRouteDebugData(
    distanceDuration: Pair<String, String>?,
    distanceDurationValues: Pair<Int, Int>?,
): RealRouteDebugData? {
    if (distanceDuration == null || distanceDurationValues == null) return null
    val (distanceText, durationText) = distanceDuration
    val (distanceMeters, durationSeconds) = distanceDurationValues
    return RealRouteDebugData(
        distanceText = distanceText,
        durationText = durationText,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
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

        return RealRouteDebugData(
            distanceText = distanceText,
            durationText = durationText,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
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
                tollAed = index * 20.0,
                salikGates = index * 2,
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
                tollAed = 0.0,
                salikGates = 0,
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
    LaunchedEffect(selectedFromLatLng, selectedToLatLng) {
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
            onValueChange = { newText ->
                selectedFromPlaceId = null
                selectedFromLatLng = null
                originText = newText
                if (newText.length < 2) {
                    fromPredictions = emptyList()
                } else if (placesClient == null) {
                    fromPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            fromPredictions = response.autocompletePredictions
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
        if (fromPredictions.isNotEmpty()) {
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
            onValueChange = { newText ->
                selectedToPlaceId = null
                selectedToLatLng = null
                destinationText = newText
                if (newText.length < 2) {
                    toPredictions = emptyList()
                } else if (placesClient == null) {
                    toPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            toPredictions = response.autocompletePredictions
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
        if (toPredictions.isNotEmpty()) {
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
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
            text = decision?.choice ?: "Enter a route",
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
            text = decision?.why
                ?: "Add starting point and destination to get a recommendation.",
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
            text = decision?.tip ?: "Start with a common UAE route.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val fromCoords = selectedFromLatLng
        val toCoords = selectedToLatLng
        if (fromCoords != null && toCoords != null) {
            directionsRequestPreview(fromCoords, toCoords)?.let { preview ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Directions URL ready",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    directionsLoading -> "Directions API loading..."
                    directionsResponse != null -> "Directions API OK"
                    else -> "Directions API failed"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            directionsStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Directions status: $status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            directionsDistanceDuration?.let { (distance, duration) ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Distance: $distance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration: $duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            directionsDistanceDurationValues?.let { (distanceValue, durationValue) ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Distance value: $distanceValue m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration value: $durationValue sec",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val directionsDataValid = isDirectionsDataValid(
                directionsStatus,
                directionsDistanceDuration,
                directionsDistanceDurationValues,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (directionsDataValid) {
                    "Directions data valid"
                } else {
                    "Directions data not valid"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            realRouteDebugData?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real route debug data ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (realRouteDebugDataList.isNotEmpty()) {
                val debugRoutes = realRouteDebugDataList
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real route options: ${debugRoutes.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (index in debugRoutes.indices) {
                    val item = debugRoutes[index]
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Route ${index + 1}: ${item.durationText}, ${item.distanceText}, fake toll ${index * 20} AED",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
