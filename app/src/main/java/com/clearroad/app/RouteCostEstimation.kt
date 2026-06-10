package com.clearroad.app

import kotlin.math.roundToInt

private const val UAE_FUEL_PRICE_PER_LITER = 2.8
private const val AVERAGE_CAR_KM_PER_LITER = 12.0

internal fun estimateFuelCostAed(distanceKm: Double): Int {
    val litersUsed = distanceKm / AVERAGE_CAR_KM_PER_LITER
    val fuelCost = litersUsed * UAE_FUEL_PRICE_PER_LITER
    return fuelCost.roundToInt()
}

internal fun estimateTotalRouteCostAed(
    tollAed: Int,
    fuelAed: Int,
): Int =
    tollAed + fuelAed

internal fun calculateAedPerMinute(
    totalCostAed: Double,
    durationMinutes: Int,
): Double =
    if (durationMinutes <= 0) totalCostAed
    else totalCostAed / durationMinutes
