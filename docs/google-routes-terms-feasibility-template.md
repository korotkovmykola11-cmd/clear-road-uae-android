# Google Routes Terms Feasibility — Audit Template

## Document status

| Item | Value |
|------|-------|
| Purpose | **Template only** |
| Terms analysis | **In progress — F01, F02, F03, F04 and F05 reviewed** |
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
| F02 | Standard Google alternatives baseline request (without waypoints) | Allowed with constraints |
| F03 | Comparing multiple Google route responses | Allowed with constraints |
| F04 | Google Map presentation | Allowed with constraints |
| F05 | Non-map presentation | Allowed with constraints |
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
| Applicable Google documents | Routes API — Set intermediate waypoints (`https://developers.google.com/maps/documentation/routes/intermed_waypoints`); Routes API — Set a point for a route to pass through (`https://developers.google.com/maps/documentation/routes/pass-through`); Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`); Routes API — Usage and billing (`https://developers.google.com/maps/documentation/routes/usage-and-billing`); Routes API — Policies (`https://developers.google.com/maps/documentation/routes/policies`) — accessed 2026-07-13 — EEA-specific Terms for customers with billing address in the EEA; standard Terms when billing address is not in the EEA. Google Cloud — Modify your Cloud Billing account (`https://cloud.google.com/billing/docs/how-to/modify-billing-account`) — accessed 2026-07-13 — Cloud Billing account linked to Google payments profile at creation; linked payments profile uses the same country as the billing account; mailing addresses stored on associated payments profile. Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`) — accessed 2026-07-13 — EEA TOS applies if billing account address is in the EEA. Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`) — accessed 2026-07-13 — controlling for confirmed non-EEA billing account. Google Maps Platform EEA Terms of Service (`https://cloud.google.com/terms/maps-platform/eea`) — accessed 2026-07-13 — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — accessed 2026-07-13 — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.2 License Requirements and Restrictions (including 3.2.1 General Restrictions: No Scraping; No Re-Creating Google Products or Features); EEA TOS applicability conditioned on billing account address in the EEA. Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching) — controlling for confirmed non-EEA billing account (AE). Google Maps Platform EEA Terms of Service — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms — Section 20 Routes API — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature |
| Preliminary verdict | **Requires written confirmation** |
| Evidence | **Technical API capability: Confirmed.** Google Routes API officially supports intermediate waypoints for `computeRoutes`, including stopover and pass-through (`via: true`) behavior within documented limits (up to 25 intermediate waypoints per request). **Terms permission for proposed MARSHIO use: Not established.** Written confirmation is required for the proposed systematic multi-request waypoint research pattern in the concrete MARSHIO UX. The reviewed official sources confirm the API capability, but they do not provide sufficiently specific affirmative authorization for the complete proposed MARSHIO multi-request research pattern (multiple separate waypoint-constrained requests → systematic corridor probing → comparison of returned Google routes → derived labels/explanations/scores → concrete display, attribution, and retention). Absence of an identified prohibition is not affirmative permission. **Administrative jurisdiction (resolved, not substantive Terms verdict):** Billing-account jurisdiction confirmed non-EEA — United Arab Emirates (AE). The Google Payments profile linked to the Google Cloud Billing account used by the MARSHIO Google Maps Platform / Routes API project has Country/Region = AE; official Cloud Billing documentation establishes that the linked payments profile uses the same country as the billing account. Jurisdiction uncertainty resolved. Standard Google Maps Platform Terms and standard Service Specific Terms apply to this confirmed non-EEA billing account. This does not establish Terms permission for the proposed systematic multi-request waypoint research pattern. **Billing and API facts (technical, not Terms verdict):** billing and API key required; documented waypoint limits apply; SKU depends on waypoint count and features used; traffic-aware or other advanced features may affect SKU; `computeAlternativeRoutes` does not return alternatives when intermediate waypoints are present — multiple corridor candidates require separate requests. **Cross-references only (not analyzed in F01):** Service Specific Terms §19.3 caching (F11); map presentation and attribution (F04, F05); route comparison (F03); derived labels/explanations/scores (F06–F08); persistence and retention (F10, F11). |
| Open questions | Terms classification cannot be completed without a concrete proposed UX (see `Concrete proposed UX` — TBD). What exact multi-request research volume and frequency are proposed? Does the pattern fall within or conflict with applicable No Scraping restrictions (ToS §3.2.1(a))? Does the pattern fall within or conflict with applicable No Re-Creating restrictions (ToS §3.2.1(d))? What exact Customer Application UX will display and compare the results? Will any route content, coordinates, derived labels, fingerprints, or scores be retained? What attribution and Google Map presentation will be used? Which parts of the proposed pattern require written confirmation from Google Maps Platform support or sales? Cross-feature dependencies remain TBD: F03 (comparing route responses), F04/F05 (map presentation, attribution), F06–F08 (derived labels, explanations, scores), F09–F11 (session processing, persistence, caching), F12 (route fingerprints). |
| Written confirmation required | **Yes** — Terms permission for the proposed systematic multi-request waypoint research pattern is not established from reviewed official sources alone. Written confirmation required for multi-request research volume and pattern under License Restrictions §3.2.1(a) and §3.2.1(d); and any disputed aspects of the concrete MARSHIO UX. Jurisdiction uncertainty is resolved; billing-account jurisdiction is not a remaining reason for written confirmation. Escalation to Google Maps Platform support or sales if internal review cannot resolve open questions. |
| Notes | Billing-account jurisdiction resolved administratively (confirmed non-EEA — AE); jurisdiction uncertainty resolved; F01 was not re-audited substantively. F01 remains **Requires written confirmation** because Terms permission for the proposed systematic multi-request waypoint research pattern is not established. Implementation not authorized. |

### F02 — Standard Google alternatives baseline request (without waypoints)

