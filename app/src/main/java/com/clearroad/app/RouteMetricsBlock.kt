package com.clearroad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RouteMetricsBlock(
    durationText: String,
    distanceText: String,
    durationOnSurfaceAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = durationText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = durationOnSurfaceAlpha,
            ),
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = distanceText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
        )
    }
}
