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
internal fun RouteTitleBlock(
    personality: String,
    title: String,
    titleAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (personality.isNotBlank()) {
            Text(
                text = personality,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(1.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = ClearRoadColors.RoadGrey.copy(alpha = titleAlpha),
        )
    }
}
