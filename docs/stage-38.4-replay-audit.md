# Stage 38.4 — Replay Audit (Anchor Cases)

**Status:** CLOSED  
**Date:** 2026-06-19  
**Scope:** Read-only verification. No code changes.

---

## Goal

Confirm that anchor PM-rush cases `18:15:09` and `18:17:35` are **real**, not artifacts of deduplication, logging, or metric math.

Verify from raw `ROUTE` + `SESSION` + `CALM_AUDIT_SUMMARY` lines in field logcat:

- `timePenaltyMin = 0`
- stress gap ≥ 40%
- CALM winner ≠ lowest-stress route

---

## Formulas (source of truth)

From `DriverStressAudit.kt`:

```
lowestCriticalManeuverIndex = argmin(critical_maneuvers_count), tie → lower index

timeLostMin / timePenaltyMin =
  max(0, bestStress.durationInTrafficMin − winner.durationInTrafficMin)

maneuversSavedPct / stressGapPct =
  round((winner.critical − bestStress.critical) / winner.critical × 100)
  (0 if winner.critical ≤ 0)

calmPickedLowestStress = (calmWinnerIndex == bestStressRouteIndex)
```

**Important:** `timePenaltyMin = 0` means the stress-optimal route is **not slower in traffic** than the CALM winner. It does **not** mean equal wall-clock tradeoff in all dimensions — if the stress route is **faster**, the metric still reports 0.

---

## Case A — 2026-06-19 18:15:09

### Raw ROUTE data (logcat)

| Index | duration_in_traffic_min | traffic_delay_min | critical | steps | CALM winner |
|-------|-------------------------|-------------------|----------|-------|-------------|
| 0 | 71 | 22 | 15 | 28 | |
| 1 | 80 | 31 | **12** | 26 | |
| 2 | 82 | 19 | 20 | 41 | **yes** |

### Manual replay

| Check | Expected (log) | Recomputed | Match |
|-------|----------------|------------|-------|
| `bestStressRoute` | 1 | min critical → index **1** (12) | ✅ |
| `calmWinnerIndex` | 2 | header `currentWinnerIndex=2` | ✅ |
| `stressGapPct` | 40 | (20−12)/20×100 = **40** | ✅ |
| `timePenaltyMin` | 0 | max(0, 80−82) = **0** | ✅ |
| `calmPickedLowestStress` | false | 2 ≠ 1 | ✅ |
| `SESSION saved%` | 40 | **40** | ✅ |
| `SESSION timeLostMin` | 0 | **0** | ✅ |

### CALM_SCORE_BREAKDOWN (why winner = 2)

| Route | delayScore | finalScore |
|-------|------------|------------|
| 0 | 85.14 | 81.22 |
| 1 | **118.19** | 114.19 |
| 2 | 63.17 | **60.65** ← pick |

Route 1 (lowest stress) has highest `traffic_delay_min` (31) → highest `delayScore` → excluded despite **fewest critical maneuvers**.

### Verdict Case A

**REAL — not an artifact.**

Stronger than “0 min penalty” headline:

- CALM winner (82 min, 20 critical) is **2 min slower** than stress route (80 min, 12 critical).
- CALM loses on **both time and stress** vs route 1.

Dedup: unique fingerprint `(71/55.27, 80/51.42, 82/78.61)` — not a mode-switch duplicate.

---

## Case B — 2026-06-19 18:17:35

### Raw ROUTE data (2-route set in log; 3 routes in session)

Log shows **3 routes** at 18:17:35 block — user paste included 2-route CALM at 18:17:35:

| Index | duration_in_traffic_min | traffic_delay_min | critical | CALM winner |
|-------|-------------------------|-------------------|----------|-------------|
| 0 | 16 | 1 | **5** | |
| 1 | 18 | 0 | 9 | **yes** |

*(Third route not in this timestamp block — 2-route query.)*

### Manual replay

| Check | Expected (log) | Recomputed | Match |
|-------|----------------|------------|-------|
| `bestStressRoute` | 0 | min critical → index **0** (5) | ✅ |
| `calmWinnerIndex` | 1 | header `currentWinnerIndex=1` | ✅ |
| `stressGapPct` | 44 | (9−5)/9×100 = **44.4 → 44** | ✅ |
| `timePenaltyMin` | 0 | max(0, 16−18) = **0** | ✅ |
| `calmPickedLowestStress` | false | 1 ≠ 0 | ✅ |
| `SESSION saved%` | 44 | **44** | ✅ |

### CALM_SCORE_BREAKDOWN

| Route | delayScore | finalScore |
|-------|------------|------------|
| 0 | 0.62 | −3.38 |
| 1 | 0.00 | **−3.95** ← pick |

Scores are **tight** (−3.38 vs −3.95). Winner is **2 min slower** (18 vs 16) with **80% more critical maneuvers** (9 vs 5).

### Verdict Case B

**REAL — not an artifact.**

Blind spot confirmed: CALM picks higher finalScore route that is **slower and more stressful** on raw ROUTE metrics.

---

## Artifact checks (both cases)

| Risk | Result |
|------|--------|
| Dedup collision | ❌ Not duplicates of other sets in 47-set corpus |
| SESSION / CALM summary mismatch | ❌ SESSION and CALM_AUDIT_SUMMARY agree on both |
| `stressRankByCriticalCount` inconsistent | ❌ Ranks match critical ordering in ROUTE lines |
| `is_current_winner` ≠ CALM index | ❌ Consistent on both |
| Zero critical → div-by-zero | ❌ N/A (winner critical > 0) |

**Not re-parsed:** raw Directions JSON step lists. Critical counts are taken as logged from `extractStepRecordsFromRouteJson`. Spot-check: keyword counts in ROUTE lines are plausible for stated critical totals.

---

## Stage 38.4 conclusion

| Question | Answer |
|----------|--------|
| Are 18:15:09 and 18:17:35 real? | **Yes** |
| Is timePenalty = 0 real per code definition? | **Yes** |
| Is stress gap 40%+ real? | **Yes (40% and 44%)** |
| Is motivation for Stage 39 spec still valid? | **Yes** |

**Caveat for Stage 39 Spec:** headline “0 min penalty” understates Case A and B — stress-optimal routes are **faster in traffic** in both anchors. Spec should use **“no SESSION time penalty”** not **“equal duration.”**

---

## Next gate (not this stage)

- Offline replay across all 47 sets with candidate policy options
- Stage 39 implementation **HOLD** until replay + explicit GO
