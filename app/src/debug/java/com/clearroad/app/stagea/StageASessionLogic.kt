package com.clearroad.app.stagea

object StageASessionLogic {
    fun startSession(participantId: String, group: AssignmentGroup): StageASessionState {
        return StageASessionState(
            participantId = participantId.trim(),
            assignmentGroup = group,
            phase = SessionPhase.TRIAL_STIMULUS,
            trialIndex = 0,
            trials = StageAAssignment.trials(group),
            completedRecords = emptyList(),
            currentDraft = createDraft(group, StageAAssignment.trials(group).first()),
        )
    }

    fun repairRestoredState(state: StageASessionState): StageASessionState {
        if (state.phase != SessionPhase.TRIAL_STIMULUS &&
            state.phase != SessionPhase.TRIAL_POST_CHOICE
        ) {
            return state
        }
        if (state.currentDraft != null) return state
        val group = state.assignmentGroup ?: return StageASessionState(
            participantId = state.participantId,
        )
        val assignment = state.trials.getOrNull(state.trialIndex) ?: return StageASessionState(
            participantId = state.participantId,
        )
        return state.copy(
            phase = SessionPhase.TRIAL_STIMULUS,
            currentDraft = createDraft(group, assignment),
        )
    }

    fun applyActivityRestorationPolicy(state: StageASessionState): StageASessionState {
        val draft = state.currentDraft ?: return state
        if (draft.timingState != TrialTimingState.RUNNING) return state
        return state.copy(
            currentDraft = draft.copy(
                timingState = TrialTimingState.INVALID,
                timingValid = false,
                protocolDeviation = true,
                decisionTimeMs = null,
                protocolDeviationNote = appendNote(draft.protocolDeviationNote, "activity_restoration"),
            ),
        )
    }

    fun createDraft(group: AssignmentGroup, assignment: StageATrialAssignment): StageATrialDraft {
        val spec = StageACaseCatalog.spec(assignment.caseId)
        return StageATrialDraft(
            trialOrder = assignment.trialOrder,
            assignment = assignment,
            caseSpec = spec,
            stimulusText = StageAStimuli.text(assignment.caseId, assignment.condition),
            timingState = TrialTimingState.NOT_STARTED,
            timerStartElapsedMs = 0L,
        )
    }

    fun startTimedChoice(
        state: StageASessionState,
        clock: MonotonicClock = StageASessionClock.DEFAULT,
    ): StageASessionState {
        val draft = state.currentDraft ?: return state
        if (draft.timingState != TrialTimingState.NOT_STARTED || draft.choiceLocked) return state
        return state.copy(
            currentDraft = draft.copy(
                timingState = TrialTimingState.RUNNING,
                timerStartElapsedMs = clock.nowMs(),
                timingValid = true,
            ),
        )
    }

    fun confirmChoice(
        state: StageASessionState,
        choice: RouteChoice,
        clock: MonotonicClock = StageASessionClock.DEFAULT,
    ): StageASessionState {
        val draft = state.currentDraft ?: return state
        if (draft.choiceLocked) return state
        if (draft.timingState == TrialTimingState.NOT_STARTED ||
            draft.timingState == TrialTimingState.STOPPED
        ) {
            return state
        }
        val timingValid = draft.timingValid &&
            !draft.protocolDeviation &&
            draft.timingState == TrialTimingState.RUNNING
        val elapsed = if (timingValid) {
            clock.nowMs() - draft.timerStartElapsedMs
        } else {
            null
        }
        return state.copy(
            currentDraft = draft.copy(
                selectedRouteId = choice.csvValue,
                decisionTimeMs = elapsed,
                timingValid = timingValid,
                timingState = TrialTimingState.STOPPED,
                choiceLocked = true,
            ),
            phase = SessionPhase.TRIAL_POST_CHOICE,
        )
    }

    fun markInterruption(state: StageASessionState, note: String): StageASessionState {
        val draft = state.currentDraft ?: return state
        val trimmed = note.trim()
        val updatedDraft = when (draft.timingState) {
            TrialTimingState.NOT_STARTED -> draft.copy(
                timingState = TrialTimingState.INVALID,
                timerStartElapsedMs = 0L,
                timingValid = false,
                decisionTimeMs = null,
                protocolDeviation = true,
                protocolDeviationNote = appendNote(trimmed, "interruption_before_start"),
            )
            TrialTimingState.RUNNING -> draft.copy(
                timingState = TrialTimingState.INVALID,
                timingValid = false,
                decisionTimeMs = null,
                protocolDeviation = true,
                protocolDeviationNote = trimmed,
            )
            TrialTimingState.INVALID -> draft.copy(
                timingValid = false,
                decisionTimeMs = null,
                protocolDeviation = true,
                protocolDeviationNote = appendNote(draft.protocolDeviationNote, trimmed),
            )
            TrialTimingState.STOPPED -> draft.copy(
                timingValid = false,
                decisionTimeMs = null,
                protocolDeviation = true,
                protocolDeviationNote = appendNote(draft.protocolDeviationNote, trimmed),
            )
        }
        return state.copy(currentDraft = updatedDraft)
    }

