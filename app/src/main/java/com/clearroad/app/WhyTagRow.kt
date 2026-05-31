package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.model.WhyTagUiModel
import com.clearroad.app.ui.theme.ClearRoadColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WhyTagRow(
    tags: List<WhyTagUiModel>,
    modifier: Modifier = Modifier,
    accentColor: Color = ClearRoadColors.ClearSkyBlue,
    compact: Boolean = false,
) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = tag.label,
                modifier = Modifier
                    .background(
                        color = accentColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = if (compact) 2.dp else 3.dp,
                    ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = ClearRoadColors.RoadGreyMuted,
            )
        }
    }
}
