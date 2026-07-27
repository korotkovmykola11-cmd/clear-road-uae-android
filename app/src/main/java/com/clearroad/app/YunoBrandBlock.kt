package com.clearroad.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.theme.ClearRoadColors

/**
 * Reusable YUNO brand block — Stage 29.0c / 29.1a.
 *
 * @param corner When true, text sits left of the mark with the character at the trailing edge
 *   (Home header placement).
 */
@Composable
internal fun YunoBrandBlock(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    corner: Boolean = false,
) {
    val markSize = if (compact) 42.dp else 100.dp
    val textGap = if (compact) 8.dp else 10.dp
    val textAlign = if (corner) TextAlign.End else TextAlign.Start

    @Composable
    fun YunoLabels() {
        Column(horizontalAlignment = if (corner) Alignment.End else Alignment.Start) {
            Text(
                text = "YUNO",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                ),
                color = ClearRoadColors.ClearSkyBlue,
                textAlign = textAlign,
            )
            Spacer(modifier = Modifier.height(if (compact) 1.dp else 2.dp))
            Text(
                text = "Your UAE Road Advisor",
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = ClearRoadColors.RoadGreyMuted,
                textAlign = textAlign,
            )
        }
    }

    @Composable
    fun YunoMark() {
        Image(
            painter = painterResource(R.drawable.yuno_character),
            contentDescription = "YUNO",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(markSize),
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = if (corner) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (corner) {
            YunoLabels()
            Spacer(modifier = Modifier.width(textGap))
            YunoMark()
        } else {
            YunoMark()
            Spacer(modifier = Modifier.width(textGap))
            YunoLabels()
        }
    }
}
