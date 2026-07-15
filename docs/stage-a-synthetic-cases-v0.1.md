# Stage A — Synthetic Cases v0.1

**Status:** Candidate stimuli (documentation only)
**Version:** 0.1
**Scope:** Dataset A and Dataset B truth tables for future Stage A preparation

---

## Stage boundary

This package prepares candidate contracts and synthetic stimuli for a future Stage A feasibility pilot.

It does not execute Stage A.

Stage A remains **Not started**.

No participants are recruited or tested.

No conclusions about task comprehensibility, user behaviour, baseline performance, decision time, or product value may be made from this document.

---

## Contract references

| Contract | File |
|----------|------|
| Decision Compression | `docs/stage-a-decision-compression-contract-v0.1.md` |
| Explicit Route Preference | `docs/stage-a-explicit-preference-contract-v0.1.md` |

### Fixed thresholds (v0.1)

| Constant | Value |
|----------|-------|
| `ETA_MEANINGFUL_DIFFERENCE_MIN` | 3 minutes |
| `MIN_TOLL_DIFFERENCE_AED` | 4 AED |

---

## Dataset A — Decision Compression

**Total: 8 cases** — 4 FASTEST, 4 NO_TOLLS — **preference = none in every case**

Only ETA and reliable toll affect the decision. `distanceKm`, `corridorKey`, and `corridorLabel` are display/identification only.

All `tollAed` values are multiples of AED 4 (synthetic toll domain).

---

### A-F1 — meaningful ETA advantage

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | 24 | E11 | E11 via D86 |
| Route B | 36 | KNOWN | 8 | 26 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | FASTEST |
| Required facts reliable? | Yes |
| **Expected outcome** | **RECOMMEND** |
| **Expected selected route** | Route A |
| **Expected explanation** | Take Route A · 6 min faster · Same toll |

---

### A-F2 — mode-equivalent with toll difference

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | 24 | E11 | E11 via D86 |
| Route B | 32 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | FASTEST |
| Required facts reliable? | Yes |
| **Expected outcome** | **EQUIVALENT** |
| **Expected selected route** | None |
| **Expected explanation** | Equivalent for the selected FASTEST mode · No meaningful ETA difference for FASTEST · Route B costs AED 8 less · No FASTEST winner assigned |

---

### A-F3 — missing ETA

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | 24 | E11 | E11 via D86 |
| Route B | UNKNOWN | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | FASTEST |
| Required facts reliable? | No (ETA incomplete) |
| **Expected outcome** | **INSUFFICIENT_DATA** |
| **Expected selected route** | None |
| **Expected explanation** | Not enough reliable information · ETA information is incomplete · No recommendation assigned |

---

### A-F4 — unknown toll does not block FASTEST

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | UNKNOWN | — | 24 | E11 | E11 via D86 |
| Route B | 35 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | FASTEST |
| Required facts reliable? | Yes (ETA only required for FASTEST) |
| **Expected outcome** | **RECOMMEND** |
| **Expected selected route** | Route A |
| **Expected explanation** | Take Route A · 5 min faster · Toll comparison unavailable |

---

### A-N1 — meaningful toll advantage

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | 24 | E11 | E11 via D86 |
| Route B | 34 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | NO_TOLLS |
| Required facts reliable? | Yes |
| **Expected outcome** | **RECOMMEND** |
| **Expected selected route** | Route B |
| **Expected explanation** | Take Route B · AED 8 less · 4 min longer |

---

### A-N2 — same toll, meaningful ETA tie-break

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 0 | 24 | E11 | E11 via D86 |
| Route B | 36 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | NO_TOLLS |
| Required facts reliable? | Yes |
| **Expected outcome** | **RECOMMEND** |
| **Expected selected route** | Route A |
| **Expected explanation** | Take Route A · Same toll · 6 min faster |

---

### A-N3 — unknown toll

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | UNKNOWN | — | 24 | E11 | E11 via D86 |
| Route B | 32 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | NO_TOLLS |
| Required facts reliable? | No (toll incomplete) |
| **Expected outcome** | **INSUFFICIENT_DATA** |
| **Expected selected route** | None |
| **Expected explanation** | Not enough reliable information · Toll information is incomplete · No recommendation assigned |

---

