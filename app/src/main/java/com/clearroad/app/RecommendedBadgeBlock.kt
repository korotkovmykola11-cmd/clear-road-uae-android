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
                .padding(horizontal = 10.dp, vertical = 2.dp),
            style = chipStyle.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
        )
        Text(
            text = nuance,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
            ),
            color = ClearRoadColors.RoadGreyMuted,
        )
    }
}
