package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentityResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class MarshallUiAssemblyTest {

    @Test
    fun buildRecommendationSurfaceUiModel_prefersProviderDurationText() {
        val routes =
            listOf(
                route(durationText = "24 mins", durationSeconds = 1447),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)

        val model =
            buildRecommendationSurfaceUiModel(
                ready = true,
                loading = false,
                loadingMessage = "",
                routes = routes,
                recommendedRouteIndex = 0,
                routeIdentity = identities[0].primaryName,
                routeIdentities = identities,
                mode = PreferenceMode.FASTEST,
            )

        assertEquals("24 mins", model.travelTime)
    }

    @Test
    fun buildRecommendationSurfaceUiModel_fallsBackToFormatterWhenDurationTextBlank() {
        val routes =
            listOf(
                route(durationText = "", durationSeconds = 1447),
            )
        val identities = RouteIdentityResolver.resolveAll(routes)

        val model =
            buildRecommendationSurfaceUiModel(
                ready = true,
                loading = false,
                loadingMessage = "",
                routes = routes,
                recommendedRouteIndex = 0,
                routeIdentity = identities[0].primaryName,
                routeIdentities = identities,
                mode = PreferenceMode.FASTEST,
            )

        assertEquals("25 mins", model.travelTime)
    }

    private fun route(
        durationText: String,
        durationSeconds: Int,
    ): RealRouteDebugData =
        RealRouteDebugData(
            distanceText = "12 km",
            durationText = durationText,
            distanceMeters = 12_000,
            durationSeconds = durationSeconds,
            tollAED = 0,
            hasToll = false,
        )
}