### A-N4 — same toll and non-meaningful ETA difference

| routeId | etaMinutes | tollStatus | tollAed | distanceKm | corridorKey | corridorLabel |
|---------|------------|------------|---------|------------|-------------|---------------|
| Route A | 30 | KNOWN | 0 | 24 | E11 | E11 via D86 |
| Route B | 32 | KNOWN | 0 | 28 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| Mode | NO_TOLLS |
| Required facts reliable? | Yes |
| **Expected outcome** | **EQUIVALENT** |
| **Expected selected route** | None |
| **Expected explanation** | Routes are effectively equivalent for NO_TOLLS · Same toll · No meaningful ETA difference · Choose either |

---

## Dataset A summary

| Case | Mode | Outcome | Winner |
|------|------|---------|--------|
| A-F1 | FASTEST | RECOMMEND | Route A |
| A-F2 | FASTEST | EQUIVALENT | None |
| A-F3 | FASTEST | INSUFFICIENT_DATA | None |
| A-F4 | FASTEST | RECOMMEND | Route A |
| A-N1 | NO_TOLLS | RECOMMEND | Route B |
| A-N2 | NO_TOLLS | RECOMMEND | Route A |
| A-N3 | NO_TOLLS | INSUFFICIENT_DATA | None |
| A-N4 | NO_TOLLS | EQUIVALENT | None |

**Total: 8** — preference = none in all cases.

---

## Dataset B — Explicit Route Preference

**Total: 8 cases** — 2 per profile (P1–P4) — **exactly one active preference per case**

Dataset B is separate from Dataset A. It does not modify Dataset A outcomes.

Candidate B uses the reduced route schema: `routeId`, `etaMinutes`, `tollStatus`, `tollAed`, `corridorKey`, `corridorLabel`. `distanceKm` is omitted.

All `tollAed` values are multiples of AED 4 (synthetic toll domain).

P3 and P4 match corridors using **exact equality on `corridorKey` only**.

---

### B-P1a — Meaningful Fastest confirms existing base route

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 12 | E11 | E11 via D86 |
| Route B | 34 | KNOWN | 0 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P1 — Meaningful Fastest |
| preferenceStatus | **APPLIED** |
| preferenceSelectedRoute | Route A |
| finalDecision | RECOMMEND Route A |
| expectedExplanation | Meaningful fastest preference · Take Route A · 4 min faster · AED 12 more · Preference confirms lowest reliable ETA with meaningful advantage |

P1 confirms the existing base route. It does not select the alternative.

---

### B-P1b — Meaningful Fastest does not create winner on non-meaningful ETA gap

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 31 | KNOWN | 0 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | EQUIVALENT |
| baseSelectedRoute | None |
| activePreference | P1 — Meaningful Fastest |
| preferenceStatus | **NOT_APPLIED** |
| preferenceSelectedRoute | None |
| finalDecision | EQUIVALENT |
| expectedExplanation | Equivalent for the selected FASTEST mode · No meaningful ETA difference for FASTEST · Route B costs AED 8 less · No FASTEST winner assigned · Preference does not override equivalence |

No artificial winner.

---

### B-P2a — Toll Tolerance at inclusive boundary (5 min / AED 4)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 35 | KNOWN | 0 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P2 — Toll Tolerance |
| preferenceStatus | **APPLIED** |
| preferenceSelectedRoute | Route B |
| finalDecision | RECOMMEND Route B |
| expectedExplanation | Toll tolerance preference · Take Route B · AED 8 less · 5 min longer · Within stated tolerance |

---

### B-P2b — Toll Tolerance rejected (6 extra minutes)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 36 | KNOWN | 0 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P2 — Toll Tolerance |
| preferenceStatus | **NOT_APPLIED** |
| preferenceSelectedRoute | None |
| finalDecision | RECOMMEND Route A |
| expectedExplanation | Take Route A · 6 min faster · AED 8 more · Preference not applied — exceeds 5 min tolerance |

---

### B-P3a — Preferred Corridor E311 at inclusive boundary (3 min)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 33 | KNOWN | 8 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P3 — Preferred Corridor |
| preferenceStatus | **APPLIED** |
| preferenceSelectedRoute | Route B |
| finalDecision | RECOMMEND Route B |
| expectedExplanation | Preferred corridor preference · Take Route B · E311 · Same toll · 3 min longer · Within stated corridor preference |

