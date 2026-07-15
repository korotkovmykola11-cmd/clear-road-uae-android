# Stage A — Decision Compression Contract v0.1

**Status:** Candidate contract (documentation only)
**Version:** 0.1
**Scope:** Synthetic stimuli preparation for a future Stage A feasibility pilot

---

## Stage boundary

This package prepares candidate contracts and synthetic stimuli for a future Stage A feasibility pilot.

It does not execute Stage A.

Stage A remains **Not started**.

No conclusions about user behaviour, decision speed, task comprehension, product value, or preference value are established by this package.

No participants are recruited or tested.

---

## Project status (unchanged)

| Item | Status |
|------|--------|
| Stage 0B | **Completed** |
| Terms feasibility | **In progress — F01–F05 reviewed** |
| Stage A | **Not started** |
| Stage B | **Not started** |
| Stage C1 | **Not started** |
| Stage C2 | **Not started** |
| Waypoint research | **Blocked** |
| Implementation | **Not authorized** |

Research Decision Record 001 is approved and is not reopened by this document.

---

## MARSHIO identity (working boundary)

MARSHIO is a decision layer over already computed route candidates.

An external routing provider computes candidate routes.

MARSHIO may present a mode-consistent decision and explain the comparison using reliable route facts.

MARSHIO is not a routing engine.

MARSHIO does not create routes absent from the provider.

Actual navigation is performed by an external navigator.

This contract does not authorize routing-provider exploration, waypoint research, multi-provider discovery, or production implementation.

---

## Candidate A — Decision Compression

### Hypothesis to be tested later

A Decision Compression presentation may help a driver make a mode-consistent route choice more quickly than a neutral presentation of the same route facts.

This document does **not** claim that value is already demonstrated.

### Purpose

Decision Compression receives reliable facts from two or three synthetic route candidates and returns exactly one of three outcome classes:

| Outcome | Meaning |
|---------|---------|
| **RECOMMEND** | One route is selected with a mode-relevant measurable advantage |
| **EQUIVALENT** | No meaningful mode-relevant difference; no artificial winner |
| **INSUFFICIENT_DATA** | Required facts are missing or unreliable; no winner assigned |

This is a **contract behaviour** for synthetic cases. It is not a production algorithm. It is not implementation authorization.

---

## Experimental separation

**Dataset A — Decision Compression**

- Exactly 8 cases (documented in `stage-a-synthetic-cases-v0.1.md`)
- 4 FASTEST; 4 NO_TOLLS
- `preference = none` in every case
- Only **ETA** and **reliable toll** may affect the decision

**Dataset B — Explicit Route Preference** is defined in a separate contract and truth table. It is not part of Dataset A.

---

## Synthetic route input schema

Each synthetic route candidate must contain:

| Field | Rule |
|-------|------|
| `routeId` | Required. Unique inside the case. Examples: Route A, Route B, Route C |
| `etaMinutes` | Positive whole number or `UNKNOWN` |
| `tollStatus` | `KNOWN` or `UNKNOWN` |
| `tollAed` | Non-negative whole AED when `tollStatus = KNOWN`. Must be a multiple of **AED 4** (see Synthetic toll domain). Absent when `UNKNOWN` |
| `distanceKm` | Positive number or `UNKNOWN`. Display/comparison only. **Must not influence Candidate A selection** |
| `corridorKey` | Normalized machine-comparable synthetic corridor identifier. Examples: E11, E311, E44. **Must not influence Candidate A selection** |
| `corridorLabel` | Human-readable display text. Examples: E11 via D86, E311 via E44. **Must not influence Candidate A selection** |

`corridorKey` must be explicitly present in each fixture. Do not derive `corridorKey` from `corridorLabel`.

### Dataset A corridor boundary

In Dataset A:

- `corridorKey` and `corridorLabel` are display/identification only
- They must not influence Decision Compression selection

### Structural boundaries (every case)

- Minimum 2 routes; maximum 3 routes
- Unique `routeId` values
- Uniform units: whole minutes, AED, kilometres
- `etaMinutes > 0` when known
- `tollAed ≥ 0` when known, or toll `UNKNOWN`

### Prohibited inputs

Coordinates; polylines; Google route IDs; Google road content; traffic intervals; real route payloads; provider response objects.

---

## Supported modes (Candidate A only)

| Mode | Included |
|------|----------|
| FASTEST | Yes |
| NO_TOLLS | Yes |

Excluded from Candidate A: Smooth Drive; Easy Drive; stress; traffic quality; safety; stability; AI; route learning; Route Intelligence.

