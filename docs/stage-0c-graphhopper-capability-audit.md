MARSHIO Stage 0C GraphHopper Constrained Routing Capability Audit (Revised)
Revision authority: Product Owner — AUTHORIZED FOR DOCUMENT REVISION ONLY
Revision date: 2026-07-21
Mode: Report-only. No repository files created, modified, moved, or deleted. No Git operations.

Stage 0B: Completed, immutable. Stage A / production: Unchanged. This document does not authorize a live compatibility probe, Stage 0C implementation, or production work.

Evidence: Prior audit revisions; read-only repository evidence cited therein; official GraphHopper API, pricing, and Terms documentation (access date 2026-07-21). No live API requests. No credentials inspected or exposed.

1. Executive Result
GraphHopper Cloud documents constrained routing via POST /route with ch.disable: true, inline custom_model (speed, priority, distance_influence, request-defined areas), optional algorithm=alternative_route, multi-point points, and optional details (e.g. street_ref, toll, road_class).

Stage 0C soft corridor preference (proposed mechanism in this audit): reducing priority outside the candidate corridor area. That is a geographical heuristic; it is not road-reference identification; it does not establish that the corridor is E311; it depends on the accuracy of the candidate polygon; it may affect nearby and parallel roads; it may penalize required entry and exit segments; it requires a separate corridor-verification method.

Concept	Meaning
Road-reference preference
Directly targeting a road identity such as E311
Geographical approximation
Reducing priority outside a defined candidate corridor area
Forced corridor
Requiring passage through one or more via points
These mechanisms are not equivalent.

Per-request inline custom_model: Current official documentation describes per-request inline custom_model as directly usable with the POST Route endpoint. No published Premium-only requirement was identified for this per-request mechanism. The current account’s key status, plan, remaining quota, applicable routing-location limit, and permission for the intended research use remain unconfirmed. Absence of an identified restriction is not proof of unrestricted availability.

Named custom profiles (cp_) via /profiles: Available starting with the Premium package. Geographic coverage depends on the selected package. Separate from per-request inline models.

Alternative routes + constraints: Documented, but not experimentally verified for the current account and exact Stage 0C request shape.

Storage: Terms allow temporary client-side caching; redistribution requires applicable agreement; reviewed Terms do not grant explicit research-archive permission for raw responses and polylines. STORAGE TERMS UNCONFIRMED if a future protocol retains provider content beyond permitted temporary handling.

Gate 1: GATE 1 INCONCLUSIVE

Pending: (1) one minimal live compatibility probe; (2) a defined corridor-verification method. Account operational readiness is required before the probe; account uncertainty is not evidence of a Premium-only inline-model restriction. Storage clarification applies only if a future protocol retains provider responses, geometries, or other provider content beyond permitted temporary handling.

INCONCLUSIVE does not establish that GraphHopper is unsuitable; does not establish that GraphHopper works for MARSHIO. No live probe, Stage 0C implementation, or production work is authorized.

2. Repository Evidence
Stage 0B: docs/stage-0b-multi-provider-completion.md — GraphHopper: profile=car, two points, algorithm=alternative_route, alternative_route.max_paths=3, no custom_model, no areas, no vias, no traffic integration.

Test provider (GraphHopperLiveBenchmarkProvider.kt): POST https://graphhopper.com/api/1/route; car; two points; alternative_route; max_paths=3; calc_points, points_encoded, instructions=true; no ch.disable, no custom_model, no details.

Parser (GraphHopperResponseParser.kt): distance, time, polyline, instruction text / street_name only.

Corridor labeling (CorridorClassifier.kt): text/heuristic — insufficient for ref-level audit without a defined verification method.

Keys (BenchmarkLiveConfig.kt): PRESENT/MISSING only — no plan, quota, or location limit.

