# Stage 34.5 — SMOOTH DRIVE Implementation Freeze

**Status:** Frozen  
**Date:** 2026-06-02  
**Scope:** Internal project note — no app behavior change

---

## Decision

**Current SMOOTH DRIVE remains unchanged.**

The new predictability-based SMOOTH formula (Stages 34.2–34.4) is **NOT implemented**.

---

## Reason

Live Stage 34.4 spot-checks against real Google Directions data did not validate the proposed formula:

- On all 15 tested alternatives (5 UAE routes), `duration_in_traffic` was **equal to or lower than** base `duration`.
- The primary signal — **traffic delay ratio** — was **0% on every alternative** at fetch time.
- NEW SMOOTH winners were therefore driven almost entirely by the **corridor heuristic**, not predictability.
- In at least one key case (DIFC → Dubai Marina), NEW SMOOTH converged with SAVE AED — the opposite of the intended product behavior.
- Corridor keyword weights are **not calibrated** enough for production use.

Until live data proves the model, implementing the formula would change UI promises without a validated scoring basis.

---

## Product definitions (locked for future work)

| Mode | Definition |
|------|------------|
| **FASTEST** | Get there as quickly as possible. |
| **SAVE AED** | Reduce Salik and trip cost. |
| **SMOOTH DRIVE** | Choose the most predictable and least stressful trip, even if it costs a little more or takes a little longer. |

These definitions stand. **Scoring does not yet match the SMOOTH DRIVE definition** — current CALM scoring remains cost-weighted (see Stage 34.0 audit).

---

## Future work

Repeat live tests during **peak traffic** (typical windows: Dubai weekday 07:30–09:30, 17:00–19:30 GST).

Use existing `DirectionsAudit` logcat output — no new APIs required.

---

## Required before implementation

1. **Peak-hour DirectionsAudit tests** — at least 5 UAE O/D pairs with real alternatives logged.
2. **Traffic delay > 0 on real routes** — confirm `duration_in_traffic` exceeds base `duration` on some alternatives so the delay-ratio signal is active.
3. **Calibration of corridor heuristics** — validate highway vs urban weave weights against live winners; fix cases where corridor alone inverts intent (e.g. DIFC → Marina).
4. **Proof that SMOOTH differs from SAVE AED for the right reasons** — not merely different index, but predictability/stress logic, not toll minimization.

---

## What stays as-is

- `PreferenceMode.CALM` scoring in `MainActivity.kt` — unchanged.
- Home card SMOOTH DRIVE UI copy — unchanged.
- No Stage 35.0 scoring work until the four gates above pass.

---

## Reference audits

| Stage | Topic |
|-------|-------|
| 34.0 | Current CALM ≈ SAVE AED Lite |
| 34.1 | Product definition — predictability, not cost |
| 34.2 | Proposed formula (paper only) |
| 34.3 | Paper validation — 10 UAE scenarios |
| 34.4 | Live spot-check — 5 routes, 3/5 differ from SAVE AED, delay signal inactive |
| **34.5** | **This freeze note** |
