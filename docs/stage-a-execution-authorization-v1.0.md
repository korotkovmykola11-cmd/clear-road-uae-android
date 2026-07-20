# Stage A Pilot Execution Authorization v1.0

| Field | Value |
|-------|-------|
| **Title** | Stage A Pilot Execution Authorization v1.0 |
| **Decision type** | Research execution gate |
| **Document status** | Candidate — pending Product Owner approval |
| **Stage A debug prototype** | Completed |
| **Stage A participant pilot** | Not started |
| **Stage B** | Not started |
| **Production implementation** | Not authorized |
| **Participant research authorization** | NOT AUTHORIZED |

---

## 1. Purpose

This document decides **only** whether the Stage A synthetic feasibility pilot may begin with human participants using the **candidate protocol** (versioned Experiment Pack artifact) and the committed debug research instrument.

It does **not** authorize:

- Stage B design or execution;
- Google-content research;
- waypoint research;
- production implementation;
- release deployment;
- external API use;
- Google API use;
- collection of data outside the **current 23-field candidate schema** (`docs/stage-a-data-sheet-v0.1.csv`).

The completed owner smoke and committed debug prototype establish **technical readiness of the research instrument**. They do **not** establish participant-study authorization by themselves.

---

## 2. Stage A pilot objective

Stage A follows the **staged validation methodology approved in Research Decision Record 001** and the versioned Stage A Experiment Pack v0.1 candidate artifact.

The pilot objective is limited to **measurement feasibility**:

- verify that participants understand the route-choice task;
- verify that synthetic stimuli can be presented consistently;
- verify that choices and questionnaire answers can be captured;
- verify that decision timing can be measured reliably under researcher control;
- observe missing responses;
- observe exclusions;
- observe protocol deviations;
- obtain preliminary baseline and treatment behaviour for planning;
- obtain planning estimates for a possible Stage B design.

Stage A **cannot** produce a product GO/KILL decision.

Stage A **cannot** establish statistical effectiveness.

Stage A **cannot** authorize Stage B automatically.

---

## 3. Research question

Can the **candidate Stage A protocol** (versioned protocol artifact) be executed consistently with the intended participant population, producing interpretable choice, timing, confidence, ease and reason data without unacceptable missingness or protocol failure?

This question is **not**:

- Does MARSHIO work?
- Does MARSHIO improve decisions?
- Would users buy MARSHIO?

Those questions are outside Stage A.

---

## 4. Defined stimuli and conditions

Stage A uses **Candidate A only**:

- Baseline neutral route rows;
- MARSHIO Decision Compression treatment;
- Dataset A synthetic cases (8 cases);
- FASTEST and NO_TOLLS modes.

**Excluded** from Stage A:

- Candidate B / Explicit Preference;
- Google content, Google Maps, Google screenshots, polylines, coordinates, route IDs;
- GraphHopper;
- real traffic and real routes;
- GPS and live APIs;
- production data;
- participant-supplied origins or destinations.

Normative case order, G1/G2 assignment tables, and truth tables remain as committed in `docs/stage-a-synthetic-cases-v0.1.md` and `docs/stage-a-experiment-pack-v0.1.md`. This document does not alter them. **Product Owner approval** for participant use of these stimuli and for execution under the Experiment Pack remains **UNRESOLVED** (Section 22).

---

## 5. Participant eligibility

### Required characteristics (recommended minimum)

- adult;
- able to understand the study language;
- capable of interpreting basic route choices involving ETA and toll;
- familiar enough with road navigation to understand FASTEST and NO_TOLLS modes;
- able to provide informed consent;
- not currently driving during the study.

### Exclusions (minimum)

- participant is driving or operating machinery;
- cannot provide informed consent;
- cannot understand the task instruction;
- researcher has a personal or supervisory relationship that makes voluntary participation doubtful;
- accessibility need cannot be supported by the current protocol;
- participant previously saw the complete Stage A truth table or researcher-only answers;
- technical condition prevents reliable timing or response capture.

This document does **not** require UAE citizenship unless the Product Owner separately approves that constraint.

The pilot sample must **not** be described as representing all UAE drivers unless the Product Owner explicitly approves that claim.

---

## 6. Participant population decision

**OWNER DECISION REQUIRED**

Required decision: **Who is the intended Stage A feasibility population?**

Dimensions for Product Owner decision (no automatic selection):

