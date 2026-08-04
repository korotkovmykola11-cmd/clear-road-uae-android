# Google Maps Platform ToS — Technical Audit (Repository Facts)

## Document purpose and limits

| Item | Value |
|------|-------|
| Purpose | Collect **verifiable technical facts** from this repository, mapped to Google Maps Platform Terms / documentation, for **legal review** |
| This is **not** legal advice | No compliance, violation, allowed, or prohibited conclusions |
| Code changes | **None** — audit only |
| Basis document | Prior research template: `docs/google-routes-terms-feasibility-template.md` (F01–F05 partial; F06–F21 TBD there) |
| Audit date (document) | 2026-08-03 (Room trip history follow-up **2026-08-04**; retention alignment **2026-08-04**) |
| Prod fetch path (repository) | `ArchitectureValidation.USE_ROUTES_V2_FETCH = true` → Routes API v2 `computeRoutes` via `RoutesV2Fetch.kt` |

> **Legal interpretation:** Every topic below ends with **Requires legal review** where ToS relevance is identified. Status labels in the summary table use only: `No apparent issue from repository facts` | `Needs legal review` | `Insufficient evidence`.

---

## Billing jurisdiction (administrative context — not re-verified in this audit)

| Fact | Source |
|------|--------|
| Prior template research recorded Google Cloud billing / payments profile country **United Arab Emirates (AE)**, treated as **non-EEA** for standard (non-EEA) Maps Platform Terms | `docs/google-routes-terms-feasibility-template.md` (F01–F05 evidence blocks) |
| This audit did **not** independently verify billing-account country in Google Cloud Console | **Not verified** |

Controlling Terms assumed for cross-reference below: **Google Maps Platform Terms of Service** and **Google Maps Platform Service Specific Terms** (non-EEA). EEA Terms cross-reference only.

---

## 1. Inventory — Google Maps Platform data stored or derived

Legend for **Storage lifetime**:

- **Memory only** — process/Compose session; cleared on new O/D or app restart
- **Persisted across sessions** — survives app restart on device
- **Committed into git** — version-controlled indefinitely (subject to git history)

