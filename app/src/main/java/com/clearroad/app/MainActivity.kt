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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

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

private const val UAE_FUEL_PRICE_PER_LITER = 2.8
private const val AVERAGE_CAR_KM_PER_LITER = 12.0

private enum class UaeCorridorTollHint {
    HIGH_LIKELIHOOD,
    LOW_LIKELIHOOD,
    NEUTRAL,
}

private val uaeHighTollCorridorKeywords = listOf(
    "sheikh zayed road",
    "szr",
    "e11",
    "al garhoud",
    "downtown dubai",
    "business bay",
    "financial centre",
    "financial center",
    "dubai marina",
)

private val uaeLowerTollCorridorKeywords = listOf(
    "mohammed bin zayed road",
    "mbz road",
    "e311",
    "emirates road",
    "e611",
    "ajman",
    "sharjah",
)

private fun corridorTollHintFromScan(scanText: String): UaeCorridorTollHint {
    val lc = scanText.lowercase(Locale.US)
    val highHits = uaeHighTollCorridorKeywords.count { lc.contains(it) }
    val lowHits = uaeLowerTollCorridorKeywords.count { lc.contains(it) }
    return when {
        highHits > lowHits -> UaeCorridorTollHint.HIGH_LIKELIHOOD
        lowHits > highHits -> UaeCorridorTollHint.LOW_LIKELIHOOD
        else -> UaeCorridorTollHint.NEUTRAL
    }
}

private fun preferenceModeLabel(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> "Fastest"
        PreferenceMode.NO_TOLLS -> "No tolls"
        PreferenceMode.CALM -> "Calm"
    }

/** Preference tab mode for lightweight UX copy (Fastest / No tolls / Calm). */
private typealias RouteMode = PreferenceMode

/** Static nuance label beside Recommended — wording pools only, not traffic prediction or scoring. */
private fun confidenceHintBelowRecommendation(
    mode: RouteMode,
    directionsStatus: String?,
    recommendedRouteIndex: Int,
    routeCount: Int,
): String {
    if (directionsStatus != "OK") {
        return when (mode) {
            PreferenceMode.FASTEST -> "Timing unclear"
            PreferenceMode.NO_TOLLS -> "Cost unclear"
            PreferenceMode.CALM -> "Pace unclear"
        }
    }
    val slot =
        ((recommendedRouteIndex.coerceAtLeast(0) + routeCount.coerceAtLeast(1)) % 3)
    return when (mode) {
        PreferenceMode.FASTEST ->
            when (slot) {
                0 -> "Traffic unstable"
                1 -> "Stable flow"
                else -> "Rush-hour sensitive"
            }
        PreferenceMode.NO_TOLLS ->
            when (slot) {
                0 -> "Predictable cost"
                1 -> "Budget-friendly"
                else -> "Longer but cheaper"
            }
        PreferenceMode.CALM ->
            when (slot) {
                0 -> "Smoother drive"
                1 -> "Easier city entry"
                else -> "Less aggressive flow"
            }
    }
}

private fun getTollLevel(tollAED: Int): String =
    when {
        tollAED == 0 -> "none"
        tollAED in 1..8 -> "low"
        tollAED in 9..20 -> "medium"
        else -> "high"
    }

