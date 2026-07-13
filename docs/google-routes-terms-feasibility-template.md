# Google Routes Terms Feasibility — Audit Template

## Document status

| Item | Value |
|------|-------|
| Purpose | **Template only** |
| Terms analysis | **In progress — F01 completed** |
| Implementation authorization | **No** |
| Last completed research stage | **Stage 0B** |
| Next research stage | **Terms feasibility** |

> **Warning:** This document is a research template.
>
> It is not a legal interpretation.
>
> It does not authorize implementation.
>
> It does not authorize product UX.
>
> It does not authorize production use.
>
> Implementation remains blocked until the Terms feasibility stage is completed and a separate implementation decision is explicitly recorded.

---

## How to use this template

This file defines the **structure** for a future Google Routes Terms feasibility audit.

- Fill fields only during a separately authorized **Terms feasibility** stage.
- Do not treat this template as permission to build product features.
- Do not reuse Stage 0B conclusions as proof for waypoint research.

### Placeholder rules

| Rule | Meaning |
|------|---------|
| `TBD` | To be determined during the future audit |
| `TBD` is not a verdict | Absence of analysis is not approval |
| `Unknown` ≠ `Allowed` | Unreviewed items stay blocked for implementation |
| Absence of a restriction in this template does not constitute permission | Silence here is not consent |

### Allowed verdict values (do not assign during template creation)

- `Allowed as-is`
- `Allowed with constraints`
- `Requires written confirmation`
- `Not allowed`
- `Unknown`

Unreviewed Preliminary verdict fields remain TBD. Verdicts may be assigned only during an explicitly authorized Terms feasibility audit.

---

## Feature index

| ID | Feature / UX | Preliminary verdict |
|----|--------------|---------------------|
| F01 | Multiple waypoint-constrained Google requests | Requires written confirmation |
| F02 | Standard Google alternatives baseline request (without waypoints) | TBD |
| F03 | Comparing multiple Google route responses | TBD |
| F04 | Google Map presentation | TBD |
| F05 | Non-map presentation | TBD |
| F06 | Derived labels | TBD |
| F07 | Derived explanations | TBD |
| F08 | Derived scores | TBD |
| F09 | Session-only processing | TBD |
| F10 | Persistence | TBD |
| F11 | Caching | TBD |
| F12 | Route fingerprints | TBD |
| F13 | Aggregated metrics | TBD |
| F14 | User studies | TBD |
| F15 | Route geometry | TBD |
| F16 | Steps | TBD |
| F17 | Maneuvers | TBD |
| F18 | Traffic intervals | TBD |
| F19 | ETA comparison | TBD |
| F20 | Toll comparison | TBD |
| F21 | External navigation handoff to Google Maps | TBD |

---

## Feature records

Each feature uses the same field set. All values are `TBD` until the future audit.

### F01 — Multiple waypoint-constrained Google requests

| Field | Value |
|-------|-------|
| Feature / UX | Multiple waypoint-constrained Google requests |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | Routes API — Set intermediate waypoints (`https://developers.google.com/maps/documentation/routes/intermed_waypoints`); Routes API — Set a point for a route to pass through (`https://developers.google.com/maps/documentation/routes/pass-through`); Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`); Routes API — Usage and billing (`https://developers.google.com/maps/documentation/routes/usage-and-billing`); Routes API — Policies (`https://developers.google.com/maps/documentation/routes/policies`); Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`); Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`); Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — applicability depends on billing-account jurisdiction (TBD) |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.2 License Requirements and Restrictions (including 3.2.1 General Restrictions: No Scraping; No Re-Creating Google Products or Features); Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching); Google Maps Platform EEA Service Specific Terms — Section 20 Routes API (if EEA billing applies) — cross-reference only, not analyzed in this feature |
| Preliminary verdict | **Requires written confirmation** |
| Evidence | **Technical API capability: Confirmed.** Google Routes API officially supports intermediate waypoints for `computeRoutes`, including stopover and pass-through (`via: true`) behavior within documented limits (up to 25 intermediate waypoints per request). **Terms permission for proposed MARSHIO use: Not established.** Written confirmation is required for the proposed systematic multi-request waypoint research pattern in the concrete MARSHIO UX. The reviewed official sources confirm the API capability, but they do not provide sufficiently specific affirmative authorization for the complete proposed MARSHIO multi-request research pattern (multiple separate waypoint-constrained requests → systematic corridor probing → comparison of returned Google routes → derived labels/explanations/scores → concrete display, attribution, and retention). Absence of an identified prohibition is not affirmative permission. **Billing and API facts (technical, not Terms verdict):** billing and API key required; documented waypoint limits apply; SKU depends on waypoint count and features used; traffic-aware or other advanced features may affect SKU; `computeAlternativeRoutes` does not return alternatives when intermediate waypoints are present — multiple corridor candidates require separate requests. **Cross-references only (not analyzed in F01):** Service Specific Terms §19.3 caching (F11); map presentation and attribution (F04, F05); route comparison (F03); derived labels/explanations/scores (F06–F08); persistence and retention (F10, F11). |
| Open questions | Terms classification cannot be completed without a concrete proposed UX (see `Concrete proposed UX` — TBD). Which billing-account jurisdiction and controlling Terms apply? What exact multi-request research volume and frequency are proposed? Does the pattern fall within or conflict with applicable No Scraping restrictions (ToS §3.2.1(a))? Does the pattern fall within or conflict with applicable No Re-Creating restrictions (ToS §3.2.1(d))? What exact Customer Application UX will display and compare the results? Will any route content, coordinates, derived labels, fingerprints, or scores be retained? What attribution and Google Map presentation will be used? Which parts of the proposed pattern require written confirmation from Google Maps Platform support or sales? Cross-feature dependencies remain TBD: F03 (comparing route responses), F04/F05 (map presentation, attribution), F06–F08 (derived labels, explanations, scores), F09–F11 (session processing, persistence, caching), F12 (route fingerprints). |
| Written confirmation required | **Yes** — Terms permission for the proposed systematic multi-request waypoint research pattern is not established from reviewed official sources alone. Written confirmation required for billing-account jurisdiction; multi-request research volume and pattern under License Restrictions §3.2.1(a) and §3.2.1(d); and any disputed aspects of the concrete MARSHIO UX. Escalation to Google Maps Platform support or sales if internal review cannot resolve open questions. |
| Notes | TBD |

