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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteDecisionEngine
import com.clearroad.app.domain.RouteOption
import com.clearroad.app.domain.RouteReasoning
import com.clearroad.app.domain.RouteReasoningContext
import com.clearroad.app.ui.theme.ClearRoad2Theme
import com.clearroad.app.ui.theme.ClearRoadShellBackground
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.Locale
import kotlinx.coroutines.delay

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
                ClearRoadShellBackground {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = ClearRoadColors.CloudBackground,
                    ) { innerPadding ->
                        ClearRoadScreen(
                            modifier = Modifier.padding(innerPadding),
                            placesClient = placesClient,
                        )
                    }
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

/** Static nuance label beside Recommended — wording from route metrics, not scoring. */
private fun confidenceHintBelowRecommendation(
    mode: RouteMode,
    directionsStatus: String?,
    recommendedRouteIndex: Int,
    routes: List<RealRouteDebugData>,
): String =
    recommendationExplanationText(
        mode = mode,
        directionsStatus = directionsStatus,
        routes = routes,
        recommendedRouteIndex = recommendedRouteIndex,
        compact = true,
    )

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
    RouteReasoning.alignedExplanation(personality, mode)?.let {
        Triple(it.choice, it.why, it.tip)
    }

private fun manualRouteChoice(
    mode: PreferenceMode,
    routeIndex: Int,
    tollAED: Int? = null,
): String {
    val level = tollAED?.let(::getTollLevel)
    return when (mode) {
        PreferenceMode.FASTEST -> when (routeIndex) {
            0 -> "Best pace among these"
            1 -> "Almost as fast — slightly longer"
            else -> "Longer drive"
        }
        PreferenceMode.NO_TOLLS -> when {
            level == "none" -> "Lightest Salik pick here"
            level == "low" -> "Light toll route"
            else -> when (routeIndex) {
                0 -> "Easiest on tolls"
                1 -> "Moderate toll route"
                else -> "Higher toll route"
            }
        }
        PreferenceMode.CALM -> "Steadier run among these"
    }
}

private fun manualRouteWhy(
    mode: PreferenceMode,
    routeIndex: Int,
    tollAED: Int? = null,
): String =
    RouteReasoning.manualWhy(
        RouteReasoningContext(
            mode = mode,
            routeIndex = routeIndex,
            tollAed = tollAED,
        ),
    )

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
    if (item.durationSeconds <= 0 || item.distanceMeters <= 0) return "Light read"
    if (directionsStatus != "OK") return "Light read"
    val tollKnownFromFare = item.hasToll || item.tollAED > 0
    return if (tollKnownFromFare) "Steady" else "Typical"
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

private fun costSummaryLines(
    mode: PreferenceMode,
    tollAed: Int,
): Pair<String, String?> =
    RouteReasoning.tripAtAGlanceLines(mode, tollAed)

private data class DecisionSnapshotLines(
    val recommendedHeading: String,
    val recommendedSummary: String,
    val othersHeading: String,
    val othersSummary: String,
)

private fun decisionSnapshotLines(
    mode: PreferenceMode,
    recommendedRouteIndex: Int,
): DecisionSnapshotLines =
    when (mode) {
        PreferenceMode.FASTEST ->
            DecisionSnapshotLines(
                recommendedHeading =
                    "Route ${recommendedRouteIndex + 1} (recommended)",
                recommendedSummary = "Fastest arrival with low extra cost.",
                othersHeading = "Other routes",
                othersSummary = "Slightly slower without a clear gain.",
            )
        PreferenceMode.NO_TOLLS ->
            DecisionSnapshotLines(
                recommendedHeading = "Recommended route",
                recommendedSummary = "Lowest Salik impact on this trip.",
                othersHeading = "Other routes",
                othersSummary = "Higher toll spending than recommended.",
            )
        PreferenceMode.CALM ->
            DecisionSnapshotLines(
                recommendedHeading = "Recommended route",
                recommendedSummary = "Smoother drive with a small time tradeoff.",
                othersHeading = "Other routes",
                othersSummary = "More movement, less comfort focus.",
            )
    }

