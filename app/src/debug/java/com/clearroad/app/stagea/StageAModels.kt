package com.clearroad.app.stagea

object StageACsvHeader {
    const val LINE =
        "participant_id,assignment_group,trial_order,case_id,mode,condition,expected_outcome," +
            "selected_route_id,mode_consistent,decision_time_ms,timing_valid,confidence_1_7," +
            "ease_1_7,reason_code,completed,excluded,exclusion_reason,protocol_deviation," +
            "protocol_deviation_note,post_session_record,post_unclear_text,post_format_helped," +
            "researcher_note"
}

enum class AssignmentGroup(val label: String) {
    G1("G1"),
    G2("G2"),
}

enum class SessionPhase {
    START,
    TRIAL_STIMULUS,
    TRIAL_POST_CHOICE,
    POST_SESSION,
    EXPORT,
}

enum class RouteChoice(val csvValue: String) {
    ROUTE_A("Route A"),
    ROUTE_B("Route B"),
    CANNOT_DECIDE("cannot_decide"),
}

enum class ReasonCode(val csvValue: String) {
    SHORTEST_ETA("shortest_eta"),
    LOWER_TOLL("lower_toll"),
    NO_TOLL("no_toll"),
    ROUTES_EQUIVALENT("routes_equivalent"),
    INSUFFICIENT_INFORMATION("insufficient_information"),
    DID_NOT_UNDERSTAND("did_not_understand"),
    OTHER("other"),
}

enum class PostFormatHelped(val csvValue: String) {
    NEUTRAL_ROWS("neutral_rows"),
    DECISION_COMPRESSION("decision_compression"),
    NEITHER("neither"),
    UNSURE("unsure"),
}

enum class ExclusionReason(val csvValue: String) {
    NO_FINAL_CHOICE("no_final_choice"),
    MISSING_REQUIRED_FIELDS("missing_required_fields"),
    WRONG_STIMULUS("wrong_stimulus"),
    WRONG_CONDITION("wrong_condition"),
    RESEARCHER_WITHDRAWAL_STOP("researcher_withdrawal_stop"),
}

enum class TrialTimingState {
    NOT_STARTED,
    RUNNING,
    STOPPED,
    INVALID,
}

data class StageACaseSpec(
    val caseId: String,
    val mode: String,
    val expectedOutcome: String,
    val expectedWinner: String?,
)

data class StageATrialAssignment(
    val trialOrder: Int,
    val caseId: String,
    val condition: String,
)

data class StageATrialDraft(
    val trialOrder: Int,
    val assignment: StageATrialAssignment,
    val caseSpec: StageACaseSpec,
    val stimulusText: String,
    val timingState: TrialTimingState = TrialTimingState.NOT_STARTED,
    val timerStartElapsedMs: Long = 0L,
    val selectedRouteId: String? = null,
    val decisionTimeMs: Long? = null,
    val timingValid: Boolean = true,
    val confidence: Int? = null,
    val ease: Int? = null,
    val reasonCode: String? = null,
    val protocolDeviation: Boolean = false,
    val protocolDeviationNote: String = "",
    val excluded: Boolean = false,
    val exclusionReason: String = "",
    val researcherNote: String = "",
    val choiceLocked: Boolean = false,
)

data class StageATrialRecord(
    val participantId: String,
    val assignmentGroup: String,
    val trialOrder: Int,
    val caseId: String,
    val mode: String,
    val condition: String,
    val expectedOutcome: String,
    val selectedRouteId: String,
    val modeConsistent: String,
    val decisionTimeMs: String,
    val timingValid: Boolean,
    val confidence17: String,
    val ease17: String,
    val reasonCode: String,
    val completed: Boolean,
    val excluded: Boolean,
    val exclusionReason: String,
    val protocolDeviation: Boolean,
    val protocolDeviationNote: String,
    val postSessionRecord: Boolean,
    val postUnclearText: String,
    val postFormatHelped: String,
    val researcherNote: String,
)

data class StageASessionState(
    val participantId: String = "",
    val assignmentGroup: AssignmentGroup? = null,
    val phase: SessionPhase = SessionPhase.START,
    val trialIndex: Int = 0,
    val trials: List<StageATrialAssignment> = emptyList(),
    val completedRecords: List<StageATrialRecord> = emptyList(),
    val currentDraft: StageATrialDraft? = null,
    val sessionFinishedEarly: Boolean = false,
    val postUnclearText: String = "",
    val postFormatHelped: PostFormatHelped? = null,
    val postSessionAdministered: Boolean = false,
    val pendingBackConfirm: Boolean = false,
)
