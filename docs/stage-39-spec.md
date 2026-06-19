# Stage 39 Spec — CALM Stress Blind Spot (Guarded)

**Status:** APPROVED (spec only)  
**Implementation:** HOLD  
**Depends on:** Stage 38.3 CLOSED, Stage 38.4 CLOSED  
**Baseline:** `docs/stage-38.3-baseline.md`  
**Anchor verification:** `docs/stage-38.4-replay-audit.md`

---

## Problem

In PM rush, CALM (`SmoothDriveScoring`) sometimes selects a route with **materially higher step-derived stress** when a lower-stress alternative exists and SESSION/`CALM_AUDIT_SUMMARY` report **`timePenaltyMin = 0`**.

Verified anchor cases (2026-06-19 PM):

| Timestamp | stressGapPct | timePenaltyMin | Note |
|-----------|--------------|----------------|------|
| 18:15:09 | 40% | 0 | Winner slower **and** more stressful vs best stress |
| 18:17:35 | 44% | 0 | Winner 18 min / 9 crit vs 16 min / 5 crit |

Root cause hypothesis: **`delayScore` dominates** when traffic delay is high; stress metrics from Directions steps are **orthogonal** to delay (AM vs PM bucket divergence in 38.3).

This is a **CALM scoring blind spot**, not a map or handoff issue.

---

## Question

> When should CALM evaluate whether the stress-optimal route should override the current SmoothDrive winner?

Exact selection policy: **TBD after offline replay** across 47 baseline sets.

---

## Evaluation guards (conceptual)

Consider override evaluation **only if all true**:

| Guard | Threshold |
|-------|-----------|
| Mode | CALM only |
| Time budget | `timePenaltyMin ≤ 5` |
| Stress gap | `maneuversSavedPct ≥ 20` (or `stressGapPct` equivalent) |
| Blind-spot class | `timePenaltyMin == 0` **or** winner not strictly faster in `duration_in_traffic` |

**Action when guards pass:**

Evaluate whether the stress-optimal route should override the current CALM winner.

**Exact selection policy TBD after offline replay** (must report: cases improved, false positives, AM regressions).

**Action when guards fail:** no change — current `SmoothDriveScoring` winner.

---

## Hard constraints

- Do **not** change FASTEST selection
- Do **not** change SAVE AED / NO_TOLLS selection
- Do **not** remove delay-first as CALM primary signal when winner is **materially faster**
- Do **not** apply when `timePenaltyMin > 5`
- Do **not** change Home UI, MEP, ComparativeEvidence, EquivalentTripHonesty in Stage 39
- Do **not** add user-facing copy in Stage 39 implementation
- Do **not** ship without unit tests on guard boundaries

---

## Out of scope

- Last-mile / gates / lanes
- New product modes
- Full SmoothDriveScoring rewrite
- Subjective driver feedback loop (future field)

---

## Acceptance criteria (future IMPLEMENTATION GO)

1. Offline replay on 47-set baseline: document **improved / false positive / AM regression** counts per policy option
2. Unit tests: anchor replay `18:15:09`, `18:17:35`
3. Unit tests: guard edges (19% vs 20%, 5 vs 6 min)
4. No regression: FASTEST / SAVE AED tests
5. Field: ≥5 post-implementation drives (≥2 PM rush) with subjective calm note

**Implementation GO requires:** replay sign-off + explicit GO (separate from this spec).

---

## Decision gate

| Item | Status |
|------|--------|
| Stage 38.3 baseline | CLOSED |
| Stage 38.4 anchor replay | CLOSED |
| Stage 39 Spec | **APPROVED** |
| Stage 39 Code | **HOLD** |
