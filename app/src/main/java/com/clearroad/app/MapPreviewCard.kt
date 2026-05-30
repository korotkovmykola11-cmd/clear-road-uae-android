package com.clearroad.app

import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.theme.ClearRoadColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

internal const val ROUTE_PREVIEW_DEBUG_TAG = "RoutePreviewDebug"

private val MapPreviewHeight = 170.dp
private val RoutePreviewPolylineColor = Color(0xFF00C853)
private val RoutePreviewPolylineWidth = 15f

@Composable
internal fun MapPreviewCard(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    routePathPoints: List<LatLng> = emptyList(),
    cardBorder: BorderStroke,
    modifier: Modifier = Modifier,
    debugRouteIndex: Int = -1,
    debugRouteName: String = "unknown",
) {
    val cameraPositionState = rememberCameraPositionState()
    val pointsCount = routePathPoints.size
    val isEmpty = routePathPoints.isEmpty()
    val drawingPolyline = !isEmpty
    LaunchedEffect(debugRouteIndex, debugRouteName, routePathPoints) {
        Log.d(
            ROUTE_PREVIEW_DEBUG_TAG,
            "RoutePreviewDebug:\n" +
                "RouteIndex=$debugRouteIndex\n" +
                "RouteName=$debugRouteName\n" +
                "Points=$pointsCount\n" +
                "IsEmpty=$isEmpty\n" +
                "DrawingPolyline=$drawingPolyline",
        )
    }
    LaunchedEffect(fromLatLng, toLatLng, routePathPoints) {
        val boundsBuilder = LatLngBounds.builder()
            .include(fromLatLng)
            .include(toLatLng)
        routePathPoints.forEach { boundsBuilder.include(it) }
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 64),
        )
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
                text = "Route preview",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
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
                ) {
                    if (routePathPoints.isNotEmpty()) {
                        Log.d(
                            ROUTE_PREVIEW_DEBUG_TAG,
                            "RoutePreviewDebug:\n" +
                                "RouteIndex=$debugRouteIndex\n" +
                                "RouteName=$debugRouteName\n" +
                                "PolylineComposableInvoked=true",
                        )
                        Polyline(
                            points = routePathPoints,
                            color = RoutePreviewPolylineColor,
                            width = RoutePreviewPolylineWidth,
                        )
                    }
                    Marker(
                        state = MarkerState(position = fromLatLng),
                        title = "Start",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = { true },
                    )
                    Marker(
                        state = MarkerState(position = toLatLng),
                        title = "End",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = { true },
                    )
                }
            }
        }
    }
}
