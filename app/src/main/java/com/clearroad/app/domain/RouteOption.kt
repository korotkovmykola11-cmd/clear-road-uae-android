package com.clearroad.app.domain

data class RouteOption(
    val id: String,
    val name: String,
    val durationMin: Int,
    val distanceKm: Double,
    val tollAed: Double,
    val salikGates: Int,
    val passesAbuDhabi: Boolean,
    val parkingMayBePaid: Boolean,
)
