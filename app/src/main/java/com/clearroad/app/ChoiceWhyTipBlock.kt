package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.clearroad.app.ui.model.ChoiceWhyTipUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
