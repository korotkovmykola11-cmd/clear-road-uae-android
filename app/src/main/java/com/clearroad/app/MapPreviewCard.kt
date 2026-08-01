package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.map.rememberMarshioMapProperties
import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RouteGoogleMarshioDecisionUiModel
import com.clearroad.app.ui.model.RouteMapEvidenceUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val MapPreviewHeight = 272.dp
private val SharedRoutePolylineColor = Color(0xFFBDBDBD)
private val SharedRoutePolylineWidth = 14f
private val StrategicMapBoundsPaddingPx = 112
private val LocalMapBoundsPaddingPx = 72

internal enum class RoutePreviewCameraPolicy {
    AlwaysFitOnDataChange,
    InitialFitOnce,
}

private const val RoutePreviewLegend =
    "Indigo = MARSHIO selected route · Cyan dashed = Google alternatives"
private const val ForkFallbackMessage = "Routes differ on this trip."

internal const val MarshioGoogleValidationLine =
    "MARSHIO checked Google's route — same pick."

internal fun routePreviewDecisionCaption(
    decision: RouteGoogleMarshioDecisionUiModel?,
): String =
    when (decision?.state) {
        GoogleMarshioDecisionState.AGREES,
        GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
        -> MarshioGoogleValidationLine
        GoogleMarshioDecisionState.DISAGREES ->
            "MARSHIO chose a different route than Google."
        null -> "MARSHIO route preview."
    }

internal fun routePreviewShowsAlternativesLegend(alternativePathCount: Int): Boolean =
    alternativePathCount > 0

internal fun hasLocalForkMapEvidence(mapEvidence: RouteMapEvidenceUiModel): Boolean =
    mapEvidence.splitPoint != null &&
        mapEvidence.googleDivergentPath.size >= 2 &&
        mapEvidence.marshioDivergentPath.size >= 2

internal fun routePreviewForkFallbackVisible(
    mapEvidence: RouteMapEvidenceUiModel?,
): Boolean =
    mapEvidence?.enabled == true && !hasLocalForkMapEvidence(mapEvidence)

@Composable
internal fun MapPreviewCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    routePathPoints: List<LatLng> = emptyList(),
    trafficSegments: List<TrafficSegment> = emptyList(),
    junctionAnnotations: List<RouteJunctionAnnotation> = emptyList(),
    otherRoutePathPoints: List<List<LatLng>> = emptyList(),
    routeOptionsCount: Int = 1,
    googleMarshioDecision: RouteGoogleMarshioDecisionUiModel? = null,
    mapEvidence: RouteMapEvidenceUiModel? = null,
    cardBorder: BorderStroke,
    modifier: Modifier = Modifier,
    mapPreviewHeight: Dp = MapPreviewHeight,
    onStudyRouteClick: (() -> Unit)? = null,
) {
    val alternativePaths = otherRoutePathPoints.filter { it.isNotEmpty() }
    val decisionCaption = routePreviewDecisionCaption(googleMarshioDecision)
    val strategicCaption =
        if (googleMarshioDecision?.state == GoogleMarshioDecisionState.DISAGREES) {
            mapEvidence?.strategicCaption?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    val showLegend = routePreviewShowsAlternativesLegend(alternativePaths.size)
    val preMapNote =
        if (routePreviewForkFallbackVisible(mapEvidence)) ForkFallbackMessage else null

    Column(modifier = modifier.fillMaxWidth()) {
        SingleRouteMapPreviewCard(
            fromLatLng = fromLatLng,
            toLatLng = toLatLng,
            routePathPoints = routePathPoints,
            trafficSegments = trafficSegments,
            junctionAnnotations = junctionAnnotations,
            alternativePaths = alternativePaths,
            decisionCaption = decisionCaption,
            strategicCaption = strategicCaption,
            preMapNote = preMapNote,
            showLegend = showLegend,
            cardBorder = cardBorder,
            mapPreviewHeight = mapPreviewHeight,
        )
        if (onStudyRouteClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStudyRouteClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = cardBorder,
            ) {
                Text(
                    text = "Explore route",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = ClearRoadColors.RoadGrey,
                )
            }
        }
        if (mapEvidence?.enabled == true && hasLocalForkMapEvidence(mapEvidence)) {
            RouteLocalForkEvidenceCard(
                fromLatLng = fromLatLng,
                toLatLng = toLatLng,
                mapEvidence = mapEvidence,
                cardBorder = cardBorder,
            )
        }
    }
}

@Composable
private fun RouteLocalForkEvidenceCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    mapEvidence: RouteMapEvidenceUiModel,
    cardBorder: BorderStroke,
) {
    Spacer(modifier = Modifier.height(12.dp))
    ComparisonMapCard(
        decisionFirst = false,
        headline = mapEvidence.localHeadline,
        caption = mapEvidence.localCaption,
        detailCaption = null,
        legend = RoutePreviewLegend,
        cardBorder = cardBorder,
        fromLatLng = fromLatLng,
        toLatLng = toLatLng,
        boundsPaddingPx = LocalMapBoundsPaddingPx,
        showStartEndMarkers = false,
        footerLabel = mapEvidence.splitLabel,
        splitPoint = mapEvidence.splitPoint,
        splitMarkerTitle = mapEvidence.splitLabel,
        splitMarkerSnippet = mapEvidence.localCaption,
        boundsPoints = buildLocalEvidenceBounds(mapEvidence),
    ) {
        renderLocalForkPolylines(mapEvidence = mapEvidence)
    }
}

