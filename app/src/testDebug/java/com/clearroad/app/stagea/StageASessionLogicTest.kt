package com.clearroad.app.stagea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StageASessionLogicTest {
    @Test
    fun draftCreation_leavesTimingNotStarted() {
        val state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.NOT_STARTED, draft.timingState)
        assertEquals(0L, draft.timerStartElapsedMs)
    }

    @Test
    fun choiceBeforeTimingStart_isRejected() {
        val state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        val after = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A)
        assertEquals(SessionPhase.TRIAL_STIMULUS, after.phase)
        assertNull(after.currentDraft?.selectedRouteId)
        assertEquals(TrialTimingState.NOT_STARTED, after.currentDraft?.timingState)
    }

    @Test
    fun startTimedChoice_createsNewImmutableStateAndDraft() {
        val clock = FakeMonotonicClock(5000)
        val state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        val stateSnapshot = state
        val draftSnapshot = requireNotNull(state.currentDraft)

        val newState = StageASessionLogic.startTimedChoice(state, clock)

        assertEquals(stateSnapshot, state)
        assertEquals(TrialTimingState.NOT_STARTED, draftSnapshot.timingState)
        assertEquals(0L, draftSnapshot.timerStartElapsedMs)
        assertNotEquals(state, newState)
        assertTrue(newState.currentDraft !== draftSnapshot)
        assertEquals(TrialTimingState.RUNNING, newState.currentDraft?.timingState)
        assertEquals(5000L, newState.currentDraft?.timerStartElapsedMs)
    }

    @Test
    fun confirmChoice_doesNotMutateOriginalStateOrDraft() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        val stateSnapshot = state
        val draftSnapshot = requireNotNull(state.currentDraft)
        clock.advance(500)

        val newState = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A, clock)

        assertEquals(stateSnapshot, state)
        assertEquals(TrialTimingState.RUNNING, draftSnapshot.timingState)
        assertNull(draftSnapshot.selectedRouteId)
        assertTrue(newState.currentDraft !== draftSnapshot)
        assertEquals(TrialTimingState.STOPPED, newState.currentDraft?.timingState)
    }

    @Test
    fun markInterruption_doesNotMutateOriginalStateOrDraft() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        val stateSnapshot = state
        val draftSnapshot = requireNotNull(state.currentDraft)

        val newState = StageASessionLogic.markInterruption(state, "phone rang")

        assertEquals(stateSnapshot, state)
        assertEquals(TrialTimingState.RUNNING, draftSnapshot.timingState)
        assertTrue(newState.currentDraft !== draftSnapshot)
        assertEquals(TrialTimingState.INVALID, newState.currentDraft?.timingState)
    }

    @Test
    fun applyActivityRestorationPolicy_doesNotMutateOriginalStateOrDraft() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        val stateSnapshot = state
        val draftSnapshot = requireNotNull(state.currentDraft)

        val newState = StageASessionLogic.applyActivityRestorationPolicy(state)

        assertEquals(stateSnapshot, state)
        assertEquals(TrialTimingState.RUNNING, draftSnapshot.timingState)
        assertTrue(newState.currentDraft !== draftSnapshot)
        assertEquals(TrialTimingState.INVALID, newState.currentDraft?.timingState)
    }

    @Test
    fun secondStartTimedChoice_isRejected() {
        val clock = FakeMonotonicClock(5000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        val firstStart = requireNotNull(state.currentDraft).timerStartElapsedMs

        val afterSecond = StageASessionLogic.startTimedChoice(state, clock)

        assertEquals(firstStart, afterSecond.currentDraft?.timerStartElapsedMs)
        assertEquals(TrialTimingState.RUNNING, afterSecond.currentDraft?.timingState)
        assertEquals(state, afterSecond)
    }

    @Test
    fun fakeClock_excludesWaitBeforeStart_exactly3200ms() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        clock.advance(4000)
        state = StageASessionLogic.startTimedChoice(state, clock)
        clock.advance(3200)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A, clock)

        assertEquals(3200L, state.currentDraft?.decisionTimeMs)
        assertNotEquals(7200L, state.currentDraft?.decisionTimeMs)
    }

    @Test
    fun nextTrial_remainsNotStarted() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = completeTrial(state, RouteChoice.ROUTE_A, clock)
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.NOT_STARTED, draft.timingState)
        assertEquals(0L, draft.timerStartElapsedMs)
    }

    @Test
    fun runningInterruption_producesInvalid() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.markInterruption(state, "phone rang")
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.INVALID, draft.timingState)
        assertFalse(draft.timingValid)
        assertNull(draft.decisionTimeMs)
        assertTrue(draft.protocolDeviation)
        assertEquals("phone rang", draft.protocolDeviationNote)
    }

    @Test
    fun notStartedInterruption_producesInvalidWithoutElapsedTime() {
        val state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        val after = StageASessionLogic.markInterruption(state, "paused before start")
        val draft = requireNotNull(after.currentDraft)
        assertEquals(TrialTimingState.INVALID, draft.timingState)
        assertEquals(0L, draft.timerStartElapsedMs)
        assertFalse(draft.timingValid)
        assertNull(draft.decisionTimeMs)
        assertTrue(draft.protocolDeviation)
        assertTrue(draft.protocolDeviationNote.contains("interruption_before_start"))
    }

    @Test
    fun startAfterInvalid_isRejected() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.markInterruption(state, "before start")
        val before = state
        val afterStart = StageASessionLogic.startTimedChoice(state, clock)
        assertEquals(before, afterStart)
        assertEquals(TrialTimingState.INVALID, afterStart.currentDraft?.timingState)
    }

    @Test
    fun startAfterStopped_isRejected() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A, clock)
        val before = state
        val afterStart = StageASessionLogic.startTimedChoice(state, clock)
        assertEquals(before, afterStart)
        assertEquals(TrialTimingState.STOPPED, afterStart.currentDraft?.timingState)
    }

    @Test
    fun finalChoiceAfterInvalid_producesStoppedWithInvalidTiming() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.markInterruption(state, "phone rang")
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_B, clock)
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.STOPPED, draft.timingState)
        assertFalse(draft.timingValid)
        assertNull(draft.decisionTimeMs)
        assertEquals("Route B", draft.selectedRouteId)
    }

    @Test
    fun runningRestoration_producesInvalid() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.applyActivityRestorationPolicy(state)
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.INVALID, draft.timingState)
        assertFalse(draft.timingValid)
        assertNull(draft.decisionTimeMs)
        assertTrue(draft.protocolDeviation)
        assertTrue(draft.protocolDeviationNote.contains("activity_restoration"))
    }

    @Test
    fun stoppedChoice_survivesRestoration() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A, clock)
        val before = requireNotNull(state.currentDraft)
        state = StageASessionLogic.applyActivityRestorationPolicy(state)
        val after = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.STOPPED, after.timingState)
        assertEquals(before.selectedRouteId, after.selectedRouteId)
        assertEquals(before.confidence, after.confidence)
        assertEquals(before.ease, after.ease)
        assertEquals(before.reasonCode, after.reasonCode)
    }

    @Test
    fun saverRoundTrip_preservesTimingStatesSafely() {
        val group = AssignmentGroup.G1
        val assignment = StageAAssignment.trials(group).first()
        val spec = StageACaseCatalog.spec(assignment.caseId)
        val base = StageATrialDraft(
            trialOrder = assignment.trialOrder,
            assignment = assignment,
            caseSpec = spec,
            stimulusText = StageAStimuli.text(assignment.caseId, assignment.condition),
        )
        val states = listOf(
            base.copy(timingState = TrialTimingState.NOT_STARTED),
            base.copy(timingState = TrialTimingState.RUNNING, timerStartElapsedMs = 5000L),
            base.copy(
                timingState = TrialTimingState.STOPPED,
                timerStartElapsedMs = 5000L,
                selectedRouteId = "Route A",
                decisionTimeMs = 3200L,
                choiceLocked = true,
            ),
            base.copy(
                timingState = TrialTimingState.INVALID,
                timerStartElapsedMs = 5000L,
                timingValid = false,
                protocolDeviation = true,
                protocolDeviationNote = "activity_restoration",
            ),
        )
        states.forEach { draft ->
            val roundTripped = StageASessionStateSaver.roundTripDraftForTest(draft, group)
            assertEquals(draft.timingState, roundTripped.timingState)
            assertEquals(draft.timingValid, roundTripped.timingValid)
            assertEquals(draft.protocolDeviation, roundTripped.protocolDeviation)
            if (draft.timingState == TrialTimingState.INVALID) {
                assertFalse(roundTripped.timingValid)
            }
        }
    }

    @Test
    fun csvHeader_remainsExactly23Fields() {
        assertEquals(23, StageACsvHeader.LINE.split(",").size)
    }

    @Test
    fun decisionTimeMsEmpty_whenTimingValidFalse() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.markInterruption(state, "phone rang")
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_B, clock)
        val draft = requireNotNull(state.currentDraft).copy(
            confidence = 4,
            ease = 5,
            reasonCode = ReasonCode.LOWER_TOLL.csvValue,
        )
        state = StageASessionLogic.finishTrialAndAdvance(state.copy(currentDraft = draft))
        val record = state.completedRecords.single()
        assertFalse(record.timingValid)
        assertEquals("", record.decisionTimeMs)
        assertTrue(record.protocolDeviation)
    }

    @Test
    fun stoppedInterruption_doesNotChangeSelectedRoute() {
        val clock = FakeMonotonicClock(1000)
        var state = StageASessionLogic.startSession("P1", AssignmentGroup.G1)
        state = StageASessionLogic.startTimedChoice(state, clock)
        state = StageASessionLogic.confirmChoice(state, RouteChoice.ROUTE_A, clock)
        state = StageASessionLogic.markInterruption(state, "post-choice note")
        val draft = requireNotNull(state.currentDraft)
        assertEquals(TrialTimingState.STOPPED, draft.timingState)
        assertEquals("Route A", draft.selectedRouteId)
        assertFalse(draft.timingValid)
        assertNull(draft.decisionTimeMs)
    }

    private fun completeTrial(
        state: StageASessionState,
        choice: RouteChoice,
        clock: FakeMonotonicClock,
    ): StageASessionState {
        var current = StageASessionLogic.startTimedChoice(state, clock)
        current = StageASessionLogic.confirmChoice(current, choice, clock)
        val draft = requireNotNull(current.currentDraft).copy(
            confidence = 5,
            ease = 5,
            reasonCode = ReasonCode.SHORTEST_ETA.csvValue,
        )
        return StageASessionLogic.finishTrialAndAdvance(current.copy(currentDraft = draft))
    }
}
