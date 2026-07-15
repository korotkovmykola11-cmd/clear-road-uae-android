# Stage A — Explicit Route Preference Contract v0.1

**Status:** Candidate contract (documentation only)
**Version:** 0.1
**Scope:** Synthetic preference stimuli preparation for a future separately authorized experiment

---

## Stage boundary

This package prepares candidate contracts and synthetic stimuli for a future Stage A feasibility pilot.

It does not execute Stage A.

Stage A remains **Not started**.

No conclusions about user behaviour, decision speed, task comprehension, product value, or preference value are established by this package.

---

## Candidate B — Explicit Route Preference

### Hypothesis to be tested later

An explicitly stated driver preference may produce an additional understandable decision difference when applied to the same reliable synthetic route facts.

This document does **not** claim that preference value is already demonstrated.

### Naming

Use **Explicit Route Preference** — not Personal Route Memory — for v0.1 because:

- Automatic learning is prohibited
- Trip history is not stored
- User memory is not implemented

---

## Experimental separation

| Dataset | Purpose | Cases | Preferences |
|---------|---------|-------|-------------|
| **Dataset A** | Decision Compression | 8 | `none` in every case |
| **Dataset B** | Explicit Route Preference | 8 | Exactly one active preference per case |

Dataset B:

- Does **not** modify the Decision Compression Contract
- Is **not** part of Dataset A stimuli
- Is **not** an executed experiment under this package
- Makes **no** claim of incremental value over Dataset A

### Required research sequence

```
Experiment A — Decision Compression
        ↓
   (separate authorization only)
        ↓
Experiment B — Explicit Route Preference
```

These experiments must not be mixed in one treatment or one truth table.

---

## Candidate B boundary

Candidate B:

- Supports **exactly two route candidates** per case
- Does not train automatically
- Does not use trip history
- Does not store provider content
- Allows **exactly one** active preference per case
- Does not authorize persistence or implementation

Selection among three or more route candidates is **outside Candidate B v0.1**. A separate deterministic candidate-selection rule would be required before supporting three or more routes.

---

## Candidate B reduced route schema

Candidate B uses a reduced synthetic route schema:

| Field | Rule |
|-------|------|
| `routeId` | Required. Unique inside the case |
| `etaMinutes` | Positive whole number or `UNKNOWN` |
| `tollStatus` | `KNOWN` or `UNKNOWN` |
| `tollAed` | Multiple of AED 4 when `KNOWN` (see Decision Compression toll domain) |
| `corridorKey` | Normalized machine-comparable identifier. Required for P3/P4 |
| `corridorLabel` | Human-readable display text only |

`distanceKm` is **omitted** because it does not participate in any Candidate B preference rule. This is not permission to use distance as a hidden rule.

### Corridor matching (Dataset B)

In Dataset B:

- P3 and P4 may use **exact `corridorKey` equality only**
- `corridorLabel` must not participate in preference matching

| Field | Role |
|-------|------|
| `corridorKey` | Used for P3/P4 preference matching — **exact equality only** |
| `corridorLabel` | Display only — must not participate in preference matching |

Prohibited: substring matching; text extraction; normalization inference; hidden matching rules.

`corridorKey` must be explicitly present in each fixture. Do not derive it from `corridorLabel`.

---

## Base-decision model

Candidate B evaluates the **same two routes** that produced `baseDecision`.

`baseDecision` must already have been produced by the Decision Compression Contract.

| baseDecision | baseSelectedRoute | otherRoute |
|--------------|-------------------|------------|
| RECOMMEND | Route selected by Decision Compression | The second route |
| EQUIVALENT | **None** — no route may be inferred as base winner | Both routes remain a mode-equivalent pair |
| INSUFFICIENT_DATA | N/A | Preference not evaluated — see below |

### When baseDecision = INSUFFICIENT_DATA

```
preferenceStatus = INSUFFICIENT_FOR_PREFERENCE
preferenceSelectedRoute = None
finalDecision = baseDecision
```

A preference may **not** repair, replace, or override `baseDecision = INSUFFICIENT_DATA`.

### Preference result fields

Every Candidate B case must contain:

| Field | Description |
|-------|-------------|
| `baseMode` | FASTEST or NO_TOLLS |
| `baseDecision` | RECOMMEND \| EQUIVALENT \| INSUFFICIENT_DATA |
| `baseSelectedRoute` | Route ID or None |
| `activePreference` | One profile (P1–P4) |
| `preferenceStatus` | APPLIED \| NOT_APPLIED \| INSUFFICIENT_FOR_PREFERENCE |
| `preferenceSelectedRoute` | Route ID or None |
| `finalDecision` | Outcome after preference evaluation |
| `expectedExplanation` | Human-readable explanation contract |

