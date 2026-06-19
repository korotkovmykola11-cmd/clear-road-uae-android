# Stage 39A — Offline Replay Report

**Status:** CLOSED  
**Date:** 2026-06-19  
**Scope:** Test-only replay framework. **No production code changes.**

---

## Goal

Replay 47 frozen Stage 38.3 CALM route sets through three offline policies and compare outcomes before any Stage 39 scoring change.

---

## Policies

| Policy | Name | Definition |
|--------|------|------------|
| **A** | Current CALM | Frozen field `currentWinnerIndex` from logcat (observed CALM selection at capture time) |
| **B** | Stress Tie-Break | If field CALM missed lowest stress with `stressGap ≥ 20%`, `timePenalty ≤ 5 min`, and blind-spot class (`timePenalty == 0` OR winner not strictly faster in traffic) → pick lowest critical count |
| **C** | Strict Stress Tie-Break | Same override, but only when `timePenalty == 0` and `stressGap ≥ 35%` |

Policies B/C apply **on top of Policy A baseline**, not a re-computed `SmoothDriveScoring` without corridor text.

---

## Dataset

Same 47 deduplicated sets as `docs/stage-38.3-baseline.md`:

| Bucket | Sets |
|--------|------|
| 2026-06-18 PM | 14 |
| 2026-06-19 AM | 17 |
| 2026-06-19 PM | 16 |
| **Total** | **47** |

Implementation: `app/src/test/java/com/clearroad/app/replay/`  
Frozen routes + winners: `Stage39AFieldBaseline.kt`  
Run: `./gradlew :app:testDebugUnitTest --tests "com.clearroad.app.replay.Stage39AReplayTest"`

---

## Results

| Metric | Current (A) | Policy B | Policy C |
|--------|-------------|----------|----------|
| PM blind spots fixed | — | **3** | **2** |
| AM regressions | — | **0** | **0** |
| Meaningful opportunities captured | **0 / 14** | **6 / 14** | **3 / 14** |
| False positives | — | **0** | **0** |
| Dual blind spots (slower + stressier) under A | **12** | — | — |
| Dual blind spots fixed | — | **5** | **3** |
| CALM match (lowest stress) % | **40%** | **53%** | **46%** |

**Denominator note:** “Meaningful opportunity” = field CALM winner had `stressGap ≥ 20%` and `timePenalty ≤ 5 min` vs lowest-stress route in the same set (14 sets). Policy A captures **0 by definition** on those sets — that is the blind spot.

---

## Key findings

### 1. Dual blind spot is not a single anchor case

Under Policy A, **12 / 47 sets (26%)** have a CALM winner that is **both slower in traffic and more stressful** (higher critical maneuver count) than the lowest-stress alternative.

This includes verified anchors:

- `2026-06-19T18:15:09` — gap 40%, timePenalty 0; winner route 2 (82 min / 20 crit) vs best stress route 1 (80 min / 12 crit)
- `2026-06-19T18:17:35` — gap 44%, timePenalty 0; winner route 1 (18 min / 9 crit) vs best stress route 0 (16 min / 5 crit)

**This is a CALM scoring blind spot, not a one-off log anomaly.**

### 2. Policy B improves capture without AM regressions

- Meaningful opportunities: **0 → 6 / 14** (43% of missed opportunities recovered)
- CALM–stress alignment: **40% → 53%**
- Dual blind spots fixed: **5 / 12**
- **Zero** AM regressions and **zero** false positives in this corpus

### 3. Policy C is safer but narrower

- Meaningful opportunities: **3 / 14**
- Dual blind spots fixed: **3 / 12** (includes both anchors)
- PM blind spots fixed: **2 / 16** PM sets
- Still **zero** AM regressions and false positives

---

## Interpretation

| Question | Answer |
|----------|--------|
| Is the blind spot real beyond anecdotes? | **Yes — 12 dual-defect sets** |
| Does a guarded tie-break help? | **Yes — B fixes 6/14 meaningful misses, 0 regressions** |
| Is strict policy enough? | **Partially — C fixes anchors + 3/14 meaningful, fewer total wins** |
| Ready for Stage 39 implementation GO? | **Data supports SPEC; policy choice still TBD** |

Replay does **not** auto-approve prod changes. It narrows the decision:

- **Policy B** maximizes recovery on this corpus with no measured AM cost here
- **Policy C** is a conservative floor (strong gaps, zero time penalty only)

---

## Limitations

1. **Policy A** replays frozen field winners, not live `SmoothDriveScoring` recompute (corridor text and base-duration inputs not in frozen ROUTE lines).
2. **47 sets** — Dubai rush-hour sample; not product-wide proof.
3. **Stress proxy** — critical maneuver count from Directions steps; no subjective drive data in replay.
4. **False positive definition** — policy changed winner without valid guard pass; real-world user-perceived “wrong switch” not modeled.

---

## Gate status (unchanged)

| Item | Status |
|------|--------|
| Stage 38.3 baseline | CLOSED |
| Stage 38.4 anchor audit | CLOSED |
| Stage 39 Spec | APPROVED |
| **Stage 39A replay** | **CLOSED** |
| Stage 39 implementation | **HOLD** — await explicit GO + policy pick (B vs C vs hybrid) |

---

## Next step (human decision)

1. Choose candidate policy (B, C, or gated hybrid) based on table above  
2. If GO → Stage 39 implementation with unit tests replaying anchors + aggregate regression guard  
3. Push branch only when ready (docs + replay + optional implementation)
