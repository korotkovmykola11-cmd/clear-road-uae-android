package com.clearroad.app

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem

internal object TrafficPolylineStyle {
    val PreviewMainRouteCoreColor = Color(0xFF3D5AFE)
    val PreviewAlternativeCoreColor = Color(0xFF29B6F6)
    val OutlineWhite = Color(0xFFFFFFFF)

    private val ModerateYellow = Color(0xFFC9920A)
    private val SlowOrange = Color(0xFFD35400)
    private val JamRed = Color(0xFFC0392B)

    const val PreviewMainRouteHaloWidthPx = 30f
    const val PreviewMainRouteCoreWidthPx = 20f
    const val PreviewMainRouteHaloZIndex = 1f
    const val PreviewMainRouteCoreZIndex = 2f

    const val PreviewAlternativeHaloWidthPx = 18f
    const val PreviewAlternativeCoreWidthPx = 14f
    const val PreviewAlternativeHaloZIndex = -1f
    const val PreviewAlternativeCoreZIndex = 0f
    const val PreviewAlternativeDashLengthPx = 20f
    const val PreviewAlternativeGapLengthPx = 12f

    fun previewMainRouteCoreColor(): Color = PreviewMainRouteCoreColor

    fun previewAlternativeCoreColor(): Color = PreviewAlternativeCoreColor

    fun previewAlternativePattern(): List<PatternItem> =
        listOf(
            Dash(PreviewAlternativeDashLengthPx),
            Gap(PreviewAlternativeGapLengthPx),
        )

    fun lineColor(category: SpeedCategory): Color =
        when (category) {
            SpeedCategory.FREE -> PreviewMainRouteCoreColor
            SpeedCategory.MODERATE -> ModerateYellow
            SpeedCategory.SLOW -> SlowOrange
            SpeedCategory.JAM -> JamRed
            SpeedCategory.UNKNOWN -> PreviewMainRouteCoreColor
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

    fun selectedRouteCoreWidthPx(density: Float): Float = PreviewMainRouteCoreWidthPx

    fun alternativeRouteCoreWidthPx(density: Float): Float = PreviewAlternativeCoreWidthPx

    fun outlineExtraPx(density: Float): Float =
        PreviewMainRouteHaloWidthPx - PreviewMainRouteCoreWidthPx
}