- UAE resident drivers;
- frequent UAE drivers;
- licensed drivers;
- language;
- navigation-app familiarity;
- age boundary;
- driving frequency.

---

## 7. Pilot size

Stage A is **not** a powered experiment.

**Recommended default (recommendation only, not approved):** **5 participants**

Purpose of the recommendation:

- detect obvious protocol failures;
- observe repeated comprehension problems;
- exercise both G1 and G2 assignment;
- estimate missingness and timing behaviour;
- assess whether the protocol is usable before Stage B planning.

**Pilot participant count:** **OWNER DECISION REQUIRED**

Recruitment must **not** proceed with an unresolved participant count.

---

## 8. Assignment rule

From the Experiment Pack (normative):

- each participant sees each case exactly once across eight trials;
- each participant receives four Baseline and four Treatment trials;
- trial order is determined by G1 or G2 assignment group;
- no researcher-selected condition switching;
- no case repetition except exclusion and authorized restart per protocol;
- no participant sees both Baseline and Treatment for the same case.

**Normative G1/G2 mapping** follows the **versioned assignment rule** in `docs/stage-a-experiment-pack-v0.1.md` Section E (including reproducible assignment from synthetic `participant_id` or pre-defined `assignment_group`). Product Owner approval to use this rule for participant execution remains **UNRESOLVED** (Section 22: Stage A Experiment Pack approved for participant execution).

**Recommended pilot allocation across enrolment order (recommendation only):**

| Enrolment order | Assignment group |
|-----------------|------------------|
| Participant 1 | G1 |
| Participant 2 | G2 |
| Participant 3 | G1 |
| Participant 4 | G2 |
| Participant 5 | G1 |

Assignment must **not** be derived from personal information. If the Product Owner approves a different allocation rule, record it here before recruitment.

---

## 9. Researcher

Record before first participant session:

- who will conduct sessions;
- whether one or multiple researchers;
- whether all researchers have read the Experiment Pack;
- whether they completed an owner/researcher dry run;
- agreement not to explain the correct answer;
- agreement not to coach participants.

If researcher identity or count is not approved:

**OWNER DECISION REQUIRED**

**Recommended constraint (recommendation only):** one trained researcher for all initial feasibility sessions, to reduce instruction and timing variance.

---

## 10. Study location and format

**OWNER DECISION REQUIRED** on:

- in person or remote;
- device used (emulator, dedicated Android device, or researcher-controlled phone);
- seated, non-driving environment;
- language of instruction;
- whether the researcher or participant operates the device.

**Minimum safety constraint (mandatory):** the study must never be conducted while the participant is driving.

---

## 11. Consent and privacy boundary

This document does **not** provide legal guarantees.

Minimum operational privacy boundary:

- participation is voluntary;
- participant may stop at any time;
- no participant drives during the study;
- only synthetic route content is shown;
- no Google content is shown;
- no actual location, coordinates, or route history is collected;
- no API data is collected;
- no audio, video, or screen recording unless separately authorized;
- no name, phone number, email, or account identifier;
- **synthetic participant ID only**.

A short participant-facing consent statement is required before the first trial.

**Consent wording:** **OWNER DECISION REQUIRED**

Participant execution cannot be authorized without approved consent wording.

---

## 12. Candidate data fields (23-field schema)

Stage A may collect **only** fields defined in the **current 23-field candidate schema** (`docs/stage-a-data-sheet-v0.1.csv` and the Experiment Pack). **No additional fields.** Product Owner approval of this schema for participant data collection is **UNRESOLVED** until recorded in Section 22 (with protocol and governance approvals).

| Field | Role |
|-------|------|
| participant_id | Synthetic identifier only |
| assignment_group | G1 or G2 |
| trial_order | 1–8 per session |
| case_id | Dataset A case |
| mode | FASTEST or NO_TOLLS |
| condition | Baseline or Treatment |
| expected_outcome | Researcher/analysis field (truth table) |
| selected_route_id | Participant final choice |
| mode_consistent | Researcher/analysis field |
| decision_time_ms | Timing (empty when invalid) |
| timing_valid | Boolean |
| confidence_1_7 | Post-choice |
| ease_1_7 | Post-choice |
| reason_code | Post-choice |
| completed | Trial completion flag |
| excluded | Exclusion flag |
| exclusion_reason | When excluded |
| protocol_deviation | Deviation flag |
| protocol_deviation_note | Factual note |
| post_session_record | Single true row per session |
| post_unclear_text | Post-session Q5 |
| post_format_helped | Post-session Q6 |
| researcher_note | Factual; no personal data |

