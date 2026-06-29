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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

private val MapPreviewHeight = 272.dp
private val RecommendedRoutePolylineColor = Color(0xFF00E676)
private val AlternativeRoutePolylineColor = Color(0xFF757575)
private val SharedRoutePolylineColor = Color(0xFFBDBDBD)
private val RecommendedRoutePolylineWidth = 22f
private val AlternativeRoutePolylineWidth = 18f
private val SharedRoutePolylineWidth = 14f
private val StrategicMapBoundsPaddingPx = 112
private val LocalMapBoundsPaddingPx = 72

@Composable
internal fun MapPreviewCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    routePathPoints: List<LatLng> = emptyList(),
    otherRoutePathPoints: List<List<LatLng>> = emptyList(),
    routeOptionsCount: Int = 1,
    mapEvidence: RouteMapEvidenceUiModel? = null,
    cardBorder: BorderStroke,
    modifier: Modifier = Modifier,
) {
    val evidenceMode = mapEvidence?.enabled == true
    if (evidenceMode && mapEvidence != null) {
        RouteComparisonEvidenceSection(
            fromLatLng = fromLatLng,
            toLatLng = toLatLng,
            mapEvidence = mapEvidence,
            marshioRoutePath = routePathPoints,
            cardBorder = cardBorder,
            modifier = modifier,
        )
        return
    }

    SingleRouteMapPreviewCard(
        fromLatLng = fromLatLng,
        toLatLng = toLatLng,
        routePathPoints = routePathPoints,
        otherRoutePathPoints = otherRoutePathPoints,
        routeOptionsCount = routeOptionsCount,
        cardBorder = cardBorder,
        modifier = modifier,
    )
}

