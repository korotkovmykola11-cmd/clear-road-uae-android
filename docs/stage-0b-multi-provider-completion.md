# Stage 0B — Multi-Provider Live Benchmark

**Status:** Completed  
**Date:** 2026-07-12  
**Scope:** Documentation-only closure. Benchmark infrastructure preserved. No production changes.

---

## Experiment status

| Item | Value |
|------|-------|
| Stage | 0B |
| Status | **Completed** |
| Technical outcome | Benchmark pilot executed successfully |
| Product verdict | Multi-provider route expansion hypothesis: **NOT SUFFICIENTLY SUPPORTED** |
| Expansion decision | **Do not expand** current multi-provider hypothesis to 10 cases |

Stage 0B is **not** failed, abandoned, or unfinished. The experiment finished correctly; the product hypothesis did not gain sufficient support to justify further expansion.

---

## Hypothesis tested

> GraphHopper systematically adds useful route alternatives not represented among Google routes.

This wording records the original research question in neutral form. It does not assert provider superiority, universal UAE coverage, or final rejection of GraphHopper for all future use cases.

---

## Version references

Reproducible implementation anchors (benchmark test sources only):

| SHA | Commit message | Relevance |
|-----|----------------|-----------|
| `cc3b45a` | `test(benchmark): add detailed Stage 0B live analysis report` | Report schema, case summaries, pair comparisons |
| `dee8e1f` | `test(benchmark): normalize Arabic UAE road codes safely` | Offline corridor-label re-evaluation for `live-difc-marina` |
| `49d7bce` | `test(benchmark): add safe Stage 0B case window selection` | Final controlled two-case live run |

| Run | Repository state |
|-----|------------------|
| Initial pilot (`live-difc-marina`) | Exact run SHA **not preserved** in this completion record. Pilot predates `dee8e1f`. |
| Final controlled window (2 cases) | `49d7bce` |

| Component | Source file |
|-----------|-------------|
| Report schema | `Stage0BLiveRunResult.kt` — `stage0b-live-report-v2` |
| Case catalog | `Stage0BLiveCases.kt` — `stage0b-v1-10cases` |
| Similarity algorithm | `GeometrySimilarity.kt` — `geometry-similarity-v1` |
| Duplicate / genuinely-different thresholds | `GeometrySimilarity.kt` |
| GraphHopper novelty predicate | `Stage0BLiveRunResult.kt` — `hasGraphHopperCandidateWithoutAnyGoogleDuplicateHypothesis()` |
| Request budget caps | `BenchmarkLiveBudget.kt` — total 20, Google 10, GraphHopper 10 |
| Live entry gate | `Stage0BLiveEntryGateTest.kt` |

---

## Request configuration (non-secret)

| Setting | Value |
|---------|-------|
| Providers | Google Routes API v2; GraphHopper Cloud Routing API v1 |
| Execution | Sequential (one provider fetch per budget permit) |
| Retry | Disabled |
| Redirect | Disabled (`instanceFollowRedirects = false`; redirect responses rejected) |
| Initial pilot selection | `live-difc-marina` (default offset 0, count 1) |
| Final controlled selection | `caseOffset = 1`, `caseCount = 2` |

No API keys, credential-bearing URLs, raw request bodies, coordinates, or polylines are recorded here.

---

## Request count and budget

| Metric | Value |
|--------|-------|
| Completed live cases | 3 |
| Google requests | 3 |
| GraphHopper requests | 3 |
| Total provider requests | 6 |
| Retries | 0 |
| Redirects | 0 |

| Phase | Cases | Provider requests |
|-------|-------|-------------------|
| Initial pilot | 1 (`live-difc-marina`) | 2 |
| Controlled final window | 2 (`live-marina-airport-t3`, `live-sharjah-downtown`) | 4 |

All runs stayed within `BenchmarkLiveBudget` caps (20 total / 10 per provider). Actual reservations: 6 of 20 total.

---

## Classification thresholds

Benchmark hypotheses used during analysis:

| Hypothesis | Rule |
|------------|------|
| Duplicate hypothesis | shared geometry overlap ≥ **85%** |
| Genuinely-different hypothesis | shared geometry overlap < **70%** |

These are **benchmark classification thresholds**, not validated measures of user value. Full logic (including invalid-geometry handling and case-level novelty predicate) is defined in `GeometrySimilarity.kt` and `Stage0BLiveRunResult.kt` at the SHAs above.

A GraphHopper candidate counts as **non-duplicate under the benchmark hypothesis** when it has at least one Google comparison and **no** GH-vs-Google pair reaches the duplicate threshold.

---

## Product-level continuation criteria

A formal product-level success threshold was **not** preserved as a versioned repository artifact before the live runs.

