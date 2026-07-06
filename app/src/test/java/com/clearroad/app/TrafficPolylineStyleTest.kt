package com.clearroad.app

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficPolylineStyleTest {

    @Test
    fun lineColor_mapsCategoriesToMarshioPalette() {
        assertEquals(0xFF2ECC71.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.FREE).toArgb())
        assertEquals(0xFFF1C40F.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.MODERATE).toArgb())
        assertEquals(0xFFE67E22.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.SLOW).toArgb())
        assertEquals(0xFFE74C3C.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.JAM).toArgb())
        assertEquals(0xFF1D9E75.toInt(), TrafficPolylineStyle.lineColor(SpeedCategory.UNKNOWN).toArgb())
    }

    @Test
    fun lineWidthPx_jamSegmentsAreThicker() {
        val density = 2f
        assertEquals(12f, TrafficPolylineStyle.lineWidthPx(SpeedCategory.FREE, density))
        assertEquals(16f, TrafficPolylineStyle.lineWidthPx(SpeedCategory.JAM, density))
    }
}
