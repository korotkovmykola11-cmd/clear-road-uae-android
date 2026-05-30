package com.clearroad.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.clearroad.app.domain.PreferenceMode

/** Semantic brand tokens — locked in Stage 21.5 Design Direction. */
object ClearRoadColors {
    val ClearSkyBlue = Color(0xFF3BA4D9)
    val CloudBackground = Color(0xFFF4F9FD)
    val RouteCardSurface = Color(0xFFFFFFFF)
    val RouteCardSurfaceMuted = Color(0xFFF8FBFE)
    val RoadGrey = Color(0xFF2E3438)
    val RoadGreyMuted = Color(0xFF5C6670)
    val SandSecondary = Color(0xFFE9E2D6)
    val SalikNeutral = Color(0xFF4A5568)
    val ModeFastest = Color(0xFF3B82F6)
    val ModeNoTolls = Color(0xFF22A861)
    val ModeCalm = Color(0xFF8B5CF6)
    val RecommendedTint = Color(0xFF3BA4D9)
}

fun PreferenceMode.accentColor(): Color =
    when (this) {
        PreferenceMode.FASTEST -> ClearRoadColors.ModeFastest
        PreferenceMode.NO_TOLLS -> ClearRoadColors.ModeNoTolls
        PreferenceMode.CALM -> ClearRoadColors.ModeCalm
    }
