# MARSHIO Stage 0C — Product Owner Decision Register

| Field | Value |
|-------|-------|
| Document type | Decision register (traceability only) |
| Register date | 2026-07-22 |
| Decision authority | Product Owner |
| Source specification | `docs/stage-0c-minimal-live-probe-specification.md` (committed package) |
| Register status | Part A: **1** decision **RESOLVED** (PO-0C-REQ-008); **38** **UNRESOLVED** |

---

## Register scope and limits

The **Product Owner Decision Register** in this document is **Part A only** (PO-0C-REQ-001 through PO-0C-REQ-039). It **records** unresolved Product Owner decisions required before the accepted Minimal Live Compatibility Probe Specification §6.3 normative request-definition table can become **reproducible**. It **does not make** any decision, **does not** recommend or select values, and **grants no** implementation, account-access, live-execution, or provider-call authority.

All **Part A** decision entries remain **UNRESOLVED** until the Product Owner records an explicit decision and updates the specification (§6.3 approval evidence as applicable). **PO-0C-REQ-008** is **RESOLVED** (Product Owner Decision Pack 1); all other Part A entries remain **UNRESOLVED**.

The specification remains **DRAFT — BLOCKED PENDING REQUEST-PARAMETER DECISIONS** until all **39** Part A decisions are resolved (**38** remain unresolved). Live probe execution and implementation remain separately **NOT AUTHORIZED** regardless of appendix traceability items.

**Fixed by the accepted specification (not open parameter decisions in Part A):** four request identifiers R1–R4 (§6.3 `request ID` row); fixed four-request client budget with zero retries, warm-up, replacement, and exploratory requests (§4).

**Total reproducibility decisions (Part A):** **39**. **Resolved:** **1**. **Unresolved:** **38**.

---

## Part A — Normative request parameters (§6.3)

One register entry per §6.3 normative table **Field** row where execution values or approval evidence remain unresolved. Source: §6.3; supporting context in §6.1, §6.2, §6.4, §5, §8, §10.1 where cited.

