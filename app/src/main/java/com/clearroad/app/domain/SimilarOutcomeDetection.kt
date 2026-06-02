package com.clearroad.app.domain

import kotlin.math.abs
import kotlin.math.ceil

/**
 * Stage 30.4 — conservative V1 detection when route alternatives are effectively equivalent.
 * Stage 30.5 — context-aware Similar Outcome messaging (detection rules unchanged).
 * Guidance only; does not change route selection or scoring.
 */
object SimilarOutcomeDetection {

    const val MAX_TIME_DIFF_MINUTES = 2
    const val MAX_TIME_DIFF_SECONDS = MAX_TIME_DIFF_MINUTES * 60
    private const val SALIK_AED_PER_GATE = 4
    private const val EQUAL_TIME_TOLERANCE_SECONDS = 30

    data class SimilarOutcomeRouteInput(
        val durationSeconds: Int,
        val tollAed: Int,
    )

    data class SimilarOutcomeGuidance(
        val choice: String,
        val why: String,
    )

    private enum class SimilarOutcomeTradeoff {
        SAME_TOLL_EQUAL_TIME,
        SAME_TOLL_RECOMMENDED_FASTER,
        SAME_TOLL_ALTERNATIVE_FASTER,
        ALTERNATIVE_SAVES_SALIK,
    }

    fun detect(
        routes: List<SimilarOutcomeRouteInput>,
        recommendedIndex: Int,
        mode: PreferenceMode,
    ): SimilarOutcomeGuidance? {
        if (routes.size < 2) return null
        if (recommendedIndex !in routes.indices) return null

        val recommended = routes[recommendedIndex]
        if (!isReliable(recommended)) return null

        val alternativeIndex =
            routes.indices
                .filter { it != recommendedIndex && isReliable(routes[it]) }
                .minByOrNull { abs(routes[it].durationSeconds - recommended.durationSeconds) }
                ?: return null

        val alternative = routes[alternativeIndex]
        if (!isSimilarByTime(recommended, alternative)) return null
        if (!isTollDifferenceMeaningful(recommended, alternative)) return null

        return buildGuidance(
            mode = mode,
            recommended = recommended,
            alternative = alternative,
        )
    }

    internal fun estimateSalikGates(tollAed: Int): Int {
        if (tollAed <= 0) return 0
        return ceil(tollAed.toDouble() / SALIK_AED_PER_GATE).toInt().coerceAtLeast(1)
    }

    private fun isReliable(route: SimilarOutcomeRouteInput): Boolean =
        route.durationSeconds > 0

    private fun isSimilarByTime(
        recommended: SimilarOutcomeRouteInput,
        alternative: SimilarOutcomeRouteInput,
    ): Boolean =
        abs(alternative.durationSeconds - recommended.durationSeconds) <= MAX_TIME_DIFF_SECONDS

    /**
     * Returns true when toll difference is NOT meaningful enough to block Similar Outcome.
     */
    private fun isTollDifferenceMeaningful(
        recommended: SimilarOutcomeRouteInput,
        alternative: SimilarOutcomeRouteInput,
    ): Boolean {
        if (recommended.tollAed == alternative.tollAed) return true

        val recommendedGates = estimateSalikGates(recommended.tollAed)
        val alternativeGates = estimateSalikGates(alternative.tollAed)

        // Example D: alternative adds 2+ Salik gates with similar time — not equivalent.
        if (alternativeGates - recommendedGates >= 2) return false

        // Alternative saves Salik with only <= 2 minutes extra (Example B).
        if (
            alternative.tollAed < recommended.tollAed &&
            alternative.durationSeconds <= recommended.durationSeconds + MAX_TIME_DIFF_SECONDS
        ) {
            return true
        }

        // Same toll already handled; other toll gaps are not equivalent in V1.
        return false
    }

