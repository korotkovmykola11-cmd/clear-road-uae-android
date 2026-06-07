package com.clearroad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SalikDetectionTest {

    @Test
    fun tollRoadCountTimesFourAed() {
        val scan =
            "Sheikh Zayed Rd/E11 Merge onto Sheikh Zayed Rd / E11 Toll road " +
                "Continue on Sheikh Zayed Rd / E11 Toll road"
        val estimate = SalikDetection.estimate(scan)
        assertEquals(8, estimate.estimatedSalikPenaltyAed)
        assertEquals(SalikDetection.Exposure.HIGH, estimate.exposure)
    }

    @Test
    fun e311OnlyRouteIsLowExposure() {
        val scan =
            "Sheikh Mohammed Bin Zayed Rd/E311 " +
                "Take the ramp onto Sheikh Mohammed Bin Zayed Rd / E311"
        val estimate = SalikDetection.estimate(scan)
        assertEquals(0, estimate.estimatedSalikPenaltyAed)
        assertEquals(SalikDetection.Exposure.LOW, estimate.exposure)
    }

    @Test
    fun e11WithoutTollRoadLabelGetsMediumPenalty() {
        val scan = "E11 Turn left onto Al Ittihad Rd / Dubai - Sharjah Rd / E11"
        val estimate = SalikDetection.estimate(scan)
        assertEquals(4, estimate.estimatedSalikPenaltyAed)
        assertEquals(SalikDetection.Exposure.MEDIUM, estimate.exposure)
    }

    @Test
    fun difcToMarina_e11Direct_beatsE44OnSaveAedScoring() {
        val e11Direct =
            corridorLike(
                durationMin = 25,
                distanceKm = 24.6,
                scan = "Sheikh Zayed Rd/E11 Sheikh Zayed Rd / E11 Toll road",
                tollRoadCount = 2,
            )
        val e44Via =
            corridorLike(
                durationMin = 32,
                distanceKm = 29.0,
                scan = "Al Khail Rd/E44 Al Khail Rd / E44 then E11 Toll road",
                tollRoadCount = 1,
            )
        val routes = listOf(e11Direct, e44Via)
        assertEquals(0, pickIndex(routes, PreferenceMode.FASTEST))
        assertEquals(1, pickIndex(routes, PreferenceMode.NO_TOLLS))
    }

    @Test
    fun marinaToSharjah_e311LowerPenaltyThanE11TollRoad() {
        val e11 =
            corridorLike(
                durationMin = 41,
                distanceKm = 46.6,
                scan = "E11 Sheikh Zayed Rd / E11 Toll road Al Ittihad Rd / E11 Toll road",
                tollRoadCount = 3,
            )
        val e311 =
            corridorLike(
                durationMin = 51,
                distanceKm = 65.4,
                scan = "Sheikh Mohammed Bin Zayed Rd/E311 ramp onto E311",
                tollRoadCount = 0,
            )
        val savePick = pickIndex(listOf(e11, e311), PreferenceMode.NO_TOLLS)
        assertEquals(1, savePick)
    }

    @Test
    fun ajmanToDubaiMall_convergesWhenSalikExposureEqual() {
        val routes =
            listOf(
                corridorLike(
                    durationMin = 39,
                    distanceKm = 37.6,
                    scan = "E11 Sheikh Zayed Rd / E11",
                    tollRoadCount = 1,
                ),
                corridorLike(
                    durationMin = 41,
                    distanceKm = 42.6,
                    scan = "E11 Sheikh Zayed Rd alternative",
                    tollRoadCount = 1,
                ),
            )
        assertEquals(0, pickIndex(routes, PreferenceMode.FASTEST))
        assertEquals(0, pickIndex(routes, PreferenceMode.NO_TOLLS))
        assertEquals(0, pickIndex(routes, PreferenceMode.CALM))
    }

    private data class ProbeRoute(
        val durationMin: Int,
        val distanceKm: Double,
        val googleTollAed: Int,
        val corridorScanText: String,
    )

    private fun corridorLike(
        durationMin: Int,
        distanceKm: Double,
        scan: String,
        tollRoadCount: Int,
    ): ProbeRoute {
        val tollPhrase = " Toll road".repeat(tollRoadCount).trim()
        return ProbeRoute(
            durationMin = durationMin,
            distanceKm = distanceKm,
            googleTollAed = 0,
            corridorScanText = "$scan $tollPhrase".trim(),
        )
    }

    private fun effectiveToll(route: ProbeRoute): Int {
        if (route.googleTollAed > 0) return route.googleTollAed
        return SalikDetection.estimate(route.corridorScanText).estimatedSalikPenaltyAed
    }

    private fun scoreRoute(route: ProbeRoute, mode: PreferenceMode): Double {
        val fuelAed = kotlin.math.round((route.distanceKm / 12.0) * 2.8).toInt()
        val toll = effectiveToll(route).toDouble()
        val total = (toll + fuelAed).toDouble()
        val dur = route.durationMin
        val dist = route.distanceKm
        val apm = if (dur > 0) total / dur else total
        return when (mode) {
            PreferenceMode.FASTEST ->
                dur + total * 0.15 + dist * 0.05 + apm * 0.2
            PreferenceMode.NO_TOLLS ->
                total * 4.0 + toll * 6.0 + dur * 0.35 + apm * 0.4
            PreferenceMode.CALM ->
                dur * 0.6 + total * 1.2 + dist * 0.15 + apm * 0.3
        }
    }

    private fun pickIndex(routes: List<ProbeRoute>, mode: PreferenceMode): Int {
        var bestScore = Double.MAX_VALUE
        var bestIdx = 0
        routes.forEachIndexed { index, route ->
            val s = scoreRoute(route, mode)
            if (s < bestScore || (s == bestScore && index < bestIdx)) {
                bestScore = s
                bestIdx = index
            }
        }
        return bestIdx
    }
}
