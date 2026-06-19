# Stage 39 — Implementation Plan (Policy B)

**Status:** APPROVED (plan only; code **HOLD**)  
**Policy:** B (Stress Tie-Break) only  
**Implementation code:** **HOLD** until user says **IMPLEMENTATION GO**

---

## 1. Current status

| Item | Status |
|------|--------|
| Stage 38.3 field baseline | **CLOSED** — `docs/stage-38.3-baseline.md` |
| Stage 38.4 anchor audit | **CLOSED** — `docs/stage-38.4-replay-audit.md` |
| Stage 39 Spec | **APPROVED** — `docs/stage-39-spec.md` |
| Stage 39A offline replay | **CLOSED** — `docs/stage-39a-replay-report.md` |
| Policy B | **Preferred candidate** |
| Policy C | **Conservative fallback** (not in this plan) |
| Stage 39 implementation | **HOLD** |
| Git push | **HOLD** |
| Working tree before this doc | **Clean** (Stage 39A committed locally, +4 ahead of origin) |

**Explicit gate:** No production code, scoring changes, or new tests may be merged for Stage 39 until the user issues **IMPLEMENTATION GO** after reviewing this plan.

---

## 2. Why Policy B is preferred

Offline replay on 47 frozen CALM route sets (`docs/stage-39a-replay-report.md`) shows Policy B (Stress Tie-Break) as the best trade-off on current data:

| Metric | Current (A) | Policy B | Policy C |
|--------|-------------|----------|----------|
| Meaningful opportunities captured | 0 / 14 | **6 / 14** | 3 / 14 |
| Dual blind spots fixed | — | **5 / 12** | 3 / 12 |
| PM blind spots fixed | — | **3** | 2 |
| AM regressions | — | **0** | 0 |
| False positives | — | **0** | 0 |
| CALM match (lowest stress) % | 40% | **53%** | 46% |

**Why not Policy C alone:** C captures only half the meaningful wins (3/14 vs 6/14) and fixes fewer dual blind spots (3/12 vs 5/12). C remains documented as a **fallback** if field validation after B shows too many unwanted switches — not the primary implementation target.

**Problem being addressed:** Current CALM sometimes selects a route that is **slower in traffic and more stressful** than an alternative in the same route set. Replay found **12 / 47** such dual-defect sets under observed field CALM, including verified anchors `18:15:09` and `18:17:35`.

Policy B adds a **guarded post-step** after `SmoothDriveScoring`: override to lowest critical-maneuver route only when blind-spot guards pass. It does **not** replace delay-first CALM when the SmoothDrive winner is materially faster.

---

## 3. Exact implementation scope

### 3.1 Behaviour (Policy B)

After `SmoothDriveScoring.pickWinnerIndex` returns the CALM winner, evaluate a **Stress Tie-Break override**:

1. Compute stress metrics for all routes in the set (same source as `DriverStressAudit` today: critical maneuver count from Directions steps).
2. Identify `bestStressRouteIndex` = route with minimum `criticalManeuversCount` (tie → lower index).
3. If `calmWinnerIndex == bestStressRouteIndex` → **no override** (already aligned).
4. Otherwise compute relative to the SmoothDrive winner:
   - `stressGapPct` = `(winner.critical − bestStress.critical) / winner.critical × 100` (0 if winner.critical ≤ 0)
   - `timePenaltyMin` = `max(0, bestStress.durationInTraffic − winner.durationInTraffic)` in minutes
5. **Override** to `bestStressRouteIndex` **only if all guards pass** (see §4).
6. If guards fail → keep SmoothDrive winner unchanged.

### 3.2 Code touchpoints (future — not started)

| Area | Change |
|------|--------|
| `RouteRecommendationSelection.pickCalmRouteIndex` | Call SmoothDrive winner, then apply Policy B override |
| New domain helper (proposed) | e.g. `CalmStressTieBreak` in `domain/` — pure, unit-testable; mirrors `CalmReplayPolicyEngine` Policy B logic |
| `DriverStressAudit` / step metrics | **Reuse** existing critical-count extraction; do not duplicate maneuver parsing |
| `SmoothDriveScoring` | **No change** to weights, corridor logic, or winner formula |
| FASTEST / SAVE AED | **No change** |
| Home UI, MEP, ComparativeEvidence, EquivalentTripHonesty | **No change** |
| User-facing copy / badges / “Why this route” | **No change** in Stage 39 |
| Architecture flags | Optional: gate behind explicit flag default **off** until IMPLEMENTATION GO + field sign-off (decision at implementation time) |

