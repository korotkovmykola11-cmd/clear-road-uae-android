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
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = destinationText,
            onValueChange = { destinationText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To") },
            placeholder = { Text("Enter destination") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            PreferenceMode.entries.forEachIndexed { index, mode ->
                if (index > 0) {
                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val selected = mode == selectedMode
                Text(
                    text = preferenceModeLabel(mode),
                    modifier = Modifier.clickable { selectedMode = mode },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choice",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (decision != null) {
                "${decision.choice} from $originText to $destinationText"
            } else {
                "Enter a route"
            },
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Why",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = decision?.why
                ?: "Add starting point and destination to get a recommendation.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tip",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = decision?.tip ?: "Start with a common UAE route.",
            style = MaterialTheme.typography.bodyLarge
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
