package com.clearroad.app.domain

object RouteDecisionEngine {

    val sampleRoutes: List<RouteOption> = listOf(
        RouteOption(
            id = "e11_szr",
            name = "Sheikh Zayed Road (E11)",
            durationMin = 62,
            distanceKm = 118.0,
            tollAed = 28.0,
            salikGates = 5,
            passesAbuDhabi = true,
            parkingMayBePaid = false,
        ),
        RouteOption(
            id = "e611_emirates",
            name = "Emirates Road (E611)",
            durationMin = 78,
            distanceKm = 125.0,
            tollAed = 0.0,
            salikGates = 0,
            passesAbuDhabi = true,
            parkingMayBePaid = false,
        ),
        RouteOption(
            id = "e311_dubai_al_ain",
            name = "Dubai–Al Ain Road (E66) via sweat route",
            durationMin = 71,
            distanceKm = 122.0,
            tollAed = 8.0,
            salikGates = 2,
            passesAbuDhabi = false,
            parkingMayBePaid = true,
        ),
    )

    fun choose(routes: List<RouteOption>, mode: PreferenceMode): DecisionResult {
        require(routes.isNotEmpty()) { "routes must not be empty" }

        val best = when (mode) {
            PreferenceMode.FASTEST ->
                routes.minWith(compareBy({ it.durationMin }, { it.id }))
            PreferenceMode.NO_TOLLS -> {
                val minToll = routes.minOfOrNull { it.tollAed } ?: 0.0

                routes
                    .filter { it.tollAed == minToll }
                    .minBy { it.durationMin }
            }
            PreferenceMode.CALM ->
                routes.minWith(
                    compareBy(
                        { it.salikGates },
                        { it.tollAed },
                        { it.durationMin },
                        { it.id },
                    ),
                )
        }

        val choice = buildChoice(best, mode)
        val why = buildWhy(best, mode)
        val tip = buildTip(best, mode)

        return DecisionResult(
            bestRoute = best,
            mode = mode,
            choice = choice,
            why = why,
            tip = tip,
            tripCost = null,
        )
    }

    private fun buildChoice(route: RouteOption, mode: PreferenceMode): String {
        val via = friendlyVia(route)
        return when (mode) {
            PreferenceMode.FASTEST -> "Best route via $via"
            PreferenceMode.NO_TOLLS -> "Easiest on tolls via $via"
            PreferenceMode.CALM -> "Calmer drive via $via"
        }
    }

    private fun friendlyVia(route: RouteOption): String =
        route.name.substringBefore("(").trim()

    private fun approximateTime(minutes: Int): String {
        val rounded = ((minutes + 5) / 10) * 10
        if (rounded < 60) return "About $rounded minutes"
        val h = rounded / 60
        val m = rounded % 60
        if (m == 0) return if (h == 1) "About an hour" else "About $h hours"
        return if (h == 1) "About 1 hr $m min" else "About ${h} hr $m min"
    }

    private fun buildWhy(route: RouteOption, mode: PreferenceMode): String {
        val time = approximateTime(route.durationMin)
        return when (mode) {
            PreferenceMode.FASTEST -> buildWhyFastest(route, time)
            PreferenceMode.NO_TOLLS -> buildWhyNoTolls(route, time)
            PreferenceMode.CALM -> buildWhyCalm(route, time)
        }
    }

    private fun buildWhyFastest(route: RouteOption, time: String): String =
        when {
            route.tollAed == 0.0 ->
                "$time, no tolls — great when you need to shave minutes."
            route.salikGates >= 4 ->
                "$time, fastest option — expect several toll gates."
            else ->
                "$time, quickest here with some tolls along the way."
        }

    private fun buildWhyNoTolls(route: RouteOption, time: String): String =
        when {
            route.tollAed == 0.0 ->
                "$time, no tolls — kinder on the wallet."
            else ->
                "Least toll spend among these — still expect a small Salik bite."
        }

    private fun buildWhyCalm(route: RouteOption, time: String): String =
        when {
            route.salikGates == 0 ->
                "$time, skips Salik — usually feels steadier."
            route.salikGates <= 2 ->
                "Slightly longer, lighter toll hops than the big motorways."
            else ->
                "$time, fewer merges than the busiest sprint — still some tolls."
        }

    private fun buildTip(route: RouteOption, mode: PreferenceMode): String {
        if (route.passesAbuDhabi) {
            return "Sort DARB before driving into Abu Dhabi."
        }
        if (route.parkingMayBePaid) {
            return "Allow a little extra for paid parking at the end."
        }
        return when (mode) {
            PreferenceMode.FASTEST ->
                if (route.tollAed > 0.0 || route.salikGates > 0) {
                    "Keep Salik topped up before using toll roads."
                } else {
                    "Avoid Dubai rush hour around 5–7 pm."
                }
            PreferenceMode.NO_TOLLS -> "Avoid Dubai rush hour around 5–7 pm."
            PreferenceMode.CALM -> "Good choice if you want a calmer drive."
        }
    }
}
