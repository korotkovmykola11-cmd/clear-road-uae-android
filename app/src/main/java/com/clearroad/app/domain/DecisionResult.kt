package com.clearroad.app.domain

data class DecisionResult(
    val bestRoute: RouteOption,
    val mode: PreferenceMode,
    val choice: String,
    val why: String,
    val tip: String,
    val tripCost: TripCost?,
)