Production: No GraphHopper under app/src/main/**.

Files inspected (prior audit): docs/stage-0b-multi-provider-completion.md; GraphHopperLiveBenchmarkProvider.kt; GraphHopperResponseParser.kt; GraphHopperLiveBenchmarkProviderTest.kt (partial); GraphHopperResponseParserTest.kt (referenced); CorridorClassifier.kt; BenchmarkLiveConfig.kt; BenchmarkLiveBudget.kt (partial); BenchmarkLiveHttpExecutor.kt (partial); related benchmark sanitization/runner/report files.

3. Official Sources Reviewed
Title	URL	Access date
GraphHopper Directions API
https://docs.graphhopper.com/openapi
2026-07-21
Plans and Credits
https://docs.graphhopper.com/openapi/section/plans-and-rate-limits
2026-07-21
Credit costs
https://docs.graphhopper.com/openapi/section/credit-costs
2026-07-21
Calculate a route (POST)
https://docs.graphhopper.com/openapi/routing/postroute
2026-07-21
Routing
https://docs.graphhopper.com/openapi/routing
2026-07-21
Custom Model
https://docs.graphhopper.com/openapi/custom-model
2026-07-21
Customizing priority
https://docs.graphhopper.com/openapi/custom-model/customizing-priority
2026-07-21
Limit rules to certain areas
https://docs.graphhopper.com/openapi/custom-model/limit-rules-to-certain-areas
2026-07-21
Road attributes
https://docs.graphhopper.com/openapi/custom-model/road-attributes
2026-07-21
Custom Model — Limitations
https://docs.graphhopper.com/openapi/custom-model/limitations
2026-07-21
Custom Profiles
https://docs.graphhopper.com/openapi/custom-profiles
2026-07-21
Customized Routing Profiles
https://docs.graphhopper.com/openapi/map-data-and-routing-profiles/openstreetmap/customized-routing-profiles
2026-07-21
Terms of Service
https://www.graphhopper.com/terms/
2026-07-21
Privacy (Directions API)
https://www.graphhopper.com/privacy/
2026-07-21
4. Current GraphHopper Request Baseline
Stage 0B: two-point alternative_route on car without flexible mode or constraints. Stage 0C constrained requests would differ when ch.disable and custom_model apply.

5. Custom Model Capability
Topic	Finding
Inline custom_model
POST /route; ch.disable: true required
GET /route
Inline custom_model not supported on GET
profile=car
Documented with inline model examples
Per-request inline — plan
No published Premium-only requirement was identified for per-request inline custom_model in reviewed documentation
Named cp_ via /profiles
Available starting with the Premium package; geographic coverage depends on the selected package
speed / priority / distance_influence
Independent documented properties
Custom model size limits
UNKNOWN in reviewed docs
Flexible mode performance
Slower than default CH (documented)
Mechanism	Documented plan position
Per-request inline custom_model on POST /route
Directly documented for the Route endpoint; no published Premium-only requirement identified in the reviewed documentation
Named custom profile (cp_) through /profiles
Available starting with Premium; geographic coverage depends on the selected package
Account: Current account operational readiness unconfirmed.

6. Custom Areas Capability
custom_model.areas: GeoJSON FeatureCollection, Polygon, [longitude, latitude], closed ring; in_<featureId> in conditions.

Documented area effects: e.g. priority multiply_by: 0 to block segments in an area (Customizing priority; Limit rules to certain areas). Speed adjustments are a general Custom Model capability where independently relevant; they are not the proposed Stage 0C soft-preference mechanism (§10).

Stage 0C soft preference: Reducing priority outside the candidate corridor area.

Limits: Max areas, max vertices, payload size — UNKNOWN. Multipolygon/holes — UNKNOWN where docs specify Polygon only.

7. Toll Capability
Item	Finding
Built-in avoid-toll request flag
Not on POST /route
Custom model toll
MISSING, NO, HGV, ALL
Hard exclusion
priority … multiply_by: "0"
Soft penalty
multiply_by < 1 on priority
Monetary toll
Not provided as priced toll
OSM tagging
PARTIAL reliability
8. Road-Class and Road-Reference Capability
Custom-model conditions include road_class, road_environment, road_access, toll, country, in_<area>, etc. Not documented: street_ref, literal E11/E311 conditions.

Path detail street_ref: Response details only.

9. Via-Point and Forced-Corridor Capability
Maximum routing locations per request:

Plan	Maximum routing locations per request
Free
5
Basic
30
Standard
80
Premium
200
Custom
Up to 10,000 under a custom agreement
Credits (separate): 1 credit for 2–10 locations is a credit-calculation rule, not a location limit; alternative_route adds +1 credit.

Via routing = forced corridor, not soft geographical preference.

Combinations with alternative_route + custom_model: Documented, but not experimentally verified.

Account: Applicable location limit — unconfirmed.

10. Preferred-Corridor Capability (“prefer E311”)
Soft corridor preference is expressed by reducing priority outside the candidate corridor area.

This is a geographical heuristic. It is not road-reference identification. It does not establish that the corridor is E311. It depends on the accuracy of the candidate polygon. It may affect nearby and parallel roads. It may penalize required entry and exit segments. It requires a separate corridor-verification method.

Method	Classification
Road/ref condition for E311
Road-reference preference — not supported in custom_model
Reduce priority outside candidate corridor polygon
Geographical approximation — soft
Via on corridor
Forced corridor
Road-class rules alone
Class heuristic — not E311 identification
For the proposed soft-corridor mechanism, this audit uses only reducing priority outside the candidate corridor area.

11. Avoided-Corridor Capability (“avoid E11”)
Area priority → 0 on an E11 corridor polygon = geographical approximation (hard within matched segments), INDIRECT verification, not full-network E11 proof. Ref-level E11 conditions not supported in custom_model. Defined verification method required.

12. Alternative-Route Compatibility
Documentation level
The Route API documents alternative_route parameters and custom_model on the same POST /route endpoint. Inline custom_model requires flexible routing (ch.disable: true).

Empirical level
Combined runtime behaviour has not been empirically verified for the current account and the exact Stage 0C request shape.

Combination	Classification
alternative_route + inline custom_model
Documented, but not experimentally verified
alternative_route + custom_model.areas
Documented, but not experimentally verified
Intended combination + details
Documented, but not experimentally verified
Multi-point + above (where parameters coexist)
Documented, but not experimentally verified
Schema coexistence does not prove useful alternatives under proposed constraints. That requires a separately authorized minimal live compatibility probe — not authorized here.

13. Path Details and Corridor Verification
details: street_name, street_ref, toll, road_class, etc. motorway_junction: not in reviewed OpenAPI list.

Ref-level corridor claims need a defined verification method; repo classifier alone is insufficient.

14. Geometry and Response Format
calc_points, points_encoded, multiple paths[], optional instructions, optional details. Geometry availability ≠ storage permission.

15. Storage and Retention
Client-side temporary cache: expressly allowed (Terms §5). Redistribution: custom agreement. Research archive of raw responses/polylines: STORAGE TERMS UNCONFIRMED if protocol retains provider content beyond permitted temporary handling.

16. Pricing and Current Account Entitlement
Maximum routing locations per request
Plan	Maximum routing locations per request
Free
5
Basic
30
Standard
80
Premium
200
Custom
Up to 10,000 under a custom agreement
Credits (independent)
Routing: 1 credit for 2–10 locations (calculation rule); alternative_route: +1 credit.

Inline model: Current official documentation describes per-request inline custom_model as directly usable with the POST Route endpoint. No published Premium-only requirement was identified for this per-request mechanism. The current account’s key status, plan, remaining quota, applicable routing-location limit, and permission for the intended research use remain unconfirmed.

Named profiles: Available starting with Premium; geographic coverage depends on package (see §5 table).

17. Repository Isolation Assessment
Stage 0C could remain test-only under app/src/test/.../benchmark/ without changing app/src/main if separately authorized. No implementation authorized.

18. Capability Matrix
Capability	Officially supported	Verification	Account	Main limitations	Stage 0C note
Custom model (inline POST)
YES
INDIRECT
Unconfirmed
ch.disable; slower
Probe + method pending
Custom areas
YES
INDIRECT
Unconfirmed
Polygon quality
Heuristic only
Toll penalty / hard exclusion (tagged)
YES
DIRECT for tagged segments
Unconfirmed
OSM tagging
Probe pending
Road-class penalty
YES
INDIRECT
Unconfirmed
Not corridor-specific
Heuristic
Forced corridor via-point
YES
INDIRECT
Unconfirmed
Mandatory
≠ soft prefer
Soft prefer E311
PARTIAL (outside-area priority reduction)
INDIRECT; not E311 proof
Unconfirmed
Geographical heuristic
Method pending
Avoid E11
PARTIAL (area/class)
NOT VERIFIABLE full E11
Unconfirmed
Polygon ≠ network
Method pending
Alternative routes + custom model
Documented, but not experimentally verified
Not experimentally verified — requires a separately authorized probe
Unconfirmed
Output under constraints unproven
Gate pending
Path details
YES
DIRECT for listed fields
Unconfirmed
Not in repo parser
Method needed
Corridor verification
PARTIAL
Needs defined method
Unconfirmed
Classifier insufficient
Gate #2
Full geometry
YES
DIRECT
Unconfirmed
Size/cost
Technical OK
Research response storage
Client cache explicit
N/A
Unconfirmed
No archive grant
STORAGE TERMS UNCONFIRMED if raw retention
19. Gate 1 Counting
PASS requires ≥2 of {avoided corridor, soft preferred corridor, toll avoidance} meeting all rules including confirmed operational readiness and objective verification.

Count toward PASS: 0 / 3 → consistent with INCONCLUSIVE, not FAIL.

20. Gate 1 Verdict
GATE 1 INCONCLUSIVE

Pending: (1) minimal live compatibility probe (NOT AUTHORIZED); (2) defined corridor-verification method.

Account operational readiness required before probe; account uncertainty not Premium-only inline evidence. Storage clarification only if future protocol retains provider responses/geometries beyond permitted temporary handling.

INCONCLUSIVE does not establish unsuitability or MARSHIO success. No live probe, Stage 0C implementation, or production work authorized.

21. Evidence Gaps
Current account operational readiness unconfirmed.
Documented, not experimentally verified: alternative_route + custom_model (+ areas, + details).
Defined corridor-verification method — not established.
STORAGE TERMS UNCONFIRMED if raw provider retention.
UNKNOWN: custom-model size; max areas; polygon vertices; payload limits; unsupported geometry variants (where docs silent).
Repository: no flexible-mode or details in Stage 0B GraphHopper path.
22. Recommended Next Action
Confirm account operational readiness (no credential exposure). Define corridor-verification method. Separately authorize minimal live compatibility probe — NOT AUTHORIZED by this document. Terms clarity if raw responses retained.

Final confirmation
Repository unchanged. No Git operations. No credentials exposed. No live API calls. No probe, implementation, or production authorization.

Final validation checklist
Check	Required result
Preferred-corridor mechanism consistently uses outside-area priority reduction
PASS
Road-reference targeting and geographical approximation remain distinct
PASS
Forced via-point routing remains separate from soft preference
PASS
Inline-model conclusion is limited to absence of an identified published Premium requirement
PASS
Current account operational readiness remains unconfirmed
PASS
Named custom profiles are separately recorded as available starting with Premium
PASS
Named-profile restrictions are not applied to inline per-request models
PASS
Alternative-route combinations are documented but not experimentally verified
PASS
Capability Matrix has no generic unresolved classification for the reviewed combination
PASS
Legitimate unrelated unknowns remain preserved
PASS
Routing limits remain 5 / 30 / 80 / 200 / up to 10,000
PASS
Location limits and credit calculation remain separate
PASS
Gate 1 remains INCONCLUSIVE
PASS
Pending items remain the minimal live probe and corridor-verification method
PASS
Live compatibility probe remains NOT AUTHORIZED
PASS
Stage 0C implementation remains NOT AUTHORIZED
PASS
Production implementation remains NOT AUTHORIZED
PASS
Repository remains unchanged
PASS
No Git operation was performed
PASS
GATE 1 INCONCLUSIVE

Stage 0C live compatibility probe: NOT AUTHORIZED

Stage 0C implementation: NOT AUTHORIZED

Production implementation: NOT AUTHORIZED
