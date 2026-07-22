# MARSHIO Stage 0C — Minimal Live Compatibility Probe Specification

| Field | Value |
|-------|-------|
| Document type | Experiment specification |
| Version | 1.1 |
| Decision authority | Product Owner |
| Specification date | 2026-07-21 |
| Document status | DRAFT — BLOCKED PENDING REQUEST-PARAMETER DECISIONS |
| Capability Audit | Completed — PASS |
| Gate 1 assessment | Closed with inconclusive outcome |
| Account operational readiness | Unconfirmed |
| Corridor-verification method | Not established |
| Live probe execution | Not authorized |
| Test-only implementation | Not authorized |
| Production implementation | Not authorized |

---

## 1. Objective

The minimal live compatibility probe exists only to determine:

- whether the current GraphHopper account accepts the four intended Route API request classes defined in this specification;
- whether the documented request parameters can coexist in the intended combinations;
- whether a structurally valid routing response is returned;
- whether requested path-detail fields are structurally present when available;
- whether `alternative_route` + per-request inline `custom_model` is accepted for the exact future request shape described in the accepted Capability Audit.

The probe does **not** evaluate:

- route usefulness;
- route quality;
- user value;
- product value;
- superiority over Google;
- similarity to Google;
- traffic quality;
- ETA accuracy;
- UAE driving quality;
- E11 avoidance;
- E311 preference;
- toll savings;
- navigation quality;
- production readiness.

Use this distinction:

**Request accepted** ≠ **Constraint proved effective**

**Route returned** ≠ **Route useful**

**Path details present** ≠ **Corridor identity established**

---

## 2. Research question

Does the current GraphHopper Route API account accept and return structurally valid responses for the four defined Stage 0C request classes, including the documented combination of `alternative_route` and per-request inline `custom_model`?

---

## 3. Preconditions for future execution

Execution of this probe remains blocked until separate Product Owner authorization confirms **all** of the following:

- current API key is active;
- applicable account plan is known;
- remaining quota is sufficient;
- four-request credit budget is available;
- applicable routing-location limit is known;
- intended non-production research use is permitted;
- one synthetic or research O-D fixture is approved;
- one candidate corridor-area fixture is approved;
- the toll-test fixture has an independently documented basis for expecting tagged toll segments;
- transient response-handling controls are approved;
- retained summary fields are approved;
- researcher/operator is identified;
- execution date and time window are authorized.

This specification does **not** verify these items. No unresolved prerequisite is marked satisfied here.

---

## 4. Fixed request budget

| Control | Value |
|---------|-------|
| Planned requests | exactly **4** |
| Maximum provider requests | **4** |
| Retries | **0** |
| Warm-up requests | **0** |
| Replacement requests | **0** |
| Exploratory requests | **0** |

If execution stops before all four requests are sent:

- do not replace omitted requests;
- do not retry;
- classify the probe session as **incomplete**;
- require separate authorization for another session.

Redirects or provider-internal operations do not change the client request budget. Every client-initiated Route API call counts toward the budget.

---

## 5. Shared request controls

Without including JSON, coordinates, keys, or payloads, the following controls are shared by all four requests:

- one approved O-D fixture;
- identical origin and destination for R1–R4;
- `POST /route`;
- `profile=car`;
- identical locale;
- identical geometry settings;
- identical instruction settings;
- identical approved path-detail request where applicable;
- no Google data;
- no Google route response;
- no traffic integration;
- no production application;
- no Android application;
- no participant;
- no route execution by a driver;
- no account-dependent parameter added outside the approved request table;
- no dynamic tuning between requests.

Any deviation invalidates direct structural comparison and must be recorded as a **protocol deviation**.

Shared controls listed above are **intent only** until each corresponding row in §6.3 is fixed by explicit Product Owner decision with recorded approval evidence.

---

## 6. Exact four-request matrix

Exactly four request IDs are defined. No fifth request is permitted.

Normative request parameters for execution are defined only in §6.3. Purpose statements below do not fix execution values.

### R1 — Flexible baseline

**Purpose:** Verify that the approved account accepts the base POST Route request in the flexible-routing request class intended for inline models.

**Required characteristics:**

- same approved O-D fixture;
- `profile=car`;
- flexible routing enabled;
- no inline constraint rules;
- no alternative-route algorithm;
- approved path details requested;
- one routing response expected.

R1 is a technical baseline only. It is not Stage 0B replication and does not compare route quality.

### R2 — Outside-corridor priority reduction

