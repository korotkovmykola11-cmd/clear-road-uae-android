package com.clearroad.app

import com.clearroad.app.domain.EquivalentTripHonesty
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteConfidence
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.ui.driveweather.DecisionConfidence
import com.clearroad.app.ui.driveweather.DriveMood
import com.clearroad.app.ui.driveweather.DriveWeatherChip
import com.clearroad.app.ui.driveweather.DriveWeatherHero
import com.clearroad.app.ui.driveweather.DriveWeatherUiModel
import com.clearroad.app.ui.driveweather.TripTrafficCharacter
import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RouteGoogleMarshioDecisionUiModel

/**
 * Maps live route data into Drive Weather presentation for Route Details.
 * Presentation only — does not affect routing or scoring.
 */
internal object DriveWeatherAssembly {

    private const val GOOGLE_DEFAULT_INDEX = 0
    private const val HEAVY_DELAY_RATIO = 0.22
    private const val HEAVY_DELAY_MINUTES = 6
    private const val CALM_DELAY_RATIO = 0.10
    private const val CALM_DELAY_MINUTES = 3
    private const val TIME_TRADEOFF_SECONDS = 120
    private const val SALIK_WIN_AED = 1

    data class Input(
        val detailRoute: RealRouteDebugData,
        val detailRouteIndex: Int,
        val routes: List<RealRouteDebugData>,
        val identities: List<RouteIdentity>,
        val mode: PreferenceMode,
        val recommendedIndex: Int,
        val isRecommendedRouteDetails: Boolean,
        val googleMarshioDecision: RouteGoogleMarshioDecisionUiModel?,
        val routeIdentityTitle: String,
        val routeReasonTitle: String,
        val routeReasonWhy: String,
        val directionsStatus: String?,
    )

    fun build(input: Input): DriveWeatherUiModel? {
        if (input.routes.isEmpty()) return null

        val detailRoute = input.detailRoute
        val delayRatio = RouteTrafficDelayMetrics.delayRatio(detailRoute)
        val delayMinutes = RouteTrafficDelayMetrics.delayMinutesRounded(detailRoute)
        val comparison =
            if (input.isRecommendedRouteDetails) {
                buildComparisonFacts(input.routes, input.recommendedIndex)
            } else {
                null
            }
        val trafficCharacter = detectTrafficCharacter(input, comparison, delayRatio, delayMinutes)
        val mood = resolveMood(delayRatio, delayMinutes, trafficCharacter, input.googleMarshioDecision)
        val hero =
            buildHero(
                mood = mood,
                trafficCharacter = trafficCharacter,
                decision = input.googleMarshioDecision,
                comparison = comparison,
                routeIdentityTitle = input.routeIdentityTitle,
                input = input,
                delayRatio = delayRatio,
                delayMinutes = delayMinutes,
            )
        val chips =
            buildChips(
                mood = mood,
                trafficCharacter = trafficCharacter,
                comparison = comparison,
                detailRoute = detailRoute,
                decision = input.googleMarshioDecision,
            )
        val storySteps =
            buildStorySteps(
                mood = mood,
                trafficCharacter = trafficCharacter,
                decision = input.googleMarshioDecision,
                comparison = comparison,
                input = input,
                delayMinutes = delayMinutes,
            )
        val liveLabel =
            if (input.directionsStatus == "OK") {
                "Based on live traffic"
            } else {
                "Limited traffic data"
            }
        val animationKey =
            buildString {
                append(input.mode.name)
                append('-')
                append(input.detailRouteIndex)
                append('-')
                append(mood.name)
                append('-')
                append(hero.decisionConfidence.name)
            }

        return DriveWeatherUiModel(
            hero = hero,
            liveUpdateLabel = liveLabel,
            chips = chips,
            storySteps = storySteps,
            animationKey = animationKey,
        )
    }

    private data class ComparisonFacts(
        val marshioSlowerSeconds: Int,
        val marshioFasterSeconds: Int,
        val salikSavedAed: Int,
        val trafficDelaySavedSeconds: Int?,
    )

    private fun buildComparisonFacts(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): ComparisonFacts? {
        if (routes.size < 2) return null
        val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
        val googleIdx = GOOGLE_DEFAULT_INDEX.coerceIn(0, routes.lastIndex)
        val googleRoute = routes[googleIdx]
        val marshioRoute = routes[recIdx]
        val timeDelta = marshioRoute.durationSeconds - googleRoute.durationSeconds
        val googleDelay =
            RouteTrafficDelayMetrics.trafficSeconds(googleRoute) -
                RouteTrafficDelayMetrics.baseSeconds(googleRoute)
        val marshioDelay =
            RouteTrafficDelayMetrics.trafficSeconds(marshioRoute) -
                RouteTrafficDelayMetrics.baseSeconds(marshioRoute)
        val trafficDelaySaved =
            if (googleDelay > 0 && marshioDelay >= 0) {
                (googleDelay - marshioDelay).coerceAtLeast(0)
            } else {
                null
            }
        return ComparisonFacts(
            marshioSlowerSeconds = timeDelta.coerceAtLeast(0),
            marshioFasterSeconds = (-timeDelta).coerceAtLeast(0),
            salikSavedAed = (googleRoute.tollAED - marshioRoute.tollAED).coerceAtLeast(0),
            trafficDelaySavedSeconds = trafficDelaySaved?.takeIf { it > 0 },
        )
    }

