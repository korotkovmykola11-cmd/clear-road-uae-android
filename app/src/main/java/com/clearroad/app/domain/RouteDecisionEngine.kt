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
            PreferenceMode.NO_TOLLS ->
                routes.minWith(compareBy({ it.tollAed }, { it.durationMin }, { it.id }))
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

        val choice = when (mode) {
            PreferenceMode.FASTEST -> "Best route for time: ${best.name}"
            PreferenceMode.NO_TOLLS -> "Best route to limit tolls: ${best.name}"
            PreferenceMode.CALM -> "Calmer drive: ${best.name}"
        }

        val why = buildWhy(best, mode)
        val tip = buildTip(best)

        return DecisionResult(
            bestRoute = best,
            mode = mode,
            choice = choice,
            why = why,
            tip = tip,
            tripCost = null,
        )
    }

    private fun buildWhy(route: RouteOption, mode: PreferenceMode): String {
        val parts = mutableListOf<String>()
        parts += "${route.durationMin} minutes, ${route.distanceKm} km."
        when (mode) {
            PreferenceMode.FASTEST -> {
                if (route.tollAed > 0.0) {
                    parts += "Tolls about AED ${route.tollAed.toInt()}."
                } else {
                    parts += "No toll line items on this option."
                }
            }
            PreferenceMode.NO_TOLLS -> {
                parts += if (route.tollAed == 0.0) {
                    "Keeps toll spend at zero."
                } else {
                    "Lowest toll among choices at AED ${String.format("%.0f", route.tollAed)}."
                }
            }
            PreferenceMode.CALM -> {
                parts += "${route.salikGates} Salik gate(s), tolls about AED ${route.tollAed.toInt()}."
            }
        }
        return parts.joinToString(" ")
    }

    private fun buildTip(route: RouteOption): String {
        val hints = mutableListOf<String>()
        hints += "Avoid Dubai rush peaks around 17:00–19:00 when you can."
        if (route.passesAbuDhabi) {
            hints += "Carry your Abu Dhabi toll/DARB registration if you cross into the emirate."
        }
        if (route.parkingMayBePaid) {
            hints += "Budget a little extra for paid parking at the destination."
        }
        return hints.joinToString(" ")
    }
}
