package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.model.WhyTagUiModel
import java.util.Locale
import kotlin.math.roundToInt

private enum class WhyTagCorridorHint {
    HIGH_LIKELIHOOD,
    LOW_LIKELIHOOD,
    NEUTRAL,
}

private val whyTagHighTollCorridorKeywords = listOf(
    "sheikh zayed road",
    "szr",
    "e11",
    "al garhoud",
    "downtown dubai",
    "business bay",
    "financial centre",
    "financial center",
    "dubai marina",
)

private val whyTagLowerTollCorridorKeywords = listOf(
    "mohammed bin zayed road",
    "mbz road",
    "e311",
    "emirates road",
    "e611",
    "ajman",
    "sharjah",
)

private fun corridorHintFromScan(scanText: String): WhyTagCorridorHint {
    val lc = scanText.lowercase(Locale.US)
    val highHits = whyTagHighTollCorridorKeywords.count { lc.contains(it) }
    val lowHits = whyTagLowerTollCorridorKeywords.count { lc.contains(it) }
    return when {
        highHits > lowHits -> WhyTagCorridorHint.HIGH_LIKELIHOOD
        lowHits > highHits -> WhyTagCorridorHint.LOW_LIKELIHOOD
        else -> WhyTagCorridorHint.NEUTRAL
    }
}

private fun minutesFasterThanNextAlternative(
    routeIndex: Int,
    routes: List<RealRouteDebugData>,
): Int? {
    if (routes.size < 2) return null
    val ordered =
        routes.indices.sortedBy { routes[it].durationSeconds }
    if (ordered.first() != routeIndex) return null
    val fastestSeconds = routes[routeIndex].durationSeconds
    if (fastestSeconds <= 0) return null
    val secondSeconds = routes[ordered[1]].durationSeconds
    val deltaMinutes = (secondSeconds - fastestSeconds) / 60.0
    val rounded = deltaMinutes.roundToInt()
    return if (rounded >= 2) rounded else null
}

private fun durationStretchVsFastest(
    item: RealRouteDebugData,
    routes: List<RealRouteDebugData>,
): Float {
    val fastestSeconds =
        routes.minOfOrNull { it.durationSeconds } ?: return 0f
    if (fastestSeconds <= 0 || item.durationSeconds <= 0) return 0f
    return (item.durationSeconds - fastestSeconds).toFloat() / fastestSeconds.toFloat()
}

private fun mainRoadsTag(corridorHint: WhyTagCorridorHint): WhyTagUiModel? =
    if (corridorHint == WhyTagCorridorHint.LOW_LIKELIHOOD) {
        WhyTagUiModel("Main roads")
    } else {
        null
    }

private fun takeUpToTwo(tags: List<WhyTagUiModel?>): List<WhyTagUiModel> =
    tags.filterNotNull().take(2)

/** Display-only short Why tags for route cards — never affects scoring or recommendation. */
internal fun whyTagsForRoute(
    item: RealRouteDebugData,
    routeIndex: Int,
    mode: PreferenceMode,
    routes: List<RealRouteDebugData>,
): List<WhyTagUiModel> {
    if (routes.isEmpty()) return emptyList()
    val corridorHint = corridorHintFromScan(item.corridorScanText)
    val maxToll = routes.maxOf { it.tollAED }

    return when (mode) {
        PreferenceMode.FASTEST ->
            takeUpToTwo(
                listOf(
                    minutesFasterThanNextAlternative(routeIndex, routes)?.let { minutes ->
                        WhyTagUiModel("$minutes min faster")
                    },
                    mainRoadsTag(corridorHint),
                ),
            )
        PreferenceMode.NO_TOLLS ->
            takeUpToTwo(
                listOf(
                    if (item.tollAED == 0) WhyTagUiModel("0 Salik gates") else null,
                    if (routes.size > 1 && item.tollAED > 0 && item.tollAED < maxToll) {
                        WhyTagUiModel("Lower Salik")
                    } else {
                        null
                    },
                    mainRoadsTag(corridorHint),
                ),
            )
        PreferenceMode.CALM -> {
            val lightTraffic =
                if (durationStretchVsFastest(item, routes) <= 0.12f) {
                    WhyTagUiModel("Light traffic")
                } else {
                    null
                }
            takeUpToTwo(
                listOf(
                    lightTraffic,
                    mainRoadsTag(corridorHint),
                ),
            )
        }
    }
}
