package com.clearroad.app.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val SIMULATION_TICK_MS = 250L

@Composable
internal fun MarshioGuidanceRouteScreen(
    session: MarshioGuidanceSession,
    modifier: Modifier = Modifier,
    useSimulation: Boolean = false,
    showRestartSimulation: Boolean = false,
) {
    if (session.marshioPath.size < 2) {
        MarshioGuidanceUnavailableGeometryContent(modifier = modifier)
        return
    }

    val guidanceState =
        if (useSimulation) {
            rememberSimulatedGuidanceState(session)
        } else {
            rememberGpsGuidanceState(session)
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Following MARSHIO route",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = session.routeName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MarshioGuidanceStatusPanel(
            state = guidanceState.state,
            session = session,
            locationStatus = if (useSimulation) null else guidanceState.locationStatus,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MarshioGuidanceManeuverBanner(
            state = guidanceState.state,
            steps = session.steps,
        )
        MarshioGuidanceMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            fromLatLng = session.fromLatLng,
            toLatLng = session.toLatLng,
            marshioPath = session.marshioPath,
            googlePath = session.googlePath,
            guidancePosition = guidanceState.state?.position,
            guidanceBearing = guidanceState.state?.bearingDegrees,
            followCamera = !useSimulation && guidanceState.state != null,
            useSimulatedMarkerLabel = useSimulation,
        )
        if (showRestartSimulation && useSimulation) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { guidanceState.restartSimulation?.invoke() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restart simulation")
            }
        }
    }
}

@Composable
private fun MarshioGuidanceManeuverBanner(
    state: MarshioGuidanceSimulation.State?,
    steps: List<GuidanceStepUi>,
) {
    if (state == null) return

    val bannerText =
        when {
            state.offRoute -> "Off route"
            steps.isEmpty() -> null
            else -> {
                val stepIndex =
                    MarshioGuidanceStepTracker.currentStepIndex(
                        traveledMeters = state.traveledMeters,
                        steps = steps,
                    )
                if (stepIndex == -1) null else steps[stepIndex].instruction
            }
        } ?: return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Text(
            text = bannerText,
            modifier = Modifier.padding(8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private data class GuidanceStateHolder(
    val state: MarshioGuidanceSimulation.State?,
    val locationStatus: MarshioGuidanceLocationStatus? = null,
    val restartSimulation: (() -> Unit)? = null,
)

@Composable
private fun rememberSimulatedGuidanceState(session: MarshioGuidanceSession): GuidanceStateHolder {
    var simulationStartMs by remember(session) { mutableLongStateOf(System.currentTimeMillis()) }
    var simulationNowMs by remember(session) { mutableLongStateOf(simulationStartMs) }

    LaunchedEffect(session, simulationStartMs) {
        if (session.marshioPath.size < 2) return@LaunchedEffect
        while (true) {
            simulationNowMs = System.currentTimeMillis()
            delay(SIMULATION_TICK_MS)
        }
    }

    val progress =
        MarshioGuidanceSimulation.simulationProgressFraction(
            (simulationNowMs - simulationStartMs).coerceAtLeast(0L),
        )
    val state =
        MarshioGuidanceSimulation.stateAtProgress(
            path = session.marshioPath,
            totalDurationSeconds = session.durationSeconds,
            progressFraction = progress,
        )

    return GuidanceStateHolder(
        state = state,
        restartSimulation = {
            simulationStartMs = System.currentTimeMillis()
            simulationNowMs = simulationStartMs
        },
    )
}

@Composable
private fun rememberGpsGuidanceState(session: MarshioGuidanceSession): GuidanceStateHolder {
    val (locationStatus, gpsFix) = rememberMarshioGuidanceGpsFix(enabled = session.marshioPath.size >= 2)
    val state =
        gpsFix?.let { fix ->
            MarshioGuidanceSimulation.stateAtPosition(
                position = fix.position,
                path = session.marshioPath,
                totalDurationSeconds = session.durationSeconds,
                gpsBearingDegrees = fix.bearingDegrees,
            )
        }

    return GuidanceStateHolder(
        state = state,
        locationStatus = locationStatus,
    )
}

@Composable
private fun MarshioGuidanceStatusPanel(
    state: MarshioGuidanceSimulation.State?,
    session: MarshioGuidanceSession,
    locationStatus: MarshioGuidanceLocationStatus?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state?.statusLabel ?: locationStatusLabel(locationStatus),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${session.durationText} · ${session.distanceText}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (state != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Remaining: ${MarshioGuidanceFormatting.formatDistance(state.remainingDistanceMeters)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "ETA: ${MarshioGuidanceFormatting.formatDurationMinutes(state.remainingDurationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (state.offRoute) "Off-route: Yes" else "Off-route: No",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = locationStatusDetail(locationStatus),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun locationStatusLabel(status: MarshioGuidanceLocationStatus?): String =
    when (status) {
        MarshioGuidanceLocationStatus.REQUESTING_PERMISSION -> "Waiting for location permission"
        MarshioGuidanceLocationStatus.PERMISSION_DENIED -> "Location permission required"
        MarshioGuidanceLocationStatus.WAITING_FOR_FIX -> "Acquiring GPS fix…"
        MarshioGuidanceLocationStatus.TRACKING, null -> "Following MARSHIO route"
    }

private fun locationStatusDetail(status: MarshioGuidanceLocationStatus?): String =
    when (status) {
        MarshioGuidanceLocationStatus.PERMISSION_DENIED ->
            "Enable location to follow the MARSHIO route."
        MarshioGuidanceLocationStatus.WAITING_FOR_FIX ->
            "Waiting for GPS signal…"
        MarshioGuidanceLocationStatus.REQUESTING_PERMISSION ->
            "Allow location access to start guidance."
        MarshioGuidanceLocationStatus.TRACKING, null ->
            ""
    }

@Composable
private fun MarshioGuidanceUnavailableGeometryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Route geometry unavailable",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "MARSHIO cannot draw this route yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
