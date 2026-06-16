package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.domain.SalikDetection.countOccurrences
import java.util.Locale

/**
 * Extracts route spine identity from Directions-derived route data (presentation only).
 */
internal object RouteIdentityExtractor {

    /** Short/local trips: spine from Google summary only — not full turn-by-turn scan. */
    private const val SHORT_LOCAL_DISTANCE_METERS = PresentationThresholds.SHORT_LOCAL_DISTANCE_METERS

    private val shortTripMajorHighwayKeys = setOf("E11", "E311", "E611", "E66", "E44")

    internal data class ExtractedSpine(
        val stableKey: String,
        val primaryName: String,
        val summary: String,
        val secondaryCodes: List<String>,
        val scan: String,
        var disambiguator: String? = null,
    )

    fun extractSummary(route: RealRouteDebugData): String {
        if (route.routeSummary.isNotBlank()) {
            return route.routeSummary.trim()
        }
        return extractSummaryFromScan(route.corridorScanText)
    }

    fun extractSummaryFromScan(scan: String): String {
        val trimmed = scan.trim()
        if (trimmed.isEmpty()) return ""

        val andIdx = trimmed.indexOf(" and ")
        if (andIdx > 0) {
            return trimmed.substring(0, andIdx).trim()
        }

        val headIdx = trimmed.indexOf(" Head ")
        if (headIdx > 0) {
            return trimmed.substring(0, headIdx).trim()
        }

        return trimmed.take(120).trim()
    }

    fun extractSpine(route: RealRouteDebugData): ExtractedSpine {
        val summary = extractSummary(route)
        val scan = route.corridorScanText
        val corridorClass = SmoothDriveScoring.classifyCorridor(scan)
        val shortLocalTrip =
            route.distanceMeters in 1 until SHORT_LOCAL_DISTANCE_METERS ||
                corridorClass == SmoothDriveScoring.CorridorClass.URBAN_WEAVE

        val entry = resolveCanonEntry(summary, scan, corridorClass, shortLocalTrip)
        val primaryName = displayPrimaryName(entry, route)

        val primaryKey = entry.stableKey
        val combined = "$summary $scan"
        val secondaryCodes =
            UaeRoadCanon.findRoadCodes(combined)
                .filter { it != primaryKey }
                .distinct()

        return ExtractedSpine(
            stableKey = primaryKey,
            primaryName = primaryName,
            summary = summary,
            secondaryCodes = secondaryCodes,
            scan = scan,
        )
    }

    /**
     * When canon resolves to UNKNOWN, prefer Google's route summary over generic "UAE Route".
     */
    internal fun displayPrimaryName(
        entry: UaeRoadCanon.CanonEntry,
        route: RealRouteDebugData,
    ): String {
        if (entry.stableKey == UaeRoadCanon.UNKNOWN_STABLE_KEY && route.routeSummary.isNotBlank()) {
            return RouteIdentityFormatter.formatPrimary(route.routeSummary.trim())
        }
        return entry.primaryName
    }

    private fun resolveCanonEntry(
        summary: String,
        scan: String,
        corridorClass: SmoothDriveScoring.CorridorClass,
        shortLocalTrip: Boolean,
    ): UaeRoadCanon.CanonEntry =
        if (shortLocalTrip) {
            val fromSummary = UaeRoadCanon.matchHighestPriorityCanon(summary)
            if (
                fromSummary != null &&
                fromSummary.stableKey in shortTripMajorHighwayKeys
            ) {
                fromSummary
            } else {
                UaeRoadCanon.urbanFallback(corridorClass, scan)
            }
        } else {
            val combined = "$summary $scan"
            UaeRoadCanon.matchHighestPriorityCanon(summary)
                ?: UaeRoadCanon.matchHighestPriorityCanon(combined)
                ?: UaeRoadCanon.urbanFallback(corridorClass, scan)
        }

    fun tollExposure(route: RealRouteDebugData): TollExposure {
        if (route.tollAED > 0) {
            return TollExposure.SALIK_AED
        }
        val tollRoadCount =
            countOccurrences(route.corridorScanText.lowercase(Locale.US), "toll road")
        if (tollRoadCount > 0) {
            return TollExposure.SALIK_ESTIMATED
        }
        return TollExposure.NO_SALIK
    }

    fun fullSummaryText(route: RealRouteDebugData): String {
        if (route.routeSummary.isNotBlank()) {
            return route.routeSummary.trim()
        }
        val scan = route.corridorScanText
        val headIdx = scan.indexOf(" Head ")
        return if (headIdx > 0) {
            scan.substring(0, headIdx).trim()
        } else {
            extractSummaryFromScan(scan)
        }
    }
}
