package com.clearroad.app.stagea

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.clearroad.app.ui.theme.ClearRoad2Theme

class StageAResearchActivity : ComponentActivity() {

    private var sessionState by mutableStateOf(StageASessionState())
    private var pendingExportCsv by mutableStateOf<String?>(null)

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val csv = pendingExportCsv
        if (uri != null && csv != null) {
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(this, "CSV exported", Toast.LENGTH_SHORT).show()
        }
        pendingExportCsv = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StageASessionStateSaver.restore(savedInstanceState)?.let { restored ->
            sessionState = StageASessionLogic.applyActivityRestorationPolicy(
                StageASessionLogic.repairRestoredState(restored),
            )
        }
        setContent {
            ClearRoad2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    StageAResearchScreen(
                        state = sessionState,
                        onStateChange = { sessionState = it },
                        onExport = { csv, filename ->
                            pendingExportCsv = csv
                            createDocumentLauncher.launch(filename)
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StageASessionStateSaver.save(sessionState, outState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageAResearchScreen(
    state: StageASessionState,
    onStateChange: (StageASessionState) -> Unit,
    onExport: (String, String) -> Unit,
) {
    var participantId by rememberSaveable { mutableStateOf(state.participantId) }
    var selectedGroup by rememberSaveable { mutableStateOf(state.assignmentGroup?.name ?: "") }
    var showBackConfirm by rememberSaveable { mutableStateOf(false) }
    var showInterruptionDialog by rememberSaveable { mutableStateOf(false) }
    var interruptionNote by rememberSaveable { mutableStateOf("") }
    var showExcludeDialog by rememberSaveable { mutableStateOf(false) }
    var selectedExclusion by rememberSaveable { mutableStateOf(ExclusionReason.NO_FINAL_CHOICE.name) }
    var showEarlyFinishDialog by rememberSaveable { mutableStateOf(false) }

    val displayState = StageASessionLogic.repairRestoredState(state)
    if (displayState != state) {
        LaunchedEffect(displayState) {
            onStateChange(displayState)
        }
    }

    if (state.phase != SessionPhase.START && state.phase != SessionPhase.EXPORT) {
        BackHandler { showBackConfirm = true }
    }

    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("Leave session?") },
            text = { Text("Session data is kept until you export. Confirm to go back.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirm = false
                    onStateChange(StageASessionState())
                }) { Text("Reset session") }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false }) { Text("Continue") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stage A Research (Debug)")
                        Text(
                            "Owner smoke test only",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (displayState.phase) {
                SessionPhase.START -> StartScreen(
                    participantId = participantId,
                    onParticipantIdChange = { participantId = it },
                    selectedGroup = selectedGroup,
                    onGroupChange = { selectedGroup = it },
                    onStart = {
                        if (participantId.isBlank() || selectedGroup.isBlank()) return@StartScreen
                        val group = AssignmentGroup.valueOf(selectedGroup)
                        onStateChange(StageASessionLogic.startSession(participantId, group))
                    },
                )
                SessionPhase.TRIAL_STIMULUS -> {
                    val draft = displayState.currentDraft
                    if (draft != null) {
                        TrialStimulusScreen(
                            draft = draft,
                            trialLabel = "Trial ${draft.trialOrder} / ${displayState.trials.size}",
                            onStartTimedChoice = {
                                onStateChange(StageASessionLogic.startTimedChoice(displayState))
                            },
                            onChoice = { choice ->
                                onStateChange(StageASessionLogic.confirmChoice(displayState, choice))
                            },
                            onSaveExcluded = {
                                onStateChange(StageASessionLogic.finishTrialAndAdvance(displayState))
                            },
                            researcherControls = {
                                ResearcherControls(
                                    onInterruption = { showInterruptionDialog = true },
                                    onExclude = { showExcludeDialog = true },
                                    onFinishEarly = { showEarlyFinishDialog = true },
                                )
                            },
                        )
                    } else {
                        Text("Restoring session…")
                    }
                }
                SessionPhase.TRIAL_POST_CHOICE -> {
                    val draft = displayState.currentDraft
                    if (draft != null) {
                        PostChoiceScreen(
                            draft = draft,
                            onDraftChange = { onStateChange(displayState.copy(currentDraft = it)) },
                            onContinue = {
                                val current = displayState.currentDraft ?: return@PostChoiceScreen
                                if (current.excluded) {
                                    if (current.exclusionReason.isEmpty()) return@PostChoiceScreen
                                } else if (
                                    current.selectedRouteId.isNullOrEmpty() ||
                                    current.confidence == null ||
                                    current.ease == null ||
                                    current.reasonCode.isNullOrEmpty()
                                ) {
                                    return@PostChoiceScreen
                                }
                                onStateChange(StageASessionLogic.finishTrialAndAdvance(displayState))
                            },
                            researcherControls = {
                                ResearcherControls(
                                    onInterruption = {
                                        onStateChange(
                                            StageASessionLogic.markInterruption(
                                                displayState,
                                                "Marked during post-choice",
                                            ),
                                        )
                                    },
                                    onExclude = { showExcludeDialog = true },
                                    onFinishEarly = { showEarlyFinishDialog = true },
                                )
                            },
                        )
                    } else {
                        Text("Restoring session…")
                    }
                }
                SessionPhase.POST_SESSION -> PostSessionScreen(
                    unclearText = displayState.postUnclearText,
                    formatHelped = displayState.postFormatHelped,
                    onSubmit = { unclear, helped ->
                        onStateChange(StageASessionLogic.applyPostSession(displayState, unclear, helped))
                    },
                    onSkip = { onStateChange(StageASessionLogic.skipPostSession(displayState)) },
                )
                SessionPhase.EXPORT -> ExportScreen(
                    state = displayState,
                    onExport = onExport,
                    onNewSession = { onStateChange(StageASessionState()) },
                )
            }
        }
    }

    if (showInterruptionDialog) {
        AlertDialog(
            onDismissRequest = { showInterruptionDialog = false },
            title = { Text("Mark interruption") },
            text = {
                OutlinedTextField(
                    value = interruptionNote,
                    onValueChange = { interruptionNote = it },
                    label = { Text("protocol_deviation_note") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (interruptionNote.isNotBlank()) {
                        onStateChange(StageASessionLogic.markInterruption(state, interruptionNote))
                        showInterruptionDialog = false
                        interruptionNote = ""
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showInterruptionDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showExcludeDialog) {
        AlertDialog(
            onDismissRequest = { showExcludeDialog = false },
            title = { Text("Exclude trial") },
            text = {
                Column {
                    ExclusionReason.entries.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedExclusion == reason.name,
                                onClick = { selectedExclusion = reason.name },
                            )
                            Text(reason.csvValue)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val reason = ExclusionReason.valueOf(selectedExclusion)
                    onStateChange(StageASessionLogic.excludeCurrentTrial(state, reason))
                    showExcludeDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showExcludeDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showEarlyFinishDialog) {
        AlertDialog(
            onDismissRequest = { showEarlyFinishDialog = false },
            title = { Text("Finish session early?") },
            text = { Text("No future trial rows will be created. Post-session follows.") },
            confirmButton = {
                TextButton(onClick = {
                    onStateChange(StageASessionLogic.finishSessionEarly(state))
                    showEarlyFinishDialog = false
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showEarlyFinishDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StartScreen(
    participantId: String,
    onParticipantIdChange: (String) -> Unit,
    selectedGroup: String,
    onGroupChange: (String) -> Unit,
    onStart: () -> Unit,
) {
    Text("Synthetic participant ID only (e.g. DRY-OWNER-G1)")
    OutlinedTextField(
        value = participantId,
        onValueChange = onParticipantIdChange,
        label = { Text("participant_id") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Text("assignment_group")
    AssignmentGroup.entries.forEach { group ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedGroup == group.name,
                onClick = { onGroupChange(group.name) },
            )
            Text(group.label)
        }
    }
    Button(
        onClick = onStart,
        enabled = participantId.isNotBlank() && selectedGroup.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Start session")
    }
}

@Composable
private fun TrialStimulusScreen(
    draft: StageATrialDraft,
    trialLabel: String,
    onStartTimedChoice: () -> Unit,
    onChoice: (RouteChoice) -> Unit,
    onSaveExcluded: () -> Unit,
    researcherControls: @Composable () -> Unit,
) {
    Text(trialLabel, style = MaterialTheme.typography.titleMedium)
    Text("${draft.caseSpec.caseId} · ${draft.assignment.condition} · ${draft.caseSpec.mode}")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = draft.stimulusText,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Researcher timing", style = MaterialTheme.typography.titleSmall)
            when (draft.timingState) {
                TrialTimingState.NOT_STARTED -> Text("Start timing only after the standard instruction.")
                TrialTimingState.RUNNING -> Text("Timer running.")
                TrialTimingState.INVALID -> Text("Timing invalidated — choices allowed without valid timing.")
                TrialTimingState.STOPPED -> Text("Final answer locked.")
            }
            Button(
                onClick = onStartTimedChoice,
                enabled = draft.timingState == TrialTimingState.NOT_STARTED,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start timed choice")
            }
        }
    }
    Text("Timed choice (Q1) — select final answer:")
    val choicesEnabled = draft.timingState == TrialTimingState.RUNNING ||
        draft.timingState == TrialTimingState.INVALID
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { onChoice(RouteChoice.ROUTE_A) },
            enabled = choicesEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Text("Route A")
        }
        Button(
            onClick = { onChoice(RouteChoice.ROUTE_B) },
            enabled = choicesEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Text("Route B")
        }
    }
    OutlinedButton(
        onClick = { onChoice(RouteChoice.CANNOT_DECIDE) },
        enabled = choicesEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Cannot decide")
    }
    if (draft.excluded) {
        Button(onClick = onSaveExcluded, modifier = Modifier.fillMaxWidth()) {
            Text("Save excluded trial and continue")
        }
    }
    Spacer(Modifier.height(8.dp))
    researcherControls()
}

@Composable
private fun PostChoiceScreen(
    draft: StageATrialDraft,
    onDraftChange: (StageATrialDraft) -> Unit,
    onContinue: () -> Unit,
    researcherControls: @Composable () -> Unit,
) {
    Text("Post-choice questions", style = MaterialTheme.typography.titleMedium)
    Text("Final answer: ${draft.selectedRouteId.orEmpty()}")
    if (draft.protocolDeviation) {
        Text("Interruption or restoration marked — timing invalid")
    }
    if (draft.excluded) {
        Text("Trial marked excluded: ${draft.exclusionReason}")
    }
    Text("Confidence (1–7): ${draft.confidence ?: "-"}")
    Slider(
        value = (draft.confidence ?: 4).toFloat(),
        onValueChange = { onDraftChange(draft.copy(confidence = it.toInt())) },
        valueRange = 1f..7f,
        steps = 5,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Ease (1–7): ${draft.ease ?: "-"}")
    Slider(
        value = (draft.ease ?: 4).toFloat(),
        onValueChange = { onDraftChange(draft.copy(ease = it.toInt())) },
        valueRange = 1f..7f,
        steps = 5,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Reason")
    ReasonCode.entries.forEach { reason ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = draft.reasonCode == reason.csvValue,
                onClick = { onDraftChange(draft.copy(reasonCode = reason.csvValue)) },
            )
            Text(reason.csvValue)
        }
    }
    OutlinedTextField(
        value = draft.researcherNote,
        onValueChange = { onDraftChange(draft.copy(researcherNote = it)) },
        label = { Text("researcher_note (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Save trial and continue")
    }
    researcherControls()
}

@Composable
private fun ResearcherControls(onInterruption: () -> Unit, onExclude: () -> Unit, onFinishEarly: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Researcher controls", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = onInterruption, modifier = Modifier.fillMaxWidth()) {
                Text("Mark interruption")
            }
            OutlinedButton(onClick = onExclude, modifier = Modifier.fillMaxWidth()) {
                Text("Exclude trial")
            }
            OutlinedButton(onClick = onFinishEarly, modifier = Modifier.fillMaxWidth()) {
                Text("Finish session early")
            }
        }
    }
}

@Composable
private fun PostSessionScreen(
    unclearText: String,
    formatHelped: PostFormatHelped?,
    onSubmit: (String, PostFormatHelped?) -> Unit,
    onSkip: () -> Unit,
) {
    var unclear by rememberSaveable { mutableStateOf(unclearText) }
    var helped by rememberSaveable { mutableStateOf(formatHelped?.name ?: "") }
    Text("Post-session questionnaire", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = unclear,
        onValueChange = { unclear = it },
        label = { Text("Q5: What was unclear?") },
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Q6: Which format helped you decide faster?")
    PostFormatHelped.entries.forEach { option ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = helped == option.name,
                onClick = { helped = option.name },
            )
            Text(option.csvValue)
        }
    }
    Button(
        onClick = {
            val selected = helped.takeIf { it.isNotBlank() }?.let { PostFormatHelped.valueOf(it) }
            onSubmit(unclear, selected)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Complete post-session")
    }
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip post-session")
    }
}

@Composable
private fun ExportScreen(
    state: StageASessionState,
    onExport: (String, String) -> Unit,
    onNewSession: () -> Unit,
) {
    val group = state.assignmentGroup ?: return
    val csv = runCatching { StageACsvWriter.buildCsv(state.completedRecords) }.getOrNull()
    Text("Session complete", style = MaterialTheme.typography.titleMedium)
    Text("Trials recorded: ${state.completedRecords.size}")
    Text("CSV fields: 23")
    StageASessionLogicSmoke.runAll().forEach { result ->
        Text("${if (result.passed) "PASS" else "FAIL"}: ${result.name} — ${result.detail}")
    }
    StageATimingIntegritySmoke.runAll().forEach { result ->
        Text("${if (result.passed) "PASS" else "FAIL"}: ${result.name} — ${result.detail}")
    }
    if (csv != null) {
        Button(
            onClick = {
                onExport(csv, StageACsvWriter.suggestedFilename(state.participantId, group))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export CSV (SAF)")
        }
    } else {
        Text("No trial rows — export unavailable")
    }
    OutlinedButton(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
        Text("New session")
    }
}