| Decision ID | Decision topic | Current status | Why required | Affected request(s) | Blocking effect if unresolved | Source reference |
|-------------|----------------|----------------|--------------|---------------------|-------------------------------|------------------|
| PO-0C-REQ-001 | HTTP method | UNRESOLVED | Normative table requires an approved method for every Route API call; no approval evidence recorded | R1, R2, R3, R4 | Request cannot be constructed reproducibly; §6.4 block; per-request success not operable (§8); probe-level INCONCLUSIVE for unresolved parameter (§10.5) | §6.3 row `HTTP method`; §6.1; §6.4 |
| PO-0C-REQ-002 | endpoint path (no host, no credentials) | UNRESOLVED | Path must be fixed without embedding credentials | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `endpoint path (no host, no credentials)`; §6.4 |
| PO-0C-REQ-003 | profile | UNRESOLVED | Profile value must be explicitly approved for Stage 0C probe | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `profile`; §6.4 |
| PO-0C-REQ-004 | number of routing points | UNRESOLVED | Point count must be fixed for reproducible requests | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `number of routing points`; §6.4 |
| PO-0C-REQ-005 | point order | UNRESOLVED | Order of routing points must be fixed | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `point order`; §6.4 |
| PO-0C-REQ-006 | O-D fixture ID | UNRESOLVED | Approved fixture ID required; coordinates not defined in specification | R1, R2, R3, R4 | Same as PO-0C-REQ-001; shared O-D cannot be applied | §6.3 row `O-D fixture ID`; §5; §6.4 |
| PO-0C-REQ-007 | `ch.disable` | UNRESOLVED | Flexible-routing control must be explicitly approved | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `` `ch.disable` ``; §6.4 |
| PO-0C-REQ-008 | `custom_model` state — R1 baseline (absent) | **RESOLVED** | Product Owner Decision Pack 1: R1 SHALL omit `custom_model` entirely; empty inline `custom_model` not permitted. Rationale: R1 represents provider behaviour without an inline custom model and serves as baseline control for requests that introduce custom-model rules. Does not attribute all R2–R4 differences to `custom_model` alone. | **R1 only** | R1 `custom_model` state no longer blocks §6.3 for this field. R2–R4 remain `present with rules` per specification; rule contents still governed by PO-0C-REQ-009–039 and related rows. | §6.2; §6.3 row `` `custom_model` state ``; Decision Pack 1 |
| PO-0C-REQ-009 | candidate-area fixture ID | UNRESOLVED | Candidate area must be referenced by approved fixture ID only | R2; R4 (same approved ID as R2 when fixed) | R2 and R4 corridor-area requests undefined | §6.3 row `candidate-area fixture ID`; §6.4 |
| PO-0C-REQ-010 | area-condition semantics | UNRESOLVED | Outside-area (or equivalent) condition semantics must be fixed | R2; R4 (same as R2 when fixed) | Soft-corridor rule not reproducible | §6.3 row `area-condition semantics`; §6.4 |
| PO-0C-REQ-011 | exact custom-model priority condition | UNRESOLVED | Priority rule condition text/structure must be approved | R2; R4 (same as R2 when fixed) | Inline priority rule incomplete | §6.3 row `exact custom-model priority condition`; §6.4 |
| PO-0C-REQ-012 | exact custom-model priority operation | UNRESOLVED | Priority operation must be approved | R2; R4 (same as R2 when fixed) | Inline priority rule incomplete | §6.3 row `exact custom-model priority operation`; §6.4 |
| PO-0C-REQ-013 | exact priority factor (numeric) | UNRESOLVED | Numeric priority factor must be approved | R2; R4 (same as R2 when fixed) | Inline priority rule incomplete | §6.3 row `exact priority factor (numeric)`; §6.4 |
| PO-0C-REQ-014 | exact toll condition | UNRESOLVED | Toll-based rule condition must be approved | R3 | R3 toll rule undefined | §6.3 row `exact toll condition`; §6.4 |
| PO-0C-REQ-015 | exact toll enum values covered | UNRESOLVED | Which toll enum values the rule covers must be approved | R3 | R3 toll rule ambiguous | §6.3 row `exact toll enum values covered`; §6.4 |
| PO-0C-REQ-016 | exact toll priority operation | UNRESOLVED | Toll priority operation must be approved | R3 | R3 toll rule incomplete | §6.3 row `exact toll priority operation`; §6.4 |
| PO-0C-REQ-017 | exact toll factor (numeric) | UNRESOLVED | Numeric toll penalty factor must be approved | R3 | R3 toll rule incomplete | §6.3 row `exact toll factor (numeric)`; §6.4 |
| PO-0C-REQ-018 | algorithm | UNRESOLVED | Routing algorithm parameter must be approved per request class | R1, R2, R3, R4 | Request class definitions incomplete (including R4 `alternative_route`) | §6.3 row `algorithm`; §6.4 |
| PO-0C-REQ-019 | `alternative_route.max_paths` | UNRESOLVED | Alternative-route path count parameter must be approved | R4 | R4 combination request undefined | §6.3 row `` `alternative_route.max_paths` ``; §6.4 |
| PO-0C-REQ-020 | `alternative_route.max_weight_factor` | UNRESOLVED | Alternative-route weight factor must be approved | R4 | R4 combination request undefined | §6.3 row `` `alternative_route.max_weight_factor` ``; §6.4 |
| PO-0C-REQ-021 | `alternative_route.max_share_factor` | UNRESOLVED | Alternative-route share factor must be approved | R4 | R4 combination request undefined | §6.3 row `` `alternative_route.max_share_factor` ``; §6.4 |
| PO-0C-REQ-022 | exact requested `details` list | UNRESOLVED | Named path details must be listed and approved | R1, R2, R3, R4 | Path-detail requests and downstream classifications undefined | §6.3 row `exact requested details list`; §6.4 |
| PO-0C-REQ-023 | per-detail structural classification (MANDATORY or NON-CRITICAL for each named detail) | UNRESOLVED | Each detail in approved list must be pre-classified before execution (§10.1) | R1, R2, R3, R4 | Outcome rules incomplete → probe-level INCONCLUSIVE (§6.3 note; §10.1; §10.5) | §6.3 row `per-detail structural classification`; §10.1 |
| PO-0C-REQ-024 | `calc_points` | UNRESOLVED | Geometry output control must be approved | R1, R2, R3, R4 | Shared geometry settings not fixed (§5 intent) | §6.3 row `` `calc_points` ``; §5; §6.4 |
| PO-0C-REQ-025 | `points_encoded` | UNRESOLVED | Polyline encoding flag must be approved | R1, R2, R3, R4 | Same as PO-0C-REQ-024 | §6.3 row `` `points_encoded` ``; §5; §6.4 |
| PO-0C-REQ-026 | `instructions` | UNRESOLVED | Instruction output control must be approved | R1, R2, R3, R4 | Shared instruction settings not fixed (§5 intent) | §6.3 row `` `instructions` ``; §5; §6.4 |
| PO-0C-REQ-027 | locale | UNRESOLVED | Locale must be approved and identical across R1–R4 | R1, R2, R3, R4 | Shared locale not fixed (§5 intent) | §6.3 row `locale`; §5; §6.4 |
| PO-0C-REQ-028 | `optimize` | UNRESOLVED | Optimize parameter must be approved | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `` `optimize` ``; §6.4 |
| PO-0C-REQ-029 | via-point count | UNRESOLVED | Via-point count must be approved (matrix requires no forced vias when zero) | R1, R2, R3, R4 | Via usage undefined | §6.3 row `via-point count`; §6.4 |
| PO-0C-REQ-030 | `pass_through` | UNRESOLVED | Pass-through behaviour must be approved | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `` `pass_through` ``; §6.4 |
| PO-0C-REQ-031 | heading use | UNRESOLVED | Heading parameter use must be approved | R1, R2, R3, R4 | Same as PO-0C-REQ-001 | §6.3 row `heading use`; §6.4 |
| PO-0C-REQ-032 | timeout policy | UNRESOLVED | Client timeout policy must be approved | R1, R2, R3, R4 | Transport observation rules ambiguous; stop conditions may apply | §6.3 row `timeout policy`; §6.4; §11 |
| PO-0C-REQ-033 | expected minimum path count | UNRESOLVED | Minimum paths required for technical success must be approved per request | R1, R2, R3, R4 | §8 success criteria not operable | §6.3 row `expected minimum path count`; §8; §6.4 |
| PO-0C-REQ-034 | response element: top-level paths collection — Product Owner approval of pre-declared MANDATORY classification and alignment with approved `details` | UNRESOLVED | Pre-declared MANDATORY; approval evidence not recorded | R1, R2, R3, R4 | Structural outcome rules not fully authorized; §6.3 execution blocked until PO confirms | §6.3 row `response element: top-level paths collection`; §10.1 |
| PO-0C-REQ-035 | response element: per-path distance field — Product Owner approval of pre-declared MANDATORY classification | UNRESOLVED | Pre-declared MANDATORY; approval evidence not recorded | R1, R2, R3, R4 | Mandatory structural handling not authorized for outcome taxonomy | §6.3 row `response element: per-path distance field`; §10.1–§10.2 |
| PO-0C-REQ-036 | response element: per-path time field — Product Owner approval of pre-declared MANDATORY classification | UNRESOLVED | Pre-declared MANDATORY; approval evidence not recorded | R1, R2, R3, R4 | Same as PO-0C-REQ-035 | §6.3 row `response element: per-path time field`; §10.1–§10.2 |
| PO-0C-REQ-037 | response element: points / geometry container per path — Product Owner approval of pre-declared NON-CRITICAL classification | UNRESOLVED | Pre-declared NON-CRITICAL; approval evidence not recorded | R1, R2, R3, R4 | PARTIAL/TECHNICAL PASS rules not fully authorized | §6.3 row `response element: points / geometry container per path`; §10.1–§10.3 |
| PO-0C-REQ-038 | response element: instructions container per path — Product Owner approval of pre-declared NON-CRITICAL classification | UNRESOLVED | Pre-declared NON-CRITICAL; approval evidence not recorded | R1, R2, R3, R4 | Same as PO-0C-REQ-037 | §6.3 row `response element: instructions container per path`; §10.1–§10.3 |
| PO-0C-REQ-039 | response element: each requested detail container named in approved `details` list — classification per detail | UNRESOLVED | Depends on PO-0C-REQ-022; each container must be MANDATORY or NON-CRITICAL before execution | R1, R2, R3, R4 | Missing pre-classification → INCONCLUSIVE (§6.3 note; §10.5) | §6.3 row `response element: each requested detail container named in approved details list`; §10.1 |