### F02 — Standard Google alternatives baseline request (without waypoints)

| Field | Value |
|-------|-------|
| Feature / UX | Standard Google alternatives baseline request (without waypoints) |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F03 — Comparing multiple Google route responses

| Field | Value |
|-------|-------|
| Feature / UX | Comparing multiple Google route responses |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F04 — Google Map presentation

| Field | Value |
|-------|-------|
| Feature / UX | Google Map presentation |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F05 — Non-map presentation

| Field | Value |
|-------|-------|
| Feature / UX | Non-map presentation |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F06 — Derived labels

| Field | Value |
|-------|-------|
| Feature / UX | Derived labels |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F07 — Derived explanations

| Field | Value |
|-------|-------|
| Feature / UX | Derived explanations |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F08 — Derived scores

| Field | Value |
|-------|-------|
| Feature / UX | Derived scores |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F09 — Session-only processing

| Field | Value |
|-------|-------|
| Feature / UX | Session-only processing |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F10 — Persistence

| Field | Value |
|-------|-------|
| Feature / UX | Persistence |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F11 — Caching

| Field | Value |
|-------|-------|
| Feature / UX | Caching |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F12 — Route fingerprints

| Field | Value |
|-------|-------|
| Feature / UX | Route fingerprints |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F13 — Aggregated metrics

| Field | Value |
|-------|-------|
| Feature / UX | Aggregated metrics |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F14 — User studies

| Field | Value |
|-------|-------|
| Feature / UX | User studies |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F15 — Route geometry

| Field | Value |
|-------|-------|
| Feature / UX | Route geometry |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F16 — Steps

| Field | Value |
|-------|-------|
| Feature / UX | Steps |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F17 — Maneuvers

| Field | Value |
|-------|-------|
| Feature / UX | Maneuvers |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F18 — Traffic intervals

| Field | Value |
|-------|-------|
| Feature / UX | Traffic intervals |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F19 — ETA comparison

| Field | Value |
|-------|-------|
| Feature / UX | ETA comparison |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F20 — Toll comparison

| Field | Value |
|-------|-------|
| Feature / UX | Toll comparison |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

### F21 — External navigation handoff to Google Maps

| Field | Value |
|-------|-------|
| Feature / UX | External navigation handoff to Google Maps |
| Concrete proposed UX | TBD |
| Google inputs | TBD |
| MARSHIO-owned inputs | TBD |
| Displayed output | TBD |
| Stored? | TBD |
| Retention | TBD |
| Google attribution | TBD |
| Applicable Google documents | TBD |
| Relevant Terms sections | TBD |
| Preliminary verdict | TBD |
| Evidence | TBD |
| Open questions | TBD |
| Written confirmation required | TBD |
| Notes | TBD |

---

## Future audit completion checklist

These are completion criteria for a future audit. They are not current results.

- [ ] Applicable billing-account jurisdiction confirmed
- [ ] Current controlling Terms identified
- [ ] Applicable service-specific terms identified
- [ ] Applicable product documentation identified
- [ ] Every feature reviewed in its concrete proposed UX
- [ ] Each feature assigned a verdict
- [ ] Evidence linked for every verdict
- [ ] Retention requirements reviewed
- [ ] Caching requirements reviewed
- [ ] Attribution requirements reviewed
- [ ] Google Map and non-map presentation reviewed separately
- [ ] Open questions resolved or escalated
- [ ] Required written confirmations preserved
- [ ] Unresolved items remain classified as Unknown
- [ ] Separate research authorization recorded
- [ ] Separate implementation authorization recorded
