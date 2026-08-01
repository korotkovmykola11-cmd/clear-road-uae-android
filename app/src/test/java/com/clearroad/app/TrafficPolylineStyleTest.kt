package com.clearroad.app

import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficPolylineStyleTest {

    @Test
    fun lineColor_mapsFreeAndUnknownToPreviewMainRouteIndigo() {
        assertEquals(
            0xFF3D5AFE.toInt(),
            TrafficPolylineStyle.lineColor(SpeedCategory.FREE).toArgb(),
        )
        assertEquals(
            0xFF3D5AFE.toInt(),
            TrafficPolylineStyle.lineColor(SpeedCategory.UNKNOWN).toArgb(),
        )
    }

    @Test
    fun lineColor_keepsTrafficPaletteForModerateSlowJam() {
        assertEquals(0xFFC9920A.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.MODERATE).toArgb())
        assertEquals(0xFFD35400.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.SLOW).toArgb())
        assertEquals(0xFFC0392B.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.JAM).toArgb())
    }

    @Test
    fun lineWidthPx_jamSegmentsAreThicker() {
        val density = 2f
        assertEquals(12f, TrafficPolylineStyle.lineWidthPx(SpeedCategory.FREE, density))
        assertEquals(16f, TrafficPolylineStyle.lineWidthPx(SpeedCategory.JAM, density))
    }

    @Test
    fun previewRouteColors_useOpaqueIndigoAndCyan() {
        assertEquals(0xFF3D5AFE.toInt(), TrafficPolylineStyle.previewMainRouteCoreColor().toArgb())
        assertEquals(0xFF29B6F6.toInt(), TrafficPolylineStyle.previewAlternativeCoreColor().toArgb())
        assertEquals(1f, TrafficPolylineStyle.previewMainRouteCoreColor().alpha)
        assertEquals(1f, TrafficPolylineStyle.previewAlternativeCoreColor().alpha)
    }

    @Test
    fun previewAlternativePattern_usesDashAndGapFromConstants() {
        val pattern = TrafficPolylineStyle.previewAlternativePattern()
        assertEquals(2, pattern.size)
        assertTrue(pattern[0] is Dash)
        assertTrue(pattern[1] is Gap)
        assertEquals(
            TrafficPolylineStyle.PreviewAlternativeDashLengthPx,
            (pattern[0] as Dash).length,
        )
        assertEquals(
            TrafficPolylineStyle.PreviewAlternativeGapLengthPx,
            (pattern[1] as Gap).length,
        )
    }

    @Test
    fun previewRouteWidths_andZIndices_matchSpec() {
        assertEquals(30f, TrafficPolylineStyle.PreviewMainRouteHaloWidthPx)
        assertEquals(20f, TrafficPolylineStyle.PreviewMainRouteCoreWidthPx)
        assertEquals(1f, TrafficPolylineStyle.PreviewMainRouteHaloZIndex)
        assertEquals(2f, TrafficPolylineStyle.PreviewMainRouteCoreZIndex)
        assertEquals(18f, TrafficPolylineStyle.PreviewAlternativeHaloWidthPx)
        assertEquals(14f, TrafficPolylineStyle.PreviewAlternativeCoreWidthPx)
        assertEquals(-1f, TrafficPolylineStyle.PreviewAlternativeHaloZIndex)
        assertEquals(0f, TrafficPolylineStyle.PreviewAlternativeCoreZIndex)
        assertEquals(
            20f,
            TrafficPolylineStyle.selectedRouteCoreWidthPx(density = 2.625f),
        )
        assertEquals(
            14f,
            TrafficPolylineStyle.alternativeRouteCoreWidthPx(density = 2.625f),
        )
    }
}
