package com.clearroad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    fuelCostAed: Int,
    tollAed: Int,
    confidenceLabel: String,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            text = "Route confidence",
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
                DetailsMetricRow(sectionLabel = "Fuel", valueText = "$fuelCostAed AED")
                DetailsMetricRow(sectionLabel = "Toll", valueText = "$tollAed AED", isLast = true)
            }
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
