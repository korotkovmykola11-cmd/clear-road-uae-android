# Stage 38.3 — Field Log Review Baseline

**Status:** CLOSED  
**Date frozen:** 2026-06-19  
**Branch:** `architecture-validation/recommendation-only-home` (Stage 38.1 audit logging)  
**Scope:** Read-only field analysis. No scoring, UI, or selection changes.

---

## Hypothesis status

**CONFIRMED ENOUGH TO CONTINUE — NOT CONFIRMED ENOUGH TO SHIP**

The driver-stress signal extracted from Google Directions steps is **not noise**. CALM (`SmoothDriveScoring`) **does not consistently align** with that signal, especially in PM rush.

---

## Dataset

| Bucket | Date / window | Dedup CALM route sets |
|--------|---------------|----------------------|
| Previous evening | 2026-06-18 ~21:23–21:27 | 14 |
| AM rush | 2026-06-19 ~06:48–06:53 | 17 |
| PM rush | 2026-06-19 ~18:14–18:18 | 16 |
| **Total** | | **47** |

Dedup key: `(routeCount, duration_min + distance_km per route index)`.

---

## Aggregate metrics (47 CALM sets)

| Metric | Value |
|--------|-------|
| `calmPickedLowestStress` | **19 / 47 (40%)** |
| Meaningful opportunities (≥20% maneuvers saved, ≤5 min time penalty) | **14 / 47 (30%)** |
| Strong opportunities (≥35%, ≤5 min) | **9 / 47 (19%)** |
| Very strong (≥50%, ≤5 min) | **2 / 47 (4%)** |

---

## By bucket

| Bucket | Sets | CALM match | Meaningful (all sets) |
|--------|------|------------|------------------------|
| 2026-06-18 PM | 14 | 36% | 43% |
| 2026-06-19 AM | 17 | 59% | 29% |
| 2026-06-19 PM | 16 | 25% | 19% |

---

## Key finding

Traffic delay and step-derived stress are **different signals**:

- **AM:** delay weaker; CALM aligns with lowest stress more often.
- **PM:** `delayScore` dominates; CALM often skips large stress gaps at **zero SESSION time penalty**.

Anchor cases for Stage 38.4 replay verification:

- `2026-06-19 18:15:09` — stress gap 40%, timePenalty 0
- `2026-06-19 18:17:35` — stress gap 44%, timePenalty 0

See `docs/stage-38.4-replay-audit.md`.

---

## Why Stage 39 exists

Not because “Google lacks recommendations.” Because field data shows a **CALM scoring blind spot**: the formula can pick a route that is **more stressful without a SESSION-measured time win**, when a lower-stress alternative exists within the same route set.

Stage 39 Spec is approved (minor edit). Stage 39 implementation is **HOLD** until Stage 38.4 replay and offline replay gate.

---

## Out of scope for this baseline

- Subjective drive notes (calm / nervous)
- Weekend off-peak bucket
- Implementation or scoring changes
- User-facing copy
