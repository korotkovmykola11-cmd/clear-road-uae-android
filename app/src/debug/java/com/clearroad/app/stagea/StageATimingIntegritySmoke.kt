package com.clearroad.app.stagea

/**
 * Offline timing-integrity checks for owner verification (not participant data).
 */
object StageATimingIntegritySmoke {
    data class Result(val name: String, val passed: Boolean, val detail: String)

    fun runAll(): List<Result> = listOf(
        check1DraftCreationDoesNotStartTiming(),
        check2ChoiceBeforeStartRejected(),
        check3ExplicitStartRecordsMonotonicStartTime(),
        check4SecondStartRejected(),
        check5ChoiceAfterStartRecordsElapsedTime(),
        check6NextTrialRemainsNotStarted(),
        check7InterruptionInvalidatesTiming(),
        check8RestorationOfRunningTrialInvalidatesTiming(),
        check9CompletedChoiceUnchangedByRestoration(),
        check10CsvHeaderRemains23Fields(),
    )

    private fun check1DraftCreationDoesNotStartTiming(): Result {
        val state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        val draft = state.currentDraft ?: return fail(1, "missing draft")
        if (draft.timingState != TrialTimingState.NOT_STARTED || draft.timerStartElapsedMs != 0L) {
            return fail(1, "draft started timing automatically")
        }
        return pass(1, "createDraft leaves NOT_STARTED with timerStartElapsedMs=0")
    }

    private fun check2ChoiceBeforeStartRejected(): Result {
        val state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        val after = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A)
        val draft = after.currentDraft
        if (after.phase != SessionPhase.TRIAL_STIMULUS ||
            draft?.selectedRouteId != null ||
            draft?.timingState != TrialTimingState.NOT_STARTED
        ) {
            return fail(2, "choice before start was accepted")
        }
        return pass(2, "confirmChoice rejected before timed start")
    }

    private fun check3ExplicitStartRecordsMonotonicStartTime(): Result {
        val state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        val started = StageASessionLogic.startTimedChoice(state)
        val draft = started.currentDraft ?: return fail(3, "missing draft")
        if (draft.timingState != TrialTimingState.RUNNING || draft.timerStartElapsedMs <= 0L) {
            return fail(3, "start event did not record monotonic start time")
        }
        return pass(3, "startTimedChoice sets RUNNING and elapsed start time")
    }

    private fun check4SecondStartRejected(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        val firstStart = state.currentDraft?.timerStartElapsedMs ?: return fail(4, "missing first start")
        state = StageASessionLogic.startTimedChoice(state)
        val draft = state.currentDraft ?: return fail(4, "missing draft")
        if (draft.timerStartElapsedMs != firstStart || draft.timingState != TrialTimingState.RUNNING) {
            return fail(4, "second start changed timing state")
        }
        return pass(4, "second startTimedChoice rejected")
    }

    private fun check5ChoiceAfterStartRecordsElapsedTime(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A)
        val draft = state.currentDraft ?: return fail(5, "missing draft")
        if (draft.timingState != TrialTimingState.STOPPED ||
            draft.decisionTimeMs == null ||
            draft.selectedRouteId != RouteChoice.ROUTE_A.csvValue
        ) {
            return fail(5, "choice after start did not record elapsed time")
        }
        return pass(5, "confirmChoice after start records decision_time_ms")
    }

    private fun check6NextTrialRemainsNotStarted(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = completeTrial(state, RouteChoice.ROUTE_A)
        val draft = state.currentDraft ?: return fail(6, "missing next draft")
        if (draft.timingState != TrialTimingState.NOT_STARTED || draft.timerStartElapsedMs != 0L) {
            return fail(6, "next trial auto-started timing")
        }
        return pass(6, "next trial remains NOT_STARTED")
    }

    private fun check7InterruptionInvalidatesTiming(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.markInterruption(state, "phone rang")
        val afterInterruption = state.currentDraft ?: return fail(7, "missing draft")
        if (afterInterruption.timingState != TrialTimingState.INVALID) {
            return fail(7, "RUNNING interruption left timingState=${afterInterruption.timingState}")
        }
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_B)
        val draft = state.currentDraft ?: return fail(7, "missing draft")
        val updatedDraft = draft.copy(
            confidence = 4,
            ease = 5,
            reasonCode = ReasonCode.LOWER_TOLL.csvValue,
        )
        state = StageASessionLogic.finishTrialAndAdvance(state.copy(currentDraft = updatedDraft))
        val record = state.completedRecords.single()
        if (!record.protocolDeviation || record.timingValid || record.decisionTimeMs.isNotEmpty()) {
            return fail(7, "interruption timing fields wrong")
        }
        return pass(7, "interruption clears valid timing")
    }

    private fun check8RestorationOfRunningTrialInvalidatesTiming(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.applyActivityRestorationPolicy(state)
        val draft = state.currentDraft ?: return fail(8, "missing draft")
        if (draft.timingState != TrialTimingState.INVALID ||
            draft.timingValid ||
            !draft.protocolDeviation ||
            draft.decisionTimeMs != null
        ) {
            return fail(8, "restoration policy wrong for RUNNING trial")
        }
        return pass(8, "RUNNING trial becomes INVALID on restoration")
    }

    private fun check9CompletedChoiceUnchangedByRestoration(): Result {
        var state = StageASessionLogic.startSession("DRY-OWNER-G1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A)
        val before = state.currentDraft?.selectedRouteId
        state = StageASessionLogic.applyActivityRestorationPolicy(state)
        val after = state.currentDraft?.selectedRouteId
        if (before != RouteChoice.ROUTE_A.csvValue || after != before) {
            return fail(9, "completed choice changed by restoration")
        }
        return pass(9, "STOPPED choice preserved across restoration policy")
    }

    private fun check10CsvHeaderRemains23Fields(): Result {
        val fields = StageACsvHeader.LINE.split(",")
        if (fields.size != 23) {
            return fail(10, "header field count ${fields.size}")
        }
        return pass(10, "CSV header remains 23 fields")
    }

    private fun completeTrial(state: StageASessionState, choice: RouteChoice): StageASessionState {
        var current = StageASessionLogic.startTimedChoice(state)
        current = StageASessionLogic.confirmChoice(current, choice)
        val draft = current.currentDraft ?: return current
        val updatedDraft = draft.copy(
            confidence = 5,
            ease = 5,
            reasonCode = ReasonCode.SHORTEST_ETA.csvValue,
        )
        return StageASessionLogic.finishTrialAndAdvance(current.copy(currentDraft = updatedDraft))
    }

    private fun pass(number: Int, detail: String) =
        Result("Timing $number", true, detail)

    private fun fail(number: Int, detail: String) =
        Result("Timing $number", false, detail)
}
