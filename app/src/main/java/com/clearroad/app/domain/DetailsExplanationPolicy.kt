package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteTrafficDelayMetrics

/**
 * Route Details "Why this route" copy — delegates to [ModeExplanationPolicy] for core meaning.
 * Presentation only; does not affect route selection or scoring.
 */
internal object DetailsExplanationPolicy {

    data class DetailsWhyCopy(
        val title: String,
        val why: String,
    )

    fun recommendedRouteCopy(
        mode: PreferenceMode,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): DetailsWhyCopy {
        val home =
            ModeExplanationPolicy.homeCardCopy(
                mode = mode,
                recommended = recommended,
                routes = routes,
                recommendedIndex = recommendedIndex,
            )
        val fastestSeconds = routes.minOf { it.durationSeconds }
        return when (mode) {
            PreferenceMode.FASTEST ->
                fastestDetails(
                    home = home,
                    recommendedSeconds = recommended.durationSeconds,
                    routes = routes,
                    recommendedIndex = recommendedIndex,
                )
            PreferenceMode.NO_TOLLS ->
                saveAedDetails(
                    home = home,
                    recommended = recommended,
                    routes = routes,
                    fastestSeconds = fastestSeconds,
                )
            PreferenceMode.CALM ->
                smoothDetails(
                    home = home,
                    recommended = recommended,
                    routes = routes,
                    recommendedIndex = recommendedIndex,
                    recommendedSeconds = recommended.durationSeconds,
                    fastestSeconds = fastestSeconds,
                )
        }
    }

    private fun fastestDetails(
        home: ModeExplanationPolicy.HomeCardCopy,
        recommendedSeconds: Int,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
    ): DetailsWhyCopy {
        val nextAlternativeSeconds =
            routes.indices
                .filter { it != recommendedIndex }
                .minOfOrNull { routes[it].durationSeconds }
        val whyBody = buildString {
            append("MARSHIO chose the quickest practical option on this list.")
            val savedMinutes =
                nextAlternativeSeconds?.let {
                    minutesSaved(recommendedSeconds, it)
                }
            when {
                savedMinutes != null && savedMinutes >= 2 ->
                    append(" MARSHIO's pick would save ~$savedMinutes min compared with the next option.")
                savedMinutes == 1 ->
                    append(" MARSHIO's pick would save ~1 min compared with the next option.")
                else ->
                    append(" It is the quickest option among these routes.")
            }
        }
        return DetailsWhyCopy(
            title = detailsTitle(home.summary, PreferenceMode.FASTEST),
            why = whyBody,
        )
    }

    private fun saveAedDetails(
        home: ModeExplanationPolicy.HomeCardCopy,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        fastestSeconds: Int,
    ): DetailsWhyCopy {
        val whyBody = buildString {
            if (SalikPresentationPolicy.allRoutesTollFree(routes)) {
                append(
                    extraMinutesVersusFastest(
                        recommended.durationSeconds,
                        fastestSeconds,
                    ).trim(),
                )
            } else {
                append(home.narrative)
                append(extraMinutesVersusFastest(recommended.durationSeconds, fastestSeconds))
            }
        }
        return DetailsWhyCopy(
            title = detailsTitle(home.summary, PreferenceMode.NO_TOLLS),
            why = whyBody.trim(),
        )
    }

    private fun smoothDetails(
        home: ModeExplanationPolicy.HomeCardCopy,
        recommended: RealRouteDebugData,
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        recommendedSeconds: Int,
        fastestSeconds: Int,
    ): DetailsWhyCopy {
        if (SmoothDrivePresentationPolicy.matchesFastestRoute(routes, recommendedIndex)) {
            return DetailsWhyCopy(
                title = detailsTitle(home.summary, PreferenceMode.CALM),
                why = "",
            )
        }
        val delayMinutes = RouteTrafficDelayMetrics.delayMinutesRounded(recommended)
        val whyBody = buildString {
            append(home.narrative)
            if (!RouteTrafficDelayMetrics.isLowestDelayRatioAmong(recommended, routes)) {
                if (delayMinutes > 0) {
                    append(" About $delayMinutes min from traffic slowdowns.")
                } else {
                    append(" Little traffic slowdown right now.")
                }
            }
            append(extraMinutesVersusFastest(recommendedSeconds, fastestSeconds))
            if (recommendedSeconds > fastestSeconds) {
                append(" It may not be the quickest option on the list.")
            }
            append(" Based on traffic slowdowns, not Salik or fuel.")
        }
        return DetailsWhyCopy(
            title = detailsTitle(home.summary, PreferenceMode.CALM),
            why = whyBody.trim(),
        )
    }

    internal fun detailsTitle(
        homeSummary: String,
        mode: PreferenceMode,
    ): String {
        val trimmed = homeSummary.trim().trimEnd('.')
        return when (mode) {
            PreferenceMode.FASTEST -> "Quickest option"
            PreferenceMode.NO_TOLLS -> trimmed
            PreferenceMode.CALM ->
                when (trimmed) {
                    "Less affected by traffic slowdowns" -> "SMOOTH DRIVE pick"
                    "Smooth option matches fastest today" -> "Smooth option matches fastest"
                    else -> trimmed
                }
        }
    }

    private fun extraMinutesVersusFastest(
        recommendedSeconds: Int,
        fastestSeconds: Int,
    ): String {
        if (fastestSeconds <= 0 || recommendedSeconds <= fastestSeconds) return ""
        val extraMinutes = minutesSaved(fastestSeconds, recommendedSeconds)
        return when {
            extraMinutes >= 2 ->
                " It may take about $extraMinutes extra minutes versus the fastest route."
            extraMinutes == 1 ->
                " It may take about a minute extra versus the fastest route."
            else -> ""
        }
    }

    private fun minutesSaved(fasterSeconds: Int, slowerSeconds: Int): Int =
        ((slowerSeconds - fasterSeconds).coerceAtLeast(0) + 59) / 60
}
