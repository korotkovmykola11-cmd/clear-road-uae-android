# Stage C pre-flip baseline — corridor / geometry semantics

Reference for **pre-flip gate** before `ArchitectureValidation.USE_ROUTES_V2_FETCH = true`.

**Match is NOT index equality.** Compare V2 winners to Legacy using **geometry fingerprint** and **corridor**, then classify each mode:

| Result | Meaning |
|--------|---------|
| **SAME_CORRIDOR** | `geom_fp` identical, **or** shared polyline length ≥ **85%** of shorter route, **or** same primary corridor + shared ≥ **70%** |
| **EXPLAINABLE_DIFFERENCE** | Different corridor/index but documented reason (toll trade-off, CALM stress tie-break, ≤5 min FASTEST duration delta, etc.) |
| **UNEXPLAINED_DIFFERENCE** | Different physical path with no acceptable explanation → **blocks Stage C flip** |

Implementation helpers (verification): `StageCBaselineCapture.kt`, thresholds aligned with `GeometrySimilarity`.

---

## Capture metadata

| Field | Value |
|-------|-------|
| **API** | Google Classic Directions (Legacy prod path) |
| **Engine** | `RouteRecommendationSelection` + `CorridorClassifier` + `RoutePolylineGeometry.stablePolylineHash` |
| **Geom fingerprint** | First **8 hex chars** of SHA-256 normalized polyline |
| **Corridor** | Primary stable key (e.g. `E11`, `E311`) from route summary + step text |
| **Captured** | 2026-08-02 ~21:10 UTC+4 |
| **Re-capture** | Within **7 days** of pre-flip; re-run export test below |

**Legacy baseline refresh:**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.clearroad.app.benchmark.StageCPreDecisionResearchTest.research_exportLegacyStageCBaseline_corridorGeometry"
```

Grep `app/build/test-results/.../TEST-*.xml` → `STAGE C LEGACY BASELINE`.

**Pre-flip V2 pass:** same 15 O-D, V2 fetch ON → fill `V2_*` and `*_result` columns.

**Automated alternative (recommended):** one Gradle test run performs all 45 comparisons and writes a report — no UI/emulator required for gate scoring.

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.clearroad.app.benchmark.StageCPreFlipGateTest.live_preFlipGate_dualFetch_report45Comparisons"
```

Output: console + `app/build/reports/stage-c-preflip-gate-report.txt`

Billing ≈ **$0.33/run** (15× V2 computeRoutes + up to 10× Legacy Directions; 5 frozen Legacy cases use fixtures).

**Manual only after automated pass:** visual handoff on `VISUAL_REVIEW` cases (CALM non-default + spot-check list in checklist).

---

## Legacy baseline summary

| Mode | Index non-default | Notes |
|------|------------------:|-------|
| **FASTEST** | **0/15** (0%) | All winners `idx=0` on this capture; parity vs V2 is **corridor/geometry**, not index |
| **NO_TOLLS** | n/a (index not gated) | **15/15 semantically correct** (`min_toll_zero` on every case) |
| **CALM** | **5/15 (33%)** | difc-marina, sharjah-downtown, ajman-difc, downtown-ajman, marina-deira |

CALM sanity range for V2 pre-flip: **2–6/15** non-default (baseline center **5/15**).

---

## Per-case baseline (Legacy filled · V2 empty for pre-flip)

### 1 — stage34-route1 · DIFC → Dubai Marina · 3 alts · frozen

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | c0c966dc | 0 | |
| **Legacy S** | 0 | E11 | c0c966dc | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | c0c966dc | 0 | |
| V2 F | | | | | |
| V2 S | | | | | |
| V2 C | | | | | |
| **F_result** | — | | | | |
| **S_result** | — | | | | |
| **C_result** | — | | | | |

### 2 — stage34-route2 · Dubai Marina → Airport T3 · 3 alts · frozen

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | f57cd3e5 | 0 | |
| **Legacy S** | 0 | E11 | f57cd3e5 | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | f57cd3e5 | 0 | |
| V2 F / S / C | | | | | |
| **F/S/C_result** | — | | | | |

### 3 — stage34-route3 · Sharjah → Downtown · 3 alts · frozen

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | b83eee3d | 0 | |
| **Legacy S** | 0 | E11 | b83eee3d | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | b83eee3d | 0 | |
| V2 F / S / C | | | | | |
| **F/S/C_result** | — | | | | |

