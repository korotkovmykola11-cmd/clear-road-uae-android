package com.clearroad.app.guidance

import org.junit.Assert.assertEquals
import org.junit.Test

class MarshioGuidanceStepTrackerTest {

    private val steps =
        listOf(
            GuidanceStepUi(
                instruction = "Head north",
                distanceMeters = 100,
                startLatLng = null,
            ),
            GuidanceStepUi(
                instruction = "Continue on Sheikh Zayed Rd",
                distanceMeters = 500,
                startLatLng = null,
            ),
            GuidanceStepUi(
                instruction = "Turn right",
                distanceMeters = 200,
                startLatLng = null,
            ),
        )

    @Test
    fun emptySteps_returnsMinusOne() {
        assertEquals(-1, MarshioGuidanceStepTracker.currentStepIndex(0.0, emptyList()))
    }

    @Test
    fun atZeroMeters_returnsStepZero() {
        assertEquals(0, MarshioGuidanceStepTracker.currentStepIndex(0.0, steps))
    }

    @Test
    fun midStep_returnsSameStep() {
        assertEquals(0, MarshioGuidanceStepTracker.currentStepIndex(50.0, steps))
        assertEquals(1, MarshioGuidanceStepTracker.currentStepIndex(250.0, steps))
    }

    @Test
    fun atStepBoundary_returnsNextStep() {
        assertEquals(1, MarshioGuidanceStepTracker.currentStepIndex(100.0, steps))
        assertEquals(2, MarshioGuidanceStepTracker.currentStepIndex(600.0, steps))
    }

    @Test
    fun beyondLastStep_returnsLastIndex() {
        assertEquals(2, MarshioGuidanceStepTracker.currentStepIndex(800.0, steps))
        assertEquals(2, MarshioGuidanceStepTracker.currentStepIndex(10_000.0, steps))
    }

    @Test
    fun negativeTraveledMeters_treatedAsZero() {
        assertEquals(0, MarshioGuidanceStepTracker.currentStepIndex(-50.0, steps))
    }
}