@Composable
private fun RouteComparisonEvidenceSection(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    mapEvidence: RouteMapEvidenceUiModel,
    marshioRoutePath: List<LatLng>,
    cardBorder: BorderStroke,
    modifier: Modifier = Modifier,
) {
    val googlePath = mapEvidence.googleComparisonPath
    val marshioPath =
        mapEvidence.marshioComparisonPath.takeIf { it.size >= 2 }
            ?: marshioRoutePath
    val hasLocalForkEvidence =
        mapEvidence.splitPoint != null &&
            mapEvidence.googleDivergentPath.size >= 2 &&
            mapEvidence.marshioDivergentPath.size >= 2

    Column(modifier = modifier.fillMaxWidth()) {
        ComparisonMapCard(
            headline = mapEvidence.strategicHeadline,
            caption = mapEvidence.strategicCaption,
            legend = "Green = MARSHIO · Gray = Google",
            cardBorder = cardBorder,
            fromLatLng = fromLatLng,
            toLatLng = toLatLng,
            boundsPaddingPx = StrategicMapBoundsPaddingPx,
            showStartEndMarkers = true,
            boundsPoints =
                buildStrategicComparisonBounds(
                    fromLatLng = fromLatLng,
                    toLatLng = toLatLng,
                    googlePath = googlePath,
                    marshioPath = marshioPath,
                ),
        ) {
            renderFullCorridorPolylines(
                googlePath = googlePath,
                marshioPath = marshioPath,
            )
        }

        if (hasLocalForkEvidence) {
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonMapCard(
                headline = mapEvidence.localHeadline,
                caption = mapEvidence.localCaption,
                legend = "Green = MARSHIO · Gray = Google",
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
    }
}

@Composable
private fun SingleRouteMapPreviewCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    routePathPoints: List<LatLng>,
    otherRoutePathPoints: List<List<LatLng>>,
    routeOptionsCount: Int,
    cardBorder: BorderStroke,
    modifier: Modifier = Modifier,
) {
    val alternativePaths = otherRoutePathPoints.filter { it.isNotEmpty() }
    val totalOptions = routeOptionsCount.coerceAtLeast(1 + alternativePaths.size)

    ComparisonMapCard(
        headline = "Route preview",
        caption = null,
        legend = if (totalOptions > 1) {
            "Green = MARSHIO recommendation · Gray = alternatives"
        } else {
            null
        },
        cardBorder = cardBorder,
        fromLatLng = fromLatLng,
        toLatLng = toLatLng,
        boundsPaddingPx = StrategicMapBoundsPaddingPx,
        showStartEndMarkers = true,
        modifier = modifier,
        boundsPoints = buildFullTripBounds(fromLatLng, toLatLng, routePathPoints, alternativePaths),
    ) {
        alternativePaths.forEach { path ->
            Polyline(
                points = path,
                color = AlternativeRoutePolylineColor,
                width = AlternativeRoutePolylineWidth,
                zIndex = 0f,
            )
        }
        if (routePathPoints.isNotEmpty()) {
            Polyline(
                points = routePathPoints,
                color = RecommendedRoutePolylineColor,
                width = RecommendedRoutePolylineWidth,
                zIndex = 1f,
            )
        }
    }
}

@Composable
private fun ComparisonMapCard(
    headline: String,
    caption: String?,
    legend: String?,
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
    mapContent: @Composable @GoogleMapComposable () -> Unit,
) {
    val context = LocalContext.current
    var startIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var endIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var splitIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(fromLatLng, toLatLng, boundsPaddingPx, boundsPoints) {
        val boundsBuilder = LatLngBounds.builder().include(fromLatLng).include(toLatLng)
        boundsPoints?.forEach { boundsBuilder.include(it) }
        runCatching {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), boundsPaddingPx),
            )
        }
    }

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
            Text(
                text = headline,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            if (!caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = ClearRoadColors.RoadGrey,
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
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MapPreviewHeight)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        compassEnabled = false,
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = false,
                        indoorLevelPickerEnabled = false,
                    ),
                    onMapLoaded = {
                        if (showStartEndMarkers) {
                            if (startIcon == null) {
                                startIcon = MapPreviewMarkerIcons.start(context)
                            }
                            if (endIcon == null) {
                                endIcon = MapPreviewMarkerIcons.end(context)
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
private fun renderFullCorridorPolylines(
    googlePath: List<LatLng>,
    marshioPath: List<LatLng>,
) {
    if (googlePath.size >= 2) {
        Polyline(
            points = googlePath,
            color = AlternativeRoutePolylineColor,
            width = AlternativeRoutePolylineWidth,
            zIndex = 0f,
        )
    }
    if (marshioPath.size >= 2 && marshioPath != googlePath) {
        Polyline(
            points = marshioPath,
            color = RecommendedRoutePolylineColor,
            width = RecommendedRoutePolylineWidth,
            zIndex = 1f,
        )
    }
}

@Composable
@GoogleMapComposable
private fun renderLocalForkPolylines(
    mapEvidence: RouteMapEvidenceUiModel,
) {
    if (mapEvidence.sharedPath.size >= 2) {
        Polyline(
            points = mapEvidence.sharedPath,
            color = SharedRoutePolylineColor,
            width = SharedRoutePolylineWidth,
            zIndex = 0f,
        )
    }
    Polyline(
        points = mapEvidence.googleDivergentPath,
        color = AlternativeRoutePolylineColor,
        width = AlternativeRoutePolylineWidth,
        zIndex = 1f,
    )
    val marshioForkPath =
        mapEvidence.sharedPath.lastOrNull()?.let { split ->
            listOf(split) + mapEvidence.marshioDivergentPath
        } ?: mapEvidence.marshioDivergentPath
    Polyline(
        points = marshioForkPath,
        color = RecommendedRoutePolylineColor,
        width = RecommendedRoutePolylineWidth,
        zIndex = 2f,
    )
}

private fun buildStrategicComparisonBounds(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    googlePath: List<LatLng>,
    marshioPath: List<LatLng>,
): List<LatLng> =
    buildList {
        add(fromLatLng)
        add(toLatLng)
        addAll(googlePath)
        addAll(marshioPath)
    }

private fun buildFullTripBounds(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    marshioPath: List<LatLng>,
    alternativePaths: List<List<LatLng>>,
): List<LatLng> =
    buildList {
        add(fromLatLng)
        add(toLatLng)
        addAll(marshioPath)
        alternativePaths.forEach { path -> addAll(path) }
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