    private fun detectTrafficCharacter(
        input: Input,
        comparison: ComparisonFacts?,
        delayRatio: Double,
        delayMinutes: Int,
    ): TripTrafficCharacter? {
        if (!input.isRecommendedRouteDetails || comparison == null) return null
        val stopStartTradeoff =
            comparison.salikSavedAed >= SALIK_WIN_AED &&
                comparison.marshioSlowerSeconds in 60..TIME_TRADEOFF_SECONDS &&
                delayRatio >= 0.06
        if (stopStartTradeoff && input.mode == PreferenceMode.NO_TOLLS) {
            return TripTrafficCharacter.STOP_START
        }
        if (delayRatio >= HEAVY_DELAY_RATIO || delayMinutes >= HEAVY_DELAY_MINUTES) {
            return TripTrafficCharacter.CONGESTED
        }
        if (input.googleMarshioDecision?.state == GoogleMarshioDecisionState.DISAGREES) {
            return TripTrafficCharacter.DETOUR
        }
        if (delayRatio < CALM_DELAY_RATIO && delayMinutes < CALM_DELAY_MINUTES) {
            return TripTrafficCharacter.FLOWING
        }
        return null
    }

    private fun resolveMood(
        delayRatio: Double,
        delayMinutes: Int,
        trafficCharacter: TripTrafficCharacter?,
        decision: RouteGoogleMarshioDecisionUiModel?,
    ): DriveMood {
        if (trafficCharacter == TripTrafficCharacter.STOP_START) return DriveMood.BUSY
        if (delayRatio >= HEAVY_DELAY_RATIO || delayMinutes >= HEAVY_DELAY_MINUTES) {
            return DriveMood.HEAVY
        }
        if (
            decision?.state == GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE ||
                decision?.state == GoogleMarshioDecisionState.AGREES
        ) {
            return DriveMood.CALM
        }
        if (delayRatio < CALM_DELAY_RATIO && delayMinutes < CALM_DELAY_MINUTES) {
            return DriveMood.CALM
        }
        return DriveMood.BUSY
    }

    private fun buildHero(
        mood: DriveMood,
        trafficCharacter: TripTrafficCharacter?,
        decision: RouteGoogleMarshioDecisionUiModel?,
        comparison: ComparisonFacts?,
        routeIdentityTitle: String,
        input: Input,
        delayRatio: Double,
        delayMinutes: Int,
    ): DriveWeatherHero {
        val (line1, line2, emoji, caption) =
            heroCopy(
                mood = mood,
                trafficCharacter = trafficCharacter,
                decision = decision,
                input = input,
            )
        val (etaValue, etaUnit) = etaDeltaMetrics(comparison)
        val (savingsValue, savingsUnit) = savingsMetrics(comparison)
        val confidence =
            resolveConfidence(
                mood = mood,
                trafficCharacter = trafficCharacter,
                decision = decision,
                input = input,
                delayRatio = delayRatio,
                delayMinutes = delayMinutes,
            )
        val (startLabel, endLabel) = routeEndpointLabels(routeIdentityTitle)

        return DriveWeatherHero(
            mood = mood,
            moodEmoji = emoji,
            moodCaption = caption,
            adviceLine1 = line1,
            adviceLine2 = line2,
            etaDeltaValue = etaValue,
            etaDeltaUnit = etaUnit,
            savingsValue = savingsValue,
            savingsUnit = savingsUnit,
            decisionConfidence = confidence,
            routeStartLabel = startLabel,
            routeEndLabel = endLabel,
            trafficCharacter = trafficCharacter,
        )
    }

    private fun heroCopy(
        mood: DriveMood,
        trafficCharacter: TripTrafficCharacter?,
        decision: RouteGoogleMarshioDecisionUiModel?,
        input: Input,
    ): Quadruple<String, String, String, String> {
        if (trafficCharacter == TripTrafficCharacter.STOP_START) {
            return Quadruple(
                "Lots of lights",
                "along the way.",
                "😟",
                "Stop & go",
            )
        }
        return when (mood) {
            DriveMood.CALM ->
                when (decision?.state) {
                    GoogleMarshioDecisionState.AGREES,
                    GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
                    ->
                        Quadruple(
                            "Traffic is moving well.",
                            "Same route as Google.",
                            "😊",
                            "Easy drive",
                        )
                    else ->
                        Quadruple(
                            "Traffic is moving well.",
                            input.routeReasonTitle.ifBlank { "Good conditions ahead." },
                            "😊",
                            "Easy drive",
                        )
                }
            DriveMood.BUSY ->
                Quadruple(
                    "Traffic is busy,",
                    "but it's moving.",
                    "😐",
                    "Busy but moving",
                )
            DriveMood.HEAVY ->
                Quadruple(
                    "Roads are slow",
                    "across the corridor.",
                    "😡",
                    "Heavy congestion",
                )
        }
    }

