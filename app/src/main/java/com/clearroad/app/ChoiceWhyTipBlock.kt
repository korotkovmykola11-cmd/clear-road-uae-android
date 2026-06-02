package com.clearroad.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.clearroad.app.ui.model.ChoiceWhyTipUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"

private val YunoCompactRecommendationShape = RoundedCornerShape(12.dp)
private val YunoCompactRecommendationWidthFraction = 0.84f
private val YunoCompactRecommendationHorizontalPadding = 10.dp
private val YunoCompactRecommendationVerticalPadding = 7.dp
private val YunoCompactRecommendationMarkSize = 88.dp
private val YunoCompactRecommendationMarkGap = 12.dp

private val YunoRecommendationTitleShadow =
    Shadow(
        color = Color.White.copy(alpha = 0.98f),
        offset = Offset(0f, 0.5f),
        blurRadius = 6f,
    )

private val YunoRecommendationSupportingShadow =
    Shadow(
        color = Color.White.copy(alpha = 0.92f),
        offset = Offset(0f, 0.5f),
        blurRadius = 4f,
    )

private fun Modifier.yunoCompactRecommendationGlassSurface(): Modifier =
    shadow(
        elevation = 4.dp,
        shape = YunoCompactRecommendationShape,
        ambientColor = ClearRoadColors.HomeAccentStart.copy(alpha = 0.11f),
        spotColor = ClearRoadColors.HomeShadow.copy(alpha = 0.08f),
    )
        .clip(YunoCompactRecommendationShape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    ClearRoadColors.HomeAccentStart.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.54f),
                    Color.White.copy(alpha = 0.50f),
                ),
            ),
        )
        .border(
            width = 1.5.dp,
            color = ClearRoadColors.HomeAccentStart.copy(alpha = 0.58f),
            shape = YunoCompactRecommendationShape,
        )

@Composable
private fun YunoCompactRecommendationShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .yunoCompactRecommendationGlassSurface()
            .padding(
                horizontal = YunoCompactRecommendationHorizontalPadding,
                vertical = YunoCompactRecommendationVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun YunoCompactRecommendationMark(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val characterDrawableId =
        remember(context) {
            context.resources.getIdentifier(
                YUNO_CHARACTER_DRAWABLE,
                "drawable",
                context.packageName,
            )
        }

    if (characterDrawableId != 0) {
        Image(
            painter = painterResource(characterDrawableId),
            contentDescription = "YUNO",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(YunoCompactRecommendationMarkSize)
                .alpha(0.92f),
        )
    }
}

@Composable
private fun YunoCompactRecommendationSignature(modifier: Modifier = Modifier) {
    YunoCompactRecommendationMark(modifier = modifier)
}

/** Stage 30.3D — recommendation banner visual authority (Home only). */
@Composable
internal fun YunoCompactRecommendationBlock(
    ready: Boolean,
    choice: String,
    why: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxWidth(YunoCompactRecommendationWidthFraction),
        ) {
            if (ready) {
                YunoCompactRecommendationShell {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        YunoCompactRecommendationSignature()
                        Spacer(modifier = Modifier.width(YunoCompactRecommendationMarkGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.9.sp,
                                    lineHeight = 18.sp,
                                    shadow = YunoRecommendationTitleShadow,
                                ),
                                color = ClearRoadColors.RouteDecisionChoiceText,
                                maxLines = 2,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = why,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    shadow = YunoRecommendationSupportingShadow,
                                ),
                                color = ClearRoadColors.RouteDecisionChoiceText,
                            )
                        }
                    }
                }
            } else {
                YunoCompactRecommendationShell {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        YunoCompactRecommendationSignature()
                        Spacer(modifier = Modifier.width(YunoCompactRecommendationMarkGap))
                        Text(
                            text = "YUNO is ready to help choose the best route.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.9.sp,
                                lineHeight = 18.sp,
                                shadow = YunoRecommendationTitleShadow,
                            ),
                            color = ClearRoadColors.RouteDecisionChoiceText,
                        )
                    }
                }
            }
        }
    }
}

