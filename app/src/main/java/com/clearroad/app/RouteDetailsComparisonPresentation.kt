package com.clearroad.app

import com.clearroad.app.domain.ComparativeEvidence
import com.clearroad.app.domain.EquivalentTripHonesty
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.PresentationThresholds
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RejectedAlternativeUiModel
import com.clearroad.app.ui.model.RouteDecisionRouteCardUiModel
import com.clearroad.app.ui.model.RouteGoogleMarshioDecisionUiModel
import java.util.Locale
import kotlin.math.abs

/**
 * Route Details — Google vs MARSHIO decision content (presentation only).
 */
internal object RouteDetailsComparisonPresentation {

    private const val GOOGLE_DEFAULT_INDEX = 0
    private const val DISTANCE_DELTA_THRESHOLD_METERS = 100
    private const val TIME_DELTA_THRESHOLD_SECONDS = 60
    /** Up to 2 min ETA gap treated as a tradeoff, not a clear Google win. */
    private const val TIME_MEANINGFUL_DIFFERENCE_SECONDS = 120

    data class Result(
        val googleMarshioDecision: RouteGoogleMarshioDecisionUiModel?,
        val rejectedAlternativesIntro: String = "",
        val rejectedAlternatives: List<RejectedAlternativeUiModel>,
    )

    internal data class AlternativeComparisonFacts(
        val timeDeltaSeconds: Int,
        val distanceDeltaMeters: Int,
        val salikDeltaAed: Int,
        val trafficDelayDeltaSeconds: Int?,
    )

    internal data class DisagreementFacts(
        val marshioFasterSeconds: Int,
        val marshioSlowerSeconds: Int,
        val distanceSavedMeters: Int,
        val distanceLongerMeters: Int,
        val trafficDelaySavedSeconds: Int?,
        val salikSavedAed: Int,
    )

    fun build(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        detailRouteIndex: Int,
        recommendedIndex: Int,
        mode: PreferenceMode,
        directionsStatus: String?,
    ): Result {
        if (directionsStatus != "OK" || routes.isEmpty()) {
            return Result(
                googleMarshioDecision = null,
                rejectedAlternativesIntro = "",
                rejectedAlternatives = emptyList(),
            )
        }

        val detailIdx = detailRouteIndex.coerceIn(0, routes.lastIndex)
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val isRecommendedDetails = detailIdx == recIdx

        val googleMarshioDecision =
            if (isRecommendedDetails) {
                buildGoogleMarshioDecision(
                    routes = routes,
                    identities = identities,
                    recommendedIndex = recIdx,
                    mode = mode,
                )
            } else {
                null
            }

        val equivalentTrip =
            EquivalentTripHonesty.evaluate(
                routes = routes,
                identities = identities,
                recommendedIndex = recIdx,
            ).isEquivalentTrip

        val rejectedAlternatives =
            if (isRecommendedDetails && !equivalentTrip) {
                buildRejectedAlternatives(
                    routes = routes,
                    identities = identities,
                    recommendedIndex = recIdx,
                    mode = mode,
                )
            } else {
                emptyList()
            }

        val rejectedAlternativesIntro =
            if (rejectedAlternatives.isNotEmpty()) {
                buildRejectedAlternativesIntro(
                    marshioAgreesWithGoogle = recIdx == GOOGLE_DEFAULT_INDEX,
                    alternativeCount = rejectedAlternatives.size,
                )
            } else {
                ""
            }

        return Result(
            googleMarshioDecision = googleMarshioDecision,
            rejectedAlternativesIntro = rejectedAlternativesIntro,
            rejectedAlternatives = rejectedAlternatives,
        )
    }

