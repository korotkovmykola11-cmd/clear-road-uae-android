package com.clearroad.app.intelligence

internal object OsmOverpassQueryBuilder {
    fun build(bbox: IntelligenceBoundingBox): String {
        val box = bbox.overpassSelector()
        return """
            [out:json][timeout:25];
            (
              node["highway"="traffic_signals"]($box);
              way["junction"="roundabout"]($box);
              way["highway"]($box);
            );
            out body geom;
        """.trimIndent()
    }
}
