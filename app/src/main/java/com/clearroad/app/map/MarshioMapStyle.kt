package com.clearroad.app.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.clearroad.app.R
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties

@Composable
fun rememberMarshioMapProperties(): MapProperties {
    val context = LocalContext.current
    return remember {
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions =
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.marshio_map_style,
                ),
        )
    }
}
