package com.clearroad.app.legacy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.clearroad.app.ui.model.MarshallRecommendationBannerUiModel
import com.clearroad.app.ui.theme.ClearRoadColors

private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"

private val MarshallBannerShape = RoundedCornerShape(16.dp)
private val MarshallBannerHaloSize = 82.dp
private val MarshallBannerMascotSize = 68.dp

/** Rollback-only Home banner — used when [com.clearroad.app.ArchitectureValidation.RECOMMENDATION_ONLY_HOME] is false. */
@Composable
internal fun MarshallRecommendationBanner(
    model: MarshallRecommendationBannerUiModel,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MarshallBannerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ClearRoadColors.ExecutiveNavyStart,
                        ClearRoadColors.ExecutiveNavyEnd,
                    ),
                ),
            )
            .border(
                width = 1.5.dp,
                color = ClearRoadColors.ExecutiveGold,
                shape = MarshallBannerShape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(MarshallBannerHaloSize),
        ) {
            Box(
                modifier = Modifier
                    .size(MarshallBannerHaloSize)
                    .clip(CircleShape)
                    .background(
                        ClearRoadColors.ExecutiveGold.copy(alpha = 0.14f),
                    )
                    .border(
                        width = 1.dp,
                        color = ClearRoadColors.ExecutiveGold.copy(alpha = 0.35f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                MarshallBannerYunoMark(
                    modifier = Modifier.size(MarshallBannerMascotSize),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "YUNO advises",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 0.3.sp,
                ),
                color = ClearRoadColors.ExecutiveBodyMuted,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (model.ready) {
                Text(
                    text = model.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                    ),
                    color = ClearRoadColors.ExecutiveGold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = model.metricsLine,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                    ),
                    color = ClearRoadColors.ExecutiveCream,
                    maxLines = 1,
                )
                if (model.routeIdentity.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = model.routeIdentity,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        ),
                        color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.92f),
                        maxLines = 2,
                    )
                }
                if (model.whyLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Why: ${model.whyLine}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        ),
                        color = ClearRoadColors.ExecutiveBodyMuted,
                        maxLines = 3,
                    )
                }
                if (onViewDetails != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "View Details",
                        modifier = Modifier.clickable(onClick = onViewDetails),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        ),
                        color = ClearRoadColors.ExecutiveGold,
                    )
                }
            } else {
                Text(
                    text = "YUNO is ready to help choose the best route.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    ),
                    color = ClearRoadColors.ExecutiveCream.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun MarshallBannerYunoMark(modifier: Modifier = Modifier) {
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