    private fun buildGoogleMarshioDecision(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        mode: PreferenceMode,
    ): RouteGoogleMarshioDecisionUiModel {
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val googleIdx = GOOGLE_DEFAULT_INDEX.coerceIn(0, routes.lastIndex)
        val googleRoute = routes[googleIdx]
        val marshioRoute = routes[recIdx]
        val googleCard = routeCard("Google says", googleRoute, identities.getOrNull(googleIdx))
        val marshioCard = routeCard("MARSHIO says", marshioRoute, identities.getOrNull(recIdx))

        val equivalentTrip =
            EquivalentTripHonesty.evaluate(
                routes = routes,
                identities = identities,
                recommendedIndex = recIdx,
            ).isEquivalentTrip

        if (equivalentTrip) {
            return RouteGoogleMarshioDecisionUiModel(
                state = GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
                headline = "MARSHIO sees no meaningful difference",
                subtext = "Google's route is good enough. MARSHIO agrees with Google.",
                singleRoute = googleCard,
                verdictText = "Use this route.",
            )
        }

        if (recIdx == googleIdx) {
            return RouteGoogleMarshioDecisionUiModel(
                state = GoogleMarshioDecisionState.AGREES,
                headline = "MARSHIO agrees with Google",
                subtext = "Google and MARSHIO selected the same route.",
                singleRoute = marshioCard,
                verdictText = "Use this route.",
            )
        }

        val facts = analyzeDisagreement(googleRoute, marshioRoute)
        val whyOneLiner = buildWhyOneLiner(mode, facts)
        val reasons = buildDisagreementReasons(mode, facts)

        return RouteGoogleMarshioDecisionUiModel(
            state = GoogleMarshioDecisionState.DISAGREES,
            headline = "MARSHIO disagrees with Google",
            subtext = whyOneLiner,
            googleRoute = googleCard,
            marshioRoute = marshioCard,
            disagreementReasons = reasons,
            whyOneLiner = whyOneLiner,
            verdictText = buildVerdictText(whyOneLiner),
        )
    }

    private fun analyzeDisagreement(
        googleRoute: RealRouteDebugData,
        marshioRoute: RealRouteDebugData,
    ): DisagreementFacts {
        val timeDeltaSeconds = marshioRoute.durationSeconds - googleRoute.durationSeconds
        val distanceDeltaMeters = marshioRoute.distanceMeters - googleRoute.distanceMeters

        val googleDelay = trafficDelaySeconds(googleRoute)
        val marshioDelay = trafficDelaySeconds(marshioRoute)
        val trafficDelaySavedSeconds =
            if (googleDelay != null && marshioDelay != null) {
                googleDelay - marshioDelay
            } else {
                null
            }

        return DisagreementFacts(
            marshioFasterSeconds = (-timeDeltaSeconds).coerceAtLeast(0),
            marshioSlowerSeconds = timeDeltaSeconds.coerceAtLeast(0),
            distanceSavedMeters = (-distanceDeltaMeters).coerceAtLeast(0),
            distanceLongerMeters = distanceDeltaMeters.coerceAtLeast(0),
            trafficDelaySavedSeconds = trafficDelaySavedSeconds,
            salikSavedAed = (googleRoute.tollAED - marshioRoute.tollAED).coerceAtLeast(0),
        )
    }

    internal fun buildWhyOneLiner(
        mode: PreferenceMode,
        facts: DisagreementFacts,
    ): String {
        val hasDistanceWin = facts.distanceSavedMeters >= DISTANCE_DELTA_THRESHOLD_METERS
        val hasTrafficWin =
            facts.trafficDelaySavedSeconds != null &&
                facts.trafficDelaySavedSeconds >= TIME_DELTA_THRESHOLD_SECONDS
        val hasSalikWin = facts.salikSavedAed > 0
        val marshioClearlyFaster =
            facts.marshioFasterSeconds >= TIME_DELTA_THRESHOLD_SECONDS
        val marshioClearlySlower =
            facts.marshioSlowerSeconds >= TIME_DELTA_THRESHOLD_SECONDS
        val smallTimeGap =
            facts.marshioSlowerSeconds in 1..TIME_MEANINGFUL_DIFFERENCE_SECONDS ||
                facts.marshioFasterSeconds in 1..TIME_MEANINGFUL_DIFFERENCE_SECONDS

        return when (mode) {
            PreferenceMode.NO_TOLLS -> {
                when {
                    marshioClearlyFaster ->
                        "MARSHIO recommends this route because it is ${formatMinutes(facts.marshioFasterSeconds)} faster " +
                            "and matches your No tolls preference."
                    marshioClearlySlower && hasCompensatingBenefit(facts) ->
                        buildHonestTradeoffOneLiner(facts, modeSuffix = "Matches your No tolls preference.")
                    hasSalikWin ->
                        "MARSHIO recommends this route because it avoids Salik on Google's default."
                    hasDistanceWin ->
                        "MARSHIO recommends this route because it is ${formatHumanDistance(facts.distanceSavedMeters)} shorter " +
                            "and matches your No tolls preference."
                    else ->
                        "MARSHIO selected a different route to match your No tolls preference — compare both below."
                }
            }
            PreferenceMode.CALM -> {
                when {
                    marshioClearlyFaster ->
                        "MARSHIO recommends this route because it is ${formatMinutes(facts.marshioFasterSeconds)} faster " +
                            "with less traffic slowdown."
                    marshioClearlySlower && hasCompensatingBenefit(facts) ->
                        buildHonestTradeoffOneLiner(facts, modeSuffix = "Matches your Calm preference.")
                    hasTrafficWin ->
                        "MARSHIO recommends this route because it has ${formatMinutes(facts.trafficDelaySavedSeconds!!)} less traffic delay."
                    hasDistanceWin && smallTimeGap ->
                        "MARSHIO recommends this route because it saves ${formatHumanDistance(facts.distanceSavedMeters)} " +
                            "with no meaningful time difference."
                    else ->
                        "MARSHIO selected a calmer route — compare traffic delay on both options below."
                }
            }
            PreferenceMode.FASTEST -> {
                when {
                    marshioClearlyFaster ->
                        "MARSHIO recommends this route because it is ${formatMinutes(facts.marshioFasterSeconds)} faster " +
                            "than Google's default."
                    marshioClearlySlower && hasCompensatingBenefit(facts) ->
                        buildHonestTradeoffOneLiner(facts)
                    marshioClearlySlower ->
                        "Google's default is ${formatMinutes(facts.marshioSlowerSeconds)} faster — " +
                            "MARSHIO picked a different route; compare both before you drive."
                    hasDistanceWin ->
                        "MARSHIO recommends this route because it saves ${formatHumanDistance(facts.distanceSavedMeters)} " +
                            "with no meaningful time difference."
                    else ->
                        buildHonestTradeoffOneLiner(facts).takeIf { it.isNotBlank() }
                            ?: "MARSHIO selected a different route from Google's default — compare both below."
                }
            }
        }
    }

