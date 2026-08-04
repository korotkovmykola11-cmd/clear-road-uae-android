package com.clearroad.app.triphistory

import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Rounds user-selected origin/destination coordinates to a ~500 m grid for **same O-D pair**
 * matching (not same physical corridor).
 *
 * Coordinate source: [LatLng] from user address entry via Places Autocomplete → Fetch Place
 * (Google Places API lat/lng). Keys are **not** derived from Google route polylines.
 */
internal object TripHistoryKey {
    const val GRID_METERS = 500.0

    fun fromLatLng(latLng: LatLng): String = fromCoordinates(latLng.latitude, latLng.longitude)

    fun fromCoordinates(
        latitude: Double,
        longitude: Double,
        gridMeters: Double = GRID_METERS,
    ): String {
        val latStep = gridMeters / LAT_METERS_PER_DEGREE
        val lngStep =
            gridMeters /
                (LAT_METERS_PER_DEGREE * cos(Math.toRadians(latitude)).coerceAtLeast(0.01))
        val snappedLat = (latitude / latStep).roundToInt() * latStep
        val snappedLng = (longitude / lngStep).roundToInt() * lngStep
        return "%.5f,%.5f".format(snappedLat, snappedLng)
    }

    fun pairKeys(
        origin: LatLng,
        destination: LatLng,
    ): Pair<String, String> =
        fromLatLng(origin) to fromLatLng(destination)

    private const val LAT_METERS_PER_DEGREE = 111_320.0
}