private fun isHighConfidenceRecommendation(
    mode: PreferenceMode,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
): Boolean {
    if (routes.isEmpty()) return false
    val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
    return when (mode) {
        PreferenceMode.FASTEST -> {
            val recommendedDuration = routes[recIdx].durationSeconds
            val nextFastestDuration =
                routes.indices
                    .filter { it != recIdx }
                    .minOfOrNull { routes[it].durationSeconds }
            nextFastestDuration != null &&
                recommendedDuration + 120 <= nextFastestDuration
        }
        PreferenceMode.NO_TOLLS -> {
            val recommendedToll = routes[recIdx].tollAED
            routes.indices
                .filter { it != recIdx }
                .all { routes[it].tollAED > recommendedToll }
        }
        PreferenceMode.CALM -> false
    }
}

private fun recommendationExplanationText(
    mode: PreferenceMode,
    directionsStatus: String?,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
    compact: Boolean,
): String {
    if (directionsStatus != "OK" || routes.isEmpty()) {
        return when (mode) {
            PreferenceMode.FASTEST -> "Timing unclear until routes load."
            PreferenceMode.NO_TOLLS -> "Salik cost unclear until routes load."
            PreferenceMode.CALM -> "Pace unclear until routes load."
        }
    }
    val recIdx = recommendedRouteIndex.coerceIn(0, routes.lastIndex)
    val recommended = routes[recIdx]
    val highConfidence =
        isHighConfidenceRecommendation(mode, routes, recIdx)
    val nextAlternativeDurationSeconds =
        routes.indices
            .filter { it != recIdx }
            .minOfOrNull { routes[it].durationSeconds }
    val fastestDurationSeconds = routes.minOf { it.durationSeconds }
    return RouteReasoning.humanRecommendationExplanation(
        mode = mode,
        recommendedDurationSeconds = recommended.durationSeconds,
        recommendedTollAed = recommended.tollAED,
        nextAlternativeDurationSeconds = nextAlternativeDurationSeconds,
        fastestDurationSeconds = fastestDurationSeconds,
        highConfidence = highConfidence,
        compact = compact,
    )
}

