package com.clearroad.app

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.domain.RouteIdentityResolver
import com.clearroad.app.legacy.LegacyHomePresentation
import com.clearroad.app.legacy.MarshallRecommendationBanner
import com.clearroad.app.legacy.RouteDecisionEngine
import com.clearroad.app.legacy.buildMarshallRecommendationBannerUiModel
import com.clearroad.app.ui.theme.ClearRoad2Theme
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
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
    val parsedDuration =
        routeJsonForToll?.let { routeJson ->
            val legsIdx = routeJson.indexOf("\"legs\"")
            if (legsIdx == -1) return@let null
            val legsBracket = routeJson.indexOf('[', legsIdx)
            if (legsBracket == -1) return@let null
            val firstLegBrace = routeJson.indexOf('{', legsBracket)
            if (firstLegBrace == -1) return@let null
            val stepsIdx = routeJson.indexOf("\"steps\"", firstLegBrace)
            val legScanEnd = if (stepsIdx == -1) routeJson.length else stepsIdx
            parseLegDurationFields(routeJson, firstLegBrace, legScanEnd)
        }
    val tollPair =
        routeJsonForToll?.let { deriveTollAedFromRouteJson(it) } ?: Pair(0, false)
    val corridorScanText =
        routeJsonForToll?.let(::buildCorridorScanText).orEmpty()
    return RealRouteDebugData(
        distanceText = distanceText,
        durationText = parsedDuration?.selectedDurationText ?: durationText,
        distanceMeters = distanceMeters,
        durationSeconds = parsedDuration?.selectedDurationSeconds ?: durationSeconds,
        tollAED = tollPair.first,
        hasToll = tollPair.second,
        corridorScanText = corridorScanText,
        baseDurationText = parsedDuration?.baseDurationText ?: durationText,
        baseDurationSeconds = parsedDuration?.baseDurationSeconds ?: durationSeconds,
        durationInTrafficText = parsedDuration?.durationInTrafficText,
        durationInTrafficSeconds = parsedDuration?.durationInTrafficSeconds,
    )
}

private const val DIRECTIONS_FETCH_TAG = "DirectionsFetch"

private data class DirectionsFetchResult(
    val raw: String?,
    val status: String?,
    val distanceDuration: Pair<String, String>?,
    val distanceDurationValues: Pair<Int, Int>?,
    val singleRoute: RealRouteDebugData?,
    val routes: List<RealRouteDebugData>,
)

