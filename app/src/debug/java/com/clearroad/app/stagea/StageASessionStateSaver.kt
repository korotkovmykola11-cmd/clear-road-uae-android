package com.clearroad.app.stagea

import android.os.Bundle

object StageASessionStateSaver {
    private const val KEY_PARTICIPANT = "participant_id"
    private const val KEY_GROUP = "assignment_group"
    private const val KEY_PHASE = "phase"
    private const val KEY_TRIAL_INDEX = "trial_index"
    private const val KEY_FINISHED_EARLY = "finished_early"
    private const val KEY_POST_UNCLEAR = "post_unclear"
    private const val KEY_POST_FORMAT = "post_format"
    private const val KEY_POST_ADMIN = "post_admin"
    private const val KEY_RECORDS = "records"
    private const val KEY_DRAFT = "draft"

    fun save(state: StageASessionState, outState: Bundle) {
        outState.putString(KEY_PARTICIPANT, state.participantId)
        outState.putString(KEY_GROUP, state.assignmentGroup?.name)
        outState.putString(KEY_PHASE, state.phase.name)
        outState.putInt(KEY_TRIAL_INDEX, state.trialIndex)
        outState.putBoolean(KEY_FINISHED_EARLY, state.sessionFinishedEarly)
        outState.putString(KEY_POST_UNCLEAR, state.postUnclearText)
        outState.putString(KEY_POST_FORMAT, state.postFormatHelped?.name)
        outState.putBoolean(KEY_POST_ADMIN, state.postSessionAdministered)
        outState.putString(KEY_RECORDS, encodeRecords(state.completedRecords))
        state.currentDraft?.let { outState.putString(KEY_DRAFT, encodeDraft(it)) }
    }

    fun restore(bundle: Bundle?): StageASessionState? {
        if (bundle == null || !bundle.containsKey(KEY_PHASE)) return null
        val groupName = bundle.getString(KEY_GROUP) ?: return null
        val group = runCatching { AssignmentGroup.valueOf(groupName) }.getOrNull() ?: return null
        val restored = StageASessionState(
            participantId = bundle.getString(KEY_PARTICIPANT).orEmpty(),
            assignmentGroup = group,
            phase = SessionPhase.valueOf(bundle.getString(KEY_PHASE) ?: SessionPhase.START.name),
            trialIndex = bundle.getInt(KEY_TRIAL_INDEX),
            trials = StageAAssignment.trials(group),
            completedRecords = decodeRecords(bundle.getString(KEY_RECORDS)),
            currentDraft = bundle.getString(KEY_DRAFT)?.let { decodeDraft(it, group) },
            sessionFinishedEarly = bundle.getBoolean(KEY_FINISHED_EARLY),
            postUnclearText = bundle.getString(KEY_POST_UNCLEAR).orEmpty(),
            postFormatHelped = bundle.getString(KEY_POST_FORMAT)?.let {
                runCatching { PostFormatHelped.valueOf(it) }.getOrNull()
            },
            postSessionAdministered = bundle.getBoolean(KEY_POST_ADMIN),
        )
        return StageASessionLogic.repairRestoredState(restored)
    }

    private fun encodeRecords(records: List<StageATrialRecord>): String {
        return records.joinToString("\u001E") { record ->
            listOf(
                record.participantId,
                record.assignmentGroup,
                record.trialOrder.toString(),
                record.caseId,
                record.mode,
                record.condition,
                record.expectedOutcome,
                record.selectedRouteId,
                record.modeConsistent,
                record.decisionTimeMs,
                record.timingValid.toString(),
                record.confidence17,
                record.ease17,
                record.reasonCode,
                record.completed.toString(),
                record.excluded.toString(),
                record.exclusionReason,
                record.protocolDeviation.toString(),
                record.protocolDeviationNote,
                record.postSessionRecord.toString(),
                record.postUnclearText,
                record.postFormatHelped,
                record.researcherNote,
            ).joinToString("\u001F") { it.replace("\u001F", " ").replace("\u001E", " ") }
        }
    }

