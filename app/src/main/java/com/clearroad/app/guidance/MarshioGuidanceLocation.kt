package com.clearroad.app.guidance

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

internal data class MarshioGuidanceGpsFix(
    val position: LatLng,
    val bearingDegrees: Float?,
)

internal enum class MarshioGuidanceLocationStatus {
    REQUESTING_PERMISSION,
    PERMISSION_DENIED,
    WAITING_FOR_FIX,
    TRACKING,
}

@SuppressLint("MissingPermission")
@Composable
internal fun rememberMarshioGuidanceGpsFix(
    enabled: Boolean,
): Pair<MarshioGuidanceLocationStatus, MarshioGuidanceGpsFix?> {
    val context = LocalContext.current
    var status by remember { mutableStateOf(MarshioGuidanceLocationStatus.REQUESTING_PERMISSION) }
    var fix by remember { mutableStateOf<MarshioGuidanceGpsFix?>(null) }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            permissionGranted =
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            status =
                if (permissionGranted) {
                    MarshioGuidanceLocationStatus.WAITING_FOR_FIX
                } else {
                    MarshioGuidanceLocationStatus.PERMISSION_DENIED
                }
        }

    LaunchedEffect(enabled) {
        if (!enabled) {
            fix = null
            status = MarshioGuidanceLocationStatus.REQUESTING_PERMISSION
            return@LaunchedEffect
        }
        if (!permissionGranted) {
            status = MarshioGuidanceLocationStatus.REQUESTING_PERMISSION
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    DisposableEffect(enabled, permissionGranted) {
        if (!enabled || !permissionGranted) {
            onDispose { }
        } else {
            status = MarshioGuidanceLocationStatus.WAITING_FOR_FIX
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val callback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation ?: return
                        fix = location.toGpsFix()
                        status = MarshioGuidanceLocationStatus.TRACKING
                    }
                }
            val request =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                    .setMinUpdateIntervalMillis(500L)
                    .setMinUpdateDistanceMeters(3f)
                    .build()
            fusedClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper(),
            )
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fix = location.toGpsFix()
                    status = MarshioGuidanceLocationStatus.TRACKING
                }
            }
            onDispose {
                fusedClient.removeLocationUpdates(callback)
            }
        }
    }

    return status to fix
}

private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

private fun Location.toGpsFix(): MarshioGuidanceGpsFix =
    MarshioGuidanceGpsFix(
        position = LatLng(latitude, longitude),
        bearingDegrees = bearing.takeIf { hasBearing() },
    )