| Repository location | Google data type | Storage lifetime | Storage format | Notes |
|---------------------|------------------|------------------|----------------|-------|
| `app/src/test/resources/benchmark/route1-DIFC-Marina.json` … `route5-JVC-AbuDhabi.json` | Full Legacy **Directions API** responses: `geocoded_waypoints[].place_id`, origin/destination addresses & lat/lng, `routes[].bounds`, overview + step **polylines**, step **coordinates**, `duration`, `duration_in_traffic`, `distance`, `html_instructions`, `copyrights`, toll hints in text | **Committed into git** (first tracked commit for `route1`: 2026-07-11) | JSON (PowerShell-export style) | Referenced by `cases.json` as frozen Stage 0A fixtures |
| `app/src/test/resources/benchmark/routes-v2-traffic/*.json` | Full/partial **Routes API v2** `computeRoutes` responses: `encodedPolyline`, decoded-equivalent geometry, `duration`, `staticDuration`, `distanceMeters`, `description` (route summary), `routeLabels`, `travelAdvisory.tollInfo`, `travelAdvisory.speedReadingIntervals`, step `navigationInstruction` (maneuver + instructions) | **Committed into git** (V2 batch manifest `capturedAtUtc` 2026-08-02; `route4` pair 2026-08-03) | JSON | Field mask documented in `routes-v2-traffic/manifest.json` |
| `app/src/test/resources/benchmark/cases.json` | O/D **coordinates** (originLat/Lng, destinationLat/Lng) — same points used to produce Google fixtures | **Committed into git** | JSON | Not a Google API response; user/research-defined endpoints |
| `docs/stage-34-4-live/route*.json` | Duplicate Legacy Directions captures (same schema as benchmark Legacy fixtures) | **Committed into git** (`route1` first commit 2026-06-10) | JSON | Source noted in `cases.json` |
| `docs/stage-35-0-audit/*.json`, `analysis.json` | **Derived** route summaries: duration/traffic text & seconds, distance km, toll, corridor **scan text** (concatenated step instructions), MARSHIO scores — extracted from Google routes | **Committed into git** | JSON | No raw polylines in `analysis.json`; per-route `scan` contains Google step instruction text |
| `docs/stage-c-baseline.md` | **Derived** corridor labels (e.g. E11, E311), **geometry fingerprints** (first 8 hex of SHA-256 of normalized polyline), winner indices, toll flags — computed from Google route geometry | **Committed into git** (doc update 2026-08-03) | Markdown | Fingerprints via `RoutePolylineGeometry.stablePolylineHash` per doc |
| `MainActivity.kt` — `directionsResponse` | Raw Legacy JSON or Routes v2 JSON string from live fetch | **Stored only in memory** (Compose `remember`; cleared when O/D changes) | String | See `applyDirectionsFetchResult` / `LaunchedEffect(selectedFromLatLng, selectedToLatLng)` |
| `MainActivity.kt` — `realRouteDebugDataList` | `RealRouteDebugData` per alternative: polyline points, durations, toll, corridor text, traffic intervals, steps | **Stored only in memory** | Kotlin in-memory objects | Cleared with O/D change |
| `DirectionsParsing.kt` — `RealRouteDebugData` | Aggregated fields from Google responses (see struct) | **Stored only in memory** (runtime) | Kotlin data class | Fields: `routePathPoints`, `durationSeconds`, `baseDurationSeconds`, `durationInTrafficSeconds`, `distanceMeters`, `tollAED`, `routeSummary`, `corridorScanText`, `googleSteps`, `trafficSpeedIntervals`, `stepTrafficRecords` |
| `RouteIntelligenceCache` (`RouteIntelligenceService.kt`) | Cache **key**: SHA-256 hash of Google-derived **lat/lng polyline** (`RoutePolylineGeometry.stablePolylineHash`). Cache **value**: OSM-derived `RouteProfile` (signals, roundabouts, main-road ratio) — **not** Google content | **Stored only in memory** | In-memory `MutableMap` | TTL **30 minutes** (`DEFAULT_TTL_MS = 30 * 60 * 1000`) |
| `RouteIntelligenceService` / `OsmOverpassSource` | Uses Google-decoded **`routePathPoints`** to build Overpass bbox and proximity tests; fetches OSM features | **Stored only in memory** (Google points in request lifecycle; OSM response in memory) | LatLng lists + OSM JSON transient | No Google geometry persisted by this module |
| `StaticMapPreviewUrlBuilder.kt` + `RouteDetailsScreen.kt` | Re-encodes Google route segments (`PolyUtil.encode`) into Static Maps URL `path=enc:...`; O/D **markers** as lat/lng | URL built **in memory** per composition | URL string | Traffic colors from `speedReadingIntervals` / step traffic |
| `StaticMapPreview.kt` (Coil) | Static Maps **image** fetched from `maps.googleapis.com/maps/api/staticmap` (URL embeds Google-derived encoded paths) | **Persisted across sessions** (Coil default **disk + memory** image cache; **no app-level TTL configured**) | PNG/JPEG bytes on device | Comment: "Coil-backed (memory + disk cache)" |
| `SharedPreferences` (`RouteFetchProvider.kt`) | `debug_fetch_provider=legacy` override only | **Persisted across sessions** | String pref | **Not** Google route data |
| Room / SQLite — `marshio_trip_history.db` (`com.clearroad.app.triphistory`, schema **v1**, no migrations) | On **navigation handoff** only: grid-snapped **originKey** + **destinationKey** (~500 m; rounded coordinate-derived keys from user **Places** lat/lng per `TripHistoryKey.kt`), **PreferenceMode**, **durationSeconds** + Salik flags from Google route metrics at handoff. Not on passive fetch. **30 consecutive calendar days** retention (`RETENTION_DAYS = 30`; cutoff `now − 30 days`; rows with `timestamp >= cutoff` kept), **max 50**/O-D+mode as additional count cap, **5 h** handoff dedup. History reads filter `timestamp >= cutoff` so stale rows do not feed median/Salik insight before physical purge on next handoff | **Persisted across sessions** (device-local) | SQLite via Room | Same **O-D pair + mode**, not physical corridor. Coordinate keys from Places API; duration/toll from Routes/Directions |
| Logcat — `DirectionsAuditLogging.kt` | Logs route **index**, durations, distance, toll **counts**, `routeSummary`, **4-byte polyline hash**, corridor **classification**, route labels (when parseable from raw JSON); **not** full polyline or full raw JSON | Transient (log buffer / exported logs if developer captures) | Text | Tag `DirectionsAudit` |
| Logcat — `RouteRecommendationSelection` / `SmoothDriveScoring` | Logs corridor snippets, duration/traffic fields, score breakdowns | Transient | Text | Tag `SmoothDriveScoring` |
| Logcat — `RouteIntelligenceReportLogger` | Logs OSM/Google signal aggregates, ETAs, route indices | Transient | Text | Tag `MARSHIO_INTELLIGENCE_REPORT` |
| `app/build/reports/stage-c-preflip-gate-report.txt` | Gate comparison output (corridor, geom_fp, indices) from live + fixture fetches | **Local build output** (not in git by default) | Text | Written by `StageCPreFlipGateTest.kt` |
| `MainActivity.kt` — `selectedFromPlaceId` / `selectedToPlaceId` | Places Autocomplete **`place_id`** | **Stored only in memory** | String? in Compose state | Resolved to lat/lng via `FetchPlaceRequest`; cleared when address text cleared |
| Places Autocomplete / Fetch Place | `place_id` → lat/lng for O/D | **Stored only in memory** (lat/lng in `selectedFromLatLng` / `selectedToLatLng`) | LatLng | API: Places SDK |
| `NavigationHandoff.kt` / `NavigationHandoffUrl.kt` | Handoff URL: origin + destination **lat/lng** only (no route polyline, no waypoints in prod path) | Transient intent URL | HTTPS query params | Waze: destination lat/lng only |
| `app/src/debug_archived/` (OSM map experiments, MapPreview*, TripStory*) | Referenced in prior git status as **untracked** debug experiments | **Insufficient evidence** — directory **not present** in workspace at audit time | — | Prior status listed OSM tile renderers + Google route review UIs; not audited in running tree |
| Test-only synthetic routes | Unit tests construct `RealRouteDebugData` without Google JSON | Test runtime only | Kotlin | Not Google API captures |