| Field | Value |
|-------|-------|
| Feature / UX | Standard Google alternatives baseline request (without waypoints) |
| Concrete proposed UX | MARSHIO issues one user-triggered Google Routes `computeRoutes` request for one origin and one destination with `computeAlternativeRoutes` enabled and no intermediate waypoints. The response is used only as the baseline candidate set for a future, separately reviewed decision-assistant research flow. This assessment does not authorize display, comparison, persistence, derived scoring, navigation, waypoint probing, or production implementation. |
| Google inputs | `computeRoutes` (REST: `https://routes.googleapis.com/directions/v2:computeRoutes`). Required: `origin` (Waypoint), `destination` (Waypoint); response field mask via `X-Goog-FieldMask` header, `$fields` URL parameter, or `fields` URL parameter (per computeRoutes reference: this method requires a response field mask). Required for API access: `X-Goog-Api-Key` or OAuth token. Optional: `computeAlternativeRoutes=true`; `travelMode`; `routingPreference`; `departureTime` / `arrivalTime`; `routeModifiers`. Must omit `intermediates[]` (or leave empty). Billing-enabled Google Cloud project. |
| MARSHIO-owned inputs | User-selected or research-specified origin and destination encoded as Waypoint objects; user trigger for the single request; required response field-mask selection specifying which response properties to return (downstream use of returned fields is separately reviewed); optional `travelMode`, `routingPreference`, and departure-time parameters if used. |
| Displayed output | Not assessed in F02 — see F04, F05. |
| Stored? | Not assessed in F02 — see F10. |
| Retention | Not assessed in F02 — see F10, F11. |
| Google attribution | Not assessed in F02 — see F04, F05. |
| Applicable Google documents | Routes API — Get alternative routes (`https://developers.google.com/maps/documentation/routes/alternative-routes`) — accessed 2026-07-13 — `computeAlternativeRoutes` usage; zero-alternatives behavior; example request fields; alternative-route count wording (see Open questions — documentation inconsistency). Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`) — accessed 2026-07-13 — required `origin`/`destination`; required response field mask (`X-Goog-FieldMask`, `$fields`, or `fields`); `computeAlternativeRoutes` boolean; no alternatives when intermediate waypoints present; `routes[]` array wording (see Open questions — documentation inconsistency). Routes API — Usage and billing (`https://developers.google.com/maps/documentation/routes/usage-and-billing`) — accessed 2026-07-13 — billing and API key/OAuth required; per-request SKU by features used; Compute Routes rate limits. Routes API — Policies (`https://developers.google.com/maps/documentation/routes/policies`) — accessed 2026-07-13 — use governed by Agreement; EEA-specific Terms for customers with billing address in the EEA; standard Terms when billing address is not in the EEA. Google Cloud — Modify your Cloud Billing account (`https://cloud.google.com/billing/docs/how-to/modify-billing-account`) — accessed 2026-07-13 — Cloud Billing account linked to Google payments profile at creation; linked payments profile uses the same country as the billing account; mailing addresses stored on associated payments profile. Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`) — accessed 2026-07-13 — §3.1 license grant; §3.2 license restrictions; EEA TOS applies if billing account address is in the EEA. Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`) — accessed 2026-07-13 — §19 Routes API; controlling for confirmed non-EEA billing account. Google Maps Platform EEA Terms of Service (`https://cloud.google.com/terms/maps-platform/eea`) — accessed 2026-07-13 — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — accessed 2026-07-13 — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.1 License Grant; Section 3.2 License Requirements and Restrictions (including 3.2.1 General Restrictions; 3.2.3(a) No Scraping; 3.2.3(d) No Re-Creating Google Products or Features); EEA TOS applicability conditioned on billing account address in the EEA. Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching — cross-reference only, not analyzed in this feature) — controlling for confirmed non-EEA billing account (AE). Google Maps Platform EEA Terms of Service — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms — Section 20 Routes API — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Preliminary verdict | **Allowed with constraints** |
| Evidence | **Technical API capability: Confirmed.** Google Routes API officially supports `computeAlternativeRoutes` on `computeRoutes` when no intermediate waypoints are present. Documented behavior: sometimes no alternatives are available and only the default route is returned; official sources are internally inconsistent on the exact maximum number of routes returned when alternatives are present (see Open questions — documentation inconsistency). F02 does not depend on resolving that maximum. API reference states alternative routes are not returned for requests with intermediate waypoints. Required request fields for this pattern: `origin`, `destination`, and a response field mask (`X-Goog-FieldMask`, `$fields`, or `fields` per computeRoutes reference); `computeAlternativeRoutes=true` is the documented opt-in. **Administrative jurisdiction (resolved, not substantive Terms verdict):** Billing-account jurisdiction confirmed non-EEA — United Arab Emirates (AE). The Google Payments profile linked to the Google Cloud Billing account used by the MARSHIO Google Maps Platform / Routes API project has Country/Region = AE; official Cloud Billing documentation establishes that the linked payments profile uses the same country as the billing account. Jurisdiction uncertainty resolved. Standard Google Maps Platform Terms and standard Service Specific Terms apply to this confirmed non-EEA billing account. **Billing and API facts (technical, not Terms verdict):** billing must be enabled; each request requires an API key or OAuth token; Compute Routes is billed per request; SKU category (Essentials / Pro / Enterprise) depends on features used in the request (for example, `TRAFFIC_AWARE` or `TRAFFIC_AWARE_OPTIMAL` routing preferences); documented rate limit is 3,000 queries per minute for Compute Routes. **Terms permission for issuing the narrow F02 request: Partially established, subject to constraints.** Reviewed official sources establish that `computeRoutes` with `computeAlternativeRoutes` is a documented, intended Routes API capability used under the Google Maps Platform license grant (ToS §3.1) and standard API access requirements. Service Specific Terms §19.1 affirmatively permits Routes API content use in Customer Applications without a corresponding Google Map — relevant to downstream content handling but not a substitute for separate review of display, attribution, storage, or comparison. For the narrow F02 scope (one user-triggered, single, standard alternatives request with no intermediate waypoints), reviewed sources do not identify a prohibition that would forbid the request itself when made through authenticated, billed API access in compliance with §3.2 restrictions. **Terms permission for the broader MARSHIO research pattern: Not established by F02.** API authorization for this single baseline request does not automatically authorize response comparison, ranking, derived labels/explanations/scores, persistence, caching, fingerprints, user studies, map/non-map presentation, navigation, waypoint probing, or production implementation. Absence of an identified prohibition is not affirmative permission for those downstream uses. **Cross-references only (not analyzed in F02):** route comparison (F03); map presentation and attribution (F04, F05); derived labels/explanations/scores (F06–F08); session processing (F09); persistence and retention (F10); caching and lat/lng retention limits (F11); route fingerprints (F12); user studies (F14). **Anti-authorization:** F02 assesses only the feasibility of issuing the baseline request. It does not authorize route display, route comparison, derived labels, derived scores, persistence, caching, waypoint probing, navigation, or production implementation. |
| Open questions | **Documentation inconsistency — alternative route count (unresolved):** Routes API — Get alternative routes (`https://developers.google.com/maps/documentation/routes/alternative-routes`) states that when alternative routes are requested, the API returns up to three routes along with the default route. Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`) states that `routes[]` contains an array of computed routes (up to three) when `computeAlternativeRoutes` is specified. These wordings are internally inconsistent; F02 does not resolve this contradiction and does not depend on the exact maximum route count. What exact `travelMode`, `routingPreference`, departure-time, and field-mask field paths will MARSHIO use for the baseline request, and how do those choices affect SKU classification? What request volume and frequency are proposed beyond this single user-triggered pattern (if any later research expands scope)? Does any later downstream handling of the returned routes fall within or conflict with applicable No Scraping (ToS §3.2.3(a)) or No Re-Creating (ToS §3.2.3(d)) restrictions — Not assessed in F02 — see F03, F06–F08, F10–F12, F14. What display format and Google attribution will apply if routes are shown — Not assessed in F02 — see F04, F05. Will any route content be stored or cached — Not assessed in F02 — see F10, F11. Will MARSHIO compare ETAs, tolls, or rank routes — Not assessed in F02 — see F03, F19, F20. |
| Written confirmation required | **No** — for the narrow F02 scope (one user-triggered `computeRoutes` request with `computeAlternativeRoutes=true` and no intermediate waypoints), reviewed official sources establish licensed API use subject to stated constraints. Escalate to Google Maps Platform support or sales if a downstream feature review raises disputed compliance questions outside F02 scope. |
| Notes | F02 is independent of F01. F01 assessed systematic multi-request waypoint probing; F02 assesses only a single standard alternatives baseline request. F01’s **Requires written confirmation** verdict is previous context only and is not evidence for F02. Billing-account jurisdiction resolved administratively (confirmed non-EEA — AE); jurisdiction uncertainty resolved; F02 was not re-audited substantively. Per the computeRoutes reference, a response field mask is required for every valid `computeRoutes` request; F02 treats it as a required request component, not an optional detail. F02 must not be treated as commit-ready until independent Codex review. F02 assesses only the feasibility of issuing the baseline request. It does not authorize route display, route comparison, derived labels, derived scores, persistence, caching, waypoint probing, navigation, or production implementation. |

### F03 — Comparing multiple Google route responses

| Field | Value |
|-------|-------|
| Feature / UX | Comparing multiple Google route responses |
| Concrete proposed UX | **Terminology scope:** The feature index label uses “responses” (plural). F03 evaluates a narrower pattern: after one user-triggered F02 `computeRoutes` response with `computeAlternativeRoutes=true` and no intermediate waypoints, MARSHIO performs conceptual transient juxtaposition of two or more fully computed Google route objects returned in that single response’s `routes[]` array by transient side-by-side reading of Google-provided properties per the granted field mask (for example `routeLabels`, `duration`, `distanceMeters`) without creating, storing, logging, displaying, or exporting any derived label, derived explanation, derived score, derived ranking, derived recommendation, or any other derived content; without modifying, merging, or re-routing Google geometry; without cross-provider comparison; and without persisting, caching, logging raw responses, or presenting results to users. **Applicability:** F03 applies only when that single F02 response contains at least two returned route objects. If the response contains only the default route, there is no multi-route juxtaposition operation to assess under F03. F03 does not create additional route objects and does not authorize additional API requests. Comparing route objects from multiple separate API responses (for example F01 waypoint-constrained requests) is out of F03 scope. This assessment does not authorize display, derived labels, explanations, scores, ETA/toll claims, fingerprints, user studies, navigation, waypoint probing, or production implementation. |
| Google inputs | One F02-pattern `computeRoutes` response. Inputs to comparison: `routes[]` array of fully computed Route objects from that single response; per-route Google-provided properties returned per field mask (for example `routeLabels` with `DEFAULT_ROUTE` / `DEFAULT_ROUTE_ALTERNATE`, `duration`, `distanceMeters`). Underlying request: origin, destination, `computeAlternativeRoutes=true`, no `intermediates[]`, required response field mask (per F02). |
| MARSHIO-owned inputs | Selection of which Google-provided route properties to read side-by-side; conceptual transient juxtaposition only; no MARSHIO routing-engine inputs; no geometry modification or route merging; no derived content creation. |
| Displayed output | Not assessed in F03 — see F04, F05. |
| Stored? | Not assessed in F03 — see F10. F03 narrow UX assumes no persistence; F10 remains TBD. |
| Retention | Not assessed in F03 — see F10, F11. |
| Google attribution | Not assessed in F03 — see F04, F05. |
| Applicable Google documents | Routes API — Get alternative routes (`https://developers.google.com/maps/documentation/routes/alternative-routes`) — accessed 2026-07-13 — `computeAlternativeRoutes` usage; `routeLabels`; customer route choice using `duration` and `distanceMeters`; zero-alternatives / default-only behavior; route-count wording (unresolved — see F02 Open questions). Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`) — accessed 2026-07-13 — `routes[]` array; `routeLabels` “useful to identify specific properties of the route to compare against others”; `DEFAULT_ROUTE` / `DEFAULT_ROUTE_ALTERNATE`; no alternatives with intermediate waypoints; required response field mask. Routes API — Policies (`https://developers.google.com/maps/documentation/routes/policies`) — accessed 2026-07-13 — use governed by Agreement; EEA-specific Terms for customers with billing address in the EEA; standard Terms when billing address is not in the EEA; attribution requirements (cross-reference only for F03). Google Cloud — Modify your Cloud Billing account (`https://cloud.google.com/billing/docs/how-to/modify-billing-account`) — accessed 2026-07-13 — Cloud Billing account linked to Google payments profile at creation; linked payments profile uses the same country as the billing account; mailing addresses stored on associated payments profile. Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`) — accessed 2026-07-13 — §3.1 license grant; §3.2 license restrictions; EEA TOS applies if billing account address is in the EEA. Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`) — accessed 2026-07-13 — §19 Routes API; controlling for confirmed non-EEA billing account. Google Maps Platform EEA Terms of Service (`https://cloud.google.com/terms/maps-platform/eea`) — accessed 2026-07-13 — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — accessed 2026-07-13 — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.1 License Grant; Section 3.2 License Requirements and Restrictions (including 3.2.3(a) No Scraping; 3.2.3(c) No Creating Content From Google Maps Content; 3.2.3(d) No Re-Creating Google Products or Features); EEA TOS applicability conditioned on billing account address in the EEA. Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching — cross-reference only, not analyzed in this feature) — controlling for confirmed non-EEA billing account (AE). Google Maps Platform EEA Terms of Service — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms — Section 20 Routes API — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Preliminary verdict | **Allowed with constraints** |
| Evidence | **Technical API capability: Conditional.** When `computeAlternativeRoutes=true` and no intermediate waypoints are present, `computeRoutes` may return multiple route objects in a single response’s `routes[]` array. F03 applies only when a single F02 response contains at least two returned route objects. If the response contains only the default route, there is no multi-route juxtaposition operation to assess under F03. A default-only response is a valid normal outcome (consistent with F02). F03 does not create additional route objects and does not authorize additional API requests. When at least two route objects are present, `routeLabels` distinguish `DEFAULT_ROUTE` from `DEFAULT_ROUTE_ALTERNATE`. The Alternative Routes guide documents that customers may use returned properties such as `duration` and `distanceMeters` to choose among routes. The computeRoutes reference states `routeLabels` are useful to identify properties to compare against other routes. The unresolved maximum route-count inconsistency documented in F02 is not resolved here; F03 does not depend on the exact maximum. **Administrative jurisdiction (resolved, not substantive Terms verdict):** Billing-account jurisdiction confirmed non-EEA — United Arab Emirates (AE). The Google Payments profile linked to the Google Cloud Billing account used by the MARSHIO Google Maps Platform / Routes API project has Country/Region = AE; official Cloud Billing documentation establishes that the linked payments profile uses the same country as the billing account. Jurisdiction uncertainty resolved. Standard Google Maps Platform Terms and standard Service Specific Terms apply to this confirmed non-EEA billing account. This administrative resolution does not expand F03 beyond conceptual transient side-by-side reading of Google-provided field values. **Terms permission for the narrow F03 comparison pattern: Partially established, subject to constraints.** Reviewed official sources describe comparing Google-provided route properties within one alternatives response as intended customer-application behavior (route choice), under the Platform license grant (ToS §3.1). Service Specific Terms §19.1 permits Routes API content use in Customer Applications without a corresponding Google Map — relevant to downstream handling but not a substitute for separate display/attribution review. For the narrow F03 scope (conceptual transient juxtaposition of Google-provided field values across at least two route objects from one F02 response, without creating any derived content, without modifying/merging geometry, without own routing engine, without persistence or user-facing presentation), reviewed sources support this as licensed use of returned Google Maps Content, subject to §3.2 restrictions and stated constraints. This is not affirmative permission for derived labels, scores, systematic multi-response comparison, or substitute routing products. **Terms permission for broader MARSHIO research or product pattern: Not established by F03.** F03 does not authorize display, derived labels/explanations/scores, persistence, caching, fingerprints, ETA/toll claims, user studies, navigation, waypoint probing, cross-provider comparison, or production implementation. **F03 vs F09 boundary:** F03 assesses only the conceptual act of transient side-by-side reading of Google-provided field values from two or more route objects returned in one F02 response — without creating, storing, logging, displaying, or exporting any derived label, derived explanation, derived score, derived ranking, derived recommendation, or any other derived content; no second API request within F03. F09 separately assesses the processing lifecycle and technical controls, including session boundaries, memory lifecycle, destruction, logging, telemetry, crash reporting, diagnostics, temporary files, and guarantees against persistence. F03 does not authorize implementation. Any implementation of transient juxtaposition remains blocked until F09 is reviewed and its processing-lifecycle constraints are satisfied. F03 verdict = assessment of the juxtaposition operation itself; F09 verdict = assessment of how that operation is technically executed. **Cross-references only (not analyzed in F03; remain TBD/blocked):** F04 (Google Map presentation); F05 (Non-map presentation); F06 (Derived labels); F07 (Derived explanations); F08 (Derived scores); F09 (Session-only processing); F10 (Persistence); F11 (Caching); F12 (Route fingerprints); F13 (Aggregated metrics); F14 (User studies); F15 (Route geometry); F16 (Steps); F17 (Maneuvers); F18 (Traffic intervals); F19 (ETA comparison); F20 (Toll comparison); F21 (External navigation handoff). **Anti-authorization:** F03 assesses only the narrow juxtaposition pattern defined in its Concrete proposed UX. It does not authorize display, derived labels, explanations, scores, persistence, caching, fingerprints, geometry processing, ETA/toll claims, user studies, navigation, waypoint probing, or production implementation. |
| Open questions | **Terminology — feature index label vs assessed scope:** The index label “Comparing multiple Google route responses” may imply multiple separate API responses; F03 assesses juxtaposition of multiple route objects within one F02 response. Should the feature index label be renamed in a future authorized edit? **Route-count documentation inconsistency (unresolved, inherited from F02):** Alternative Routes guide vs computeRoutes reference wording on maximum routes; F03 does not depend on resolving this. Which exact Google-provided route properties will MARSHIO read side-by-side (beyond documented examples such as `routeLabels`, `duration`, `distanceMeters`)? Do any proposed juxtaposed fields (for example toll-related or traffic-interval fields) require separate F18–F20 review? **F09 dependency (blocking for implementation):** F03 assesses the juxtaposition operation only; F09 must separately assess processing lifecycle and technical controls before any implementation of transient juxtaposition is permitted — Not assessed in F03 — see F09. Will any juxtaposed content be stored, cached, fingerprinted, displayed, or labeled — Not assessed in F03 — see F04, F05, F06–F08, F10–F12. Would juxtaposing route objects from multiple separate API responses (for example F01 waypoint-constrained requests) require a separate feature assessment outside this F03 scope? |
| Written confirmation required | **No** — for the narrow F03 scope (conceptual transient juxtaposition of Google-provided field values across route objects from one F02 alternatives response, without creating any derived content, without geometry modification, persistence, or user-facing presentation), reviewed official sources establish permitted juxtaposition subject to stated constraints. Escalate to Google Maps Platform support or sales if juxtaposition of specific route properties raises disputed compliance questions outside F03 scope. |
| Notes | F03 is independent of F01 and F02 substance. F01’s **Requires written confirmation** and F02’s **Allowed with constraints** verdicts are previous context only, not evidence for F03. F02 explicitly deferred route comparison to F03; F03 does not retroactively authorize F02 downstream uses. The feature index label is unchanged pending separate authorization. Billing-account jurisdiction resolved administratively (confirmed non-EEA — AE); jurisdiction uncertainty resolved; F03 was not re-audited substantively. This administrative resolution does not expand F03 beyond conceptual transient side-by-side reading of Google-provided field values. **F03 vs F09:** F03 verdict assesses the conceptual transient juxtaposition operation only; F09 must assess processing lifecycle before implementation. F03 does not authorize implementation; any implementation of transient juxtaposition remains blocked until F09 is reviewed. F03 must not be treated as commit-ready until independent Codex review. F03 assesses only the narrow juxtaposition pattern defined in its Concrete proposed UX. It does not authorize display, derived labels, explanations, scores, persistence, caching, fingerprints, geometry processing, ETA/toll claims, user studies, navigation, waypoint probing, or production implementation. |