---

## Coverage index (§6.3 field → Decision ID)

| §6.3 Field (exact) | Decision ID |
|--------------------|-------------|
| HTTP method | PO-0C-REQ-001 |
| endpoint path (no host, no credentials) | PO-0C-REQ-002 |
| profile | PO-0C-REQ-003 |
| number of routing points | PO-0C-REQ-004 |
| point order | PO-0C-REQ-005 |
| O-D fixture ID | PO-0C-REQ-006 |
| `ch.disable` | PO-0C-REQ-007 |
| `custom_model` state | PO-0C-REQ-008 |
| candidate-area fixture ID | PO-0C-REQ-009 |
| area-condition semantics | PO-0C-REQ-010 |
| exact custom-model priority condition | PO-0C-REQ-011 |
| exact custom-model priority operation | PO-0C-REQ-012 |
| exact priority factor (numeric) | PO-0C-REQ-013 |
| exact toll condition | PO-0C-REQ-014 |
| exact toll enum values covered | PO-0C-REQ-015 |
| exact toll priority operation | PO-0C-REQ-016 |
| exact toll factor (numeric) | PO-0C-REQ-017 |
| algorithm | PO-0C-REQ-018 |
| `alternative_route.max_paths` | PO-0C-REQ-019 |
| `alternative_route.max_weight_factor` | PO-0C-REQ-020 |
| `alternative_route.max_share_factor` | PO-0C-REQ-021 |
| exact requested `details` list | PO-0C-REQ-022 |
| per-detail structural classification | PO-0C-REQ-023 |
| `calc_points` | PO-0C-REQ-024 |
| `points_encoded` | PO-0C-REQ-025 |
| `instructions` | PO-0C-REQ-026 |
| locale | PO-0C-REQ-027 |
| `optimize` | PO-0C-REQ-028 |
| via-point count | PO-0C-REQ-029 |
| `pass_through` | PO-0C-REQ-030 |
| heading use | PO-0C-REQ-031 |
| timeout policy | PO-0C-REQ-032 |
| expected minimum path count | PO-0C-REQ-033 |
| response element: top-level paths collection | PO-0C-REQ-034 |
| response element: per-path distance field | PO-0C-REQ-035 |
| response element: per-path time field | PO-0C-REQ-036 |
| response element: points / geometry container per path | PO-0C-REQ-037 |
| response element: instructions container per path | PO-0C-REQ-038 |
| response element: each requested detail container named in approved `details` list | PO-0C-REQ-039 |