@Composable
private fun SingleRouteMapPreviewCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    routePathPoints: List<LatLng>,
    trafficSegments: List<TrafficSegment>,
    junctionAnnotations: List<RouteJunctionAnnotation>,
    alternativePaths: List<List<LatLng>>,
    decisionCaption: String,
    strategicCaption: String?,
    preMapNote: String?,
    showLegend: Boolean,
    cardBorder: BorderStroke,
    mapPreviewHeight: Dp,
    modifier: Modifier = Modifier,
) {
    ComparisonMapCard(
        decisionFirst = true,
        sectionLabel = "Route preview",
        decisionHeadline = decisionCaption,
        supportingCaption = strategicCaption,
        preMapNote = preMapNote,
        legend = if (showLegend) RoutePreviewLegend else null,
        cardBorder = cardBorder,
        fromLatLng = fromLatLng,
        toLatLng = toLatLng,
        boundsPaddingPx = StrategicMapBoundsPaddingPx,
        showStartEndMarkers = true,
        mapPreviewHeight = mapPreviewHeight,
        modifier = modifier,
        boundsPoints =
            buildFullTripBounds(
                fromLatLng = fromLatLng,
                toLatLng = toLatLng,
                marshioPath = routePathPoints,
                alternativePaths = alternativePaths,
                junctionAnnotations = junctionAnnotations,
            ),
    ) {
        RoutePreviewMapLayers(
            routePathPoints = routePathPoints,
            trafficSegments = trafficSegments,
            junctionAnnotations = junctionAnnotations,
            alternativePaths = alternativePaths,
        )
    }
}

@Composable
@GoogleMapComposable
internal fun RoutePreviewMapLayers(
    routePathPoints: List<LatLng>,
    trafficSegments: List<TrafficSegment>,
    junctionAnnotations: List<RouteJunctionAnnotation>,
    alternativePaths: List<List<LatLng>>,
) {
    val density = LocalDensity.current.density
    alternativePaths.forEach { path ->
        if (path.size < 2) return@forEach
        RenderPreviewAlternativeRoutePolyline(points = path)
    }
    val segmentsToRender =
        trafficSegments.filter { it.points.size >= 2 }.ifEmpty {
            if (routePathPoints.size >= 2) {
                listOf(
                    TrafficSegment(
                        points = routePathPoints,
                        speedCategory = SpeedCategory.UNKNOWN,
                    ),
                )
            } else {
                emptyList()
            }
        }
    RenderTrafficSegments(
        segments = segmentsToRender,
        density = density,
        zIndexBase = TrafficPolylineStyle.PreviewMainRouteHaloZIndex,
    )
    RenderJunctionAnnotations(annotations = junctionAnnotations)
}

