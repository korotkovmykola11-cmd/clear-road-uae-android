package com.clearroad.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

private const val HOME_BACKGROUND_DRAWABLE = "home_hero_dubai"
private const val HOME_BRAND_ICON_DRAWABLE = "home_brand_marshio_icon"
private const val HOME_BRAND_WORD_DRAWABLE = "home_brand_marshio_word"
private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"
private const val HOME_MODE_FASTEST_DRAWABLE = "home_mode_fastest_lightning"
private const val HOME_MODE_SAVE_AED_DRAWABLE = "home_mode_save_aed"
private const val HOME_MODE_SMOOTH_DRIVE_DRAWABLE = "home_mode_smooth_drive"

/** Full-screen Dubai road photo — docs/design/img_3.png */
@Composable
internal fun HomeDubaiBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawableId =
        remember(context) {
            context.resources.getIdentifier(
                HOME_BACKGROUND_DRAWABLE,
                "drawable",
                context.packageName,
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        if (drawableId != 0) {
            Image(
                painter = painterResource(drawableId),
                contentDescription = "Dubai road",
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.5f, 0.22f),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ClearRoadColors.CloudHighlight,
                                ClearRoadColors.CloudBackground,
                            ),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.40f to Color.White.copy(alpha = 0.05f),
                            0.70f to Color.White.copy(alpha = 0.18f),
                            1f to Color.White.copy(alpha = 0.32f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.58f),
                            0.55f to Color.White.copy(alpha = 0.28f),
                            1f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

private val YunoIdentitySubtitleColor = Color(0xFF000000)
private val YunoHomeMarkSize = 129.dp
private val HomeBrandIconMaxHeight = 68.dp
private val HomeBrandWordMaxHeight = 18.dp
private val HomeBrandTextBlockLift = 10.dp
private val HomeBrandHeroTopInset = 20.dp
private val HomeBrandBlockRaise = 48.dp

/** MARSHIO logo from docs/img_1.png — icon + word split for text vertical tuning */
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = HomeBrandHeroTopInset),
        horizontalAlignment = Alignment.CenterHorizontally,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = HomeBrandWordMaxHeight),
                    )
                    Text(
                        text = "UAE Route Decision Assistant",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            lineHeight = 16.sp,
                            shadow = Shadow(
                                color = Color.White.copy(alpha = 0.82f),
                                offset = Offset(0f, 0.5f),
                                blurRadius = 2f,
                            ),
                        ),
                        color = ClearRoadColors.RouteDecisionChoiceText,
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
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 18.dp)
                .width(2.dp)
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ClearRoadColors.InputDotFrom,
                            ClearRoadColors.InputDotTo,
                        ),
                    ),
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

private val HomeRouteInputSkyTextShadow =
    Shadow(
        color = Color.White.copy(alpha = 0.88f),
        offset = Offset(0f, 0.5f),
        blurRadius = 3f,
    )

