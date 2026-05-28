package com.clearroad.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AvailableRoutesHeader(
    routeCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Available routes: $routeCount",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
