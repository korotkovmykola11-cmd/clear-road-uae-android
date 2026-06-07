package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor

private const val HOME_BRAND_ICON_DRAWABLE = "home_brand_marshio_icon"
private const val HOME_BRAND_WORD_DRAWABLE = "home_brand_marshio_word"
private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"
private const val HOME_MODE_FASTEST_DRAWABLE = "home_mode_fastest_lightning"
private const val HOME_MODE_SAVE_AED_DRAWABLE = "home_mode_save_aed"
private const val HOME_MODE_SMOOTH_DRIVE_DRAWABLE = "home_mode_smooth_drive"

private val HomeSurfaceLight = Color(0xFFF5F9FF)
private val HomeCardSurface = Color.White
private val HomeProductBorder = ClearRoadColors.SalikNeutral.copy(alpha = 0.20f)
private val HomeProductMutedText = ClearRoadColors.RoadGreyMuted
private val HomeProductPrimaryText = ClearRoadColors.RoadGrey
private val HomeProductTagline = "One decision. Before Google Maps."
private val HomeProductCardShape = RoundedCornerShape(14.dp)

/** Stage Home 9.0 — vertical rhythm between home sections. */
internal val HomeSpacingAfterHeader = 10.dp
internal val HomeSpacingAfterRouteInput = 14.dp
internal val HomeSpacingBeforeRecommendation = 16.dp

/** Light utility surface — aligned with Route Details product tone. */
@Composable
internal fun HomeDubaiBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeSurfaceLight),
    )
}

private fun Modifier.homeLightCardSurface(
    shape: Shape = HomeProductCardShape,
): Modifier =
    shadow(
        elevation = 2.dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.04f),
        spotColor = Color.Black.copy(alpha = 0.06f),
    )
        .clip(shape)
        .background(HomeCardSurface)
        .border(
            width = 1.dp,
            color = HomeProductBorder,
            shape = shape,
        )

private val YunoIdentitySubtitleColor = Color(0xFF000000)
private val YunoHomeMarkSize = 129.dp
private val HomeBrandIconMaxHeight = 68.dp
private val HomeBrandWordMaxHeight = 22.dp
private val HomeRouteInputPanelShape = RoundedCornerShape(14.dp)
private val HomeBrandTextBlockLift = 10.dp
private val HomeBrandHeroTopInset = 20.dp
private val HomeBrandBlockRaise = 48.dp

/** MARSHIO brand header — product promise first. */
@Composable
internal fun HomeScreenHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconDrawableId =
        remember(context) {
            context.resources.getIdentifier(
                HOME_BRAND_ICON_DRAWABLE,
                "drawable",
                context.packageName,
            )
        }
    val wordDrawableId =
        remember(context) {
            context.resources.getIdentifier(
                HOME_BRAND_WORD_DRAWABLE,
                "drawable",
                context.packageName,
            )
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = HomeBrandHeroTopInset),
    ) {
        if (iconDrawableId != 0 && wordDrawableId != 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val raisePx = HomeBrandBlockRaise.roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(
                            placeable.width,
                            (placeable.height - raisePx).coerceAtLeast(0),
                        ) {
                            placeable.placeRelative(0, -raisePx)
                        }
                    },
            ) {
                Image(
                    painter = painterResource(iconDrawableId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = HomeBrandIconMaxHeight),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = -HomeBrandTextBlockLift),
                ) {
                    Image(
                        painter = painterResource(wordDrawableId),
                        contentDescription = "MARSHIO",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.TopCenter,
                        colorFilter = ColorFilter.tint(
                            ClearRoadColors.BrandTitleClear,
                            BlendMode.SrcIn,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = HomeBrandWordMaxHeight),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = HomeProductTagline,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            letterSpacing = 0.15.sp,
                            lineHeight = 19.sp,
                        ),
                        color = HomeProductMutedText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** docs/index.html `.glass-panel` */
@Composable
internal fun HomeGlassPanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ClearRoadColors.HomeShadow.copy(alpha = 0.09f),
                spotColor = ClearRoadColors.HomeShadow.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ClearRoadColors.GlassPanelFill,
                        ClearRoadColors.GlassPanelFillDeep,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = ClearRoadColors.GlassPanelBorder,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
internal fun HomePanelSectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = ClearRoadColors.GlassInputBorder.copy(alpha = 0.18f),
        thickness = 0.5.dp,
    )
}