    private fun resolveConfidence(
        mood: DriveMood,
        trafficCharacter: TripTrafficCharacter?,
        decision: RouteGoogleMarshioDecisionUiModel?,
        input: Input,
        delayRatio: Double,
        delayMinutes: Int,
    ): DecisionConfidence {
        if (!input.isRecommendedRouteDetails) {
            return DecisionConfidence.WORTH_CONSIDERING
        }
        if (trafficCharacter == TripTrafficCharacter.STOP_START) {
            return DecisionConfidence.WORTH_CONSIDERING
        }
        if (mood == DriveMood.HEAVY) {
            return DecisionConfidence.CLOSE_CALL
        }
        val routeConfidence =
            RouteConfidence.fromAdvantageOverNext(
                routes = input.routes,
                recommendedIndex = input.recommendedIndex,
            )
        return when (decision?.state) {
            GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
            GoogleMarshioDecisionState.AGREES,
            ->
                if (routeConfidence.level == RouteConfidence.Level.LOW) {
                    DecisionConfidence.WORTH_CONSIDERING
                } else {
                    DecisionConfidence.STRONG
                }
            GoogleMarshioDecisionState.DISAGREES -> {
                if (mood == DriveMood.BUSY && routeConfidence.isHighConfidence) {
                    DecisionConfidence.STRONG_CHOICE
                } else if (routeConfidence.level == RouteConfidence.Level.LOW) {
                    DecisionConfidence.CLOSE_CALL
                } else if (routeConfidence.level == RouteConfidence.Level.MEDIUM) {
                    DecisionConfidence.WORTH_CONSIDERING
                } else {
                    DecisionConfidence.STRONG_CHOICE
                }
            }
            null ->
                when {
                    delayRatio >= HEAVY_DELAY_RATIO || delayMinutes >= HEAVY_DELAY_MINUTES ->
                        DecisionConfidence.CLOSE_CALL
                    routeConfidence.isHighConfidence -> DecisionConfidence.STRONG
                    routeConfidence.level == RouteConfidence.Level.MEDIUM ->
                        DecisionConfidence.WORTH_CONSIDERING
                    else -> DecisionConfidence.CLOSE_CALL
                }
        }
    }

    private fun buildChips(
        mood: DriveMood,
        trafficCharacter: TripTrafficCharacter?,
        comparison: ComparisonFacts?,
        detailRoute: RealRouteDebugData,
        decision: RouteGoogleMarshioDecisionUiModel?,
    ): List<DriveWeatherChip> {
        val chips = mutableListOf<DriveWeatherChip>()

        val trafficChip =
            when {
                trafficCharacter == TripTrafficCharacter.STOP_START ->
                    DriveWeatherChip(emoji = "🟠", text = "Stop & go")
                mood == DriveMood.HEAVY ->
                    DriveWeatherChip(emoji = "🔴", text = "Heavy congestion")
                mood == DriveMood.BUSY ->
                    DriveWeatherChip(emoji = "🟠", text = "Busy but moving")
                else ->
                    DriveWeatherChip(emoji = "🟢", text = "Easy drive")
            }
        chips += trafficChip

        comparison?.let { facts ->
            if (facts.salikSavedAed >= SALIK_WIN_AED) {
                chips +=
                    DriveWeatherChip(
                        emoji = "🟢",
                        text = "Save ${facts.salikSavedAed} AED",
                    )
            } else if (detailRoute.tollAED == 0 && detailRoute.hasToll.not()) {
                val googleToll = decision?.googleRoute?.salikText.orEmpty()
                if (googleToll.isNotBlank() && googleToll != "No Salik") {
                    chips += DriveWeatherChip(emoji = "🟢", text = "No Salik")
                }
            }

            when {
                facts.marshioSlowerSeconds in 60..TIME_TRADEOFF_SECONDS -> {
                    val minutes = formatMinutesRounded(facts.marshioSlowerSeconds)
                    chips += DriveWeatherChip(emoji = "⚪", text = "+$minutes min")
                }
                facts.marshioFasterSeconds >= 60 -> {
                    val minutes = formatMinutesRounded(facts.marshioFasterSeconds)
                    chips += DriveWeatherChip(emoji = "⚪", text = "-$minutes min")
                }
                decision?.state == GoogleMarshioDecisionState.AGREES ||
                    decision?.state == GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE ->
                    chips += DriveWeatherChip(emoji = "⚪", text = "Same time")
            }
        }

        return chips
    }

