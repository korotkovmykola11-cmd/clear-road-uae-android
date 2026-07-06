package com.clearroad.app

import com.google.android.gms.maps.model.LatLng

data class RouteJunctionAnnotation(
    val type: JunctionType,
    val position: LatLng,
    val instruction: String,
)
