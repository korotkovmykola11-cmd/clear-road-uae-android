# Stage A — Experiment Pack v0.1

**Status:** Candidate protocol — documentation only
**Version:** 0.1
**Scope:** Reproducible feasibility-pilot protocol for Candidate A only

**Stage A execution:** Not authorized

**Implementation:** Not authorized

---

## Document boundary

This pack prepares a reproducible protocol for a **future** Stage A synthetic feasibility pilot.

It does **not** execute Stage A.

It does **not** recruit participants.

It does **not** authorize implementation.

It does **not** modify committed contracts or truth tables.

### Source artifacts (read-only)

| Artifact | Commit / location |
|----------|-------------------|
| Research Decision Record 001 | `docs/research-decision-record-001-staged-validation.md` |
| Decision Compression Contract v0.1 | `docs/stage-a-decision-compression-contract-v0.1.md` — commit `80504364748dbee4526a0044126ccd56e484d515` |
| Explicit Route Preference Contract v0.1 | `docs/stage-a-explicit-preference-contract-v0.1.md` — same commit |
| Synthetic Cases v0.1 | `docs/stage-a-synthetic-cases-v0.1.md` — same commit |
| Data sheet template | `docs/stage-a-data-sheet-v0.1.csv` |

### Project status (unchanged)

| Item | Status |
|------|--------|
| Stage 0B | **Completed** |
| Terms feasibility | **In progress — F01–F05 reviewed** |
| Stage A contracts v0.1 | **Recorded** in commit `80504364748dbee4526a0044126ccd56e484d515` |
| Stage A execution | **Not started** |
| Waypoint research | **Blocked** |
| Implementation | **Not authorized** |

---

## A. Purpose and boundaries

Stage A is a **feasibility pilot**, not a powered efficacy test and not a product-validation study.

This pack tests **only Candidate A — Decision Compression**.

**Candidate B — Explicit Route Preference is excluded.** Dataset B cases, preference profiles, and preference stimuli must not appear in this pilot.

| Included | Excluded |
|----------|----------|
| MARSHIO-owned synthetic Dataset A stimuli (8 cases) | Google Maps, Google API, Google screenshots, polylines, coordinates, route IDs |
| Baseline neutral rows vs Decision Compression treatment | Explicit Preference, Personal Route Memory |
| Measurement of choice, timing, comprehension, deviations | Calm/Smooth mode, stress score, junction score, traffic stability |
| Descriptive feasibility outputs | AI explanations, route intelligence, navigation, learned preferences |
| Planning estimates for later Stage B design | Product GO/KILL, statistical effectiveness claims, Stage B authorization |

Stage A:

- does **not** prove product value;
- does **not** prove statistical effectiveness;
- does **not** authorize Stage B;
- does **not** authorize implementation;
- does **not** validate synthetic contract thresholds (3 minutes / AED 4) as market or user policy — those remain **synthetic contract constants only**.

---

## B. Pilot questions

Stage A answers **only** these feasibility questions:

1. Do participants understand the task?
2. Can they choose a route in the assigned mode (FASTEST or NO_TOLLS), or state that they cannot decide from the information shown?
3. Can choice and decision time be measured consistently across researchers?
4. Is there an obvious ceiling or floor effect in the neutral baseline?
5. Is the Decision Compression format understandable?
6. How many missing responses, exclusions, and protocol deviations occur?
7. Is the procedure suitable for informing a later Stage B design?

No additional product hypotheses are introduced by this pack.

---

## C. Candidate A only

### Conditions

| Condition | Definition |
|-----------|------------|
| **Baseline** | Neutral, symmetric presentation of all route rows for the case using ordinary text rows. No recommendation. No outcome label. No explanation. |
| **Treatment** | Decision Compression presentation per `stage-a-decision-compression-contract-v0.1.md` for the same case inputs and mode. |

### Prohibited in this pilot

Explicit Preference; Personal Route Memory; Calm/Smooth/Easy Drive modes; stress, junction, or traffic-stability scores; AI explanations; route intelligence; Google comparison; navigation handoff; learned or stored preferences; Dataset B cases.

Decision Compression and Explicit Preference must **not** be mixed in one pilot session or one truth table.

---

## D. Stimulus construction rules

### Shared rules

- Use **only** the eight committed Dataset A cases: `A-F1`, `A-F2`, `A-F3`, `A-F4`, `A-N1`, `A-N2`, `A-N3`, `A-N4`.
- Do **not** change case inputs or truth-table outcomes.
- Do **not** add a third route.
- Each case has exactly **two** routes: Route A and Route B.
- `distanceKm`, `corridorKey`, and `corridorLabel` are display-only and do **not** affect the decision engine. For participant-facing stimuli, show `corridorLabel` only as route identification. Do **not** show `distanceKm`.
- Truth table fields (`expected_outcome`, expected winner, mode-consistent answer) are **never** shown to participants.
- Synthetic contract constants (`ETA_MEANINGFUL_DIFFERENCE_MIN = 3 minutes`, `MIN_TOLL_DIFFERENCE_AED = 4 AED`) are **not** disclosed to participants.

### Session-wide route labeling

At session start, tell participants:

> Throughout this session, the two route options are always labeled **Route A** and **Route B**.

These labels match the committed synthetic `routeId` values exactly (`Route A`, `Route B`). Do not use shortened labels (`A`, `B`).

### Participant choice instruction

Use this exact closing line in every Baseline and Treatment stimulus (all outcomes):

