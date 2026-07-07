package com.clearroad.app.guidance

import android.content.Context
import android.content.Intent
import android.util.Log
import com.clearroad.app.DirectionsStepRecord
import com.clearroad.app.JunctionAnnotationClassifier
import com.clearroad.app.ui.model.RouteDetailsUiModel
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

data class GuidanceStepUi(
    val instruction: String,
    val distanceMeters: Int,
    val startLatLng: Pair<Double, Double>?,
)

/** Presentation-only launch wiring for in-app MARSHIO Guidance. */
internal object MarshioGuidanceLaunch {

    private const val TAG = "MarshioGuidanceLaunch"
    private const val MAX_GUIDANCE_STEPS = 50

    data class Args(
        val fromLatLng: LatLng,
        val toLatLng: LatLng,
        val routeName: String,
        val durationText: String,
        val distanceText: String,
        val durationSeconds: Int,
        val selectedRouteIndex: Int,
        val marshioPath: List<LatLng>,
        val googlePath: List<LatLng>,
        val steps: List<GuidanceStepUi> = emptyList(),
    ) {
        fun toSession(): MarshioGuidanceSession =
            MarshioGuidanceSession(
                routeName = routeName,
                fromLatLng = fromLatLng,
                toLatLng = toLatLng,
                marshioPath = marshioPath,
                googlePath = googlePath,
                durationText = durationText,
                distanceText = distanceText,
                durationSeconds = durationSeconds,
                selectedRouteIndex = selectedRouteIndex,
                steps = steps,
            )
    }

    fun showEntryInRouteDetails(
        model: RouteDetailsUiModel,
    ): Boolean =
        model.showMarshioGuidanceEntry &&
            model.fromLatLng != null &&
            model.toLatLng != null &&
            model.handoffRoutePathPoints.size >= 2

    fun argsFromRouteDetails(
        model: RouteDetailsUiModel,
        googleSteps: List<DirectionsStepRecord> = emptyList(),
    ): Args? {
        val from = model.fromLatLng ?: return null
        val to = model.toLatLng ?: return null
        if (model.handoffRoutePathPoints.size < 2) return null
        return Args(
            fromLatLng = from,
            toLatLng = to,
            routeName = model.routeIdentityTitle.ifBlank { "Route ${model.routeNumber}" },
            durationText = model.durationText,
            distanceText = model.distanceText,
            durationSeconds = model.durationSeconds,
            selectedRouteIndex = model.selectedRouteIndex,
            marshioPath = model.handoffRoutePathPoints,
            googlePath = model.googleDefaultRoutePathPoints,
            steps = mapGuidanceSteps(googleSteps),
        )
    }

    internal fun mapGuidanceSteps(
        googleSteps: List<DirectionsStepRecord>,
    ): List<GuidanceStepUi> {
        if (googleSteps.isEmpty()) return emptyList()
        return googleSteps
            .take(MAX_GUIDANCE_STEPS)
            .map { step ->
                val instruction =
                    JunctionAnnotationClassifier.stripHtml(step.htmlInstructions)
                        .ifBlank { step.maneuver.orEmpty() }
                GuidanceStepUi(
                    instruction = instruction,
                    distanceMeters = step.distanceMeters,
                    startLatLng = step.startLocation?.let { it.latitude to it.longitude },
                )
            }
    }

    internal fun encodeStepsJson(steps: List<GuidanceStepUi>): String {
        val array = JSONArray()
        steps.forEach { step ->
            array.put(
                JSONObject().apply {
                    put("instruction", step.instruction)
                    put("distanceMeters", step.distanceMeters)
                    step.startLatLng?.let { (lat, lng) ->
                        put("startLat", lat)
                        put("startLng", lng)
                    }
                },
            )
        }
        return array.toString()
    }

