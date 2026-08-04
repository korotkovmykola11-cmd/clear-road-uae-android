package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.driveweather.DriveMoodBackground
import com.clearroad.app.ui.driveweather.DriveWeatherSection
import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RejectedAlternativeUiModel
import com.clearroad.app.ui.model.RouteDecisionRouteCardUiModel
import com.clearroad.app.ui.model.RouteGoogleMarshioDecisionUiModel
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor

/**
 * Route details panel for modal / sheet hosts. Wired from route list selection + metrics helpers.
 */
@Composable
internal fun RouteDetailsScreen(
    model: RouteDetailsUiModel,
) {
    val modeAccent = model.mode.accentColor()
    val cardBorder = BorderStroke(
        width = 1.dp,
        color = modeAccent.copy(alpha = 0.20f),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Route Details",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = ClearRoadColors.RoadGrey,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = model.routeIdentityTitle.ifBlank { "Route ${model.routeNumber}" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = modeAccent,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(16.dp))

        RouteDetailsStaticMapPreview(
            model = model,
            cardBorder = cardBorder,
        )

        if (model.driveWeather != null) {
            DriveMoodBackground(
                mood = model.driveWeather.hero.mood,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .padding(bottom = 4.dp),
            ) {
                DriveWeatherSection(
                    model = model.driveWeather,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        } else {
            RouteDetailsWhyCard(
                model = model,
                modeAccent = modeAccent,
                cardBorder = cardBorder,
            )
        }
        if (model.rejectedAlternatives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            RouteDetailsRejectedAlternativesCard(
                introText = model.rejectedAlternativesIntro,
                alternatives = model.rejectedAlternatives,
                cardBorder = cardBorder,
            )
        } else if (model.rejectedAlternativeLines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            RouteDetailsRejectedAlternativesLegacyCard(
                lines = model.rejectedAlternativeLines,
                cardBorder = cardBorder,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        RouteDetailsTripCard(
            model = model,
            cardBorder = cardBorder,
        )
        model.tripHistoryInsight?.lines?.takeIf { it.isNotEmpty() }?.let { lines ->
            Spacer(modifier = Modifier.height(12.dp))
            RouteDetailsTripHistoryCard(
                lines = lines,
                cardBorder = cardBorder,
            )
        }
        if (model.fromLatLng != null && model.toLatLng != null) {
            RouteDetailsIntelligenceSection(
                request = model.routeIntelligenceRequest,
                comparisonRequest = model.routeIntelligenceComparisonRequest,
                showSection = model.showRouteIntelligenceSection,
                unavailableReason = model.routeIntelligenceUnavailableReason,
                cardBorder = cardBorder,
            )
            Spacer(modifier = Modifier.height(16.dp))
            RouteDetailsHandoffFooter(
                model = model,
                modeAccent = modeAccent,
            )
        }
    }
}

@Composable
private fun RouteDetailsStaticMapPreview(
    model: RouteDetailsUiModel,
    cardBorder: BorderStroke,
) {
    val from = model.fromLatLng ?: return
    val to = model.toLatLng ?: return
    if (model.trafficSegments.isEmpty()) return

    val apiKey = BuildConfig.PLACES_API_KEY
    if (apiKey.isBlank()) return

    val staticMapUrl =
        remember(model.trafficSegments, from, to, apiKey) {
            buildStaticMapUrl(
                trafficSegments = model.trafficSegments,
                from = from,
                to = to,
                apiKey = apiKey,
            )
        }

    StaticMapPreview(
        url = staticMapUrl,
        cardBorder = cardBorder,
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun RouteDetailsWhyCard(
    model: RouteDetailsUiModel,
    modeAccent: Color,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "Why this route",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClearRoadColors.RoadGreyMuted,
                    modifier = Modifier.weight(1f),
                )
                YunoBrandBlock(compact = true)
            }
            if (model.whyTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                WhyTagRow(
                    tags = model.whyTags,
                    accentColor = modeAccent,
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            when {
                model.googleMarshioDecision != null -> {
                    RouteDetailsGoogleMarshioBlock(
                        decision = model.googleMarshioDecision,
                        modeAccent = modeAccent,
                    )
                }
                model.showLegacyWhyCopy -> {
                    Text(
                        text = model.routeReasonTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ClearRoadColors.RoadGrey,
                    )
                    if (model.routeReasonWhy.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = model.routeReasonWhy,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = ClearRoadColors.RoadGreyMuted,
                        )
                    }
                    if (model.recommendationConfidenceTitle.isNotBlank() ||
                        model.recommendationConfidenceText.isNotBlank()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        RouteDetailsConfidenceCallout(
                            title = model.recommendationConfidenceTitle,
                            text = model.recommendationConfidenceText,
                            tradeoffText = model.recommendationTradeoffText,
                            isHighConfidence = model.isHighConfidence,
                            modeAccent = modeAccent,
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Route comparison will appear when routes finish loading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
            }
        }
    }
}

private val MarshioPickGreen = Color(0xFF00C853)

@Composable
private fun RouteDetailsGoogleMarshioBlock(
    decision: RouteGoogleMarshioDecisionUiModel,
    modeAccent: Color,
) {
    Text(
        text = decision.headline,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = ClearRoadColors.RoadGrey,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = decision.subtext,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = ClearRoadColors.RoadGrey,
    )
    Spacer(modifier = Modifier.height(14.dp))

    when (decision.state) {
        GoogleMarshioDecisionState.DISAGREES -> {
            decision.googleRoute?.let { route ->
                RouteDetailsDecisionRouteCard(route = route, emphasized = false)
                Spacer(modifier = Modifier.height(10.dp))
            }
            decision.marshioRoute?.let { route ->
                RouteDetailsDecisionRouteCard(route = route, emphasized = true)
            }
            if (decision.disagreementReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Why?",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClearRoadColors.RoadGreyMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                decision.disagreementReasons.forEach { reason ->
                    Text(
                        text = "✓ $reason",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
            }
        }
        GoogleMarshioDecisionState.AGREES,
        GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
        -> {
            decision.singleRoute?.let { route ->
                RouteDetailsDecisionRouteCard(route = route, emphasized = true)
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = "Verdict",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = ClearRoadColors.RoadGreyMuted,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = decision.verdictText,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = modeAccent,
    )
}

@Composable
private fun RouteDetailsDecisionRouteCard(
    route: RouteDecisionRouteCardUiModel,
    emphasized: Boolean,
) {
    val borderColor =
        if (emphasized) MarshioPickGreen else ClearRoadColors.SalikNeutral.copy(alpha = 0.25f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (emphasized) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .background(
                color =
                    if (emphasized) MarshioPickGreen.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = route.cardTitle,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (emphasized) MarshioPickGreen else ClearRoadColors.RoadGreyMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = route.roadName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ClearRoadColors.RoadGrey,
        )
        Spacer(modifier = Modifier.height(6.dp))
        RouteDetailsMetricRow(label = "ETA", value = route.etaText)
        RouteDetailsMetricRow(label = "Distance", value = route.distanceText)
        route.trafficDelayText?.let { RouteDetailsMetricRow(label = "Traffic delay", value = it) }
        route.salikText?.let { RouteDetailsMetricRow(label = "Salik", value = it) }
    }
}

@Composable
private fun RouteDetailsMetricRow(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = ClearRoadColors.RoadGrey,
    )
}

@Composable
private fun RouteDetailsConfidenceCallout(
    title: String,
    text: String,
    tradeoffText: String?,
    isHighConfidence: Boolean,
    modeAccent: Color,
) {
    val backgroundColor =
        if (isHighConfidence) {
            ClearRoadColors.CloudHighlight.copy(alpha = 0.92f)
        } else {
            ClearRoadColors.SalikNeutral.copy(alpha = 0.07f)
        }
    val borderColor =
        if (isHighConfidence) {
            modeAccent.copy(alpha = 0.36f)
        } else {
            ClearRoadColors.SalikNeutral.copy(alpha = 0.24f)
        }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ClearRoadColors.RoadGrey,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighConfidence && title.isBlank()) FontWeight.Medium else FontWeight.Normal,
            ),
            color = ClearRoadColors.RoadGrey,
        )
        if (!tradeoffText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tradeoffText,
                style = MaterialTheme.typography.bodySmall,
                color = ClearRoadColors.RoadGreyMuted,
            )
        }
    }
}

@Composable
private fun RouteDetailsRejectedAlternativesCard(
    introText: String,
    alternatives: List<RejectedAlternativeUiModel>,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Other Google alternatives",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            if (introText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = introText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClearRoadColors.RoadGreyMuted,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            alternatives.forEachIndexed { index, alternative ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = alternative.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ClearRoadColors.RoadGrey,
                )
                Spacer(modifier = Modifier.height(4.dp))
                alternative.advantageLines.forEach { advantage ->
                    Text(
                        text = "✅ $advantage",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
                alternative.drawbackLines.forEach { drawback ->
                    Text(
                        text = "❌ $drawback",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ClearRoadColors.RoadGrey,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alternative.verdictText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = ClearRoadColors.RoadGreyMuted,
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsRejectedAlternativesLegacyCard(
    lines: List<String>,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Other Google alternatives",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
            lines.forEachIndexed { index, line ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = ClearRoadColors.RoadGrey,
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsTripCard(
    model: RouteDetailsUiModel,
    cardBorder: BorderStroke,
) {
    val salikValueText =
        if (model.tollAed > 0) "${model.tollAed} AED" else "No Salik"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Trip at a glance",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RouteDetailsMetricColumn(
                    label = "Time",
                    value = model.durationText,
                    valueBold = true,
                    modifier = Modifier.weight(1f),
                )
                RouteDetailsMetricColumn(
                    label = "Distance",
                    value = model.distanceText,
                    modifier = Modifier.weight(1f),
                )
                RouteDetailsMetricColumn(
                    label = "Salik",
                    value = salikValueText,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = model.confidenceLabel,
                modifier = Modifier
                    .background(
                        color = ClearRoadColors.SalikNeutral.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = model.costSummaryPrimary,
                style = MaterialTheme.typography.bodyMedium,
                color = ClearRoadColors.RoadGreyMuted,
            )
            if (!model.costSummarySecondary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = model.costSummarySecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearRoadColors.RoadGreyMuted,
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsTripHistoryCard(
    lines: List<String>,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = ClearRoadColors.RouteCardSurfaceMuted,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Your usual pattern for this trip",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            lines.forEachIndexed { index, line ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearRoadColors.RoadGrey,
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueBold: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClearRoadColors.RoadGreyMuted,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (valueBold) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = ClearRoadColors.RoadGrey,
        )
    }
}