### 3.3 Wiring rule

Override applies **only** when:

- `PreferenceMode.CALM`
- Route set has valid SmoothDrive inputs for all routes (same precondition as today)
- Stress metrics available for all routes (same precondition as audit logging)

If stress data is incomplete → **fallback to SmoothDrive winner only** (no override).

### 3.4 Audit logging (minimal)

When override fires, log one debug line (existing audit channel or `SmoothDriveScoring` tag):

- `calmWinnerIndex`, `overrideIndex`, `stressGapPct`, `timePenaltyMin`, `policy=STRESS_TIE_BREAK_B`

Purpose: post-implementation field comparison only. Not user-facing.

---

## 4. Guard conditions (Policy B — exact)

All must be **true** to override:

| # | Guard | Rule |
|---|-------|------|
| G1 | Mode | CALM only |
| G2 | Time budget | `timePenaltyMin ≤ 5` |
| G3 | Stress gap | `stressGapPct ≥ 20` |
| G4 | Blind-spot class | `timePenaltyMin == 0` **OR** SmoothDrive winner is **not** strictly faster in `duration_in_traffic` than best-stress route |

**Override action:** select route at `bestStressRouteIndex` (lowest critical maneuver count).

**If any guard fails:** keep SmoothDrive winner.

**Formulas** (must match `DriverStressAudit.kt` and Stage 39A replay):

```
bestStressRouteIndex = argmin(critical_maneuvers_count), tie → lower index

timePenaltyMin = max(0, bestStress.durationInTrafficMin − winner.durationInTrafficMin)

stressGapPct = round((winner.critical − bestStress.critical) / winner.critical × 100)
               (0 if winner.critical ≤ 0)

winnerStrictlyFasterInTraffic =
  winner.durationInTraffic < bestStress.durationInTraffic
```

**G4 in plain language:** Override is allowed when there is no SESSION-measured time cost (`timePenaltyMin == 0`), **or** when CALM did not win on traffic time anyway (winner not strictly faster than the stress-optimal route). This preserves delay-first behaviour when SmoothDrive legitimately picks a faster route.

---

## 5. Out of scope

- Policy C implementation (fallback only; separate stage if needed)
- Full `SmoothDriveScoring` rewrite or weight tuning
- FASTEST, SAVE AED / NO_TOLLS selection changes
- UI, MEP, narrative, confidence, or recommendation copy
- Subjective driver feedback capture (future field stage)
- Last-mile / gates / lanes
- New product modes
- Push to origin as part of implementation (separate decision)
- Changing Stage 38.3 / 39A frozen baselines or replay results
- Production deploy / Play release

---

## 6. Tests required before implementation merge

These tests must exist and pass **before** any Stage 39 prod code is merged. They may be written after IMPLEMENTATION GO but **must pass in the same PR** as the behaviour change.

### 6.1 Policy B unit tests (new)

Mirror guards in `CalmReplayPolicyEngine` / proposed `CalmStressTieBreak`:

| Test | Intent |
|------|--------|
| Anchor `18:15:09` | Override to best stress; gap 40%, timePenalty 0 |
| Anchor `18:17:35` | Override to best stress; gap 44%, timePenalty 0 |
| Guard edge: gap 19% vs 20% | No override at 19%; override at 20% (with other guards met) |
| Guard edge: timePenalty 5 vs 6 min | Override at 5; no override at 6 |
| G4: winner strictly faster | No override when winner faster in traffic and timePenalty > 0 |
| G4: timePenalty == 0 | Override allowed even if durations differ (anchor class) |
| Already lowest stress | No override when SmoothDrive winner == bestStress |
| Tie on critical count | Lower index wins |
| Incomplete route data | No override; SmoothDrive winner preserved |

