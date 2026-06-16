package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import java.util.Locale

/**
 * Presentation-only tradeoff copy for non-equivalent trips (Phase 1).
 * Does not affect route selection, scoring, or winner index.
 */
internal object ComparativeEvidence {

    /** Align with Phase 1 FASTEST gate so Details appears when Home shows time evidence. */
    private const val DETAILS_MIN_TIME_DELTA_SECONDS =
        PresentationThresholds.COMPARATIVE_DETAILS_MIN_TIME_DELTA_SECONDS

    data class Result(
        val lines: List<String>,
    ) {
        val isVisible: Boolean get() = lines.isNotEmpty()
    }

    data class RejectedAlternativeLine(
        val label: String,
        val detail: String,
    ) {
        val formatted: String get() = "$label: $detail"
    }

    data class RejectedAlternativesResult(
        val lines: List<RejectedAlternativeLine>,
    ) {
        val isVisible: Boolean get() = lines.isNotEmpty()
        val formattedLines: List<String> get() = lines.map { it.formatted }
    }

    fun build(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        mode: PreferenceMode,
        isEquivalentTrip: Boolean,
    ): Result {
        if (isEquivalentTrip || routes.size < 2) {
            return Result(lines = emptyList())
        }

        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val selected = routes[recIdx]
        val fastestIndex = fastestRouteIndex(routes) ?: return Result(lines = emptyList())
        val fastest = routes[fastestIndex]

        val lines =
            when (mode) {
                PreferenceMode.FASTEST ->
                    fastestModeLines(selected, recIdx, routes)
                PreferenceMode.NO_TOLLS ->
                    saveAedModeLines(selected, fastest)
                PreferenceMode.CALM ->
                    smoothModeLines(selected, recIdx, fastest, fastestIndex, routes)
            }

        return Result(lines = lines.take(2))
    }

    /**
     * Details-only Phase 2: one line per rejected alternative vs the recommended route.
     * Hidden on equivalent trips. Does not affect selection or scoring.
     */
    fun buildRejectedAlternatives(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        isEquivalentTrip: Boolean,
    ): RejectedAlternativesResult {
        if (isEquivalentTrip || routes.size < 2) {
            return RejectedAlternativesResult(lines = emptyList())
        }

        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val selected = routes[recIdx]
        val selectedIdentity = identities.getOrNull(recIdx)

        val lines =
            routes.indices
                .filter { it != recIdx }
                .sortedBy { routes[it].durationSeconds }
                .mapNotNull { altIdx ->
                    val alternative = routes[altIdx]
                    val alternativeIdentity = identities.getOrNull(altIdx)
                    if (
                        !hasDetailsTradeoff(
                            selected = selected,
                            alternative = alternative,
                            selectedIdentity = selectedIdentity,
                            alternativeIdentity = alternativeIdentity,
                        )
                    ) {
                        return@mapNotNull null
                    }
                    val label = alternativeLabel(alternative, alternativeIdentity, altIdx)
                    val detail = rejectedAlternativeDetail(selected, alternative)
                    if (detail.isBlank()) return@mapNotNull null
                    RejectedAlternativeLine(label = label, detail = detail)
                }

        return RejectedAlternativesResult(lines = lines)
    }

    internal fun fastestRouteIndex(routes: List<RealRouteDebugData>): Int? {
        if (routes.isEmpty()) return null
        return routes.indices.minWith(
            compareBy<Int> { routes[it].durationSeconds }.thenBy { it },
        )
    }

    internal fun nextFastestAlternativeIndex(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): Int? {
        if (routes.size < 2) return null
        return routes.indices
            .filter { it != recommendedIndex }
            .minWithOrNull(
                compareBy<Int> { routes[it].durationSeconds }.thenBy { it },
            )
    }

    private fun fastestModeLines(
        selected: RealRouteDebugData,
        recommendedIndex: Int,
        routes: List<RealRouteDebugData>,
    ): List<String> {
        val nextIndex = nextFastestAlternativeIndex(routes, recommendedIndex) ?: return emptyList()
        val nextRoute = routes[nextIndex]
        val advantageSeconds =
            (nextRoute.durationSeconds - selected.durationSeconds).coerceAtLeast(0)
        if (advantageSeconds < PresentationThresholds.COMPARATIVE_FASTEST_MIN_ADVANTAGE_SECONDS) {
            return emptyList()
        }
        val minutes = minutesRounded(advantageSeconds)
        return listOf(
            "Compared to next option:",
            "$minutes min faster than next option",
        )
    }