internal fun homeRouteSkyReadableTextStyle(
    base: androidx.compose.ui.text.TextStyle,
    placeholder: Boolean = false,
): androidx.compose.ui.text.TextStyle =
    base.copy(
        color =
            ClearRoadColors.RouteDecisionChoiceText.copy(
                alpha = if (placeholder) 0.66f else 1f,
            ),
        shadow = HomeRouteInputSkyTextShadow,
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
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                homeRouteSkyReadableTextStyle(MaterialTheme.typography.bodyMedium).copy(
                    fontWeight = FontWeight.Medium,
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
private val HomeModeCardIconSize = 52.dp
private val HomeModeCardSecondaryIconRenderSize = 64.dp
private val HomeModeCardActiveIconSize = 58.dp
private val HomeModeCardActiveSecondaryIconRenderSize = 72.dp
private val HomeModeInactivePlinthShape = RoundedCornerShape(14.dp)
private val HomeModeCardHeight = 64.dp
private val HomeModeCardSpacing = 6.dp
private val HomeModeCardBlockWidthFraction = 0.84f
private val HomeModeCardIconTextGap = 12.dp
private val HomeModeInactivePlinthHorizontalPadding = 10.dp
private val HomeModeInactivePlinthVerticalPadding = 6.dp

private val HomeModeInactiveTitleShadow =
    Shadow(
        color = Color.White.copy(alpha = 0.94f),
        offset = Offset(0f, 0.5f),
        blurRadius = 5f,
    )

private val HomeModeActiveTitleShadow =
    Shadow(
        color = Color.White.copy(alpha = 0.98f),
        offset = Offset(0f, 0.5f),
        blurRadius = 6f,
    )

private fun homeModeInactivePlinthTint(mode: PreferenceMode): Color =
    when (mode) {
        PreferenceMode.FASTEST -> ClearRoadColors.HomeAccentStart.copy(alpha = 0.14f)
        PreferenceMode.NO_TOLLS -> Color(0xFF059669).copy(alpha = 0.12f)
        PreferenceMode.CALM -> Color(0xFF6366F1).copy(alpha = 0.12f)
    }

private fun Modifier.homeModeInactiveContentPlinth(mode: PreferenceMode): Modifier {
    val plinthTint = homeModeInactivePlinthTint(mode)
    return shadow(
        elevation = 3.dp,
        shape = HomeModeInactivePlinthShape,
        ambientColor = ClearRoadColors.HomeShadow.copy(alpha = 0.10f),
        spotColor = ClearRoadColors.HomeShadow.copy(alpha = 0.06f),
    )
        .clip(HomeModeInactivePlinthShape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    plinthTint,
                    Color.White.copy(alpha = 0.46f),
                    Color.White.copy(alpha = 0.40f),
                ),
            ),
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.42f),
            shape = HomeModeInactivePlinthShape,
        )
        .padding(
            horizontal = HomeModeInactivePlinthHorizontalPadding,
            vertical = HomeModeInactivePlinthVerticalPadding,
        )
}

private fun Modifier.homeModeActiveContentPlinth(mode: PreferenceMode): Modifier {
    val plinthTint = homeModeInactivePlinthTint(mode)
    return shadow(
        elevation = 5.dp,
        shape = HomeModeInactivePlinthShape,
        ambientColor = ClearRoadColors.HomeAccentStart.copy(alpha = 0.12f),
        spotColor = ClearRoadColors.HomeAccentEnd.copy(alpha = 0.08f),
    )
        .clip(HomeModeInactivePlinthShape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    ClearRoadColors.HomeAccentStart.copy(alpha = 0.10f),
                    ClearRoadColors.HomeAccentStart.copy(alpha = 0.07f),
                    Color.White.copy(alpha = 0.50f),
                    plinthTint,
                    ClearRoadColors.HomeAccentEnd.copy(alpha = 0.08f),
                ),
            ),
        )
        .border(
            width = 1.75.dp,
            color = ClearRoadColors.HomeAccentStart.copy(alpha = 0.80f),
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
    val titleColor = ClearRoadColors.RouteDecisionChoiceText
    val titleStyle =
        MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = if (selected) 0.9.sp else 0.8.sp,
            lineHeight = 18.sp,
            shadow =
                if (selected) {
                    HomeModeActiveTitleShadow
                } else {
                    HomeModeInactiveTitleShadow
                },
        )
    val iconRenderSize = homeModeCardIconRenderSize(mode, selected)
    val iconSlotModifier =
        Modifier
            .size(iconRenderSize)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = ClearRoadColors.HomeAccentStart.copy(alpha = 0.16f),
                        spotColor = ClearRoadColors.HomeAccentEnd.copy(alpha = 0.10f),
                    )
                } else {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color.White.copy(alpha = 0.55f),
                        spotColor = ClearRoadColors.HomeShadow.copy(alpha = 0.08f),
                    )
                },
            )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeModeCardHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier =
                (if (selected) {
                    Modifier.homeModeActiveContentPlinth(mode)
                } else {
                    Modifier.homeModeInactiveContentPlinth(mode)
                })
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
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxWidth(HomeModeCardBlockWidthFraction),
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
