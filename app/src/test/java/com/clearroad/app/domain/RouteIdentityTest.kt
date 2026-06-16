package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.extractRouteLegsDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RouteIdentityTest {

    @Test
    fun canon_e11_fromSheikhZayedSummary() {
        val entry = UaeRoadCanon.matchHighestPriorityCanon("Sheikh Zayed Rd/E11")
        assertEquals("E11", entry?.stableKey)
        assertEquals("Sheikh Zayed Road (E11)", entry?.primaryName)
    }

    @Test
    fun canon_e311_fromMbzSummary() {
        val entry =
            UaeRoadCanon.matchHighestPriorityCanon("Sheikh Mohammed Bin Zayed Rd/E311")
        assertEquals("E311", entry?.stableKey)
        assertEquals("E311 (MBZ Road)", entry?.primaryName)
    }

    @Test
    fun canon_e611_fromEmiratesRoad() {
        val entry = UaeRoadCanon.matchHighestPriorityCanon("Emirates Rd/E611")
        assertEquals("E611", entry?.stableKey)
        assertEquals("Emirates Road (E611)", entry?.primaryName)
    }

    @Test
    fun canon_s120_fromSharjahLink() {
        val entry = UaeRoadCanon.matchHighestPriorityCanon("S120 and E11")
        assertEquals("E11", entry?.stableKey)
        assertTrue(UaeRoadCanon.findRoadCodes("S120 and E11").contains("S120"))
    }

    @Test
    fun canon_d94_fromMarinaSummary() {
        val entry =
            UaeRoadCanon.matchHighestPriorityCanon(
                "Sheikh Zayed Rd/E11 and King Salman Bin Abdulaziz Al Saud St/D94",
            )
        assertEquals("E11", entry?.stableKey)
        assertTrue(
            UaeRoadCanon.findRoadCodes(
                "King Salman Bin Abdulaziz Al Saud St/D94",
            ).contains("D94"),
        )
    }

    @Test
    fun resolve_singleE11Route() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Merge onto Sheikh Zayed Rd / E11 Toll road",
                    tollAed = 8,
                ),
            )
        assertEquals("E11", identity.stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identity.primaryName)
        assertEquals("Sheikh Zayed Road (E11)", identity.fullName)
        assertEquals(TollExposure.SALIK_AED, identity.tollExposure)
    }

    @Test
    fun resolve_e311_noSalikFare() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                ),
            )
        assertEquals("E311", identity.stableKey)
        assertEquals("E311 (MBZ Road)", identity.primaryName)
        assertEquals(TollExposure.NO_SALIK, identity.tollExposure)
    }

    @Test
    fun difcToMarina_threeE11Alternatives_disambiguate() {
        val identities = RouteIdentityResolver.resolveAll(Stage35Fixtures.difcToMarina())

        assertEquals("E11", identities[0].stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].primaryName)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].fullName)

        assertEquals("E11", identities[1].stableKey)
        assertEquals("via D94", identities[1].disambiguator)
        assertEquals("Sheikh Zayed Road (E11) · via D94", identities[1].fullName)

        assertEquals("E11", identities[2].stableKey)
        assertEquals("via D61", identities[2].disambiguator)
        assertEquals("Sheikh Zayed Road (E11) · via D61", identities[2].fullName)
    }

    @Test
    fun ajmanToDifc_e11_e311_e311viaE44() {
        val identities = RouteIdentityResolver.resolveAll(Stage35Fixtures.ajmanToDifc())

        assertEquals("E11", identities[0].stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].primaryName)

        assertEquals("E311", identities[1].stableKey)
        assertEquals("E311 (MBZ Road)", identities[1].primaryName)
        assertEquals("E311 (MBZ Road)", identities[1].fullName)

        assertEquals("E311", identities[2].stableKey)
        assertEquals("via E44", identities[2].disambiguator)
        assertEquals("E311 (MBZ Road) · via E44", identities[2].fullName)
    }

    @Test
    fun sharjahToDowntown_e11_e11viaS120_e311() {
        val identities = RouteIdentityResolver.resolveAll(Stage35Fixtures.sharjahToDowntown())

        assertEquals("E11", identities[0].stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].fullName)

        assertEquals("E11", identities[1].stableKey)
        assertEquals("via S120", identities[1].disambiguator)
        assertEquals("Sheikh Zayed Road (E11) · via S120", identities[1].fullName)

        assertEquals("E311", identities[2].stableKey)
        assertEquals("E311 (MBZ Road)", identities[2].fullName)
    }

    @Test
    fun jvcToAbuDhabi_e11Variants() {
        val identities = RouteIdentityResolver.resolveAll(Stage35Fixtures.jvcToAbuDhabi())

        assertEquals("E11", identities[0].stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].fullName)

        assertEquals("E11", identities[1].stableKey)
        assertEquals("via E10", identities[1].disambiguator)

        assertEquals("E11", identities[2].stableKey)
        assertEquals("via E77", identities[2].disambiguator)
    }

    @Test
    fun fixtureJson_difcMarina_parsesRouteSummaryAndIdentity() {
        val routes = loadFixture("difc-marina.json")
        assertEquals(3, routes.size)
        assertEquals("Sheikh Zayed Rd/E11", routes[0].routeSummary)

        val identities = RouteIdentityResolver.resolveAll(routes)
        assertEquals("Sheikh Zayed Road (E11)", identities[0].primaryName)
        assertEquals("Sheikh Zayed Road (E11) · via D94", identities[1].fullName)
    }

    @Test
    fun fixtureJson_ajmanDifc_identifiesE311() {
        val routes = loadFixture("ajman-difc.json")
        val identities = RouteIdentityResolver.resolveAll(routes)
        assertEquals("E311", identities[1].stableKey)
        assertEquals("E311 (MBZ Road)", identities[1].primaryName)
    }

    @Test
    fun fixtureJson_sharjahDowntown_identifiesS120Disambiguator() {
        val routes = loadFixture("sharjah-downtown.json")
        val identities = RouteIdentityResolver.resolveAll(routes)
        assertEquals("via S120", identities[1].disambiguator)
    }

    @Test
    fun unknownWithNonEmptyGoogleSummary_usesSummaryAsDisplayName() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "Sheikh Khalifa Bin Zayed St",
                    scan = "Merge onto Sheikh Zayed Rd / E11 Head south",
                    distanceMeters = 9_400,
                ),
            )

        assertEquals(UaeRoadCanon.UNKNOWN_STABLE_KEY, identity.stableKey)
        assertEquals("Sheikh Khalifa Bin Zayed St", identity.primaryName)
        assertEquals("Sheikh Khalifa Bin Zayed St", identity.fullName)
    }

    @Test
    fun unknownWithEmptyGoogleSummary_keepsUaeRouteFallback() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "",
                    scan = "",
                    distanceMeters = 9_400,
                ),
            )

        assertEquals(UaeRoadCanon.UNKNOWN_STABLE_KEY, identity.stableKey)
        assertEquals("UAE Route", identity.primaryName)
    }

    @Test
    fun e11Summary_stillReturnsSheikhZayedRoadCanon() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Merge onto Sheikh Zayed Rd / E11 Toll road",
                ),
            )

        assertEquals("E11", identity.stableKey)
        assertEquals("Sheikh Zayed Road (E11)", identity.primaryName)
    }

    @Test
    fun shortLocalRouteWithUsefulSummary_doesNotBecomeUaeRoute() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "شارع الكورنيش",
                    scan = "Sheikh Zayed Rd local connector",
                    distanceMeters = 6_200,
                ),
            )

        assertEquals("شارع الكورنيش", identity.primaryName)
        assertEquals(UaeRoadCanon.UNKNOWN_STABLE_KEY, identity.stableKey)
    }

    @Test
    fun shortLocalTrip_scanMentionsAirport_doesNotUseAirportApproachDisambiguator() {
        val routes =
            listOf(
                route(
                    summary = "King Faisal St",
                    scan = "King Faisal St Head south signs for airport merge local street",
                    distanceMeters = 8_700,
                ),
                route(
                    summary = "King Faisal St",
                    scan = "King Faisal St Head north local street",
                    distanceMeters = 8_900,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        identities.forEach { identity ->
            assertTrue(
                identity.fullName.contains("Airport approach").not(),
            )
        }
    }

    @Test
    fun shortLocalTrip_e11OnlyFromSummary_notFromScan() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "King Faisal St",
                    scan = "King Faisal St Merge onto Sheikh Zayed Rd / E11 for 200 m Turn left",
                    distanceMeters = 6_200,
                ),
            )

        assertFalse(identity.primaryName.contains("Sheikh Zayed Road"))
        assertEquals("King Faisal St", identity.primaryName)
    }

    @Test
    fun scanMentionsAbuDhabiOnSharjahTrip_doesNotBecomeAbuDhabiLocalStreets() {
        val identity =
            RouteIdentityResolver.resolve(
                route(
                    summary = "Al Wahda St",
                    scan = "Al Wahda St Continue through Sharjah toward Abu Dhabi connector",
                    distanceMeters = 14_000,
                ),
            )
        assertEquals("Sharjah Urban Route", identity.primaryName)
    }

    private fun route(
        summary: String,
        scan: String,
        tollAed: Int = 0,
        distanceMeters: Int = 20_000,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceMeters / 1000.0} km",
            durationText = "24 min",
            distanceMeters = distanceMeters,
            durationSeconds = 1440,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            routeSummary = summary,
            corridorScanText = scan,
        )

    private fun loadFixture(name: String): List<RealRouteDebugData> {
        val file = File("../docs/stage-35-0-audit/$name")
        assertTrue("Fixture missing: ${file.absolutePath}", file.exists())
        return extractRouteLegsDebugData(file.readText())
    }
}

