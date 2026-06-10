package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import java.util.Locale

internal enum class UaeCorridorTollHint {
    HIGH_LIKELIHOOD,
    LOW_LIKELIHOOD,
    NEUTRAL,
}

private val uaeHighTollCorridorKeywords = listOf(
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

private val uaeLowerTollCorridorKeywords = listOf(
    "mohammed bin zayed road",
    "mbz road",
    "e311",
    "emirates road",
    "e611",
    "ajman",
    "sharjah",
)

internal fun corridorTollHintFromScan(scanText: String): UaeCorridorTollHint {
    val lc = scanText.lowercase(Locale.US)
    val highHits = uaeHighTollCorridorKeywords.count { lc.contains(it) }
    val lowHits = uaeLowerTollCorridorKeywords.count { lc.contains(it) }
    return when {
        highHits > lowHits -> UaeCorridorTollHint.HIGH_LIKELIHOOD
        lowHits > highHits -> UaeCorridorTollHint.LOW_LIKELIHOOD
        else -> UaeCorridorTollHint.NEUTRAL
    }
}

internal fun getTollLevel(tollAed: Int): String =
    when {
        tollAed == 0 -> "none"
        tollAed in 1..8 -> "low"
        tollAed in 9..20 -> "medium"
        else -> "high"
    }

internal fun tollPhraseForCard(
    item: RealRouteDebugData,
    routeIndex: Int,
    selectedMode: PreferenceMode,
    recommendedRouteIndex: Int,
    routes: List<RealRouteDebugData>,
): String {
    if (
        selectedMode == PreferenceMode.NO_TOLLS &&
        routeIndex == recommendedRouteIndex
    ) {
        return "Lowest toll route"
    }
    val minDurIdx =
        routes.indices.minByOrNull { routes[it].durationSeconds } ?: routeIndex
    val fastest = routes[minDurIdx]
    val thisTotal =
        estimateTotalRouteCostAed(
            item.tollAED,
            estimateFuelCostAed(item.distanceMeters / 1000.0),
        )
    val fastestTotal =
        estimateTotalRouteCostAed(
            fastest.tollAED,
            estimateFuelCostAed(fastest.distanceMeters / 1000.0),
        )
    if (
        item.durationSeconds > fastest.durationSeconds &&
        thisTotal < fastestTotal
    ) {
        return when (selectedMode) {
            PreferenceMode.NO_TOLLS -> "Salik-saving leg"
            else -> "Smoother city approach"
        }
    }
    if (item.durationSeconds <= 0 || item.distanceMeters <= 0) {
        return "Toll estimate"
    }
    val corridorHint = corridorTollHintFromScan(item.corridorScanText)
    val isFastestTimeRoute = routeIndex == minDurIdx
    val durationStretchVsFastest =
        if (fastest.durationSeconds <= 0) {
            0f
        } else {
            (item.durationSeconds - fastest.durationSeconds).toFloat() /
                fastest.durationSeconds.toFloat()
        }
    val tollBand = getTollLevel(item.tollAED)

    return when (selectedMode) {
        PreferenceMode.FASTEST ->
            when {
                isFastestTimeRoute &&
                    (
                        corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD ||
                            tollBand == "medium" ||
                            tollBand == "high"
                        ) ->
                    "Faster urban stretch"
                isFastestTimeRoute -> "Dubai corridor"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    "More Salik ahead"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD ->
                    "Steadier corridor leg"
                durationStretchVsFastest > 0.12f -> "Easier traffic stretch"
                tollBand == "none" -> "Fast city run"
                tollBand == "low" -> "Main motorway stretch"
                routeIndex % 2 == 0 -> "Main motorway stretch"
                else -> "More Salik ahead"
            }
        PreferenceMode.NO_TOLLS ->
            when (corridorHint) {
                UaeCorridorTollHint.HIGH_LIKELIHOOD ->
                    if (item.tollAED >= fastest.tollAED) {
                        "Higher toll pick"
                    } else {
                        "More Salik ahead"
                    }
                UaeCorridorTollHint.LOW_LIKELIHOOD -> "Lower Salik route"
                UaeCorridorTollHint.NEUTRAL ->
                    when (tollBand) {
                        "none" -> "Budget-friendly drive"
                        "low" -> "Salik-saving leg"
                        else ->
                            if (routeIndex % 2 == 0) {
                                "Main motorway stretch"
                            } else {
                                "More Salik ahead"
                            }
                    }
            }
        PreferenceMode.CALM ->
            when {
                durationStretchVsFastest > 0.1f -> "Smoother UAE leg"
                corridorHint == UaeCorridorTollHint.LOW_LIKELIHOOD -> "Lower Salik route"
                corridorHint == UaeCorridorTollHint.HIGH_LIKELIHOOD -> "More Salik ahead"
                else -> "Smoother city approach"
            }
    }
}