The continuation decision therefore used:

- the observed duplicate pattern across completed cases;
- the isolated nature of the E66 geometry/corridor signal;
- the absence of demonstrated user value from that signal.

The original infrastructure supported expansion to ten cases. The decision not to continue was made **after reviewing the three completed cases**, not because a fixed sample size of three was pre-declared as sufficient.

---

## Completed live scope

Three live cases were executed across two phases:

| Case ID | Phase | Notes |
|---------|-------|-------|
| `live-difc-marina` | Initial pilot | Single-case gate validation |
| `live-marina-airport-t3` | Controlled final window | offset=1, count=2 |
| `live-sharjah-downtown` | Controlled final window | offset=1, count=2 |

`live-difc-marina` was **not** included in the final controlled window.

The benchmark infrastructure successfully completed a controlled live pilot:

- provider fetch and parsing (Google Routes v2, GraphHopper Routing v1)
- candidate merging and corridor classification
- geometry fingerprinting and pairwise overlap analysis
- duplicate-hypothesis and novelty predicates
- sanitized readable reporting (`stage0b-live-report-v2`)
- request budget and live entry gate enforcement

Continuation to the full 10-case catalog was **consciously stopped**. The stop was driven by insufficient product payoff from further expansion, **not** by benchmark malfunction.

---

## Aggregated results by case

### `live-difc-marina` (initial pilot)

| Metric | Value |
|--------|-------|
| Google candidates | 3 |
| GraphHopper candidates | 1 |
| GraphHopper duplicate hypotheses | 1 of 1 |
| GraphHopper non-duplicate hypotheses | 0 of 1 |

**Key cross-provider result:** GH-0 vs GOOGLE-0 overlap = **100%**

**Novelty outcome:** `graphHopperCandidateWithoutAnyGoogleDuplicateHypothesis = no`

**Corridor labels** (re-evaluated offline after benchmark-only Arabic road-code normalization; geometry / overlap / novelty verdict unchanged):

| Candidate | Primary corridor |
|-----------|------------------|
| GOOGLE-0 | E11 |
| GOOGLE-1 | E11 |
| GOOGLE-2 | E44 |
| GRAPHHOPPER-0 | E11 |

Consistency status for this pilot was **not preserved** as a versioned repository artifact.

---

### `live-marina-airport-t3` (controlled final)

| Metric | Value |
|--------|-------|
| Google candidates | 3 |
| GraphHopper candidates | 2 |
| GraphHopper duplicate hypotheses | 2 of 2 |
| GraphHopper non-duplicate hypotheses | 0 of 2 |

**Key cross-provider results:**

- GH-0 vs GOOGLE-0 overlap = **100%**
- GH-1 vs GOOGLE-1 overlap = **100%**

**Novelty outcome:** `graphHopperCandidateWithoutAnyGoogleDuplicateHypothesis = no`

GH-1 carried **Al Khail** connector evidence, but **no new primary corridor** was confirmed.

**Consistency:** `consistencyStatus = OK`

---

### `live-sharjah-downtown` (controlled final)

| Metric | Value |
|--------|-------|
| Google candidates | 2 |
| GraphHopper candidates | 3 |
| GraphHopper duplicate hypotheses | 2 of 3 |
| GraphHopper non-duplicate hypotheses | 1 of 3 |

**Key cross-provider results:**

- GH-0 vs GOOGLE-0 overlap = **93.6%** (duplicate hypothesis)
- GH-2 vs GOOGLE-1 overlap = **93.5%** (duplicate hypothesis)

**Positive signal (preserved explicitly):** GH-1 had primary corridor **E66** and did **not** meet the duplicate threshold against any Google candidate.

**Limitation of that signal:** E66 already appeared in GOOGLE-0 evidence as a **connector**, not as Google's primary corridor.

Exact GH-1 vs Google overlap values were **not preserved** as versioned repository artifacts. This record proves the non-duplicate predicate passed, not the geometric margin against the 85% threshold.

**Novelty outcome:** `graphHopperCandidateWithoutAnyGoogleDuplicateHypothesis = yes`

A geometry/corridor novelty hypothesis was observed, but **user value was not established**. GH-1 is **not** recorded here as a proven useful route.

**Consistency:** `consistencyStatus = OK`

---

## Aggregate summary

Across the **two final controlled cases** (`live-marina-airport-t3`, `live-sharjah-downtown`):

- **4 of 5** GraphHopper candidates had a duplicate hypothesis against at least one Google candidate.

Across **all three completed Stage 0B cases**:

- **5 of 6** GraphHopper candidates had a duplicate hypothesis;
- **1 of 6** was non-duplicate under the benchmark hypothesis.

