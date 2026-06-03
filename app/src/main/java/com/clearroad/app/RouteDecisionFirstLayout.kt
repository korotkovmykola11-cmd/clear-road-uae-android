package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.model.RouteCardUiModel
import com.clearroad.app.ui.theme.ClearRoadColors

/** MARSHIO Home v2.1 — Decision First route card hierarchy (visual only). */
@Composable
internal fun DecisionFirstRouteCardContent(
    cardModel: RouteCardUiModel,
    personality: String,
    modeAccent: Color,
    showUserSelectedChrome: Boolean,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reasonLine =
        decisionFirstReasonLine(
            whyTags = cardModel.whyTags,
            recommendedNuance = cardModel.recommendedNuance,
            isRecommended = cardModel.isRecommended,
            personality = personality,
        )
    val supportingTags =
        decisionFirstSupportingWhyTags(
            whyTags = cardModel.whyTags,
            reasonLine = reasonLine,
        )
    val tollSummary = tollOnlyFromSalikLine(cardModel.salikLine)
    val cardLineGap = 2.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = reasonLine,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                ),
                color = ClearRoadColors.RoadGrey,
                maxLines = 2,
            )
            if (showUserSelectedChrome) {
                Text(
                    text = "View details →",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(modeAccent.copy(alpha = 0.10f))
                        .clickable(onClick = onViewDetails)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = modeAccent.copy(alpha = 0.92f),
                )
            }
        }
        Spacer(modifier = Modifier.height(cardLineGap))
        Text(
            text = cardModel.durationText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
            ),
            color = ClearRoadColors.RoadGrey.copy(
                alpha = cardModel.durationOnSurfaceAlpha,
            ),
        )
        Spacer(modifier = Modifier.height(cardLineGap))
        Text(
            text = cardModel.routeTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = ClearRoadColors.RoadGrey.copy(alpha = 0.88f),
        )
        if (personality.isNotBlank()) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = personality,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = ClearRoadColors.RoadGreyMuted,
                maxLines = 2,
            )
        }
        Spacer(modifier = Modifier.height(cardLineGap))
        Text(
            text = buildString {
                append(cardModel.distanceText)
                append(" · ")
                append(tollSummary)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = ClearRoadColors.RoadGreyMuted,
        )
        if (cardModel.confidence.isNotBlank()) {
            Spacer(modifier = Modifier.height(1.dp))
            RouteFuelConfidenceLine(text = cardModel.confidence)
        }
        if (supportingTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(cardLineGap))
            WhyTagRow(
                tags = supportingTags,
                accentColor = modeAccent,
                compact = true,
            )
        }
        if (showUserSelectedChrome) {
            Spacer(modifier = Modifier.height(1.dp))
            SelectedLabelBlock(
                labelAlpha = if (cardModel.isRecommended) 0.46f else 0.54f,
            )
        }
    }
}
