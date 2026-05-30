package com.clearroad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Isolated route details panel for future Stage 17+ wiring (mini map, Waze/GMaps deep links).
 * Not used on the main decision screen yet; callers pass metrics from [RealRouteDebugData] and helpers.
 *
 * @param routeDisplayName Short label such as `"Route 2"` for the user's selection.
 * @param durationText Leg duration string from Directions (matches [RealRouteDebugData.durationText]).
 * @param distanceText Leg distance string from Directions (matches [RealRouteDebugData.distanceText]).
 * @param tollAed Salik estimate in AED (matches [RealRouteDebugData.tollAED]).
 * @param confidenceLabel e.g. `"Reliable"`, `"Estimated"`, `"Limited"` — from route confidence helpers.
 */
@Composable
internal fun RouteDetailsSurface(
    modifier: Modifier = Modifier,
    routeDisplayName: String,
    durationText: String,
    distanceText: String,
    tollAed: Int,
    confidenceLabel: String,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "Selected route",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface.copy(alpha = 0.94f),
            )
            Text(
                text = routeDisplayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.primary.copy(alpha = 0.9f),
            )
            Spacer(modifier = Modifier.height(12.dp))

            RouteDetailMetricRow(label = "Time", valueText = durationText)
            RouteDetailMetricRow(label = "Distance", valueText = distanceText)
            RouteDetailMetricRow(
                label = "Salik",
                valueText = if (tollAed > 0) "$tollAed AED" else "No Salik",
            )
            RouteDetailMetricRow(label = "Confidence", valueText = confidenceLabel)
        }
    }
}

@Composable
private fun RouteDetailMetricRow(
    label: String,
    valueText: String,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.93f),
        )
    }
}
