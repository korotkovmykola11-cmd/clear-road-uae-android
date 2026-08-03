package com.clearroad.app

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val TAG = "StaticMapPreviewUrl"
internal const val STATIC_MAP_URL_MAX_LENGTH = 8192
private const val STATIC_MAP_BASE_URL = "https://maps.googleapis.com/maps/api/staticmap?"

/** Static Maps path colors — same palette as the removed interactive traffic renderer. */
internal object StaticMapTrafficColors {
    fun hexRgb(category: SpeedCategory): String =
        when (category) {
            SpeedCategory.FREE -> "2ECC71"
            SpeedCategory.MODERATE -> "F1C40F"
            SpeedCategory.SLOW -> "E67E22"
            SpeedCategory.JAM -> "E74C3C"
            SpeedCategory.UNKNOWN -> "1D9E75"
        }
}

internal data class StaticMapUrlBuildResult(
    val url: String,
    val exceedsMaxLength: Boolean,
    val usedFallback: Boolean,
)

internal fun buildStaticMapUrl(
    trafficSegments: List<TrafficSegment>,
    from: LatLng,
    to: LatLng,
    apiKey: String,
    width: Int = 640,
    height: Int = 400,
    scale: Int = 2,
): String =
    buildStaticMapUrlWithMeta(
        trafficSegments = trafficSegments,
        from = from,
        to = to,
        apiKey = apiKey,
        width = width,
        height = height,
        scale = scale,
    ).url

internal fun buildStaticMapUrlWithMeta(
    trafficSegments: List<TrafficSegment>,
    from: LatLng,
    to: LatLng,
    apiKey: String,
    width: Int = 640,
    height: Int = 400,
    scale: Int = 2,
): StaticMapUrlBuildResult {
    val renderableSegments =
        trafficSegments.filter { segment -> segment.points.size >= 2 }
    if (renderableSegments.isEmpty()) {
        return StaticMapUrlBuildResult(
            url = assembleStaticMapUrl(emptyList(), from, to, apiKey, width, height, scale),
            exceedsMaxLength = false,
            usedFallback = false,
        )
    }

    var paths = renderableSegments.map { segmentToPath(it) }
    var url = assembleStaticMapUrl(paths, from, to, apiKey, width, height, scale)
    if (url.length <= STATIC_MAP_URL_MAX_LENGTH) {
        return StaticMapUrlBuildResult(url, exceedsMaxLength = false, usedFallback = false)
    }

    paths = mergePathsByColor(renderableSegments)
    url = assembleStaticMapUrl(paths, from, to, apiKey, width, height, scale)
    if (url.length <= STATIC_MAP_URL_MAX_LENGTH) {
        logUrlLengthWarning(url.length, usedFallback = true)
        return StaticMapUrlBuildResult(url, exceedsMaxLength = false, usedFallback = true)
    }

    for (toleranceMeters in listOf(20.0, 50.0, 100.0, 200.0)) {
        paths =
            renderableSegments.map { segment ->
                val simplified = PolyUtil.simplify(segment.points, toleranceMeters)
                segmentToPath(
                    TrafficSegment(
                        points = simplified,
                        speedCategory = segment.speedCategory,
                    ),
                )
            }.filter { it.points.size >= 2 }
        url = assembleStaticMapUrl(paths, from, to, apiKey, width, height, scale)
        if (url.length <= STATIC_MAP_URL_MAX_LENGTH) {
            logUrlLengthWarning(url.length, usedFallback = true)
            return StaticMapUrlBuildResult(url, exceedsMaxLength = false, usedFallback = true)
        }
    }

    logUrlLengthWarning(url.length, usedFallback = true)
    return StaticMapUrlBuildResult(
        url = url,
        exceedsMaxLength = url.length > STATIC_MAP_URL_MAX_LENGTH,
        usedFallback = true,
    )
}

private data class StaticMapPathSpec(
    val points: List<LatLng>,
    val colorHex: String,
)

private fun segmentToPath(segment: TrafficSegment): StaticMapPathSpec =
    StaticMapPathSpec(
        points = segment.points,
        colorHex = StaticMapTrafficColors.hexRgb(segment.speedCategory),
    )

private fun mergePathsByColor(segments: List<TrafficSegment>): List<StaticMapPathSpec> {
    if (segments.isEmpty()) return emptyList()
    val merged = mutableListOf<StaticMapPathSpec>()
    var currentCategory = segments.first().speedCategory
    var currentPoints = segments.first().points.toMutableList()

    fun flush() {
        if (currentPoints.size >= 2) {
            merged +=
                StaticMapPathSpec(
                    points = currentPoints.toList(),
                    colorHex = StaticMapTrafficColors.hexRgb(currentCategory),
                )
        }
    }

    for (index in 1 until segments.size) {
        val segment = segments[index]
        if (segment.speedCategory == currentCategory) {
            appendPathPoints(currentPoints, segment.points)
        } else {
            flush()
            currentCategory = segment.speedCategory
            currentPoints = segment.points.toMutableList()
        }
    }
    flush()
    return merged
}

private fun appendPathPoints(
    target: MutableList<LatLng>,
    incoming: List<LatLng>,
) {
    if (incoming.isEmpty()) return
    val first = incoming.first()
    if (target.isNotEmpty() && sameLatLng(target.last(), first)) {
        target.addAll(incoming.drop(1))
    } else {
        target.addAll(incoming)
    }
}

private fun sameLatLng(
    left: LatLng,
    right: LatLng,
): Boolean = left.latitude == right.latitude && left.longitude == right.longitude

private fun assembleStaticMapUrl(
    paths: List<StaticMapPathSpec>,
    from: LatLng,
    to: LatLng,
    apiKey: String,
    width: Int,
    height: Int,
    scale: Int,
): String =
    buildString {
        append(STATIC_MAP_BASE_URL)
        paths.forEach { path ->
            append("path=")
            append(
                urlEncodeParameter(
                    "color:0x${path.colorHex}FF|weight:5|enc:${PolyUtil.encode(path.points)}",
                ),
            )
            append("&")
        }
        append("markers=")
        append(urlEncodeParameter("color:blue|label:A|${formatLatLng(from)}"))
        append("&markers=")
        append(urlEncodeParameter("color:red|label:B|${formatLatLng(to)}"))
        append("&size=${width}x$height")
        append("&scale=$scale")
        append("&maptype=roadmap")
        append("&key=")
        append(urlEncodeParameter(apiKey))
    }

private fun formatLatLng(latLng: LatLng): String =
    "${latLng.latitude},${latLng.longitude}"

private fun urlEncodeParameter(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

private fun logUrlLengthWarning(
    length: Int,
    usedFallback: Boolean,
) {
    if (length > STATIC_MAP_URL_MAX_LENGTH) {
        Log.w(
            TAG,
            "Static map URL length $length exceeds $STATIC_MAP_URL_MAX_LENGTH; " +
                "Google Static Maps may reject the request. fallbackApplied=$usedFallback",
        )
    } else if (usedFallback) {
        Log.i(
            TAG,
            "Static map URL shortened with fallback (length=$length, max=$STATIC_MAP_URL_MAX_LENGTH)",
        )
    }
}
