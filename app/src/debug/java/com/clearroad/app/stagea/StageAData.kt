package com.clearroad.app.stagea

object StageACaseCatalog {
    val allCases: Map<String, StageACaseSpec> = listOf(
        StageACaseSpec("A-F1", "FASTEST", "RECOMMEND", "Route A"),
        StageACaseSpec("A-F2", "FASTEST", "EQUIVALENT", null),
        StageACaseSpec("A-F3", "FASTEST", "INSUFFICIENT_DATA", null),
        StageACaseSpec("A-F4", "FASTEST", "RECOMMEND", "Route A"),
        StageACaseSpec("A-N1", "NO_TOLLS", "RECOMMEND", "Route B"),
        StageACaseSpec("A-N2", "NO_TOLLS", "RECOMMEND", "Route A"),
        StageACaseSpec("A-N3", "NO_TOLLS", "INSUFFICIENT_DATA", null),
        StageACaseSpec("A-N4", "NO_TOLLS", "EQUIVALENT", null),
    ).associateBy { it.caseId }

    fun spec(caseId: String): StageACaseSpec =
        allCases[caseId] ?: error("Unknown case_id: $caseId")
}

object StageAStimuli {
    private val baseline: Map<String, String> = mapOf(
        "A-F1" to """
Mode: FASTEST

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: 36 minutes
Toll: AED 8
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F2" to """
Mode: FASTEST

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F3" to """
Mode: FASTEST

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: unavailable
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F4" to """
Mode: FASTEST

Route A
Arrival: 30 minutes
Toll: unavailable
Corridor: E11 via D86

Route B
Arrival: 35 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N1" to """
Mode: NO_TOLLS

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: 34 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N2" to """
Mode: NO_TOLLS

Route A
Arrival: 30 minutes
Toll: no toll
Corridor: E11 via D86

Route B
Arrival: 36 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N3" to """
Mode: NO_TOLLS

Route A
Arrival: 30 minutes
Toll: unavailable
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N4" to """
Mode: NO_TOLLS

Route A
Arrival: 30 minutes
Toll: no toll
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
    )

    private val treatment: Map<String, String> = mapOf(
        "A-F1" to """
Mode: FASTEST

Take Route A
6 min faster
Same toll

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F2" to """
Mode: FASTEST

Equivalent for the selected FASTEST mode
No meaningful ETA difference for FASTEST
Route B costs AED 8 less
No FASTEST winner assigned

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F3" to """
Mode: FASTEST

Not enough reliable information
ETA information is incomplete
No recommendation assigned

Route A
Arrival: 30 minutes
Toll: AED 8
Corridor: E11 via D86

Route B
Arrival: unavailable
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-F4" to """
Mode: FASTEST

Take Route A
5 min faster
Toll comparison unavailable

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N1" to """
Mode: NO_TOLLS

Take Route B
AED 8 less
4 min longer

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N2" to """
Mode: NO_TOLLS

Take Route A
Same toll
6 min faster

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N3" to """
Mode: NO_TOLLS

Not enough reliable information
Toll information is incomplete
No recommendation assigned

Route A
Arrival: 30 minutes
Toll: unavailable
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
        "A-N4" to """
Mode: NO_TOLLS

Routes are effectively equivalent for NO_TOLLS
Same toll
No meaningful ETA difference
Choose either

Route A
Arrival: 30 minutes
Toll: no toll
Corridor: E11 via D86

Route B
Arrival: 32 minutes
Toll: no toll
Corridor: E311 via E44

Choose Route A, Route B, or say that you cannot decide from the information shown.
        """.trimIndent(),
    )

    fun text(caseId: String, condition: String): String {
        val map = if (condition == "Baseline") baseline else treatment
        return map[caseId] ?: error("Missing stimulus for $caseId / $condition")
    }
}

object StageAAssignment {
    private val g1Order = listOf(
        "A-F1" to "Baseline",
        "A-F3" to "Treatment",
        "A-F2" to "Baseline",
        "A-F4" to "Treatment",
        "A-N1" to "Baseline",
        "A-N3" to "Treatment",
        "A-N2" to "Baseline",
        "A-N4" to "Treatment",
    )

    private val g2Order = listOf(
        "A-F1" to "Treatment",
        "A-F3" to "Baseline",
        "A-F2" to "Treatment",
        "A-F4" to "Baseline",
        "A-N1" to "Treatment",
        "A-N3" to "Baseline",
        "A-N2" to "Treatment",
        "A-N4" to "Baseline",
    )

    fun trials(group: AssignmentGroup): List<StageATrialAssignment> {
        val order = when (group) {
            AssignmentGroup.G1 -> g1Order
            AssignmentGroup.G2 -> g2Order
        }
        return order.mapIndexed { index, (caseId, condition) ->
            StageATrialAssignment(
                trialOrder = index + 1,
                caseId = caseId,
                condition = condition,
            )
        }
    }
}

object StageAModeConsistent {
    fun compute(expectedOutcome: String, expectedWinner: String?, selectedRouteId: String?): String {
        if (selectedRouteId.isNullOrEmpty()) return ""
        return when (expectedOutcome) {
            "RECOMMEND" -> when {
                selectedRouteId == expectedWinner -> "true"
                else -> "false"
            }
            "EQUIVALENT" -> when (selectedRouteId) {
                "Route A", "Route B" -> "true"
                else -> ""
            }
            "INSUFFICIENT_DATA" -> ""
            else -> ""
        }
    }
}
