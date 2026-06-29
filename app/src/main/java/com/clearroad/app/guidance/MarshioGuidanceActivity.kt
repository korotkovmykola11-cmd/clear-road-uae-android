package com.clearroad.app.guidance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.theme.ClearRoad2Theme

/** In-app MARSHIO Guidance — follows the selected MARSHIO polyline with live GPS. */
class MarshioGuidanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = MarshioGuidanceLaunch.readArgs(intent)
        setContent {
            ClearRoad2Theme {
                if (args == null) {
                    MarshioGuidanceMissingArgsScreen()
                } else {
                    MarshioGuidanceRouteScreen(
                        session = args.toSession(),
                        useSimulation = false,
                        showRestartSimulation = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarshioGuidanceMissingArgsScreen() {
    Text(
        text = "MARSHIO Guidance could not load route data.",
        modifier = Modifier.padding(16.dp),
    )
}