### Field coverage matrix (by storage object)

| Field | Benchmark Legacy JSON | Benchmark V2 JSON | RealRouteDebugData (runtime) | stage-35 analysis | stage-c baseline | Git |
|-------|----------------------|-------------------|------------------------------|-------------------|------------------|-----|
| polyline / coordinates | Yes | Yes (`encodedPolyline`) | Yes (`routePathPoints`) | No (scan text only) | Fingerprint only | Legacy/V2 JSON |
| origin/destination | Yes (geocoded) | Request implied in tests | User O/D LatLng | Scenario labels | Case labels | cases.json coords |
| waypoint | No in prod handoff | No in prod fetch body | No | No | No | — |
| duration | Yes | Yes | Yes | Yes | Indirect (idx) | Yes |
| staticDuration | N/A (Legacy: leg duration) | Yes | Yes (`baseDurationSeconds`) | Yes (`baseSec`) | No | V2 JSON |
| traffic duration | Yes (`duration_in_traffic`) | Yes (`duration`) | Yes | Yes | No | Yes |
| tollInfo / toll AED | Text + inferred | Yes (`tollInfo`) | Yes (`tollAED`) | Yes | Yes (toll col) | Yes |
| place_id | Yes (geocoded_waypoints) | Not observed in V2 fixtures | No (runtime uses lat/lng) | No | No | Legacy JSON |
| speedReadingIntervals | N/A Legacy fixtures | Yes | Yes (parsed) | No | No | V2 JSON |
| geometry fingerprint | — | — | Computed at runtime/tests | No | Yes (`geom_fp`) | baseline.md |
| corridor text | Step instructions | Step instructions | Yes (`corridorScanText`) | Yes (`scan`) | Yes (corridor) | analysis + runtime |
| route summary | Yes (`summary`) | Yes (`description`) | Yes | Yes | Yes | Yes |
| routeLabels | No (Legacy) | Yes | Parsed in audit logs only | No | No | V2 JSON |
| speed categories / traffic coloring | N/A | Yes | Derived for Static Maps | No | No | Runtime |

---

## 2. ToS topic sections

### A. Caching

#### Topic
Caching — 30-day rule, coordinates, route geometry, frozen fixtures, git history, runtime cache.

#### Repository facts

1. **Git — long-lived storage:** At least **11 JSON files** under `app/src/test/resources/benchmark/` and **10+ JSON files** under `docs/stage-34-4-live/` and `docs/stage-35-0-audit/` contain full or partial Google API responses or Google-derived summaries. Git history shows `docs/stage-34-4-live/route1-DIFC-Marina.json` tracked since **2026-06-10**; benchmark `route1-DIFC-Marina.json` since **2026-07-11** (repository evidence: `git log`).
2. **Runtime — raw JSON:** `directionsResponse` holds full API response string in Compose state until O/D changes; not written to app storage (Repository evidence: `MainActivity.kt`).
3. **Runtime — decoded routes:** `RealRouteDebugData` holds decoded polylines (lists of lat/lng), durations, toll, corridor text, traffic intervals in memory for the active session.
4. **RouteIntelligenceCache:** In-memory cache keyed by hash of Google polyline coordinates; stores **OSM** profiles; TTL **30 minutes** (Repository evidence: `RouteIntelligenceService.kt`, `RouteIntelligenceCache.DEFAULT_TTL_MS`).
5. **Static Maps / Coil:** Static map URLs embed Google-derived encoded polylines; Coil caches rendered **images** on device disk with **no custom eviction policy** in app code (Repository evidence: `StaticMapPreview.kt`, `StaticMapPreviewUrlBuilder.kt`).
6. **SharedPreferences:** Only debug provider flag — no Google route payload (Repository evidence: `RouteFetchProvider.kt`).
7. **No Room/SQLite** persistence for Google Maps content found in `app/src/main`.
8. **Benchmark gate reports:** `StageCPreFlipGateTest` writes `build/reports/stage-c-preflip-gate-report.txt` locally (derived comparisons, not full Google JSON).

#### Relevant Google documentation / ToS

- **Google Maps Platform Terms of Service §3.2.3(b) No Caching:** *"Customer will not cache Google Maps Content except as expressly permitted under the Maps Service Specific Terms."*  
  URL: https://cloud.google.com/maps-platform/terms (accessed 2026-08-03)