    private fun saveAedModeLines(
        selected: RealRouteDebugData,
        fastest: RealRouteDebugData,
    ): List<String> {
        val timeDeltaSeconds = selected.durationSeconds - fastest.durationSeconds
        val salikDiffers = salikDiffers(selected, fastest)
        if (!salikDiffers && timeDeltaSeconds < PresentationThresholds.COMPARATIVE_SAVE_AED_MIN_TIME_DELTA_SECONDS) {
            return emptyList()
        }
        if (!salikDiffers) {
            return emptyList()
        }

        val detailLine =
            when {
                timeDeltaSeconds <= PresentationThresholds.COMPARATIVE_SAVE_AED_SAME_TIME_TOLERANCE_SECONDS ->
                    "Same time · lower Salik"
                timeDeltaSeconds > 0 ->
                    "+${minutesRounded(timeDeltaSeconds)} min · lower Salik"
                else ->
                    "${minutesRounded(-timeDeltaSeconds)} min faster · lower Salik"
            }

        return listOf(
            "Compared to fastest:",
            detailLine,
        )
    }

    private fun smoothModeLines(
        selected: RealRouteDebugData,
        recommendedIndex: Int,
        fastest: RealRouteDebugData,
        fastestIndex: Int,
        routes: List<RealRouteDebugData>,
    ): List<String> {
        if (recommendedIndex == fastestIndex) {
            return emptyList()
        }

        val selectedDelay = trafficDelaySeconds(selected)
        val fastestDelay = trafficDelaySeconds(fastest)
        val delayDelta = fastestDelay - selectedDelay
        val routeDiffers = recommendedIndex != fastestIndex

        if (!routeDiffers) {
            return emptyList()
        }
        if (delayDelta < PresentationThresholds.COMPARATIVE_SMOOTH_MEANINGFUL_DELAY_DELTA_SECONDS) {
            return emptyList()
        }

        val timeDeltaSeconds = selected.durationSeconds - fastest.durationSeconds
        val detailLine =
            when {
                timeDeltaSeconds > 0 ->
                    "+${minutesRounded(timeDeltaSeconds)} min · less affected by traffic slowdowns"
                timeDeltaSeconds < -PresentationThresholds.COMPARATIVE_SAVE_AED_SAME_TIME_TOLERANCE_SECONDS ->
                    "${minutesRounded(-timeDeltaSeconds)} min faster · less affected by traffic slowdowns"
                else ->
                    "Less affected by traffic slowdowns"
            }

        return listOf(
            "Compared to fastest:",
            detailLine,
        )
    }

    internal fun trafficDelaySeconds(route: RealRouteDebugData): Int =
        SmoothDriveScoring.trafficDelaySeconds(
            route.baseDurationSeconds.takeIf { it > 0 }
                ?: route.durationSeconds.takeIf { it > 0 }
                ?: 0,
            route.durationInTrafficSeconds?.takeIf { it > 0 }
                ?: route.durationSeconds.takeIf { it > 0 }
                ?: 0,
        )

    internal fun salikDiffers(
        left: RealRouteDebugData,
        right: RealRouteDebugData,
    ): Boolean = SalikFacts.salikDiffers(left, right)

    internal fun presentationTollCount(route: RealRouteDebugData): Int =
        SalikFacts.presentationTollCount(route)

    internal fun minutesRounded(seconds: Int): Int = (seconds.coerceAtLeast(0) + 59) / 60

    internal fun hasDetailsTradeoff(
        selected: RealRouteDebugData,
        alternative: RealRouteDebugData,
        selectedIdentity: RouteIdentity?,
        alternativeIdentity: RouteIdentity?,
    ): Boolean {
        val timeDeltaSeconds = alternative.durationSeconds - selected.durationSeconds
        if (kotlin.math.abs(timeDeltaSeconds) >= DETAILS_MIN_TIME_DELTA_SECONDS) {
            return true
        }
        if (salikDiffers(selected, alternative)) {
            return true
        }
        return routesVisiblyDiffer(
            selected = selected,
            alternative = alternative,
            selectedIdentity = selectedIdentity,
            alternativeIdentity = alternativeIdentity,
        )
    }