private object Stage35Fixtures {

    fun difcToMarina(): List<RealRouteDebugData> =
        listOf(
            peakRoute(
                summary = "Sheikh Zayed Rd/E11",
                scan = "Sheikh Zayed Rd/E11 Head northeast Merge onto Sheikh Zayed Rd / E11 Toll road",
                tollAed = 8,
            ),
            peakRoute(
                summary = "Sheikh Zayed Rd/E11 and King Salman Bin Abdulaziz Al Saud St/D94",
                scan =
                    "Sheikh Zayed Rd/E11 and King Salman Bin Abdulaziz Al Saud St/D94 " +
                        "Merge onto Sheikh Zayed Rd / E11 Toll road King Salman Bin Abdulaziz Al Saud St / D94",
                tollAed = 8,
            ),
            peakRoute(
                summary =
                    "Sheikh Zayed Rd/E11, Al Naseem St/D61 and King Salman Bin Abdulaziz Al Saud St/D94",
                scan =
                    "Sheikh Zayed Rd/E11, Al Naseem St/D61 and King Salman Bin Abdulaziz Al Saud St/D94 " +
                        "Al Naseem St / D61 King Salman Bin Abdulaziz Al Saud St / D94",
                tollAed = 8,
            ),
        )

    fun ajmanToDifc(): List<RealRouteDebugData> =
        listOf(
            peakRoute(summary = "E11", scan = "E11 Merge onto E11 Toll road", tollAed = 12),
            peakRoute(
                summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                scan = "Sheikh Mohammed Bin Zayed Rd/E311 Sheikh Mohammed Bin Zayed Rd / E311",
                tollAed = 4,
            ),
            peakRoute(
                summary = "Sheikh Mohammed Bin Zayed Rd/E311 and Ras Al Khor Rd/E44",
                scan =
                    "Sheikh Mohammed Bin Zayed Rd/E311 and Ras Al Khor Rd/E44 " +
                        "Sheikh Mohammed Bin Zayed Rd / E311 Ras Al Khor Rd / E44",
                tollAed = 4,
            ),
        )

