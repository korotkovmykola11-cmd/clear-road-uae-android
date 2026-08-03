package com.clearroad.app.benchmark

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StageCBaselineCaptureTest {

    @Test
    fun explainLiveAjmanDifcCalmFork_e11VsE311Motorway_isExplainable() {
        val legacyRoutes = listOf(motorwayRoute("E11 Sheikh Zayed Rd"))
        val v2Routes = listOf(motorwayRoute("E311 Mohammed Bin Zayed Rd"))
        val legacy =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E11",
                geomFp = "aaa",
                tollAed = 0,
                durationSec = 2800,
                distanceM = 90000,
            )
        val v2 =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E311",
                geomFp = "bbb",
                tollAed = 0,
                durationSec = 2900,
                distanceM = 92000,
            )

        assertTrue(
            StageCBaselineCapture.explainLiveAjmanDifcCalmFork(
                caseId = "live-ajman-difc",
                legacy = legacy,
                v2 = v2,
                legacyRoutes = legacyRoutes,
                v2Routes = v2Routes,
            ),
        )
    }

    @Test
    fun explainLiveAjmanDifcCalmFork_otherCaseId_isNotExplainable() {
        val legacyRoutes = listOf(motorwayRoute("E11 Sheikh Zayed Rd"))
        val v2Routes = listOf(motorwayRoute("E311 Mohammed Bin Zayed Rd"))
        val legacy =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E11",
                geomFp = "aaa",
                tollAed = 0,
                durationSec = 2800,
                distanceM = 90000,
            )
        val v2 =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E311",
                geomFp = "bbb",
                tollAed = 0,
                durationSec = 2900,
                distanceM = 92000,
            )

        assertFalse(
            StageCBaselineCapture.explainLiveAjmanDifcCalmFork(
                caseId = "stage34-route4",
                legacy = legacy,
                v2 = v2,
                legacyRoutes = legacyRoutes,
                v2Routes = v2Routes,
            ),
        )
    }

    @Test
    fun compareWinners_liveAjmanDifcCalmFork_classifiesExplainable() {
        val legacyRoutes = listOf(motorwayRoute("E11 Sheikh Zayed Rd"))
        val v2Routes = listOf(motorwayRoute("E311 Mohammed Bin Zayed Rd"))
        val legacyPath =
            listOf(
                LatLng(25.40, 55.51),
                LatLng(25.35, 55.45),
                LatLng(25.21, 55.28),
            )
        val v2Path =
            listOf(
                LatLng(25.40, 55.51),
                LatLng(25.38, 55.40),
                LatLng(25.21, 55.28),
            )
        val legacy =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E11",
                geomFp = "aaa",
                tollAed = 0,
                durationSec = 2800,
                distanceM = 90000,
            )
        val v2 =
            StageCBaselineCapture.ModeWinnerSnapshot(
                index = 0,
                corridor = "E311",
                geomFp = "bbb",
                tollAed = 0,
                durationSec = 2900,
                distanceM = 92000,
            )

        val result =
            StageCBaselineCapture.compareWinners(
                caseId = "live-ajman-difc",
                legacy = legacy,
                v2 = v2,
                mode = PreferenceMode.CALM,
                allLegacyRoutes = legacyRoutes,
                allV2Routes = v2Routes,
                legacyWinnerPath = legacyPath,
                v2WinnerPath = v2Path,
            )

        assertTrue(result == StageCBaselineCapture.ComparisonResult.EXPLAINABLE_DIFFERENCE)
    }

    private fun motorwayRoute(summary: String): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "90 km",
            durationText = "45 min",
            distanceMeters = 90000,
            durationSeconds = 2700,
            tollAED = 0,
            hasToll = false,
            routeSummary = summary,
            corridorScanText = "$summary motorway e11 e311",
        )
}
