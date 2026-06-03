package com.clearroad.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.model.RecommendationSurfaceUiModel
import com.clearroad.app.ui.theme.ClearRoadColors

private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"

private val DecisionPanelShape = RoundedCornerShape(24.dp)
// Stage 32.1 — ~30% expansion for at-a-glance dominance (visual only).
private val DecisionPanelMinHeightReady = 350.dp
private val DecisionPanelMinHeightIdle = 262.dp
private val AdvisorMarkSize = 36.dp

/** Stage 32.1 — decision surface dominance scale-up; visual hierarchy only. */
@Composable
internal fun RecommendationSurface(
    model: RecommendationSurfaceUiModel,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
) {
    val panelMinHeight =
        when {
            model.ready -> DecisionPanelMinHeightReady
            else -> DecisionPanelMinHeightIdle
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = panelMinHeight)
            .clip(DecisionPanelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ClearRoadColors.ExecutiveNavyStart,
                        ClearRoadColors.ExecutiveNavyEnd,
                    ),
                ),
            )
            .border(
                width = 2.dp,
                color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.85f),
                shape = DecisionPanelShape,
            )
            .padding(horizontal = 29.dp, vertical = 29.dp),
    ) {
        DecisionSurfaceAdvisorMark()
        Spacer(modifier = Modifier.height(14.dp))

        when {
            model.loading -> {
                Spacer(modifier = Modifier.height(31.dp))
                Text(
                    text = model.loadingMessage,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                    ),
                    color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.88f),
                )
            }
            model.ready -> {
                if (model.routeName.isNotBlank()) {
                    Text(
                        text = model.routeName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            lineHeight = 29.sp,
                            letterSpacing = 0.2.sp,
                        ),
                        color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.94f),
                        maxLines = 2,
                    )
                    Spacer(modifier = Modifier.height(13.dp))
                }
                Text(
                    text = model.travelTime,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 68.sp,
                        lineHeight = 72.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = ClearRoadColors.ExecutiveCream,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.28f),
                )
                Spacer(modifier = Modifier.height(21.dp))
                Text(
                    text = model.decisionSummary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = 1.6.sp,
                    ),
                    color = ClearRoadColors.ExecutiveGold,
                )
                if (model.confidenceDisplay.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = model.confidenceDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 23.sp,
                            lineHeight = 29.sp,
                        ),
                        color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.95f),
                    )
                }
                if (model.narrative.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = model.narrative,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                        ),
                        color = ClearRoadColors.ExecutiveBodyMuted,
                        maxLines = 3,
                    )
                }
                if (onViewDetails != null) {
                    Spacer(modifier = Modifier.height(26.dp))
                    Text(
                        text = "View Details",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onViewDetails)
                            .padding(vertical = 13.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            letterSpacing = 0.3.sp,
                        ),
                        color = ClearRoadColors.ExecutiveGold,
                    )
                }
            }
            else -> {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = model.emptyTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                    ),
                    color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.height(13.dp))
                Text(
                    text = model.emptySubtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                    ),
                    color = ClearRoadColors.ExecutiveBodyMuted,
                )
            }
        }
    }
}

@Composable
private fun DecisionSurfaceAdvisorMark() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .size(AdvisorMarkSize)
                .clip(CircleShape)
                .background(
                    ClearRoadColors.ExecutiveGold.copy(alpha = 0.12f),
                )
                .border(
                    width = 1.dp,
                    color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            RecommendationSurfaceYunoMark(
                modifier = Modifier.size(AdvisorMarkSize - 6.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "YUNO advises",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
            ),
            color = ClearRoadColors.ExecutiveBodyMuted,
        )
    }
}

@Composable
private fun RecommendationSurfaceYunoMark(modifier: Modifier = Modifier) {
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
            modifier = modifier,
        )
    }
}