- **Service Specific Terms §19.3 Routes API — Caching:** *"Customer may temporarily cache latitude (lat) and longitude (lng) values from the Routes API for up to 30 consecutive calendar days, after which Customer must delete the cached latitude and longitude values."*  
  URL: https://cloud.google.com/maps-platform/terms/maps-service-terms (accessed 2026-08-03)
- **Service Specific Terms §4.3 Directions API — Caching:** Same **30 consecutive calendar days** rule for lat/lng from Directions API.
- **Service Specific Terms §3 Google ID Caching:** *"Customer may cache the Google ID values from the Services that return such field… (a) place_id from Places API, Directions API, Geolocation API and Routes API…"* (per documentation).

#### Potential relevance

- Git-stored fixtures and docs contain **lat/lng polylines**, **place_id**, durations, toll fields, and instruction text beyond bare lat/lng pairs.
- Some git-tracked Legacy captures may exceed **30 calendar days** from capture date to present (e.g. `docs/stage-34-4-live/` since 2026-06-10).
- Service Specific Terms §19.3 / §4.3 explicitly address **lat/lng** caching duration; they do **not** in the fetched text enumerate polylines, durations, tollInfo, or full JSON responses.
- RouteIntelligenceCache TTL (30 min) is shorter than 30 days but stores a **derivative key** from Google coordinates.
- Coil disk cache duration for Static Maps images embedding route geometry is **not configured** in repository.

#### Legal interpretation

**Requires legal review.**

---

### B. Use with non-Google maps

#### Topic
Whether Google Routes / Directions data is used together with a non-Google map (post–MapPreviewCard removal, Static Maps, debug renderers, custom polyline renderer).

#### Repository facts

1. **Dependencies (`app/build.gradle.kts`):** Implements Places SDK, Play Services Location, `android-maps-utils` (PolyUtil/SphericalUtil), Coil. **No** `play-services-maps` or `maps-compose` dependency in production `implementation` list (Repository evidence).
2. **No matches** in `app/src/main` for `GoogleMap(`, `MapPreviewCard`, `maps-compose`, or OSM map composables (Repository evidence: grep).
3. **Route preview (prod):** `RouteDetailsScreen` → `buildStaticMapUrl` → `StaticMapPreview` loads **Google Static Maps API** (`https://maps.googleapis.com/maps/api/staticmap`) with `path=enc:...` built from Google route segments (`StaticMapPreviewUrlBuilder.kt`, `TrafficPolylineBuilder.kt`).
4. **Traffic coloring:** `TrafficPolylineBuilder` splits Google `routePathPoints` using `speedReadingIntervals` / step traffic, assigns speed categories, re-encodes segments for Static Maps paths — custom **visualization** on **Google** Static Maps base map.
5. **Route Intelligence:** `OsmOverpassSource` uses Google polyline only to query **OSM Overpass** and compute features; **no map UI** renders Google geometry on OSM tiles in `app/src/main` (Repository evidence: `RouteIntelligenceSources.kt`).
6. **Navigation handoff:** `openGoogleMapsHandoff` → Google Maps app URL; `openWazeHandoff` → Waze URL with **destination lat/lng only** — does not pass Google polyline (Repository evidence: `NavigationHandoff.kt`, `NavigationHandoffUrl.kt`).
7. **`app/src/debug_archived/`:** Not present in workspace at audit time — cannot confirm whether archived OSM map experiments combined Google route data with non-Google maps (**Insufficient evidence**).

#### Relevant Google documentation / ToS

- **Service Specific Terms §19.2 Routes API:** *"Customer must not use Google Maps Content from the Routes API in conjunction with a non-Google map."*
- **Service Specific Terms §19.1 Routes API:** *"Customer may use Google Maps Content from the Routes API in Customer Applications without a corresponding Google Map."*
- **Service Specific Terms §4.2 Directions API:** *"Customer must not use Google Maps Content from the Directions API in conjunction with a non-Google map."*
- **Platform Terms §3.2.3(e) No Use With Non-Google Maps:** Prohibits using Google Maps Core Services with or near a non-Google Map (examples include displaying Places content on a non-Google Map).
- **Routes API Policies** (referenced in prior template F04): Routes results displayed **on a map** should use a **Google Map** (Static Maps is a Maps Platform map product — **not verified** in this audit whether Static Maps qualifies identically to Maps SDK for §19.2).

#### Potential relevance

- Production UI displays Google-derived route geometry on **Google Static Maps**, not on an interactive third-party map engine in `app/src/main`.
- Google route geometry is used as input to OSM analysis without an OSM **map display** in the prod path reviewed.
- Waze handoff uses user/places-derived coordinates, not exported Google polylines.
- Archived debug OSM renderers (if any) are **unverified** in current tree.

#### Legal interpretation

**Requires legal review.**

---

