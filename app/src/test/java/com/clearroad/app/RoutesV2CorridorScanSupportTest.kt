package com.clearroad.app

import com.clearroad.app.benchmark.RoutesV2ResponseAdapterTestFixtures
import com.clearroad.app.domain.SmoothDriveScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesV2CorridorScanSupportTest {

    @Test
    fun normalizeEmiratesRoadCodes_mapsArabicIndicAndLatinCodes() {
        assertEquals(
            "e11 e311",
            RoutesV2CorridorScanSupport.normalizeEmiratesRoadCodes("إ11 E311"),
        )
    }

    @Test
    fun enrichCorridorScanText_appendsMotorwayTokensFromArabicDescription() {
        val enriched =
            RoutesV2CorridorScanSupport.enrichCorridorScanText(
                routeSummary = "Ø´Ø§Ø±Ø¹ Ø§Ù„Ø´ÙŠØ® Ø²Ø§ÙŠØ¯/Ø¥11",
                stepInstructions = "local approach street",
            )
        assertTrue(enriched.contains("e11"))
        assertTrue(enriched.contains("sheikh zayed"))
        assertEquals(
            SmoothDriveScoring.CorridorClass.MOTORWAY,
            SmoothDriveScoring.classifyCorridor(enriched),
        )
    }

    @Test
    fun v2DifcMarinaFixture_route0_classifiesAsMotorway() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-difc-marina-traffic.json"),
            )
        val route = routes.first()
        assertEquals(
            SmoothDriveScoring.CorridorClass.MOTORWAY,
            SmoothDriveScoring.classifyCorridor(route.corridorScanText),
        )
    }

    @Test
    fun v2SharjahFixture_allRoutes_classifyAsMotorwayOrMixedNotUrbanWeaveOnly() {
        val routes =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json"),
            )
        routes.forEach { route ->
            val corridorClass = SmoothDriveScoring.classifyCorridor(route.corridorScanText)
            assertTrue(
                "route corridorScan=${route.corridorScanText.take(120)} class=$corridorClass",
                corridorClass == SmoothDriveScoring.CorridorClass.MOTORWAY ||
                    corridorClass == SmoothDriveScoring.CorridorClass.MIXED,
            )
        }
    }
}