### F04 — Google Map presentation

| Field | Value |
|-------|-------|
| Feature / UX | Google Map presentation |
| Concrete proposed UX | MARSHIO displays the unmodified Google-provided polyline of the Route object explicitly identified by `routes[].routeLabels` as `DEFAULT_ROUTE` from one F02 `computeRoutes` response on a Google Map rendered by the official Google Maps SDK for Android inside the Customer Application. **Identification:** F04 applies only when the F02 response contains a route object whose `routeLabels` includes `DEFAULT_ROUTE`; if no such route object exists, no F04 presentation occurs. F04 does not assess or authorize selection of an alternative route (`DEFAULT_ROUTE_ALTERNATE` or any other label). No fallback route-selection rule. Presentation is limited to that `DEFAULT_ROUTE` polyline overlaid on the live Google Map with required Google attribution preserved and visible. No modification of route point order; no geometry reconstruction, merging, simplification, or custom route generation; no cross-provider content; no simultaneous multiple-route presentation; no route comparison, alternative-route selection, ranking, or recommendation signaling; no persistence; no caching; no screenshots; no exports; no offline presentation; no navigation; no production implementation. |
| Google inputs | One F02-pattern `computeRoutes` response. Required Google-provided fields for F04 identification and presentation: `routes[].routeLabels` (must include `DEFAULT_ROUTE` for the presented route object) and the applicable route polyline field (for example `routes.polyline.encodedPolyline` per field mask). Encoded polyline using the documented Encoded Polyline Algorithm Format unless another officially requested encoding is used. Underlying F02 request parameters (origin, destination, `computeAlternativeRoutes` as applicable, no `intermediates[]` for this narrow UX, required response field mask including `routes.routeLabels` and applicable polyline field). Google Map base map tiles and SDK-rendered attribution from the official Google Maps SDK for Android. |
| MARSHIO-owned inputs | Identification of the Route object whose `routes[].routeLabels` contains `DEFAULT_ROUTE` (no inference from array position, sole returned route, duration, distance, or visual preference); decode-and-render step to draw that Google-provided polyline on the Google Map without changing point order or geometry; neutral Maps SDK `Polyline` rendering properties required for display (for example width/color) that do not signal preference, ranking, or recommendation; Customer Application map viewport/camera framing only. |
| Displayed output | One unmodified Google-provided `DEFAULT_ROUTE` polyline from one F02 response, overlaid on a live Google Map rendered by the official Google Maps SDK for Android, with Google Maps attribution visible on the map. |
| Stored? | Not authorized by F04 — Not assessed in F04 — see F10. F04 narrow UX assumes no persistence. |
| Retention | Not authorized by F04 — Not assessed in F04 — see F10, F11. |
| Google attribution | Required for F04 narrow UX: Google Maps attribution must not be removed, altered, hidden, or obscured; it must remain visible and legible within the applicable visual container. Positioning must follow the applicable Google attribution guidance for the selected SDK and visual container. When Routes API content is shown on a Google Map where Google Maps attribution is already visible, official policies state extra attribution is not required. Included SDK attribution must remain visible; Google Maps attribution must be clearly associated with Google Maps Content. Logo/text attribution, accessibility, contrast, and clear-space requirements apply when additional attribution is necessary outside SDK-rendered map attribution. If third-party data-provider attribution is supplied by Google for the presented content, it must be included as required. F04 does not authorize screenshots, exports, offline presentation, or attribution-free presentation. |
| Applicable Google documents | Routes API — Get alternative routes (`https://developers.google.com/maps/documentation/routes/alternative-routes`) — accessed 2026-07-13 — include `routes.polyline` in field mask; display polyline on a map. Routes API — Request route polylines (`https://developers.google.com/maps/documentation/routes/traffic_on_polylines`) — accessed 2026-07-13 — `routes.polyline` / `routes.polyline.encodedPolyline`; encoded polyline format; route-level polyline; rendering polylines with Maps SDK. Routes API — Choose fields to return (`https://developers.google.com/maps/documentation/routes/choose_fields`) — accessed 2026-07-13 — required response field mask; `routes.polyline.encodedPolyline` example. Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`) — accessed 2026-07-13 — `polyline` object; `encodedPolyline`; `routeToken` for Navigation SDK only (not required for narrow F04 display). Routes API — Policies (`https://developers.google.com/maps/documentation/routes/policies`) — accessed 2026-07-13 — Routes API results displayed on a map must be shown on a Google Map; attribution requirements; EEA-specific Terms for customers with billing address in the EEA; standard Terms when billing address is not in the EEA. Google Cloud — Modify your Cloud Billing account (`https://cloud.google.com/billing/docs/how-to/modify-billing-account`) — accessed 2026-07-13 — Cloud Billing account linked to Google payments profile at creation; linked payments profile uses the same country as the billing account; mailing addresses stored on associated payments profile. Maps SDK for Android — Shapes (`https://developers.google.com/maps/documentation/android-sdk/shapes`) — accessed 2026-07-13 — `Polyline` / `PolylineOptions`; add polyline to `GoogleMap`. Maps SDK for Android — Policies and attributions (`https://developers.google.com/maps/documentation/android-sdk/policies`) — accessed 2026-07-13 — attribution on Google Map; included SDK attribution; visibility and modification prohibitions. Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`) — accessed 2026-07-13 — §3.1 license grant; §3.2 restrictions including §3.2.3(e) No Use With Non-Google Maps; EEA TOS applies if billing account address is in the EEA. Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`) — accessed 2026-07-13 — §19 Routes API; controlling for confirmed non-EEA billing account. Google Maps Platform EEA Terms of Service (`https://cloud.google.com/terms/maps-platform/eea`) — accessed 2026-07-13 — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — accessed 2026-07-13 — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.1 License Grant; Section 3.2 License Requirements and Restrictions (including 3.2.3(a) No Scraping; 3.2.3(b) No Caching; 3.2.3(c) No Creating Content From Google Maps Content; 3.2.3(d) No Re-Creating Google Products or Features; 3.2.3(e) No Use With Non-Google Maps); EEA TOS applicability conditioned on billing account address in the EEA. Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching — cross-reference only, not analyzed in this feature) — controlling for confirmed non-EEA billing account (AE). Google Maps Platform EEA Terms of Service — applies only if billing account address is in the EEA (not applicable to confirmed non-EEA billing account). Google Maps Platform EEA Service Specific Terms — Section 20 Routes API — not controlling for confirmed non-EEA billing account — cross-reference only, not analyzed in this feature. |
| Preliminary verdict | **Allowed with constraints** |
| Evidence | **Technical API capability: Confirmed.** Routes API can return a route polyline via response field mask (for example `routes.polyline` / `routes.polyline.encodedPolyline`) and route identity via `routes.routeLabels` (including `DEFAULT_ROUTE`). Official Routes documentation describes displaying returned polylines on a map. The official Google Maps SDK for Android supports drawing a `Polyline` on a `GoogleMap` from an ordered sequence of `LatLng` points. For the narrow F04 pattern, Navigation SDK and `routeToken` are not required; `routeToken` is documented for Navigation SDK reconstruction, not for basic polyline overlay display. Displaying a route polyline on a map is distinct from turn-by-turn navigation. **Administrative jurisdiction (resolved, not substantive Terms verdict):** Billing-account jurisdiction confirmed non-EEA — United Arab Emirates (AE). The Google Payments profile linked to the Google Cloud Billing account used by the MARSHIO Google Maps Platform / Routes API project has Country/Region = AE; official Cloud Billing documentation establishes that the linked payments profile uses the same country as the billing account. Jurisdiction uncertainty resolved. Standard Google Maps Platform Terms and standard Service Specific Terms apply to this confirmed non-EEA billing account. This administrative resolution does not authorize route selection, comparison, derived content, persistence, caching, screenshots, exports, navigation, implementation, or production deployment. **Terms permission for the narrow F04 presentation concept: Partially established, subject to constraints.** Routes API policies affirmatively define Google Map presentation as the required map context when Routes API results are displayed on a map. Service Specific Terms §19.2 separately prohibit use of Routes API content with a non-Google map and are consistent with that map-context requirement; this prohibition is a separate restriction, not the affirmative basis for F04 permission. Service Specific Terms §19.1 permits Routes API content in Customer Applications without a corresponding Google Map, but F04 evaluates only Google Map presentation and does not authorize non-map presentation (see F05). Official Routes and Maps SDK policies require Google Maps attribution; when content is shown on a Google Map where attribution is already visible, extra attribution is not required, but attribution must not be removed, altered, hidden, or obscured and must remain visible and legible. For the narrow F04 scope (one unmodified Google-provided `DEFAULT_ROUTE` polyline explicitly identified via `routes.routeLabels` from one F02 response on official Google Maps SDK for Android, with attribution preserved per applicable guidance, without persistence, caching, screenshots, exports, offline use, navigation, alternative-route selection, or preference-signaling presentation), reviewed sources support this as a documented presentation model under the Platform license grant (ToS §3.1), subject to stated constraints. **Terms permission for broader MARSHIO research or product pattern: Not established by F04.** F04 does not authorize route comparison, multiple-route presentation, alternative-route selection, derived labels/explanations/scores, rankings, recommendations, persistence, caching, fingerprints, ETA/toll claims, user studies, screenshots, exports, offline use, navigation, waypoint research, cross-provider content, implementation, or production deployment. **Implementation dependencies:** F04’s preliminary verdict assesses only the narrow presentation concept. It does not authorize implementation. Implementation remains blocked until F09, F15, and every other applicable dependency have been separately reviewed and all required constraints are satisfied. **Cross-references only (not analyzed in F04; remain TBD/blocked):** F03 (route comparison); F05 (Non-map presentation); F06 (Derived labels); F07 (Derived explanations); F08 (Derived scores); F09 (Session-only processing); F10 (Persistence); F11 (Caching); F12 (Route fingerprints); F13 (Aggregated metrics); F14 (User studies); F15 (Route geometry); F16 (Steps); F17 (Maneuvers); F18 (Traffic intervals); F19 (ETA comparison); F20 (Toll comparison); F21 (External navigation handoff). **Anti-authorization:** F04 assesses only presentation of one unmodified Google-provided `DEFAULT_ROUTE` polyline explicitly identified via `routes.routeLabels` from one F02 response on a Google Map rendered by the official Google Maps SDK for Android. F04 does not authorize route comparison, multiple-route presentation, alternative-route selection, derived labels, explanations, scores, rankings, recommendations, persistence, caching, fingerprints, geometry modification, ETA/toll claims, user studies, screenshots, exports, offline use, navigation, waypoint research, cross-provider content, implementation, or production deployment. |
| Open questions | Does the F02 response field mask always include both `routes.routeLabels` and the applicable `routes.polyline` field for the `DEFAULT_ROUTE` object, and which `polylineQuality` / `polylineEncoding` values will be used? Do any proposed polyline rendering properties beyond neutral display (for example traffic-interval coloring or preference-signaling styling) require separate F15 or F18 review? Does SDK-rendered Google Map attribution remain sufficient for the concrete Customer Application layout, or is additional attribution required for any presented Routes API content? Can standard attribution requirements be met on target devices without screenshots/exports/offline presentation? What happens when the F02 response contains no route object marked `DEFAULT_ROUTE` — F04 presentation does not occur; no fallback selection rule. **F09 dependency (blocking for implementation):** F04 assesses presentation concept only; session lifecycle and technical controls remain blocked until F09 review — Not assessed in F04 — see F09. **F15 dependency (blocking for implementation details):** polyline decode/process details beyond the narrow unmodified-display concept may require F15 review — Not assessed in F04 — see F15. Will any presented content be stored, cached, compared, labeled, scored, or used for navigation — Not assessed in F04 — see F03, F06–F08, F10–F12, F19–F21. |
| Written confirmation required | **No** — for the narrow F04 scope (one unmodified Google-provided `DEFAULT_ROUTE` polyline explicitly identified via `routes.routeLabels` from one F02 response on official Google Maps SDK for Android with required attribution preserved per applicable guidance), reviewed official sources establish permitted Google Map presentation subject to stated constraints. Escalate to Google Maps Platform support or sales if standard attribution display is not feasible on target devices (per Routes policies licensing note) or if concrete rendering choices raise disputed compliance questions outside F04 scope. |
| Notes | F04 is independent of F01, F02, and F03 substance. Prior feature verdicts are context only, not evidence for F04. Billing-account jurisdiction resolved administratively (confirmed non-EEA — AE); jurisdiction uncertainty resolved; F04 was not re-audited substantively. This administrative resolution does not authorize route selection, comparison, derived content, persistence, caching, screenshots, exports, navigation, implementation, or production deployment. F04 must not be treated as commit-ready until independent Codex review. F04 assesses only presentation of one unmodified Google-provided `DEFAULT_ROUTE` polyline explicitly identified via `routes.routeLabels` from one F02 response on a Google Map rendered by the official Google Maps SDK for Android. F04 does not assess or authorize alternative-route selection. F04 does not authorize route comparison, multiple-route presentation, alternative-route selection, derived labels, explanations, scores, rankings, recommendations, persistence, caching, fingerprints, geometry modification, ETA/toll claims, user studies, screenshots, exports, offline use, navigation, waypoint research, cross-provider content, implementation, or production deployment. F04’s preliminary verdict assesses only the narrow presentation concept; implementation remains blocked until F09, F15, and other applicable dependencies are reviewed. |