```
Choose Route A, Route B, or say that you cannot decide from the information shown.
```

Participants are **not** required to pick Route A or Route B when the shown information is insufficient for a mode-consistent choice.

Every Baseline trial uses the same symmetric structure:

```
Mode: [FASTEST | NO_TOLLS]

Route A
Arrival: [value | unavailable]
Toll: [AED value | no toll | unavailable]
Corridor: [corridorLabel]

Route B
Arrival: [value | unavailable]
Toll: [AED value | no toll | unavailable]
Corridor: [corridorLabel]

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

Baseline rules:

- Both routes use identical field order and formatting.
- No route is highlighted, ordered first for emphasis, or marked recommended.
- No outcome words (`RECOMMEND`, `EQUIVALENT`, `INSUFFICIENT_DATA`) appear.
- No MARSHIO explanation block appears.
- `unavailable` is used when the committed fixture has `UNKNOWN` for ETA or toll.
- `no toll` is used when `tollAed = 0` and toll is `KNOWN`.

### Treatment form template

Every Treatment trial begins with the mode line, then shows the committed Decision Compression text for that case.

**Treatment — RECOMMEND cases**

Show only the compression block. Route labels are already defined session-wide.

```
Mode: [FASTEST | NO_TOLLS]

[Decision Compression lines from truth table]

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

**Treatment — EQUIVALENT or INSUFFICIENT_DATA cases**

Show the compression block, then repeat the same neutral route rows as Baseline so the participant can identify Route A and Route B.

```
Mode: [FASTEST | NO_TOLLS]

[Decision Compression lines from truth table]

Route A
Arrival: [value | unavailable]
Toll: [AED value | no toll | unavailable]
Corridor: [corridorLabel]

Route B
Arrival: [value | unavailable]
Toll: [AED value | no toll | unavailable]
Corridor: [corridorLabel]

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

Treatment rules:

- Use only contract-supported facts from the committed truth table.
- Do **not** invent trade-offs.
- Do **not** add preference language.
- Do **not** add qualitative claims beyond the committed explanation lines.

---

### Participant-facing stimuli — all eight cases

#### A-F1 — FASTEST — RECOMMEND Route A

**Baseline**

```
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
```

**Treatment**

```
Mode: FASTEST

Take Route A
6 min faster
Same toll

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

---

#### A-F2 — FASTEST — EQUIVALENT

**Baseline**

```
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
```

**Treatment**

```
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
```

---

#### A-F3 — FASTEST — INSUFFICIENT_DATA

**Baseline**

```
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
```

**Treatment**

```
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
```

---

#### A-F4 — FASTEST — RECOMMEND Route A

**Baseline**

```
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
```

**Treatment**

```
Mode: FASTEST

Take Route A
5 min faster
Toll comparison unavailable

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

---

#### A-N1 — NO_TOLLS — RECOMMEND Route B

**Baseline**

```
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
```

**Treatment**

```
Mode: NO_TOLLS

Take Route B
AED 8 less
4 min longer

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

---

#### A-N2 — NO_TOLLS — RECOMMEND Route A

**Baseline**

```
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
```

**Treatment**

```
Mode: NO_TOLLS

Take Route A
Same toll
6 min faster

Choose Route A, Route B, or say that you cannot decide from the information shown.
```

---

#### A-N3 — NO_TOLLS — INSUFFICIENT_DATA

**Baseline**

```
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
```

**Treatment**

```
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
```

---

#### A-N4 — NO_TOLLS — EQUIVALENT

**Baseline**

```
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
```

**Treatment**

```
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
```

---

## E. Experimental assignment

### Core rules

- Each participant sees each Dataset A case **exactly once**.
- No participant sees the same `case_id` in both Baseline and Treatment.
- Across participants, every case must appear in **both** conditions.
- FASTEST and NO_TOLLS cases must both appear in Baseline and in Treatment across the sample.
- Assignment must be reproducible from anonymous `participant_id` or pre-defined `assignment_group`.
- This pack does **not** set participant count. It defines a repeatable matrix for **two or more balanced groups**.

### Assignment groups

Use two groups. Group membership is deterministic:

| Rule | Group |
|------|-------|
| `participant_id` ends with an **odd** final digit | `G1` |
| `participant_id` ends with an **even** final digit (including 0) | `G2` |

If alphanumeric IDs are used, use the last hexadecimal digit: `0–7` → `G1`, `8–F` → `G2`. Document the chosen rule in `researcher_note` for the session.

### Condition map per case

| case_id | G1 condition | G2 condition |
|---------|--------------|--------------|
| A-F1 | Baseline | Treatment |
| A-F2 | Baseline | Treatment |
| A-F3 | Treatment | Baseline |
| A-F4 | Treatment | Baseline |
| A-N1 | Baseline | Treatment |
| A-N2 | Baseline | Treatment |
| A-N3 | Treatment | Baseline |
| A-N4 | Treatment | Baseline |

Balance check:

- G1: 4 Baseline + 4 Treatment
- G2: 4 Baseline + 4 Treatment
- Each case appears in both conditions across groups
- FASTEST: 2 Baseline + 2 Treatment per group
- NO_TOLLS: 2 Baseline + 2 Treatment per group

### Trial order (normative)

**The assignment tables below are normative. No alternative trial order is permitted in v0.1.**

Condition per trial comes from the participant's group.

**G1 trial order** — alternates starting with Baseline:

| trial_order | case_id | condition |
|-------------|---------|-----------|
| 1 | A-F1 | Baseline |
| 2 | A-F3 | Treatment |
| 3 | A-F2 | Baseline |
| 4 | A-F4 | Treatment |
| 5 | A-N1 | Baseline |
| 6 | A-N3 | Treatment |
| 7 | A-N2 | Baseline |
| 8 | A-N4 | Treatment |

**G2 trial order** — alternates starting with Treatment:

| trial_order | case_id | condition |
|-------------|---------|-----------|
| 1 | A-F1 | Treatment |
| 2 | A-F3 | Baseline |
| 3 | A-F2 | Treatment |
| 4 | A-F4 | Baseline |
| 5 | A-N1 | Treatment |
| 6 | A-N3 | Baseline |
| 7 | A-N2 | Treatment |
| 8 | A-N4 | Baseline |

### Minimum balance condition

At least **two assignment groups** must be used so every case receives both Baseline and Treatment exposure across participants. Additional groups may replicate this matrix; they must not change per-case condition mapping.

This assignment is **not** an RCT and makes no statistical-power claim.

---

## F. Moderator script

Read aloud unless noted. Do not paraphrase threshold logic or name the correct route.

### Opening statement

> Thank you for taking part in this research session.
>
> This is a route-choice task using synthetic example routes.
>
> You are not being tested on geography knowledge.
>
> There are no right or wrong opinions about brands or apps.
>
> I will show you pairs of routes labeled Route A and Route B.
>
> For each trial, choose Route A, Route B, or say that you cannot decide from the information shown.

### Task instruction (before each trial)

> Please look at the information on the screen.
>
> The mode for this trial is shown at the top.
>
> Choose Route A, Route B, or say that you cannot decide from the information shown.
>
> Tell me your final answer when you are ready. Your final answer stops the timer.

### FASTEST meaning (once per session, before first FASTEST trial)

> **FASTEST** means you want the route with the lowest reliable arrival time shown.
>
> If arrival time is unavailable for a route, use only the information that is shown.

### NO_TOLLS meaning (once per session, before first NO_TOLLS trial)

> **NO_TOLLS** means you want the route with the lowest reliable toll shown.
>
> If toll is unavailable for a route, use only the information that is shown.
>
> "No toll" means zero toll.

### Final-answer rule

> You may change your answer before you confirm.
>
> When you give your final answer — Route A, Route B, or that you cannot decide — that ends the timed part of the trial.

### Closing statement

> That was the last route-choice trial.
>
> I will now ask a few short follow-up questions about the session.

### Researcher prohibitions

The researcher must **not** explain:

- which route is correct;
- why MARSHIO recommends a route;
- the meaning of specific ETA or toll differences;
- threshold logic (including 3-minute or AED 4 contract constants);
- what the outcome label means for the current case;
- how the product works beyond the standard mode definitions.

If a participant still does not understand the task after **one** repetition of the standard task instruction, record a comprehensibility problem in `researcher_note`. Do not give free-form coaching.

---

## G. Timing protocol

Decision time is measured by the researcher with a stopwatch or timer. Participants do not self-report elapsed time from memory.

### Start event

Start timing when:

1. the full stimulus for the trial is visible to the participant; and
2. the researcher has finished the standard task instruction for that trial.

### Stop event

Stop timing when the participant states a **final** answer:

- `Route A`
- `Route B`
- `cannot_decide` — the participant states they cannot decide from the information shown

For `cannot_decide`, the stop event occurs when the participant **finally** states that they cannot choose from the shown information.

### Q1 is the timed response

**Q1 is the timed response.** The participant's final answer stops the timer. The researcher records that same answer in `selected_route_id`. **Q1 is not asked again** after the timer stops.

After the stop event, the researcher records only:

- `confidence_1_7` (Q2)
- `ease_1_7` (Q3)
- `reason_code` (Q4)

### `timing_valid`

Every **actually recorded** trial row must have `timing_valid` set to `true` or `false`. An empty `timing_valid` value is **prohibited** for an existing row.

| Value | When |
|-------|------|
| `true` | Timer start and stop were correct; no interruption after start; measurement is trustworthy; `decision_time_ms` is present |
| `false` | Timer did not start correctly; timer stop was incorrect; interruption occurred after start; timing is untrustworthy; no final answer; measurement cannot be used |

If `timing_valid = false`, then `decision_time_ms` must be **empty**. Do not use reconstructed time, pause/resume time, or approximate values.

### Special handling

| Event | Recording rule |
|-------|----------------|
| Changed answer before final confirmation | Use time to **final** confirmation only; note earlier change in `researcher_note` if useful |
| No final answer | `completed = false`; `selected_route_id` empty; `decision_time_ms` empty; `timing_valid = false`; record `exclusion_reason = no_final_choice` if trial is excluded |
| **Interruption after timer start** | **`timing_valid = false`**; **`decision_time_ms` empty**; **do not pause/resume**; preserve final route choice if given; preserve confidence, ease, and reason if collected; **`protocol_deviation = true`**; describe in `protocol_deviation_note`; trial remains eligible for non-timing analyses unless separately excluded |
| Researcher error | `protocol_deviation = true`; describe in `protocol_deviation_note`; exclude trial if stimulus or instruction was wrong |
| Stimulus not fully visible | Do not start timer; fix display; restart trial only if authorized by session protocol; otherwise exclude |
| Timer failure | `protocol_deviation = true`; `timing_valid = false`; `decision_time_ms` empty; do **not** reconstruct time without a pre-defined independent timestamp |

