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
import com.clearroad.app.ui.theme.ClearRoadColors

@Composable
internal fun RouteMetricsBlock(
    durationText: String,
    distanceText: String,
    durationOnSurfaceAlpha: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = durationText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = ClearRoadColors.RoadGrey.copy(alpha = durationOnSurfaceAlpha),
        )
        Spacer(modifier = Modifier.height(if (compact) 1.dp else 2.dp))
        Text(
            text = distanceText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = ClearRoadColors.RoadGreyMuted,
        )
    }
}