    internal fun decodeStepsJson(json: String?): List<GuidanceStepUi> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        val steps = mutableListOf<GuidanceStepUi>()
        for (index in 0 until array.length()) {
            val obj = array.getJSONObject(index)
            val startLat = obj.optDouble("startLat", Double.NaN)
            val startLng = obj.optDouble("startLng", Double.NaN)
            val startLatLng =
                if (startLat.isNaN() || startLng.isNaN()) {
                    null
                } else {
                    startLat to startLng
                }
            steps.add(
                GuidanceStepUi(
                    instruction = obj.optString("instruction", ""),
                    distanceMeters = obj.optInt("distanceMeters", 0),
                    startLatLng = startLatLng,
                ),
            )
        }
        return steps
    }

    fun createIntent(
        context: Context,
        args: Args,
    ): Intent {
        Log.d(TAG, "guidance intent path size=${args.marshioPath.size} steps=${args.steps.size}")
        return Intent(context, MarshioGuidanceActivity::class.java).apply {
            putExtra(EXTRA_FROM_LAT, args.fromLatLng.latitude)
            putExtra(EXTRA_FROM_LNG, args.fromLatLng.longitude)
            putExtra(EXTRA_TO_LAT, args.toLatLng.latitude)
            putExtra(EXTRA_TO_LNG, args.toLatLng.longitude)
            putExtra(EXTRA_ROUTE_NAME, args.routeName)
            putExtra(EXTRA_DURATION_TEXT, args.durationText)
            putExtra(EXTRA_DISTANCE_TEXT, args.distanceText)
            putExtra(EXTRA_DURATION_SECONDS, args.durationSeconds)
            putExtra(EXTRA_SELECTED_ROUTE_INDEX, args.selectedRouteIndex)
            putExtra(EXTRA_MARSHIO_LATITUDES, args.marshioPath.map { it.latitude }.toDoubleArray())
            putExtra(EXTRA_MARSHIO_LONGITUDES, args.marshioPath.map { it.longitude }.toDoubleArray())
            if (args.googlePath.size >= 2) {
                putExtra(EXTRA_GOOGLE_LATITUDES, args.googlePath.map { it.latitude }.toDoubleArray())
                putExtra(EXTRA_GOOGLE_LONGITUDES, args.googlePath.map { it.longitude }.toDoubleArray())
            }
            if (args.steps.isNotEmpty()) {
                putExtra(EXTRA_STEPS_JSON, encodeStepsJson(args.steps))
            }
        }
    }

    fun readArgs(intent: Intent): Args? {
        if (!intent.hasExtra(EXTRA_MARSHIO_LATITUDES)) return null
        val marshioLatitudes = intent.getDoubleArrayExtra(EXTRA_MARSHIO_LATITUDES) ?: return null
        val marshioLongitudes = intent.getDoubleArrayExtra(EXTRA_MARSHIO_LONGITUDES) ?: return null
        if (marshioLatitudes.size < 2 || marshioLatitudes.size != marshioLongitudes.size) return null

        val googleLatitudes = intent.getDoubleArrayExtra(EXTRA_GOOGLE_LATITUDES)
        val googleLongitudes = intent.getDoubleArrayExtra(EXTRA_GOOGLE_LONGITUDES)
        val googlePath =
            if (
                googleLatitudes != null &&
                    googleLongitudes != null &&
                    googleLatitudes.size >= 2 &&
                    googleLatitudes.size == googleLongitudes.size
            ) {
                googleLatitudes.indices.map { index ->
                    LatLng(googleLatitudes[index], googleLongitudes[index])
                }
            } else {
                emptyList()
            }

        return Args(
            fromLatLng = LatLng(
                intent.getDoubleExtra(EXTRA_FROM_LAT, 0.0),
                intent.getDoubleExtra(EXTRA_FROM_LNG, 0.0),
            ),
            toLatLng = LatLng(
                intent.getDoubleExtra(EXTRA_TO_LAT, 0.0),
                intent.getDoubleExtra(EXTRA_TO_LNG, 0.0),
            ),
            routeName = intent.getStringExtra(EXTRA_ROUTE_NAME).orEmpty(),
            durationText = intent.getStringExtra(EXTRA_DURATION_TEXT).orEmpty(),
            distanceText = intent.getStringExtra(EXTRA_DISTANCE_TEXT).orEmpty(),
            durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 0),
            selectedRouteIndex = intent.getIntExtra(EXTRA_SELECTED_ROUTE_INDEX, 0),
            marshioPath =
                marshioLatitudes.indices.map { index ->
                    LatLng(marshioLatitudes[index], marshioLongitudes[index])
                },
            googlePath = googlePath,
            steps = decodeStepsJson(intent.getStringExtra(EXTRA_STEPS_JSON)),
        )
    }

    private const val EXTRA_FROM_LAT = "marshio_guidance.from_lat"
    private const val EXTRA_FROM_LNG = "marshio_guidance.from_lng"
    private const val EXTRA_TO_LAT = "marshio_guidance.to_lat"
    private const val EXTRA_TO_LNG = "marshio_guidance.to_lng"
    private const val EXTRA_ROUTE_NAME = "marshio_guidance.route_name"
    private const val EXTRA_DURATION_TEXT = "marshio_guidance.duration_text"
    private const val EXTRA_DISTANCE_TEXT = "marshio_guidance.distance_text"
    private const val EXTRA_DURATION_SECONDS = "marshio_guidance.duration_seconds"
    private const val EXTRA_SELECTED_ROUTE_INDEX = "marshio_guidance.selected_route_index"
    private const val EXTRA_MARSHIO_LATITUDES = "marshio_guidance.marshio_latitudes"
    private const val EXTRA_MARSHIO_LONGITUDES = "marshio_guidance.marshio_longitudes"
    private const val EXTRA_GOOGLE_LATITUDES = "marshio_guidance.google_latitudes"
    private const val EXTRA_GOOGLE_LONGITUDES = "marshio_guidance.google_longitudes"
    private const val EXTRA_STEPS_JSON = "marshio_guidance.steps_json"
}

data class MarshioGuidanceSession(
    val routeName: String,
    val fromLatLng: LatLng,
    val toLatLng: LatLng,
    val marshioPath: List<LatLng>,
    val googlePath: List<LatLng>,
    val durationText: String,
    val distanceText: String,
    val durationSeconds: Int,
    val selectedRouteIndex: Int,
    val steps: List<GuidanceStepUi> = emptyList(),
)