**Any interruption after timer start makes `timing_valid = false`.** Do not compute comparable decision time from pause/resume or estimated active time.

Invalid timing alone does **not** automatically make a trial incomplete if the final answer and other required post-stop fields were recorded.

Invalid timing alone excludes the trial only from **timing analysis**, not automatically from all non-timing summaries, when `excluded = false`.

No product decision-time threshold is set by this pack. Record actual `decision_time_ms` for descriptive analysis only when `timing_valid = true`.

---

## H. Questionnaire

Maximum six participant-facing questions.

### Per trial — timed and post-stop (questions 1–4)

**Q1 — timed response (not repeated after timer stop)**

Q1 is the participant's **final timed answer** to the route-choice task. It is **not** a separate question asked after timing ends.

The researcher records the same final answer in `selected_route_id`:

| Final answer | `selected_route_id` |
|--------------|---------------------|
| Route A | `Route A` |
| Route B | `Route B` |
| Cannot decide | `cannot_decide` |

Allowed values are exactly `Route A`, `Route B`, and `cannot_decide`. Do not use `A`, `B`, or other abbreviations.

After the stop event, ask only Q2–Q4. **Do not ask Q1 again.**

**Q2.** How confident are you in your choice?

- Scale 1–7 (1 = not confident, 7 = very confident)
- Record: `confidence_1_7`
- Ask even when `selected_route_id = cannot_decide`

**Q3.** How easy was it to make this decision?

- Scale 1–7 (1 = very difficult, 7 = very easy)
- Record: `ease_1_7`

**Q4.** What is the main reason for your choice?

- One option from the closed reason list (Section I) plus `other`
- Record: `reason_code`

### Post-session (questions 5–6)

Ask once after the participant's trials end (after all trials that will be run, which may be fewer than eight).

**Post-session storage rule:**

> Exactly one existing participant row may contain post-session responses.
>
> That row **MUST** be the participant's **last actually recorded trial row**.
>
> It may be completed, incomplete, or excluded.
>
> No earlier row may have `post_session_record = true`.
>
> No artificial trial row may be created.

**Q5.** What, if anything, was unclear during the session?

- Free text without personal identifiers
- Record: `post_unclear_text` only when `post_session_record = true` on the last actually recorded row
- Empty in all other rows

**Q6.** Which format helped you decide faster: neutral route rows, Decision Compression, or neither?

- Allowed answers: `neutral_rows`, `decision_compression`, `neither`, `unsure`
- Record: `post_format_helped` only when `post_session_record = true` on the last actually recorded row
- Empty in all other rows

Set `post_session_record = true` on exactly one row **only when** the post-session questionnaire was actually administered. Otherwise every row has `post_session_record = false` and both post-session fields are empty.

Do **not** store Q5 or Q6 in `researcher_note`.

**Partial-session example:** If the session ends after trial 3, only rows 1–3 exist; rows 4–8 are **not** created. If post-session was administered, row 3 has `post_session_record = true`. If post-session was not administered, rows 1–3 all have `post_session_record = false`.

### Prohibited questions

Do not ask about liking MARSHIO, trusting the brand, agreeing with MARSHIO, purchase intent, app usage intent, or preference for Google.

Trust, acceptance, and stated purchase intent are **not** primary outcomes.

---

## I. Reason categories

Closed list for Q4 (`reason_code`):

| Code | Participant-facing label |
|------|--------------------------|
| `shortest_eta` | Shortest arrival time |
| `lower_toll` | Lower toll |
| `no_toll` | No toll / zero toll |
| `routes_equivalent` | Routes seemed equivalent |
| `insufficient_information` | Not enough information shown |
| `did_not_understand` | I did not understand the task |
| `other` | Other |

Do not add corridor preference, smoothness, stress, or brand-related reasons.

---

## J. Completion and exclusion rules

### Completed trial

A trial is `completed = true` when:

- the correct stimulus and condition were shown;
- the participant gave a final answer (`Route A`, `Route B`, or `cannot_decide`);
- Q2–Q4 were recorded (Q1 is the timed final answer recorded in `selected_route_id`);
- timing start/stop rules were followed or deviation was documented.

For `expected_outcome = INSUFFICIENT_DATA`:

- the participant is **not** required to choose Route A or Route B;
- `cannot_decide` is a valid completed final answer;
- `cannot_decide` does **not** automatically make the trial incomplete;
- `cannot_decide` does **not** automatically make the trial excluded;
- `mode_consistent` remains empty in v0.1;
- `cannot_decide` is analyzed descriptively as its own response category.

`cannot_decide` is **not** declared correct or mode-consistent without a separate contract rule.

### Incomplete trial

`completed = false` when there is no final answer (`selected_route_id` empty) or required post-stop fields (Q2–Q4) are missing.

For every actually recorded row, `timing_valid` must be `true` or `false` — never empty.

Leave `decision_time_ms`, `confidence_1_7`, `ease_1_7`, `reason_code`, or `mode_consistent` empty when not collected or not applicable. Do **not** convert missing values to `0` or `false` automatically (except mandatory booleans: `timing_valid`, `completed`, `excluded`, `protocol_deviation`, `post_session_record`).

A trial may have `completed = true`, `timing_valid = false`, and `excluded = false` when the final answer and Q2–Q4 were recorded but timing is invalid. Such a trial remains eligible for non-timing analyses.

### Protocol deviation

`protocol_deviation = true` when the researcher:

- gave non-script explanation;
- showed the wrong case, mode, or condition;
- violated assignment or trial order;
- started or stopped timing incorrectly;
- allowed an interruption after timer start (timing becomes invalid).

Describe in `protocol_deviation_note`.

### Exclusion

`excluded = true` when a trial must not enter primary feasibility summaries.

Common `exclusion_reason` values:

- `no_final_choice`
- `missing_required_fields`
- `wrong_stimulus`
- `wrong_condition`
- `researcher_withdrawal_stop`

`timing_invalid` alone does **not** require `excluded = true`. Invalid timing excludes the trial from timing analysis only.

Excluded trials remain in the CSV. Do not delete rows silently.

This pack does not define replacement participants or quotas.

---

## K. Data dictionary

Use `docs/stage-a-data-sheet-v0.1.csv` — **23 fields**, header only, no participant rows in the committed template.

| Field | Type | Rule |
|-------|------|------|
| `participant_id` | string | Anonymous pseudonym only. No legal name, phone, email, or exact geolocation. |
| `assignment_group` | string | `G1` or `G2` per Section E |
| `trial_order` | integer | 1–8 per normative group-specific order for rows that actually exist |
| `case_id` | string | `A-F1` … `A-N4` |
| `mode` | string | `FASTEST` or `NO_TOLLS` from truth table |
| `condition` | string | `Baseline` or `Treatment` |
| `expected_outcome` | string | `RECOMMEND`, `EQUIVALENT`, or `INSUFFICIENT_DATA` from committed truth table |
| `selected_route_id` | string | `Route A`, `Route B`, `cannot_decide`, or empty only if no final answer |
| `mode_consistent` | string | `true`, `false`, or empty per outcome rules below |
| `decision_time_ms` | integer | Milliseconds from start to final answer; **empty when `timing_valid = false`** |
| `timing_valid` | boolean | **`true` or `false` — required for every existing row; empty prohibited** |
| `confidence_1_7` | integer | 1–7 or empty |
| `ease_1_7` | integer | 1–7 or empty |
| `reason_code` | string | Closed code from Section I or empty |
| `completed` | boolean | `true` / `false` — required for every existing row |
| `excluded` | boolean | `true` / `false` — required for every existing row |
| `exclusion_reason` | string | Required when `excluded = true`; otherwise empty |
| `protocol_deviation` | boolean | `true` / `false` — required for every existing row |
| `protocol_deviation_note` | string | Free text without personal data; one logical CSV field |
| `post_session_record` | boolean | `true` only on the single last actually recorded row when post-session was administered; `false` on every other row; may be `false` on all rows if post-session was not administered |
| `post_unclear_text` | string | Q5 free text without personal data; populated only when `post_session_record = true`; otherwise empty |
| `post_format_helped` | string | Q6: `neutral_rows`, `decision_compression`, `neither`, `unsure`, or empty if unanswered; populated only when `post_session_record = true`; otherwise empty |
| `researcher_note` | string | Optional trial/session note without personal data; **not** for Q5 or Q6 |

### `selected_route_id` values

| Value | Use |
|-------|-----|
| `Route A` | Final choice of Route A — exact committed fixture label |
| `Route B` | Final choice of Route B — exact committed fixture label |
| `cannot_decide` | Participant cannot decide from shown information |
| *(empty)* | No final answer |

Do not use `A`, `B`, or other abbreviations.

### `mode_consistent` computation

Apply **after** the participant's final answer using the committed truth table. Never announce to the participant during the trial.

| `expected_outcome` | Expected winner | `mode_consistent` |
|--------------------|-----------------|-------------------|
| `RECOMMEND` | Route A or Route B per case | `true` if `selected_route_id` equals expected winner; `false` if the other route was chosen; **`false` if `cannot_decide`** |
| `EQUIVALENT` | None | `true` if `selected_route_id` is `Route A` or `Route B`; **empty if `cannot_decide`**; empty if no final answer |
| `INSUFFICIENT_DATA` | None | **empty** for `Route A`, `Route B`, or `cannot_decide` — no mode-consistent route is defined by the truth table |

Empty `mode_consistent` is **not** automatically `false`. `cannot_decide` on INSUFFICIENT_DATA and EQUIVALENT cases is analyzed descriptively and must not silently enter the binary mode-consistent rate denominator.

### CSV escaping rules (future data entry)

When filling the data sheet during a future authorized session:

- Any value containing comma, double quote, CR, or LF must be wrapped in double quotes.
- An embedded double quote is escaped as two double quotes (`""`).
- `post_unclear_text`, `protocol_deviation_note`, and `researcher_note` must remain **one logical CSV field** each.
- An unescaped newline must not create an additional record.
- A missing value is an empty field — not the literal strings `"null"`, `0`, or `false`.

### Privacy

Do not collect Google identifiers, coordinates, polylines, API payloads, or participant contact details in the CSV.

---

## L. Analysis plan

Descriptive feasibility analysis only. No p-values, significance claims, causal product claims, market claims, production thresholds, or ROI conclusions.

Every rate must be reported as **numerator / denominator**. Do not publish percentages without numerator and denominator.

Example: `Mode-consistent choices: 12/16`

### Normative analysis denominators

#### Choice / mode-consistent analysis

Include only trials where:

```
completed = true
excluded = false
mode_consistent is not empty
```

Report: `Mode-consistent choices: [numerator]/[denominator]`

Separately publish descriptive response counts for `Route A`, `Route B`, and `cannot_decide` by:

