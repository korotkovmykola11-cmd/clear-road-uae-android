package com.clearroad.app.intelligence

import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import org.json.JSONObject

internal data class OsmOverpassElement(
    val osmId: Long,
    val type: String,
    val tags: Map<String, String>,
    val points: List<LatLng>,
)

internal object OsmOverpassParser {

    internal sealed interface ParseResult {
        data class Ok(val elements: List<OsmOverpassElement>) : ParseResult

        data class MalformedJson(val errorMessage: String) : ParseResult
    }

    fun parseResponse(json: String): ParseResult {
        return try {
            val elements = JSONObject(json).optJSONArray("elements") ?: return ParseResult.Ok(emptyList())
            ParseResult.Ok(
                buildList {
                    for (index in 0 until elements.length()) {
                        val obj = elements.optJSONObject(index) ?: continue
                        parseElement(obj)?.let(::add)
                    }
                },
            )
        } catch (exception: JSONException) {
            ParseResult.MalformedJson(
                "Malformed Overpass JSON: ${exception.message ?: "parse error"}",
            )
        }
    }

    internal fun parseElement(obj: JSONObject): OsmOverpassElement? {
        val type = obj.optString("type")
        if (type.isBlank()) return null
        val osmId = obj.optLong("id")
        val tagsObj = obj.optJSONObject("tags") ?: JSONObject()
        val tags = tagsObj.keys().asSequence().associateWith { key -> tagsObj.optString(key) }
        val points = parsePoints(obj, type)
        return OsmOverpassElement(
            osmId = osmId,
            type = type,
            tags = tags,
            points = points,
        )
    }

    private fun parsePoints(
        obj: JSONObject,
        type: String,
    ): List<LatLng> =
        when (type) {
            "node" -> {
                val lat = obj.optDoubleOrNull("lat") ?: return emptyList()
                val lon = obj.optDoubleOrNull("lon") ?: return emptyList()
                listOf(LatLng(lat, lon))
            }
            "way", "relation" -> {
                val geometry = obj.optJSONArray("geometry") ?: return emptyList()
                buildList {
                    for (index in 0 until geometry.length()) {
                        val point = geometry.optJSONObject(index) ?: continue
                        val lat = point.optDoubleOrNull("lat") ?: continue
                        val lon = point.optDoubleOrNull("lon") ?: continue
                        add(LatLng(lat, lon))
                    }
                }
            }
            else -> emptyList()
        }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key)) optDouble(key) else null
}

internal object OsmRouteFeatureExtractor {

    data class ExtractedFeatures(
        val trafficSignalsCount: Int,
        val roundaboutsCount: Int,
        val mainRoadRatio: Float?,
        val roadTypeBreakdown: RoadTypeBreakdown?,
        val trafficLightEvidence: TrafficLightEvidence,
        val roundaboutEvidence: RoundaboutEvidence,
        val mainRoadEvidence: MainRoadEvidence?,
    )