### 4 — stage34-route4 · Ajman → DIFC · 3 alts · frozen

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 793dc2c6 | 0 | |
| **Legacy S** | 0 | E11 | 793dc2c6 | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | 793dc2c6 | 0 | |
| V2 F / S / C | | | | | |
| **F/S/C_result** | — | | | | |

### 5 — stage34-route5 · JVC → Abu Dhabi · 3 alts · frozen

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | f9da705b | 0 | |
| **Legacy S** | 0 | E11 | f9da705b | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | f9da705b | 0 | |
| V2 F / S / C | | | | | |
| **F/S/C_result** | — | | | | |

### 6 — live-difc-marina · DIFC → Dubai Marina · 3 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | c0c966dc | 0 | |
| **Legacy S** | 0 | E11 | c0c966dc | 0 | min_toll_zero |
| **Legacy C** | **1** | E11 | **e5ed1077** | 0 | same corridor, different geometry vs F |
| V2 F / S / C | | | | | |
| **F/S/C_result** | — | | | | |

### 7 — live-marina-airport-t3 · 3 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 01dcd179 | 0 | |
| **Legacy S** | 0 | E11 | 01dcd179 | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | 01dcd179 | 0 | |
| V2 F / S / C | | | | | |

### 8 — live-sharjah-downtown · 3 alts · live · **CALM corridor fork**

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | a714d959 | 0 | |
| **Legacy S** | 0 | E11 | a714d959 | 0 | min_toll_zero |
| **Legacy C** | **2** | **E311** | 85603da6 | 0 | CALM picks E311 alt |
| V2 F / S / C | | | | | |

### 9 — live-ajman-difc · 3 alts · live · **CALM corridor fork**

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 793dc2c6 | 0 | |
| **Legacy S** | 0 | E11 | 793dc2c6 | 0 | min_toll_zero |
| **Legacy C** | **2** | **E311** | 56f0d1dd | 0 | CALM picks E311 alt |
| V2 F / S / C | | | | | |

### 10 — live-jvc-abu-dhabi · 3 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 3d8e07eb | 0 | |
| **Legacy S** | 0 | E11 | 3d8e07eb | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | 3d8e07eb | 0 | |
| V2 F / S / C | | | | | |

### 11 — live-downtown-abu-dhabi · 2 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 6eca25ae | 0 | |
| **Legacy S** | 0 | E11 | 6eca25ae | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | 6eca25ae | 0 | |
| V2 F / S / C | | | | | |

### 12 — live-downtown-ajman · 3 alts · live · **CALM non-default**

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | **E311** | b607eee9 | 0 | FASTEST uses E311 |
| **Legacy S** | 0 | E311 | b607eee9 | 0 | min_toll_zero |
| **Legacy C** | **1** | **E11** | 53278404 | 0 | CALM prefers E11 alt |
| V2 F / S / C | | | | | |

### 13 — live-business-bay-jlt · 3 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | fe46cc16 | 0 | |
| **Legacy S** | 0 | E11 | fe46cc16 | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | fe46cc16 | 0 | |
| V2 F / S / C | | | | | |

### 14 — live-difc-palm-jumeirah · 3 alts · live

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 315518d3 | 0 | |
| **Legacy S** | 0 | E11 | 315518d3 | 0 | min_toll_zero |
| **Legacy C** | 0 | E11 | 315518d3 | 0 | |
| V2 F / S / C | | | | | |

### 15 — live-marina-deira · 3 alts · live · **CALM non-default**

| | idx | corridor | geom_fp | toll | note |
|---|----:|----------|---------|-----:|------|
| **Legacy F** | 0 | E11 | 436f7d54 | 0 | |
| **Legacy S** | 0 | E11 | 436f7d54 | 0 | min_toll_zero |
| **Legacy C** | **1** | E11 | **059e9ff0** | 0 | same corridor, different geometry |
| V2 F / S / C | | | | | |

---

## Pre-flip gates (45 mode comparisons: 15 O-D × F/S/C)

### FASTEST

| Criterion | Threshold |
|-----------|-----------|
| Corridor/geometry | **≥ 14/15** → `SAME_CORRIDOR` **or** `EXPLAINABLE_DIFFERENCE` |
| Blocker | **Any** `UNEXPLAINED_DIFFERENCE` |
| Index | **Not gated** (informational only) |
| Policy B | **Not used** — duration / cost / distance / toll only |