    private fun classifyTradeoff(
        recommended: SimilarOutcomeRouteInput,
        alternative: SimilarOutcomeRouteInput,
    ): SimilarOutcomeTradeoff {
        if (alternative.tollAed < recommended.tollAed) {
            return SimilarOutcomeTradeoff.ALTERNATIVE_SAVES_SALIK
        }

        val timeDeltaSeconds = alternative.durationSeconds - recommended.durationSeconds
        return when {
            abs(timeDeltaSeconds) <= EQUAL_TIME_TOLERANCE_SECONDS ->
                SimilarOutcomeTradeoff.SAME_TOLL_EQUAL_TIME
            timeDeltaSeconds > 0 ->
                SimilarOutcomeTradeoff.SAME_TOLL_RECOMMENDED_FASTER
            else ->
                SimilarOutcomeTradeoff.SAME_TOLL_ALTERNATIVE_FASTER
        }
    }

    private fun buildGuidance(
        mode: PreferenceMode,
        recommended: SimilarOutcomeRouteInput,
        alternative: SimilarOutcomeRouteInput,
    ): SimilarOutcomeGuidance {
        val tradeoff = classifyTradeoff(recommended, alternative)
        val why =
            when (mode) {
                PreferenceMode.FASTEST -> fastestSimilarWhy(tradeoff)
                PreferenceMode.NO_TOLLS -> saveAedSimilarWhy(tradeoff, recommended, alternative)
                PreferenceMode.CALM -> calmSimilarWhy(tradeoff)
            }

        return SimilarOutcomeGuidance(
            choice = "Similar outcome",
            why = why,
        )
    }

    private fun fastestSimilarWhy(tradeoff: SimilarOutcomeTradeoff): String =
        when (tradeoff) {
            SimilarOutcomeTradeoff.SAME_TOLL_RECOMMENDED_FASTER ->
                "Fastest arrives slightly earlier, but the difference is unlikely to matter."
            SimilarOutcomeTradeoff.SAME_TOLL_EQUAL_TIME,
            SimilarOutcomeTradeoff.SAME_TOLL_ALTERNATIVE_FASTER,
            SimilarOutcomeTradeoff.ALTERNATIVE_SAVES_SALIK,
            ->
                "Travel times are nearly identical. Choose whichever route feels more comfortable."
        }

    private fun saveAedSimilarWhy(
        tradeoff: SimilarOutcomeTradeoff,
        recommended: SimilarOutcomeRouteInput,
        alternative: SimilarOutcomeRouteInput,
    ): String =
        when (tradeoff) {
            SimilarOutcomeTradeoff.ALTERNATIVE_SAVES_SALIK -> {
                val extraMinutes =
                    extraMinutesIfSlower(
                        recommendedSeconds = recommended.durationSeconds,
                        alternativeSeconds = alternative.durationSeconds,
                    )
                when (extraMinutes) {
                    0 -> "Save AED avoids Salik with almost no time penalty."
                    1 -> "Save AED avoids Salik with only 1 extra minute."
                    else -> "You can save on tolls without meaningfully increasing travel time."
                }
            }
            SimilarOutcomeTradeoff.SAME_TOLL_EQUAL_TIME,
            SimilarOutcomeTradeoff.SAME_TOLL_RECOMMENDED_FASTER,
            SimilarOutcomeTradeoff.SAME_TOLL_ALTERNATIVE_FASTER,
            ->
                "Salik cost is nearly the same across these routes."
        }

    private fun calmSimilarWhy(tradeoff: SimilarOutcomeTradeoff): String =
        when (tradeoff) {
            SimilarOutcomeTradeoff.SAME_TOLL_EQUAL_TIME,
            SimilarOutcomeTradeoff.SAME_TOLL_RECOMMENDED_FASTER,
            ->
                "Smooth Drive offers a calmer route with almost identical arrival time."
            SimilarOutcomeTradeoff.SAME_TOLL_ALTERNATIVE_FASTER,
            SimilarOutcomeTradeoff.ALTERNATIVE_SAVES_SALIK,
            ->
                "Less traffic pressure, with little impact on arrival time."
        }

    private fun extraMinutesIfSlower(
        recommendedSeconds: Int,
        alternativeSeconds: Int,
    ): Int =
        ((alternativeSeconds - recommendedSeconds).coerceAtLeast(0) + 59) / 60
}
