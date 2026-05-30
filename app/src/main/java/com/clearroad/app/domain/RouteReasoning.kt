package com.clearroad.app.domain

import kotlin.math.roundToInt

/**
 * Lightweight reasoning copy for Clear Road decisions.
 *
 * Only [ReasoningLayer.CORE] is active. Other layers are reserved so future
 * predictive, hyperlocal, emotional, and comparative copy can plug in without
 * architecture changes.
 */
enum class ReasoningLayer {
    CORE,
    /** Reserved: predictive ETA / time-window reasoning */
    PREDICTIVE_TIME,
    /** Reserved: Marina, SZR, Hessa-style local cues */
    HYPERLOCAL,
    /** Reserved: comfort / stress tone */
    EMOTIONAL,
    /** Reserved: vs-other-routes explanations */
    COMPARATIVE,
}

data class RouteReasoningContext(
    val mode: PreferenceMode,
    val personality: String = "",
    val routeIndex: Int = 0,
    val tollAed: Int? = null,
    val directionsStatus: String? = null,
    val recommendedRouteIndex: Int = 0,
    val routeCount: Int = 1,
    val activeLayers: Set<ReasoningLayer> = setOf(ReasoningLayer.CORE),
)

data class DecisionCopy(
    val choice: String,
    val why: String,
    val tip: String,
)

object RouteReasoning {

    /**
     * Trip at a glance copy — one voice per mode (time / Salik / comfort).
     * Wording only; uses toll fact from the opened route.
     */
    fun tripAtAGlanceLines(
        mode: PreferenceMode,
        tollAed: Int,
    ): Pair<String, String?> =
        when (mode) {
            PreferenceMode.FASTEST ->
                if (tollAed == 0) {
                    Pair("Time is the main factor.", "No Salik on this route.")
                } else {
                    Pair("Time is the main factor.", "Salik $tollAed AED on this route.")
                }
            PreferenceMode.NO_TOLLS ->
                Pair("Salik kept low on this route.", "May add a few minutes.")
            PreferenceMode.CALM ->
                if (tollAed > 0) {
                    Pair("Extra time buys a smoother drive.", "Salik $tollAed AED on this route.")
                } else {
                    Pair("Extra time buys a smoother drive.", "No Salik on this route.")
                }
        }

    fun alignedExplanation(
        personality: String,
        mode: PreferenceMode,
    ): DecisionCopy? {
        val core =
            when (personality) {
                "Lowest toll route" ->
                    DecisionCopy(
                        choice = "Lowest toll route",
                        why = "Keeps Salik spending lighter on this list.",
                        tip = "Good when Salik needs to stay predictable.",
                    )
                "Faster urban stretch" ->
                    DecisionCopy(
                        choice = "Faster urban stretch",
                        why = "Fast city flow through this corridor.",
                        tip = "Top up Salik before you roll.",
                    )
                "Dubai corridor" ->
                    DecisionCopy(
                        choice = "Dubai corridor",
                        why = "Better pace through central Dubai.",
                        tip = "When minutes matter most.",
                    )
                "Smoother city approach" ->
                    if (mode == PreferenceMode.CALM) {
                        DecisionCopy(
                            choice = "Smoother city approach",
                            why = "Smoother merge rhythm for evening traffic.",
                            tip = "Fine when extra minutes buy calm.",
                        )
                    } else {
                        DecisionCopy(
                            choice = "Smoother city approach",
                            why = "Gentler flow than the quickest Salik-heavy cut.",
                            tip = "Check Salik before you go.",
                        )
                    }
                "Steadier corridor leg" ->
                    if (mode == PreferenceMode.CALM) {
                        DecisionCopy(
                            choice = "Steadier corridor leg",
                            why = "Steadier motorway rhythm with less lane pressure.",
                            tip = "When calm beats rushing.",
                        )
                    } else {
                        DecisionCopy(
                            choice = "Steadier corridor leg",
                            why = "Steadier motorway rhythm with lighter Salik.",
                            tip = "When you want pace without heavy gates.",
                        )
                    }
                "Easier traffic stretch" ->
                    DecisionCopy(
                        choice = "Easier traffic stretch",
                        why = "Less aggressive lane pressure on this leg.",
                        tip = "When you are not chasing every minute.",
                    )
                "Smoother UAE leg" ->
                    DecisionCopy(
                        choice = "Smoother UAE leg",
                        why = "Gentler highway flow through busy areas.",
                        tip = "When calm beats rushing.",
                    )
                "More Salik ahead" ->
                    if (mode == PreferenceMode.CALM) {
                        DecisionCopy(
                            choice = "More Salik ahead",
                            why = "Gentler pace with less lane pressure on this leg.",
                            tip = "Fine when calm beats rushing.",
                        )
                    } else if (mode == PreferenceMode.NO_TOLLS) {
                        DecisionCopy(
                            choice = "More Salik ahead",
                            why = "Heavier toll pressure than lighter picks here.",
                            tip = "Check Salik before you head out.",
                        )
                    } else {
                        DecisionCopy(
                            choice = "More Salik ahead",
                            why = "Quickest movement through a Salik-heavy line.",
                            tip = "Check Salik before you head out.",
                        )
                    }
                "Salik-saving leg" ->
                    DecisionCopy(
                        choice = "Salik-saving leg",
                        why = "Avoids heavier toll pressure on this list.",
                        tip = "When savings beat shaving minutes.",
                    )
                "Fast city run" ->
                    DecisionCopy(
                        choice = "Fast city run",
                        why = "Fast city flow through this corridor.",
                        tip = "Pad a little time if rush hour is building.",
                    )
                "Main motorway stretch" ->
                    DecisionCopy(
                        choice = "Main motorway stretch",
                        why = "Strong UAE motorway pace on this pick.",
                        tip = "Pad extra time at rush hour.",
                    )
                "Higher toll pick" ->
                    DecisionCopy(
                        choice = "Higher toll pick",
                        why = "More Salik than the lighter picks here.",
                        tip = "When time matters more than cost.",
                    )
                "Lower Salik route" ->
                    if (mode == PreferenceMode.CALM) {
                        DecisionCopy(
                            choice = "Lower Salik route",
                            why = "Smoother merge rhythm on this leg.",
                            tip = "Fine when extra minutes buy calm.",
                        )
                    } else {
                        DecisionCopy(
                            choice = "Lower Salik route",
                            why = "Keeps Salik spending lighter.",
                            tip = "Still check exits on your map.",
                        )
                    }
                "Budget-friendly drive" ->
                    DecisionCopy(
                        choice = "Budget-friendly drive",
                        why = "More predictable Salik cost between these picks.",
                        tip = "Fine for everyday UAE runs.",
                    )
                "Toll estimate" -> null
                else -> null
            } ?: return null

        return mergeLayers(core, RouteReasoningContext(mode = mode, personality = personality))
    }