Free-text fields must not contain names, contact details, or real locations.

---

## 13. Storage and access

**OWNER DECISION REQUIRED** for:

- where exported CSV files are stored;
- filename convention;
- who may access files;
- backup policy;
- retention period;
- deletion process;
- encryption at rest;
- handling of accidental personal data.

**Recommended minimum (recommendation only, not approved):**

- local owner-controlled folder;
- no Git, no repository, no cloud sync unless explicitly approved;
- no email or messaging-app transfer;
- access limited to Product Owner and designated researcher;
- one CSV per participant with synthetic ID in filename;
- research log kept separately without personal identity mapping unless absolutely necessary.

Participant execution cannot be authorized while storage, access, or retention remain unresolved.

---

## 14. Session procedure

Follow the committed Experiment Pack sequence:

1. confirm consent;
2. assign synthetic participant ID;
3. assign G1 or G2 per the versioned Experiment Pack assignment rule;
4. read standard instruction;
5. present each synthetic trial;
6. start timing only after full stimulus visibility and completed instruction;
7. collect final choice;
8. collect confidence, ease, reason;
9. record interruptions and deviations;
10. complete or terminate session;
11. administer post-session questions;
12. export CSV through SAF;
13. verify CSV exists and is readable;
14. store per the approved storage policy (once recorded in Section 22);

Researchers must **not**:

- reveal expected outcomes or correct routes;
- coach participants;
- change trial order;
- reinterpret answers;
- repair missing data from memory;
- fabricate decision times;
- convert missing values to zero;
- add fictitious future trial rows.

---

## 15. Technical readiness evidence

The following **technical** evidence is recorded as satisfied for the debug instrument (not participant feasibility):

| Evidence | Status |
|----------|--------|
| Debug-only Stage A Activity in debug manifest | Verified |
| Release isolation (no Stage A in release manifest/artifact) | Verified |
| Timing integrity unit tests | PASS |
| Immutable timing state transitions | Implemented and tested |
| Deterministic fake-clock test (pre-start wait excluded) | PASS |
| Owner manual smoke (timing, interruption, SAF, CSV parse) | PASS |
| Interruption RUNNING to INVALID | Verified in smoke and tests |
| SAF export flow | PASS |
| 23-field CSV schema | Verified |
| Invalid timing fields when interrupted (trial-level) | Verified in owner smoke |

This evidence does **not** prove:

- participant comprehension;
- measurement feasibility with participants;
- acceptable missingness or deviation rates;
- product value;
- Stage B or production readiness.

---

## 16. Pre-session checklist

Mandatory before each participant session:

- [ ] Current debug APK built from committed prototype installed
- [ ] Device or emulator functional
- [ ] App data cleared for new session
- [ ] Synthetic participant ID prepared
- [ ] G1 or G2 assignment prepared per versioned Experiment Pack rule
- [ ] Researcher has Experiment Pack script
- [ ] Consent completed (participant-facing wording approved in Section 22)
- [ ] Participant is not driving
- [ ] Participant has not seen truth table
- [ ] Stimulus fully visible before timing start
- [ ] Timer control verified
- [ ] SAF export destination available
- [ ] Storage location approved
- [ ] No Google or live content present
- [ ] No personal identifiers entered

---

## 17. Session completion checklist

Mandatory after each session:

- [ ] Expected trials recorded (or early finish documented)
- [ ] Interruptions and deviations recorded
- [ ] Timing validity reviewed
- [ ] Post-session administered or explicitly skipped per protocol
- [ ] CSV exported via SAF
- [ ] CSV file found and readable
- [ ] Header has 23 fields
- [ ] Row count plausible for session outcome
- [ ] Synthetic participant ID correct in file
- [ ] File moved to approved storage
- [ ] Temporary copies handled per approved policy
- [ ] No participant data added to Git

---

## 18. Feasibility metrics

Descriptive metrics only (no p-values, no powered-effect claims, no product GO/KILL):

- recruitment count;
- consented count;
- completed-session count;
- early-stop count;
- missing-choice count;
- cannot-decide count;
- exclusion count;
- protocol-deviation count;
- valid-timing count;
- invalid-timing count;
- mode-consistent rate by condition (explicit denominator);
- median decision time by condition (explicit denominator);
- confidence, ease, and reason-code distributions;
- participant comments;
- case-level anomalies.

