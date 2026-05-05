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
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Places.isInitialized() && BuildConfig.PLACES_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        }
        val placesClient =
            if (Places.isInitialized()) Places.createClient(this) else null
        setContent {
            ClearRoad2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClearRoadScreen(
                        modifier = Modifier.padding(innerPadding),
                        placesClient = placesClient,
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

private fun fetchLatLng(
    client: PlacesClient?,
    placeId: String?,
    onResult: (LatLng?) -> Unit,
) {
    if (placeId == null || client == null) {
        onResult(null)
        return
    }

    client.fetchPlace(
        FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.LOCATION),
        ).build(),
    )
        .addOnSuccessListener { response ->
            onResult(response.place.location)
        }
        .addOnFailureListener {
            onResult(null)
        }
}

@Composable
fun ClearRoadScreen(
    modifier: Modifier = Modifier,
    placesClient: PlacesClient? = null,
) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PreferenceMode.FASTEST) }
    var fromPredictions by remember {
        mutableStateOf<List<AutocompletePrediction>>(emptyList())
    }
    var toPredictions by remember {
        mutableStateOf<List<AutocompletePrediction>>(emptyList())
    }
    var selectedFromPlaceId by remember { mutableStateOf<String?>(null) }
    var selectedToPlaceId by remember { mutableStateOf<String?>(null) }
    var selectedFromLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }
    var selectedToLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }
    val isRouteReady =
        selectedFromLatLng != null && selectedToLatLng != null
    val decision = if (isRouteReady) {
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
        if (isRouteReady) {
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
        if (isRouteReady) {
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
            onValueChange = { newText ->
                selectedFromPlaceId = null
                selectedFromLatLng = null
                originText = newText
                if (newText.length < 2) {
                    fromPredictions = emptyList()
                } else if (placesClient == null) {
                    fromPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            fromPredictions = response.autocompletePredictions
                        }
                        .addOnFailureListener {
                            fromPredictions = emptyList()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("From") },
            placeholder = { Text("Enter starting point") },
            singleLine = true,
            maxLines = 1,
        )
        if (fromPredictions.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                fromPredictions.take(5).forEach { prediction ->
                    val label = prediction.getFullText(null).toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                originText = label
                                fromPredictions = emptyList()
                                selectedFromPlaceId = prediction.placeId
                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                    selectedFromLatLng = result
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = destinationText,
            onValueChange = { newText ->
                selectedToPlaceId = null
                selectedToLatLng = null
                destinationText = newText
                if (newText.length < 2) {
                    toPredictions = emptyList()
                } else if (placesClient == null) {
                    toPredictions = emptyList()
                } else {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newText)
                        .setCountries(listOf("AE"))
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            toPredictions = response.autocompletePredictions
                        }
                        .addOnFailureListener {
                            toPredictions = emptyList()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To") },
            placeholder = { Text("Enter destination") },
            singleLine = true,
            maxLines = 1,
        )
        if (toPredictions.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                toPredictions.take(5).forEach { prediction ->
                    val label = prediction.getFullText(null).toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                destinationText = label
                                toPredictions = emptyList()
                                selectedToPlaceId = prediction.placeId
                                fetchLatLng(placesClient, prediction.placeId) { result ->
                                    selectedToLatLng = result
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
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
