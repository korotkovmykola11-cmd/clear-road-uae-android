package com.clearroad.app.benchmark

/**
 * Stage 0A — frozen UAE origin/destination benchmark case (fixture metadata only).
 */
data class UaeRouteBenchmarkCase(
    val caseId: String,
    val originLabel: String,
    val destinationLabel: String,
    val originLat: Double,
    val originLng: Double,
    val destinationLat: Double,
    val destinationLng: Double,
    val expectedMajorCorridors: List<String>,
    val tags: List<String>,
    val notes: String,
)