**Purpose:** Verify acceptance of an inline `custom_model` containing the approved candidate area and the documented soft-preference mechanism.

**Required characteristics:**

- identical shared controls;
- one approved candidate corridor area;
- priority reduced outside that candidate area;
- no forced via point;
- no alternative-route algorithm;
- approved path details requested.

**Explicit limitations:**

- R2 does **not** prove E311 preference;
- the area must **not** be described as verified E311;
- response acceptance does **not** prove that the rule changed the route;
- interpreting physical corridor identity requires the separate corridor-verification method.

### R3 — Tagged-toll penalty

**Purpose:** Verify acceptance of an inline toll-based priority rule and structural availability of the requested toll path detail.

**Required characteristics:**

- identical shared controls;
- one toll priority penalty using documented categorical toll attributes;
- no monetary toll calculation;
- no forced via point;
- no alternative-route algorithm;
- toll path detail requested.

**Explicit limitations:**

- R3 tests documented tagged-toll handling only;
- it does **not** prove avoidance of every Salik facility;
- it does **not** calculate AED cost;
- missing or incomplete OSM toll tags can make behavioural interpretation inconclusive;
- a returned route without tagged toll segments does not by itself prove that the penalty caused avoidance.

### R4 — Alternative route plus inline custom model

**Purpose:** Verify acceptance of the exact documented parameter combination remaining unresolved after the Capability Audit.

**Required characteristics:**

- identical shared controls;
- same candidate area and priority rule as R2 (values fixed only in §6.3 when approved);
- `alternative_route` enabled (parameter values fixed only in §6.3 when approved);
- approved path details requested (list fixed only in §6.3 when approved);
- no via points.

**Explicit limitations:**

- R4 tests parameter compatibility and response structure;
- it does **not** test Google comparison;
- it does **not** prove useful alternatives;
- a valid response containing one path can still demonstrate request acceptance;
- returning fewer alternative paths than requested is not automatically an API compatibility failure.

### 6.1 Stage 0C probe parameter approval rule

A value is **fixed** only if an explicit Product Owner decision approves that **exact** value for this Stage 0C minimal live compatibility probe and records approval evidence in §6.3.

Do **not** infer Stage 0C parameter approval from:

- the Capability Audit (`docs/stage-0c-graphhopper-capability-audit.md`);
- the Gate 1 Closure Record (`docs/stage-0c-gate-1-closure.md`);
- Stage 0B request parameters (`docs/stage-0b-multi-provider-completion.md` and benchmark code);
- GraphHopper documentation;
- API defaults;
- Cursor recommendations;
- Codex examples.

Those sources may establish capability or historical context. They do **not** automatically approve Stage 0C execution parameters for this probe.

This revision is authorized for **document revision only**. It is **not** Product Owner approval of any technical parameter, fixture ID, factor, timeout, details list, or structural classification.

### 6.2 R1 `custom_model` absent versus present-empty

R1 must use **exactly one** of the following mutually exclusive states:

| State | Meaning |
|-------|---------|
| **Absent** | No `custom_model` field in the request |
| **Present but empty** | `custom_model` field present with no constraint rules |

Only one state may be selected for execution. They are **not** equivalent for compatibility testing.

Neither state currently has explicit Product Owner approval recorded in §6.3. Until one state is approved, R1 request construction remains **unresolved**.

### 6.3 Normative request-definition table

The table below is the single consolidated normative definition for R1–R4. Fixture references use **approved fixture IDs only** (no coordinates or polygon geometry in this document).