    fun manualWhy(context: RouteReasoningContext): String {
        val level = context.tollAed?.let(::tollLevel)
        val core =
            when (context.mode) {
                PreferenceMode.FASTEST ->
                    when {
                        level == "high" ->
                            "Quickest movement even with heavier Salik."
                        context.routeIndex == 0 ->
                            "Fast city flow through this corridor."
                        context.routeIndex == 1 ->
                            "Close to quickest — better pace through town."
                        else ->
                            "Longer leg — lighter pressure than the fastest cuts."
                    }
                PreferenceMode.NO_TOLLS ->
                    when {
                        level == "none" ->
                            "Keeps Salik spending lighter on this list."
                        level == "low" ->
                            "More predictable Salik cost among these."
                        context.routeIndex == 0 ->
                            "Avoids heavier toll pressure compared with others."
                        context.routeIndex == 1 ->
                            "Balances Salik cost and time sensibly."
                        else ->
                            "Heavier Salik exposure than the lighter options."
                    }
                PreferenceMode.CALM ->
                    "Smoother merge rhythm for evening traffic."
            }
        return layerLine(ReasoningLayer.CORE, context) ?: core
    }

    fun recommendedNuance(context: RouteReasoningContext): String {
        if (context.directionsStatus != "OK") {
            return when (context.mode) {
                PreferenceMode.FASTEST -> "Timing unclear until routes load."
                PreferenceMode.NO_TOLLS -> "Salik cost unclear until routes load."
                PreferenceMode.CALM -> "Pace unclear until routes load."
            }
        }
        return when (context.mode) {
            PreferenceMode.FASTEST -> "Best arrival time among available routes."
            PreferenceMode.NO_TOLLS -> "Good balance between cost and travel time."
            PreferenceMode.CALM -> "Designed for a more relaxed drive."
        }
    }

