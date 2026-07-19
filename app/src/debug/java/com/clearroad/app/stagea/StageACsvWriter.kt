package com.clearroad.app.stagea

object StageACsvWriter {
    fun escapeField(value: String?): String {
        if (value == null || value.isEmpty()) return ""
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    fun boolField(value: Boolean): String = if (value) "true" else "false"

    fun recordToLine(record: StageATrialRecord): String = listOf(
        escapeField(record.participantId),
        escapeField(record.assignmentGroup),
        record.trialOrder.toString(),
        escapeField(record.caseId),
        escapeField(record.mode),
        escapeField(record.condition),
        escapeField(record.expectedOutcome),
        escapeField(record.selectedRouteId),
        escapeField(record.modeConsistent),
        escapeField(record.decisionTimeMs),
        boolField(record.timingValid),
        escapeField(record.confidence17),
        escapeField(record.ease17),
        escapeField(record.reasonCode),
        boolField(record.completed),
        boolField(record.excluded),
        escapeField(record.exclusionReason),
        boolField(record.protocolDeviation),
        escapeField(record.protocolDeviationNote),
        boolField(record.postSessionRecord),
        escapeField(record.postUnclearText),
        escapeField(record.postFormatHelped),
        escapeField(record.researcherNote),
    ).joinToString(",")

    fun buildCsv(records: List<StageATrialRecord>): String {
        require(records.isNotEmpty()) { "Cannot export empty session" }
        return buildString {
            appendLine(StageACsvHeader.LINE)
            records.forEach { appendLine(recordToLine(it)) }
        }
    }

    fun suggestedFilename(participantId: String, group: AssignmentGroup): String =
        "stage-a_${participantId}_${group.label}.csv"
}
