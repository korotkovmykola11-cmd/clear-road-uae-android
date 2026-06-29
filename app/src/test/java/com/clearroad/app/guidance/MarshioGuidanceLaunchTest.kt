package com.clearroad.app.guidance

import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarshioGuidanceLaunchTest {

    @Test
    fun showEntryInRouteDetails_visibleInDebugWhenMarshioPathExists() {
        val model = sampleRouteDetailsModel(showMarshioGuidanceEntry = true)

        assertTrue(MarshioGuidanceLaunch.showEntryInRouteDetails(isDebugBuild = true, model = model))
        assertFalse(MarshioGuidanceLaunch.showEntryInRouteDetails(isDebugBuild = false, model = model))
    }

    @Test
    fun showEntryInRouteDetails_hiddenWithoutMarshioPath() {
        val model =
            sampleRouteDetailsModel(
                showMarshioGuidanceEntry = false,
                handoffRoutePathPoints = emptyList(),
            )

        assertFalse(MarshioGuidanceLaunch.showEntryInRouteDetails(isDebugBuild = true, model = model))
    }

    @Test
    fun argsFromRouteDetails_usesSelectedMarshioRouteNotGoogleDefault() {
        val marshioPath =
            listOf(
                LatLng(25.0000, 55.0000),
                LatLng(25.0100, 55.0100),
            )
        val googlePath =
            listOf(
                LatLng(25.0000, 55.0000),
                LatLng(25.0200, 55.0300),
            )
        val model =
            sampleRouteDetailsModel(
                handoffRoutePathPoints = marshioPath,
                googleDefaultRoutePathPoints = googlePath,
                durationSeconds = 540,
                durationText = "9 min",
                distanceText = "3.0 km",
                routeIdentityTitle = "Marshio Pick Rd",
                selectedRouteIndex = 1,
            )

        val args = MarshioGuidanceLaunch.argsFromRouteDetails(model)

        assertNotNull(args)
        assertEquals(marshioPath, args?.marshioPath)
        assertEquals(googlePath, args?.googlePath)
        assertEquals("9 min", args?.durationText)
        assertEquals("3.0 km", args?.distanceText)
        assertEquals(540, args?.durationSeconds)
        assertEquals("Marshio Pick Rd", args?.routeName)
        assertEquals(1, args?.selectedRouteIndex)
        assertEquals(marshioPath.last(), args?.marshioPath?.last())
    }

    @Test
    fun argsToSession_preservesGuidanceFields() {
        val args =
            MarshioGuidanceLaunch.Args(
                fromLatLng = LatLng(25.0, 55.0),
                toLatLng = LatLng(25.1, 55.1),
                routeName = "Route 2",
                durationText = "9 min",
                distanceText = "3.0 km",
                durationSeconds = 540,
                selectedRouteIndex = 1,
                marshioPath = listOf(LatLng(25.0, 55.0), LatLng(25.05, 55.05)),
                googlePath = listOf(LatLng(25.0, 55.0), LatLng(25.06, 55.06)),
            )

        val session = args.toSession()

        assertEquals(args.routeName, session.routeName)
        assertEquals(args.durationSeconds, session.durationSeconds)
        assertEquals(args.marshioPath, session.marshioPath)
        assertEquals(args.googlePath, session.googlePath)
        assertEquals(args.selectedRouteIndex, session.selectedRouteIndex)
    }

    private fun sampleRouteDetailsModel(
        showMarshioGuidanceEntry: Boolean = true,
        selectedRouteIndex: Int = 0,
        handoffRoutePathPoints: List<LatLng> =
            listOf(
                LatLng(25.0, 55.0),
                LatLng(25.01, 55.01),
            ),
        googleDefaultRoutePathPoints: List<LatLng> = emptyList(),
        durationSeconds: Int = 480,
        durationText: String = "8 min",
        distanceText: String = "3.1 km",
        routeIdentityTitle: String = "Route 1",
    ): RouteDetailsUiModel =
        RouteDetailsUiModel(
            routeNumber = 1,
            routeIdentityTitle = routeIdentityTitle,
            routeReasonTitle = "Reason",
            routeReasonWhy = "Why",
            durationText = durationText,
            distanceText = distanceText,
            tollAed = 0,
            confidenceLabel = "Typical",
            costSummaryPrimary = "Primary",
            costSummarySecondary = null,
            recommendationConfidenceText = "",
            fromLatLng = LatLng(25.0, 55.0),
            toLatLng = LatLng(25.1, 55.1),
            handoffRoutePathPoints = handoffRoutePathPoints,
            googleDefaultRoutePathPoints = googleDefaultRoutePathPoints,
            durationSeconds = durationSeconds,
            selectedRouteIndex = selectedRouteIndex,
            showMarshioGuidanceEntry = showMarshioGuidanceEntry,
            mode = PreferenceMode.FASTEST,
        )
}