---

### B-P3b — Preferred Corridor E311 rejected (4 min)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 34 | KNOWN | 8 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P3 — Preferred Corridor |
| preferenceStatus | **NOT_APPLIED** |
| preferenceSelectedRoute | None |
| finalDecision | RECOMMEND Route A |
| expectedExplanation | Take Route A · 4 min faster · Same toll · Preference not applied — E311 exceeds 3 min slack |

---

### B-P4a — Avoided Corridor E11 at inclusive boundary (8 min)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 38 | KNOWN | 8 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P4 — Avoided Corridor |
| preferenceStatus | **APPLIED** |
| preferenceSelectedRoute | Route B |
| finalDecision | RECOMMEND Route B |
| expectedExplanation | Avoid E11 preference · Take Route B · E311 · Same toll · 8 min longer · E11 avoided within stated boundary |

---

### B-P4b — Avoided Corridor E11 rejected (9 min penalty)

| routeId | etaMinutes | tollStatus | tollAed | corridorKey | corridorLabel |
|---------|------------|------------|---------|-------------|---------------|
| Route A | 30 | KNOWN | 8 | E11 | E11 via D86 |
| Route B | 39 | KNOWN | 8 | E311 | E311 via E44 |

| Field | Value |
|-------|-------|
| baseMode | FASTEST |
| baseDecision | RECOMMEND Route A |
| baseSelectedRoute | Route A |
| activePreference | P4 — Avoided Corridor |
| preferenceStatus | **NOT_APPLIED** |
| preferenceSelectedRoute | None |
| finalDecision | RECOMMEND Route A |
| expectedExplanation | Take Route A · 9 min faster · Same toll · Preference not applied — every non-E11 alternative exceeds 8 min penalty |

---

## Dataset B truth table

| Case | Route facts | Base mode | Base decision | Base selected route | Active preference | Preference status | Preference selected route | Final decision | Expected explanation |
|------|-------------|-----------|---------------|---------------------|-------------------|-------------------|---------------------------|----------------|----------------------|
| B-P1a | A: 30 min, AED 12, E11 · B: 34 min, AED 0, E311 | FASTEST | RECOMMEND Route A | Route A | P1 Meaningful Fastest | APPLIED | Route A | RECOMMEND Route A | Meaningful fastest confirms Route A (4 min faster) |
| B-P1b | A: 30 min, AED 8, E11 · B: 31 min, AED 0, E311 | FASTEST | EQUIVALENT | None | P1 Meaningful Fastest | NOT_APPLIED | None | EQUIVALENT | No artificial winner; equivalence preserved |
| B-P2a | A: 30 min, AED 8, E11 · B: 35 min, AED 0, E311 | FASTEST | RECOMMEND Route A | Route A | P2 Toll Tolerance | APPLIED | Route B | RECOMMEND Route B | AED 8 saving, 5 min penalty — within tolerance |
| B-P2b | A: 30 min, AED 8, E11 · B: 36 min, AED 0, E311 | FASTEST | RECOMMEND Route A | Route A | P2 Toll Tolerance | NOT_APPLIED | None | RECOMMEND Route A | 6 min penalty exceeds 5 min max |
| B-P3a | A: 30 min, AED 8, E11 · B: 33 min, AED 8, E311 | FASTEST | RECOMMEND Route A | Route A | P3 Preferred Corridor | APPLIED | Route B | RECOMMEND Route B | E311 exactly 3 min slower — within slack |
| B-P3b | A: 30 min, AED 8, E11 · B: 34 min, AED 8, E311 | FASTEST | RECOMMEND Route A | Route A | P3 Preferred Corridor | NOT_APPLIED | None | RECOMMEND Route A | E311 4 min slower exceeds 3 min max |
| B-P4a | A: 30 min, AED 8, E11 · B: 38 min, AED 8, E311 | FASTEST | RECOMMEND Route A | Route A | P4 Avoided Corridor | APPLIED | Route B | RECOMMEND Route B | Non-E11 exactly 8 min slower — avoidance qualifies |
| B-P4b | A: 30 min, AED 8, E11 · B: 39 min, AED 8, E311 | FASTEST | RECOMMEND Route A | Route A | P4 Avoided Corridor | NOT_APPLIED | None | RECOMMEND Route A | Non-E11 9 min slower exceeds 8 min boundary |