### C. DecisionEngine / route ranking architecture

#### Topic
Architecture of Google alternatives vs MARSHIO recommendation (not legal assessment).

#### Repository facts

1. **Fetch:** Prod uses Routes v2 `computeRoutes` with `computeAlternativeRoutes: true`, `routingPreference: TRAFFIC_AWARE`, `extraComputations: [TOLLS, TRAFFIC_ON_POLYLINE]` (`RoutesV2ResponseAdapter.buildComputeRoutesRequestBody`). Legacy path uses Directions JSON `alternatives=true` (`DirectionsParsing.buildDirectionsUrl`).
2. **Adapter:** `RoutesV2ResponseAdapter.extractRouteLegsDebugData` maps each route in `routes[]` to `RealRouteDebugData` (polyline, durations, toll, corridor text, traffic intervals, steps).
3. **Selection:** `RouteRecommendationSelection.pickRecommendedRouteIndex` chooses winner by mode:
   - **FASTEST / NO_TOLLS:** legacy cost/time scoring (`pickLegacyRouteIndex`)
   - **CALM:** `SmoothDriveScoring.pickWinnerIndex` + optional `CalmStressTieBreak.applyPolicyB` (Policy B hybrid rules)
4. **Google default assumption in UI comparison:** `RouteDetailsComparisonPresentation` uses `GOOGLE_DEFAULT_INDEX = 0` (hardcoded) for "Google says" vs "MARSHIO says" — **does not** read Routes v2 `routeLabels` / `DEFAULT_ROUTE` in this path (Repository evidence: `RouteDetailsComparisonPresentation.kt`).
5. **Disagreement UI:** When `recommendedIndex != 0`, UI can show `GoogleMarshioDecisionState.DISAGREES` with comparative ETAs, toll, traffic delay (`RouteDetailsComparisonPresentation.buildGoogleMarshioDecision`).
6. **Legacy rollback engine:** `RouteDecisionEngine` (sample routes) inactive when `RECOMMENDATION_ONLY_HOME = true` (Repository evidence: `ArchitectureValidation.kt`, `RouteDecisionEngine.kt`).
7. **Handoff:** Does not navigate along MARSHIO-selected polyline — opens Google Maps with O/D only (`NavigationHandoffUrl.googleMapsDirectionsUrl`).

#### Relevant Google documentation / ToS

- **Routes API — Get alternative routes:** Documents `computeAlternativeRoutes`, `routeLabels` (`DEFAULT_ROUTE`, `DEFAULT_ROUTE_ALTERNATE`), customer route choice using returned properties (Repository evidence: prior template F02/F03 citations).
- **Platform Terms §3.2.3(c) No Creating Content From Google Maps Content** and **§3.2.3(d) No Re-Creating Google Products or Features** — cited in prior template for derived ranking / comparison patterns (**not analyzed** in prior template F06–F08).

#### Potential relevance

- App requests Google's alternative route set, then applies **MARSHIO-owned scoring** that may select a non-index-0 route.
- UI explicitly compares MARSHIO recommendation to **index 0** labeled "Google's default", which may or may not match Google's `DEFAULT_ROUTE` label when v2 labels differ from array order.
- Derived explanations and scores are user-facing (Route Details).

#### Legal interpretation

**Requires legal review.**

---

### D. ML / AI / Grounded Output

#### Topic
Whether "Grounded Output" Terms apply to ordinary Directions API / Routes API responses.

#### Repository facts

1. **No LLM / Vertex / Gemini / Maps Grounding Lite client code** in `app/src/main` (Repository evidence: grep for LLM, Gemini, Grounding, Vertex — no matches).
2. **RouteIntelligenceReportJson** serializes comparison reports to JSON string in code — used for logging/diagnostics pattern; **no** evidence of persisting this JSON to disk in `app/src/main` (Repository evidence: `RouteIntelligenceReportLogger.kt`).
3. App uses deterministic scoring (`SmoothDriveScoring`, `DriverStressAudit`, `SalikDetection`) — not ML model inference over Google data.

#### Relevant Google documentation / ToS

- **Service Specific Terms §10 (Maps Grounding Lite API)** — Definitions (accessed 2026-08-03):
  - *"Grounded Output" means output created when Google Maps Content is combined with the output of any LLM.*
  - §10.2–10.3 restrict extraction, model training, and modification of Grounded Output.
  - §10.2.2 allows caching Grounded Output up to 30 days **for LLM display optimization**.