### F05 — Non-map presentation

| Field | Value |
|-------|-------|
| Feature / UX | Non-map presentation |
| Concrete proposed UX | MARSHIO displays the exact Google-provided localized textual values `routes[].localizedValues.distance` and `routes[].localizedValues.duration` (the unmodified `LocalizedText.text` values returned within those fields) for the Route object explicitly identified by `routes[].routeLabels` containing `DEFAULT_ROUTE` from one F02 `computeRoutes` response, without displaying any Google Map. **Identification:** F05 applies only when the F02 response contains a route object whose `routeLabels` includes `DEFAULT_ROUTE`; if no such route object exists, no F05 presentation occurs. F05 does not assess or authorize selection of an alternative route (`DEFAULT_ROUTE_ALTERNATE` or any other label). No inference of `DEFAULT_ROUTE` from array position, route count, duration, distance, visual preference, or any MARSHIO rule. No fallback route-selection rule. If either localized value is absent, F05 must not invent, reconstruct, estimate, or derive it — no F05 presentation occurs for that missing value. The two values are presented without a Google Map, without comparison to another route or provider, and without transformation into a score, ranking, recommendation, explanation, stability claim, or statement of absolute truth. F05 does not label either value as better, worse, faster, shorter, optimal, safer, simpler, or more stable. No map substitute (polyline, static map, route diagram, geometry visualization, turn list, step list, maneuver list, traffic visualization, corridor reconstruction, or route reconstruction). No persistence; no caching; no screenshots; no exports; no offline presentation; no navigation; no production implementation. |
| Google inputs | One F02-pattern `computeRoutes` response. Required response field mask: `routes.routeLabels`, `routes.localizedValues.distance`, `routes.localizedValues.duration`. Required Google-provided fields for F05 identification and presentation: `routes[].routeLabels` (must include `DEFAULT_ROUTE` for the presented route object); `routes[].localizedValues.distance` (`LocalizedText` — travel distance in text form); `routes[].localizedValues.duration` (`LocalizedText` — duration in text form, localized to the region of the query, traffic-aware when traffic information was requested). Underlying F02 request parameters (origin, destination, `computeAlternativeRoutes` as applicable, no `intermediates[]` for this narrow UX). Optional request parameters affecting localized text: `languageCode`, `units` (if omitted, API infers language and units from origin waypoint for `computeRoutes`). |
| MARSHIO-owned inputs | Identification of the Route object whose `routes[].routeLabels` contains `DEFAULT_ROUTE` (no inference from array position, sole returned route, duration, distance, or visual preference); extraction and display of the unmodified `LocalizedText.text` values from the requested `localizedValues.distance` and `localizedValues.duration` fields only; Customer Application visual container framing for the two Google-provided text values and required Google attribution/copyright. Any UI headings or contextual text that explain what the values mean are MARSHIO-owned and are not authorized by F05 — Not assessed in F05 — see F06 and F07. |
| Displayed output | Up to two unmodified Google-provided localized text strings from one `DEFAULT_ROUTE` object in one F02 response: the `text` value from `routes[].localizedValues.distance` and the `text` value from `routes[].localizedValues.duration`, presented without a Google Map, with required Google Maps attribution and copyright statement visible per applicable guidance. |
| Stored? | Not authorized by F05 — Not assessed in F05 — see F10. F05 narrow UX assumes no persistence. |
| Retention | Not authorized by F05 — Not assessed in F05 — see F10, F11. |
| Google attribution | **Required for F05 narrow UX** — F05 has no Google Map and no SDK-rendered map attribution; official Routes policies state attribution (including the Google logo) is required when Routes API content is not displayed on a Google Map. Customer must follow Google Maps attribution requirements: prefer Google Maps logo attribution; text attribution spelling `Google Maps` is acceptable only where logo space is insufficient; do not modify, hide, obscure, or remove attribution; maintain legibility, contrast, and accessibility (logo accessibility label: `Google Maps`); follow logo size (16–19dp height), clear-space, and text-styling requirements (Roboto 400, 12–16sp, specified colors); the HTML attribute `translate="no"` applies only to HTML/browser text attribution and is not applicable to the native Android presentation assessed by F05; position attribution near the top or bottom of the content within the same visual container; visually distinguish Google Maps Platform Content from other content. If Google supplies third-party data-provider attribution for the presented content, include it as required. Official Routes documentation also requires the copyright statement `Powered by Google, ©YEAR Google` when displaying results to users. Platform ToS §3.2.2(b) requires displaying all attribution Google provides through the Services (including branding, logos, and copyright and trademark notices) and attribution specified in Maps Service Specific Terms; Customer must not modify, obscure, or delete such attribution. F05 does not authorize attribution-free presentation. Exact attribution placement within a concrete Customer Application layout is an implementation dependency until UI is defined. |
| Applicable Google documents | Routes API — Policies and attributions (`https://developers.google.com/maps/documentation/routes/policies`) — accessed 2026-07-13 — Customer Application publicly accessible Terms of Use and Privacy Policy requirements; Routes API results on a map must be on a Google Map; attribution required when not on a Google Map; Google Maps logo/text attribution, sizing, clear-space, contrast, accessibility, visual-container placement, and third-party provider rules; EEA applicability for customers with a billing address in the EEA, standard Terms if billing address is not in the EEA; contact Google sales if standard attribution display is not feasible. Google Cloud — Modify your Cloud Billing account (`https://cloud.google.com/billing/docs/how-to/modify-billing-account`) — accessed 2026-07-13 — Cloud Billing account linked to Google payments profile at creation; linked payments profile uses the same country as the billing account; mailing addresses for billing account stored on associated payments profile; billing-account country cannot be changed after creation. Routes API — Request localized values (`https://developers.google.com/maps/documentation/routes/localized-values`) — accessed 2026-07-13 — `routes.localizedValues` field mask; `distance` and `duration` localized text; optional `languageCode` and `units`; inference from origin when omitted. Routes API — Understand route responses (`https://developers.google.com/maps/documentation/routes/understand-route-response`) — accessed 2026-07-13 — `DEFAULT_ROUTE` / `DEFAULT_ROUTE_ALTERNATE` labels; localized response values; copyright statement `Powered by Google, ©YEAR Google` when displaying results to users. Routes API — Choose fields to return (`https://developers.google.com/maps/documentation/routes/choose_fields`) — accessed 2026-07-13 — response field mask required; field-path construction for `routes.localizedValues.distance` and `routes.localizedValues.duration`. Routes API — Method: computeRoutes (`https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes`) — accessed 2026-07-13 — `Route.routeLabels`; `RouteLocalizedValues` (`distance`, `duration` as `LocalizedText`); `routeToken` for Navigation SDK only (not required for narrow F05 textual presentation). Google Maps Platform Terms of Service (`https://cloud.google.com/maps-platform/terms`) — accessed 2026-07-13 — §3.1 license grant; §3.2.2(a) Customer Application Terms of Service and Privacy Policy; §3.2.2(b) Attribution; EEA TOS applies if billing account address is in the EEA. Google Maps Platform Service Specific Terms (`https://cloud.google.com/maps-platform/terms/maps-service-terms`) — accessed 2026-07-13 — §19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching — cross-reference only, not analyzed in this feature). Google Maps Platform EEA Terms of Service (`https://cloud.google.com/terms/maps-platform/eea`) — accessed 2026-07-13 — governs only if Customer's billing account address is in the EEA. Google Maps Platform EEA Service Specific Terms (`https://cloud.google.com/terms/maps-platform/eea/maps-service-terms`) — accessed 2026-07-13 — Section 17 concerns Pollen API (not applicable to F05); Section 20 Routes API (20.1 No Use With any Map for description or steps; 20.2 Caching — cross-reference only, not analyzed in this feature); current EEA Routes provisions reviewed during F05 do not contain an affirmative clause equivalent to standard §19.1 for this exact F05 pattern; not controlling for administratively confirmed non-EEA billing account. |
| Relevant Terms sections | Google Maps Platform Terms of Service — Section 3.1 License Grant; Section 3.2 License Requirements and Restrictions (including 3.2.2(a) Terms of Service and Privacy Policy; 3.2.2(b) Attribution; 3.2.3(a) No Scraping; 3.2.3(b) No Caching; 3.2.3(c) No Creating Content From Google Maps Content; 3.2.3(d) No Re-Creating Google Products or Features); EEA TOS applicability conditioned on billing account address in the EEA. Google Maps Platform Service Specific Terms — Section 19 Routes API (19.1 Use without a Google Map; 19.2 No use with a non-Google map; 19.3 Caching — cross-reference only, not analyzed in this feature) — controlling for administratively confirmed non-EEA billing account. Google Maps Platform EEA Terms of Service — applies only if billing account address is in the EEA (not applicable to confirmed AE billing account). Google Maps Platform EEA Service Specific Terms — Section 17 Pollen API (not applicable to F05); Section 20 Routes API (20.1 No Use With any Map for description or steps; 20.2 Caching — cross-reference only, not analyzed in this feature); not controlling for confirmed non-EEA billing account; EEA Routes provisions reviewed during F05 do not contain an affirmative clause equivalent to standard §19.1 for this exact F05 pattern. |
| Preliminary verdict | **Allowed with constraints** |
| Evidence | **Technical API capability: Confirmed.** `computeRoutes` returns `routes[].localizedValues.distance` and `routes[].localizedValues.duration` as `LocalizedText` objects (travel distance and duration represented in localized text form) when requested through the response field mask. Field mask is required for any response fields; sub-paths `routes.localizedValues.distance` and `routes.localizedValues.duration` are valid per field-mask path rules. `languageCode` and `units` are optional request parameters; if omitted, the API infers language and units from the origin waypoint for `computeRoutes`. These fields are presentation-ready localized textual Google-provided values and do not require geometry decoding, Maps SDK, or Navigation SDK. `routeToken` is documented for Navigation SDK reconstruction only and is irrelevant to this narrow textual presentation. If `DEFAULT_ROUTE` is absent from `routeLabels`, or either localized value is absent, F05 presentation does not occur; MARSHIO must not invent or derive missing values. **Terms permission — controlling non-EEA outcome.** For the administratively confirmed non-EEA billing account, standard Service Specific Terms §19.1 affirmatively states: *"Customer may use Google Maps Content from the Routes API in Customer Applications without a corresponding Google Map."* This permission applies to the narrow Google-provided textual `localizedValues.distance` and `localizedValues.duration` content from Routes API, subject to all recorded attribution, Customer Application, scope, and implementation constraints. Standard §19.2 separately prohibits use of Routes API content with a non-Google map; F05 presents no map at all and does not trigger that conjunction restriction. **Administrative jurisdiction confirmed.** The Google Payments profile linked to the Google Cloud Billing account used by the MARSHIO Google Maps Platform / Routes API project has Country/Region = United Arab Emirates (AE). Official Cloud Billing documentation establishes that each Cloud Billing account is linked to a Google payments profile at creation and that the linked payments profile uses the same country as the billing account; mailing addresses for the billing account are stored on the associated payments profile. Official Routes policies and Google Maps Platform Terms establish that EEA-specific Terms apply to customers with a billing address in the EEA, and that standard non-EEA Terms apply when the billing address is not in the EEA; EEA Terms of Service apply when Customer's billing account address is in the EEA. AE is outside the EEA; the applicable billing-account country/address places this account outside the EEA Service Specific Terms applicability condition. The standard non-EEA Google Maps Platform Service Specific Terms apply to this billing account. F05 therefore relies on standard Routes API §19.1. **F05 is Allowed with constraints** for the confirmed non-EEA billing account under standard Service Specific Terms §19.1, subject to every previously recorded attribution, Customer Application, scope, lifecycle, anti-expansion, and implementation constraint. **EEA (historical, not controlling):** During the original F05 review, current EEA Service Specific Terms (Routes API under Section 20) did not contain an affirmative clause equivalent to standard §19.1 for this exact F05 pattern; that finding is preserved but is not controlling for the confirmed non-EEA billing account. No permission is inferred from silence. **Attribution and Customer Application constraints: Mandatory.** Because F05 has no Google Map, Routes policies require Google Maps attribution (logo preferred; `Google Maps` text where space-limited) with visibility, legibility, placement, and styling requirements. Understand-route-response requires the copyright statement `Powered by Google, ©YEAR Google` when displaying results to users. Platform ToS §3.2.2(a) requires the Customer Application to have publicly accessible terms of service and privacy policy notifying users that the application includes Google Maps features and content and flowing down Google Maps End User Terms and Google Privacy Policy. Platform ToS §3.2.2(b) requires displaying attribution Google provides through the Services. **Meaning of displayed values: Display only.** Official field definitions describe `distance` as travel distance in text form and `duration` as duration in text form localized to the query region (traffic-aware when traffic was requested). F05 authorizes displaying those exact Google-provided text values only; it does not authorize comparing ETAs, claiming objective truth, ranking routes, stability claims, shortest-route claims, or recommendations — Not authorized by F05 — Not assessed in F05 — see F19, F08, F03. **No derived content.** F05 does not authorize derived labels, explanations, scores, rankings, comparisons, normalized/converted values, or contextual headings — Not authorized by F05 — Not assessed in F05 — see F06, F07, F08, F03. **Implementation dependencies:** F05’s preliminary verdict assesses only the narrow non-map presentation concept. It does not authorize implementation. Implementation remains blocked until F06, F07, F09, and every other applicable dependency have been separately reviewed and all required constraints have been satisfied. **Cross-references only (not analyzed in F05; remain TBD/blocked):** F01 (waypoint-constrained requests); F03 (route comparison); F04 (Google Map presentation); F06 (Derived labels); F07 (Derived explanations); F08 (Derived scores, ranking, recommendations); F09 (Session-only processing); F10 (Persistence); F11 (Caching); F12 (Route fingerprints); F13 (Aggregated metrics); F14 (User studies); F15 (Route geometry); F16 (Steps); F17 (Maneuvers); F18 (Traffic intervals); F19 (ETA comparison and ETA claims); F20 (Toll comparison and toll claims); F21 (External navigation handoff). **Anti-expansion:** F05 assesses only non-map presentation of the exact Google-provided localized distance and localized duration values for the Route object explicitly identified by Google as `DEFAULT_ROUTE` in one F02 response. F05 does not authorize route comparison, alternative-route selection, derived labels, explanations, scores, rankings, recommendations, persistence, caching, fingerprints, aggregated metrics, geometry, steps, maneuvers, traffic intervals, ETA claims, toll claims, user studies, screenshots, exports, offline use, navigation, waypoint research, cross-provider content, implementation, or production deployment. |
| Open questions | Can the concrete Customer Application layout satisfy logo/text attribution, copyright statement (`Powered by Google, ©YEAR Google`), visual-container placement, contrast, and accessibility requirements for a non-map textual block without screenshots/exports/offline presentation? If standard attribution display is not feasible on target devices, does Google sales licensing apply per Routes policies? What exact `languageCode` and `units` request values will MARSHIO use, and how do those choices affect the returned localized text? What happens when the F02 response contains no route object marked `DEFAULT_ROUTE` — F05 presentation does not occur; no fallback selection rule. What happens when `localizedValues.distance` or `localizedValues.duration` is absent — F05 presentation does not occur for the missing value; no derivation permitted. **UI context dependency:** Any headings, labels, or explanatory text needed to make the two values understandable are not authorized by F05 — Not assessed in F05 — see F06 and F07. **F09 dependency (blocking for implementation):** field lifecycle, temporary processing, logging, telemetry, crash reporting, and destruction remain blocked until F09 review — Not assessed in F05 — see F09. Will any presented content be stored, cached, compared, scored, or used for navigation — Not assessed in F05 — see F03, F08, F10–F12, F19–F21. **Historical note (not controlling):** During the original F05 review, equivalent EEA affirmative non-map permission for this exact F05 pattern was not established under EEA §20; that finding remains historical context only for the confirmed non-EEA billing account. |
| Written confirmation required | **No** — for the narrow F05 pattern under standard non-EEA Service Specific Terms §19.1, subject to all recorded constraints. Escalate to Google Maps Platform support or sales if standard attribution display is not feasible on target devices (per Routes policies licensing note) or if concrete presentation choices raise disputed compliance questions outside F05 scope. |
| Notes | F05 is independent of F01, F02, F03, and F04 substance. Prior feature verdicts are context only, not evidence for F05. F03 route comparison and F04 Google Map presentation are not re-evaluated or authorized by F05. F01 waypoint-constrained requests are not authorized by F05. Billing-account jurisdiction was resolved administratively (Payments profile Country/Region = AE); F05 was not re-audited substantively. The prior non-EEA conditional outcome under standard §19.1 is now controlling; every existing F05 constraint remains unchanged; implementation remains not authorized. F05 must not be treated as commit-ready until independent Codex review of this administrative update. F05 assesses only non-map presentation of the exact Google-provided localized distance and localized duration values for the Route object explicitly identified by Google as `DEFAULT_ROUTE` in one F02 response. F05 does not authorize route comparison, alternative-route selection, derived labels, explanations, scores, rankings, recommendations, persistence, caching, fingerprints, aggregated metrics, geometry, steps, maneuvers, traffic intervals, ETA claims, toll claims, user studies, screenshots, exports, offline use, navigation, waypoint research, cross-provider content, implementation, or production deployment. F05’s preliminary verdict assesses only the narrow presentation concept; implementation remains blocked until F06, F07, F09, and other applicable dependencies are reviewed. |

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
