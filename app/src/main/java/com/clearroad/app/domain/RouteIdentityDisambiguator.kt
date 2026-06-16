package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import java.util.Locale

/**
 * Assigns disambiguators when multiple alternatives share the same primary stable key.
 */
internal object RouteIdentityDisambiguator {

    fun assignDisambiguators(
        spines: MutableList<RouteIdentityExtractor.ExtractedSpine>,
        routes: List<RealRouteDebugData>,
        indices: List<Int>,
    ) {
        if (indices.size <= 1) {
            indices.forEach { spines[it].disambiguator = null }
            return
        }

        val summaries =
            indices.associateWith { index ->
                RouteIdentityExtractor.fullSummaryText(routes[index]).lowercase(Locale.US)
            }
        val scans =
            indices.associateWith { index ->
                routes[index].corridorScanText.lowercase(Locale.US)
            }

        for (index in indices) {
            spines[index].disambiguator =
                pickDisambiguator(
                    index = index,
                    siblingIndices = indices,
                    spine = spines[index],
                    summaries = summaries,
                    scans = scans,
                    routes = routes,
                )
        }
    }

    private fun pickDisambiguator(
        index: Int,
        siblingIndices: List<Int>,
        spine: RouteIdentityExtractor.ExtractedSpine,
        summaries: Map<Int, String>,
        scans: Map<Int, String>,
        routes: List<RealRouteDebugData>,
    ): String? {
        val mySummary = summaries[index].orEmpty()
        val myScan = scans[index].orEmpty()

        for (code in UaeRoadCanon.disambiguatorCodeOrder) {
            if (code == spine.stableKey) continue
            if (!summaryContainsCode(mySummary, code)) continue
            val presentInAllSiblings =
                siblingIndices.all { sibling ->
                    sibling == index || summaryContainsCode(summaries[sibling].orEmpty(), code)
                }
            if (!presentInAllSiblings) {
                return UaeRoadCanon.disambiguatorLabelForKey(code)
            }
        }

        for (code in spine.secondaryCodes) {
            if (code == spine.stableKey) continue
            if (!summaryContainsCode(mySummary, code)) continue
            val presentInAllSiblings =
                siblingIndices.all { sibling ->
                    sibling == index || summaryContainsCode(summaries[sibling].orEmpty(), code)
                }
            if (!presentInAllSiblings) {
                return UaeRoadCanon.disambiguatorLabelForKey(code)
            }
        }

        parseExitDisambiguator(myScan)?.let { return it }

        parseDistrictDisambiguator(myScan, siblingIndices, scans, index, routes)?.let { return it }

        parseEmirateDisambiguator(myScan, siblingIndices, scans, index)?.let { return it }

        parseDistanceDisambiguator(index, siblingIndices, routes)?.let { return it }

        return RouteIdentityFormatter.DIRECT_DISAMBIGUATOR
    }

    private fun summaryContainsCode(
        summary: String,
        code: String,
    ): Boolean {
        val key = code.lowercase(Locale.US)
        val entry = UaeRoadCanon.entryForKey(code)
        val patternHit = entry.patterns.any { pattern -> summary.contains(pattern) }
        return patternHit || summary.contains(key)
    }

    private fun summaryOrScanContainsCode(
        summary: String,
        scan: String,
        code: String,
    ): Boolean {
        val key = code.lowercase(Locale.US)
        val entry = UaeRoadCanon.entryForKey(code)
        val patternHit =
            entry.patterns.any { pattern ->
                summary.contains(pattern) || scan.contains(pattern)
            }
        return patternHit || summary.contains(key) || scan.contains(key)
    }

    private fun parseExitDisambiguator(scan: String): String? {
        val exitPattern =
            Regex("""exit\s+\d+[a-z]?\s+for\s+([^(\n]+?)(?:\s*/\s*([ed]\d+))?""", RegexOption.IGNORE_CASE)
        val match = exitPattern.find(scan) ?: return null
        val roadCode = match.groupValues.getOrNull(2)?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }
        if (roadCode != null) {
            return UaeRoadCanon.disambiguatorLabelForKey(roadCode)
        }
        val name = match.groupValues[1].trim()
        if (name.isNotBlank()) {
            return "via ${name.take(24).trim()}"
        }
        return null
    }

    private fun parseDistrictDisambiguator(
        scan: String,
        siblingIndices: List<Int>,
        scans: Map<Int, String>,
        index: Int,
        routes: List<RealRouteDebugData>,
    ): String? {
        if (routes[index].distanceMeters in 1 until PresentationThresholds.SHORT_LOCAL_DISTANCE_METERS) {
            return null
        }
        val districts =
            listOf(
                "marina" to "Marina approach",
                "difc" to "DIFC approach",
                "jlt" to "JLT approach",
                "downtown" to "Downtown approach",
                "business bay" to "Business Bay approach",
                "airport" to "Airport approach",
            )
        for ((keyword, label) in districts) {
            if (!scan.contains(keyword)) continue
            if (keyword == "airport" && !airportDisambiguatorAllowed(routes[index], scan)) {
                continue
            }
            val unique =
                siblingIndices.none { sibling ->
                    sibling != index && scans[sibling].orEmpty().contains(keyword)
                }
            if (unique) return label
        }
        return null
    }

    private fun airportDisambiguatorAllowed(
        route: RealRouteDebugData,
        scan: String,
    ): Boolean {
        if (route.routeSummary.lowercase(Locale.US).contains("airport")) return true
        val lc = scan.lowercase(Locale.US)
        return lc.contains("international airport") ||
            lc.contains("airport rd") ||
            lc.contains("airport road") ||
            lc.contains("airport terminal")
    }

    private fun parseEmirateDisambiguator(
        scan: String,
        siblingIndices: List<Int>,
        scans: Map<Int, String>,
        index: Int,
    ): String? {
        val emirates =
            listOf(
                "ajman" to "from Ajman",
                "sharjah" to "from Sharjah",
                "abu dhabi" to "from Abu Dhabi",
            )
        for ((keyword, label) in emirates) {
            if (!scan.contains(keyword)) continue
            val unique =
                siblingIndices.none { sibling ->
                    sibling != index && scans[sibling].orEmpty().contains(keyword)
                }
            if (unique) return label
        }
        return null
    }

    private fun parseDistanceDisambiguator(
        index: Int,
        siblingIndices: List<Int>,
        routes: List<RealRouteDebugData>,
    ): String? {
        val distances = siblingIndices.map { routes[it].distanceMeters }
        val minDistance = distances.minOrNull() ?: return null
        val maxDistance = distances.maxOrNull() ?: return null
        if (minDistance == maxDistance) return null
        val myDistance = routes[index].distanceMeters
        return when (myDistance) {
            minDistance -> "shorter distance"
            maxDistance -> "slightly longer route"
            else -> null
        }
    }
}