/** docs/index.html `.yuno-section` — real YUNO asset + advisor bubble */
@Composable
internal fun HomeYunoBubbleSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val characterDrawableId =
        remember(context) {
            context.resources.getIdentifier(
                YUNO_CHARACTER_DRAWABLE,
                "drawable",
                context.packageName,
            )
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (characterDrawableId != 0) {
            Image(
                painter = painterResource(characterDrawableId),
                contentDescription = "YUNO",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(YunoHomeMarkSize),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello! I'm YUNO",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 0.5f),
                        blurRadius = 2f,
                    ),
                ),
                color = ClearRoadColors.RouteDecisionChoiceText,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Your UAE Route Advisor.\nEnter your destination and I'll help you choose the best route for your journey.",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    lineHeight = 16.sp,
                    shadow = Shadow(
                        color = Color.White.copy(alpha = 0.75f),
                        offset = Offset(0f, 0.5f),
                        blurRadius = 2f,
                    ),
                ),
                color = YunoIdentitySubtitleColor,
            )
        }
    }
}

@Composable
internal fun HomeSearchCapsule(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = ClearRoadColors.HomeShadow.copy(alpha = 0.10f),
                spotColor = ClearRoadColors.HomeShadow.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ClearRoadColors.GlassPanelFill,
                        ClearRoadColors.GlassPanelFillDeep,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = ClearRoadColors.GlassPanelBorder,
                shape = RoundedCornerShape(28.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "🔍 Where are you going today?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
            color = ClearRoadColors.HomeHeaderTitle.copy(alpha = 0.72f),
        )
    }
}

@Composable
internal fun HomeRouteInputGroup(
    modifier: Modifier = Modifier,
    fromValue: String,
    onFromValueChange: (String) -> Unit,
    fromPlaceholder: String,
    toValue: String,
    onToValueChange: (String) -> Unit,
    toPlaceholder: String,
    fromPredictions: @Composable ColumnScope.() -> Unit = {},
    toPredictions: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .homeLightCardSurface(shape = HomeRouteInputPanelShape)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 18.dp)
                .width(2.dp)
                .height(16.dp)
                .background(
                    ClearRoadColors.SalikNeutral.copy(alpha = 0.35f),
                ),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            HomeRouteInputRow(
                dotColor = ClearRoadColors.InputDotFrom,
                placeholder = fromPlaceholder,
                value = fromValue,
                onValueChange = onFromValueChange,
            )
            fromPredictions()
            HomeRouteInputRow(
                dotColor = ClearRoadColors.InputDotTo,
                placeholder = toPlaceholder,
                value = toValue,
                onValueChange = onToValueChange,
            )
            toPredictions()
        }
    }
}

internal fun homeRouteSkyReadableTextStyle(
    base: androidx.compose.ui.text.TextStyle,
    placeholder: Boolean = false,
): androidx.compose.ui.text.TextStyle =
    base.copy(
        color =
            ClearRoadColors.RoadGrey.copy(
                alpha = if (placeholder) 0.45f else 0.92f,
            ),
    )

@Composable
private fun HomeRouteInputRow(
    dotColor: Color,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.72f)),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                homeRouteSkyReadableTextStyle(MaterialTheme.typography.bodyMedium).copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style =
                                homeRouteSkyReadableTextStyle(
                                    MaterialTheme.typography.bodyMedium,
                                    placeholder = true,
                                ).copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                ),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** Stage 30.3 — vertical mode selection cards (visual only). */
private val HomeModeCardIconSize = 40.dp
private val HomeModeCardSecondaryIconRenderSize = 48.dp
private val HomeModeCardActiveIconSize = 44.dp
private val HomeModeCardActiveSecondaryIconRenderSize = 52.dp
private val HomeModeInactivePlinthShape = RoundedCornerShape(12.dp)
private val HomeModeCardHeight = 50.dp
private val HomeModeCardSpacing = 4.dp
private val HomeModeCardIconTextGap = 10.dp
private val HomeModeInactivePlinthHorizontalPadding = 14.dp
private val HomeModeInactivePlinthVerticalPadding = 4.dp

private fun Modifier.homeModeContentSurface(
    mode: PreferenceMode,
    selected: Boolean,
): Modifier {
    val borderColor =
        if (selected) {
            mode.accentColor().copy(alpha = 0.28f)
        } else {
            HomeProductBorder
        }
    return shadow(
        elevation = 1.dp,
        shape = HomeModeInactivePlinthShape,
        ambientColor = Color.Black.copy(alpha = 0.03f),
        spotColor = Color.Black.copy(alpha = 0.05f),
    )
        .clip(HomeModeInactivePlinthShape)
        .background(HomeCardSurface)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = HomeModeInactivePlinthShape,
        )
        .padding(
            horizontal = HomeModeInactivePlinthHorizontalPadding,
            vertical = HomeModeInactivePlinthVerticalPadding,
        )
}

