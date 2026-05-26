package com.clearroad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
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
            PreferenceMode.NO_TOLLS -> "Salik cost unclear"
            PreferenceMode.CALM -> "Pace unclear"
        }
    }
    val slot =
        ((recommendedRouteIndex.coerceAtLeast(0) + routeCount.coerceAtLeast(1)) % 3)
    return when (mode) {
        PreferenceMode.FASTEST ->
            when (slot) {
                0 -> "Traffic shifts quickly"
                1 -> "Steady flow"
                else -> "Peak-hour sensitive"
            }
        PreferenceMode.NO_TOLLS ->
            when (slot) {
                0 -> "Predictable Salik spend"
                1 -> "Lower toll exposure"
                else -> "Budget-friendly route"
            }
        PreferenceMode.CALM ->
            when (slot) {
                0 -> "Smoother city flow"
                1 -> "Gentler traffic merge"
                else -> "Easier city entry"
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
            PreferenceMode.NO_TOLLS -> "Salik-saving leg"
            else -> "Smoother city approach"
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
                    "Faster urban stretch"
                isFastestTimeRoute -> "Dubai corridor"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    "More Salik ahead"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD ->
                    "Steadier corridor leg"
                durationStretchVsFastest > 0.12f -> "Easier traffic stretch"
                tollBand == "none" -> "Fast city run"
                tollBand == "low" -> "Main motorway stretch"
                routeIndex % 2 == 0 -> "Main motorway stretch"
                else -> "More Salik ahead"
            }
        PreferenceMode.NO_TOLLS ->
            when (corridorHint) {
                UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    if (item.tollAED >= fastest.tollAED) {
                        "Higher toll pick"
                    } else {
                        "More Salik ahead"
                    }
                UaeCorridorTollHint.LOW_LIKELIHOOD -> "Lower Salik route"
                UaeCorridorTollHint.NEUTRAL ->
                    when (tollBand) {
                        "none" -> "Budget-friendly drive"
                        "low" -> "Salik-saving leg"
                        else ->
                            if (routeIndex % 2 == 0) {
                                "Main motorway stretch"
                            } else {
                                "More Salik ahead"
                            }
                    }
            }
        PreferenceMode.CALM ->
            when {
                durationStretchVsFastest > 0.1f -> "Smoother UAE leg"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD -> "Lower Salik route"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD -> "More Salik ahead"
                else -> "Smoother city approach"
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
                "Least Salik spend on this list.",
                "Good when Salik needs to stay predictable.",
            )
        "Faster urban stretch" ->
            Triple(
                "Faster urban stretch",
                "Fastest line — expect more Salik.",
                "Top up Salik before you roll.",
            )
        "Dubai corridor" ->
            Triple(
                "Dubai corridor",
                "Fast city cut through town.",
                "When minutes matter most.",
            )
        "Smoother city approach" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "Smoother city approach",
                    "Gentler merge — UAE traffic pace.",
                    "Fine when extra minutes buy calm.",
                )
            } else {
                Triple(
                    "Smoother city approach",
                    "Slightly longer — lighter Salik than the quickest cut.",
                    "Glance at fuel and Salik before you go.",
                )
            }
        "Steadier corridor leg" ->
            Triple(
                "Steadier corridor leg",
                "Salik stays lighter than the fastest pick.",
                "When you want pace without heavy gates.",
            )
        "Easier traffic stretch" ->
            Triple(
                "Easier traffic stretch",
                "Few extra minutes — steadier flow.",
                "When you are not chasing every minute.",
            )
        "Smoother UAE leg" ->
            Triple(
                "Smoother UAE leg",
                "Runs longer — feels less rushed.",
                "When calm beats rushing.",
            )
        "More Salik ahead" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "More Salik ahead",
                    "Busy UAE stretch — Salik adds up.",
                    "Keep your tag topped.",
                )
            } else {
                Triple(
                    "More Salik ahead",
                    "More Salik gates along this line.",
                    "Check Salik before you head out.",
                )
            }
        "Salik-saving leg" ->
            Triple(
                "Salik-saving leg",
                "Longer — lighter Salik than the fast cuts.",
                "When savings beat shaving minutes.",
            )
        "Fast city run" ->
            Triple(
                "Fast city run",
                "Fairly direct shot into town.",
                "Glance at traffic if time is tight.",
            )
        "Main motorway stretch" ->
            Triple(
                "Main motorway stretch",
                "Typical UAE motorway rhythm.",
                "Pad extra time at rush hour.",
            )
        "Higher toll pick" ->
            Triple(
                "Higher toll pick",
                "More Salik than the lighter picks here.",
                "When time matters more than cost.",
            )
        "Lower Salik route" ->
            if (mode == PreferenceMode.CALM) {
                Triple(
                    "Lower Salik route",
                    "Skips heavy Salik where it can.",
                    "Still glance at exits before you move.",
                )
            } else {
                Triple(
                    "Lower Salik route",
                    "Likely avoids heavier Salik stretches.",
                    "Still check exits on your map.",
                )
            }
        "Budget-friendly drive" ->
            Triple(
                "Budget-friendly drive",
                "Keeps Salik light between these picks.",
                "Fine for everyday UAE runs.",
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
            level == "none" -> "Lowest Salik option"
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
                "Lowest Salik among these routes."
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
            "Handy when traffic already feels like enough."
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var detailsRouteIndex by remember { mutableStateOf<Int?>(null) }
    /** True only after user taps a route card; cleared when system realigns selection. */
    var userExplicitRouteSelection by remember { mutableStateOf(false) }
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
        userExplicitRouteSelection = false
        selectedRouteIndex =
            recommendedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
    }
    LaunchedEffect(selectedFromLatLng, selectedToLatLng) {
        detailsRouteIndex = null
        userExplicitRouteSelection = false
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
    LaunchedEffect(realRouteDebugDataList) {
        val idx = detailsRouteIndex ?: return@LaunchedEffect
        if (
            realRouteDebugDataList.isEmpty() ||
            idx !in realRouteDebugDataList.indices
        ) {
            detailsRouteIndex = null
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
        Spacer(modifier = Modifier.height(14.dp))

        val recommendationCompact = showRouteCardOverrides
        val recLabelStyle =
            if (recommendationCompact) {
                MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.35.sp,
                )
            } else {
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.35.sp,
                )
            }
        val recBodyStyle =
            if (recommendationCompact) {
                MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
            } else {
                MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
            }
        val recGapLabelToBody = if (recommendationCompact) 2.dp else 3.dp
        val recGapBetweenSections = if (recommendationCompact) 7.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = "Choice",
                style = recLabelStyle,
                color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary,
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
        }
        Spacer(modifier = Modifier.height(6.dp))
        val fromCoords = selectedFromLatLng
        val toCoords = selectedToLatLng
        if (fromCoords != null && toCoords != null) {
            if (realRouteDebugDataList.isNotEmpty()) {
                val debugRoutes = realRouteDebugDataList
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Available routes: ${debugRoutes.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (index in debugRoutes.indices) {
                    val item = debugRoutes[index]
                    val isUserSelected = index == routeCardSelectionIndex
                    val showUserSelectedChrome =
                        isUserSelected && userExplicitRouteSelection
                    val isRecommended = index == recommendedRouteIndex
                    val scheme = MaterialTheme.colorScheme
                    val outline = scheme.outline
                    val primary = scheme.primary
                    // Recommended (system) > Selected (user) > plain — outlines and fill follow that order.
                    val containerAlpha = when {
                        isUserSelected && isRecommended -> 0.89f
                        isRecommended -> 0.745f
                        isUserSelected -> 0.52f
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
                                width = 1.35.dp,
                                color = primary.copy(alpha = 0.44f),
                            )
                        isUserSelected ->
                            BorderStroke(
                                width = 1.1.dp,
                                color = outline.copy(alpha = 0.53f),
                            )
                        else -> null
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                userExplicitRouteSelection = true
                                selectedRouteIndex = index
                            },
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
                                                color = primary.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(5.dp),
                                            )
                                            .padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = chipStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = primary.copy(alpha = 0.94f),
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
                            if (showUserSelectedChrome) {
                                val softenSelectedBadgeWithRecommended = isRecommended
                                Text(
                                    text = "Selected",
                                    modifier = Modifier
                                        .background(
                                            color = outline.copy(
                                                alpha =
                                                    if (softenSelectedBadgeWithRecommended) {
                                                        0.085f
                                                    } else {
                                                        0.12f
                                                    },
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color =
                                        if (softenSelectedBadgeWithRecommended) {
                                            scheme.onSurfaceVariant.copy(alpha = 0.68f)
                                        } else {
                                            scheme.onSurfaceVariant.copy(alpha = 0.78f)
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
                                        alpha = when {
                                            isRecommended -> 0.97f
                                            isUserSelected -> 0.90f
                                            else -> 0.94f
                                        },
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
                            if (showUserSelectedChrome) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "View details →",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            outline.copy(alpha = 0.11f),
                                        )
                                        .clickable {
                                            detailsRouteIndex = index
                                        }
                                        .padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp,
                                        ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = primary.copy(alpha = 0.92f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        detailsRouteIndex?.let { detailIdx ->
            val detailItem = realRouteDebugDataList.getOrNull(detailIdx)
            if (detailItem != null) {
                ModalBottomSheet(
                    onDismissRequest = { detailsRouteIndex = null },
                ) {
                    val fuelForDetails =
                        estimateFuelCostAed(detailItem.distanceMeters / 1000.0)
                    val detailPersonality = tollPhraseForCard(
                        detailItem,
                        detailIdx,
                        selectedMode,
                        recommendedRouteIndex,
                        realRouteDebugDataList,
                    )
                    val detailAligned =
                        recommendationAlignedExplanation(detailPersonality, selectedMode)
                    val routeReasonTitle =
                        detailAligned?.first ?: detailPersonality
                    val routeReasonWhy =
                        detailAligned?.second.orEmpty()
                    RouteDetailsScreen(
                        routeNumber = detailIdx + 1,
                        routeReasonTitle = routeReasonTitle,
                        routeReasonWhy = routeReasonWhy,
                        durationText = detailItem.durationText,
                        distanceText = detailItem.distanceText,
                        fuelCostAed = fuelForDetails,
                        tollAed = detailItem.tollAED,
                        confidenceLabel = routeConfidenceLabel(
                            directionsStatus,
                            detailItem,
                        ),
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