private suspend fun fetchDirectionsForRoute(
    from: LatLng,
    to: LatLng,
): DirectionsFetchResult {
    Log.d(
        DIRECTIONS_FETCH_TAG,
        "Fetching directions ${from.latitude},${from.longitude} -> ${to.latitude},${to.longitude}",
    )
    val raw = fetchDirectionsRaw(buildDirectionsUrl(from, to))
    val distanceDuration = raw?.let { extractFirstLegDistanceDuration(it) }
    val distanceDurationValues = raw?.let { extractFirstLegDistanceDurationValues(it) }
    val singleRoute = buildRealRouteDebugData(
        distanceDuration,
        distanceDurationValues,
        raw?.let(::firstRouteObjectJson),
    )
    val routes = raw?.let { extractRouteLegsDebugData(it) } ?: emptyList()
    val status = raw?.let { extractDirectionsStatus(it) }
    logDirectionsAuditRoutes(status, routes, rawJson = raw)
    if (status == "OK" && routes.isNotEmpty()) {
        logHonestyAuditRoutes(routes)
        logRouteComplexityAudit(routes, rawJson = raw)
        logSaveAedAuditRoutes(routes)
    }
    Log.d(
        DIRECTIONS_FETCH_TAG,
        "Directions fetch complete status=$status routes=${routes.size}",
    )
    return DirectionsFetchResult(
        raw = raw,
        status = status,
        distanceDuration = distanceDuration,
        distanceDurationValues = distanceDurationValues,
        singleRoute = singleRoute,
        routes = routes,
    )
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
    var showRouteInputs by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun applyDirectionsFetchResult(result: DirectionsFetchResult) {
        directionsResponse = result.raw
        directionsStatus = result.status
        directionsDistanceDuration = result.distanceDuration
        directionsDistanceDurationValues = result.distanceDurationValues
        realRouteDebugData = result.singleRoute
        realRouteDebugDataList = result.routes
    }

    suspend fun loadDirections(from: LatLng, to: LatLng) {
        directionsLoading = true
        try {
            applyDirectionsFetchResult(fetchDirectionsForRoute(from, to))
        } finally {
            directionsLoading = false
        }
    }

    /** True only after user taps a route card; cleared when system realigns selection. */
    var userExplicitRouteSelection by remember { mutableStateOf(false) }
    val isRouteReady =
        selectedFromLatLng != null && selectedToLatLng != null
    val routeList = realRouteDebugDataList
    val data = realRouteDebugData
    val legacyDecision =
        if (!ArchitectureValidation.RECOMMENDATION_ONLY_HOME && isRouteReady) {
            RouteDecisionEngine.choose(
                LegacyHomePresentation.routesForDecision(routeList, data),
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
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                realRouteDebugDataList,
                selectedMode,
            )
        }
    LaunchedEffect(realRouteDebugDataList, selectedMode, recommendedRouteIndex, directionsResponse) {
        if (realRouteDebugDataList.isEmpty()) return@LaunchedEffect
        RouteRecommendationSelection.logSmoothAudit(realRouteDebugDataList)
        logSalikScoringProbe(
            routes = realRouteDebugDataList,
            mode = selectedMode,
            recommendedRouteIndex = recommendedRouteIndex,
        )
        logDriverStressAudit(
            routes = realRouteDebugDataList,
            rawJson = directionsResponse,
            mode = selectedMode,
            currentWinnerIndex = recommendedRouteIndex,
        )
    }
    val routeCardSelectionIndex =
        if (showRouteCardOverrides) {
            selectedRouteIndex.coerceIn(0, realRouteDebugDataList.lastIndex)
        } else {
            0
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
        loadDirections(from, to)
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
        HomeDubaiBackground(modifier = Modifier.matchParentSize())
        val routeIdentities =
            if (realRouteDebugDataList.isEmpty()) {
                emptyList()
            } else {
                RouteIdentityResolver.resolveAll(realRouteDebugDataList)
            }
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = if (showRouteCardOverrides) 18.dp else 22.dp),
        ) {
            HomeScreenHeader()
            Spacer(modifier = Modifier.height(HomeSpacingAfterHeader))
            if (!showRouteCardOverrides && !ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
                HomeYunoBubbleSection()
                Spacer(modifier = Modifier.height(6.dp))
            }
            val showRouteInputStep =
                ArchitectureValidation.RECOMMENDATION_ONLY_HOME ||
                    showRouteInputs ||
                    originText.isNotBlank() ||
                    destinationText.isNotBlank()
            if (!showRouteInputStep) {
                HomeSearchCapsule(onClick = { showRouteInputs = true })
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                HomeRouteInputGroup(
                    fromValue = originText,
                    onFromValueChange = stopFrom@{ newText ->
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
                    fromPlaceholder = "Enter starting point",
                    toValue = destinationText,
                    onToValueChange = stopTo@{ newText ->
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
                    toPlaceholder = "Enter destination",
                    fromPredictions = {
                        if (fromPredictions.isNotEmpty() &&
                            originText.length >= 2 &&
                            originText.isNotBlank()
                        ) {
                            Column(modifier = Modifier.padding(top = 6.dp, start = 28.dp)) {
                                fromPredictions.take(5).forEach { prediction ->
                                    val label = prediction.getFullText(null).toString()
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                originText = label
                                                fromPredictions = emptyList()
                                                selectedFromPlaceId = prediction.placeId
                                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                                    selectedFromLatLng = result
                                                }
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        style =
                                            homeRouteSkyReadableTextStyle(
                                                MaterialTheme.typography.bodySmall,
                                            ),
                                    )
                                }
                            }
                        }
                    },
                    toPredictions = {
                        if (toPredictions.isNotEmpty() &&
                            destinationText.length >= 2 &&
                            destinationText.isNotBlank()
                        ) {
                            Column(modifier = Modifier.padding(top = 6.dp, start = 28.dp)) {
                                toPredictions.take(5).forEach { prediction ->
                                    val label = prediction.getFullText(null).toString()
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                destinationText = label
                                                toPredictions = emptyList()
                                                selectedToPlaceId = prediction.placeId
                                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                                    selectedToLatLng = result
                                                }
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        style =
                                            homeRouteSkyReadableTextStyle(
                                                MaterialTheme.typography.bodySmall,
                                            ),
                                    )
                                }
                            }
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(HomeSpacingAfterRouteInput))
            HomeGlassModeTabs(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
            )
            Spacer(modifier = Modifier.height(HomeSpacingBeforeRecommendation))
            val fromCoords = selectedFromLatLng
            val toCoords = selectedToLatLng
            val recommendationLoading =
                directionsLoading && fromCoords != null && toCoords != null
            val recommendationLoadingMessage =
                when (selectedMode) {
                    PreferenceMode.FASTEST -> "Finding the best FASTEST route..."
                    PreferenceMode.NO_TOLLS -> "Finding the best SAVE AED route..."
                    PreferenceMode.CALM -> "Finding the best SMOOTH DRIVE route..."
                }
            val recommendedRouteIdentityPrimary =
                if (showRouteCardOverrides && routeIdentities.isNotEmpty()) {
                    val recIdx =
                        recommendedRouteIndex.coerceIn(0, routeIdentities.lastIndex)
                    RouteIdentityPresentationPolicy.displayForHome(
                        routes = realRouteDebugDataList,
                        identities = routeIdentities,
                        recommendedIndex = recIdx,
                    ).homeLine
                } else {
                    ""
                }
            val openRecommendedViewDetails: (() -> Unit)? =
                if (
                    showRouteCardOverrides &&
                        realRouteDebugDataList.isNotEmpty()
                ) {
                    {
                        detailsRouteIndex =
                            recommendedRouteIndex.coerceIn(
                                0,
                                realRouteDebugDataList.lastIndex,
                            )
                    }
                } else {
                    null
                }
            val onRefreshRoute: (() -> Unit)? =
                if (showRouteCardOverrides && realRouteDebugDataList.isNotEmpty()) {
                    {
                        val from = selectedFromLatLng
                        val to = selectedToLatLng
                        if (from != null && to != null) {
                            scope.launch { loadDirections(from, to) }
                        }
                    }
                } else {
                    null
                }
            if (ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
                RecommendationSurface(
                    model = buildRecommendationSurfaceUiModel(
                        ready = showRouteCardOverrides,
                        loading = recommendationLoading,
                        loadingMessage = recommendationLoadingMessage,
                        routes = realRouteDebugDataList,
                        recommendedRouteIndex = recommendedRouteIndex,
                        routeIdentity = recommendedRouteIdentityPrimary,
                        routeIdentities = routeIdentities,
                        mode = selectedMode,
                    ),
                    onViewDetails = openRecommendedViewDetails,
                    onRefreshRoute = onRefreshRoute,
                )
            } else {
                MarshallRecommendationBanner(
                    model = buildMarshallRecommendationBannerUiModel(
                        ready = isRouteReady || showRouteCardOverrides,
                        routes = realRouteDebugDataList,
                        recommendedRouteIndex = recommendedRouteIndex,
                        routeIdentity = recommendedRouteIdentityPrimary,
                        whyText =
                            LegacyHomePresentation.selectedDecisionWhy(
                                showRouteCardOverrides = showRouteCardOverrides,
                                routes = realRouteDebugDataList,
                                recommendedRouteIndex = recommendedRouteIndex,
                                mode = selectedMode,
                                decisionWhy = legacyDecision?.why,
                            ),
                    ),
                    onViewDetails = null,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (fromCoords != null && toCoords != null) {
            if (directionsLoading && !ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (selectedMode) {
                        PreferenceMode.FASTEST -> "Finding the best FASTEST route..."
                        PreferenceMode.NO_TOLLS ->
                            "Finding the best SAVE AED route..."
                        PreferenceMode.CALM -> "Finding the best SMOOTH DRIVE route..."
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
                if (!ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
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
                            LegacyHomePresentation.confidenceHintBelowRecommendation(
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
                            confidence = RouteDetailsAssembly.confidenceLabel(directionsStatus, item),
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
                                width = 1.5.dp,
                                color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.92f),
                            )
                        isRecommended ->
                            BorderStroke(
                                width = 1.5.dp,
                                color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.88f),
                            )
                        isUserSelected ->
                            BorderStroke(
                                width = 1.dp,
                                color = ClearRoadColors.SalikNeutral.copy(alpha = 0.22f),
                            )
                        else ->
                            BorderStroke(
                                width = 1.dp,
                                color = modeAccent.copy(alpha = 0.10f),
                            )
                    }
                    Spacer(modifier = Modifier.height(if (isRecommended) 4.dp else 4.dp))
                    HomeGlassSurface(
                        modifier = Modifier
                            .then(
                                if (showUserSelectedChrome) {
                                    Modifier.bringIntoViewRequester(
                                        bringIntoViewRequester,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        recommended = isRecommended,
                        border = cardBorder,
                        onClick = {
                            userExplicitRouteSelection = true
                            selectedRouteIndex = index
                        },
                    ) {
                        DecisionFirstRouteCardContent(
                            cardModel = cardModel,
                            personality = personality,
                            modeAccent = modeAccent,
                            showUserSelectedChrome = showUserSelectedChrome,
                            onViewDetails = { detailsRouteIndex = index },
                        )
                    }
                }
                }
                val lastRouteSelected =
                    !ArchitectureValidation.RECOMMENDATION_ONLY_HOME &&
                        userExplicitRouteSelection &&
                        debugRoutes.isNotEmpty() &&
                        routeCardSelectionIndex == debugRoutes.lastIndex
                if (!ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(
                                if (lastRouteSelected) 144.dp else 72.dp,
                            ),
                    )
                }
            }
        }
        if (ArchitectureValidation.RECOMMENDATION_ONLY_HOME) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(72.dp),
            )
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
                    RouteDetailsScreen(
                        model =
                            RouteDetailsAssembly.buildUiModel(
                                RouteDetailsAssembly.Input(
                                    detailRouteIndex = detailIdx,
                                    detailRoute = detailItem,
                                    routes = realRouteDebugDataList,
                                    identities = routeIdentities,
                                    mode = selectedMode,
                                    recommendedRouteIndex = recommendedRouteIndex,
                                    directionsStatus = directionsStatus,
                                    fromLatLng = selectedFromLatLng,
                                    toLatLng = selectedToLatLng,
                                    useLegacyHomeFallback =
                                        !ArchitectureValidation.RECOMMENDATION_ONLY_HOME,
                                ),
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
