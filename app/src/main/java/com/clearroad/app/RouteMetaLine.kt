package com.clearroad.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.clearroad.app.ui.theme.ClearRoadColors

@Composable
internal fun RouteMetaLine(
    tollText: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = tollText,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
        ),
        color = ClearRoadColors.RoadGreyMuted,
    )
}