---

## Reliability matrix

| Mode | Required for selection | Display-only / optional |
|------|------------------------|-------------------------|
| FASTEST | Reliable ETA for every candidate | toll, distance, corridor |
| NO_TOLLS | Reliable toll **and** reliable ETA for every candidate | distance, corridor |

### Reliability rules

| Condition | Outcome |
|-----------|---------|
| Unknown ETA for any candidate | `INSUFFICIENT_DATA` for FASTEST |
| Unknown toll for any candidate | `INSUFFICIENT_DATA` for NO_TOLLS |
| Unknown toll in FASTEST | Does **not** invalidate FASTEST selection |
| Unknown toll in FASTEST | MARSHIO must **not** claim same toll, cheaper, more expensive, or no toll |
| Unknown toll in FASTEST | May state: *Toll comparison unavailable.* |
| Missing or non-unique `routeId` | `INSUFFICIENT_DATA` |

**Decision variables for Experiment A:** ETA and reliable toll only.

Distance, `corridorKey`, and `corridorLabel` must not change the outcome.

---

## Synthetic toll domain v0.1

For synthetic v0.1, every `KNOWN` `tollAed` value must be a non-negative multiple of **AED 4**.

Supported examples: 0, 4, 8, 12, 16, …

A `tollAed` value outside this synthetic domain makes the fixture **invalid**.

| Status | Meaning |
|--------|---------|
| **BLOCKED** | Fixture-validation status — fixture is invalid and must not receive a normal Decision Engine outcome |
| RECOMMEND / EQUIVALENT / INSUFFICIENT_DATA | Valid Decision Engine outcomes only for fixtures that pass domain validation |

**BLOCKED is not a fourth Decision Engine outcome.**

Apply this domain to Dataset A and Dataset B fixtures.

---

## Synthetic thresholds v0.1

These values are **contract constants for synthetic v0.1 only**. They are not validated user thresholds or production policy.

| Constant | Value |
|----------|-------|
| `ETA_MEANINGFUL_DIFFERENCE_MIN` | **3 minutes** |
| `MIN_TOLL_DIFFERENCE_AED` | **4 AED** |

### ETA equivalence

- **0–2 minutes inclusive** → not meaningful (equivalent for mode purposes)
- **3+ minutes** → meaningful

### Toll equivalence

- **0 AED difference** → same toll
- **4+ AED difference** → meaningful in the synthetic contract

All synthetic toll amounts in Dataset A use multiples of AED 4 only (0, 4, 8, 12, …).

---

## FASTEST semantics

### Selection order

1. Verify reliable ETA for all candidates.
2. Find candidate(s) with minimum ETA.
3. Compare minimum ETA to the next-lowest ETA.

### FASTEST → RECOMMEND

When the fastest candidate is at least **3 minutes** faster than the next candidate:

- **Required:** selected route; ETA advantage magnitude; comparison basis
- **Conditional trade-off:** only when toll facts are reliable **and** tolls differ
- **If toll unknown:** state *Toll comparison unavailable* — do not invent toll comparison

Examples:

```
Take Route A
6 min faster
Same toll
```

```
Take Route A
5 min faster
AED 8 more
```

```
Take Route A
5 min faster
Toll comparison unavailable
```

### FASTEST → EQUIVALENT

When the two leading candidates differ by **at most 2 minutes** ETA:

- No FASTEST winner assigned
- State: *Equivalent for the selected FASTEST mode* — or: *No meaningful arrival-time difference for FASTEST mode.*
- Do **not** claim routes are equal in toll, distance, geometry, corridor, or overall quality
- If reliable toll differs, disclose toll difference without assigning FASTEST winner

Examples:

```
Equivalent for the selected FASTEST mode
No meaningful ETA difference for FASTEST
Route B costs AED 8 less
No FASTEST winner assigned
```

```
Routes are effectively equivalent for FASTEST
Choose either
```

### FASTEST → INSUFFICIENT_DATA

When any candidate ETA is `UNKNOWN`:

```
Not enough reliable information
ETA information is incomplete
No recommendation assigned
```

---

## NO_TOLLS semantics

Decision Compression does **not** introduce a time-for-toll tolerance. Time-for-toll tolerance belongs only to Explicit Route Preference (Dataset B).

### Selection order

1. Verify reliable toll and ETA for all candidates.
2. Find candidate(s) with minimum toll.
3. Compare minimum toll to the next-lowest toll.

