# MARSHIO Research Decision Record 001

## Staged Validation Methodology

| Field | Value |
|-------|-------|
| Status | **Approved by Product Owner** |
| Decision type | Research methodology |
| Approval date | 2026-07-13 |
| Stage A authorization | **Not granted** |
| Implementation authorization | **Not granted** |

---

## Decision statement

**The MARSHIO research program adopts a staged validation methodology.**

Product-value evidence, measurement feasibility, statistical effectiveness, Google Terms clearance, Google-content ecological validity, and implementation feasibility must not be treated as one experiment or one combined decision.

Independent review found that the previous integrated validation model mixed scientifically different questions—measurement feasibility, controlled presentation efficacy, Terms permission, ecological transfer to real Google content, and implementation readiness—within a single study design. That integration created premature dependencies and conclusions before each question had appropriate evidence. This decision adopts explicitly gated stages so each question is answered with the evidence type it requires.

This record documents an already-made governance decision. It does not describe the prior approach as a project failure.

---

## Official research sequence

Progression between stages is **not automatic**. Each stage requires satisfaction of its gate and **explicit Product Owner authorization**.

```
Stage A — Synthetic Feasibility Pilot
        ↓
Stage B — Powered Synthetic RCT
        ↓
Stage C1 — Google Terms Clearance
        ↓
Stage C2 — Google-content Ecological Validation
```

---

## Stage A — Synthetic Feasibility Pilot

### Purpose

- Validate measurement feasibility
- Verify task comprehensibility
- Estimate baseline behaviour
- Detect baseline ceiling effects
- Estimate approximate variance for later sample-size planning
- Verify decision-time measurement feasibility
- Estimate missing, abstention and exclusion behaviour

### Restrictions

- MARSHIO-owned synthetic stimuli only
- No Google content
- No Google Maps
- No Google screenshots
- No Google polylines
- No Google coordinates
- No Google API
- No Google-derived artifacts
- No production implementation
- No product GO/KILL

### Output

**Planning estimates only.**

**Methodological boundary:**

> Stage A cannot confirm or reject the Candidate MVP hypothesis.
>
> No product GO/KILL decision may be made from Stage A.

### Stage A gate

Progression from Stage A to Stage B is **not automatic**.

Stage B requires:

- Successful measurement-feasibility results
- Completion of all required research-governance checks
- Approval of all required Product Owner decisions listed in this record
- Explicit Product Owner authorization

Numerical feasibility criteria for Stage A are **not defined in this record**.

---

## Stage B — Powered Synthetic RCT

### Purpose

Determine whether MARSHIO’s decision-first presentation improves mode-consistent decision performance relative to a defined neutral baseline under controlled synthetic conditions.

### Restrictions

- Synthetic stimuli only
- No Google content
- No Google-derived artifacts
- No Google Maps or Google route presentation
- Minimum meaningful uplift is owner-defined independently
- Decision-time limit is owner-defined independently
- KILL scope is owner-defined independently
- Pilot results may inform variance and sample size
- Pilot results must **not** retrospectively determine the minimum meaningful uplift

**Explicit separation:**

> Stage B continues to use MARSHIO-owned synthetic stimuli.
>
> It introduces no Google content or Google-derived artifacts.
>
> No Google-derived stimulus may be prepared or shown before Stage C1 clearance.

### Output

A decision only about the **synthetic presentation hypothesis**.

Stage B does **not** establish:

- Full Candidate MVP value
- Google-content ecological validity
- Terms permission
- Implementation authorization

### Stage B gate

Progression from Stage B to Stage C1 is **not automatic**.

Stage C1 requires:

- A GO for the explicitly scoped synthetic presentation hypothesis
- Completion of the preregistered Stage B analysis
- Explicit Product Owner authorization

A Stage B KILL applies only to the scope defined before Stage B begins. It must **not** automatically kill unrelated MARSHIO hypotheses or the entire project.

---

## Stage C1 — Google Terms Clearance

### Purpose

Determine whether the exact proposed Stage C2 protocol is permitted, including:

- Stimulus
- Google inputs
- Presentation
- Attribution
- Processing
- Comparison
- Derived output
- Storage
- Caching
- Retention
- Participant-study workflow
- Research outputs

### Restrictions

- Terms assessment only
- No participant study
- No live study execution
- No implementation authorization
- No inference of permission from technical API capability
- No inference of permission from the absence of an explicit prohibition

**Explicit boundary:**

> No Google-content study may begin before clearance of the exact Stage C2 protocol.
>
> The existing F01–F05 assessments must not be represented as blanket clearance for Stage C2.

### Stage C1 gate

Progression from Stage C1 to Stage C2 is **not automatic**.

Stage C2 requires:

- Clearance of the exact Stage C2 protocol
- Resolution of all blocking Terms questions
- Completion of required attribution, processing and retention constraints
- Explicit Product Owner authorization

---

## Stage C2 — Google-content Ecological Validation

### Purpose

Determine whether the effect observed under synthetic conditions persists when using authorized real Google-derived content.

Stage C2 evaluates **ecological validity**.

> Stage C2 tests the ecological validity of the same product hypothesis.
>
> It does not create a new product hypothesis.