### Result rules

**When `preferenceStatus = APPLIED`:**

- `preferenceSelectedRoute` must be present
- `finalDecision` uses `preferenceSelectedRoute`
- `preferenceSelectedRoute` may equal `baseSelectedRoute` (confirming base route)
- `preferenceSelectedRoute` may instead be the other route
- **APPLIED does not necessarily mean the route changed**

**When `preferenceStatus = NOT_APPLIED`:**

- `preferenceSelectedRoute = None`
- `finalDecision = baseDecision`

**When `preferenceStatus = INSUFFICIENT_FOR_PREFERENCE`:**

- `preferenceSelectedRoute = None`
- `finalDecision = baseDecision`
- The preference must not invent a replacement route

### Prohibited universal rule

Do **not** use: *When APPLIED, the alternative always replaces the base-selected route.*

That rule is invalid because:

- P1 may confirm the existing base-selected route
- `baseDecision = EQUIVALENT` has no selected route
- An applied preference may select either member of the pair

A preference may modify or confirm an existing RECOMMEND decision. A preference may remain NOT_APPLIED when `baseDecision = EQUIVALENT`.

---

## Preference application model (summary)

Each Candidate B case applies one profile to the two-route pair defined above.

### Rules

| Rule | Value |
|------|-------|
| Route candidates per case | Exactly **2** |
| Active preferences per case | Exactly **one** |
| Threshold comparisons | **Inclusive** (exactly at boundary qualifies) |
| Multiple-preference precedence | **Out of scope** for v0.1 |
| Combined toll tolerance + corridor preference in one case | **Prohibited** |

### Status semantics

| Status | Meaning |
|--------|---------|
| **APPLIED** | Preference conditions met; `preferenceSelectedRoute` assigned per profile rules |
| **NOT_APPLIED** | Preference conditions not met; `preferenceSelectedRoute = None`; `finalDecision = baseDecision` |
| **INSUFFICIENT_FOR_PREFERENCE** | Required facts missing; `preferenceSelectedRoute = None`; `finalDecision = baseDecision` |

---

## Candidate B reliability matrix

| Profile | Required facts |
|---------|----------------|
| P1 Meaningful Fastest | Reliable ETA for both routes |
| P2 Toll Tolerance | Reliable ETA and reliable toll for both routes |
| P3 Preferred Corridor | Reliable ETA and explicit `corridorKey` for both routes |
| P4 Avoided Corridor | Reliable ETA and explicit `corridorKey` for both routes |

### Common rule

If any profile-required fact is missing:

```
preferenceStatus = INSUFFICIENT_FOR_PREFERENCE
preferenceSelectedRoute = None
finalDecision = baseDecision
```

The preference must not invent a replacement route.

### P2-specific reliability

Unknown ETA or unknown toll for either route → `INSUFFICIENT_FOR_PREFERENCE`

### P3/P4-specific reliability

Missing `corridorKey` or missing ETA for either route → `INSUFFICIENT_FOR_PREFERENCE`

---

## Synthetic profiles (v0.1)

Deterministic rules for **exactly two routes**.

### Profile P1 — Meaningful Fastest

**Human-readable:** Choose the lowest reliable ETA only when its advantage is meaningful under the Decision Compression threshold.

**Normalized:**

```
type = MEANINGFUL_FASTEST
minimumMeaningfulEtaAdvantageMinutes = 3
```

**Rules:**

Compare the ETA of the two routes.

| Condition | Result |
|-----------|--------|
| One route is ≥ 3 minutes faster than the other | `APPLIED`; `preferenceSelectedRoute` = faster route |
| ETA difference is 0–2 minutes | `NOT_APPLIED`; `preferenceSelectedRoute = None`; `finalDecision = baseDecision` |
| Any required ETA is `UNKNOWN` | `INSUFFICIENT_FOR_PREFERENCE`; `preferenceSelectedRoute = None`; `finalDecision = baseDecision` |

P1 may confirm the same route already selected by `baseDecision`. **APPLIED does not require the physical route to change.**

P1 functions primarily as a **control profile**. It is not evidence of personalization value.

---

### Profile P2 — Toll Tolerance

**Human-readable:** Accept up to 5 extra minutes to save at least AED 4.

**Normalized:**

```
type = TOLL_TOLERANCE
maxExtraMinutes = 5
minimumTollSavingAed = 4
```

**Evaluation (two routes):**

1. Identify the route with lower toll
2. Calculate toll saving relative to the other route
3. Calculate ETA penalty of the lower-toll route relative to the faster route

**Apply when:** `tollSavingAed >= 4` **AND** `extraMinutes <= 5`