---

## 19. Stage A feasibility criteria

### Authorization prerequisites

See Section 22.

### Pilot completion criteria

**OWNER DECISION REQUIRED** for thresholds including:

- acceptable completed-session rate;
- acceptable missing-response rate;
- acceptable protocol-deviation rate;
- minimum valid-timing rate;
- maximum participants unable to understand instruction;
- acceptable technical failure rate;
- rule for revising stimuli;
- rule for repeating Stage A;
- rule for stopping before Stage B.

### Recommendations only (not approval)

Until Product Owner thresholds exist, use descriptive reporting only. Do not convert recommendations into approved gates without explicit Product Owner sign-off.

---

## 20. Stop conditions

### Immediate session-level stops

- participant withdraws or withdraws consent;
- participant appears uncomfortable;
- participant begins driving;
- device unreliable;
- wrong stimulus shown;
- researcher reveals expected answer;
- timing integrity cannot be preserved;
- CSV cannot be safely stored.

### Pilot-level pause conditions

- repeated comprehension failure;
- repeated technical failure;
- repeated protocol deviation;
- evidence of personal-data collection;
- inconsistent researcher instructions;
- corrupted or missing CSV files;
- participant population does not match recruitment criteria once those criteria are approved (Section 22).

Numerical pause thresholds require Product Owner approval if used.

---

## 21. Deviations and exclusions

Preserve committed CSV semantics:

- interruption does not automatically erase the participant final choice;
- invalid timing excludes timing analysis unless the trial is separately excluded;
- exclusion requires a defined exclusion reason code per Experiment Pack;
- missing data remains empty;
- no reconstructed or approximate timing;
- researcher notes remain factual;
- no correction of participant answers after the session.

---

## 22. Authorization prerequisites

**Commit versus approval.** A Git commit proves an artifact exists, is versioned, and can be referenced. It does **not** prove Product Owner approval, participant-execution approval, research-governance approval, or authorization to recruit. Candidate contract and protocol text are **not** approval evidence. Do not infer approval from committed status, owner smoke, implementation completion, or existence of this candidate document.

| Requirement | Status | Evidence / owner decision | Blocking? |
|-------------|--------|-----------------------------|-----------|
| Stage A contracts committed | SATISFIED | Candidate Stage A contract artifacts exist and are versioned in repository; does not prove participant-use approval | Yes |
| Experiment Pack committed | SATISFIED | Candidate Experiment Pack artifact exists and is versioned (`docs/stage-a-experiment-pack-v0.1.md`); does not prove participant-execution approval | Yes |
| Debug prototype committed | SATISFIED | Debug-only Stage A implementation committed (reference `4d50f4483f4c448851cacb65569cadcc45e89d72`); proves instrument existence only | Yes |
| Unit tests passed | SATISFIED | `StageASessionLogicTest` — 20 tests, 0 failures; proves logic/timing integrity in test environment only | Yes |
| Owner smoke passed | SATISFIED | Documented owner manual verification of debug instrument; does not prove participant feasibility or research approval | Yes |
| Release isolation verified | SATISFIED | No Stage A in release manifest/artifact; proves build isolation only | Yes |
| Final Stage A contracts approved for participant use | UNRESOLVED | Explicit Product Owner approval not recorded | Yes |
| Synthetic cases and truth tables approved for participant use | UNRESOLVED | Explicit Product Owner approval not recorded | Yes |
| Stage A Experiment Pack approved for participant execution | UNRESOLVED | Explicit Product Owner approval not recorded | Yes |
| Final candidate-product-hypothesis wording approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Meaningful duration margin approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Meaningful toll difference approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Exact decision-time limit approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Synthetic case design philosophy approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Synthetic stimulus provenance rules approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval of provenance **policy/rules** not recorded (distinct from verification of actual participant-facing stimuli) | Yes |
| Actual stimulus provenance verified and approved | UNRESOLVED | Required evidence not complete; explicit Product Owner approval of actual participant-facing stimulus set not recorded (see below) | Yes |
| Number of synthetic cases approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Mode balance approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Acceptable baseline ceiling approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Primary outcome structure approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Participant population approved | UNRESOLVED | RDR 001 deferred decisions; Section 6 — explicit Product Owner approval not recorded | Yes |
| Research consent requirements approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Participant privacy requirements approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Study-data retention requirements approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Stage A feasibility criteria approved | UNRESOLVED | RDR 001 deferred decisions; Section 19 — explicit Product Owner approval not recorded | Yes |
| Minimum meaningful uplift for Stage B approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Stage B GO/KILL scope approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Stage C2 definition of “effect persists” approved | UNRESOLVED | RDR 001 deferred decisions — explicit Product Owner approval not recorded | Yes |
| Participant-facing consent wording approved | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Participant privacy notice approved | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Pseudonymization method approved | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Recruitment criteria approved | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Participant withdrawal procedure approved | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Applicable research/privacy requirements determination completed | UNRESOLVED | RDR 001 research-governance boundary — explicit Product Owner approval not recorded | Yes |
| Study location and format approved | UNRESOLVED | Section 10 — explicit Product Owner approval not recorded | Yes |
| Researcher identity and count approved | UNRESOLVED | Section 9 — explicit Product Owner approval not recorded | Yes |
| Participant-facing instruction approved | UNRESOLVED | Experiment Pack script — explicit Product Owner sign-off not recorded | Yes |
| Exact pilot participant count approved | UNRESOLVED | Section 7 — explicit Product Owner approval not recorded | Yes |
| First-session date/window approved | UNRESOLVED | Explicit Product Owner schedule decision not recorded | Yes |
| CSV storage location approved | UNRESOLVED | Section 13 — explicit Product Owner approval not recorded | Yes |
| CSV access policy approved | UNRESOLVED | Section 13 — explicit Product Owner approval not recorded | Yes |
| Data retention period approved | UNRESOLVED | Section 13 — explicit Product Owner approval not recorded | Yes |
| Data deletion procedure approved | UNRESOLVED | Section 13 — explicit Product Owner approval not recorded | Yes |
| Accidental personal-data handling procedure approved | UNRESOLVED | Section 13 — explicit Product Owner approval not recorded | Yes |