### NO_TOLLS → RECOMMEND (by toll)

When one candidate is at least **AED 4** cheaper than the next:

- Primary objective: minimum reliable toll
- **Conditional:** show ETA disadvantage if selected route is slower

Example:

```
Take Route B
AED 8 less
4 min longer
```

### NO_TOLLS → time tie-break (same minimum toll)

When multiple candidates share the same minimum toll:

1. Among tied candidates, find minimum ETA
2. Assign winner only if ETA advantage is **≥ 3 minutes**
3. If ETA difference is **0–2 minutes** → `EQUIVALENT`

Example:

```
Take Route A
Same toll
6 min faster
```

### NO_TOLLS → EQUIVALENT

When minimum toll is equal **and** ETA difference between leading tied candidates is **≤ 2 minutes**:

```
Routes are effectively equivalent for NO_TOLLS
Same toll
No meaningful ETA difference
Choose either
```

### NO_TOLLS → INSUFFICIENT_DATA

When toll of any candidate is `UNKNOWN`:

```
Not enough reliable information
Toll information is incomplete
No recommendation assigned
```

---

## Outcome contract

### RECOMMEND

| Element | Required? |
|---------|-----------|
| Selected route | Yes |
| Mode-relevant measurable advantage | Yes |
| Comparison basis | Yes |
| Primary trade-off | **Only when** selected route is worse on another reliable displayed metric |

If no measured downside exists, MARSHIO must **not** invent a trade-off.

> MARSHIO never invents a reason or trade-off.

### EQUIVALENT

| Element | Required? |
|---------|-----------|
| No artificial winner | Yes |
| Mode for which no meaningful difference exists | Yes |
| Other reliable differences disclosed | When present |
| Claim of geometric/objective identity | **Prohibited** |

### INSUFFICIENT_DATA

| Element | Required? |
|---------|-----------|
| Missing/unreliable field identified | Yes |
| No selected route | Yes |
| No recommendation | Yes |
| No fallback inference | Yes |

---

## Decision flow

```
Are all mode-required facts reliable?
│
├── No
│   └── INSUFFICIENT_DATA
│       ├── State missing/unreliable field
│       └── Do not assign a winner
│
└── Yes
    │
    ├── Apply exact mode semantics (FASTEST or NO_TOLLS)
    │
    ├── Is the mode-relevant difference meaningful?
    │   │
    │   ├── No (within equivalence threshold)
    │   │   └── EQUIVALENT
    │   │       ├── Do not assign a winner
    │   │       └── Disclose other reliable differences
    │   │
    │   └── Yes (≥ threshold)
    │       └── RECOMMEND
    │           ├── State selected route
    │           ├── State magnitude of advantage
    │           └── State trade-off only if supported by reliable facts
```

Excluded from this contract: probabilistic ranking; machine learning; confidence scores; bootstrap; power analysis; RCT design; live thresholds; production scoring.

---

## Anti-arbitrariness rule

Cursor (and any future author) must **not** invent:

- Additional thresholds
- Preference precedence
- Missing-value fallbacks
- Hidden ranking criteria
- Reliability assumptions beyond this matrix
- Extra outcome classes
- Production behaviour

If a required rule is absent or contradictory, the affected item must be reported as **BLOCKED** instead of assigning an expected result.

---

## Claims boundary

### Prohibited language

best route; optimal route; safest route; easiest route; calmer route; lower stress; better traffic; users will make better decisions; users will decide faster; personalization creates value; validated; production-ready.

### Permitted synthetic descriptions

faster; slower; cheaper; more expensive; longer; same toll; toll comparison unavailable; toll information incomplete; ETA information incomplete; no meaningful difference for selected mode.

---

## Future experiment boundary (not executed)

**Experiment A — Decision Compression** (future, separately authorized)

| Arm | Content |
|-----|---------|
| Baseline | Neutral rows containing the same synthetic route facts |
| Treatment | One Decision Compression outcome: RECOMMEND, EQUIVALENT, or INSUFFICIENT_DATA |

**Future research question:** Does Decision Compression help a participant make a mode-consistent choice more quickly than a neutral presentation of the same facts?

This document does not design or authorize a participant study.

---

## Related artifacts

| Artifact | Location |
|----------|----------|
| Explicit Route Preference Contract | `docs/stage-a-explicit-preference-contract-v0.1.md` |
| Synthetic cases and truth tables | `docs/stage-a-synthetic-cases-v0.1.md` |
| Staged validation methodology | `docs/research-decision-record-001-staged-validation.md` |
