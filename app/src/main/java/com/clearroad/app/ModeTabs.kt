package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.theme.ClearRoadColors
import com.clearroad.app.ui.theme.accentColor

@Composable
internal fun ModeTabs(
    selectedMode: PreferenceMode,
    onModeSelected: (PreferenceMode) -> Unit,
    tabVerticalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        PreferenceMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            val modeAccent = mode.accentColor()
            Text(
                text = modeTabLabel(mode),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onModeSelected(mode) }
                    .background(
                        color = if (selected) {
                            modeAccent.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(
                        vertical = tabVerticalPadding,
                        horizontal = 6.dp,
                    ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        modeAccent
                    } else {
                        ClearRoadColors.RoadGreyMuted
                    },
                    textDecoration = TextDecoration.None,
                ),
            )
        }
    }
}

private fun modeTabLabel(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> "Fastest"
        PreferenceMode.NO_TOLLS -> "No tolls"
        PreferenceMode.CALM -> "Calm"
    }
