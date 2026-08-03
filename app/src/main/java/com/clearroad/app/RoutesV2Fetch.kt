package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import java.net.HttpURLConnection
import java.net.URL
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ROUTES_V2_COMPUTE_URL =
    "https://routes.googleapis.com/directions/v2:computeRoutes"

internal data class DirectionsFetchResult(
    val raw: String?,
    val status: String?,
    val distanceDuration: Pair<String, String>?,
    val distanceDurationValues: Pair<Int, Int>?,
    val singleRoute: RealRouteDebugData?,
    val routes: List<RealRouteDebugData>,
)

internal suspend fun fetchRoutesV2Raw(
    origin: LatLng,
    destination: LatLng,
    apiKey: String = BuildConfig.PLACES_API_KEY,
): String? =
    withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            conn =
                (URL(ROUTES_V2_COMPUTE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 25_000
                    readTimeout = 25_000
                    RoutesV2ResponseAdapter.buildComputeRoutesHeaders(apiKey).forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
            val body =
                RoutesV2ResponseAdapter.buildComputeRoutesRequestBody(
                    originLat = origin.latitude,
                    originLng = origin.longitude,
                    destinationLat = destination.latitude,
                    destinationLng = destination.longitude,
                )
            conn.outputStream.use { stream -> stream.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return@withContext null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

internal fun extractRoutesV2Status(responseJson: String?): String {
    if (responseJson.isNullOrBlank()) return "EMPTY"
    val routes = RoutesV2ResponseAdapter.extractRouteLegsDebugData(responseJson)
    if (routes.isNotEmpty()) return "OK"
    return try {
        val error = org.json.JSONObject(responseJson).optJSONObject("error")
        error?.optString("status").takeIf { !it.isNullOrBlank() } ?: "ZERO_RESULTS"
    } catch (_: Exception) {
        "PARSE_ERROR"
    }
}

internal fun buildDirectionsFetchResultFromV2(raw: String?): DirectionsFetchResult {
    val routes = raw?.let { RoutesV2ResponseAdapter.extractRouteLegsDebugData(it) } ?: emptyList()
    val status = extractRoutesV2Status(raw)
    val first = routes.firstOrNull()
    val distanceDuration = first?.let { it.distanceText to it.durationText }
    val distanceDurationValues = first?.let { it.distanceMeters to it.durationSeconds }
    return DirectionsFetchResult(
        raw = raw,
        status = status,
        distanceDuration = distanceDuration,
        distanceDurationValues = distanceDurationValues,
        singleRoute = first,
        routes = routes,
    )
}