    private fun decodeRecords(raw: String?): List<StageATrialRecord> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split('\u001E').mapNotNull { line ->
            val p = line.split('\u001F')
            if (p.size < 23) return@mapNotNull null
            StageATrialRecord(
                participantId = p[0],
                assignmentGroup = p[1],
                trialOrder = p[2].toInt(),
                caseId = p[3],
                mode = p[4],
                condition = p[5],
                expectedOutcome = p[6],
                selectedRouteId = p[7],
                modeConsistent = p[8],
                decisionTimeMs = p[9],
                timingValid = p[10].toBoolean(),
                confidence17 = p[11],
                ease17 = p[12],
                reasonCode = p[13],
                completed = p[14].toBoolean(),
                excluded = p[15].toBoolean(),
                exclusionReason = p[16],
                protocolDeviation = p[17].toBoolean(),
                protocolDeviationNote = p[18],
                postSessionRecord = p[19].toBoolean(),
                postUnclearText = p[20],
                postFormatHelped = p[21],
                researcherNote = p[22],
            )
        }
    }

    private fun encodeDraft(draft: StageATrialDraft): String = listOf(
        draft.trialOrder.toString(),
        draft.assignment.caseId,
        draft.assignment.condition,
        draft.timerStartElapsedMs.toString(),
        draft.selectedRouteId.orEmpty(),
        draft.decisionTimeMs?.toString().orEmpty(),
        draft.timingValid.toString(),
        draft.confidence?.toString().orEmpty(),
        draft.ease?.toString().orEmpty(),
        draft.reasonCode.orEmpty(),
        draft.protocolDeviation.toString(),
        draft.protocolDeviationNote,
        draft.excluded.toString(),
        draft.exclusionReason,
        draft.researcherNote,
        draft.choiceLocked.toString(),
        draft.timingState.name,
    ).joinToString("\u001F") { it.replace("\u001F", " ") }

    private fun decodeDraft(raw: String, group: AssignmentGroup): StageATrialDraft {
        val p = raw.split('\u001F')
        val trialOrder = p[0].toInt()
        val assignment = StageAAssignment.trials(group).first { it.trialOrder == trialOrder }
        val spec = StageACaseCatalog.spec(assignment.caseId)
        val choiceLocked = p.getOrNull(15)?.toBoolean() ?: false
        val timerStartElapsedMs = p[3].toLongOrNull() ?: 0L
        val timingState = p.getOrNull(16)?.let {
            runCatching { TrialTimingState.valueOf(it) }.getOrNull()
        } ?: inferTimingState(
            timerStartElapsedMs = timerStartElapsedMs,
            choiceLocked = choiceLocked,
            timingValid = p[6].toBoolean(),
            protocolDeviation = p[10].toBoolean(),
        )
        return StageATrialDraft(
            trialOrder = trialOrder,
            assignment = assignment,
            caseSpec = spec,
            stimulusText = StageAStimuli.text(assignment.caseId, assignment.condition),
            timingState = timingState,
            timerStartElapsedMs = timerStartElapsedMs,
            selectedRouteId = p[4].ifEmpty { null },
            decisionTimeMs = p[5].toLongOrNull(),
            timingValid = p[6].toBoolean(),
            confidence = p[7].toIntOrNull(),
            ease = p[8].toIntOrNull(),
            reasonCode = p[9].ifEmpty { null },
            protocolDeviation = p[10].toBoolean(),
            protocolDeviationNote = p[11],
            excluded = p[12].toBoolean(),
            exclusionReason = p[13],
            researcherNote = p[14],
            choiceLocked = choiceLocked,
        )
    }

    private fun inferTimingState(
        timerStartElapsedMs: Long,
        choiceLocked: Boolean,
        timingValid: Boolean,
        protocolDeviation: Boolean,
    ): TrialTimingState {
        return when {
            choiceLocked -> TrialTimingState.STOPPED
            !timingValid && protocolDeviation -> TrialTimingState.INVALID
            timerStartElapsedMs > 0L -> TrialTimingState.RUNNING
            else -> TrialTimingState.NOT_STARTED
        }
    }

    internal fun roundTripDraftForTest(draft: StageATrialDraft, group: AssignmentGroup): StageATrialDraft {
        return decodeDraft(encodeDraft(draft), group)
    }
}

object StageACsvSelfTest {
    fun verifyEscaping(): Boolean {
        val sample = StageATrialRecord(
            participantId = "DRY-OWNER-G1",
            assignmentGroup = "G1",
            trialOrder = 1,
            caseId = "A-F1",
            mode = "FASTEST",
            condition = "Baseline",
            expectedOutcome = "RECOMMEND",
            selectedRouteId = "Route A",
            modeConsistent = "true",
            decisionTimeMs = "1000",
            timingValid = true,
            confidence17 = "5",
            ease17 = "5",
            reasonCode = "shortest_eta",
            completed = true,
            excluded = false,
            exclusionReason = "",
            protocolDeviation = false,
            protocolDeviationNote = "",
            postSessionRecord = true,
            postUnclearText = "comma, quote\" and\nnewline",
            postFormatHelped = "neutral_rows",
            researcherNote = "note with, comma",
        )
        val csv = StageACsvWriter.buildCsv(listOf(sample))
        val lines = csv.trim().lines()
        if (lines.size != 2) return false
        if (lines[0] != StageACsvHeader.LINE) return false
        if (!lines[1].contains("\"comma, quote\"\" and")) return false
        return true
    }
}