Stage C2 is distinct from:

- Terms clearance
- The initial synthetic efficacy test
- Implementation authorization

### Explicit exclusions

Stage C2 does **not** claim or measure:

- Navigation fidelity after external handoff
- Actual route following
- Real trip outcomes
- ETA accuracy in the wild
- Toll accuracy in the wild
- Production readiness

### Stage C2 boundary

> Stage C2 success does not authorize implementation.
>
> Any implementation decision requires a separate architecture, safety, privacy, product and implementation authorization process.

---

## Official candidate product hypothesis

**Candidate wording (not finally approved):**

> MARSHIO helps drivers make a mode-consistent route choice (Fastest or No Tolls) within approximately three seconds.

**Status:** **Candidate — not yet finally approved.**

Final approval must clarify:

- The meaning of *mode-consistent*
- That the decision is among presented alternatives
- The navigation-fidelity exclusion
- The exact decision-time limit
- The ecological scope of the claim

This record does not finalize the wording.

---

## Candidate primary outcome

**Candidate wording (not finally approved):**

> Correct choice within a fixed decision-time limit.

**Status:** **Candidate — not yet finally approved.**

The following are **not** primary outcomes:

- Trust
- Recommendation acceptance
- Recognition
- Recall
- Agreement with MARSHIO
- Google-route identification

This record does not select a numerical time limit. It does not decide whether the outcome will be composite or separately reported.

---

## Research principles

1. Measurement feasibility precedes powered efficacy testing.

2. Synthetic presentation efficacy is evaluated before Google-content integration.

3. Synthetic efficacy is evidence about the controlled presentation hypothesis, not proof of full product value or ecological validity.

4. Powered synthetic testing precedes Google-content ecological validation.

5. Google Terms do not determine the MVP.

6. Google Terms clearance does not prove product value.

7. Terms clearance never implies implementation authorization.

8. Synthetic success does not imply ecological validity.

9. Ecological validity does not imply implementation authorization.

10. Progression between stages is never automatic.

11. Each stage requires satisfaction of its gate and explicit Product Owner authorization.

---

## Deferred Product Owner decisions

Before Stage A begins, the Product Owner must approve:

- Final candidate-product-hypothesis wording
- Meaningful duration margin
- Meaningful toll difference
- Exact decision-time limit
- Synthetic case design philosophy
- Synthetic stimulus provenance rules
- Number of synthetic cases
- Mode balance
- Acceptable baseline ceiling
- Primary outcome structure
- Participant population
- Research consent requirements
- Participant privacy requirements
- Study-data retention requirements
- Stage A feasibility criteria
- Minimum meaningful uplift for Stage B
- Stage B GO/KILL scope
- Stage C2 definition of *effect persists*

This record does not select values or propose defaults.

---

## Synthetic provenance requirement

All Stage A and Stage B stimuli must have documented MARSHIO-owned provenance.

Synthetic route cards must **not** reproduce or derive from:

- Google route responses
- Google Maps screenshots
- Google polylines
- Google coordinates
- Google route summaries
- Google road labels
- Google-derived numeric route facts
- Cached Google content

Synthetic data may be realistic, but its generation and ownership must be independently documented.

This record does not design the generation process.

---

## Research-governance boundary

Before any human-participant study begins, a separate authorization must address:

- Informed consent
- Privacy notice
- Pseudonymization
- Recruitment criteria
- Participant withdrawal
- Response-data retention
- Access control
- Deletion policy
- Applicable research and privacy requirements

This record does not perform that assessment. Google Terms clearance does **not** cover participant privacy or research governance.

---

## Current project status

| Item | Status |
|------|--------|
| Stage 0B | **Completed** |
| Terms feasibility | **In progress — F01–F05 reviewed** |
| Waypoint research | **Blocked** |
| Implementation | **Not authorized** |
| Staged validation methodology | **Approved** |
| Stage A | **Not started — blocked pending Product Owner decisions and separate authorization** |
| Stage B | **Not started** |
| Stage C1 | **Not started** |
| Stage C2 | **Not started** |

Terms feasibility is **not** marked completed by this record.

---

## Explicitly not authorized

This decision does **not** authorize:

- Stage A execution
- Synthetic stimulus creation
- Participant recruitment
- Participant data collection
- F06–F21 Terms assessments
- Google-content experiments
- Waypoint research
- Production implementation
- Code changes
- Benchmark changes
- Live API experiments
- Google API requests
- Architecture changes

---

## Rationale

- The integrated model mixed measurement feasibility, efficacy, Terms compliance, and ecological validity within one decision path.
- Independent review found these require different evidence types and should not be answered simultaneously.
- Staged gates reduce premature product conclusions before measurement and permission are established.
- Synthetic testing (Stages A and B) prevents Google Terms status from defining the product hypothesis before controlled presentation evidence exists.
- Stage C1 prevents unauthorized Google-content participant studies.
- Stage C2 prevents synthetic success from being mistaken for real-content ecological validity.

Adoption of this methodology does not guarantee product success. It defines how evidence must be gathered before major build, Terms, or implementation decisions.