    fun extract(
        routePath: List<LatLng>,
        elements: List<OsmOverpassElement>,
        nearRouteMeters: Double = RoutePolylineGeometry.NEAR_ROUTE_METERS,
    ): ExtractedFeatures {
        val signalIds = mutableListOf<Long>()
        val signalPositions = mutableListOf<LatLng>()
        val roundaboutIds = mutableListOf<Long>()
        val roundaboutPositions = mutableListOf<LatLng>()
        var mainRoadMeters = 0.0
        var classifiedMeters = 0.0
        val breakdown = mutableMapOf<RoadTypeBucket, Double>()

        elements.forEach { element ->
            val representativePoint = element.points.firstOrNull() ?: return@forEach
            if (!RoutePolylineGeometry.isNearRoute(representativePoint, routePath, nearRouteMeters)) {
                return@forEach
            }

            val highway = element.tags["highway"]
            val junction = element.tags["junction"]

            if (element.type == "node" && highway == "traffic_signals") {
                signalIds += element.osmId
                signalPositions += representativePoint
            }
            if (element.type == "way" && junction == "roundabout") {
                roundaboutIds += element.osmId
                roundaboutPositions += representativePoint
            }
            if (element.type == "way" && !highway.isNullOrBlank() && element.points.size >= 2) {
                val length =
                    RoutePolylineGeometry.nearRouteLengthMeters(
                        segmentPoints = element.points,
                        path = routePath,
                        thresholdMeters = nearRouteMeters,
                    )
                if (length > 0.0) {
                    classifiedMeters += length
                    val bucket = roadTypeBucket(highway)
                    breakdown[bucket] = (breakdown[bucket] ?: 0.0) + length
                    if (RoutePolylineGeometry.isMainRoadHighway(highway)) {
                        mainRoadMeters += length
                    }
                }
            }
        }

        val mainRoadRatio =
            if (classifiedMeters > 0.0) {
                (mainRoadMeters / classifiedMeters).toFloat().coerceIn(0f, 1f)
            } else {
                null
            }

        val roadTypeBreakdown =
            if (classifiedMeters > 0.0) {
                breakdown.toRoadTypeBreakdown()
            } else {
                null
            }

        return ExtractedFeatures(
            trafficSignalsCount = signalIds.size,
            roundaboutsCount = roundaboutIds.size,
            mainRoadRatio = mainRoadRatio,
            roadTypeBreakdown = roadTypeBreakdown,
            trafficLightEvidence =
                TrafficLightEvidence(
                    count = signalIds.size,
                    osmNodeIds = signalIds,
                    nearRoutePositions = signalPositions,
                ),
            roundaboutEvidence =
                RoundaboutEvidence(
                    count = roundaboutIds.size,
                    osmWayIds = roundaboutIds,
                    nearRoutePositions = roundaboutPositions,
                ),
            mainRoadEvidence =
                mainRoadRatio?.let { ratio ->
                    MainRoadEvidence(
                        mainRoadRatio = ratio,
                        mainRoadMeters = mainRoadMeters,
                        classifiedMeters = classifiedMeters,
                        roadTypeBreakdown = roadTypeBreakdown,
                    )
                },
        )
    }

    internal enum class RoadTypeBucket {
        MOTORWAY,
        TRUNK,
        PRIMARY,
        SECONDARY,
        TERTIARY,
        RESIDENTIAL,
        SERVICE,
        UNCLASSIFIED,
        UNKNOWN,
    }

    internal fun roadTypeBucket(highway: String): RoadTypeBucket =
        when {
            highway == "motorway" || highway == "motorway_link" -> RoadTypeBucket.MOTORWAY
            highway == "trunk" || highway == "trunk_link" -> RoadTypeBucket.TRUNK
            highway == "primary" || highway == "primary_link" -> RoadTypeBucket.PRIMARY
            highway == "secondary" || highway == "secondary_link" -> RoadTypeBucket.SECONDARY
            highway == "tertiary" || highway == "tertiary_link" -> RoadTypeBucket.TERTIARY
            highway == "residential" -> RoadTypeBucket.RESIDENTIAL
            highway == "service" -> RoadTypeBucket.SERVICE
            highway == "unclassified" -> RoadTypeBucket.UNCLASSIFIED
            else -> RoadTypeBucket.UNKNOWN
        }

    internal fun Map<RoadTypeBucket, Double>.toRoadTypeBreakdown(): RoadTypeBreakdown =
        RoadTypeBreakdown(
            motorwayMeters = this[RoadTypeBucket.MOTORWAY] ?: 0.0,
            trunkMeters = this[RoadTypeBucket.TRUNK] ?: 0.0,
            primaryMeters = this[RoadTypeBucket.PRIMARY] ?: 0.0,
            secondaryMeters = this[RoadTypeBucket.SECONDARY] ?: 0.0,
            tertiaryMeters = this[RoadTypeBucket.TERTIARY] ?: 0.0,
            residentialMeters = this[RoadTypeBucket.RESIDENTIAL] ?: 0.0,
            serviceMeters = this[RoadTypeBucket.SERVICE] ?: 0.0,
            unclassifiedMeters = this[RoadTypeBucket.UNCLASSIFIED] ?: 0.0,
            unknownMeters = this[RoadTypeBucket.UNKNOWN] ?: 0.0,
        )
}