**§6.3 row excluded (fixed):** `request ID` — approval evidence recorded in specification; not an open parameter decision.

---

## Out-of-scope execution prerequisites and authorization boundaries

This appendix is **not** part of the Product Owner Decision Register. It preserves traceability to other specification gates. Resolving Part A does **not** by itself authorize execution or satisfy these items.

- **Appendix B** lists **execution preconditions** from the source specification §3.
- **Appendix C** lists **authorization and governance boundaries** stated **NOT AUTHORIZED** in the source specification.
- Neither Appendix B nor Appendix C is counted as a Product Owner decision required to make §6.3 **reproducible**.
- The number of unresolved Product Owner decisions for specification reproducibility is **38** of **39** Part A entries (PO-0C-REQ-008 resolved).

### Appendix B — Execution preconditions (§3)

Appendix entries (not Part A decision entries). Status: **UNRESOLVED** per §3 (no prerequisite marked satisfied in the specification).

| Appendix entry ID | Decision topic | Current status | Why required | Affected request(s) | Blocking effect if unresolved | Source reference |
|-------------------|----------------|----------------|--------------|---------------------|-------------------------------|------------------|
| PO-0C-EX-001 | Confirm current API key is active | UNRESOLVED | §3 execution precondition | R1, R2, R3, R4 (session) | Execution remains blocked; account boundary (§15); likely probe INCONCLUSIVE if attempted | §3; §15 |
| PO-0C-EX-002 | Confirm applicable account plan is known | UNRESOLVED | §3 execution precondition | Session | Same as PO-0C-EX-001 | §3; §15 |
| PO-0C-EX-003 | Confirm remaining quota is sufficient | UNRESOLVED | §3 execution precondition | Session | Same as PO-0C-EX-001 | §3; §10.5 |
| PO-0C-EX-004 | Confirm four-request credit budget is available | UNRESOLVED | §3 execution precondition | R1–R4 budget (§4) | Session cannot proceed safely within budget | §3; §4 |
| PO-0C-EX-005 | Confirm applicable routing-location limit is known | UNRESOLVED | §3 execution precondition | R1, R2, R3, R4 | Request construction or acceptance may be indeterminate | §3; §10.5 |
| PO-0C-EX-006 | Confirm intended non-production research use is permitted | UNRESOLVED | §3 execution precondition | Session | Execution not authorized | §3; §16–§18 |
| PO-0C-EX-007 | Approve one synthetic or research O-D fixture | UNRESOLVED | §3; complements PO-0C-REQ-006 fixture ID decision | R1, R2, R3, R4 | Fixture use not authorized even if ID later fixed | §3; §6.3 `O-D fixture ID` |
| PO-0C-EX-008 | Approve one candidate corridor-area fixture | UNRESOLVED | §3; complements PO-0C-REQ-009 | R2, R4 | Candidate area not authorized for use | §3; §6.3 `candidate-area fixture ID` |
| PO-0C-EX-009 | Confirm toll-test fixture has independently documented basis for expecting tagged toll segments | UNRESOLVED | §3 execution precondition | R3 | R3 behavioural context not established; fixture use not authorized | §3; §6 R3 purpose |
| PO-0C-EX-010 | Approve transient response-handling controls | UNRESOLVED | §3; §12.1 states specification does not authorize transient handling without execution authorization | R1, R2, R3, R4 | Data-handling stop conditions may apply (§11; §12.3) | §3; §12.1 |
| PO-0C-EX-011 | Approve retained summary fields for any post-session retention | UNRESOLVED | §3; §12.2 candidate list not authorized by specification text alone | Session reporting | Retention prohibited beyond §12.3 unless separately approved | §3; §12.2–§12.3 |
| PO-0C-EX-012 | Identify researcher/operator | UNRESOLVED | §3 execution precondition | Session | Execution accountability not established | §3 |
| PO-0C-EX-013 | Authorize execution date and time window | UNRESOLVED | §3 execution precondition | Session | Live probe execution not authorized (§18) | §3; §16–§18 |

