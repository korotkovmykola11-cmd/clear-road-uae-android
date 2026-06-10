# Stage 35.7 — Smooth Delay Signal Gate

**Status:** Pending road test  
**Date:** 2026-06-02  
**Scope:** Validation gate only — no scoring, UI, or selection changes until verdict

---

## Context

Stage 35.1 `SmoothDriveScoring` uses **delayRatio** as its primary signal:

```
delaySeconds = max(0, duration_in_traffic − duration)
delayRatio   = delaySeconds / baseDurationSeconds
P_delayRatio = delayRatio × 120
```

Audit findings (Stages 35.0 / 35.6):

| Dataset | `duration_in_traffic > duration` | delayRatio active | Winner driver |
|---------|----------------------------------|-------------------|---------------|
| `docs/stage-35-0-audit/` (peak captures) | **12 / 12** routes | Yes (spread 3.7–10.5 pp) | Delay changes winner in **3 / 4** scenarios |
| `docs/stage-34-4-live/` | **0 / 15** routes | No (all zero) | Corridor / distance only |
| Device logs (off-peak / 34.4-like) | Often 0 | No | `WINNER_DRIVER likely corridor/distance tie-break` |

**Conclusion so far:** Not proven as a bug. Proven as **gate risk** — the main signal may be inactive on most live fetches.

Stage 35.7 decides whether Stage 35 scoring stays, gets a fallback signal, or is redesigned.

---

## Goal

Confirm in **real peak traffic** whether Google returns enough `duration_in_traffic > duration` data for delayRatio to drive SMOOTH winners.

---

## Test windows (GST, weekday)

| Window | Time |
|--------|------|
| Morning peak | **07:30 – 09:30** |
| Evening peak | **17:00 – 19:30** |

Run each route **once per window** (8 captures minimum). Extra runs optional.

Record actual fetch timestamp in the results sheet.

---

## Routes (mandatory O/D set)

| ID | Origin | Destination | Stage 35.0 reference |
|----|--------|-------------|----------------------|
| R1 | DIFC | Dubai Marina | `difc-marina.json` |
| R2 | Ajman | DIFC | `ajman-difc.json` |
| R3 | Sharjah | Downtown Dubai | `sharjah-downtown.json` |
| R4 | JVC | Abu Dhabi | `jvc-abudhabi.json` |

Use the same place picks as Stage 35.0 audit when possible (repeatable comparison).

---

## Build & setup

1. Debug build with current branch (`SmoothDriveScoring` wired via `RouteRecommendationSelection`).
2. `ArchitectureValidation.RECOMMENDATION_ONLY_HOME = true`.
3. Android Studio Logcat filters (either tag):
   - `SmoothDriveScoring`
   - `DirectionsAudit`
4. Optional combined filter: `tag:SmoothDriveScoring | tag:DirectionsAudit`

No code changes required for the gate run — logging is already in place from Stages 35.3 / 35.6.

---

## Logcat — what to collect per fetch

After Directions load, copy the block from `=== SMOOTH AUDIT ===` through `WINNER route=…`.

### DirectionsAudit (raw Google fields)

```
ROUTE_METRICS routeIndex=… duration=… baseDuration=… durationInTraffic=… trafficDelay=…
ROUTE_PAIR_COMPARE … (optional, Route 2 vs 3)
```

### SmoothDriveScoring (formula + verdict)

| Line | Field | Meaning |
|------|-------|---------|
| `DATA … trafficSource=` | `GOOGLE_duration_in_traffic` or `FALLBACK_durationSeconds` | Data source for traffic seconds |
| `FORMULA delaySeconds=…` | delaySeconds | Primary signal input |
| `FORMULA delayRatio=…` | delayRatio | Normalized delay |
| `FORMULA P_delayRatio=…` | score component | Weight × delayRatio |
| `SCORE total=…` | SMOOTH score | Full breakdown sum |
| `WINNER route=…` | SMOOTH winner index | 0-based route index |
| `DELAY_SIGNAL_INACTIVE reason=…` | Warning | Missing field or traffic ≤ base |
| `WINNER_DRIVER likely corridor…` | Warning | Delay components all zero |

---

## Per-capture scoring sheet

Fill one row per fetch in `docs/stage-35-7-gate/results.csv` (template included).

| Column | Value |
|--------|-------|
| `run_id` | e.g. `R1-morning-2026-06-03` |
| `route_id` | R1–R4 |
| `window` | `morning` / `evening` |
| `timestamp_gst` | HH:MM |
| `alternatives_count` | Usually 3 |
| `delay_gt_zero_count` | Count routes where `delaySeconds > 0` |
| `delay_active_pct` | `delay_gt_zero_count / alternatives_count × 100` |
| `traffic_source` | `GOOGLE` / `FALLBACK` / `MIXED` |
| `max_delay_ratio` | Max delayRatio across routes |
| `delay_spread` | max − min delayRatio |
| `smooth_winner` | 0-based index |
| `winner_driver` | `delay` / `corridor` / `distance` / `time` / `mixed` |
| `delay_signal_inactive` | `yes` / `no` |
| `notes` | Free text |

