package com.clearroad.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SelectedLabelBlock(
    labelAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Selected",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = labelAlpha),
    )
}