    internal fun buildDisagreementReasons(
        mode: PreferenceMode,
        facts: DisagreementFacts,
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (facts.marshioFasterSeconds >= TIME_DELTA_THRESHOLD_SECONDS) {
            reasons += "${formatMinutes(facts.marshioFasterSeconds)} faster than Google's default"
        }

        if (facts.distanceSavedMeters >= DISTANCE_DELTA_THRESHOLD_METERS) {
            reasons += "Saves ${formatHumanDistance(facts.distanceSavedMeters)}"
        }

        facts.trafficDelaySavedSeconds?.let { saved ->
            if (saved >= TIME_DELTA_THRESHOLD_SECONDS) {
                reasons += "${formatMinutes(saved)} less traffic delay"
            }
        }

        if (facts.salikSavedAed > 0) {
            reasons += "${facts.salikSavedAed} AED less Salik"
        }

        if (
            facts.marshioSlowerSeconds in 1..TIME_MEANINGFUL_DIFFERENCE_SECONDS &&
            reasons.isNotEmpty()
        ) {
            reasons += "No meaningful time difference"
        }

        when (mode) {
            PreferenceMode.NO_TOLLS ->
                if (reasons.isEmpty() || facts.salikSavedAed > 0) {
                    reasons += "Matches your No tolls preference"
                }
            PreferenceMode.CALM ->
                if (facts.trafficDelaySavedSeconds != null && facts.trafficDelaySavedSeconds >= TIME_DELTA_THRESHOLD_SECONDS) {
                    reasons += "Matches your Calm preference"
                }
            PreferenceMode.FASTEST -> Unit
        }

        return reasons.distinct()
    }

    internal fun buildDisagreementReasons(
        googleRoute: RealRouteDebugData,
        marshioRoute: RealRouteDebugData,
    ): List<String> =
        buildDisagreementReasons(
            mode = PreferenceMode.FASTEST,
            facts = analyzeDisagreement(googleRoute, marshioRoute),
        )

    internal fun buildVerdictText(whyOneLiner: String): String = whyOneLiner

    private fun hasCompensatingBenefit(facts: DisagreementFacts): Boolean =
        facts.distanceSavedMeters >= DISTANCE_DELTA_THRESHOLD_METERS ||
            facts.salikSavedAed > 0 ||
            (facts.trafficDelaySavedSeconds != null &&
                facts.trafficDelaySavedSeconds >= TIME_DELTA_THRESHOLD_SECONDS)