@Composable
internal fun RoutePreviewMapHost(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    boundsPaddingPx: Int,
    showStartEndMarkers: Boolean,
    modifier: Modifier = Modifier,
    boundsPoints: List<LatLng>? = null,
    splitPoint: LatLng? = null,
    splitMarkerTitle: String? = null,
    splitMarkerSnippet: String? = null,
    cameraPolicy: RoutePreviewCameraPolicy = RoutePreviewCameraPolicy.AlwaysFitOnDataChange,
    zoomControlsEnabled: Boolean = false,
    fitRouteTrigger: Int = 0,
    mapContent: @Composable @GoogleMapComposable () -> Unit,
) {
    val context = LocalContext.current
    var startIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var endIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var splitIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var initialFitComplete by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()

    suspend fun fitRouteBounds(): Boolean {
        val boundsBuilder = LatLngBounds.builder().include(fromLatLng).include(toLatLng)
        boundsPoints?.forEach { boundsBuilder.include(it) }
        return runCatching {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    boundsPaddingPx,
                ),
            )
        }.isSuccess
    }

    when (cameraPolicy) {
        RoutePreviewCameraPolicy.AlwaysFitOnDataChange -> {
            LaunchedEffect(mapReady, fromLatLng, toLatLng, boundsPoints, boundsPaddingPx) {
                if (!mapReady) return@LaunchedEffect
                fitRouteBounds()
            }
        }
        RoutePreviewCameraPolicy.InitialFitOnce -> {
            LaunchedEffect(mapReady) {
                if (!mapReady || initialFitComplete) return@LaunchedEffect
                if (fitRouteBounds()) {
                    initialFitComplete = true
                }
            }
            LaunchedEffect(fitRouteTrigger) {
                if (!mapReady || fitRouteTrigger == 0) return@LaunchedEffect
                fitRouteBounds()
            }
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = rememberMarshioMapProperties(),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = zoomControlsEnabled,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                indoorLevelPickerEnabled = false,
            ),
            onMapLoaded = {
                mapReady = true
                if (showStartEndMarkers) {
                    if (startIcon == null) {
                        startIcon = MapPreviewMarkerIcons.previewStart(context)
                    }
                    if (endIcon == null) {
                        endIcon = MapPreviewMarkerIcons.previewEnd(context)
                    }
                }
                if (splitPoint != null && splitIcon == null) {
                    splitIcon = MapPreviewMarkerIcons.split(context)
                }
            },
        ) {
            mapContent()
            if (showStartEndMarkers) {
                startIcon?.let { icon ->
                    Marker(
                        state = MarkerState(position = fromLatLng),
                        title = "Start",
                        icon = icon,
                        zIndex = 2f,
                        onClick = { true },
                    )
                }
                endIcon?.let { icon ->
                    Marker(
                        state = MarkerState(position = toLatLng),
                        title = "End",
                        icon = icon,
                        zIndex = 2f,
                        onClick = { true },
                    )
                }
            }
            splitPoint?.let { point ->
                splitIcon?.let { icon ->
                    Marker(
                        state = MarkerState(position = point),
                        title = splitMarkerTitle,
                        snippet = splitMarkerSnippet,
                        icon = icon,
                        zIndex = 3f,
                        onClick = { true },
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonMapCard(
    decisionFirst: Boolean,
    sectionLabel: String? = null,
    decisionHeadline: String? = null,
    supportingCaption: String? = null,
    preMapNote: String? = null,
    headline: String? = null,
    caption: String? = null,
    detailCaption: String? = null,
    legend: String? = null,
    cardBorder: BorderStroke,
    fromLatLng: LatLng,
    toLatLng: LatLng,
    boundsPaddingPx: Int,
    showStartEndMarkers: Boolean,
    modifier: Modifier = Modifier,
    footerLabel: String? = null,
    splitPoint: LatLng? = null,
    splitMarkerTitle: String? = null,
    splitMarkerSnippet: String? = null,
    boundsPoints: List<LatLng>? = null,
    mapPreviewHeight: Dp = MapPreviewHeight,
    mapContent: @Composable @GoogleMapComposable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (decisionFirst) {
                if (!sectionLabel.isNullOrBlank()) {
                    Text(
                        text = sectionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (!decisionHeadline.isNullOrBlank()) {
                    Text(
                        text = decisionHeadline,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                        ),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
                if (!supportingCaption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = supportingCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
                if (!legend.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = legend,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
                if (!preMapNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = preMapNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
            } else {
                if (!headline.isNullOrBlank()) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
                if (!caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
                if (!detailCaption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detailCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
                if (!legend.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = legend,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            RoutePreviewMapHost(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(mapPreviewHeight)
                        .clip(RoundedCornerShape(10.dp)),
                fromLatLng = fromLatLng,
                toLatLng = toLatLng,
                boundsPaddingPx = boundsPaddingPx,
                showStartEndMarkers = showStartEndMarkers,
                boundsPoints = boundsPoints,
                splitPoint = splitPoint,
                splitMarkerTitle = splitMarkerTitle,
                splitMarkerSnippet = splitMarkerSnippet,
                mapContent = mapContent,
            )
            if (!footerLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = footerLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClearRoadColors.RoadGrey,
                )
            }
        }
    }
}

@Composable
@GoogleMapComposable
private fun renderLocalForkPolylines(
    mapEvidence: RouteMapEvidenceUiModel,
) {
    val density = LocalDensity.current.density
    if (mapEvidence.sharedPath.size >= 2) {
        RenderPreviewSecondaryPolyline(
            points = mapEvidence.sharedPath,
            color = SharedRoutePolylineColor,
            width = SharedRoutePolylineWidth,
            zIndex = 0f,
        )
    }
    RenderPreviewAlternativeRoutePolyline(
        points = mapEvidence.googleDivergentPath,
    )
    val marshioForkPath =
        mapEvidence.sharedPath.lastOrNull()?.let { split ->
            listOf(split) + mapEvidence.marshioDivergentPath
        } ?: mapEvidence.marshioDivergentPath
    RenderPreviewMarshioSolidPolyline(
        points = marshioForkPath,
        density = density,
        width = TrafficPolylineStyle.PreviewMainRouteCoreWidthPx,
        zIndex = TrafficPolylineStyle.PreviewMainRouteHaloZIndex,
    )
}

internal fun buildFullTripBounds(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    marshioPath: List<LatLng>,
    alternativePaths: List<List<LatLng>>,
    junctionAnnotations: List<RouteJunctionAnnotation> = emptyList(),
): List<LatLng> =
    buildList {
        add(fromLatLng)
        add(toLatLng)
        addAll(marshioPath)
        alternativePaths.forEach { path -> addAll(path) }
        junctionAnnotations.forEach { add(it.position) }
    }

private fun buildLocalEvidenceBounds(
    mapEvidence: RouteMapEvidenceUiModel,
): List<LatLng> =
    buildList {
        mapEvidence.splitPoint?.let { add(it) }
        addAll(mapEvidence.sharedPath)
        addAll(mapEvidence.googleDivergentPath)
        addAll(mapEvidence.marshioDivergentPath)
    }