### Winner driver (manual classification)

| Label | When |
|-------|------|
| **delay** | `P_delayRatio` or `P_delayAbs` differs across routes and lowest total aligns with lowest delayRatio |
| **corridor** | All delay components 0 (or tied); winner has best `P_corridor` |
| **distance** | Delay tied; winner decided by `P_dist` |
| **time** | Winner outside time budget; `P_time` dominates |
| **mixed** | Multiple components within 0.01 of winner swap |

---

## Gate criteria

### PASS

All of:

- **≥ 50% of alternatives** across the gate campaign have `delaySeconds > 0`  
  (campaign total: sum of `delay_gt_zero_count` / sum of `alternatives_count`)
- **≥ 3 of 4 routes** have at least one fetch with `delay_active_pct ≥ 50%`
- `trafficSource=GOOGLE_duration_in_traffic` on all PASS captures (no FALLBACK-only passes)
- At least **2 routes** show `winner_driver = delay` in at least one peak fetch

**Action:** Keep Stage 35.1 scoring. Proceed to explanation / road-test polish.

### FAIL

Any of:

- Campaign-wide **< 30%** alternatives with `delaySeconds > 0`
- **≥ 6 of 8** mandatory fetches log `DELAY_SIGNAL_INACTIVE`
- **≥ 6 of 8** fetches show `WINNER_DRIVER likely corridor/distance tie-break`
- Any mandatory route never reaches `delay_active_pct ≥ 33%` in either peak window

**Action:** Revisit Stage 35.1 primary signal (not copy). Do not treat corridor-only SMOOTH as validated.

### PARTIAL

Between PASS and FAIL:

- Campaign delay-active rate **30–49%**, or
- Delay signal active but `winner_driver = corridor` on most fetches, or
- Only one peak window shows delay (morning OR evening, not both)

**Action:** Keep Stage 35.1 scoring temporarily. Add **corridor-dominance guard** before wider release (separate stage — e.g. cap `P_corridor` influence when `delayRatio` spread < ε, or require minimum delay spread for SMOOTH).

---

## Verdict template

```
STAGE 35.7 VERDICT: PASS | PARTIAL | FAIL

Campaign summary:
- Fetches: N / 8
- Alternatives with delay > 0: X / Y (Z%)
- Routes with ≥50% delay-active fetch: N / 4
- Fetches with DELAY_SIGNAL_INACTIVE: N
- Fetches with corridor-dominance warning: N
- Fetches where delay changed winner (vs corridor-only): N

Per route:
  R1 DIFC→Marina:     delay_active_pct=…  winner_driver=…
  R2 Ajman→DIFC:      delay_active_pct=…  winner_driver=…
  R3 Sharjah→Downtown: delay_active_pct=…  winner_driver=…
  R4 JVC→Abu Dhabi:   delay_active_pct=…  winner_driver=…

Decision:
  [ ] Keep Stage 35.1 scoring
  [ ] Add corridor-dominance guard (PARTIAL path)
  [ ] Redesign primary signal (FAIL path)

Signed off: __________  Date: __________
```

---

## Offline helper (saved JSON)

If you save raw Directions JSON from a peak fetch into `docs/stage-35-7-gate/captures/`:

```powershell
cd docs/stage-35-7-gate
./evaluate-gate.ps1 -CapturePath captures/R1-morning.json
```

Script reports per-route `delaySeconds`, `delayRatio`, and whether the capture would contribute to PASS.

Automated regression (fixtures):

```bash
./gradlew :app:testDebugUnitTest --tests "com.clearroad.app.domain.SmoothDriveDelaySignalAuditTest"
```

Expected: Stage 35.0 fixtures PASS delay checks; Stage 34.4 fixtures show zero delay (baseline for FAIL-like data).

---

## Reference

| Stage | Topic |
|-------|-------|
| 34.4 | Live spot-check — delay inactive |
| 34.5 | Implementation freeze |
| 35.0 | Peak audit fixtures — delay active |
| 35.1 | `SmoothDriveScoring` formula |
| 35.3 | Wired to Home CALM selection |
| 35.6 | Delay diagnostics + audit tests |
| **35.7** | **This gate — peak road test** |

---

## What does not change during 35.7

- Scoring weights
- Route selection logic
- UI copy / layout
- No new API calls

Gate is **observe and record only**.
