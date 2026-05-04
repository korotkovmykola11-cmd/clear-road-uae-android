package com.clearroad.app.domain

data class TripCost(
    val fuelLiters: Double,
    val fuelAedLow: Double,
    val fuelAedHigh: Double,
    val tollAed: Double,
    val totalAedLow: Double,
    val totalAedHigh: Double,
)