### Appendix C — Authorization and governance boundaries

Appendix entries (not Part A decision entries). Status: **NOT AUTHORIZED** as stated in the source specification.

| Appendix entry ID | Decision topic | Current status | Why required | Affected request(s) | Blocking effect if unresolved | Source reference |
|-------------------|----------------|----------------|--------------|---------------------|-------------------------------|------------------|
| PO-0C-AUTH-001 | Account operational-readiness check authorization | NOT AUTHORIZED | Administrative readiness distinct from parameter table | Session | §15; cannot interpret account failures as API FAIL without readiness | §18; §15 |
| PO-0C-AUTH-002 | Live compatibility probe execution authorization | NOT AUTHORIZED | Distinct from fixing §6.3 | R1–R4 | No live API calls permitted | §16–§18; specification final line |
| PO-0C-AUTH-003 | Test-only implementation authorization | NOT AUTHORIZED | Execution path not designed in specification | N/A (implementation) | No test-only client authorized | §18; §15 Stage 0B boundary |
| PO-0C-AUTH-004 | Provider-content retention authorization (beyond approved transient boundary) | NOT AUTHORIZED | §12.3 default prohibition | Session | Retention stop conditions (§11) | §12.1–§12.3; §16 |
| PO-0C-AUTH-005 | Corridor-verification method (separate document and review) | NOT AUTHORIZED | Gate 1 and route-identity boundary | Interpretation of R2/R4 results | E11/E311 claims blocked; Gate 1 remains INCONCLUSIVE (§9) | §14; §9; §18 |

---

## Register summary

- **Total reproducibility decisions:** **39**
- **Resolved reproducibility decisions:** **1** (PO-0C-REQ-008)
- **Unresolved reproducibility decisions:** **38**
- **Appendix execution preconditions:** **13**
- **Appendix authorization and governance boundaries:** **5**
- **Total traceability entries:** **57**

STAGE 0C PRODUCT OWNER DECISION REGISTER — PO-0C-REQ-008 RECORDED; NO EXECUTION AUTHORIZED
