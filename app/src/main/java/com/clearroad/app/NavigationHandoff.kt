package com.clearroad.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng

private const val TAG = "NavigationHandoff"
private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
private const val WAZE_PACKAGE = "com.waze"

private val WAZE_PLAY_STORE_MARKET_URI =
    Uri.parse("market://details?id=$WAZE_PACKAGE")
private val WAZE_PLAY_STORE_WEB_URI =
    Uri.parse("https://play.google.com/store/apps/details?id=$WAZE_PACKAGE")

internal fun latLngParam(latLng: LatLng): String =
    "${latLng.latitude},${latLng.longitude}"

internal fun googleMapsDirectionsUri(origin: LatLng, destination: LatLng): Uri =
    Uri.parse(
        "https://www.google.com/maps/dir/?api=1" +
            "&origin=${latLngParam(origin)}" +
            "&destination=${latLngParam(destination)}" +
            "&travelmode=driving",
    )

internal fun wazeNavigateUri(destination: LatLng): Uri =
    Uri.parse(
        "https://waze.com/ul?ll=${latLngParam(destination)}&navigate=yes",
    )

internal fun openGoogleMapsHandoff(
    context: Context,
    origin: LatLng,
    destination: LatLng,
) {
    val uri = googleMapsDirectionsUri(origin, destination)
    Log.d(TAG, "Google Maps handoff: generic ACTION_VIEW, uri=$uri")
    launchViewIntent(context, uri, preferPackage = null)
}

/** App-specific launch for diagnostics; not used in normal handoff. */
internal fun openGoogleMapsHandoffAppSpecific(
    context: Context,
    origin: LatLng,
    destination: LatLng,
) {
    val uri = googleMapsDirectionsUri(origin, destination)
    Log.d(TAG, "Google Maps handoff: app-specific ($GOOGLE_MAPS_PACKAGE), uri=$uri")
    launchViewIntent(context, uri, preferPackage = GOOGLE_MAPS_PACKAGE)
}

internal fun openWazeHandoff(
    context: Context,
    destination: LatLng,
) {
    val uri = wazeNavigateUri(destination)
    Log.d(TAG, "Waze handoff: generic ACTION_VIEW, uri=$uri")
    val wazeIntent = Intent(Intent.ACTION_VIEW, uri)
    if (wazeIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(wazeIntent)
        return
    }
    Log.d(TAG, "Waze URL not handled, opening Play Store fallback")
    openWazeInstallFallback(context)
}

private fun openWazeInstallFallback(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, WAZE_PLAY_STORE_MARKET_URI)
    if (marketIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(marketIntent)
        return
    }
    val webIntent = Intent(Intent.ACTION_VIEW, WAZE_PLAY_STORE_WEB_URI)
    if (webIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(webIntent)
        return
    }
    Log.d(TAG, "Waze fallback unavailable, no-op")
}

private fun launchViewIntent(
    context: Context,
    uri: Uri,
    preferPackage: String?,
) {
    if (preferPackage != null) {
        val packageIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(preferPackage)
        }
        if (packageIntent.resolveActivity(context.packageManager) != null) {
            Log.d(TAG, "Handoff: starting app-specific ($preferPackage)")
            context.startActivity(packageIntent)
            return
        }
        Log.d(
            TAG,
            "Handoff: app-specific ($preferPackage) not available, falling back to generic",
        )
    }
    val genericIntent = Intent(Intent.ACTION_VIEW, uri)
    if (genericIntent.resolveActivity(context.packageManager) != null) {
        Log.d(TAG, "Handoff: starting generic ACTION_VIEW")
        context.startActivity(genericIntent)
        return
    }
    Log.d(TAG, "Handoff: no activity available for uri=$uri")
}