- `condition`
- `expected_outcome`
- `mode`
- `case_id` (for case-level review)

`cannot_decide` on INSUFFICIENT_DATA and EQUIVALENT cases must **not** silently enter the binary mode-consistent rate denominator when `mode_consistent` is empty.

#### Timing analysis

Include only trials where:

```
completed = true
excluded = false
timing_valid = true
decision_time_ms is present
```

Report median and other timing summaries with explicit denominator count.

`protocol_deviation = true` alone does **not** exclude a trial from timing analysis if timing remains trustworthy (`timing_valid = true`).

Interruption after timer start (`timing_valid = false`) excludes the trial from timing analysis only.

#### Confidence analysis

Include only trials where:

```
completed = true
excluded = false
confidence_1_7 is present
```

Always report denominator.

#### Ease analysis

Include only trials where:

```
completed = true
excluded = false
ease_1_7 is present
```

Always report denominator.

#### Reason analysis

Include only trials where:

```
completed = true
excluded = false
reason_code is present
```

Always report denominator.

#### Post-session analysis

Include **one observation per participant** where:

```
post_session_record = true
```

- analyze `post_unclear_text` descriptively;
- count `post_format_helped` by closed categories (`neutral_rows`, `decision_compression`, `neither`, `unsure`);
- denominator = number of participants with `post_session_record = true` (0 or 1 per `participant_id`).

If no row has `post_session_record = true`, post-session fields are empty in all rows and post-session analysis denominator is 0.

Do **not** treat eight trial rows as eight post-session observations. Do **not** assume `trial_order = 8` is the post-session row.

### Pre-specified outputs

| Output | Description |
|--------|-------------|
| Completed trials by condition | Count Baseline vs Treatment |
| Missing / incomplete trials | `completed = false` by case and condition |
| Exclusions | Count and `exclusion_reason` breakdown |
| Protocol deviations | Count and note review |
| Mode-consistent choice rate by condition | Numerator/denominator per filter above |
| Response counts (`Route A` / `Route B` / `cannot_decide`) | By condition, outcome, mode, case |
| Median `decision_time_ms` by condition | Timing denominator only |
| Confidence distribution | Confidence denominator only |
| Ease distribution | Ease denominator only |
| Reason-category counts | Reason denominator only |
| Post-session format preference | One row per participant where `post_session_record = true` |
| FASTEST / NO_TOLLS split | Repeat key outputs by `mode` |
| Case-level anomalies | Cases with high incomprehension, deviations, or invalid timing |
| Baseline ceiling / floor indicators | Very high Baseline accuracy or very low Baseline accuracy patterns |

### Allowed charts

- Choice consistency by condition (with numerator/denominator)
- Median decision time by condition (timing-valid trials only)
- Confidence and ease distributions
- Missing / exclusion / deviation counts
- `cannot_decide` counts by outcome and condition
- Case-level comparison tables

### Not allowed

p-values; claims of statistical significance; "MARSHIO is better" conclusions; purchase or adoption forecasts; using Stage A timing data as a GO/KILL gate.

---

## M. Allowed and forbidden conclusions

### Stage A may conclude

- Procedure is understandable or needs revision
- Measurement is feasible or infeasible
- Specific stimuli are ambiguous
- Baseline is too easy or too hard for feasibility purposes
- Timing procedure works or does not work
- Data are suitable or unsuitable for planning Stage B

### Stage A may not conclude

- MARSHIO is proven as a product
- MARSHIO is better than Google
- Decision Compression is statistically effective
- Users will adopt or buy the product
- 3-minute / AED 4 thresholds are market-validated
- Stage B is automatically authorized
- Implementation is authorized

---

## N. Owner decisions still required

The following remain **unapproved** and must not be filled in by this pack:

- Participant population and inclusion criteria
- Participant count
- Recruitment method
- Informed consent process and legal text
- Session location or remote format
- Product decision-time limit
- Acceptable baseline ceiling for progression
- Minimum meaningful uplift for Stage B
- Stage B GO/KILL scope
- Stage A feasibility success criteria
- **Authorization to execute Stage A**

---

## O. Pre-session checklist

| # | Check |
|---|-------|
| 1 | Correct `assignment_group` loaded for this `participant_id` |
| 2 | Correct `case_id` and `condition` for each `trial_order` |
| 3 | Stimulus text matches Section D for case and condition |
| 4 | Full stimulus visible before timing starts |
| 5 | Timer or stopwatch ready |
| 6 | No Google content in materials |
| 7 | No personal identifiers collected |
| 8 | Standard moderator script available |
| 9 | Researcher prepared not to add explanations |
| 10 | `docs/stage-a-data-sheet-v0.1.csv` ready (23-field header) |
| 11 | Plan to document deviations and exclusions in CSV |
| 12 | Post-session will use `post_session_record` on last actually recorded row only |

---

## P. Researcher-only dry-run verification

This section verifies internal reproducibility of the procedure and data schema. It is **not** Stage A execution, uses **no real participants**, and produces **no behavioural evidence**. Dry-run rows exist **only in this Markdown** — not in the CSV template.

Synthetic identifiers: `DRY-G1`, `DRY-G2`.

### Partial-session integrity example (schema only)

If a participant stops after trial 3:

- only rows with `trial_order` 1–3 exist;
- rows 4–8 are **not** created;
- if post-session was administered, row 3 has `post_session_record = true` and post-session fields;
- if post-session was not administered, rows 1–3 all have `post_session_record = false` and empty post-session fields;
- no artificial trial rows are created.

