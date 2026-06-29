package com.clearroad.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmOverpassParserTest {

    @Test
    fun parseElements_readsTrafficSignalsAndRoundabouts() {
        val json =
            """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 101,
                  "lat": 25.01,
                  "lon": 55.01,
                  "tags": { "highway": "traffic_signals" }
                },
                {
                  "type": "way",
                  "id": 202,
                  "tags": { "junction": "roundabout", "highway": "primary" },
                  "geometry": [
                    { "lat": 25.02, "lon": 55.02 },
                    { "lat": 25.021, "lon": 55.021 }
                  ]
                }
              ]
            }
            """.trimIndent()

        val result = OsmOverpassParser.parseResponse(json)

        assertTrue(result is OsmOverpassParser.ParseResult.Ok)
        assertEquals(2, (result as OsmOverpassParser.ParseResult.Ok).elements.size)
        assertEquals("traffic_signals", result.elements[0].tags["highway"])
        assertEquals("roundabout", result.elements[1].tags["junction"])
        assertEquals(2, result.elements[1].points.size)
    }

    @Test
    fun buildOverpassQuery_includesSignalsRoundaboutsAndHighways() {
        val query =
            OsmOverpassQueryBuilder.build(
                IntelligenceBoundingBox(
                    south = 25.0,
                    west = 55.0,
                    north = 25.1,
                    east = 55.1,
                ),
            )

        assertTrue(query.contains("traffic_signals"))
        assertTrue(query.contains("roundabout"))
        assertTrue(query.contains("way[\"highway\"]"))
    }
}

class OsmRouteFeatureExtractorTest {

    private val routePath =
        listOf(
            com.google.android.gms.maps.model.LatLng(25.0, 55.0),
            com.google.android.gms.maps.model.LatLng(25.01, 55.0),
        )

    @Test
    fun extract_countsNearRouteSignalsAndIgnoresParallelRoad() {
        val elements =
            listOf(
                OsmOverpassElement(
                    osmId = 1,
                    type = "node",
                    tags = mapOf("highway" to "traffic_signals"),
                    points = listOf(com.google.android.gms.maps.model.LatLng(25.005, 55.0)),
                ),
                OsmOverpassElement(
                    osmId = 2,
                    type = "node",
                    tags = mapOf("highway" to "traffic_signals"),
                    points = listOf(com.google.android.gms.maps.model.LatLng(25.005, 55.01)),
                ),
            )

        val features = OsmRouteFeatureExtractor.extract(routePath, elements)

        assertEquals(1, features.trafficSignalsCount)
        assertEquals(listOf(1L), features.trafficLightEvidence.osmNodeIds)
    }

    @Test
    fun extract_countsRoundaboutNearRoute() {
        val elements =
            listOf(
                OsmOverpassElement(
                    osmId = 99,
                    type = "way",
                    tags = mapOf("junction" to "roundabout", "highway" to "primary"),
                    points =
                        listOf(
                            com.google.android.gms.maps.model.LatLng(25.004, 55.0),
                            com.google.android.gms.maps.model.LatLng(25.006, 55.0),
                        ),
                ),
            )

        val features = OsmRouteFeatureExtractor.extract(routePath, elements)

        assertEquals(1, features.roundaboutsCount)
        assertEquals(listOf(99L), features.roundaboutEvidence.osmWayIds)
    }
}