private fun tollPhraseForCard(
    item: RealRouteDebugData,
    routeIndex: Int,
    selectedMode: PreferenceMode,
    recommendedRouteIndex: Int,
    routes: List<RealRouteDebugData>,
): String {
    if (
        selectedMode == PreferenceMode.NO_TOLLS &&
        routeIndex == recommendedRouteIndex
    ) {
        return "Lowest toll route"
    }
    val minDurIdx =
        routes.indices.minByOrNull { routes[it].durationSeconds } ?: routeIndex
    val fastest = routes[minDurIdx]
    val thisTotal =
        estimateTotalRouteCostAed(
            item.tollAED,
            estimateFuelCostAed(item.distanceMeters / 1000.0),
        )
    val fastestTotal =
        estimateTotalRouteCostAed(
            fastest.tollAED,
            estimateFuelCostAed(fastest.distanceMeters / 1000.0),
        )
    if (
        item.durationSeconds > fastest.durationSeconds &&
        thisTotal < fastestTotal
    ) {
        return when (selectedMode) {
            PreferenceMode.NO_TOLLS -> "Toll-saving route"
            else -> "Balanced city entry"
        }
    }
    if (item.durationSeconds <= 0 || item.distanceMeters <= 0) {
        return "Toll estimate"
    }
    val corridorHint = corridorTollHintFromScan(item.corridorScanText)
    val isFastestTimeRoute = routeIndex == minDurIdx
    val durationStretchVsFastest =
        if (fastest.durationSeconds <= 0) {
            0f
        } else {
            (item.durationSeconds - fastest.durationSeconds).toFloat() /
                fastest.durationSeconds.toFloat()
        }
    val tollBand = getTollLevel(item.tollAED)

    return when (selectedMode) {
        PreferenceMode.FASTEST ->
            when {
                isFastestTimeRoute &&
                    (
                        corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD ||
                            tollBand == "medium" ||
                            tollBand == "high"
                        ) ->
                    "Fast toll run"
                isFastestTimeRoute -> "Fast Dubai corridor"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    "Heavier toll drive"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD ->
                    "Balanced city entry"
                durationStretchVsFastest > 0.12f -> "Easier traffic pace"
                tollBand == "none" -> "Direct city run"
                tollBand == "low" -> "Main highway run"
                routeIndex % 2 == 0 -> "Main highway run"
                else -> "Heavier toll drive"
            }
        PreferenceMode.NO_TOLLS ->
            when (corridorHint) {
                UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    if (item.tollAED >= fastest.tollAED) {
                        "Higher toll option"
                    } else {
                        "Heavier toll drive"
                    }
                UaeCorridorTollHint.LOW_LIKELIHOOD -> "Easy on tolls"
                UaeCorridorTollHint.NEUTRAL ->
                    when (tollBand) {
                        "none" -> "Budget-friendly route"
                        "low" -> "Toll-saving route"
                        else ->
                            if (routeIndex % 2 == 0) {
                                "Main highway run"
                            } else {
                                "Heavier toll drive"
                            }
                    }
            }
        PreferenceMode.CALM ->
            when {
                durationStretchVsFastest > 0.1f -> "Smoother drive"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD -> "Easy on tolls"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD -> "Heavier toll drive"
                else -> "Balanced city entry"
            }
    }
}