This is not a third dry run and does not require a full 23-column table.

---

### Dry Run G1 — full simulation (`DRY-G1`)

Normative G1 order. Includes: normal Route A; normal Route B; `cannot_decide` on INSUFFICIENT_DATA (trial 2); interruption (trial 4); fully excluded trial (trial 8); post-session on last actually recorded row (trial 8).

| participant_id | assignment_group | trial_order | case_id | mode | condition | expected_outcome | selected_route_id | mode_consistent | decision_time_ms | timing_valid | confidence_1_7 | ease_1_7 | reason_code | completed | excluded | exclusion_reason | protocol_deviation | protocol_deviation_note | post_session_record | post_unclear_text | post_format_helped | researcher_note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DRY-G1 | G1 | 1 | A-F1 | FASTEST | Baseline | RECOMMEND | Route A | true | 4200 | true | 6 | 5 | shortest_eta | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 2 | A-F3 | FASTEST | Treatment | INSUFFICIENT_DATA | cannot_decide | (empty) | 5100 | true | 4 | 3 | insufficient_information | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 3 | A-F2 | FASTEST | Baseline | EQUIVALENT | Route A | true | 3800 | true | 5 | 6 | routes_equivalent | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 4 | A-F4 | FASTEST | Treatment | RECOMMEND | Route A | true | (empty) | false | 5 | 4 | shortest_eta | true | false | (empty) | true | Phone buzz interrupted trial after timer start | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 5 | A-N1 | NO_TOLLS | Baseline | RECOMMEND | Route B | true | 4500 | true | 6 | 5 | lower_toll | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 6 | A-N3 | NO_TOLLS | Treatment | INSUFFICIENT_DATA | Route B | (empty) | 6200 | true | 3 | 4 | insufficient_information | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 7 | A-N2 | NO_TOLLS | Baseline | RECOMMEND | Route A | true | 3900 | true | 7 | 6 | shortest_eta | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G1 | G1 | 8 | A-N4 | NO_TOLLS | Treatment | EQUIVALENT | (empty) | (empty) | (empty) | false | (empty) | (empty) | (empty) | false | true | no_final_choice | false | (empty) | true | FASTEST vs NO_TOLLS label switching | decision_compression | Participant withdrew mid-trial |

**Dry Run G1 schema check:** 8 rows; 23 columns; `trial_order` 1–8 exactly once; normative G1 case/condition order; all boolean fields explicit. **Pass.**

**Post-session integrity (`DRY-G1`):** `count(post_session_record = true) = 1` (trial 8 only); trial 8 has maximum `trial_order`; rows 1–7 have `post_session_record = false` and empty post-session fields. **Pass.**

#### G1 reproducible analysis arithmetic

**Choice / mode-consistent analysis**

- Included `trial_order`: 1, 3, 4, 5, 7
- Excluded `trial_order`: 2 (empty `mode_consistent`), 6 (empty `mode_consistent`), 8 (`excluded = true`)
- Exclusion reasons: trial 8 → `no_final_choice`
- Numerator (`mode_consistent = true`): 5
- Denominator: 5
- Result: **5/5**

**Timing analysis**

- Included `trial_order`: 1, 2, 3, 5, 6, 7
- Excluded `trial_order`: 4 (`timing_valid = false`), 8 (`excluded = true`)
- Exclusion reasons: trial 8 → `no_final_choice`
- Numerator: n/a (median descriptive)
- Denominator: 6
- Result: median of {4200, 5100, 3800, 4500, 6200, 3900} ms = **4350 ms** (6 trials)

**Confidence analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7
- Excluded `trial_order`: 8 (`excluded = true`, `confidence_1_7` empty)
- Exclusion reasons: trial 8 → `no_final_choice`
- Denominator: 7

**Ease analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7
- Excluded `trial_order`: 8
- Exclusion reasons: trial 8 → `no_final_choice`
- Denominator: 7

**Reason analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7
- Excluded `trial_order`: 8
- Exclusion reasons: trial 8 → `no_final_choice`
- Denominator: 7

**Post-session analysis**

- Included: row with `post_session_record = true` → trial 8 only
- Denominator: 1
- `post_format_helped`: `decision_compression` (1/1)

---

### Dry Run G2 — full simulation (`DRY-G2`)

Normative G2 order. All eight trials completed; none excluded; no protocol deviations; all timing valid; `cannot_decide` on INSUFFICIENT_DATA trials 2 and 6; post-session on trial 8.

| participant_id | assignment_group | trial_order | case_id | mode | condition | expected_outcome | selected_route_id | mode_consistent | decision_time_ms | timing_valid | confidence_1_7 | ease_1_7 | reason_code | completed | excluded | exclusion_reason | protocol_deviation | protocol_deviation_note | post_session_record | post_unclear_text | post_format_helped | researcher_note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DRY-G2 | G2 | 1 | A-F1 | FASTEST | Treatment | RECOMMEND | Route A | true | 4400 | true | 6 | 6 | shortest_eta | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 2 | A-F3 | FASTEST | Baseline | INSUFFICIENT_DATA | cannot_decide | (empty) | 5600 | true | 4 | 3 | insufficient_information | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 3 | A-F2 | FASTEST | Treatment | EQUIVALENT | Route B | true | 4100 | true | 5 | 5 | routes_equivalent | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 4 | A-F4 | FASTEST | Baseline | RECOMMEND | Route A | true | 4000 | true | 6 | 5 | shortest_eta | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 5 | A-N1 | NO_TOLLS | Treatment | RECOMMEND | Route B | true | 4700 | true | 7 | 6 | lower_toll | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 6 | A-N3 | NO_TOLLS | Baseline | INSUFFICIENT_DATA | cannot_decide | (empty) | 5800 | true | 3 | 4 | insufficient_information | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 7 | A-N2 | NO_TOLLS | Treatment | RECOMMEND | Route A | true | 4200 | true | 6 | 6 | shortest_eta | true | false | (empty) | false | (empty) | false | (empty) | (empty) | (empty) |
| DRY-G2 | G2 | 8 | A-N4 | NO_TOLLS | Baseline | EQUIVALENT | Route A | true | 4300 | true | 5 | 5 | routes_equivalent | true | false | (empty) | false | (empty) | true | Nothing major | neutral_rows | (empty) |

