package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteIdentityPresentationPolicyTest {

    @Test
    fun shortEquivalentRoute_8to9km_homeLineEmpty() {
        val routes =
            listOf(
                localRoute(
                    summary = "King Faisal St",
                    scan = "King Faisal St Head south Turn right",
                    distanceMeters = 8_700,
                    durationSeconds = 12 * 60,
                ),
                localRoute(
                    summary = "King Faisal St",
                    scan = "King Faisal St Head south Turn left",
                    distanceMeters = 8_800,
                    durationSeconds = 12 * 60 + 20,
                ),
                localRoute(
                    summary = "King Faisal St",
                    scan = "King Faisal St Continue straight",
                    distanceMeters = 8_600,
                    durationSeconds = 12 * 60 + 30,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val display =
            RouteIdentityPresentationPolicy.displayForHome(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            )

        assertEquals("", display.homeLine)
        assertEquals(
            RouteIdentityPresentationPolicy.RouteIdentityVisibility.SUPPRESSED,
            display.visibility,
        )
        assertFalse(RouteIdentityPresentationPolicy.shouldShowOnHome(routes, identities, 0))
    }

    @Test
    fun shortEquivalentRoute_5to6km_homeLineEmpty() {
        val routes =
            listOf(
                localRoute(
                    summary = "Al Nuaimiya St",
                    scan = "Al Nuaimiya St Head east",
                    distanceMeters = 5_700,
                    durationSeconds = 10 * 60,
                ),
                localRoute(
                    summary = "Al Nuaimiya St",
                    scan = "Al Nuaimiya St Head northeast",
                    distanceMeters = 6_100,
                    durationSeconds = 11 * 60,
                ),
                localRoute(
                    summary = "Al Nuaimiya St",
                    scan = "Al Nuaimiya St Turn right",
                    distanceMeters = 5_900,
                    durationSeconds = 10 * 60 + 30,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val display =
            RouteIdentityPresentationPolicy.displayForHome(
                routes = routes,
                identities = identities,
                recommendedIndex = 1,
            )

        assertEquals("", display.homeLine)
    }

    @Test
    fun longRouteWithSalikTradeoff_homeLineVisible() {
        val routes =
            listOf(
                localRoute(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Merge onto Sheikh Zayed Rd / E11 Toll road",
                    distanceMeters = 29_700,
                    durationSeconds = 27 * 60,
                    tollAed = 16,
                ),
                localRoute(
                    summary = "Sheikh Zayed Rd/E11",
                    scan = "Sheikh Zayed Rd/E11 Merge onto Sheikh Zayed Rd / E11 Toll road",
                    distanceMeters = 31_700,
                    durationSeconds = 30 * 60,
                    tollAed = 16,
                ),
                localRoute(
                    summary = "Sheikh Mohammed Bin Zayed Rd/E311",
                    scan = "Sheikh Mohammed Bin Zayed Rd/E311",
                    distanceMeters = 27_400,
                    durationSeconds = 35 * 60,
                    tollAed = 4,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val display =
            RouteIdentityPresentationPolicy.displayForHome(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            )

        assertTrue(display.homeLine.isNotBlank())
        assertEquals("Sheikh Zayed Road (E11)", display.homeLine)
        assertTrue(RouteIdentityPresentationPolicy.shouldShowOnHome(routes, identities, 0))
    }

    @Test
    fun shortRoute_e11OnlyInScan_notPromotedToSheikhZayedRoad() {
        val route =
            localRoute(
                summary = "King Faisal St",
                scan =
                    "Ajman King Faisal St Head south " +
                        "Merge onto Sheikh Zayed Rd / E11 for 200 m " +
                        "Turn left onto local street",
                distanceMeters = 6_200,
                durationSeconds = 11 * 60,
            )
        val identity = RouteIdentityResolver.resolve(route)

        assertFalse(identity.primaryName.contains("Sheikh Zayed Road"))
        assertEquals("Ajman local route", identity.primaryName)
        assertEquals("URBAN-AJM", identity.stableKey)
    }

    @Test
    fun detailsShowsHonestTitleWhenHomeSuppressesIdentity() {
        val routes =
            listOf(
                localRoute(
                    summary = "University City Rd/S120",
                    scan = "University City Rd/S120 Head west",
                    distanceMeters = 8_200,
                    durationSeconds = 12 * 60,
                ),
                localRoute(
                    summary = "University City Rd/S120",
                    scan = "University City Rd/S120 Turn right",
                    distanceMeters = 8_400,
                    durationSeconds = 12 * 60 + 15,
                ),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)
        val home =
            RouteIdentityPresentationPolicy.displayForHome(
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            )
        val details =
            RouteIdentityPresentationPolicy.displayForDetails(
                route = routes[0],
                identity = identities[0],
                routes = routes,
                identities = identities,
                recommendedIndex = 0,
            )

        assertEquals("", home.homeLine)
        assertEquals("University City Rd/S120", details.detailsTitle)
    }

    private fun localRoute(
        summary: String,
        scan: String,
        distanceMeters: Int,
        durationSeconds: Int,
        tollAed: Int = 0,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "${distanceMeters / 1000.0} km",
            durationText = "${durationSeconds / 60} mins",
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            tollAED = tollAed,
            hasToll = tollAed > 0,
            routeSummary = summary,
            corridorScanText = scan,
            baseDurationSeconds = durationSeconds,
            durationInTrafficSeconds = durationSeconds,
        )
}
