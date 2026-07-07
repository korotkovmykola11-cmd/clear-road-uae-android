package com.clearroad.app.guidance

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.clearroad.app.MapPreviewMarkerIcons
import com.clearroad.app.map.rememberMarshioMapProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val MarshioRouteColor = Color(0xFF00E676)
private val GoogleRouteColor = Color(0xFF757575)
private val MarshioRouteWidth = 22f
private val GoogleRouteWidth = 18f
private const val MapBoundsPaddingPx = 96
private const val FollowZoom = 17f
private const val CameraAnimationMs = 450

@Composable
internal fun MarshioGuidanceMap(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    marshioPath: List<LatLng>,
    googlePath: List<LatLng>,
    guidancePosition: LatLng?,
    modifier: Modifier = Modifier,
    guidanceBearing: Float? = null,
    followCamera: Boolean = false,
    useSimulatedMarkerLabel: Boolean = false,
) {
    val context = LocalContext.current
    var startIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var endIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var guidanceIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var initialBoundsApplied by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val cameraFollowing = followCamera && initialBoundsApplied

    LaunchedEffect(guidancePosition, guidanceBearing, cameraFollowing) {
        val position = guidancePosition ?: return@LaunchedEffect
        if (!cameraFollowing) return@LaunchedEffect
        val cameraBuilder = CameraPosition.Builder()
            .target(position)
            .zoom(FollowZoom)
        guidanceBearing?.let { cameraBuilder.bearing(it) }
        cameraPositionState.animate(
            update = CameraUpdateFactory.newCameraPosition(cameraBuilder.build()),
            durationMs = CameraAnimationMs,
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = rememberMarshioMapProperties(),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = !cameraFollowing,
            zoomGesturesEnabled = !cameraFollowing,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = !cameraFollowing,
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            indoorLevelPickerEnabled = false,
        ),
        onMapLoaded = {
            if (startIcon == null) startIcon = MapPreviewMarkerIcons.start(context)
            if (endIcon == null) endIcon = MapPreviewMarkerIcons.end(context)
            if (guidanceIcon == null) guidanceIcon = GuidanceMarkerIcons.guidanceDot(context)
            if (!initialBoundsApplied) {
                val boundsBuilder = LatLngBounds.builder().include(fromLatLng).include(toLatLng)
                marshioPath.forEach { boundsBuilder.include(it) }
                googlePath.forEach { boundsBuilder.include(it) }
                scope.launch {
                    runCatching {
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngBounds(
                                boundsBuilder.build(),
                                MapBoundsPaddingPx,
                            ),
                        )
                    }.onSuccess {
                        initialBoundsApplied = true
                    }
                }
            }
        },
    ) {
        if (googlePath.size >= 2) {
            Polyline(
                points = googlePath,
                color = GoogleRouteColor,
                width = GoogleRouteWidth,
                zIndex = 0f,
            )
        }
        if (marshioPath.size >= 2) {
            Polyline(
                points = marshioPath,
                color = MarshioRouteColor,
                width = MarshioRouteWidth,
                zIndex = 1f,
            )
        }
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
                title = "Destination",
                icon = icon,
                zIndex = 2f,
                onClick = { true },
            )
        }
        guidancePosition?.let { position ->
            guidanceIcon?.let { icon ->
                Marker(
                    state = MarkerState(position = position),
                    title = if (useSimulatedMarkerLabel) "Simulated GPS" else "Your location",
                    icon = icon,
                    zIndex = 3f,
                    onClick = { true },
                )
            }
        }
    }
}