    internal fun buildHonestTradeoffOneLiner(
        facts: DisagreementFacts,
        modeSuffix: String? = null,
    ): String {
        val googleAdvantage =
            if (facts.marshioSlowerSeconds >= TIME_DELTA_THRESHOLD_SECONDS) {
                "Google's route is ${formatMinutes(facts.marshioSlowerSeconds)} faster."
            } else {
                ""
            }
        val marshioBenefit =
            when {
                facts.distanceSavedMeters >= DISTANCE_DELTA_THRESHOLD_METERS ->
                    "MARSHIO selected the shorter route (${formatHumanDistance(facts.distanceSavedMeters)} less)."
                facts.salikSavedAed > 0 ->
                    "MARSHIO avoids ${facts.salikSavedAed} AED Salik."
                facts.trafficDelaySavedSeconds != null &&
                    facts.trafficDelaySavedSeconds >= TIME_DELTA_THRESHOLD_SECONDS ->
                    "MARSHIO has ${formatMinutes(facts.trafficDelaySavedSeconds)} less traffic delay."
                else ->
                    "MARSHIO selected a different route."
            }
        val suffix = modeSuffix?.takeIf { it.isNotBlank() }.orEmpty()
        return listOf(googleAdvantage, marshioBenefit, suffix, "Decide which trade-off you prefer.")
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun routeCard(
        cardTitle: String,
        route: RealRouteDebugData,
        identity: RouteIdentity?,
    ): RouteDecisionRouteCardUiModel =
        RouteDecisionRouteCardUiModel(
            cardTitle = cardTitle,
            roadName = roadName(route, identity),
            etaText = route.durationText,
            distanceText = route.distanceText,
            trafficDelayText = trafficDelayText(route),
            salikText = salikText(route),
        )

    internal fun roadName(
        route: RealRouteDebugData,
        identity: RouteIdentity?,
    ): String =
        identity?.fullName?.takeIf { it.isNotBlank() }
            ?: identity?.primaryName?.takeIf { it.isNotBlank() }
            ?: RouteIdentityPresentationPolicy.honestSummary(route).takeIf { it.isNotBlank() }
            ?: route.routeSummary.takeIf { it.isNotBlank() }
            ?: "Unnamed route"

    internal fun trafficDelayText(route: RealRouteDebugData): String? {
        val inTraffic = route.durationInTrafficSeconds ?: return null
        val base = route.baseDurationSeconds.takeIf { it > 0 } ?: return null
        val delaySeconds = inTraffic - base
        if (delaySeconds < TIME_DELTA_THRESHOLD_SECONDS) return null
        val minutes = ComparativeEvidence.minutesRounded(delaySeconds)
        return "+$minutes min traffic"
    }

    internal fun trafficDelaySeconds(route: RealRouteDebugData): Int? {
        val inTraffic = route.durationInTrafficSeconds ?: return null
        val base = route.baseDurationSeconds.takeIf { it > 0 } ?: return null
        return (inTraffic - base).coerceAtLeast(0)
    }

    internal fun salikText(route: RealRouteDebugData): String? =
        when {
            route.tollAED > 0 -> "${route.tollAED} AED Salik"
            !route.hasToll -> "No Salik"
            else -> null
        }

    internal fun buildRejectedAlternativesIntro(
        marshioAgreesWithGoogle: Boolean,
        alternativeCount: Int,
    ): String =
        if (marshioAgreesWithGoogle) {
            val altWord =
                if (alternativeCount == 1) {
                    "1 alternative"
                } else {
                    "$alternativeCount alternatives"
                }
            "Google also showed $altWord. MARSHIO rejected them because they do not beat the selected route for this mode."
        } else {
            "These are the other Google routes MARSHIO rejected."
        }

    private fun buildRejectedAlternatives(
        routes: List<RealRouteDebugData>,
        identities: List<RouteIdentity>,
        recommendedIndex: Int,
        mode: PreferenceMode,
    ): List<RejectedAlternativeUiModel> {
        val selected = routes[recommendedIndex]
        return routes.indices
            .filter { it != recommendedIndex && it != GOOGLE_DEFAULT_INDEX }
            .sortedBy { routes[it].durationSeconds }
            .map { altIdx ->
                val alternative = routes[altIdx]
                val facts = compareAlternative(selected, alternative)
                val advantages = buildAlternativeAdvantageLines(facts)
                val drawbacks = buildAlternativeDrawbackLines(facts)
                val road = roadName(alternative, identities.getOrNull(altIdx))
                RejectedAlternativeUiModel(
                    label = "Alternative ${altIdx + 1} — $road",
                    advantageLines = advantages,
                    drawbackLines = drawbacks,
                    verdictText =
                        buildAlternativeRejectionVerdict(
                            mode = mode,
                            facts = facts,
                            advantages = advantages,
                            drawbacks = drawbacks,
                        ),
                )
            }
    }

    internal fun compareAlternative(
        selected: RealRouteDebugData,
        alternative: RealRouteDebugData,
    ): AlternativeComparisonFacts {
        val selectedTraffic = trafficDelaySeconds(selected)
        val alternativeTraffic = trafficDelaySeconds(alternative)
        val trafficDelayDeltaSeconds =
            if (selectedTraffic != null && alternativeTraffic != null) {
                alternativeTraffic - selectedTraffic
            } else {
                null
            }
        return AlternativeComparisonFacts(
            timeDeltaSeconds = alternative.durationSeconds - selected.durationSeconds,
            distanceDeltaMeters = alternative.distanceMeters - selected.distanceMeters,
            salikDeltaAed = alternative.tollAED - selected.tollAED,
            trafficDelayDeltaSeconds = trafficDelayDeltaSeconds,
        )
    }

    internal fun buildAlternativeAdvantageLines(
        facts: AlternativeComparisonFacts,
    ): List<String> {
        val advantages = mutableListOf<String>()
        if (facts.timeDeltaSeconds <= -TIME_DELTA_THRESHOLD_SECONDS) {
            advantages +=
                "${formatMinutes(-facts.timeDeltaSeconds)} faster"
        }
        if (facts.distanceDeltaMeters <= -DISTANCE_DELTA_THRESHOLD_METERS) {
            advantages +=
                "${formatHumanDistance(-facts.distanceDeltaMeters)} shorter"
        }
        if (facts.salikDeltaAed < 0) {
            advantages +=
                "${-facts.salikDeltaAed} AED less Salik"
        }
        facts.trafficDelayDeltaSeconds?.let { delta ->
            if (delta <= -TIME_DELTA_THRESHOLD_SECONDS) {
                advantages +=
                    "${formatMinutes(-delta)} less traffic delay"
            }
        }
        return advantages
    }

    internal fun buildAlternativeDrawbackLines(
        facts: AlternativeComparisonFacts,
    ): List<String> {
        val drawbacks = mutableListOf<String>()
        if (facts.timeDeltaSeconds >= TIME_DELTA_THRESHOLD_SECONDS) {
            val minutes = ComparativeEvidence.minutesRounded(facts.timeDeltaSeconds)
            drawbacks += "+$minutes min slower"
        }
        if (facts.distanceDeltaMeters >= DISTANCE_DELTA_THRESHOLD_METERS) {
            drawbacks += "+${formatHumanDistance(facts.distanceDeltaMeters)} longer"
        }
        if (facts.salikDeltaAed > 0) {
            drawbacks += "+${facts.salikDeltaAed} AED Salik"
        }
        facts.trafficDelayDeltaSeconds?.let { delta ->
            if (delta >= TIME_DELTA_THRESHOLD_SECONDS) {
                drawbacks += "+${formatMinutes(delta)} traffic delay"
            }
        }
        return drawbacks
    }

    internal fun buildAlternativeRejectionVerdict(
        mode: PreferenceMode,
        facts: AlternativeComparisonFacts,
        advantages: List<String>,
        drawbacks: List<String>,
    ): String {
        val altSlower = facts.timeDeltaSeconds >= TIME_DELTA_THRESHOLD_SECONDS
        val altFaster = facts.timeDeltaSeconds <= -TIME_DELTA_THRESHOLD_SECONDS
        val altShorter = facts.distanceDeltaMeters <= -DISTANCE_DELTA_THRESHOLD_METERS
        val altLonger = facts.distanceDeltaMeters >= DISTANCE_DELTA_THRESHOLD_METERS
        val altCheaperSalik = facts.salikDeltaAed < 0
        val altCostlierSalik = facts.salikDeltaAed > 0
        val altLessTraffic =
            facts.trafficDelayDeltaSeconds?.let { it <= -TIME_DELTA_THRESHOLD_SECONDS } == true
        val altMoreTraffic =
            facts.trafficDelayDeltaSeconds?.let { it >= TIME_DELTA_THRESHOLD_SECONDS } == true
        val hasAnyAdvantage = advantages.isNotEmpty()
        val hasAnyDisadvantage = drawbacks.isNotEmpty()

        return when (mode) {
            PreferenceMode.FASTEST ->
                when {
                    altShorter && altSlower ->
                        "Rejected: shorter distance, but slower ETA. FASTEST mode prioritizes time."
                    altSlower && altLonger ->
                        "Rejected: slower and longer than MARSHIO route."
                    altSlower ->
                        "Rejected: slower ETA."
                    altFaster && altCostlierSalik ->
                        "Rejected: faster, but has Salik."
                    altFaster ->
                        "Not selected because MARSHIO picked the route that best fits FASTEST mode."
                    altLonger && !hasAnyAdvantage ->
                        "Rejected: longer than the selected route."
                    hasAnyAdvantage && hasAnyDisadvantage ->
                        buildMixedAdvantageVerdict(advantages, drawbacks, mode)
                    !hasAnyAdvantage && hasAnyDisadvantage ->
                        "Rejected: no advantage over the selected route for FASTEST mode."
                    else ->
                        "Rejected: does not beat the selected route for FASTEST mode."
                }
            PreferenceMode.NO_TOLLS ->
                when {
                    altCostlierSalik ->
                        "Rejected: higher Salik cost."
                    altCheaperSalik &&
                        altSlower &&
                        facts.timeDeltaSeconds >= PresentationThresholds.COMPARATIVE_SAVE_AED_MIN_TIME_DELTA_SECONDS ->
                        "Cheaper, but much slower. Not selected for this mode threshold."
                    altCheaperSalik && altSlower ->
                        "Cheaper, but slower. Not selected in Save AED mode."
                    altSlower ->
                        "Rejected: slower ETA."
                    altFaster && altCostlierSalik ->
                        "Rejected: faster, but has Salik."
                    hasAnyAdvantage && hasAnyDisadvantage ->
                        buildMixedAdvantageVerdict(advantages, drawbacks, mode)
                    !hasAnyAdvantage && hasAnyDisadvantage ->
                        "Rejected: no advantage over the selected route for Save AED mode."
                    else ->
                        "Rejected: does not beat the selected route for Save AED mode."
                }
            PreferenceMode.CALM ->
                when {
                    altMoreTraffic ->
                        "Rejected: less calm route."
                    altLessTraffic && altSlower ->
                        "Rejected: less traffic delay, but slower ETA."
                    facts.trafficDelayDeltaSeconds == null && !hasAnyAdvantage ->
                        "Rejected: no clear advantage for Smooth Drive."
                    altSlower && !altLessTraffic ->
                        "Rejected: slower ETA with no calm-route benefit."
                    hasAnyAdvantage && hasAnyDisadvantage ->
                        buildMixedAdvantageVerdict(advantages, drawbacks, mode)
                    !hasAnyAdvantage && hasAnyDisadvantage ->
                        "Rejected: no advantage over the selected route for Smooth Drive."
                    else ->
                        "Rejected: does not beat the selected route for Smooth Drive."
                }
        }
    }

    private fun buildMixedAdvantageVerdict(
        advantages: List<String>,
        drawbacks: List<String>,
        mode: PreferenceMode,
    ): String {
        val advantageSummary = advantages.firstOrNull()?.lowercase().orEmpty()
        val drawbackSummary = drawbacks.firstOrNull()?.lowercase().orEmpty()
        val modeLabel =
            when (mode) {
                PreferenceMode.FASTEST -> "FASTEST mode"
                PreferenceMode.NO_TOLLS -> "Save AED mode"
                PreferenceMode.CALM -> "Smooth Drive mode"
            }
        return when {
            advantageSummary.contains("shorter") && drawbackSummary.contains("slower") ->
                "Rejected: shorter distance, but slower ETA. $modeLabel prioritizes time."
            advantageSummary.contains("faster") && drawbackSummary.contains("salik") ->
                "Rejected: faster, but has Salik. Not selected for this mode."
            advantageSummary.contains("less salik") && drawbackSummary.contains("slower") ->
                "Rejected: cheaper, but slower. Not selected for this mode."
            else ->
                "Rejected: has one advantage, but not enough for $modeLabel."
        }
    }

    internal fun formatHumanDistance(meters: Int): String =
        if (meters < 1000) {
            "${meters} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        }

    private fun formatMinutes(seconds: Int): String {
        val minutes = ComparativeEvidence.minutesRounded(seconds.coerceAtLeast(0))
        return if (minutes == 1) "1 min" else "$minutes min"
    }
}
