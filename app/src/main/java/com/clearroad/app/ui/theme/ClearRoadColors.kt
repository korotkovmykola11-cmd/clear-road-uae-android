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
    val RouteCardTint = Color(0xFFF5F2EC)
    val SalikNeutral = Color(0xFF4A5568)
    val ModeFastest = Color(0xFF3B82F6)
    val ModeNoTolls = Color(0xFF22A861)
    val ModeCalm = Color(0xFF8B5CF6)
    val RecommendedTint = Color(0xFF3BA4D9)
    val CloudHighlight = Color(0xFFE3F2FA)

    // docs/index.html glass home tokens
    val HomeHeaderSubtitle = Color(0xB3005078)
    val HomeHeaderTitle = Color(0xFF0A3D5C)
    val HomeAccentStart = Color(0xFF0EA5E9)
    val HomeAccentEnd = Color(0xFF06B6D4)
    val HomeAccentTeal = Color(0xFF14B8A6)
    val HomeShadow = Color(0xFF005078)
    val GlassPanelFill = Color(0xB3FFFFFF)
    val GlassPanelFillDeep = Color(0xBBFFFFFF)
    val GlassPanelBorder = Color(0x78FFFFFF)
    val GlassCardFill = Color(0xB8FFFFFF)
    val GlassRecommendedCardFill = Color(0xE4FFFFFF)
    val GlassInputBorder = Color(0x24005078)
    val BrandZoneFillTop = Color(0xA0FFFFFF)
    val BrandZoneFillBottom = Color(0x7AFFFFFF)
    val BrandZoneBorder = Color(0x44FFFFFF)
    val BrandTitleClear = Color(0xFF083148)
    val BrandTitleUae = Color(0xFF093A52)
    val YunoAdvisorUnitFill = Color(0x99FFFFFF)
    val YunoAdvisorUnitBorder = Color(0x180EA5E9)
    val RouteDecisionAccent = Color(0xFF0A3D5C)
    val RouteDecisionChoiceText = Color(0xFF062A40)
    val RouteDecisionBlockFill = Color(0xE8FFFFFF)
    val RouteDecisionBlockBorder = Color(0x400EA5E9)
    val RouteDecisionChoicePlinthFill = Color(0xFFF0F7FB)
    val InputDotFrom = Color(0xFF22C55E)
    val InputDotTo = Color(0xFF0EA5E9)

    // MARSHIO Home v2.2 executive banner + recommended route frame
    val ExecutiveGold = Color(0xFFC9A861)
    val ExecutiveCream = Color(0xFFF5EDDB)
    val ExecutiveNavyStart = Color(0xFF1A2332)
    val ExecutiveNavyEnd = Color(0xFF0F1623)
    val ExecutiveBodyMuted = Color(0xFFB8A88A)
}

fun PreferenceMode.accentColor(): Color =
    when (this) {
        PreferenceMode.FASTEST -> ClearRoadColors.ModeFastest
        PreferenceMode.NO_TOLLS -> ClearRoadColors.ModeNoTolls
        PreferenceMode.CALM -> ClearRoadColors.ModeCalm
    }