private fun recommendationAlignedExplanation(
    personality: String,
    mode: PreferenceMode,
): Triple<String, String, String>? =
    when (personality) {
        "Lowest toll route" ->
            Triple(
                "Lowest toll route",
                "Lowest toll among these options.",
                "Good when you want predictable Salik cost.",
            )
        "Fast toll run" ->
            Triple(
                "Fast toll run",
                "Quick run — Salik is more likely along this line.",
                "Top up Salik before you set off.",
            )
        "Fast Dubai corridor" ->
            Triple(
                "Fast Dubai corridor",
                "Faster run through Dubai-side roads.",
                "Use when time matters most.",
            )
        "Balanced city entry" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "Balanced city entry",
                    "Easier city entry than the fastest cut.",
                    "Fine when a few extra minutes buy calmer roads.",
                )
            } else {
                Triple(
                    "Balanced city entry",
                    "Slightly longer, but lighter toll and fuel than the fastest line.",
                    "Glance at fuel and Salik before you head out.",
                )
            }
        "Easier traffic pace" ->
            Triple(
                "Easier traffic pace",
                "Few extra minutes for a steadier stretch.",
                "Fine when you are not chasing every minute.",
            )
        "Smoother drive" ->
            Triple(
                "Smoother drive",
                "Usually runs a bit longer — feels less rushed.",
                "Nice when you’d rather settle in than sprint.",
            )
        "Heavier toll drive" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "Heavier toll drive",
                    "Busier stretch — Salik ramps up here.",
                    "Keep the tag topped before you roll.",
                )
            } else {
                Triple(
                    "Heavier toll drive",
                    "More toll gates likely on this stretch.",
                    "Check Salik before you go.",
                )
            }
        "Toll-saving route" ->
            Triple(
                "Toll-saving route",
                "Longer leg, lighter toll than the quicker cuts.",
                "Pick this when Salik savings beat shaving minutes.",
            )
        "Direct city run" ->
            Triple(
                "Direct city run",
                "Fairly straight shot toward city-side roads.",
                "Glance at traffic if timing feels tight.",
            )
        "Main highway run" ->
            Triple(
                "Main highway run",
                "Typical UAE motorway-style stretch.",
                "Pad time at peak hours.",
            )
        "Higher toll option" ->
            Triple(
                "Higher toll option",
                "Higher toll than the lighter options here.",
                "Use when saving time matters more.",
            )
        "Easy on tolls" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "Easy on tolls",
                    "Skips the heavier Salik stretches when possible.",
                    "Still glance at exits before you commit.",
                )
            } else {
                Triple(
                    "Easy on tolls",
                    "Likely skips the heavier Salik stretches.",
                    "Still eyeball exits on your map.",
                )
            }
        "Budget-friendly route" ->
            Triple(
                "Budget-friendly route",
                "Keeps toll fairly tame between these options.",
                "Works well for everyday UAE trips.",
            )
        "Toll estimate" -> null
        else -> null
    }

private fun manualRouteChoice(
    mode: PreferenceMode,
    routeIndex: Int,
    tollAED: Int? = null,
): String {
    val level = tollAED?.let(::getTollLevel)
    return when (mode) {
        PreferenceMode.FASTEST -> when (routeIndex) {
            0 -> "Fastest option here"
            1 -> "Almost as fast — slightly longer"
            else -> "Longer drive"
        }
        PreferenceMode.NO_TOLLS -> when {
            level == "none" -> "Lowest toll option"
            level == "low" -> "Light toll route"
            else -> when (routeIndex) {
                0 -> "Easiest on tolls"
                1 -> "Moderate toll route"
                else -> "Higher toll route"
            }
        }
        PreferenceMode.CALM -> "Steadier middle ground"
    }
}

private fun manualRouteWhy(
    mode: PreferenceMode,
    routeIndex: Int,
    tollAED: Int? = null,
): String {
    val level = tollAED?.let(::getTollLevel)
    return when (mode) {
        PreferenceMode.FASTEST -> when {
            level == "high" ->
                "Saves time even with higher tolls."
            else -> when (routeIndex) {
                0 ->
                    "Shortest drive time among these."
                1 ->
                    "Close to fastest — slightly more time on the road."
                else ->
                    "Longer — compare tolls and comfort."
            }
        }
        PreferenceMode.NO_TOLLS -> when {
            level == "none" ->
                "Lowest toll among these routes."
            level == "low" ->
                "Keeps toll cost fairly low."
            else -> when (routeIndex) {
                0 ->
                    "Easiest on tolls compared with the others."
                1 ->
                    "Balances toll cost and time."
                else ->
                    "More toll exposure — heavier Salik than lighter options."
            }
        }
        PreferenceMode.CALM ->
            "Splits time and toll without pushing for every minute saved."
    }
}