private fun confidenceLines(
    mode: PreferenceMode,
    directionsStatus: String?,
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int,
): String =
    recommendationExplanationText(
        mode = mode,
        directionsStatus = directionsStatus,
        routes = routes,
        recommendedRouteIndex = recommendedRouteIndex,
        compact = false,
    )

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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = 24.dp,
                    top = if (showRouteCardOverrides) 12.dp else 16.dp,
                    end = 24.dp,
                )
                .navigationBarsPadding(),
        ) {
        Text(
            text = "Clear Road",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = ClearRoadColors.RoadGrey,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Route decision assistant",
            style = MaterialTheme.typography.bodyMedium,
            color = ClearRoadColors.RoadGreyMuted,
        )
        Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 20.dp else 32.dp))

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
        Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 12.dp else 16.dp))
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
        Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 16.dp else 24.dp))

        ModeTabs(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            tabVerticalPadding = if (showRouteCardOverrides) 8.dp else 11.dp,
        )
        Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 8.dp else 14.dp))

        val selectedDecisionTitle =
            when {
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
            }
        val selectedDecisionWhy =
            when {
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
            }
        val selectedDecisionTip =
            when {
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
            }
        ChoiceWhyTipBlock(
            model = choiceWhyTipUiModel(
                choice = selectedDecisionTitle,
                why = selectedDecisionWhy,
                tip = selectedDecisionTip,
                compact = showRouteCardOverrides,
            ),
        )
        Spacer(modifier = Modifier.height(if (showRouteCardOverrides) 2.dp else 6.dp))
        val fromCoords = selectedFromLatLng
        val toCoords = selectedToLatLng
        if (fromCoords != null && toCoords != null) {
            if (directionsLoading) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (selectedMode) {
                        PreferenceMode.FASTEST -> "Checking route options..."
                        PreferenceMode.NO_TOLLS ->
                            "Looking for the best route balance..."
                        PreferenceMode.CALM -> "Finding calmer route choices..."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.62f,
                        ),
                )
            } else if (realRouteDebugDataList.isNotEmpty()) {
                val debugRoutes = realRouteDebugDataList
                Spacer(modifier = Modifier.height(2.dp))
                AvailableRoutesHeader(routeCount = realRouteDebugDataList.size)
                for (index in debugRoutes.indices) {
                    val item = debugRoutes[index]
                    val personality =
                        tollPhraseForCard(
                            item,
                            index,
                            selectedMode,
                            recommendedRouteIndex,
                            debugRoutes,
                        )
                    val recommendedNuance =
                        if (index == recommendedRouteIndex) {
                            confidenceHintBelowRecommendation(
                                selectedMode,
                                directionsStatus,
                                recommendedRouteIndex,
                                debugRoutes,
                            )
                        } else {
                            null
                        }
                    val cardModel =
                        buildRouteCardUiModel(
                            routeIndex = index,
                            item = item,
                            selectedMode = selectedMode,
                            routes = debugRoutes,
                            recommendedRouteIndex = recommendedRouteIndex,
                            routeCardSelectionIndex = routeCardSelectionIndex,
                            userExplicitRouteSelection = userExplicitRouteSelection,
                            salikLine = salikMetaText(item.tollAED, personality),
                            confidence = routeConfidenceLabel(directionsStatus, item),
                            recommendedNuance = recommendedNuance,
                        )
                    val isUserSelected = cardModel.isUserSelected
                    val showUserSelectedChrome = cardModel.showUserSelectedChrome
                    val isRecommended = cardModel.isRecommended
                    val bringIntoViewRequester = remember(index) {
                        BringIntoViewRequester()
                    }
                    LaunchedEffect(showUserSelectedChrome) {
                        if (showUserSelectedChrome) {
                            delay(64)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                    val modeAccent = cardModel.mode.accentColor()
                    val cardBorder = when {
                        isUserSelected && isRecommended ->
                            BorderStroke(
                                width = 2.dp,
                                color = modeAccent.copy(alpha = 0.55f),
                            )
                        isRecommended ->
                            BorderStroke(
                                width = 1.5.dp,
                                color = modeAccent.copy(alpha = 0.45f),
                            )
                        isUserSelected ->
                            BorderStroke(
                                width = 1.dp,
                                color = ClearRoadColors.SalikNeutral.copy(alpha = 0.35f),
                            )
                        else ->
                            BorderStroke(
                                width = 1.dp,
                                color = modeAccent.copy(alpha = 0.20f),
                            )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (showUserSelectedChrome) {
                                    Modifier.bringIntoViewRequester(
                                        bringIntoViewRequester,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                userExplicitRouteSelection = true
                                selectedRouteIndex = index
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ClearRoadColors.RouteCardSurface,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = when {
                                        showUserSelectedChrome -> 12.dp
                                        isRecommended -> 11.dp
                                        else -> 10.dp
                                    },
                                ),
                        ) {
                            val cardLineGap = 4.dp
                            val metricsLineGap = 2.dp
                            if (isRecommended && cardModel.recommendedNuance != null) {
                                RecommendedBadgeBlock(
                                    nuance = cardModel.recommendedNuance,
                                    accentColor = modeAccent,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RouteTitleBlock(
                                    personality = "",
                                    title = cardModel.routeTitle,
                                    titleAlpha = 0.93f,
                                    modifier = Modifier.weight(1f),
                                )
                                if (showUserSelectedChrome) {
                                    Text(
                                        text = "View details →",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                modeAccent.copy(alpha = 0.10f),
                                            )
                                            .clickable {
                                                detailsRouteIndex = index
                                            }
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = 4.dp,
                                            ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = modeAccent.copy(alpha = 0.92f),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(cardLineGap))
                            RouteMetricsBlock(
                                durationText = cardModel.durationText,
                                distanceText = cardModel.distanceText,
                                durationOnSurfaceAlpha = cardModel.durationOnSurfaceAlpha,
                            )
                            Spacer(modifier = Modifier.height(cardLineGap))
                            WhyTagRow(
                                tags = cardModel.whyTags,
                                accentColor = modeAccent,
                            )
                            Spacer(modifier = Modifier.height(cardLineGap))
                            RouteMetaLine(
                                tollText = cardModel.salikLine,
                            )
                            Spacer(modifier = Modifier.height(metricsLineGap))
                            RouteFuelConfidenceLine(
                                text = cardModel.confidence,
                            )
                            if (showUserSelectedChrome) {
                                Spacer(modifier = Modifier.height(1.dp))
                                SelectedLabelBlock(
                                    labelAlpha =
                                        if (isRecommended) 0.46f else 0.54f,
                                )
                            }
                        }
                    }
                }
                val lastRouteSelected =
                    userExplicitRouteSelection &&
                        debugRoutes.isNotEmpty() &&
                        routeCardSelectionIndex == debugRoutes.lastIndex
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(if (lastRouteSelected) 144.dp else 72.dp),
                )
            }
        }
    }

        detailsRouteIndex?.let { detailIdx ->
            val detailItem = realRouteDebugDataList.getOrNull(detailIdx)
            if (detailItem != null) {
                ModalBottomSheet(
                    onDismissRequest = { detailsRouteIndex = null },
                    containerColor = ClearRoadColors.RouteCardSurface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ) {
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
                    val (costSummaryPrimary, costSummarySecondary) =
                        costSummaryLines(
                            selectedMode,
                            detailItem.tollAED,
                        )
                    val decisionSnapshot =
                        decisionSnapshotLines(
                            selectedMode,
                            recommendedRouteIndex.coerceIn(
                                0,
                                realRouteDebugDataList.lastIndex,
                            ),
                        )
                    val recIdxForConfidence =
                        recommendedRouteIndex.coerceIn(
                            0,
                            realRouteDebugDataList.lastIndex,
                        )
                    val isRecommendedRouteDetails = detailIdx == recIdxForConfidence
                    val recommendationConfidenceText =
                        if (isRecommendedRouteDetails) {
                            confidenceLines(
                                selectedMode,
                                directionsStatus,
                                realRouteDebugDataList,
                                recIdxForConfidence,
                            )
                        } else {
                            ""
                        }
                    val isHighConfidence =
                        if (isRecommendedRouteDetails) {
                            isHighConfidenceRecommendation(
                                selectedMode,
                                realRouteDebugDataList,
                                recIdxForConfidence,
                            )
                        } else {
                            false
                        }
                    val recommendationTradeoffText =
                        if (isRecommendedRouteDetails && directionsStatus == "OK") {
                            RouteReasoning.routeTradeoffExplanation(selectedMode)
                        } else {
                            null
                        }
                    RouteDetailsScreen(
                        model = buildRouteDetailsUiModel(
                            routeIndex = detailIdx,
                            routeNumber = detailIdx + 1,
                            routeReasonTitle = routeReasonTitle,
                            routeReasonWhy = routeReasonWhy,
                            item = detailItem,
                            selectedMode = selectedMode,
                            routes = realRouteDebugDataList,
                            confidenceLabel = routeConfidenceLabel(
                                directionsStatus,
                                detailItem,
                            ),
                            costSummaryPrimary = costSummaryPrimary,
                            costSummarySecondary = costSummarySecondary,
                            decisionSnapshotRecommendedHeading =
                                decisionSnapshot.recommendedHeading,
                            decisionSnapshotRecommendedSummary =
                                decisionSnapshot.recommendedSummary,
                            decisionSnapshotOthersHeading =
                                decisionSnapshot.othersHeading,
                            decisionSnapshotOthersSummary =
                                decisionSnapshot.othersSummary,
                            recommendationConfidenceText = recommendationConfidenceText,
                            recommendationTradeoffText = recommendationTradeoffText,
                            isHighConfidence = isHighConfidence,
                            fromLatLng = selectedFromLatLng,
                            toLatLng = selectedToLatLng,
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
