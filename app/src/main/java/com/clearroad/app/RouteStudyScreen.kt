package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor

private val RouteStudyMapBoundsPaddingPx = 96

@Composable
internal fun RouteStudyScreen(
    model: RouteDetailsUiModel,
    onClose: () -> Unit,
) {
    val fromLatLng = model.fromLatLng ?: return
    val toLatLng = model.toLatLng ?: return
    val modeAccent = model.mode.accentColor()
    val alternativePaths = model.otherRoutePathPoints.filter { it.isNotEmpty() }
    var fitRouteTrigger by remember { mutableIntStateOf(0) }
    val boundsPoints =
        remember(fromLatLng, toLatLng, model.routePathPoints, alternativePaths, model.junctionAnnotations) {
            buildFullTripBounds(
                fromLatLng = fromLatLng,
                toLatLng = toLatLng,
                marshioPath = model.routePathPoints,
                alternativePaths = alternativePaths,
                junctionAnnotations = model.junctionAnnotations,
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ClearRoadColors.RouteCardSurface)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = modeAccent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.routeIdentityTitle.ifBlank { "Route ${model.routeNumber}" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ClearRoadColors.RoadGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model.durationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClearRoadColors.RoadGreyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { fitRouteTrigger++ }) {
                Text(
                    text = "Fit route",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = modeAccent,
                )
            }
        }
        RoutePreviewMapHost(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            fromLatLng = fromLatLng,
            toLatLng = toLatLng,
            boundsPaddingPx = RouteStudyMapBoundsPaddingPx,
            showStartEndMarkers = true,
            boundsPoints = boundsPoints,
            cameraPolicy = RoutePreviewCameraPolicy.InitialFitOnce,
            zoomControlsEnabled = true,
            fitRouteTrigger = fitRouteTrigger,
        ) {
            RoutePreviewMapLayers(
                routePathPoints = model.routePathPoints,
                trafficSegments = model.trafficSegments,
                junctionAnnotations = model.junctionAnnotations,
                alternativePaths = alternativePaths,
            )
        }
        RouteDetailsHandoffFooter(
            model = model,
            modeAccent = modeAccent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            showWazeHandoff = false,
        )
    }
}
