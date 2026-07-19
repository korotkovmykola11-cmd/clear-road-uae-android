package com.clearroad.app.stagea

/**
 * Offline programmatic smoke checks for owner verification (not participant data).
 */
object StageASessionLogicSmoke {
    data class Result(val name: String, val passed: Boolean, val detail: String)

    fun runAll(): List<Result> = listOf(
        smoke1FullG1(),
        smoke2FullG2(),
        smoke3Interruption(),
        smoke4EarlyFinish(),
        smoke5Exclusion(),
        smoke6CsvEscaping(),
    )

    private fun smoke1FullG1(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        val choices = listOf(
            RouteChoice.ROUTE_A,
            RouteChoice.ROUTE_B,
            RouteChoice.CANNOT_DECIDE,
            RouteChoice.ROUTE_A,
            RouteChoice.ROUTE_B,
            RouteChoice.CANNOT_DECIDE,
            RouteChoice.ROUTE_A,
            RouteChoice.ROUTE_B,
        )
        val expectedOrder = listOf(
            "A-F1" to "Baseline",
            "A-F3" to "Treatment",
            "A-F2" to "Baseline",
            "A-F4" to "Treatment",
            "A-N1" to "Baseline",
            "A-N3" to "Treatment",
            "A-N2" to "Baseline",
            "A-N4" to "Treatment",
        )
        choices.forEachIndexed { index, choice ->
            state = advanceTrial(state, choice)
            val record = state.completedRecords.getOrNull(index)
                ?: return Result("Smoke 1 — Full G1", false, "Missing row ${index + 1}")
            val (caseId, condition) = expectedOrder[index]
            if (record.caseId != caseId || record.condition != condition) {
                return Result("Smoke 1 — Full G1", false, "Wrong case at row ${index + 1}")
            }
        }
        if (state.completedRecords.size != 8) {
            return Result("Smoke 1 — Full G1", false, "Expected 8 rows, got ${state.completedRecords.size}")
        }
        state = StageASessionLogic.applyPostSession(state, "none", PostFormatHelped.NEUTRAL_ROWS)
        val postFlags = state.completedRecords.map { it.postSessionRecord }
        if (postFlags != List(8) { it == 7 }) {
            return Result("Smoke 1 — Full G1", false, "post_session_record wrong: $postFlags")
        }
        val csv = StageACsvWriter.buildCsv(state.completedRecords)
        val headerFields = csv.lines().first().split(",")
        if (headerFields.size != 23) {
            return Result("Smoke 1 — Full G1", false, "Header field count ${headerFields.size}")
        }
        return Result("Smoke 1 — Full G1", true, "8 rows, G1 order, post_session on row 8, CSV ok")
    }

    private fun smoke2FullG2(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G2", AssignmentGroup.G2)
        repeat(8) {
            state = advanceTrial(state, RouteChoice.ROUTE_A)
        }
        val expectedConditions = listOf(
            "Treatment", "Baseline", "Treatment", "Baseline",
            "Treatment", "Baseline", "Treatment", "Baseline",
        )
        val actual = state.completedRecords.map { it.condition }
        if (actual != expectedConditions) {
            return Result("Smoke 2 — Full G2", false, "Conditions: $actual")
        }
        state = StageASessionLogic.applyPostSession(state, "", PostFormatHelped.DECISION_COMPRESSION)
        val csv = runCatching { StageACsvWriter.buildCsv(state.completedRecords) }.getOrNull()
            ?: return Result("Smoke 2 — Full G2", false, "CSV build failed")
        if (csv.lines().size != 9) {
            return Result("Smoke 2 — Full G2", false, "Expected 9 lines (header+8)")
        }
        return Result("Smoke 2 — Full G2", true, "8 rows, G2 conditions, CSV ok")
    }