### 6.2 Regression tests (existing — must stay green)

| Suite | Requirement |
|-------|-------------|
| `SmoothDriveScoringTest` | No changes to scoring outputs |
| `RouteRecommendationSelectionTest` | FASTEST / SAVE AED unchanged; CALM tests updated only where override expected |
| `DriverStressAuditTest` | Metric formulas unchanged |
| Stage 39A replay | `./gradlew :app:testDebugUnitTest --tests "com.clearroad.app.replay.Stage39AReplayTest"` — Policy B column unchanged |

### 6.3 Full gate

```bash
./gradlew test
```

All unit tests green before merge.

---

## 7. Rollback plan

### 7.1 Code rollback

- Revert the commit(s) that add `CalmStressTieBreak` / `pickCalmRouteIndex` override wiring.
- CALM selection returns to `SmoothDriveScoring.pickWinnerIndex` only.
- No database or migration rollback (selection is stateless per request).

### 7.2 Flag rollback (if feature flag used)

- Set flag default to **off** → instant revert without redeploy of logic removal.
- Preferred for first field trial if implementation adds a flag.

### 7.3 Validation after rollback

- `./gradlew test` green
- Optional: one manual CALM fetch — winner matches pre-Stage-39 behaviour on a known route set

### 7.4 When to rollback

- Field drives show systematic **wrong** overrides (user-perceived, not just metric drift)
- AM regressions appear in post-implementation audit that replay did not predict
- False-positive rate unacceptable on expanded corpus
- Any production crash or selection failure on incomplete data

---

## 8. GO / NO-GO criteria

### GO (user says **IMPLEMENTATION GO**)

All required:

| # | Criterion |
|---|-----------|
| 1 | This implementation plan **reviewed and accepted** |
| 2 | Stage 39A replay **CLOSED** with Policy B preferred — **done** |
| 3 | User explicit **IMPLEMENTATION GO** (separate from spec approval) |
| 4 | Test plan §6 agreed; tests written in same PR as code |
| 5 | Rollback approach agreed (revert or flag) |

### NO-GO (defer or reject implementation)

Any of:

| # | Criterion |
|---|-----------|
| 1 | User has not said **IMPLEMENTATION GO** |
| 2 | Expanded replay or new field data shows Policy B **AM regressions** or **false positives** on a larger corpus |
| 3 | Implementation scope creeps beyond §3 (UI, other modes, scoring rewrite) |
| 4 | Tests in §6 cannot be made to pass without breaking spec guards |
| 5 | Policy C is chosen instead of B without updating this plan |

### Post-implementation field gate (before “ship” narrative)

Not blocking IMPLEMENTATION GO, but blocking product **ship** claim:

- ≥5 post-implementation drives, ≥2 PM rush
- Subjective calm note per drive
- Compare `DRIVER_STRESS_AUDIT` to 38.3 baseline buckets

---

## 9. Stage 39 code remains HOLD

**Stage 39 production code is HOLD until the user explicitly says IMPLEMENTATION GO.**

This document is planning only. It does not authorize:

- Editing `RouteRecommendationSelection`, `SmoothDriveScoring`, or Home selection paths
- Adding production override logic
- Committing implementation code
- Pushing to origin

**Allowed now:** Review this plan, adjust guards or scope, then issue IMPLEMENTATION GO or request changes.

---

## References

| Doc | Role |
|-----|------|
| `docs/stage-38.3-baseline.md` | 47-set frozen baseline |
| `docs/stage-38.4-replay-audit.md` | Anchor verification |
| `docs/stage-39-spec.md` | Problem statement and hard constraints |
| `docs/stage-39a-replay-report.md` | Policy A/B/C results |
| `app/src/test/java/com/clearroad/app/replay/CalmReplayPolicy.kt` | Policy B reference implementation (test-only) |
| `app/src/main/java/com/clearroad/app/DriverStressAudit.kt` | Metric formulas (source of truth) |

---

## Suggested commit message (after review)

```
Stage 39: Policy B implementation plan (HOLD)
```

Do **not** commit until user approves this document.
