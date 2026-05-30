package com.clearroad.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Material color scheme mapped from Clear Road semantic tokens (Stage 22). */
val ClearRoadLightColorScheme = lightColorScheme(
    primary = ClearRoadColors.ClearSkyBlue,
    onPrimary = Color.White,
    primaryContainer = ClearRoadColors.RouteCardSurfaceMuted,
    onPrimaryContainer = ClearRoadColors.RoadGrey,
    secondary = ClearRoadColors.SandSecondary,
    onSecondary = ClearRoadColors.RoadGrey,
    tertiary = ClearRoadColors.ModeCalm,
    onTertiary = Color.White,
    background = ClearRoadColors.CloudBackground,
    onBackground = ClearRoadColors.RoadGrey,
    surface = ClearRoadColors.RouteCardSurface,
    onSurface = ClearRoadColors.RoadGrey,
    surfaceVariant = ClearRoadColors.RouteCardSurfaceMuted,
    onSurfaceVariant = ClearRoadColors.RoadGreyMuted,
    outline = ClearRoadColors.SalikNeutral,
)

/** Premium light theme first — dark theme deferred to a later stage. */
val ClearRoadDarkColorScheme = ClearRoadLightColorScheme
