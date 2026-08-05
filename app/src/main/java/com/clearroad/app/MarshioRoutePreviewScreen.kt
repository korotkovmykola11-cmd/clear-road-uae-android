package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearroad.app.triphistory.HandoffDecisionRecorder
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.clearroad.app.ui.theme.ClearRoadColors

@Composable
internal fun RouteDetailsHandoffFooter(
    model: RouteDetailsUiModel,
    modeAccent: Color,
    modifier: Modifier = Modifier,
    showWazeHandoff: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fromLatLng = model.fromLatLng ?: return
    val toLatLng = model.toLatLng ?: return
    val hasSalik = model.tollAed > 0
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                HandoffDecisionRecorder.recordHandoffAsync(
                    scope = scope,
                    context = context,
                    origin = fromLatLng,
                    destination = toLatLng,
                    durationSeconds = model.durationSeconds,
                    mode = model.mode,
                    hasSalik = hasSalik,
                    tollAed = model.tollAed.takeIf { it > 0 }?.toDouble(),
                    snapshot = model.handoffSnapshot,
                )
                openGoogleMapsHandoff(
                    context = context,
                    origin = fromLatLng,
                    destination = toLatLng,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "Open in Google Maps",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = modeAccent,
            )
        }
        if (showWazeHandoff) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    HandoffDecisionRecorder.recordHandoffAsync(
                        scope = scope,
                        context = context,
                        origin = fromLatLng,
                        destination = toLatLng,
                        durationSeconds = model.durationSeconds,
                        mode = model.mode,
                        hasSalik = hasSalik,
                        tollAed = model.tollAed.takeIf { it > 0 }?.toDouble(),
                        snapshot = model.handoffSnapshot,
                    )
                    openWazeHandoff(context, toLatLng)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = "Open in Waze",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = modeAccent,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Opens navigation to your destination in Google Maps or Waze. Route may differ slightly.",
                style = MaterialTheme.typography.bodySmall,
                color = ClearRoadColors.RoadGreyMuted,
            )
        }
    }
}