    internal fun routesVisiblyDiffer(
        selected: RealRouteDebugData,
        alternative: RealRouteDebugData,
        selectedIdentity: RouteIdentity?,
        alternativeIdentity: RouteIdentity?,
    ): Boolean {
        if (identitiesDiffer(selectedIdentity, alternativeIdentity)) {
            return true
        }
        if (identityDisplayDiffers(selectedIdentity, alternativeIdentity)) {
            return true
        }
        val selectedSummary = RouteIdentityPresentationPolicy.honestSummary(selected)
        val alternativeSummary = RouteIdentityPresentationPolicy.honestSummary(alternative)
        if (
            selectedSummary.isNotBlank() &&
            alternativeSummary.isNotBlank() &&
            !selectedSummary.equals(alternativeSummary, ignoreCase = true)
        ) {
            return true
        }
        if (kotlin.math.abs(alternative.distanceMeters - selected.distanceMeters) >=
            PresentationThresholds.ROUTES_VISIBLY_DIFFER_DISTANCE_METERS
        ) {
            return true
        }
        return false
    }

    internal fun identityDisplayDiffers(
        left: RouteIdentity?,
        right: RouteIdentity?,
    ): Boolean {
        if (left == null || right == null) return false
        if (left.fullName != right.fullName) return true
        return left.disambiguator != right.disambiguator
    }

    internal fun alternativeLabelForAudit(
        alternative: RealRouteDebugData,
        identity: RouteIdentity?,
        routeIndex: Int,
    ): String = alternativeLabel(alternative, identity, routeIndex)

    internal fun identitiesDiffer(
        left: RouteIdentity?,
        right: RouteIdentity?,
    ): Boolean {
        if (left == null || right == null) return false
        if (left.stableKey != right.stableKey) return true
        return left.primaryName != right.primaryName
    }

    private fun alternativeLabel(
        alternative: RealRouteDebugData,
        identity: RouteIdentity?,
        routeIndex: Int,
    ): String {
        identity?.fullName?.takeIf { it.isNotBlank() }?.let { return it }
        identity?.primaryName?.takeIf { it.isNotBlank() }?.let { return it }
        RouteIdentityPresentationPolicy.honestSummary(alternative)
            .takeIf { it.isNotBlank() }
            ?.let { return it }
        return "Route ${routeIndex + 1}"
    }

    private fun rejectedAlternativeDetail(
        selected: RealRouteDebugData,
        alternative: RealRouteDebugData,
    ): String {
        val parts = mutableListOf<String>()
        val timeDeltaSeconds = alternative.durationSeconds - selected.durationSeconds
        when {
            timeDeltaSeconds >= DETAILS_MIN_TIME_DELTA_SECONDS ->
                parts.add("+${minutesRounded(timeDeltaSeconds)} min")
            timeDeltaSeconds <= -DETAILS_MIN_TIME_DELTA_SECONDS ->
                parts.add("${minutesRounded(-timeDeltaSeconds)} min faster")
        }

        salikComparisonPhrase(selected, alternative)?.let(parts::add)

        if (parts.isEmpty()) {
            return "different route"
        }
        return parts.joinToString(separator = " · ")
    }

    private fun salikComparisonPhrase(
        selected: RealRouteDebugData,
        alternative: RealRouteDebugData,
    ): String? {
        val selectedAed = selected.tollAED
        val alternativeAed = alternative.tollAED
        if (alternativeAed > selectedAed) {
            val delta = alternativeAed - selectedAed
            if (delta > 0) return "+$delta AED Salik"
        }
        if (alternativeAed < selectedAed) {
            val saved = selectedAed - alternativeAed
            if (saved > 0) return "saves $saved AED Salik"
        }

        val selectedCount = presentationTollCount(selected)
        val alternativeCount = presentationTollCount(alternative)
        return when {
            alternativeCount > selectedCount -> "more Salik"
            alternativeCount < selectedCount -> "saves Salik"
            else -> null
        }
    }
}