Then: `APPLIED`; `preferenceSelectedRoute` = lower-toll route

**Otherwise:** `NOT_APPLIED`; `preferenceSelectedRoute = None`; `finalDecision = baseDecision`

**If both routes have the same toll:** `NOT_APPLIED`

Boundary comparisons are **inclusive**.

Unknown ETA or unknown toll for either route → `INSUFFICIENT_FOR_PREFERENCE`

Decision Compression time-for-toll tolerance is **not** used here.

---

### Profile P3 — Preferred Corridor

**Human-readable:** Prefer `corridorKey = E311` when it is no more than 3 minutes slower than the faster route.

**Normalized:**

```
type = PREFERRED_CORRIDOR
preferredCorridorKey = E311
maxExtraMinutes = 3
```

**Requirements:**

- Exactly one route must have `corridorKey = E311`
- ETA for both routes must be reliable

**Apply when:** E311 route's `extraMinutes <= 3` (relative to faster route)

Then: `preferenceSelectedRoute` = E311 route

**If neither or both routes have `corridorKey = E311`:** `INSUFFICIENT_FOR_PREFERENCE` — do not infer a preferred route

Matching uses **exact equality on `corridorKey` only**. `corridorLabel` must not participate.

---

### Profile P4 — Avoided Corridor

**Human-readable:** Avoid `corridorKey = E11` unless the non-E11 route is more than 8 minutes slower.

**Normalized:**

```
type = AVOIDED_CORRIDOR
avoidedCorridorKey = E11
maxAvoidancePenaltyMinutes = 8
```

**Requirements:**

- Exactly one route has `corridorKey = E11`
- Exactly one route has a different `corridorKey`
- ETA for both routes is reliable

**Apply when:** `nonE11ExtraMinutes <= 8` (non-E11 route's ETA penalty relative to E11 route)

Then: `preferenceSelectedRoute` = non-E11 route

**If non-E11 route is 9 or more minutes slower:** `NOT_APPLIED`; `finalDecision = baseDecision`

**If both or neither route has `corridorKey = E11`:** `INSUFFICIENT_FOR_PREFERENCE`

Matching uses **exact equality on `corridorKey` only**.

---

## Profile assignment (Dataset B)

| Profile | Cases | Focus |
|---------|-------|-------|
| P1 — Meaningful Fastest | 2 | Confirms meaningful fastest route; non-meaningful ETA gap |
| P2 — Toll Tolerance | 2 | Boundary apply (5 min / AED 4); boundary reject (6 min) |
| P3 — Preferred Corridor | 2 | E311 at exactly 3 min; E311 at 4 min |
| P4 — Avoided Corridor | 2 | Non-E11 at exactly 8 min; non-E11 at 9+ min |

No case combines multiple profile types.

---

## Candidate future user-owned data boundary

**Do not** use the phrase "allowed stored data."

### Candidate fields (synthetic contract only)

| Field | Purpose |
|-------|---------|
| `preferenceType` | P1–P4 normalized type |
| `maxExtraMinutes` | Toll tolerance or corridor slack |
| `minimumTollSavingAed` | Minimum saving for toll tolerance |
| `preferredCorridorKey` | e.g. E311 |
| `avoidedCorridorKey` | e.g. E11 |

> These fields define a synthetic candidate contract only.
>
> This document does not authorize persistence, database changes, telemetry, production collection, automatic learning, or implementation.
>
> Any future storage requires separate authorization and applicable privacy, security, and Terms review.

### Prohibited data

Google responses; route objects; polylines; coordinates; Google route IDs; ETA history; traffic intervals; route fingerprints; navigation history; automatic preference inference; provider payloads.

---

## Future experiment boundary (not executed)

**Experiment B — Explicit Route Preference** (future, separately authorized)

| Comparison | Content |
|------------|---------|
| Base | Decision Compression outcome on same synthetic facts |
| Treatment | Same facts with one explicit preference applied |

**Future research question:** Does the resulting decision and explanation correctly and understandably apply the participant's stated preference?

Do **not** use only: *Did the decision change?* — preference may correctly confirm the base choice.

Experiment B is not executed or authorized by this package.

---

## Anti-arbitrariness rule

Must not invent:

- Preference precedence beyond single-preference rule
- Hidden corridor ranking outside profile definitions
- Missing-value fallbacks not stated here
- Additional profiles or combined preferences

If a required rule is absent, mark the case **BLOCKED**.

---

## Related artifacts

| Artifact | Location |
|----------|----------|
| Decision Compression Contract | `docs/stage-a-decision-compression-contract-v0.1.md` |
| Synthetic cases and truth tables | `docs/stage-a-synthetic-cases-v0.1.md` |
