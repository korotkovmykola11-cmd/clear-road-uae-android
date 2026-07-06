package com.clearroad.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
@GoogleMapComposable
internal fun RenderJunctionAnnotations(
    annotations: List<RouteJunctionAnnotation>,
) {
    if (annotations.isEmpty()) return
    val context = LocalContext.current
    val icons =
        remember(annotations) {
            annotations.associateWith { annotation ->
                JunctionAnnotationMarkerIcons.card(context, annotation)
            }
        }
    annotations.forEach { annotation ->
        icons[annotation]?.let { icon ->
            Marker(
                state = MarkerState(position = annotation.position),
                icon = icon,
                anchor = Offset(0.5f, 1f),
                zIndex = 10f,
                onClick = { true },
            )
        }
    }
}