    fun sharjahToDowntown(): List<RealRouteDebugData> =
        listOf(
            peakRoute(summary = "E11", scan = "E11 Merge onto E11 Toll road", tollAed = 16),
            peakRoute(
                summary = "S120 and E11",
                scan = "S120 and E11 University City Rd / S120 Merge onto E11 Toll road",
                tollAed = 16,
            ),
            peakRoute(
                summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                scan = "Sheikh Mohammed Bin Zayed Rd/E311 Sheikh Mohammed Bin Zayed Rd / E311",
                tollAed = 4,
            ),
        )

    fun jvcToAbuDhabi(): List<RealRouteDebugData> =
        listOf(
            peakRoute(summary = "E11", scan = "E11 Sheikh Zayed Rd / E11", tollAed = 0),
            peakRoute(
                summary = "E11 and Al Shahama - Abu Dhabi Rd/Sheikh Zayed Bin Sultan St/E10",
                scan =
                    "E11 and Al Shahama - Abu Dhabi Rd/Sheikh Zayed Bin Sultan St/E10 " +
                        "Sheikh Zayed Rd / E11 Al Shahama - Abu Dhabi Rd / E10",
                tollAed = 4,
            ),
            peakRoute(
                summary = "Expo Rd/E77 and E11",
                scan = "Expo Rd/E77 and E11 Expo Rd / E77 Sheikh Zayed Rd / E11",
                tollAed = 0,
            ),
        )

    private fun peakRoute(
        summary: String,
        scan: String,
        tollAed: Int,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "30 km",
            durationText = "30 min",
            distanceMeters = 30_000,
            durationSeconds = 1800,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            routeSummary = summary,
            corridorScanText = scan,
        )
}
