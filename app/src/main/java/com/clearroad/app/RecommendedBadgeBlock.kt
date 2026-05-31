package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.theme.ClearRoadColors

@Composable
internal fun RecommendedBadgeBlock(
    nuance: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
) {
    val chipStyle = MaterialTheme.typography.labelSmall
    Column(modifier = modifier) {
        Text(
            text = "Recommended",
            modifier = Modifier
                .background(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = if (compact) 0.dp else 1.dp,
                ),
            style = chipStyle.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
        )
        if (nuance.isNotBlank()) {
            Text(
                text = nuance,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = if (compact) 12.sp else 13.sp,
                ),
                color = ClearRoadColors.RoadGreyMuted,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
            )
        }
    }
}