**Dry Run G2 schema check:** 8 rows; 23 columns; `trial_order` 1–8 exactly once; normative G2 case/condition order; all boolean fields explicit. **Pass.**

**Post-session integrity (`DRY-G2`):** `count(post_session_record = true) = 1` (trial 8 only); rows 1–7 have empty post-session fields. **Pass.**

#### G2 reproducible analysis arithmetic

**Choice / mode-consistent analysis**

- Included `trial_order`: 1, 3, 4, 5, 7, 8
- Excluded `trial_order`: 2 (empty `mode_consistent`), 6 (empty `mode_consistent`)
- Exclusion reasons: none
- Numerator (`mode_consistent = true`): 6
- Denominator: 6
- Result: **6/6**

**Timing analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7, 8
- Excluded `trial_order`: none
- Exclusion reasons: none
- Denominator: 8
- Result: median of {4400, 5600, 4100, 4000, 4700, 5800, 4200, 4300} ms = **4350 ms** (8 trials)

**Confidence analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7, 8
- Excluded `trial_order`: none
- Denominator: 8

**Ease analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7, 8
- Excluded `trial_order`: none
- Denominator: 8

**Reason analysis**

- Included `trial_order`: 1, 2, 3, 4, 5, 6, 7, 8
- Excluded `trial_order`: none
- Denominator: 8

**Post-session analysis**

- Included: trial 8 (`post_session_record = true`)
- Denominator: 1
- `post_format_helped`: `neutral_rows` (1/1)

---

### Dry-run validation checklist

| # | Check | Result |
|---|-------|--------|
| 1 | No artificial trial rows for post-session | Pass |
| 2 | `post_session_record` on last actually recorded row only | Pass |
| 3 | Full G1 table — 8 rows × 23 columns | Pass |
| 4 | Full G2 table — 8 rows × 23 columns | Pass |
| 5 | Interruption (G1 trial 4) and exclusion (G1 trial 8) are different rows | Pass |
| 6 | All denominators computed from tables above | Pass |
| 7 | `timing_valid` never empty on existing rows | Pass |
| 8 | Partial-session example does not create fictitious rows | Pass |

---

## Consistency verification

| # | Check | Result |
|---|-------|--------|
| 1 | Candidate B fully excluded from pilot | Pass |
| 2 | All eight Dataset A cases represented | Pass |
| 3 | No participant sees same case in both conditions | Pass — Section E |
| 4 | Every case appears in both conditions across groups | Pass — G1/G2 map |
| 5 | FASTEST and NO_TOLLS in both conditions | Pass — 2+2 per group |
| 6 | Truth not shown to participant | Pass — researcher-only fields |
| 7 | Time measured objectively by researcher | Pass — Section G |
| 8 | `mode_consistent` derived from truth table | Pass — Section K |
| 9 | Missing / exclusions / `cannot_decide` not auto-coded as wrong | Pass |
| 10 | 3 min / AED 4 named as synthetic contract constants only | Pass — Sections A, D |
| 11 | Stage A has no product GO/KILL authority | Pass — Sections A, M |
| 12 | Stage A execution remains Not started | Pass — header |
| 13 | Implementation remains Not authorized | Pass — header |
| 14 | `cannot_decide` supported in all conditions | Pass — Sections D, F, G, H, J, K |
| 15 | Post-session uses `post_session_record` on last actually recorded row | Pass — Sections H, K, P |
| 16 | Normative trial order without contradiction | Pass — Section E, P |
| 17 | Analysis denominators defined and reproducible | Pass — Sections L, P |
| 18 | CSV header has exactly 23 unique fields | Pass — Section K |
| 19 | No artificial trial rows for post-session | Pass — Sections H, P |
| 20 | `timing_valid` mandatory on every existing row | Pass — Sections G, J, K |

**Blockers found:** None (after targeted revision #2)

---

## Pack status

| Item | Status |
|------|--------|
| Stage A Experiment Pack v0.1 | **Candidate — pending another independent Codex review** |
| Stage A execution | **Not started** |
| Implementation | **Not authorized** |

This pack is **not** commit-ready until another independent Codex review passes.

---

## Related artifacts

| Artifact | Location |
|----------|----------|
| Decision Compression Contract v0.1 | `docs/stage-a-decision-compression-contract-v0.1.md` |
| Synthetic Cases v0.1 | `docs/stage-a-synthetic-cases-v0.1.md` |
| Data sheet template | `docs/stage-a-data-sheet-v0.1.csv` |
| Research Decision Record 001 | `docs/research-decision-record-001-staged-validation.md` |