private fun manualRouteTip(
    mode: PreferenceMode,
    routeIndex: Int,
    tollAED: Int? = null,
): String {
    val level = tollAED?.let(::getTollLevel)
    return when (mode) {
        PreferenceMode.FASTEST -> when {
            level == "high" ->
                "Good when arriving sooner matters more than toll cost."
            else -> when (routeIndex) {
                0 ->
                    "Use when minutes matter most."
                1 ->
                    "Solid option if traffic slows the fastest route."
                else ->
                    "Use when you want more comfort than raw speed."
            }
        }
        PreferenceMode.NO_TOLLS -> when {
            level == "none" ->
                "Good when you want predictable Salik cost."
            level == "low" ->
                "Light tolls — keep Salik topped up anyway."
            else -> when (routeIndex) {
                0 ->
                    "Easiest on Salik among these."
                1 ->
                    "Keep Salik balance for occasional gates."
                else ->
                    "Check Salik balance — tolls add up on this one."
            }
        }
        PreferenceMode.CALM ->
            "Handy when Dubai traffic already feels like plenty."
    }
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
    val corridorScanText: String = "",
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
    val corridorScanText =
        routeJsonForToll?.let(::buildCorridorScanText).orEmpty()
    return RealRouteDebugData(
        distanceText = distanceText,
        durationText = durationText,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        tollAED = tollPair.first,
        hasToll = tollPair.second,
        corridorScanText = corridorScanText,
    )
}

private fun routeConfidenceLabel(
    directionsStatus: String?,
    item: RealRouteDebugData,
): String {
    if (item.durationSeconds <= 0 || item.distanceMeters <= 0) return "Limited"
    if (directionsStatus != "OK") return "Limited"
    val tollKnownFromFare = item.hasToll || item.tollAED > 0
    return if (tollKnownFromFare) "Reliable" else "Estimated"
}

private fun estimateFuelCostAed(distanceKm: Double): Int {
    val litersUsed = distanceKm / AVERAGE_CAR_KM_PER_LITER
    val fuelCost = litersUsed * UAE_FUEL_PRICE_PER_LITER
    return fuelCost.roundToInt()
}

private fun estimateTotalRouteCostAed(
    tollAED: Int,
    fuelAED: Int,
): Int =
    tollAED + fuelAED

private fun calculateAedPerMinute(
    totalCostAed: Double,
    durationMinutes: Int,
): Double =
    if (durationMinutes <= 0) totalCostAed
    else totalCostAed / durationMinutes

