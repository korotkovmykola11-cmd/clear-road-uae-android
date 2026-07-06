package com.clearroad.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Polyline

@Composable
@GoogleMapComposable
internal fun RenderTrafficSegments(
    segments: List<TrafficSegment>,
    density: Float,
    zIndexBase: Float = 1f,
) {
    segments.forEach { segment ->
        if (segment.points.size < 2) return@forEach
        val color = TrafficPolylineStyle.lineColor(segment.speedCategory)
        val width = TrafficPolylineStyle.lineWidthPx(segment.speedCategory, density)
        val outlineWidth = width + TrafficPolylineStyle.outlineExtraPx(density)

        Polyline(
            points = segment.points,
            color = TrafficPolylineStyle.OutlineWhite,
            width = outlineWidth,
            zIndex = zIndexBase,
            startCap = com.google.android.gms.maps.model.RoundCap(),
            endCap = com.google.android.gms.maps.model.RoundCap(),
            jointType = com.google.android.gms.maps.model.JointType.ROUND,
        )
        Polyline(
            points = segment.points,
            color = color,
            width = width,
            zIndex = zIndexBase + 0.5f,
            startCap = com.google.android.gms.maps.model.RoundCap(),
            endCap = com.google.android.gms.maps.model.RoundCap(),
            jointType = com.google.android.gms.maps.model.JointType.ROUND,
        )
    }
}

/** Single MARSHIO-green fallback when no traffic segmentation is available. */
@Composable
@GoogleMapComposable
internal fun RenderFallbackRoutePolyline(
    points: List<com.google.android.gms.maps.model.LatLng>,
    density: Float,
    zIndex: Float = 1f,
) {
    if (points.size < 2) return
    val width = TrafficPolylineStyle.lineWidthPx(SpeedCategory.UNKNOWN, density)
    val outlineWidth = width + TrafficPolylineStyle.outlineExtraPx(density)
    Polyline(
        points = points,
        color = TrafficPolylineStyle.OutlineWhite,
        width = outlineWidth,
        zIndex = zIndex,
        startCap = com.google.android.gms.maps.model.RoundCap(),
        endCap = com.google.android.gms.maps.model.RoundCap(),
        jointType = com.google.android.gms.maps.model.JointType.ROUND,
    )
    Polyline(
        points = points,
        color = TrafficPolylineStyle.FallbackGreen,
        width = width,
        zIndex = zIndex + 0.5f,
        startCap = com.google.android.gms.maps.model.RoundCap(),
        endCap = com.google.android.gms.maps.model.RoundCap(),
        jointType = com.google.android.gms.maps.model.JointType.ROUND,
    )
}
