package com.clearroad.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clearroad.app.ui.theme.ClearRoadColors

@Composable
internal fun AvailableRoutesHeader(
    routeCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Routes · $routeCount",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.35.sp,
        ),
        color = ClearRoadColors.RoadGrey,
    )
}
