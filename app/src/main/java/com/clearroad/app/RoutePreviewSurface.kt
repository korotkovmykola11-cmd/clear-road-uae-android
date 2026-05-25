package com.clearroad.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Abstract UAE movement mood for preview only — not navigation or geography. */
private fun previewUaeCorridorFeelingPhrase(corridorScanText: String): String? {
    val trimmed = corridorScanText.trim()
    if (trimmed.isEmpty()) return null
    val lc = trimmed.lowercase(Locale.US)
    val moods: List<Triple<Int, String, List<String>>> =
        listOf(
            Triple(
                10,
                "Airport corridor",
                listOf(
                    "dubai airport",
                    "international airport",
                    "al maktoum",
                    "jebel ali airport",
                    "terminal 3",
                    "terminal 2",
                    "terminal 1",
                    " dxb",
                    "dxb ",
                    " dwc",
                    "dwc ",
                ),
            ),
            Triple(
                15,
                "SZR flow",
                listOf(
                    "sheikh zayed road",
                    "sheikh zayed",
                    "szr",
                    "e11",
                ),
            ),
            Triple(
                20,
                "Business district flow",
                listOf(
                    "business bay",
                    "financial centre",
                    "financial center",
                    "difc",
                    "downtown dubai",
                    "trade centre",
                    "trade center",
                ),
            ),
            Triple(
                25,
                "Marina side",
                listOf(
                    "dubai marina",
                    "marina walk",
                    "jumeirah beach residence",
                    " jbr",
                    "jbr ",
                ),
            ),
            Triple(
                30,
                "Coastal direction",
                listOf(
                    "palm jumeirah",
                    "palm island",
                    "jumeirah beach",
                    "kite beach",
                    "jumeirah ",
                ),
            ),
            Triple(
                35,
                "Northern emirate stretch",
                listOf(
                    "sharjah",
                    "ajman",
                ),
            ),
            Triple(
                40,
                "Main motorway mood",
                listOf(
                    "mohammed bin zayed",
                    "mbz road",
                    "emirates road",
                    "e311",
                    "e611",
                ),
            ),
            Triple(
                45,
                "City entry stretch",
                listOf(
                    "al garhoud",
                    "garhoud",
                    "port saeed",
                ),
            ),
            Triple(
                50,
                "Inner-city weave",
                listOf(
                    "deira",
                    "bur dubai",
                    "karama",
                    "satwa",
                ),
            ),
            Triple(
                55,
                "Capitalward stretch",
                listOf(
                    "abu dhabi",
                    "al ain",
                    "shahama",
                ),
            ),
        )
    val labels =
        moods
            .filter { (_, _, needles) -> needles.any { lc.contains(it) } }
            .sortedBy { it.first }
            .distinctBy { it.second }
            .take(2)
            .map { it.second }
    return if (labels.isNotEmpty()) {
        labels.joinToString(" · ")
    } else {
        "Everyday UAE corridor stretch"
    }
}

@Composable
internal fun RoutePreviewSurface(
    previewRouteIndex: Int,
    previewRouteItem: RealRouteDebugData,
    originText: String,
    destinationText: String,
) {
    val scheme = MaterialTheme.colorScheme
    val muted = scheme.onSurfaceVariant
    val corridorMoodPhrase =
        previewUaeCorridorFeelingPhrase(previewRouteItem.corridorScanText)
    val contextText =
        when {
            corridorMoodPhrase != null -> "UAE context · $corridorMoodPhrase"
            else -> {
                val from = originText.trim().ifBlank { "Start" }
                val to = destinationText.trim().ifBlank { "End" }
                "Your route · $from → $to"
            }
        }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = scheme.surfaceVariant.copy(alpha = 0.118f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.084f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text =
                    "Route ${previewRouteIndex + 1} · " +
                        previewRouteItem.durationText +
                        " · " +
                        previewRouteItem.distanceText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.012.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = muted.copy(alpha = 0.47f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contextText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.015.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = muted.copy(alpha = 0.34f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