    private fun buildStorySteps(
        mood: DriveMood,
        trafficCharacter: TripTrafficCharacter?,
        decision: RouteGoogleMarshioDecisionUiModel?,
        comparison: ComparisonFacts?,
        input: Input,
        delayMinutes: Int,
    ): List<String> {
        if (!input.isRecommendedRouteDetails) {
            return listOfNotNull(
                input.routeReasonTitle.takeIf { it.isNotBlank() },
                input.routeReasonWhy.takeIf { it.isNotBlank() },
                "Compare with other options below",
            ).take(4).let { padStory(it) }
        }

        if (trafficCharacter == TripTrafficCharacter.STOP_START) {
            val extraMin =
                comparison?.marshioSlowerSeconds?.let { formatMinutesRounded(it) } ?: "a few"
            return listOf(
                "Lots of lights ahead",
                "Checked every option",
                "+$extraMin min on city streets",
                "Better value overall",
            )
        }

        return when (decision?.state) {
            GoogleMarshioDecisionState.NO_MEANINGFUL_DIFFERENCE,
            GoogleMarshioDecisionState.AGREES,
            ->
                listOf(
                    "Checked traffic",
                    if (decision.state == GoogleMarshioDecisionState.AGREES) {
                        "Same route as Google"
                    } else {
                        "No faster alternative"
                    },
                    "Same arrival",
                    "You're good to go",
                )
            GoogleMarshioDecisionState.DISAGREES -> {
                when (mood) {
                    DriveMood.HEAVY ->
                        listOf(
                            "Congestion everywhere",
                            "Compared all options",
                            "This is still best",
                            "Expect delays",
                        )
                    DriveMood.BUSY -> {
                        val extraMin =
                            comparison?.marshioSlowerSeconds?.let { formatMinutesRounded(it) }
                                ?: "2"
                        val step2 =
                            when {
                                comparison?.salikSavedAed?.let { it >= SALIK_WIN_AED } == true ->
                                    "Found cheaper route"
                                comparison?.trafficDelaySavedSeconds?.let { it >= 60 } == true ->
                                    "Found calmer route"
                                else -> "Found a better fit"
                            }
                        listOf(
                            if (delayMinutes >= 4) "Heavy traffic ahead" else "Traffic is busy",
                            step2,
                            "+$extraMin min only",
                            "Worth the extra $extraMin min",
                        )
                    }
                    DriveMood.CALM ->
                        listOf(
                            "Checked traffic",
                            "Found a better fit",
                            "Minimal tradeoff",
                            "Worth considering",
                        )
                }
            }
            null ->
                when (mood) {
                    DriveMood.HEAVY ->
                        listOf(
                            "Congestion on route",
                            "Compared options",
                            "Best available now",
                            "Expect delays",
                        )
                    DriveMood.BUSY ->
                        listOf(
                            "Traffic is busy",
                            "Reviewed alternatives",
                            "This route fits",
                            "Worth considering",
                        )
                    DriveMood.CALM ->
                        listOf(
                            "Checked traffic",
                            "Conditions look good",
                            "Route is ready",
                            "You're good to go",
                        )
                }
        }
    }

    private fun padStory(steps: List<String>): List<String> {
        if (steps.size >= 4) return steps.take(4)
        return steps
    }

    private fun etaDeltaMetrics(comparison: ComparisonFacts?): Pair<String?, String?> {
        if (comparison == null) return null to null
        if (comparison.marshioSlowerSeconds in 60..TIME_TRADEOFF_SECONDS) {
            return "+${formatMinutesRounded(comparison.marshioSlowerSeconds)}" to "min"
        }
        return null to null
    }

    private fun savingsMetrics(comparison: ComparisonFacts?): Pair<String?, String?> {
        if (comparison == null) return null to null
        if (comparison.salikSavedAed >= SALIK_WIN_AED) {
            return "Save ${comparison.salikSavedAed}" to "AED"
        }
        return null to null
    }

    private fun routeEndpointLabels(routeIdentityTitle: String): Pair<String, String> {
        val arrow = "→"
        if (routeIdentityTitle.contains(arrow)) {
            val parts = routeIdentityTitle.split(arrow, limit = 2)
            val end = parts.getOrNull(1)?.trim().orEmpty()
            if (end.isNotBlank()) {
                val shortEnd =
                    end.split(',', '·').firstOrNull()?.trim().orEmpty().ifBlank { "Destination" }
                return "Start" to shortEnd
            }
        }
        return "Start" to "Destination"
    }

    private fun formatMinutesRounded(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        return minutes.coerceAtLeast(1).toString()
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
