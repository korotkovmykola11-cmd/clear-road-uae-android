package com.clearroad.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.unit.dp

/**
 * Route details panel for modal / sheet hosts. Wired from route list selection + metrics helpers.
 */
@Composable
internal fun RouteDetailsScreen(
    routeNumber: Int,
    routeReasonTitle: String,
    routeReasonWhy: String,
    durationText: String,
    distanceText: String,
    tollAed: Int,
    confidenceLabel: String,
    costSummaryPrimary: String,
    costSummarySecondary: String? = null,
    decisionSnapshotRecommendedHeading: String,
    decisionSnapshotRecommendedSummary: String,
    decisionSnapshotOthersHeading: String,
    decisionSnapshotOthersSummary: String,
    recommendationConfidenceText: String,
    fromLatLng: LatLng? = null,
    toLatLng: LatLng? = null,
) {
    val scheme = MaterialTheme.colorScheme
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
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Route $routeNumber",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.primary.copy(alpha = 0.92f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Why this route",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = routeReasonTitle,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface.copy(alpha = 0.94f),
        )
        if (routeReasonWhy.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = routeReasonWhy,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Route read",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = confidenceLabel,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface.copy(alpha = 0.92f),
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 18.dp),
            color = scheme.outline.copy(alpha = 0.24f),
        )
        Text(
            text = "Trip data",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = scheme.surfaceVariant.copy(alpha = 0.42f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                DetailsMetricRow(sectionLabel = "Time", valueText = durationText)
                DetailsMetricRow(sectionLabel = "Distance", valueText = distanceText)
                DetailsMetricRow(
                    sectionLabel = "Salik",
                    valueText = if (tollAed > 0) "$tollAed AED" else "No Salik",
                    isLast = true,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Salik summary",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = costSummaryPrimary,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
        )
        if (!costSummarySecondary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = costSummarySecondary,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Decision snapshot",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = decisionSnapshotRecommendedHeading,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface.copy(alpha = 0.94f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = decisionSnapshotRecommendedSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = decisionSnapshotOthersHeading,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface.copy(alpha = 0.94f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = decisionSnapshotOthersSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Recommendation confidence",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = recommendationConfidenceText,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
        )
        if (fromLatLng != null && toLatLng != null) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Open in Google Maps",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openGoogleMapsHandoff(context, fromLatLng, toLatLng)
                    },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = scheme.primary.copy(alpha = 0.92f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open in Waze",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openWazeHandoff(context, toLatLng)
                    },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = scheme.primary.copy(alpha = 0.92f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text =
                    "Opens your trip in Google Maps or Waze. Route may differ slightly.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun DetailsMetricRow(
    sectionLabel: String,
    valueText: String,
    isLast: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = sectionLabel,
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = valueText,
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurface.copy(alpha = 0.82f),
    )
    if (!isLast) {
        Spacer(modifier = Modifier.height(12.dp))
    }
}