    fun excludeCurrentTrial(state: StageASessionState, reason: ExclusionReason): StageASessionState {
        val draft = state.currentDraft ?: return state
        return state.copy(
            currentDraft = draft.copy(
                excluded = true,
                exclusionReason = reason.csvValue,
            ),
        )
    }

    fun finishTrialAndAdvance(state: StageASessionState): StageASessionState {
        val draft = state.currentDraft ?: return state
        val group = state.assignmentGroup ?: return state
        val record = draftToRecord(state.participantId, group, draft)
        val newRecords = state.completedRecords + record
        val nextIndex = state.trialIndex + 1
        return if (nextIndex >= state.trials.size) {
            state.copy(
                completedRecords = newRecords,
                currentDraft = null,
                trialIndex = nextIndex,
                phase = SessionPhase.POST_SESSION,
            )
        } else {
            state.copy(
                completedRecords = newRecords,
                trialIndex = nextIndex,
                currentDraft = createDraft(group, state.trials[nextIndex]),
                phase = SessionPhase.TRIAL_STIMULUS,
            )
        }
    }

    fun finishSessionEarly(state: StageASessionState): StageASessionState {
        val draft = state.currentDraft
        val group = state.assignmentGroup ?: return state
        val records = if (draft != null) {
            state.completedRecords + draftToRecord(state.participantId, group, draft)
        } else {
            state.completedRecords
        }
        return state.copy(
            completedRecords = records,
            currentDraft = null,
            sessionFinishedEarly = true,
            phase = SessionPhase.POST_SESSION,
        )
    }

    fun applyPostSession(
        state: StageASessionState,
        unclearText: String,
        formatHelped: PostFormatHelped?,
    ): StageASessionState {
        if (state.completedRecords.isEmpty()) return state
        val lastIndex = state.completedRecords.lastIndex
        val updated = state.completedRecords.mapIndexed { index, record ->
            if (index == lastIndex) {
                record.copy(
                    postSessionRecord = true,
                    postUnclearText = unclearText,
                    postFormatHelped = formatHelped?.csvValue.orEmpty(),
                )
            } else {
                record.copy(
                    postSessionRecord = false,
                    postUnclearText = "",
                    postFormatHelped = "",
                )
            }
        }
        return state.copy(
            completedRecords = updated,
            postUnclearText = unclearText,
            postFormatHelped = formatHelped,
            postSessionAdministered = true,
            phase = SessionPhase.EXPORT,
        )
    }

    fun skipPostSession(state: StageASessionState): StageASessionState {
        if (state.completedRecords.isEmpty()) return state
        val updated = state.completedRecords.map { record ->
            record.copy(
                postSessionRecord = false,
                postUnclearText = "",
                postFormatHelped = "",
            )
        }
        return state.copy(
            completedRecords = updated,
            postSessionAdministered = false,
            phase = SessionPhase.EXPORT,
        )
    }

    internal fun appendNote(existing: String, note: String): String = when {
        note.isBlank() -> existing
        existing.isBlank() -> note
        existing.contains(note) -> existing
        else -> "$existing; $note"
    }

    private fun draftToRecord(
        participantId: String,
        group: AssignmentGroup,
        draft: StageATrialDraft,
    ): StageATrialRecord {
        val selected = draft.selectedRouteId.orEmpty()
        val hasAnswer = selected.isNotEmpty()
        val hasPostChoice = draft.confidence != null &&
            draft.ease != null &&
            !draft.reasonCode.isNullOrEmpty()
        val completed = !draft.excluded && hasAnswer && hasPostChoice
        val timingValid = if (draft.excluded && !hasAnswer) {
            false
        } else {
            draft.timingValid &&
                draft.timingState == TrialTimingState.STOPPED &&
                !draft.protocolDeviation
        }
        val modeConsistent = if (hasAnswer) {
            StageAModeConsistent.compute(
                draft.caseSpec.expectedOutcome,
                draft.caseSpec.expectedWinner,
                selected,
            )
        } else {
            ""
        }
        return StageATrialRecord(
            participantId = participantId,
            assignmentGroup = group.label,
            trialOrder = draft.trialOrder,
            caseId = draft.caseSpec.caseId,
            mode = draft.caseSpec.mode,
            condition = draft.assignment.condition,
            expectedOutcome = draft.caseSpec.expectedOutcome,
            selectedRouteId = selected,
            modeConsistent = modeConsistent,
            decisionTimeMs = if (timingValid && draft.decisionTimeMs != null) {
                draft.decisionTimeMs.toString()
            } else {
                ""
            },
            timingValid = timingValid,
            confidence17 = draft.confidence?.toString().orEmpty(),
            ease17 = draft.ease?.toString().orEmpty(),
            reasonCode = draft.reasonCode.orEmpty(),
            completed = completed,
            excluded = draft.excluded,
            exclusionReason = if (draft.excluded) draft.exclusionReason else "",
            protocolDeviation = draft.protocolDeviation,
            protocolDeviationNote = draft.protocolDeviationNote,
            postSessionRecord = false,
            postUnclearText = "",
            postFormatHelped = "",
            researcherNote = draft.researcherNote,
        )
    }
}