- **Maps Grounding Lite product docs** describe MCP tools for LLMs (`compute_routes`, `search_places`) — separate from standard REST Routes API used by the app (Google documentation: https://developers.google.com/maps/ai/grounding-lite — accessed 2026-08-03).
- **Standard Routes API / Directions API** caching and use restrictions remain in §19 / §4 of Service Specific Terms — not under §10 Grounded Output definitions in the fetched text.

#### Potential relevance

- §10 "Grounded Output" definition requires combination with **LLM output**. This app's prod path uses REST `computeRoutes` / Directions JSON without an LLM in the loop.
- Maps Grounding Lite `compute_routes` MCP tool is a **different product surface** from `routes.googleapis.com/directions/v2:computeRoutes` used in `RoutesV2Fetch.kt`.

#### Legal interpretation

**Requires legal review.**  
**Documentation ambiguity:** If legal review scope includes future LLM features fed with stored Google route JSON, interaction of §10 vs §19 may need explicit Google guidance. From repository facts alone, §10 appears scoped to LLM + Maps Grounding Lite, not the current REST adapter path — **not verified** by Google legal text beyond fetched definitions.

---

### E. Persistence and git (supplement to A)

#### Topic
Persistence beyond caching — benchmarks, docs, test reports.

#### Repository facts

- Frozen fixtures are **intentional long-lived test inputs** committed to git (`cases.json` notes: "frozen Google Classic Directions fixtures").
- `StageCPreFlipGateTest` may call live Google APIs and write human-readable comparison reports under `app/build/reports/` (local only).
- `docs/stage-35-0-audit/analysis.json` persists Google-derived **scan text** and metrics without encoded polylines.

#### Relevant Google documentation / ToS

- §3.2.3(b) No Caching (Platform Terms).
- §19.3 / §4.3 lat/lng 30-day cache permissions (Service Specific Terms).
- Prior template F10 (Persistence) — **TBD**, not completed.

#### Potential relevance

Git-stored artifacts function as indefinite storage outside the 30-day lat/lng cache window described in Service Specific Terms.

#### Legal interpretation

**Requires legal review.**

---

### F. Derived labels, explanations, scores (supplement)

#### Topic
MARSHIO-derived presentation atop Google route data.

#### Repository facts

- **Corridor labels / identity:** `RouteIdentityExtractor`, `CorridorClassifier`, `SmoothDriveScoring.classifyCorridor` derive labels from Google `routeSummary` + step/corridor text.
- **Scores:** `SmoothDriveScoring`, legacy toll/time scoring, `DriverStressAudit` — used for CALM selection.
- **UI copy:** `RouteDetailsComparisonPresentation` generates "MARSHIO disagrees with Google" narratives comparing Google index-0 vs MARSHIO pick using Google durations, toll AED, traffic delay.
- **Salik heuristic:** `SalikDetection` may augment scoring when Google `tollAED == 0` (`ArchitectureValidation.USE_HEURISTIC_SALIK_FOR_SCORING`).

#### Relevant Google documentation / ToS

- Platform Terms §3.2.3(c) No Creating Content From Google Maps Content.
- Platform Terms §3.2.3(d) No Re-Creating Google Products or Features.
- Prior template F06–F08 — **TBD**.

#### Potential relevance

User-facing text and rankings are **not** raw Google fields; they are computed comparisons and preference-based recommendations using Google ETAs, distances, toll fields, and instruction-derived corridor text.

#### Legal interpretation

**Requires legal review.**

---

### G. Places API and place_id

#### Topic
place_id storage and lat/lng resolution.

#### Repository facts

- Autocomplete stores `prediction.placeId` in Compose state (`selectedFromPlaceId`, `selectedToPlaceId`).
- `FetchPlaceRequest` resolves place_id → lat/lng for routing; lat/lng used in API requests.
- Legacy benchmark JSON includes `geocoded_waypoints[].place_id` **committed in git**.
- Runtime does not persist place_id to SharedPreferences / DB.

#### Relevant Google documentation / ToS

- Service Specific Terms **§3 Google ID Caching** — place_id cacheable per documentation for Directions/Routes/Places APIs.
- Service Specific Terms §14.3 Places API — lat/lng cache 30 days.

#### Potential relevance

Git fixtures retain place_id indefinitely. Runtime place_id is session-only; resolved coordinates flow into Routes/Directions requests and memory-held `RealRouteDebugData`.

#### Legal interpretation

**Requires legal review.**

---

### H. External navigation handoff

#### Topic
Handoff to Google Maps and Waze.

#### Repository facts

- Google Maps: `https://www.google.com/maps/dir/?api=1&origin=...&destination=...&travelmode=driving&dir_action=navigate` — lat/lng only, no waypoints (`NavigationHandoffUrl.kt`).
- Waze: destination lat/lng only.
- Handoff triggered from `MarshioRoutePreviewScreen` / route preview flow (Repository evidence: grep `openGoogleMapsHandoff`).
- MARSHIO-selected route geometry is **not** passed to Google Maps intent.

#### Relevant Google documentation / ToS

- Prior template F21 — **TBD**.
- Navigation SDK / routeToken not used for handoff (Routes v2 `routeToken` not referenced in handoff code).

#### Potential relevance

Handoff uses coordinates (user-selected via Places or map picker), not exported Google polylines or turn lists.

#### Legal interpretation

**Requires legal review.**

---

### I. Attribution

#### Topic
Google attribution in non-map and Static Maps presentation.

#### Repository facts

- Static Maps responses include Google logo/copyright on the **image** (standard Static Maps behavior — **Runtime observation** not captured in repo code).
- Route Details UI shows Google-derived durations, summaries, Static Map preview — **no** explicit "Powered by Google" composable found in `RouteDetailsScreen.kt` grep scope (Repository evidence: partial read).
- Legacy JSON includes `"copyrights": "Powered by Google, ©2026 Google"`.
- Prior template F05 documents attribution requirements for non-map localized text — prod UI shows formatted duration/distance text from adapter, not necessarily raw `localizedValues` fields.

#### Relevant Google documentation / ToS

- Routes API Policies — attribution when not on Google Map (prior template F05 citations).
- Platform Terms §3.2.2(b) Attribution.

#### Potential relevance

Static Maps preview may satisfy map-attribution via embedded image; textual route metrics on surrounding UI may have separate attribution obligations.

#### Legal interpretation

**Requires legal review.**

---

## 3. Source index (by evidence type)

| Evidence type | Examples in this document |
|---------------|---------------------------|
| **Repository evidence** | All Kotlin/JSON/Markdown paths cited; `git log` dates for tracked files |
| **Google documentation** | Routes alternative routes docs (via prior template); Maps Grounding Lite developer pages |
| **Google ToS** | Platform Terms §3.2.3; Service Specific Terms §3, §4, §10, §19 (fetched 2026-08-03) |
| **Test fixture** | `app/src/test/resources/benchmark/**`, `routes-v2-traffic/manifest.json` |
| **Generated code** | `RouteIntelligenceReportJson.toJson` (in-memory serialization helper) |
| **Runtime observation** | Coil default disk cache; Static Maps image attribution — **partially Not verified** |
| **Not verified** | Billing account country (re-check Cloud Console); `debug_archived` OSM experiments; exact Coil cache TTL/size on device; whether Static Maps counts as "Google Map" for every §19.2 scenario |

---

## 4. Summary table

| Topic | Repository fact | Relevant Google clause | Status |
|-------|-----------------|------------------------|--------|
| Git benchmark Legacy JSON | Full Directions responses with polylines, place_id, durations committed since 2026-06–2026-07 | §3.2.3(b); §4.3 | **Needs legal review** |
| Git benchmark V2 JSON | Full computeRoutes fields including polylines, tollInfo, speedReadingIntervals | §3.2.3(b); §19.3 | **Needs legal review** |
| docs/stage-* audit artifacts | Derived scans, metrics, geom fingerprints in git | §3.2.3(b); §19.3 | **Needs legal review** |
| Runtime session data | `RealRouteDebugData` + raw JSON in memory only; cleared on O/D change | §19.3; §4.3 | **No apparent issue from repository facts** (session-only; subject to legal view on transient memory) |
| RouteIntelligenceCache | 30-min in-memory; OSM values keyed by Google polyline hash | §19.3 (lat/lng) | **Needs legal review** (key derived from Google coordinates) |
| Coil Static Maps cache | Image disk cache of map containing encoded Google paths; no TTL in app | §3.2.3(b); §19.3 | **Needs legal review** |
| SharedPreferences | Debug provider flag only | — | **No apparent issue from repository facts** |
| Room trip history (`trip_history`) | Handoff-only; rounded coordinate-derived O-D keys (Places lat/lng); Google duration/toll; **30-day** time retention + max **50** count cap; insight reads exclude rows below cutoff | §19.3; §4.3; §14.3 Places lat/lng | **Needs legal review** |
| Static Maps route preview | Google-derived polylines on Google Static Maps | §19.2; §4.2 | **Needs legal review** (Static Maps vs "Google Map" classification) |
| OSM intelligence | Google polyline used for Overpass queries; no OSM map UI in prod main | §19.2 | **No apparent issue from repository facts** (no non-Google map display found in `app/src/main`) |
| Waze handoff | Destination lat/lng only | §19.2 | **No apparent issue from repository facts** (no Google geometry passed) |
| debug_archived OSM experiments | Directory absent in workspace | §19.2 | **Insufficient evidence** |
| DecisionEngine ranking | MARSHIO scoring over Google alternatives; UI compares to index 0 | §3.2.3(c)/(d); alternatives docs | **Needs legal review** |
| Grounded Output / ML | No LLM; REST Routes/Directions only | §10 Maps Grounding Lite | **No apparent issue from repository facts** for current code path; **Needs legal review** if product scope expands |
| place_id in git fixtures | Committed in Legacy JSON | §3 Google ID Caching | **Needs legal review** |
| Logcat audits | Metrics/hashes/summaries; not full polylines | §3.2.3(b) | **Needs legal review** (if logs retained/exported) |
| Attribution in UI | Static Maps image; textual metrics in Compose | §3.2.2(b); Routes policies | **Needs legal review** |
| Navigation handoff | O/D lat/lng to Google Maps app | F21 TBD in template | **Needs legal review** |

---

## 5. Lists for legal handoff

### No apparent issue from repository facts

- Google route data in prod is held **in memory** for the active O/D session and cleared when origin/destination changes (`MainActivity.kt`).
- **No Room/SQLite** persistence of Google Maps responses in `app/src/main`.
- SharedPreferences stores only **`debug_fetch_provider`**, not Google content.
- **`app/src/main` does not render** Google route geometry on an OSM/third-party interactive map (no `GoogleMap` / OSM map composable found).
- **Waze handoff** passes destination coordinates only, not Google polylines or steps.
- **No LLM / Grounded Output pipeline** in production code; REST Routes v2 + Legacy Directions adapters only.
- **RouteIntelligenceCache values** are OSM-derived profiles; Google content is not stored as the cached value payload.

### Needs legal review

- **Git-stored** Legacy and V2 benchmark fixtures and `docs/stage-*` captures — full responses, polylines, place_id, durations, traffic intervals, instruction text; retention **unbounded** in git history.
- **30-day caching rules** (§19.3, §4.3) vs types of data stored (full JSON, polylines, tollInfo, durations, scans, fingerprints, analysis.json).
- **Platform Terms §3.2.3(b) No Caching** vs permitted lat/lng caching and place_id caching rules.
- **Room trip history** — rounded coordinate-derived origin/destination keys (Places lat/lng) + Google duration/toll retained on device for **30 consecutive calendar days** (time cutoff); max **50** rows per O-D+mode is an additional count cap; rows below the cutoff are excluded from insight calculations even before physical purge.
- Whether the cited **30-day lat/lng caching clause** (§19.3 / §4.3 / §14.3) applies identically to client-side Room storage of rounded coordinate-derived keys **requires legal review**.
- **Coil disk cache** of Static Maps images whose URLs embed Google-encoded route geometry — duration and whether URL parameters constitute stored Google Maps Content.
- **Static Maps + custom traffic-colored polylines** — whether this satisfies "Google Map" context under §19.2 / Routes policies.
- **MARSHIO DecisionEngine** — alternative route ranking, CALM/FASTEST/NO_TOLLS scoring, and UI comparing **index 0** to MARSHIO pick (including when that differs from `DEFAULT_ROUTE` labels).
- **Derived labels, explanations, scores** shown to users (`RouteDetailsComparisonPresentation`, corridor classifiers, Salik heuristics).
- **Route Intelligence** using Google polylines to query OSM (combined data presentation to user).
- **place_id** in committed fixtures vs runtime session-only storage.
- **Logcat audit tags** emitting Google-derived metrics, summaries, partial corridor text — if logs are collected/stored.
- **Attribution** for non-map textual Google-derived ETAs/distances alongside Static Maps preview.
- **Billing jurisdiction** (AE / non-EEA) — confirm controlling Terms still accurate.
- **Grounded Output §10** — confirm inapplicability to current REST usage; clarify if any planned AI features change scope.

### Practical follow-up candidates (discussion topics — not fixes)

- Intended **retention policy** for benchmark fixtures and `docs/stage-*` JSON relative to Google cache periods.
- Whether git history should retain **full Google API responses** or redacted/synthetic substitutes for CI.
- **Coil cache policy** (disable disk cache, shorten TTL, or cache-bust Static Map URLs) for route previews.
- **GOOGLE_DEFAULT_INDEX = 0** vs Routes v2 **`routeLabels`** for "Google says" comparison accuracy and disclosure.
- **Stage C gate** live API usage in CI (`StageCPreFlipGateTest`) — frequency, stored reports, and billing/ToS implications.
- **Logcat audit verbosity** — corridor text and Google metrics in production builds.
- **Attribution placement** audit on Route Details screen (map + text blocks).
- **Trip history on device** — retention/dedup policy vs lat/lng caching rules; coordinate key provenance (Places API).
- Written Google confirmation topics flagged in prior template **F01** (multi-request waypoint research) if that pattern is revived.

---

## 6. Relation to prior template

`docs/google-routes-terms-feasibility-template.md` remains a **separate feasibility template** (F01–F05 partially reviewed; F06–F21 TBD). This audit **does not** assign template verdicts (`Allowed with constraints`, etc.) and **does not** authorize implementation.

Cross-reference:

| Template ID | This audit coverage |
|-------------|---------------------|
| F09 Session-only processing | Runtime facts in §1 + Topic A |
| F10 Persistence | Topic A, E |
| F11 Caching | Topic A |
| F12 Route fingerprints | Inventory + `docs/stage-c-baseline.md` |
| F15 Route geometry | Inventory + Static Maps |
| F18 Traffic intervals | V2 fixtures + `TrafficPolylineBuilder` |
| F19/F20 ETA/toll comparison | DecisionEngine + Route Details UI |
| F21 Handoff | Topic H |

---

*End of audit document. No repository changes were made.*
