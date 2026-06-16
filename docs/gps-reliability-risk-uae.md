# GPS Reliability Risk (UAE)

**Status:** Backlog — risk register only  
**Date:** 2026-06-02  
**Scope:** Documentation. No code, no stages, no implementation.

---

## Summary

GPS jamming and spoofing activity in the UAE region can degrade **device position accuracy**. This affects live navigation and location-based UX — not MARSHIO’s core route **decision** layer when origin and destination are known coordinates.

**Product boundary (locked):**

```
MARSHIO decides. Google Maps navigates.
```

If Google Maps misbehaves at an interchange because of GPS interference, that does **not** mean MARSHIO chose the wrong route.

---

## Observations (field context)

Reported over roughly **3–4 weeks** (as of mid-2026), including on **aviation Garmin** units (e.g. GTN 650, GTN 750, GNS 430, Aera-class devices) — not only consumer phones.

Typical symptoms when interference is present:

- Loss of normal GPS fix or delayed fix
- Position jump or drift on the map
- Incorrect “current” position relative to ground truth
- Navigation cues that disagree with the road network at junctions

This suggests a **regional environmental factor**, not an app-specific bug in MARSHIO.

---

## What MARSHIO uses today

Route calculation goes through **Google Directions API** using explicit **From** and **To** coordinates (Places autocomplete → lat/lng).

Google routing relies on:

- Road graph / network data
- Cloud traffic models
- Aggregated probe data from many devices
- Server-side path computation

For a trip like **Ajman → Dubai Marina**, Google can still return alternatives and ETAs even when a single phone’s GPS is unreliable — **as long as the start and end points sent to the API are correct**.

---

## Current product scope (what we test vs. what we do not)

### What road testing validates today

```
From (manual / Places)
    ↓
Google Directions
    ↓
Alternatives
    ↓
MARSHIO scoring
    ↓
Recommendation
```

This is the **product core** and matches how the app is built and tested now (emulator and fixtures with explicit O/D).

### What is outside current MARSHIO

The following are **not in the app today** and are **not** covered by current test passes:

```
GPS permissions
    ↓
Fused Location Provider
    ↓
Current Location button
    ↓
Location drift / spoofing handling
    ↓
Live tracking / turn-by-turn
```

| Capability | In MARSHIO today? |
|------------|-------------------|
| GPS / location permissions | No |
| Fused Location Provider | No |
| “Current Location” as From | No |
| Live position on map | No |
| In-app navigation | No (handoff to Google Maps / Waze) |

**Implication:** GPS reliability risk **exists** in the UAE, but it sits **beyond the current product boundary**. Manual From/To testing is not a workaround — it is the correct scope for decision-layer validation right now.

---

## Risk split: decision vs. navigation

| Layer | Owner | GPS jamming impact |
|-------|--------|-------------------|
| Route alternatives + traffic ETAs | Google Directions (server) | Low — driven by submitted O/D, not phone GPS during fetch |
| Mode scoring + ONE recommendation | MARSHIO | Low — same, given correct O/D |
| Start point if derived from device GPS | Future / handoff | **High** — wrong or drifting From poisons the Directions request |
| Live guidance after handoff | Google Maps / Waze | **High** — junction errors, reroutes, arrival detection |

---

## Out of scope for near-term roadmap

Do **not** prioritize now (explicit deferral):

- AI Assistant
- Voice input / output
- Current Location as default From
- Live map tracking
- In-app turn-by-turn

These add GPS and realtime UX surface area before decision layer and copy are fully road-proven.

---

## Future checks (when Current Location or live UX is added)

Use this section when product scope expands — not before.

### A. Current Location as From

- [ ] What happens on first launch with no location permission?
- [ ] What happens when permission is denied?
- [ ] What happens with **no fix** (timeout, indoors, interference)?
- [ ] What happens with **stale fix** (last known position minutes old)?
- [ ] What happens with **jumping fix** (position teleports between reads)?
- [ ] Is the user shown **clearly** whether From came from GPS vs typed address?
- [ ] Can the user override a bad GPS-derived start before Directions fetch?

### B. Location quality gates (product behaviour)

- [ ] Minimum accuracy threshold before auto-filling From
- [ ] Debounce / stability window before accepting GPS as start
- [ ] Copy when GPS is weak: block vs warn vs fallback to manual entry
- [ ] Retry behaviour after interference clears

### C. Directions request integrity

- [ ] Log or surface when submitted From differs materially from last GPS read
- [ ] Confirm recommendation refresh when user corrects start point
- [ ] Peak-traffic scenarios with corrected vs uncorrected start

### D. Navigation handoff

- [ ] Handoff URL / intent still valid if MARSHIO used manual From but Maps uses live GPS
- [ ] User expectation: “MARSHIO picked route X; Maps may reroute from my actual position”
- [ ] Waze handoff — same checks
- [ ] Copy on handoff screen if GPS quality is known to be poor (future)

### E. Regional / environmental

- [ ] Spot-check same O/D during reported interference windows (manual From/To unchanged — control)
- [ ] Compare with aviation or external GPS health reports when available
- [ ] Document known UAE corridors or periods if patterns emerge (no speculation in UI without data)

---

## Testing strategy until Current Location ships

| Method | Use for |
|--------|---------|
| **Manual From / To** (Places, emulator, fixtures) | Scoring, copy, mode differentiation, peak-traffic gates — **primary** |
| **Current Location** | Deferred — activates GPS Reliability checklist above |
| **Live drive with Maps** | Navigation handoff sanity only; not decision-layer regression |

---

## Related product docs

- `docs/CONTEX.md` — Decision First; Clear Road decides → Maps/Waze navigate
- `docs/PRODUCT_IDENTITY.md` — not a navigation app
- Stage 35.x audit fixtures — explicit O/D captures (`docs/stage-35-0-audit/`)

---

## Revision log

| Date | Change |
|------|--------|
| 2026-06-02 | Initial backlog entry — risk register after architecture review; no implementation |
