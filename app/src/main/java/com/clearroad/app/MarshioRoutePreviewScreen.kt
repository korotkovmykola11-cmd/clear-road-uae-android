package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearroad.app.guidance.MarshioGuidanceLaunch
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
    val fromLatLng = model.fromLatLng ?: return
    val toLatLng = model.toLatLng ?: return
    val marshioRoutePath = model.handoffRoutePathPoints
    val hasMarshioPath = marshioRoutePath.size >= 2
    val showGuidanceEntry = MarshioGuidanceLaunch.showEntryInRouteDetails(model)
    Column(modifier = modifier.fillMaxWidth()) {
        if (showGuidanceEntry) {
            Button(
                onClick = {
                    MarshioGuidanceLaunch.argsFromRouteDetails(
                        model,
                        googleSteps =
                            model.marshioGuidanceGoogleSteps.map { step ->
                                DirectionsStepRecord(
                                    distanceMeters = step.distanceMeters,
                                    maneuver = step.maneuver,
                                    htmlInstructions = step.htmlInstructions,
                                    startLocation = step.startLocation,
                                )
                            },
                    )?.let { args ->
                        context.startActivity(MarshioGuidanceLaunch.createIntent(context, args))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D9E75),
                        contentColor = Color.White,
                    ),
            ) {
                Text(
                    text = "Follow with MARSHIO",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = {
                openGoogleMapsHandoff(
                    context = context,
                    origin = fromLatLng,
                    destination = toLatLng,
                    marshioRoutePath = marshioRoutePath,
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
                onClick = { openWazeHandoff(context, toLatLng) },
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
                text =
                    if (hasMarshioPath) {
                        "Opens Google Maps on MARSHIO's chosen route. Google may still adjust slightly for live traffic."
                    } else {
                        "Opens your trip in Google Maps or Waze. Route may differ slightly."
                    },
                style = MaterialTheme.typography.bodySmall,
                color = ClearRoadColors.RoadGreyMuted,
            )
        }
    }
}
