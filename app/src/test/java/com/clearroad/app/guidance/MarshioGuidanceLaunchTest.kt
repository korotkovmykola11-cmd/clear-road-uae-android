package com.clearroad.app.guidance

import com.clearroad.app.DirectionsStepRecord
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
    fun showEntryInRouteDetails_visibleWhenMarshioPathExists() {
        val model = sampleRouteDetailsModel(showMarshioGuidanceEntry = true)

        assertTrue(MarshioGuidanceLaunch.showEntryInRouteDetails(model = model))
    }

    @Test
    fun showEntryInRouteDetails_hiddenWhenNotRecommended() {
        val model = sampleRouteDetailsModel(showMarshioGuidanceEntry = false)

        assertFalse(MarshioGuidanceLaunch.showEntryInRouteDetails(model = model))
    }

    @Test
    fun showEntryInRouteDetails_hiddenWithoutMarshioPath() {
        val model =
            sampleRouteDetailsModel(
                showMarshioGuidanceEntry = false,
                handoffRoutePathPoints = emptyList(),
            )

        assertFalse(MarshioGuidanceLaunch.showEntryInRouteDetails(model = model))
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
        assertEquals(args.steps, session.steps)
    }

    @Test
    fun stepsJsonRoundTrip_preservesGuidanceSteps() {
        val steps =
            listOf(
                GuidanceStepUi(
                    instruction = "Continue on Sheikh Zayed Rd",
                    distanceMeters = 500,
                    startLatLng = 25.2048 to 55.2708,
                ),
                GuidanceStepUi(
                    instruction = "Turn right",
                    distanceMeters = 120,
                    startLatLng = null,
                ),
            )

        val json = MarshioGuidanceLaunch.encodeStepsJson(steps)

        assertEquals(steps, MarshioGuidanceLaunch.decodeStepsJson(json))
    }

    @Test
    fun argsFromRouteDetails_mapsGoogleStepsToGuidanceSteps() {
        val googleSteps =
            listOf(
                DirectionsStepRecord(
                    distanceMeters = 500,
                    maneuver = "straight",
                    htmlInstructions = "<b>Continue on Sheikh Zayed Rd</b>",
                    startLocation = LatLng(25.2048, 55.2708),
                ),
            )
        val model = sampleRouteDetailsModel()

        val args = MarshioGuidanceLaunch.argsFromRouteDetails(model, googleSteps = googleSteps)

        assertNotNull(args)
        assertEquals(1, args?.steps?.size)
        assertEquals("Continue on Sheikh Zayed Rd", args?.steps?.first()?.instruction)
        assertEquals(500, args?.steps?.first()?.distanceMeters)
        assertEquals(25.2048 to 55.2708, args?.steps?.first()?.startLatLng)
    }

    @Test
    fun mapGuidanceSteps_stripsHtmlAndCapsAtFifty() {
        val googleSteps =
            (1..55).map { index ->
                DirectionsStepRecord(
                    distanceMeters = index * 10,
                    maneuver = "straight",
                    htmlInstructions = "<b>Step $index</b>",
                    startLocation = LatLng(25.0 + index * 0.001, 55.0),
                )
            }

        val mapped = MarshioGuidanceLaunch.mapGuidanceSteps(googleSteps)

        assertEquals(50, mapped.size)
        assertEquals("Step 1", mapped.first().instruction)
        assertEquals(10, mapped.first().distanceMeters)
    }

    @Test
    fun argsFromRouteDetails_emptyGoogleSteps_yieldsEmptyStepList() {
        val model = sampleRouteDetailsModel()

        val args = MarshioGuidanceLaunch.argsFromRouteDetails(model)

        assertNotNull(args)
        assertTrue(args?.steps?.isEmpty() == true)
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
