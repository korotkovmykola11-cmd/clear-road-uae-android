package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRecommendationSelectionTest {

    @Test
    fun fastest_usesLegacyScoring_notSmoothDrive() {
        val routes =
            listOf(
                route(
                    durationSec = 1500,
                    distanceM = 24_000,
                    scan = "E11 Toll road",
                ),
                route(
                    durationSec = 1800,
                    distanceM = 20_000,
                    scan = "E311",
                ),
            )
        assertEquals(0, RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.FASTEST))
    }

    @Test
    fun saveAed_usesLegacyScoring_notSmoothDrive() {
        val routes =
            listOf(
                route(
                    durationSec = 1500,
                    distanceM = 24_600,
                    scan = "Sheikh Zayed Rd/E11 Sheikh Zayed Rd / E11 Toll road Toll road",
                ),
                route(
                    durationSec = 1920,
                    distanceM = 29_000,
                    scan = "Al Khail Rd/E44 Al Khail Rd / E44 then E11 Toll road",
                ),
            )
        assertEquals(1, RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.NO_TOLLS))
    }

    @Test
    fun calm_peakDifcToMarina_picksSmoothWinnerIdx2() {
        assertEquals(
            2,
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                Stage353PeakRoutes.difcToMarina(),
                PreferenceMode.CALM,
            ),
        )
    }

    @Test
    fun calm_peakAjmanToDifc_picksSmoothWinnerIdx1() {
        assertEquals(
            1,
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                Stage353PeakRoutes.ajmanToDifc(),
                PreferenceMode.CALM,
            ),
        )
    }

    @Test
    fun calm_peakSharjahToDowntown_picksSmoothWinnerIdx2() {
        assertEquals(
            2,
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                Stage353PeakRoutes.sharjahToDowntown(),
                PreferenceMode.CALM,
            ),
        )
    }

    @Test
    fun calm_peakJvcToAbuDhabi_picksSmoothWinnerIdx2() {
        assertEquals(
            2,
            RouteRecommendationSelection.pickRecommendedRouteIndex(
                Stage353PeakRoutes.jvcToAbuDhabi(),
                PreferenceMode.CALM,
            ),
        )
    }

    @Test
    fun calm_fallsBackToLegacyWhenTrafficDataMissing() {
        val routes =
            listOf(
                RealRouteDebugData(
                    distanceText = "10 km",
                    durationText = "0 min",
                    distanceMeters = 10_000,
                    durationSeconds = 0,
                    tollAED = 0,
                    hasToll = false,
                    corridorScanText = "E11",
                ),
                route(durationSec = 1200, distanceM = 10_000, scan = "E311"),
            )
        val index =
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.CALM)
        assertTrue(index in routes.indices)
    }

    @Test
    fun calm_emptyRoutes_returnsZero() {
        assertEquals(0, RouteRecommendationSelection.pickRecommendedRouteIndex(emptyList(), PreferenceMode.CALM))
    }

    private fun route(
        durationSec: Int,
        distanceM: Int,
        scan: String,
        baseSec: Int? = null,
        trafficSec: Int? = null,
        tollAed: Int = 0,
    ): RealRouteDebugData {
        val base = baseSec ?: durationSec
        val traffic = trafficSec ?: durationSec
        return RealRouteDebugData(
            distanceText = "${distanceM / 1000} km",
            durationText = "${durationSec / 60} min",
            distanceMeters = distanceM,
            durationSeconds = durationSec,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            corridorScanText = scan,
            baseDurationText = "${base / 60} min",
            baseDurationSeconds = base,
            durationInTrafficText = "${traffic / 60} min",
            durationInTrafficSeconds = traffic,
        )
    }
}

/** Peak-traffic metrics from Stage 35.0 audit (`docs/stage-35-0-audit/`). */
private object Stage353PeakRoutes {

    fun difcToMarina(): List<RealRouteDebugData> =
        listOf(
            peakRoute(1458, 1663, 24_300, "Sheikh Zayed Rd/E11"),
            peakRoute(1509, 1664, 26_800, "Sheikh Zayed Rd/E11 and King Salman Bin Abdulaziz Al Saud St/D94"),
            peakRoute(1710, 1771, 24_700, "Sheikh Zayed Rd/E11, Al Naseem St/D61 and King Salman Bin Abdulaziz Al Saud St/D94"),
        )

    fun ajmanToDifc(): List<RealRouteDebugData> =
        listOf(
            peakRoute(2474, 2908, 37_600, "E11"),
            peakRoute(2612, 2845, 46_300, "Sheikh Mohammed Bin Zayed Rd/E311"),
            peakRoute(2717, 2963, 48_200, "Sheikh Mohammed Bin Zayed Rd/E311 and Ras Al Khor Rd/E44"),
        )

    fun sharjahToDowntown(): List<RealRouteDebugData> =
        listOf(
            peakRoute(1974, 2044, 29_800, "E11"),
            peakRoute(1956, 2155, 27_700, "S120 and E11"),
            peakRoute(2047, 2104, 33_100, "Sheikh Mohammed Bin Zayed Rd/E311"),
        )

    fun jvcToAbuDhabi(): List<RealRouteDebugData> =
        listOf(
            peakRoute(4228, 4534, 102_700, "E11"),
            peakRoute(4353, 4594, 106_900, "E11 and Al Shahama - Abu Dhabi Rd/Sheikh Zayed Bin Sultan St/E10"),
            peakRoute(4812, 4981, 116_800, "Expo Rd/E77 and E11"),
        )

    private fun peakRoute(
        baseSec: Int,
        trafficSec: Int,
        distanceM: Int,
        corridor: String,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceM / 1000} km",
            durationText = "${trafficSec / 60} min",
            distanceMeters = distanceM,
            durationSeconds = trafficSec,
            tollAED = 0,
            hasToll = false,
            corridorScanText = corridor,
            baseDurationText = "${baseSec / 60} min",
            baseDurationSeconds = baseSec,
            durationInTrafficText = "${trafficSec / 60} min",
            durationInTrafficSeconds = trafficSec,
        )
}
