package com.clearroad.app

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunctionAnnotationClassifierTest {

    @Test
    fun classify_mapsHighwayEntryFromMergeManeuver() {
        assertEquals(
            JunctionType.HIGHWAY_ENTRY,
            JunctionAnnotationClassifier.classify("merge", "Merge onto Sheikh Zayed Rd"),
        )
    }

    @Test
    fun classify_mapsHighwayExitFromRamp() {
        assertEquals(
            JunctionType.HIGHWAY_EXIT,
            JunctionAnnotationClassifier.classify("ramp-right", "Take exit 32 toward Downtown"),
        )
    }

    @Test
    fun classify_mapsRoundabout() {
        assertEquals(
            JunctionType.ROUNDABOUT,
            JunctionAnnotationClassifier.classify("roundabout-left", "At the roundabout, take the 2nd exit"),
        )
    }

    @Test
    fun classify_mapsComplexTurnForUturn() {
        assertEquals(
            JunctionType.COMPLEX_TURN,
            JunctionAnnotationClassifier.classify("uturn-left", "Make a U-turn"),
        )
    }

    @Test
    fun classify_mapsTollFromSalikInstruction() {
        assertEquals(
            JunctionType.TOLL,
            JunctionAnnotationClassifier.classify(null, "Pass Salik gate Al Safa"),
        )
    }

    @Test
    fun classify_mapsTunnelFromInstruction() {
        assertEquals(
            JunctionType.TUNNEL,
            JunctionAnnotationClassifier.classify("straight", "Enter the tunnel"),
        )
    }

    @Test
    fun classify_mapsOverpassFromInstruction() {
        assertEquals(
            JunctionType.OVERPASS,
            JunctionAnnotationClassifier.classify("straight", "Continue on the flyover"),
        )
    }

    @Test
    fun classify_ignoresSimpleStraightManeuver() {
        assertNull(JunctionAnnotationClassifier.classify("straight", "Continue straight"))
    }

    @Test
    fun classify_ignoresSlightTurn() {
        assertNull(JunctionAnnotationClassifier.classify("turn-slight-left", "Slight left"))
    }

    @Test
    fun stripHtml_removesTags() {
        assertEquals(
            "Turn left onto Main St",
            JunctionAnnotationClassifier.stripHtml("<b>Turn left</b> onto <div>Main St</div>"),
        )
    }

    @Test
    fun displayLabel_usesCleanTypeLabelsMaxTenChars() {
        assertEquals("Merge", JunctionAnnotationClassifier.displayLabel(JunctionType.HIGHWAY_ENTRY))
        assertEquals("Exit", JunctionAnnotationClassifier.displayLabel(JunctionType.HIGHWAY_EXIT))
        assertEquals("Salik", JunctionAnnotationClassifier.displayLabel(JunctionType.TOLL))
        assertEquals(
            "Roundabout",
            JunctionAnnotationClassifier.displayLabel(JunctionType.ROUNDABOUT),
        )
        assertTrue(
            JunctionAnnotationClassifier.displayLabel(JunctionType.ROUNDABOUT).length <=
                JunctionAnnotationClassifier.MAX_LABEL_CHARS,
        )
        assertEquals(
            "Merge",
            JunctionAnnotationClassifier.shortLabel(
                JunctionType.HIGHWAY_ENTRY,
                "Merge onto Sheikh Zayed Rd",
            ),
        )
    }

    @Test
    fun iconColor_mapsTypeSpecificColors() {
        assertEquals(0xFF757575.toInt(), JunctionAnnotationClassifier.iconColor(JunctionType.HIGHWAY_EXIT))
        assertEquals(0xFFF59E0B.toInt(), JunctionAnnotationClassifier.iconColor(JunctionType.TOLL))
        assertEquals(0xFF2196F3.toInt(), JunctionAnnotationClassifier.iconColor(JunctionType.ROUNDABOUT))
        assertEquals(0xFF757575.toInt(), JunctionAnnotationClassifier.iconColor(JunctionType.HIGHWAY_ENTRY))
    }
}

class JunctionAnnotationBuilderTest {

    private val pointA = LatLng(25.0, 55.0)
    private val pointB = LatLng(25.01, 55.01)
    private val pointC = LatLng(25.02, 55.02)
    private val pointD = LatLng(25.03, 55.03)
    private val pointE = LatLng(25.04, 55.04)

    @Test
    fun build_limitsToFourMostComplexAnnotations() {
        val steps =
            listOf(
                step("straight", pointA),
                step("merge", pointB, "Merge onto highway"),
                step("roundabout-left", pointC, "Roundabout"),
                step("ramp-right", pointD, "Take exit"),
                step("uturn-left", pointE, "U-turn"),
                step(null, pointE, "Pass Salik gate"),
            )

        val annotations = JunctionAnnotationBuilder.buildFromSteps(steps)

        assertEquals(4, annotations.size)
        assertTrue(annotations.any { it.type == JunctionType.ROUNDABOUT })
        assertTrue(annotations.any { it.type == JunctionType.TOLL })
    }

    @Test
    fun build_fallbackWhenStepsMissingLocation() {
        val steps =
            listOf(
                DirectionsStepRecord(
                    distanceMeters = 500,
                    maneuver = "merge",
                    htmlInstructions = "Merge",
                    startLocation = null,
                ),
            )

        assertTrue(JunctionAnnotationBuilder.buildFromSteps(steps).isEmpty())
    }

    @Test
    fun build_fallbackWhenStepsEmpty() {
        assertTrue(JunctionAnnotationBuilder.buildFromSteps(emptyList()).isEmpty())
        assertTrue(
            JunctionAnnotationBuilder.build(
                RealRouteDebugData(
                    distanceText = "1 km",
                    durationText = "2 min",
                    distanceMeters = 1000,
                    durationSeconds = 120,
                    tollAED = 0,
                    hasToll = false,
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun extractLatLngFromStartLocation_readsGoogleStepJson() {
        val stepJson =
            """
            {
              "html_instructions": "Merge onto <b>E11</b>",
              "maneuver": "merge",
              "start_location": { "lat": 25.0772, "lng": 55.1364 }
            }
            """.trimIndent()

        val location = extractLatLngFromStartLocation(stepJson)

        assertEquals(25.0772, location!!.latitude, 0.0001)
        assertEquals(55.1364, location.longitude, 0.0001)
    }

    private fun step(
        maneuver: String?,
        location: LatLng,
        instruction: String = "Instruction",
    ): DirectionsStepRecord =
        DirectionsStepRecord(
            distanceMeters = 800,
            maneuver = maneuver,
            htmlInstructions = instruction,
            startLocation = location,
        )
}
