package com.clearroad.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.model.RecommendationSurfaceUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor
import kotlinx.coroutines.delay

private const val YUNO_CHARACTER_DRAWABLE = "yuno_character"
private const val RecommendationSectionLabel = "MARSHIO Recommends One Route"

private val HomeProductCardShape = RoundedCornerShape(14.dp)
private val HomeProductCardBg = Color.White
private val HomeProductCardBorder = ClearRoadColors.SalikNeutral.copy(alpha = 0.20f)
private val HomeProductLabelColor = ClearRoadColors.RoadGreyMuted
private val HomeConfidenceChipColor = ClearRoadColors.RoadGreyMuted.copy(alpha = 0.72f)
private val HomeProductTitleColor = ClearRoadColors.RoadGrey
private val HomeProductBodyColor = ClearRoadColors.RoadGreyMuted

private val RecommendationCardPaddingHorizontal = 20.dp
private val RecommendationCardPaddingVertical = 22.dp
private val RecommendationCompareFactors = listOf("Time", "Salik", "Traffic")
private val RecommendationYunoMarkSize = 55.dp
private val RecommendationYunoAdvisorGap = 12.dp

private const val RecommendationEmptyHero = "Enter your route"
private const val RecommendationEmptyCompareLead = "We compare:"
private const val RecommendationEmptyClosing =
    "and recommends ONE route for your selected mode."
private const val RecommendationRefreshFeedbackMessage = "Updated just now"
private const val RecommendationRefreshFeedbackDurationMs = 5_000L

private fun Modifier.homeRecommendationCardSurface(): Modifier =
    shadow(
        elevation = 3.dp,
        shape = HomeProductCardShape,
        ambientColor = Color.Black.copy(alpha = 0.05f),
        spotColor = Color.Black.copy(alpha = 0.07f),
    )
        .clip(HomeProductCardShape)
        .background(HomeProductCardBg)
        .border(
            width = 1.dp,
            color = HomeProductCardBorder,
            shape = HomeProductCardShape,
        )

/** Home recommendation card — the answer lives here. */
@Composable
internal fun RecommendationSurface(
    model: RecommendationSurfaceUiModel,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
    onRefreshRoute: (() -> Unit)? = null,
) {
    var refreshConfirmKey by remember { mutableIntStateOf(0) }
    val showRefreshFeedback = refreshConfirmKey > 0
    val wrappedOnRefreshRoute =
        onRefreshRoute?.let { callback ->
            {
                callback()
                refreshConfirmKey++
                Unit
            }
        }

    LaunchedEffect(refreshConfirmKey, model.loading, model.ready) {
        if (refreshConfirmKey == 0 || model.loading || !model.ready) return@LaunchedEffect
        delay(RecommendationRefreshFeedbackDurationMs)
        refreshConfirmKey = 0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .homeRecommendationCardSurface()
            .padding(
                horizontal = RecommendationCardPaddingHorizontal,
                vertical = RecommendationCardPaddingVertical,
            ),
    ) {
        when {
            model.loading -> {
                RecommendationSectionLabel()
                Text(
                    text = model.loadingMessage,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    ),
                    color = HomeProductBodyColor,
                )
                Spacer(modifier = Modifier.height(16.dp))
                RecommendationYunoOwnerBlock()
            }
            model.ready -> {
                RecommendationSectionLabel()
                RecommendationReadyContent(
                    model = model,
                    onViewDetails = onViewDetails,
                    onRefreshRoute = wrappedOnRefreshRoute,
                    showRefreshFeedback = showRefreshFeedback,
                )
            }
            else -> {
                RecommendationEmptyState()
            }
        }
    }
}

@Composable
private fun RecommendationReadyContent(
    model: RecommendationSurfaceUiModel,
    onViewDetails: (() -> Unit)?,
    onRefreshRoute: (() -> Unit)?,
    showRefreshFeedback: Boolean,
) {
    Text(
        text = model.travelTime,
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 68.sp,
            lineHeight = 72.sp,
            letterSpacing = (-0.5).sp,
        ),
        color = HomeProductTitleColor,
        maxLines = 1,
    )
    if (model.routeName.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.routeName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                lineHeight = 25.sp,
            ),
            color = HomeProductTitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (model.recommendationReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        RecommendationReasonChip(text = model.recommendationReason)
    }
    if (model.comparativeEvidenceLines.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        model.comparativeEvidenceLines.forEachIndexed { index, line ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                    fontSize = if (index == 0) 15.sp else 17.sp,
                    lineHeight = if (index == 0) 22.sp else 25.sp,
                ),
                color = if (index == 0) HomeProductLabelColor else HomeProductBodyColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (model.decisionSummary.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.decisionSummary,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                lineHeight = 25.sp,
            ),
            color = HomeProductTitleColor,
            maxLines = 1,
        )
    }
    if (model.narrative.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.narrative,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 25.sp,
            ),
            color = HomeProductBodyColor,
            maxLines = 2,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    RecommendationYunoOwnerBlock()
    if (onRefreshRoute != null) {
        Spacer(modifier = Modifier.height(12.dp))
        if (showRefreshFeedback) {
            Text(
                text = RecommendationRefreshFeedbackMessage,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
                color = HomeConfidenceChipColor,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = "Refresh route",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRefreshRoute)
                .padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
            color = HomeProductTitleColor,
            textAlign = TextAlign.Center,
        )
    }
    if (onViewDetails != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Why This Route",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = HomeProductCardBorder,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onViewDetails)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
            color = HomeProductTitleColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecommendationReasonChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = ClearRoadColors.SalikNeutral.copy(alpha = 0.10f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        ),
        color = HomeProductBodyColor,
        maxLines = 1,
    )
}

@Composable
private fun RecommendationBadgeChip(
    text: String,
    accentColor: Color,
) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        ),
        color = accentColor,
        maxLines = 1,
    )
}

@Composable
private fun RecommendationSectionLabel() {
    Text(
        text = RecommendationSectionLabel,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        color = HomeProductLabelColor,
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun RecommendationEmptyState() {
    Column(modifier = Modifier.fillMaxWidth()) {
        RecommendationSectionLabel()
        Text(
            text = RecommendationEmptyHero,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
            ),
            color = HomeProductTitleColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        RecommendationCompareSection()
        Spacer(modifier = Modifier.height(16.dp))
        RecommendationYunoOwnerBlock()
    }
}

@Composable
private fun RecommendationCompareSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = RecommendationEmptyCompareLead,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            color = HomeProductTitleColor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        RecommendationCompareFactorList()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = RecommendationEmptyClosing,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            color = HomeProductBodyColor,
        )
    }
}

@Composable
private fun RecommendationCompareFactorList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecommendationCompareFactors.forEach { factor ->
            Text(
                text = "• $factor",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                color = HomeProductBodyColor,
            )
        }
    }
}

@Composable
private fun RecommendationYunoOwnerBlock(modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (characterDrawableId != 0) {
            Image(
                painter = painterResource(characterDrawableId),
                contentDescription = "YUNO",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(RecommendationYunoMarkSize),
            )
            Spacer(modifier = Modifier.width(RecommendationYunoAdvisorGap))
        }
        Text(
            text = "Your UAE Road Advisor",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            color = ClearRoadColors.RoadGreyMuted,
        )
    }
}