| Field | R1 | R2 | R3 | R4 | Classification | Approval evidence |
|-------|----|----|----|----|----------------|-------------------|
| request ID | R1 | R2 | R3 | R4 | FIXED SHARED CONTROL | Product Owner-authorized specification scope: exactly four request IDs (R1–R4). |
| HTTP method | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| endpoint path (no host, no credentials) | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| profile | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| number of routing points | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| point order | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| O-D fixture ID | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `ch.disable` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `custom_model` state | **Unresolved:** absent **or** present-empty (§6.2); only one may be selected | present with rules | present with rules | present with rules (same approved model as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| candidate-area fixture ID | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED (same approved ID as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| area-condition semantics | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED (same approved semantics as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact custom-model priority condition | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED (same approved condition as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact custom-model priority operation | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED (same approved operation as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact priority factor (numeric) | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED (same approved factor as R2 when fixed) | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact toll condition | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact toll enum values covered | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact toll priority operation | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact toll factor (numeric) | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| algorithm | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `alternative_route.max_paths` | NOT APPLICABLE | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `alternative_route.max_weight_factor` | NOT APPLICABLE | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `alternative_route.max_share_factor` | NOT APPLICABLE | NOT APPLICABLE | NOT APPLICABLE | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| exact requested `details` list | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| per-detail structural classification (each named detail: MANDATORY or NON-CRITICAL) | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `calc_points` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `points_encoded` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `instructions` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| locale | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `optimize` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| via-point count | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| `pass_through` | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| heading use | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| timeout policy | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| expected minimum path count | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: top-level paths collection | MANDATORY (pre-declared; execution blocked until PO confirms list alignment with §6.3 details) | MANDATORY | MANDATORY | MANDATORY | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: per-path distance field | MANDATORY (pre-declared) | MANDATORY | MANDATORY | MANDATORY | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: per-path time field | MANDATORY (pre-declared) | MANDATORY | MANDATORY | MANDATORY | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: points / geometry container per path | NON-CRITICAL (pre-declared) | NON-CRITICAL | NON-CRITICAL | NON-CRITICAL | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: instructions container per path | NON-CRITICAL (pre-declared) | NON-CRITICAL | NON-CRITICAL | NON-CRITICAL | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |
| response element: each requested detail container named in approved `details` list | PRODUCT OWNER DECISION REQUIRED per detail | PRODUCT OWNER DECISION REQUIRED per detail | PRODUCT OWNER DECISION REQUIRED per detail | PRODUCT OWNER DECISION REQUIRED per detail | PRODUCT OWNER DECISION REQUIRED | NONE — PRODUCT OWNER DECISION REQUIRED |

No response element may be reclassified after execution results are observed. A missing pre-execution classification for any requested response element makes probe-level classification **INCONCLUSIVE** (outcome rules incomplete).

### 6.4 Unresolved request parameters and document status

If any required request value in §6.3 lacks explicit Product Owner approval evidence, that field remains **PRODUCT OWNER DECISION REQUIRED** with approval evidence **NONE — PRODUCT OWNER DECISION REQUIRED**.

**Document status:** **DRAFT — BLOCKED PENDING REQUEST-PARAMETER DECISIONS**

The specification is **not reproducible** and cannot proceed to implementation review or execution authorization until every required request parameter, fixture reference, per-detail structural classification, timeout, and alternative-route value is fixed by explicit Product Owner decision with recorded approval evidence in §6.3.

---

## 7. Observation model

For every request, distinguish three levels.

### 7.1 Transport outcome

Allowed observations:

- request sent or not sent;
- response received or not received;
- HTTP status class;
- timeout;
- network failure;
- authentication or quota failure.

### 7.2 Structural outcome

Allowed observations:

- response parseable;
- route collection present;
- path count;
- required top-level route fields present;
- requested detail containers present or absent;
- provider error category, sanitized.

### 7.3 Behavioural observation

Allowed only as a **non-conclusive** observation:

- geometry differs from R1;
- path count differs;
- requested detail values occur;
- returned paths appear structurally distinct.

Behavioural observations **cannot** establish:

- causal constraint effectiveness;
- E11/E311 identity;
- route usefulness;
- product value.

---

## 8. Per-request success criteria

Per-request technical success applies **only after** every value in §6.3 is fixed and the request is constructed exactly according to the approved specification. Until then, per-request success criteria are **not operable**.

When operable, technical success requires request acceptance, successful transport, parseable response, at least the **expected minimum path count** approved in §6.3, and processing of every **MANDATORY** response element pre-declared in §6.3 for that request.

For R4, one returned path satisfies minimum path count when §6.3 approves a minimum of one. A second alternative path is **not** required.

Do **not** require route difference from R1, toll avoidance, corridor effect, or Google comparison for per-request technical success.

---

## 9. Technical probe outcome versus Gate 1 outcome

**Technical probe outcome** ≠ **Gate 1 outcome**

The live compatibility probe evaluates **only** API/request compatibility and pre-declared structural handling. It does **not** establish corridor identity or constraint effectiveness.

| Statement | Meaning |
|-----------|---------|
| Technical probe may receive **TECHNICAL PASS** without a completed corridor-verification method | API compatibility can be assessed independently of corridor identity rules |
| Technical pass confirms only request acceptance and required response structure per §6.3 | Not route usefulness or constraint proof |
| Technical success does **not** establish E11/E311 identity | Requires separately authorized corridor-verification method |
| Technical success does **not** establish constraint effectiveness | Request accepted ≠ constraint proved effective |
| Technical success does **not** close or advance Gate 1 by itself | Gate 1 remains **INCONCLUSIVE** until corridor-verification method and other required evidence are separately reviewed |
| Missing corridor-verification method | Separate **Gate 1** blocker; blocker for route-identity interpretation; **not** a blocker for classifying pure API compatibility |

---

## 10. Probe-level outcome taxonomy

Probe-level outcomes are **mutually exclusive**. Assign **exactly one** outcome using §10.5 precedence. Do **not** use product GO/KILL.

### 10.1 Pre-declared structural classification

Before execution, every requested response element must be classified in §6.3 as **MANDATORY** or **NON-CRITICAL**.

No response element may be reclassified after results are seen. If any requested element lacks a pre-execution classification, the probe-level result is **INCONCLUSIVE** (outcome rules incomplete).

### 10.2 TECHNICAL PASS

Use **only** when:

- all four requests were sent according to the **approved** specification (§6.3 fully fixed and matched at execution);
- all four were accepted;
- all four returned parseable responses;
- every response returned at least one route path (or the approved expected minimum path count from §6.3);
- every pre-declared **MANDATORY** structural element was present and processable;
- every pre-declared **NON-CRITICAL** structural element was also present.

For R4: one returned path is sufficient; no second alternative path, geometry difference, corridor-effect proof, or toll-effect proof is required.

### 10.3 PARTIAL

Use **only** when:

- all four requests were accepted;
- all four primary route responses remained parseable and processable;
- all **MANDATORY** structural elements were present;
- at least one specifically named, pre-declared **NON-CRITICAL** structural element was absent.

Every PARTIAL result must identify:

- request ID;
- exact missing non-critical element;
- evidence that the element was requested or expected per §6.3;
- confirmation that all mandatory elements remained processable.

**PARTIAL must not** be used merely because of:

- one returned path in R4;
- no second alternative path;
- no geometry difference;
- weak or missing corridor evidence;
- weak or missing toll-effect evidence;
- no route change;
- lack of practical usefulness;
- lack of Google comparison.

Those are observations outside the technical compatibility classification.

### 10.4 TECHNICAL FAIL

Use **only** when:

- a documented, **fully approved** request shape is rejected or returns a structurally unusable response;
- independent verification confirms the request was constructed exactly according to the approved specification;
- account activity, authentication, plan, quota, routing-location limits, network, timeout, operator error, environment error, and protocol deviation have been **excluded** as causes;
- the failure is attributable to provider request compatibility.

If request conformance cannot be independently confirmed, classify **INCONCLUSIVE**, not TECHNICAL FAIL.

### 10.5 INCONCLUSIVE

Use when the result cannot be reliably attributed to GraphHopper request compatibility because of:

- inactive or invalid account/key;
- authentication uncertainty;
- quota or credit exhaustion;
- unknown plan restriction;
- routing-location-limit uncertainty;
- network failure;
- timeout;
- incomplete probe;
- protocol deviation;
- unresolved request parameter;
- request-construction mismatch;
- inability to verify request conformance;
- fixture error;
- operator error;
- environment error;
- response-handling failure not attributable to GraphHopper;
- structural element not pre-classified as mandatory or non-critical.

Do **not** include the missing corridor-verification method in this list. That method remains a separate Gate 1 and route-identity blocker (§9, §14).

### 10.6 Classification precedence

Apply outcomes in this order:

1. If attribution to GraphHopper compatibility is blocked by any uncertainty → **INCONCLUSIVE**.
2. Otherwise, if a confirmed conformant request is rejected or structurally unusable → **TECHNICAL FAIL**.
3. Otherwise, if all mandatory structures are present but a declared non-critical structure is missing → **PARTIAL**.
4. Otherwise, if all mandatory and non-critical requirements are satisfied → **TECHNICAL PASS**.

Only one probe-level outcome may be assigned.

---

## 11. Failure and stop conditions

Stop immediately and send no further requests if:

- credential exposure is suspected;
- the account reports an authorization or billing problem;
- the four-request budget would be exceeded;
- a request contains unapproved coordinates or fixtures;
- raw payload logging cannot be disabled;
- response storage exceeds the approved transient boundary;
- a protocol deviation prevents safe continuation;
- provider Terms or account conditions appear inconsistent with the approved protocol.

A stopped probe is **INCONCLUSIVE** unless existing evidence independently establishes a technical rejection.

---

## 12. Data-handling specification

Separate transient handling from retained evidence.

### 12.1 Transiently permitted during an authorized execution

Only for the duration necessary to inspect the response:

- in-memory request construction;
- in-memory response parsing;
- in-memory route geometry;
- in-memory path details;
- temporary client-side handling consistent with the accepted audit;
- display to the authorized researcher where necessary.

All transient provider content must be discarded when the observation is recorded and the probe session ends.

This specification does **not** itself authorize that handling; execution authorization must approve it.

### 12.2 Candidate retained summary fields

If separately approved for execution, retain only MARSHIO-owned technical observations:

- probe session ID;
- request ID R1–R4;
- execution timestamp;
- sent/not-sent flag;
- HTTP status class, not full headers;
- elapsed request time;
- parse success flag;
- path count;
- required-field presence flags;
- requested-detail presence flags;
- sanitized error category;
- protocol-deviation flag;
- technical outcome;
- researcher note without provider content.

Do not retain provider error-message bodies.

The candidate list is **not** authorized merely because it appears in this specification.

### 12.3 Prohibited retention

Do **not** retain:

- API keys;
- authorization headers;
- complete request URLs containing credentials;
- raw request payloads;
- raw response JSON;
- HAR files;
- network captures;
- provider logs copied from tooling;
- coordinates;
- candidate-area polygon coordinates;
- encoded or decoded polylines;
- route geometry;
- turn-by-turn instructions;
- street-name sequences;
- street-reference sequences;
- raw path-detail arrays;
- screenshots containing provider content;
- cached provider responses;
- Google content;
- Google route data;
- participant or personal data.

If these prohibitions prevent required verification, stop and request a separate data-handling decision. Do **not** silently retain additional evidence.

---

## 13. Reporting rules

A future probe report may contain only:

- approved retained summary fields;
- outcome for each request;
- probe-level outcome;
- sanitized reason for FAIL or INCONCLUSIVE;
- explicit limitations;
- confirmation of request count;
- confirmation of data destruction.

The report must **not** contain:

- raw provider responses;
- coordinates;
- polylines;
- route screenshots;
- corridor identity claims;
- Google comparisons;
- usefulness conclusions;
- implementation recommendations.

---

## 14. Corridor-verification boundary

- This specification does **not** define the corridor-verification method.
- R2 and R4 use only an approved candidate geographical fixture.
- No returned path may be labelled E11 or E311 based solely on this probe.
- `street_ref` presence alone is not yet the approved classification method.
- Gate 1 cannot advance based solely on request acceptance or a technical probe **TECHNICAL PASS**.
- A technical probe outcome does **not** satisfy the corridor-verification requirement for Gate 1.
- The corridor-verification method requires a separate authorized document and independent review.

---

## 15. Account boundary

- Published API capability does not establish readiness of the current account.
- Key activity, plan, quota and applicable limits remain administrative prerequisites.
- No credentials may be placed in this document.
- Account inspection and probe execution require separate authorization.
- An account failure produces **INCONCLUSIVE**, not a capability FAIL, unless official provider evidence establishes unsupported functionality.

---

## 16. Relationship to Stage 0B

- Stage 0B remains completed and immutable.
- R1 is not a rerun or modification of Stage 0B.
- Stage 0B provider semantics must not be changed.
- Future execution must use a separate Stage 0C test-only path.
- No Stage 0B result may be reclassified by this probe specification.

This specification does **not** design that test-only path.

---

## 17. Authorization

This specification does not authorize implementation.

This specification does not authorize account access.

This specification does not authorize live API execution.

This specification does not authorize provider-content retention.

This specification does not authorize Stage 0C development.

This specification does not authorize Android integration.

This specification does not authorize production implementation.

Separate Product Owner approval is required before any execution or implementation activity.

---

## 18. Current status

| Item | Status |
|------|--------|
| Stage 0C Capability Audit | COMPLETED |
| Gate 1 assessment | CLOSED WITH INCONCLUSIVE OUTCOME |
| Minimal live probe specification | DRAFT — BLOCKED PENDING REQUEST-PARAMETER DECISIONS |
| Account operational-readiness check | NOT AUTHORIZED |
| Corridor-verification method | NOT AUTHORIZED |
| Live compatibility probe | NOT AUTHORIZED |
| Test-only implementation | NOT AUTHORIZED |
| Android integration | NOT AUTHORIZED |
| Production implementation | NOT AUTHORIZED |
| Stage 0B | COMPLETED — UNCHANGED |

STAGE 0C MINIMAL LIVE COMPATIBILITY PROBE — SPECIFIED BUT NOT AUTHORIZED
