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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor
import com.google.android.gms.maps.model.LatLng

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
            text = "Route ${model.routeNumber}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = modeAccent,
        )
        Spacer(modifier = Modifier.height(16.dp))

        RouteDetailsWhyCard(
            model = model,
            modeAccent = modeAccent,
            cardBorder = cardBorder,
        )
        Spacer(modifier = Modifier.height(12.dp))
        RouteDetailsTripCard(
            model = model,
            cardBorder = cardBorder,
        )
        if (model.fromLatLng != null && model.toLatLng != null) {
            Spacer(modifier = Modifier.height(12.dp))
            MapPreviewCard(
                fromLatLng = model.fromLatLng,
                toLatLng = model.toLatLng,
                routePathPoints = model.routePathPoints,
                cardBorder = cardBorder,
            )
            Spacer(modifier = Modifier.height(16.dp))
            RouteDetailsHandoffFooter(
                fromLatLng = model.fromLatLng,
                toLatLng = model.toLatLng,
                modeAccent = modeAccent,
            )
        }
    }
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
            Text(
                text = "Why this route",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
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
            if (model.recommendationConfidenceText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                RouteDetailsConfidenceCallout(
                    text = model.recommendationConfidenceText,
                    tradeoffText = model.recommendationTradeoffText,
                    isHighConfidence = model.isHighConfidence,
                    modeAccent = modeAccent,
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsConfidenceCallout(
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
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighConfidence) FontWeight.Medium else FontWeight.Normal,
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

@Composable
private fun RouteDetailsHandoffFooter(
    fromLatLng: LatLng,
    toLatLng: LatLng,
    modeAccent: Color,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { openGoogleMapsHandoff(context, fromLatLng, toLatLng) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "Open in Google Maps",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = modeAccent,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { openWazeHandoff(context, toLatLng) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "Open in Waze",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = modeAccent,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Opens your trip in Google Maps or Waze. Route may differ slightly.",
            style = MaterialTheme.typography.bodySmall,
            color = ClearRoadColors.RoadGreyMuted,
        )
    }
}