private fun calculateRouteScore(
    mode: PreferenceMode,
    durationMinutes: Int,
    distanceKm: Double,
    tollAED: Double,
    totalCostAED: Double,
): Double {
    val aedPerMinute =
        calculateAedPerMinute(totalCostAED, durationMinutes)
    return when (mode) {
        PreferenceMode.FASTEST ->
            durationMinutes + totalCostAED * 0.15 + distanceKm * 0.05 +
                aedPerMinute * 0.2
        PreferenceMode.NO_TOLLS ->
            totalCostAED * 4.0 + tollAED * 6.0 + durationMinutes * 0.35 +
                aedPerMinute * 0.4
        PreferenceMode.CALM ->
            durationMinutes * 0.6 + totalCostAED * 1.2 + distanceKm * 0.15 +
                aedPerMinute * 0.3
    }
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

private fun buildCorridorScanText(routeJson: String): String =
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
        val corridorScanText = buildCorridorScanText(routeJson)

        return RealRouteDebugData(
            distanceText = distanceText,
            durationText = durationText,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollPair.first,
            hasToll = tollPair.second,
            corridorScanText = corridorScanText,
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
    val recommendedRouteIndex =
        if (!showRouteCardOverrides || realRouteDebugDataList.isEmpty()) {
            0
        } else {
            realRouteDebugDataList.indices.minWith(
                compareBy(
                    { idx ->
                        val item = realRouteDebugDataList[idx]
                        val distanceKm = item.distanceMeters / 1000.0
                        val fuelAed = estimateFuelCostAed(distanceKm)
                        val totalCostAed =
                            estimateTotalRouteCostAed(item.tollAED, fuelAed).toDouble()
                        calculateRouteScore(
                            selectedMode,
                            item.durationSeconds / 60,
                            distanceKm,
                            item.tollAED.toDouble(),
                            totalCostAed,
                        )
                    },
                    { it },
                ),
            )
        }
    val routeCardSelectionIndex =
        if (showRouteCardOverrides) {
            selectedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
        } else {
            0
        }
    val recommendationTollAed =
        if (showRouteCardOverrides && realRouteDebugDataList.isNotEmpty()) {
            realRouteDebugDataList[
                recommendedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex),
            ].tollAED
        } else {
            null
        }
    val recommendedRoutePersonalityLine =
        if (showRouteCardOverrides && realRouteDebugDataList.isNotEmpty()) {
            val ri =
                recommendedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
            tollPhraseForCard(
                realRouteDebugDataList[ri],
                ri,
                selectedMode,
                recommendedRouteIndex,
                realRouteDebugDataList,
            )
        } else {
            null
        }
    val recommendationAlignedCopy =
        recommendedRoutePersonalityLine?.let {
            recommendationAlignedExplanation(it, selectedMode)
        }
    LaunchedEffect(selectedMode, realRouteDebugDataList.size, recommendedRouteIndex) {
        if (realRouteDebugDataList.isEmpty()) return@LaunchedEffect
        selectedRouteIndex =
            recommendedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
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
                bottom = 52.dp,
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
        Spacer(modifier = Modifier.height(10.dp))

        val recommendationCompact = showRouteCardOverrides
        val recLabelStyle =
            if (recommendationCompact) {
                MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            }
        val recBodyStyle =
            if (recommendationCompact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            }
        val recGapLabelToBody = if (recommendationCompact) 1.dp else 2.dp
        val recGapBetweenSections = if (recommendationCompact) 5.dp else 8.dp

        Text(
            text = "Choice",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = when {
                showRouteCardOverrides ->
                    recommendationAlignedCopy?.first
                        ?: manualRouteChoice(
                            selectedMode,
                            recommendedRouteIndex.coerceIn(
                                0,
                                realRouteDebugDataList.lastIndex,
                            ),
                            recommendationTollAed,
                        )
                else -> decision?.choice ?: "Enter a route"
            },
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(recGapBetweenSections))

        Text(
            text = "Why",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = when {
                showRouteCardOverrides ->
                    recommendationAlignedCopy?.second
                        ?: manualRouteWhy(
                            selectedMode,
                            recommendedRouteIndex.coerceIn(
                                0,
                                realRouteDebugDataList.lastIndex,
                            ),
                            recommendationTollAed,
                        )
                else ->
                    decision?.why
                        ?: "Add starting point and destination to get a recommendation."
            },
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(recGapBetweenSections))

        Text(
            text = "Tip",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = when {
                showRouteCardOverrides ->
                    recommendationAlignedCopy?.third
                        ?: manualRouteTip(
                            selectedMode,
                            recommendedRouteIndex.coerceIn(
                                0,
                                realRouteDebugDataList.lastIndex,
                            ),
                            recommendationTollAed,
                        )
                else -> decision?.tip ?: "Start with a common UAE route."
            },
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val fromCoords = selectedFromLatLng
        val toCoords = selectedToLatLng
        if (fromCoords != null && toCoords != null) {
            if (realRouteDebugDataList.isNotEmpty()) {
                val debugRoutes = realRouteDebugDataList
                Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 2.dp else 3.dp))
                Text(
                    text = "Available routes: ${debugRoutes.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (index in debugRoutes.indices) {
                    val item = debugRoutes[index]
                    val isUserSelected = index == routeCardSelectionIndex
                    val isRecommended = index == recommendedRouteIndex
                    val scheme = MaterialTheme.colorScheme
                    val outline = scheme.outline
                    val primary = scheme.primary
                    // Plain < selected-only < recommended-only < both (primary = system; outline frame = your pick).
                    val containerAlpha = when {
                        isUserSelected && isRecommended -> 0.89f
                        isRecommended -> 0.705f
                        isUserSelected -> 0.715f
                        else -> 0.37f
                    }
                    val cardBorder = when {
                        isUserSelected && isRecommended ->
                            BorderStroke(
                                width = 2.dp,
                                color = primary.copy(alpha = 0.48f),
                            )
                        isRecommended ->
                            BorderStroke(
                                width = 1.25.dp,
                                color = primary.copy(alpha = 0.40f),
                            )
                        isUserSelected ->
                            BorderStroke(
                                width = 1.25.dp,
                                color = outline.copy(alpha = 0.87f),
                            )
                        else -> null
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRouteIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                scheme.surfaceVariant.copy(alpha = containerAlpha),
                        ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                draggedElevation = 0.dp,
                            ),
                        border = cardBorder,
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 2.dp,
                            ),
                        ) {
                            if (isRecommended) {
                                val chipStyle = MaterialTheme.typography.labelSmall
                                val nuance =
                                    confidenceHintBelowRecommendation(
                                        selectedMode,
                                        directionsStatus,
                                        recommendedRouteIndex,
                                        debugRoutes.size,
                                    )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Recommended",
                                        modifier = Modifier
                                            .background(
                                                color = primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(5.dp),
                                            )
                                            .padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = chipStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = primary.copy(alpha = 0.92f),
                                    )
                                    Text(
                                        text = nuance,
                                        modifier = Modifier
                                            .background(
                                                color = outline.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = chipStyle.copy(fontWeight = FontWeight.Normal),
                                        color =
                                            scheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            if (isUserSelected) {
                                val softenSelectedBadgeWithRecommended = isRecommended
                                Text(
                                    text = "Selected",
                                    modifier = Modifier
                                        .background(
                                            color = outline.copy(
                                                alpha =
                                                    if (softenSelectedBadgeWithRecommended) {
                                                        0.09f
                                                    } else {
                                                        0.135f
                                                    },
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight =
                                            if (softenSelectedBadgeWithRecommended) {
                                                FontWeight.Medium
                                            } else {
                                                FontWeight.SemiBold
                                            },
                                    ),
                                    color =
                                        if (softenSelectedBadgeWithRecommended) {
                                            scheme.onSurfaceVariant.copy(alpha = 0.68f)
                                        } else {
                                            scheme.onSurface.copy(alpha = 0.78f)
                                        },
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                            Text(
                                text = "Route ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = scheme.onSurface.copy(alpha = 0.93f),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.durationText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color =
                                    scheme.onSurface.copy(
                                        alpha = if (isUserSelected) 0.96f else 0.94f,
                                    ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.distanceText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                ),
                                color =
                                    scheme.onSurfaceVariant.copy(alpha = 0.48f),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = run {
                                    val fuelAed =
                                        estimateFuelCostAed(item.distanceMeters / 1000.0)
                                    val totalAed =
                                        estimateTotalRouteCostAed(item.tollAED, fuelAed)
                                    val personality =
                                        tollPhraseForCard(
                                            item,
                                            index,
                                            selectedMode,
                                            recommendedRouteIndex,
                                            debugRoutes,
                                        )
                                    val confidence =
                                        routeConfidenceLabel(directionsStatus, item)
                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color =
                                                    scheme.onSurfaceVariant.copy(
                                                        alpha = 0.62f,
                                                    ),
                                                fontWeight = FontWeight.Normal,
                                            ),
                                        ) {
                                            append("$totalAed AED · ")
                                            append(personality)
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color =
                                                    scheme.onSurfaceVariant.copy(
                                                        alpha = 0.47f,
                                                    ),
                                                fontWeight = FontWeight.Normal,
                                            ),
                                        ) {
                                            append(" · Fuel ")
                                            append(fuelAed.toString())
                                            append(" · ")
                                            append(confidence)
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
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