private fun homeModeCardTitle(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> "FASTEST"
        PreferenceMode.NO_TOLLS -> "SAVE AED"
        PreferenceMode.CALM -> "SMOOTH DRIVE"
    }

private fun homeModeCardDrawable(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> HOME_MODE_FASTEST_DRAWABLE
        PreferenceMode.NO_TOLLS -> HOME_MODE_SAVE_AED_DRAWABLE
        PreferenceMode.CALM -> HOME_MODE_SMOOTH_DRIVE_DRAWABLE
    }

private fun homeModeCardIconRenderSize(mode: PreferenceMode, selected: Boolean): Dp =
    when (mode) {
        PreferenceMode.FASTEST ->
            if (selected) HomeModeCardActiveIconSize else HomeModeCardIconSize
        PreferenceMode.NO_TOLLS, PreferenceMode.CALM ->
            if (selected) {
                HomeModeCardActiveSecondaryIconRenderSize
            } else {
                HomeModeCardSecondaryIconRenderSize
            }
    }

@Composable
private fun HomeModeCardIcon(
    mode: PreferenceMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawableId =
        remember(context, mode) {
            context.resources.getIdentifier(
                homeModeCardDrawable(mode),
                "drawable",
                context.packageName,
            )
        }

    if (drawableId != 0) {
        Image(
            painter = painterResource(drawableId),
            contentDescription = homeModeCardTitle(mode),
            contentScale = ContentScale.Fit,
            modifier = modifier.size(homeModeCardIconRenderSize(mode, selected)),
        )
    }
}

@Composable
private fun HomeModePersonalityPill(
    mode: PreferenceMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = homeModeCardTitle(mode)
    val titleColor =
        if (selected) {
            HomeProductPrimaryText
        } else {
            HomeProductMutedText
        }
    val titleStyle =
        MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            lineHeight = 15.sp,
        )
    val iconRenderSize = homeModeCardIconRenderSize(mode, selected)
    val iconSlotModifier = Modifier.size(iconRenderSize)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeModeCardHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier =
                Modifier
                    .homeModeContentSurface(mode = mode, selected = selected)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = iconSlotModifier,
                contentAlignment = Alignment.Center,
            ) {
                HomeModeCardIcon(mode = mode, selected = selected)
            }
            Spacer(modifier = Modifier.width(HomeModeCardIconTextGap))
            Text(
                text = title,
                style = titleStyle,
                color = titleColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun HomeGlassModeTabs(
    selectedMode: PreferenceMode,
    onModeSelected: (PreferenceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HomeModeCardSpacing),
        ) {
            PreferenceMode.entries.forEach { mode ->
                HomeModePersonalityPill(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** docs/index.html `.route-card` — glass family for guidance + routes */
@Composable
internal fun HomeGlassSurface(
    modifier: Modifier = Modifier,
    recommended: Boolean = false,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val clickModifier =
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val surfaceColor =
        if (recommended) {
            ClearRoadColors.GlassRecommendedCardFill
        } else {
            ClearRoadColors.GlassCardFill
        }
    val elevation = if (recommended) 10.dp else 1.dp
    val defaultBorder =
        if (recommended) {
            BorderStroke(1.5.dp, ClearRoadColors.ExecutiveGold.copy(alpha = 0.88f))
        } else {
            BorderStroke(1.dp, ClearRoadColors.GlassInputBorder.copy(alpha = 0.10f))
        }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .then(
                if (recommended) {
                    Modifier.shadow(
                        elevation = 3.dp,
                        shape = shape,
                        ambientColor = ClearRoadColors.HomeAccentStart.copy(alpha = 0.18f),
                        spotColor = ClearRoadColors.HomeAccentStart.copy(alpha = 0.12f),
                    )
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = surfaceColor,
        shadowElevation = elevation,
        border = border ?: defaultBorder,
    ) {
        Column {
            if (recommended) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ClearRoadColors.ExecutiveGold.copy(alpha = 0.55f),
                                    ClearRoadColors.ExecutiveGold,
                                    ClearRoadColors.ExecutiveGold.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = if (recommended) 5.dp else 4.dp,
                ),
                content = content,
            )
        }
    }
}