The only non-duplicate GraphHopper candidate was the **E66-primary candidate** in `live-sharjah-downtown`.

This aggregate describes the **observed sample only**. It is not a universal conclusion about GraphHopper across UAE routes.

---

## Stopping rationale

| Case | GH duplicate pattern |
|------|------------------------|
| DIFC → Marina | 1 / 1 GH duplicate |
| Marina → Airport T3 | 2 / 2 GH duplicate |
| Sharjah → Downtown | 2 / 3 GH duplicate; 1 / 3 non-duplicate E66 hypothesis |

**Engineering decision:**

The evidence was sufficient for a **resource-allocation no-go** on expanding the current GraphHopper-vs-Google series to ten cases.

It was **not** sufficient to reject the hypothesis universally across all UAE routes, providers, or future traffic conditions.

The additional cost of seven remaining catalog cases was **not justified** by the strength and consistency of the observed signal. This does **not** assert that those seven cases would certainly produce no new signal.

---

## Consistency and sanitization

For the **two controlled final cases**:

- `consistencyStatus = OK` (both cases)
- Report sanitization checks passed at test time (no API keys, credential URLs, raw JSON, coordinates, or encoded polylines in printed reports)

This completion record contains **no** API keys, credential-bearing URLs, raw JSON, coordinates, or encoded polylines.

---

## ETA, toll, and user-value limitations

- Provider-native ETA values were **not** treated as directly comparable ground truth.
- The experiment focused primarily on route candidate geometry, corridor evidence, and duplicate/novelty hypotheses.
- Toll interpretation was **not** sufficient to establish user value.
- Reported `tollAed=0` may mean **unavailable** depending on provider adapter, not necessarily zero toll.
- A geometry-novel candidate was **not** automatically considered faster, safer, simpler, more stable, or preferable.

---

## What was demonstrated

- The live benchmark path is operational end-to-end.
- Request budget and controlled execution enabled a bounded pilot without uncontrolled API spend.
- GraphHopper frequently returned candidates classified as **duplicate hypotheses** relative to Google routes.
- The isolated non-duplicating GraphHopper candidate did not demonstrate **systematic user value** from a second provider under this hypothesis.
- Collected evidence is sufficient for a **no-go** on expanding this specific multi-provider hypothesis.

---

## What was not demonstrated

- This is **not** a universal provider comparison for UAE routing.
- The experiment does **not** cover all UAE origin–destination pairs.
- Provider-native ETA values were **not** treated as directly comparable ground truth.
- Geometry novelty is **not** equivalent to user-facing route value.
- The experiment did **not** evaluate new corridor-generation methods.
- Stage 0B outcome is **not** a decision to close MARSHIO or end route-decision research.

---

## Timestamps

Exact live-run timestamps were **not preserved** as versioned repository artifacts.

---

## Infrastructure disposition

The following remain **unchanged and available** for offline use or future research:

- `Stage0BLiveRunner`, live entry gate, request budget caps
- Google and GraphHopper live benchmark providers
- 10-case catalog (`Stage0BLiveCases`) — catalog retained; full live sweep not executed
- Report schema, sanitization, geometry similarity, duplicate-hypothesis thresholds
- Completed benchmark unit tests

GraphHopper integration code is preserved as **experimental benchmark infrastructure**, not as an active production dependency.

---

## Next research stage

| Item | Value |
|------|-------|
| Status | **Not started** |
| Hypothesis | Not defined in this document |
| Stage identifier | To be assigned separately |

Any future work on additional route value must:

1. Start as a **separate stage** with its own identifier.
2. State an explicit hypothesis before live execution.
3. Define success and no-go criteria in advance.
4. Use a dedicated request budget.
5. **Not** reuse Stage 0B conclusions as proof for a new idea.

---

## Related artifacts

| Artifact | Location |
|----------|----------|
| Stage 0A fixture benchmark (offline) | `app/src/test/java/com/clearroad/app/benchmark/Stage0AFixtureBenchmarkTest.kt` |
| Stage 0B live entry + gate tests | `app/src/test/java/com/clearroad/app/benchmark/Stage0BLiveEntryGateTest.kt` |
| Stage 0B live entry benchmark | `app/src/test/java/com/clearroad/app/benchmark/Stage0BLiveBenchmarkTest.kt` |
| Live case catalog (10 cases, 3 executed live) | `app/src/test/java/com/clearroad/app/benchmark/Stage0BLiveCases.kt` |
| Offline difc-marina corridor re-evaluation | `app/src/test/java/com/clearroad/app/benchmark/BenchmarkCorridorTextNormalizerTest.kt` |

Live run outputs were **not versioned** in the repository. This record retains approved aggregate conclusions and counts verified against completed live runs.
