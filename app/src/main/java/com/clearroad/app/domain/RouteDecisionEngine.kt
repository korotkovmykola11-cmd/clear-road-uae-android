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
            PreferenceMode.CALM -> chooseCalmBalanced(routes)
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

    /** Drop shortest/longest by duration when 3+ routes; 2 routes pick rank-sum nearest balance. */
    private fun chooseCalmBalanced(routes: List<RouteOption>): RouteOption {
        require(routes.isNotEmpty())
        val sortedByDur = routes.sortedWith(compareBy({ it.durationMin }, { it.id }))
        return when (sortedByDur.size) {
            1 -> sortedByDur.first()
            2 -> chooseCalmTwoRoutes(routes)
            else -> {
                val trimmed = sortedByDur.drop(1).dropLast(1)
                trimmed[trimmed.size / 2]
            }
        }
    }

    private fun chooseCalmTwoRoutes(routes: List<RouteOption>): RouteOption {
        val durOrder = routes.sortedWith(compareBy({ it.durationMin }, { it.id }))
        val tollOrder = routes.sortedWith(compareBy({ it.tollAed }, { it.id }))
        fun durRank(r: RouteOption): Int = durOrder.indexOf(r)
        fun tollRank(r: RouteOption): Int = tollOrder.indexOf(r)
        val targetRankSum = 1.0
        return routes.minWith(
            compareBy<RouteOption> {
                kotlin.math.abs(durRank(it) + tollRank(it) - targetRankSum)
            }.thenBy { it.id },
        )
    }

    private fun buildChoice(route: RouteOption, mode: PreferenceMode): String =
        when (mode) {
            PreferenceMode.FASTEST -> "Best route via ${friendlyVia(route)}"
            PreferenceMode.NO_TOLLS -> "Easiest on tolls via ${friendlyVia(route)}"
            PreferenceMode.CALM -> "Balanced route"
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

    private fun buildWhy(route: RouteOption, mode: PreferenceMode): String =
        when (mode) {
            PreferenceMode.FASTEST -> buildWhyFastest(route, approximateTime(route.durationMin))
            PreferenceMode.NO_TOLLS -> buildWhyNoTolls(route, approximateTime(route.durationMin))
            PreferenceMode.CALM -> "Balances driving time and road cost."
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
            PreferenceMode.CALM -> "Good when you want a smoother overall drive."
        }
    }
}
