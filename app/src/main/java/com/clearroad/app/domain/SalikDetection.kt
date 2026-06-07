package com.clearroad.app.domain

import java.util.Locale

/**
 * Stage 31.5 — scoring-only Salik heuristic when Google [fare] / tollAED is missing.
 * Not shown in UI; used for route ranking probe only.
 */
object SalikDetection {

    const val AED_PER_TOLL_ROAD = 4

    enum class Exposure {
        LOW,
        MEDIUM,
        HIGH,
    }

    data class Estimate(
        val estimatedSalikPenaltyAed: Int,
        val exposure: Exposure,
        val reason: String,
    )

    private val mediumCorridorKeywords = listOf(
        "sheikh zayed",
        "szr",
        "e11",
        "al ittihad",
        "al khail rd",
        "d68",
        "garhoud",
        "airport tunnel",
        "al safa",
        "barsha",
    )

    private val lowerExposureKeywords = listOf(
        "e311",
        "e611",
        "emirates road",
        "emirates rd",
        "mohammed bin zayed",
        "sheikh mohammed bin zayed",
        "mbz road",
    )

    fun estimate(corridorScanText: String): Estimate {
        val scan = corridorScanText.trim()
        if (scan.isEmpty()) {
            return Estimate(
                estimatedSalikPenaltyAed = 0,
                exposure = Exposure.LOW,
                reason = "empty corridorScanText",
            )
        }

        val lc = scan.lowercase(Locale.US)
        val tollRoadCount = countOccurrences(lc, "toll road")
        if (tollRoadCount > 0) {
            val penalty = tollRoadCount * AED_PER_TOLL_ROAD
            val exposure =
                when {
                    tollRoadCount >= 3 -> Exposure.HIGH
                    tollRoadCount >= 2 -> Exposure.HIGH
                    else -> Exposure.MEDIUM
                }
            return Estimate(
                estimatedSalikPenaltyAed = penalty,
                exposure = exposure,
                reason = "tollRoadCount=$tollRoadCount",
            )
        }

        val mediumHits = mediumCorridorKeywords.count { lc.contains(it) }
        val lowerHits = lowerExposureKeywords.count { lc.contains(it) }

        return when {
            lowerHits > 0 && mediumHits == 0 ->
                Estimate(
                    estimatedSalikPenaltyAed = 0,
                    exposure = Exposure.LOW,
                    reason = "lowerExposureKeywords=$lowerHits",
                )
            mediumHits > lowerHits ->
                Estimate(
                    estimatedSalikPenaltyAed = AED_PER_TOLL_ROAD,
                    exposure = Exposure.MEDIUM,
                    reason = "mediumCorridorKeywords=$mediumHits",
                )
            else ->
                Estimate(
                    estimatedSalikPenaltyAed = 0,
                    exposure = Exposure.LOW,
                    reason = "noSalikSignals medium=$mediumHits lower=$lowerHits",
                )
        }
    }

    internal fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var from = 0
        while (true) {
            val idx = haystack.indexOf(needle, from)
            if (idx == -1) return count
            count++
            from = idx + needle.length
        }
    }
}