#### Actual stimulus provenance verified and approved — required evidence and satisfaction rule

**Distinction:** *Synthetic stimulus provenance rules approved* covers Product Owner approval of the provenance **policy/rules** required by RDR 001. *Actual stimulus provenance verified and approved* covers the **concrete participant-facing stimulus artifacts** that would actually be used during Stage A execution.

Required evidence (all four):

1. Source of every participant-facing synthetic stimulus is documented.
2. Confirmation that no Google content, Google-derived content, live route content, external-provider route content, screenshots, coordinates, polylines, route responses, or cached provider content was used.
3. Confirmation that every participant-facing stimulus matches the committed synthetic cases and truth tables.
4. Explicit Product Owner approval of the exact stimulus set intended for participant execution.

**Satisfaction rule:** **SATISFIED** only when all four required evidence elements are documented **and** explicit Product Owner approval of the actual participant-facing stimulus set is recorded. Otherwise **UNRESOLVED**.

Do not infer provenance from the word “synthetic”, a Git commit, existence of files, debug implementation, owner smoke, absence of an obvious Google reference, or approval of provenance rules alone. Absence of contrary evidence is not provenance verification.

### Prerequisite arithmetic

| Metric | Count |
|--------|------:|
| Total blocking prerequisites | 44 |
| SATISFIED | 6 |
| UNRESOLVED | 38 |
| NOT SATISFIED | 0 |

Invariant: SATISFIED + UNRESOLVED + NOT SATISFIED = Total blocking prerequisites (6 + 38 + 0 = 44). The Product Owner decision in Final Product Owner approval is **not** a Section 22 input prerequisite; it is evaluated separately under Section 23.

---

## 23. Decision rule

Final authorization must be exactly one of:

- **AUTHORIZED**
- **NOT AUTHORIZED**

Participant research is **AUTHORIZED** if and only if:

1. every independent blocking prerequisite in Section 22 is **SATISFIED**; and
2. Product Owner decision = **AUTHORIZED**.

In every other state:

Participant research is **NOT AUTHORIZED**.

Additional rules:

- The Product Owner may keep the decision **NOT AUTHORIZED** even when all Section 22 prerequisites are **SATISFIED**.
- Authorization does not arise automatically.
- **AUTHORIZED** grants recruitment and Stage A pilot execution only within the exact approved scope recorded in Final Product Owner approval.
- Recruitment authorization is a **consequence** of the Product Owner decision.
- Recruitment authorization is **not** an input prerequisite.
- **AUTHORIZED** does not authorize Stage B, production implementation, Google-content research, waypoint research, or other excluded work.

Do not use: conditionally authorized, mostly ready, ready with notes, automatic authorization, implied authorization, or TBD authorization.

---

## Final Product Owner approval

| Field | Current value |
|-------|---------------|
| Product Owner decision | NOT AUTHORIZED |
| Approved participant count | Not approved |
| Approved population | Not approved |
| Approved researcher | Not approved |
| Approval date | Not approved |
| Authorized recruitment window | Not approved |

Rules:

- This block may be changed only through a separate, explicit Product Owner decision.
- Satisfying technical or administrative prerequisites does not automatically authorize participant research.
- Recruitment and Stage A pilot execution are granted only when Product Owner decision = **AUTHORIZED**.
- An authorized decision applies only to the approved population, participant count, researcher, and recruitment window recorded in this block.

Recruitment must not begin while the Product Owner decision is **NOT AUTHORIZED**.

---

## 24. Current decision

Based on documented evidence and unresolved owner decisions as of this candidate document:

**Participant research authorization: NOT AUTHORIZED**

All Section 22 rows marked **UNRESOLVED** or **NOT SATISFIED** are current blocking items (38 **UNRESOLVED**, 0 **NOT SATISFIED**; see Section 22 prerequisite arithmetic).

**Blocker categories (each maps to one or more Section 22 rows; no row omitted, no extra gate):**

| Category | Section 22 rows (all **UNRESOLVED**) |
|----------|--------------------------------------|
| Candidate artifact approvals for participant use | Final Stage A contracts approved for participant use; Synthetic cases and truth tables approved for participant use; Stage A Experiment Pack approved for participant execution |
| Actual stimulus provenance (participant-facing artifacts) | Actual stimulus provenance verified and approved |
| Stage A research-design decisions (RDR 001 deferred, design parameters) | Final candidate-product-hypothesis wording approved; Meaningful duration margin approved; Meaningful toll difference approved; Exact decision-time limit approved; Synthetic case design philosophy approved; Synthetic stimulus provenance rules approved; Number of synthetic cases approved; Mode balance approved; Acceptable baseline ceiling approved; Primary outcome structure approved; Minimum meaningful uplift for Stage B approved; Stage B GO/KILL scope approved; Stage C2 definition of “effect persists” approved |
| Participant population and recruitment | Participant population approved; Recruitment criteria approved |
| Consent and privacy | Research consent requirements approved; Participant privacy requirements approved; Participant-facing consent wording approved; Participant privacy notice approved |
| Pseudonymization and governance determination | Pseudonymization method approved; Applicable research/privacy requirements determination completed; Participant withdrawal procedure approved |
| Researcher and location | Researcher identity and count approved; Study location and format approved; Participant-facing instruction approved |
| Pilot scheduling and size | Exact pilot participant count approved; First-session date/window approved |
| Storage, access, retention and deletion | Study-data retention requirements approved; CSV storage location approved; CSV access policy approved; Data retention period approved; Data deletion procedure approved; Accidental personal-data handling procedure approved |
| Feasibility thresholds | Stage A feasibility criteria approved |

**Product Owner decision (Final Product Owner approval):** **NOT AUTHORIZED**.

This is **not** a failure of Stage A technical work. Versioned candidate artifacts and the debug prototype exist; the **participant-execution gate** is incomplete pending independent Section 22 approvals and an explicit Product Owner authorization decision.

---

## 25. Stage boundaries

| Item | Status |
|------|--------|
| Stage A contracts | Completed as versioned candidate artifacts |
| Stage A Experiment Pack | Completed as a versioned candidate artifact |
| Stage A debug prototype | Completed |
| Stage A participant pilot | Not started |
| Stage A participant research | **NOT AUTHORIZED** (per this document) |
| Stage B | Not started — blocked pending completed Stage A pilot and Product Owner GO decision |
| Stage C1 | Not started |
| Stage C2 | Not started |
| Waypoint research | Blocked |
| Production implementation | Not authorized |

No other project status may change because of this document unless the Product Owner explicitly approves an updated authorization record.

---

STAGE A PARTICIPANT PILOT NOT AUTHORIZED