### NO_TOLLS (semantic — not index)

| Criterion | Threshold |
|-----------|-----------|
| Toll decision | **15/15** V2 winners **semantically correct** |
| Pass | Winner minimizes `effectiveTollAedForScoring`, **or** documented time trade-off when no free alt |
| **Fail (hard block)** | Paid route chosen when **free comparable alt exists** (`FAIL_paid_*_while_free_*` in capture) |
| Index match | **Not required**, not gated |
| `_result` column | Use `SAME_CORRIDOR` / `EXPLAINABLE` only for documentation; semantic check is authoritative |

### CALM

| Criterion | Threshold |
|-----------|-----------|
| Corridor/geometry | **≥ 12/15** → `SAME_CORRIDOR` **or** `EXPLAINABLE_DIFFERENCE` |
| Blocker | **Any** `UNEXPLAINED_DIFFERENCE` |
| Non-default sanity | V2 CALM `idx ≠ 0` on **2–6/15** cases (Legacy baseline **5/15**) |
| Stress tie-break | ≤ **4/15** sessions with tie-break override |
| Manual | **Visual handoff review** on every V2 CALM non-default win |
| Policy B | **Used** — recalibrated v2 must ship bundled with adapter |

---

## Classifying `_result` (pre-flip worksheet)

For each mode, compare Legacy winner snapshot vs V2 winner snapshot:

1. Same `geom_fp` → **SAME_CORRIDOR**
2. Else run geometry overlap (85% / 70% rules) → **SAME_CORRIDOR** or continue
3. NO_TOLLS: both semantically correct + toll/time explainable → **EXPLAINABLE_DIFFERENCE**
4. FASTEST: duration delta ≤ 5 min + different corridor label → **EXPLAINABLE_DIFFERENCE**
5. CALM: same corridor different idx, or critical-count delta ≤ 3 with stress tie-break → **EXPLAINABLE_DIFFERENCE**
6. Else → **UNEXPLAINED_DIFFERENCE** → stop, do not flip

Use `StageCBaselineCapture.compareWinners(...)` in verification tests or REPL.

---

## Bundled deploy (unchanged)

One flip: V2 fetch + adapter + **prod** Policy B v2. NO_TOLLS gate validates toll I/O; FASTEST/CALM validate geometry + scoring.

---

## Pre-flip checklist (final)

```
BASELINE
[ ] Legacy table fresh (≤7 days) — this file
[ ] Export test passes — 15 rows, NO_TOLLS semantic fails = 0

V2 PASS (fill V2_* + *_result for 15×3 = 45 cells)
[ ] FASTEST:  ≥14/15 SAME_CORRIDOR or EXPLAINABLE; zero UNEXPLAINED
[ ] NO_TOLLS: 15/15 semantically correct toll decision (index ignored)
[ ] CALM:     ≥12/15 SAME_CORRIDOR or EXPLAINABLE; zero UNEXPLAINED
[ ] CALM:     2–6/15 V2 non-default; tie-break ≤4/15
[ ] CALM:     visual review on ALL V2 non-default wins

MANUAL SPOT CHECKS
[ ] live-sharjah-downtown CALM (E311 vs E11 fork)
[ ] live-ajman-difc CALM (E311 alt)
[ ] live-downtown-ajman (F=E311, C=E11 cross-mode)
[ ] live-difc-marina CALM (E11 same corridor, different geom)

FAIL ANY HARD BLOCK → do NOT flip USE_ROUTES_V2_FETCH

FLIP (only if all pass)
[ ] ArchitectureValidation.USE_ROUTES_V2_FETCH = true
[ ] Bundled: adapter + RoutesV2ManeuverPolicy in prod

POST-FLIP WEEK 1
[ ] Re-run 45 comparisons day 1 and day 7
[ ] CALM non-default stays 2–6/15
[ ] NO_TOLLS: zero semantic toll fails
[ ] Any UNEXPLAINED → rollback flag to false
```

---

## Suggested workflow (Niko)

1. Run Legacy export test → verify matches this doc (drift → update doc).
2. Debug build, V2 fetch ON → for each case/mode record V2 idx, corridor, geom_fp, toll note.
3. Classify 45 `_result` values; apply gates above.
4. Visual handoff on CALM non-default + spot-check list.
5. Flip only if checklist green.