    private fun smoke3Interruption(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.markInterruption(state, "phone rang")
        if (state.currentDraft?.timingState != TrialTimingState.INVALID) {
            return Result("Smoke 3 — Interruption", false, "RUNNING interruption did not set INVALID")
        }
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_B)
        val draft = state.currentDraft ?: return Result("Smoke 3 — Interruption", false, "No draft")
        val updatedDraft = draft.copy(
            confidence = 4,
            ease = 5,
            reasonCode = ReasonCode.LOWER_TOLL.csvValue,
        )
        state = StageASessionLogic.finishTrialAndAdvance(state.copy(currentDraft = updatedDraft))
        val record = state.completedRecords.single()
        if (!record.protocolDeviation || record.timingValid || record.decisionTimeMs.isNotEmpty()) {
            return Result("Smoke 3 — Interruption", false, "Timing fields wrong")
        }
        if (record.excluded || record.selectedRouteId != "Route B" || record.confidence17 != "4") {
            return Result("Smoke 3 — Interruption", false, "Answers not retained")
        }
        return Result("Smoke 3 — Interruption", true, "protocol_deviation=true, timing cleared, answers kept")
    }

    private fun smoke4EarlyFinish(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        repeat(2) { state = advanceTrial(state, RouteChoice.ROUTE_A) }
        if (state.completedRecords.size != 2 || state.trialIndex != 2) {
            return Result("Smoke 4 — Early finish", false, "Expected 2 completed trials before trial 3")
        }
        state = StageASessionLogic.finishSessionEarly(state)
        if (state.completedRecords.size != 3) {
            return Result("Smoke 4 — Early finish", false, "Expected 3 rows, got ${state.completedRecords.size}")
        }
        state = StageASessionLogic.applyPostSession(state, "early", PostFormatHelped.NEITHER)
        val postFlags = state.completedRecords.map { it.postSessionRecord }
        if (postFlags != listOf(false, false, true)) {
            return Result("Smoke 4 — Early finish", false, "post_session flags: $postFlags")
        }
        val csv = StageACsvWriter.buildCsv(state.completedRecords)
        if (csv.lines().size != 4) {
            return Result("Smoke 4 — Early finish", false, "CSV line count ${csv.lines().size}")
        }
        return Result("Smoke 4 — Early finish", true, "3 rows only, post_session on row 3")
    }

    private fun smoke5Exclusion(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G2", AssignmentGroup.G2)
        state = StageASessionLogic.excludeCurrentTrial(state, ExclusionReason.WRONG_STIMULUS)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A)
        val draft = state.currentDraft ?: return Result("Smoke 5 — Exclusion", false, "No draft")
        val updatedDraft = draft.copy(
            confidence = 3,
            ease = 3,
            reasonCode = ReasonCode.OTHER.csvValue,
        )
        state = StageASessionLogic.finishTrialAndAdvance(state.copy(currentDraft = updatedDraft))
        val record = state.completedRecords.single()
        if (!record.excluded || record.exclusionReason != "wrong_stimulus") {
            return Result("Smoke 5 — Exclusion", false, "Exclusion fields wrong")
        }
        val csv = StageACsvWriter.buildCsv(state.completedRecords)
        if (!csv.contains("wrong_stimulus")) {
            return Result("Smoke 5 — Exclusion", false, "CSV missing exclusion_reason")
        }
        return Result("Smoke 5 — Exclusion", true, "excluded row retained in CSV")
    }

    private fun smoke6CsvEscaping(): Result {
        val passed = StageACsvSelfTest.verifyEscaping()
        return Result(
            "Smoke 6 — CSV escaping",
            passed,
            if (passed) "comma, quote, newline escaped; record count unchanged" else "escaping self-test failed",
        )
    }

    private fun advanceTrial(state: StageASessionState, choice: RouteChoice): StageASessionState {
        var s = StageASessionLogic.startTimedChoice(state)
        s = StageASessionLogic.confirmChoice(s, choice)
        val draft = s.currentDraft ?: return s
        val updatedDraft = draft.copy(
            confidence = 5,
            ease = 5,
            reasonCode = ReasonCode.SHORTEST_ETA.csvValue,
        )
        return StageASessionLogic.finishTrialAndAdvance(s.copy(currentDraft = updatedDraft))
    }
}
