package com.clearroad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteDecisionEngine
import com.clearroad.app.ui.theme.ClearRoad2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClearRoad2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClearRoadScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private fun preferenceModeLabel(mode: PreferenceMode): String =
    when (mode) {
        PreferenceMode.FASTEST -> "Fastest"
        PreferenceMode.NO_TOLLS -> "No tolls"
        PreferenceMode.CALM -> "Calm"
    }

@Composable
fun ClearRoadScreen(modifier: Modifier = Modifier) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PreferenceMode.FASTEST) }
    val hasRouteInput =
        originText.isNotBlank() && destinationText.isNotBlank()
    val decision = if (hasRouteInput) {
        RouteDecisionEngine.choose(
            RouteDecisionEngine.sampleRoutes,
            selectedMode,
        )
    } else {
        null
    }
    val choiceText = when (selectedMode) {
        PreferenceMode.FASTEST -> "Best route via Sheikh Zayed Road"
        PreferenceMode.NO_TOLLS -> "Easiest on tolls via Emirates Road"
        PreferenceMode.CALM -> "Calmer drive via Emirates Road"
    }
    val fullChoiceText =
        if (originText.isNotBlank() && destinationText.isNotBlank()) {
            "$choiceText from $originText to $destinationText"
        } else {
            "Enter a route"
        }
    val whyText = when (selectedMode) {
        PreferenceMode.FASTEST ->
            "Fastest option for this route, but expect toll roads."
        PreferenceMode.NO_TOLLS ->
            "A little longer, but avoids tolls and keeps cost lower."
        PreferenceMode.CALM ->
            "Usually steadier and less stressful, but not the fastest."
    }
    val fullWhyText =
        if (originText.isNotBlank() && destinationText.isNotBlank()) {
            whyText
        } else {
            "Add starting point and destination to get a recommendation."
        }
    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "Clear Road",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Route decision assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = originText,
            onValueChange = { originText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("From") },
            placeholder = { Text("Enter starting point") },
            singleLine = true,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = destinationText,
            onValueChange = { destinationText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To") },
            placeholder = { Text("Enter destination") },
            singleLine = true,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PreferenceMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Text(
                    text = preferenceModeLabel(mode),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = mode }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choice",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = fullChoiceText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Why",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = fullWhyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tip",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = decision?.tip ?: "Start with a common UAE route.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ClearRoadPreview() {
    ClearRoad2Theme {
        ClearRoadScreen()
    }
}