**Total: 8** — one preference per case; no combined profiles.

---

## Outcome counts

### Dataset A — Decision Engine outcomes

| Outcome | Count |
|---------|-------|
| RECOMMEND | 4 |
| EQUIVALENT | 2 |
| INSUFFICIENT_DATA | 2 |
| fixture BLOCKED | 0 |

### Dataset B — Preference statuses

| Status | Count |
|--------|-------|
| APPLIED | 4 |
| NOT_APPLIED | 4 |
| INSUFFICIENT_FOR_PREFERENCE | 0 |
| fixture BLOCKED | 0 |

`INSUFFICIENT_FOR_PREFERENCE` is fully defined in the Explicit Route Preference Contract but is **not fixture-covered in v0.1**. No claim of complete missing-data behavioral coverage is made.

### Dataset B — Final decisions

| Outcome | Count |
|---------|-------|
| RECOMMEND | 7 |
| EQUIVALENT | 1 |
| INSUFFICIENT_DATA | 0 |

---

## Dataset summary

| Dataset | Breakdown | Total |
|---------|-----------|-------|
| Dataset A | FASTEST = 4; NO_TOLLS = 4 | 8 |
| Dataset B | P1 Meaningful Fastest = 2; P2 Toll Tolerance = 2; P3 Preferred Corridor = 2; P4 Avoided Corridor = 2 | 8 |
| **Combined** | | **16** |

---

## Validation self-check

Checks performed against v0.1 contracts and fixtures:

| Check | Result |
|-------|--------|
| P1 wording (Meaningful Fastest) agrees with B-P1b (NOT_APPLIED on 0–2 min gap) | Pass |
| Every route row has explicit `corridorKey` | Pass (16 cases, 32 routes) |
| Corridor matching uses exact `corridorKey` equality only | Pass (P3/P4 contract + fixtures) |
| Dataset A `corridorKey`/`corridorLabel` display-only | Pass |
| Candidate B contains exactly 2 routes per case | Pass (8 cases) |
| Every APPLIED case identifies `preferenceSelectedRoute` | Pass (B-P1a, B-P2a, B-P3a, B-P4a) |
| B-P1a may select same route as `baseSelectedRoute` | Pass (Route A confirms base) |
| B-P1b supports `baseDecision = EQUIVALENT` with no base-selected route | Pass |
| No preference repairs `INSUFFICIENT_DATA` | Pass (no such fixtures in v0.1) |
| All known `tollAed` values are multiples of AED 4 | Pass (0, 8, 12 only) |
| Unsupported toll values would produce fixture BLOCKED | Pass (contract-defined; 0 fixtures) |
| Every profile has required-facts matrix entry | Pass |
| Missing preference facts → `INSUFFICIENT_FOR_PREFERENCE` | Pass (contract-defined; 0 fixtures) |
| Dataset A preference = none | Pass (all 8) |
| Dataset B one preference per case | Pass (all 8) |
| All 16 valid cases deterministic | Pass |
| No Strict Fastest wording remains | Pass |
| `defaultMode` removed from candidate boundary | Pass (preference contract) |
| `distanceKm` omitted from Dataset B schema | Pass |

| Summary | Result |
|---------|--------|
| Contradictions found | **None** |
| Ambiguous cases requiring invented thresholds | **None** |
| Missing definitions within scope | **None** |
| Unsupported fixtures | **0** |
| fixture BLOCKED count | **0** |
| INSUFFICIENT_FOR_PREFERENCE fixture coverage | **0** (contract-defined, not fixture-covered) |

---

## Anti-arbitrariness confirmation

No additional thresholds, preference precedence rules, missing-value fallbacks, or hidden ranking criteria were added beyond the owner-approved v0.1 contract constants.

---

## Related artifacts

| Artifact | Location |
|----------|------|
| Decision Compression Contract | `docs/stage-a-decision-compression-contract-v0.1.md` |
| Explicit Route Preference Contract | `docs/stage-a-explicit-preference-contract-v0.1.md` |
| Research Decision Record 001 | `docs/research-decision-record-001-staged-validation.md` |
