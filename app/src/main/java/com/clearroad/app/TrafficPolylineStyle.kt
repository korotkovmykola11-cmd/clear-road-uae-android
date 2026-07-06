package com.clearroad.app

import androidx.compose.ui.graphics.Color

internal object TrafficPolylineStyle {
    val FallbackGreen = Color(0xFF1D9E75)
    val OutlineWhite = Color(0xFFFFFFFF)

    private val FreeGreen = Color(0xFF2ECC71)
    private val ModerateYellow = Color(0xFFF1C40F)
    private val SlowOrange = Color(0xFFE67E22)
    private val JamRed = Color(0xFFE74C3C)

    fun lineColor(category: SpeedCategory): Color =
        when (category) {
            SpeedCategory.FREE -> FreeGreen
            SpeedCategory.MODERATE -> ModerateYellow
            SpeedCategory.SLOW -> SlowOrange
            SpeedCategory.JAM -> JamRed
            SpeedCategory.UNKNOWN -> FallbackGreen
        }

    fun lineWidthPx(
        category: SpeedCategory,
        density: Float,
    ): Float =
        if (category == SpeedCategory.JAM) {
            8f * density
        } else {
            6f * density
        }

    fun outlineExtraPx(density: Float): Float = 4f * density
}