    /**
     * Human-readable recommendation line for Route Details and Recommended badge.
     * Uses existing route metrics only — no scoring or selection logic.
     */
    fun humanRecommendationExplanation(
        mode: PreferenceMode,
        recommendedDurationSeconds: Int,
        recommendedTollAed: Int,
        nextAlternativeDurationSeconds: Int?,
        fastestDurationSeconds: Int,
        highConfidence: Boolean,
        compact: Boolean = false,
    ): String =
        when (mode) {
            PreferenceMode.FASTEST -> {
                if (highConfidence && nextAlternativeDurationSeconds != null) {
                    val savedMinutes =
                        ((nextAlternativeDurationSeconds - recommendedDurationSeconds) / 60.0)
                            .roundToInt()
                    when {
                        savedMinutes >= 2 ->
                            "Saves about $savedMinutes min compared with the next option."
                        savedMinutes == 1 ->
                            "Saves a minute compared with the next option."
                        else -> "Best arrival time among available routes."
                    }
                } else if (
                    fastestDurationSeconds > 0 &&
                    recommendedDurationSeconds <= fastestDurationSeconds + 60
                ) {
                    "Best arrival time among available routes."
                } else {
                    "Best arrival time among available routes."
                }
            }
            PreferenceMode.NO_TOLLS ->
                when {
                    recommendedTollAed == 0 -> "Avoids Salik charges on this trip."
                    highConfidence -> "Avoids Salik charges on this trip."
                    else -> "Good balance between cost and travel time."
                }
            PreferenceMode.CALM -> {
                val extraSeconds =
                    if (fastestDurationSeconds > 0) {
                        recommendedDurationSeconds - fastestDurationSeconds
                    } else {
                        0
                    }
                when {
                    extraSeconds >= 300 ->
                        if (compact) {
                            "Slightly longer — smoother drive."
                        } else {
                            "Slightly longer, but designed for a smoother drive."
                        }
                    extraSeconds >= 60 -> "Designed for a more relaxed drive."
                    else -> "Designed for a more relaxed drive."
                }
            }
        }

    /**
     * Short tradeoff line for Route Details — what the driver gives up with this mode.
     * Wording only; no scoring or selection logic.
     */
    fun routeTradeoffExplanation(mode: PreferenceMode): String =
        when (mode) {
            PreferenceMode.FASTEST -> "Alternative routes take longer."
            PreferenceMode.NO_TOLLS -> "Other routes may increase Salik spending."
            PreferenceMode.CALM -> "Faster routes may feel busier."
        }

    fun engineWhy(route: RouteOption, mode: PreferenceMode): String {
        val time = approximateTime(route.durationMin)
        val core =
            when (mode) {
                PreferenceMode.FASTEST -> buildWhyFastest(route, time)
                PreferenceMode.NO_TOLLS -> buildWhyNoTolls(route, time)
                PreferenceMode.CALM ->
                    "Gentler highway flow through busy areas."
            }
        return layerLine(
            ReasoningLayer.CORE,
            RouteReasoningContext(mode = mode),
        ) ?: core
    }

    /** Future layers append here; returns [base] unchanged while only CORE is active. */
    private fun mergeLayers(
        base: DecisionCopy,
        context: RouteReasoningContext,
    ): DecisionCopy {
        var why = base.why
        for (layer in ReasoningLayer.entries) {
            if (layer == ReasoningLayer.CORE || layer !in context.activeLayers) continue
            layerLine(layer, context)?.let { why = it }
        }
        return base.copy(why = why)
    }

    /** Hook for future reasoning layers — inactive until enabled in [RouteReasoningContext]. */
    @Suppress("UNUSED_PARAMETER")
    private fun layerLine(
        layer: ReasoningLayer,
        context: RouteReasoningContext,
    ): String? =
        when (layer) {
            ReasoningLayer.CORE -> null
            ReasoningLayer.PREDICTIVE_TIME -> null
            ReasoningLayer.HYPERLOCAL -> null
            ReasoningLayer.EMOTIONAL -> null
            ReasoningLayer.COMPARATIVE -> null
        }

    private fun tollLevel(tollAed: Int): String =
        when {
            tollAed == 0 -> "none"
            tollAed in 1..8 -> "low"
            tollAed in 9..20 -> "medium"
            else -> "high"
        }

    private fun approximateTime(minutes: Int): String {
        val rounded = ((minutes + 5) / 10) * 10
        if (rounded < 60) return "About $rounded minutes"
        val h = rounded / 60
        val m = rounded % 60
        if (m == 0) return if (h == 1) "About an hour" else "About $h hours"
        return if (h == 1) "About 1 hr $m min" else "About ${h} hr $m min"
    }

    private fun buildWhyFastest(route: RouteOption, time: String): String =
        when {
            route.tollAed == 0.0 ->
                "$time — fast flow with lighter Salik."
            route.salikGates >= 4 ->
                "$time — quickest here through a Salik-heavy run."
            else ->
                "$time — quickest movement with some Salik along the way."
        }

    private fun buildWhyNoTolls(route: RouteOption, time: String): String =
        when {
            route.tollAed == 0.0 ->
                "$time — keeps Salik spending lighter."
            else ->
                "$time — more predictable Salik cost on this pick."
        }
}