/** Stage 30.3A — kept for easy rollback. */
@Composable
internal fun YunoRecommendationBanner(
    choice: String,
    why: String,
    modifier: Modifier = Modifier,
) {
    YunoCompactRecommendationBlock(
        ready = true,
        choice = choice,
        why = why,
        modifier = modifier,
    )
}

@Composable
internal fun YunoRecommendationEmptyState(modifier: Modifier = Modifier) {
    YunoCompactRecommendationBlock(
        ready = false,
        choice = "",
        why = "",
        modifier = modifier,
    )
}

@Composable
internal fun ChoiceWhyTipBlock(
    model: ChoiceWhyTipUiModel,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    ChoiceWhyTipBlock(
        selectedDecisionTitle = model.choice,
        selectedDecisionWhy = model.why,
        selectedDecisionTip = model.tip,
        compact = model.compact,
        embedded = embedded,
        modifier = modifier,
    )
}

@Composable
internal fun ChoiceWhyTipBlock(
    selectedDecisionTitle: String,
    selectedDecisionWhy: String,
    selectedDecisionTip: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val guidanceLabelStyle =
        MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.35.sp,
        )
    val choiceLabelStyle =
        if (embedded) {
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        } else if (compact) {
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp,
            )
        } else {
            MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp,
            )
        }
    val choiceBodyStyle =
        if (embedded) {
            MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp,
                letterSpacing = (-0.4).sp,
            )
        } else if (compact) {
            MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp)
        } else {
            MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
        }
    val secondaryLabelStyle =
        if (embedded) {
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
        } else {
            choiceLabelStyle
        }
    val secondaryBodyStyle =
        if (embedded) {
            MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp)
        } else if (compact) {
            MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp)
        } else {
            MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
        }
    val recGapLabelToBody = if (compact) 1.dp else if (embedded) 1.dp else 3.dp
    val recGapBetweenSections = if (compact) 3.dp else if (embedded) 3.dp else 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = when {
                embedded -> 0.dp
                compact -> 1.dp
                else -> 6.dp
            }),
    ) {
        if (embedded) {
            Text(
                text = "Route guidance",
                style = guidanceLabelStyle,
                color = ClearRoadColors.RouteDecisionAccent.copy(alpha = 0.72f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClearRoadColors.RouteDecisionBlockFill)
                    .border(
                        width = 1.dp,
                        color = ClearRoadColors.RouteDecisionBlockBorder,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ClearRoadColors.HomeAccentStart,
                                    ClearRoadColors.HomeAccentEnd,
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "Choice",
                        style = choiceLabelStyle,
                        color = ClearRoadColors.HomeAccentStart.copy(alpha = 0.92f),
                    )
                    Spacer(modifier = Modifier.height(recGapLabelToBody))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ClearRoadColors.RouteDecisionChoicePlinthFill)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = selectedDecisionTitle,
                            style = choiceBodyStyle,
                            color = ClearRoadColors.RouteDecisionChoiceText,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "Choice",
                style = choiceLabelStyle,
                color = ClearRoadColors.ClearSkyBlue,
            )
            Spacer(modifier = Modifier.height(recGapLabelToBody))
            Text(
                text = selectedDecisionTitle,
                style = choiceBodyStyle,
                color = ClearRoadColors.RoadGrey,
            )
            Spacer(modifier = Modifier.height(recGapBetweenSections))
        }

        Text(
            text = "Why",
            style = secondaryLabelStyle,
            color = ClearRoadColors.ClearSkyBlue.copy(
                alpha = if (embedded) 0.58f else 1f,
            ),
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = selectedDecisionWhy,
            style = secondaryBodyStyle,
            color = if (embedded) {
                ClearRoadColors.HomeHeaderTitle.copy(alpha = 0.70f)
            } else {
                ClearRoadColors.RoadGrey
            },
        )
        Spacer(modifier = Modifier.height(recGapBetweenSections))

        Text(
            text = "Tip",
            style = secondaryLabelStyle,
            color = ClearRoadColors.ClearSkyBlue.copy(
                alpha = if (embedded) 0.58f else 1f,
            ),
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = selectedDecisionTip,
            style = secondaryBodyStyle,
            color = if (embedded) {
                ClearRoadColors.HomeHeaderTitle.copy(alpha = 0.66f)
            } else {
                ClearRoadColors.RoadGrey
            },
        )
    }
}
