package com.clearroad.app

import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RouteGoogleMarshioDecisionUiModel
import com.clearroad.app.ui.model.RouteMapEvidenceUiModel
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPreviewCardTest {

    @Test
    fun decisionCaption_agreesWithGoogle() {
        assertEquals(
            MarshioGoogleValidationLine,
            routePreviewDecisionCaption(decision(GoogleMarshioDecisionState.AGREES)),
        )
    }

    @Test
    fun decisionCaption_disagreesWithGoogle() {
        assertEquals(
            "MARSHIO chose a different route than Google.",
            routePreviewDecisionCaption(decision(GoogleMarshioDecisionState.DISAGREES)),
        )
    }

    @Test
    fun decisionCaption_unknownWhenDecisionMissing() {
        assertEquals(
            "MARSHIO route preview.",
            routePreviewDecisionCaption(null),
        )
    }

    @Test
    fun forkFallback_visibleWhenEvidenceEnabledWithoutSplit() {
        val evidence =
            RouteMapEvidenceUiModel(
                enabled = true,
                strategicCaption = "Full trip — see how different the corridors are.",
            )

        assertTrue(routePreviewForkFallbackVisible(evidence))
        assertFalse(hasLocalForkMapEvidence(evidence))
    }

    @Test
    fun forkFallback_hiddenWhenSplitMapAvailable() {
        val evidence =
            RouteMapEvidenceUiModel(
                enabled = true,
                splitPoint = LatLng(25.0, 55.0),
                googleDivergentPath = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.0)),
                marshioDivergentPath = listOf(LatLng(25.0, 55.0), LatLng(25.01, 55.01)),
            )

        assertFalse(routePreviewForkFallbackVisible(evidence))
        assertTrue(hasLocalForkMapEvidence(evidence))
    }

    @Test
    fun legend_shownWhenGrayRoutesExist() {
        assertTrue(routePreviewShowsAlternativesLegend(alternativePathCount = 1))
        assertFalse(routePreviewShowsAlternativesLegend(alternativePathCount = 0))
    }

    private fun decision(state: GoogleMarshioDecisionState) =
        RouteGoogleMarshioDecisionUiModel(
            state = state,
            headline = "headline",
            subtext = "subtext",
            verdictText = "verdict",
        )
}
